package de.felixp.fractalsgdx.rendering.renderers;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.FloatArray;
import com.github.tommyettinger.colorful.oklab.ColorTools;
import com.github.tommyettinger.colorful.oklab.GradientTools;

import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.rendering.FractalsFrameBuffer;
import de.felixp.fractalsgdx.rendering.rendererparams.DrawParamsTurtleGraphics;
import de.felixp.fractalsgdx.rendering.ParamListener;
import de.felixp.fractalsgdx.rendering.RendererContext;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.numbers.impl.DoubleComplexNumber;
import de.felixperko.fractals.system.numbers.impl.DoubleNumber;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;
import de.felixperko.fractals.system.systems.infra.SystemContext;

import static de.felixp.fractalsgdx.rendering.renderers.TurtleGraphicsSystemContext.*;

public class TurtleGraphicsRenderer extends AbstractFractalRenderer {
    final static String passthroughVertexShaderPath = "PassthroughVertex130.glsl";
    final static String passthroughFragmentShaderPath = "PassthroughFragment130.glsl";

    TurtleGraphicsSystemContext systemContext;
    NumberFactory nf = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

    ShapeRenderer shapeRenderer;

    List<Float> posXStack = new ArrayList<>();
    List<Float> posYStack = new ArrayList<>();
    List<Double> angleStack = new ArrayList<>();
    List<Integer> depthStack = new ArrayList<>();

    String lsystem = null;

    OrthographicCamera cam;

    boolean changedParams = false;
    boolean resetLSystem = false;

    FrameBuffer fbo;

    ShaderProgram passthroughShader;

    public TurtleGraphicsRenderer(RendererContext rendererContext) {
        super(rendererContext);
    }

    ComplexNumber lastMidpoint;

    @Override
    public void init() {
        passthroughShader = compileShader(passthroughVertexShaderPath, passthroughFragmentShaderPath);
        systemContext = new TurtleGraphicsSystemContext();
        systemContext.addParamListener((newSupp, oldSupp) -> {
            changedParams = true;
            String uid = newSupp.getUID();
            if (!generatedActions.contains(uid) && !generatedAngleParams.contains(uid) && !uid.equals(UID_START_ANGLE)){
                resetLSystem = true;
                paramsChanged(systemContext.getParamContainer());
            }
            if (uid.equals(PARAM_RESOLUTIONSCALE)){
                sizeChanged();
            }
        });

        cam = new OrthographicCamera(getWidth(), getHeight());
        cam.position.set(cam.viewportWidth / 2f, cam.viewportHeight / 2f, 0);
        cam.update();

        addPanListener(newMidpoint -> {
            ComplexNumber midpoint = systemContext.getMidpoint();
            ComplexNumber delta = newMidpoint.copy();
            delta.sub(midpoint);
            cam.translate((float)delta.getReal().toDouble(), (float)delta.getImag().toDouble());
            systemContext.setMidpoint(midpoint);
            paramsChanged(systemContext.getParamContainer());
        });

        TurtleGraphicsRenderer thisRenderer = this;
        addListener(new ActorGestureListener(0.001f, 0.4f, 1.1f, Integer.MAX_VALUE){
            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
                ParamSupplier supp = systemContext.getParamContainer().getParam(CommonFractalParameters.PARAM_MIDPOINT);
                ComplexNumber midpoint = supp.getGeneral(ComplexNumber.class);
                ComplexNumber shift = nf.ccn(deltaX*rendererContext.getProperties().getW(), deltaY*rendererContext.getProperties().getH());
                Number scale = systemContext.getParamContainer().getParam(TurtleGraphicsSystemContext.UID_SCALE).getGeneral(Number.class);
                shift.multNumber(scale);
                midpoint.sub(shift);
                paramsChanged(systemContext.getParamContainer());
            }

            float lastZoomDistance = -1;
            float lastZoomInitialDistance = -1;

            @Override
            public void zoom(InputEvent event, float initialDistance, float distance) {
                if (Gdx.app.getType() == Application.ApplicationType.Android){
                    if (Gdx.input.isTouched(2))
                        return;
                    if (lastZoomInitialDistance != initialDistance) {
                        lastZoomInitialDistance = initialDistance;
                        lastZoomDistance = distance;
                    }
                    else if (distance == lastZoomDistance) {
                        return;
                    }
                    NumberFactory nf = thisRenderer.getSystemContext().getNumberFactory();
                    thisRenderer.zoom(nf.createNumber((lastZoomDistance/distance)));
                    lastZoomDistance = distance;
                }
            }
        });
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        float scaleFactor = systemContext == null ? 1.0f : (float) systemContext.getParamContainer().getParam(PARAM_RESOLUTIONSCALE).getGeneral(Number.class).toDouble();
        scaleFactor = Math.max(scaleFactor, 0.05f);
        int fboWidth = (int) (scaleFactor * getWidth());
        int fboHeight = (int) (scaleFactor * getHeight());
        try {
            fbo = new FrameBuffer(Pixmap.Format.RGBA8888, fboWidth, fboHeight, false);
        } catch (IllegalStateException e){
            e.printStackTrace();
            System.err.println("Error while creating fbo with dimensions: "+fboWidth+"x"+fboHeight);
        }
        if (cam != null) {
            cam.viewportWidth = getWidth();
            cam.viewportHeight = getHeight();
            cam.position.set(cam.viewportWidth / 2f, cam.viewportHeight / 2f, 0);
            cam.update();
        }
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (amountY == 0)
            return false;
//        for (int i = 0 ; i < Math.abs(amountY) ; i++) {
//            Number factor = nf.createNumber(amountY < 0 ? 0.9 : 1.1);
//            zoomAndPan(factor, Gdx.input.getX(), Gdx.input.getY());
//            paramsChanged(systemContext.getParamContainer());
//        }
        scrollVelocity += amountY;
        return true;
    }

    float scrollFactor = 1.0f;
    float scrollFactorShift = 2.0f;
    double scrollReductionPerSecond = 4.0;
    double scrollDecayPerSecond = 6.0;
    double scrollVelocity = 0.0f;

    protected void handleScroll(){
        double delta = Math.min(Gdx.graphics.getDeltaTime(), 1.0);
        double velocityDelta = delta;
        double velocityDecrease = velocityDelta*scrollReductionPerSecond;
        double velocityDecay = velocityDelta*scrollDecayPerSecond;
        double velocityMult = Math.max(0.0, 1.0-velocityDecay);

        double newScrollVelocity = 0.0;
        if (scrollVelocity > 0.0) {
            newScrollVelocity = Math.max(0.0, scrollVelocity - velocityDecrease)*velocityMult;
        } else if (scrollVelocity < 0.0){
            newScrollVelocity = Math.min(0.0, scrollVelocity + velocityDecrease)*velocityMult;
        }

        scrollVelocity = newScrollVelocity;

        if (scrollVelocity != 0.0) {
            double factor = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ? scrollFactorShift : scrollFactor;
            double zoom = 1.0 + scrollVelocity * delta * factor;
            if (zoom > 0.0)
                zoomAndPan(systemContext.getNumberFactory().cn(zoom), Gdx.input.getX(), Gdx.input.getY());
        }
    }

    @Override
    protected void zoom(Number zoomFactor) {
        ParamSupplier supp = systemContext.getParamContainer().getParam(TurtleGraphicsSystemContext.UID_SCALE);
        Number scale = supp.getGeneral(Number.class);
        scale.mult(zoomFactor);
        cam.zoom = (float)scale.toDouble();
    }

    @Override
    public void render(Batch batch, float parentAlpha) {

        handleScroll();

        ComplexNumber midpoint = systemContext.getMidpoint();
        Number scale = systemContext.getParamContainer().getParam(TurtleGraphicsSystemContext.UID_SCALE).getGeneral(Number.class);
        cam.position.x = (float)midpoint.getReal().toDouble();
        cam.position.y = (float)midpoint.getImag().toDouble();
        cam.zoom = (float)scale.toDouble();
        cam.update();

        ParamContainer params = getSystemContext().getParamContainer();

        boolean[] animationFlags = applyParameterAnimations(params, FractalsGdxMain.mainStage.getClientParams(), systemContext.getNumberFactory());
        boolean reset = animationFlags[1];
        boolean changed = animationFlags[0];

        batch.end();
        prepareShapeRenderer(batch);

        fbo.begin();
        renderLSystem(shapeRenderer, reset, changed);
        fbo.end();

        closeShapeRenderer();
        batch.flush();
        batch.setShader(passthroughShader);
        batch.begin();

        Texture renderedTexture = fbo.getTextureAttachments().get(0);
        renderedTexture.bind(0);
        batch.draw(renderedTexture, getX(), getY(), getWidth(), getHeight(), 0, 0, renderedTexture.getWidth(), renderedTexture.getHeight(), false, true);

        if (isScreenshot(true))
            makeScreenshot();

//        batch.begin();
    }

    float screenCoordFactor;
    float screenCoordOffsetX;
    float screenCoordOffsetY;

    int lastIterations = -1;
    String lastVariables;
    List<Character> generatedActions = new ArrayList<>();
    List<Character> generatedRules = new ArrayList<>();
    List<String> generatedAngleParams = new ArrayList<>();
    Map<Character, ParamListener> ruleParamListeners = new HashMap<>();

    final static int ACTIONID_DRAW = 0;
    final static int ACTIONID_MOVE = 1;
    final static int ACTIONID_TURN = 2;
    final static int ACTIONID_PUSH = 3;
    final static int ACTIONID_POP = 4;

    Set<Character> charsDraw = new HashSet<>();
    Set<Character> charsTurn = new HashSet<>();
    Set<Character> charsPush = new HashSet<>();
    Set<Character> charsPop = new HashSet<>();
    Map<Character, Double> angles = new HashMap<>();

    private void renderLSystem(ShapeRenderer shapeRenderer, boolean reset, boolean changed) {
        ParamContainer params = getSystemContext().getParamContainer();

        refreshParams(params);
        if (changedParams) {
            changedParams = false;
            changed = true;
        }
        if (resetLSystem){
            resetLSystem = false;
            reset = true;
        }

        int iterations = params.getParam(TurtleGraphicsSystemContext.UID_ITERATIONS).getGeneral(Integer.class);

        if (iterations != lastIterations){
            lastIterations = iterations;
            reset = true;
        }
        if (reset)
            changed = true;

        long t1 = System.currentTimeMillis();

        if (lsystem == null || reset){
            lsystem = getLSystem(iterations);
//            System.out.println(lsystem);
        }

        long t2 = System.currentTimeMillis();

        if (vertices == null || changed)
            getVertices(params, iterations);

        long t3 = System.currentTimeMillis();

        int colorSteps = 256;
        float colorStepMinDelta = 1f/colorSteps;
        float lastHue = -1;

//        shapeRenderer.polyline(vertices);
        Color color = new Color();
        ParamContainer clientParams = FractalsGdxMain.mainStage.getParamUI().getClientParamsSideMenu().getParamContainer();
        Color startColor = clientParams.getParam(DrawParamsTurtleGraphics.PARAM_START_COLOR).getGeneral(Color.class);
        Color endColor = clientParams.getParam(DrawParamsTurtleGraphics.PARAM_END_COLOR).getGeneral(Color.class);
        float startAlpha = (float)clientParams.getParam(DrawParamsTurtleGraphics.PARAM_START_ALPHA).getGeneral(Number.class).toDouble();
        float endAlpha = (float)clientParams.getParam(DrawParamsTurtleGraphics.PARAM_END_ALPHA).getGeneral(Number.class).toDouble();
        FloatArray gradient = GradientTools.makeGradient(ColorTools.fromColor(startColor), ColorTools.fromColor(endColor), maxDepth+1);
        for (int depth = 0 ; depth <= maxDepth ; depth++) {
            float relDepth = ((depth / (float) maxDepth));
            if (color == null || Math.abs(relDepth - lastHue) > colorStepMinDelta) {
                lastHue = relDepth;
//                color = new Color().fromHsv(100 * relDepth + 20, 1.0f, 1.0f);
                color = ColorTools.toColor(color, gradient.get(depth));
                color.a = MathUtils.lerp(startAlpha, endAlpha, relDepth);
                shapeRenderer.setColor(color);
            }
            List<Float> vertexList = vertices.get(depth);
            int lineCount = vertexList.size() / 4;
            for (int i = 0; i < lineCount; i++) {
//            float hue = (1f-(i/(float)lineCount));
                shapeRenderer.rectLine(vertexList.get(i * 4 + 0), vertexList.get(i * 4 + 1), vertexList.get(i * 4 + 2), vertexList.get(i * 4 + 3), 1.0f);
//                shapeRenderer.rectLine(vertexList.get(i * 4 + 0), vertexList.get(i * 4 + 1), vertexList.get(i * 4 + 2), vertexList.get(i * 4 + 3), );
            }
        }

        long t4 = System.currentTimeMillis();
//        System.out.println("lsystem steps took (ms): "+(t2-t1)+" "+(t3-t2)+" "+(t4-t3));

    }

    private boolean refreshParams(ParamContainer params) {
        String alphabet = (String)params.getParam(TurtleGraphicsSystemContext.UID_ALPHABET).getGeneral(String.class);
        String variables = params.getParam(TurtleGraphicsSystemContext.UID_VARIABLES).getGeneral(String.class);

        List<Character> vars = new ArrayList<>();
        for (int i = 0 ; i < variables.length() ; i++)
            vars.add(variables.charAt(i));
        List<Character> alphabetChars = new ArrayList<>();
        for (int i = 0 ; i < alphabet.length() ; i++)
            alphabetChars.add(alphabet.charAt(i));

        ParamContainer paramContainerCopy = new ParamContainer(params, false);
        ParamConfiguration config = getSystemContext().getParamConfiguration();
        List<Character> generateActions = new ArrayList<>();
        List<Character> generateRules = new ArrayList<>();
        for (int i = 0 ; i < alphabet.length() ; i++){
            Character c = alphabet.charAt(i);
            if (!generatedActions.contains(c)){
                generateActions.add(c);
            }
            if (vars.contains(c) && !generatedRules.contains(c))
                generateRules.add(c);
        }

        Class[] staticClass = new Class[]{StaticParamSupplier.class};
        boolean changed = false;
        for (Character c : generateRules){
            String ruleName = "rule " + c;
            ParamSupplier ruleSupplier = params.getParam(ruleName);
            if (ruleSupplier == null)
                ruleSupplier = new StaticParamSupplier(ruleName, String.valueOf(c));
            config.createTempParam(ruleName, ruleName, "Rules", CommonFractalParameters.stringType, ruleSupplier, paramContainerCopy, staticClass);
            ParamListener listener = new ParamListener() {
                @Override
                public String getUid() {
                    return ruleName;
                }

                @Override
                public void changed(ParamSupplier newSupp, ParamSupplier oldSupp) {
                    resetLSystem = true;
                }
            };
            systemContext.addParamListener(listener);
            ruleParamListeners.put(c, listener);
            generatedRules.add(c);
            changed = true;
        }
        for (Character c : generateActions){
            String actionName = "action " + c;
            ParamSupplier actionSupplier = params.getParam(actionName);
            if (actionSupplier == null)
                actionSupplier = new StaticParamSupplier(actionName, "draw");
            config.createTempParam(actionName, actionName, "Actions", CommonFractalParameters.stringType, actionSupplier, paramContainerCopy, staticClass);
            generatedActions.add(c);
            changed = true;
        }

        List<String> angleNames = new ArrayList<>();
        //for all actions
        for (Character c : new ArrayList<>(generatedActions)){
            //remove old unused actions
            String paramName = "action "+c;
            if (!alphabetChars.contains(c)){
                paramContainerCopy.removeParam(paramName);
                systemContext.getParamConfiguration().removeParameterDefinition(paramName);
                generatedActions.remove(c);
                changed = true;
            }

            //generate missing angle params and fill angleName list
            ParamSupplier actionSupplier = params.getParam(paramName);
            String action = (String)actionSupplier.getGeneral();
            if (action != null && action.startsWith("turn(") && action.endsWith(")")){
                String angleParamName = action.substring(5, action.length()-1).trim();
                angleNames.add(angleParamName);
                if (generatedAngleParams.contains(angleParamName))
                    continue;
                ParamSupplier angleSupplier = params.getParam(angleParamName);
                if (angleSupplier == null)
                    angleSupplier = new StaticParamSupplier(angleParamName, nf.cn(45.0));
                ParamDefinition def = config.createTempParam(angleParamName, angleParamName, "Angles", CommonFractalParameters.numberType, angleSupplier, paramContainerCopy, staticClass);
                def.withHints("ui-element[default]:slider min=-90 max=90");
                def.setResetRendererOnChange(false);
                generatedAngleParams.add(angleParamName);
                changed = true;
            }
        }
        //for all rules
        for (Character c : new ArrayList<>(generatedRules)){
            //remove old unused rules
            if (!vars.contains(c)){
                String paramName = "rule "+c;
                paramContainerCopy.removeParam(paramName);
                systemContext.getParamConfiguration().removeParameterDefinition(paramName);
                generatedRules.remove(c);
                systemContext.removeParamListener(ruleParamListeners.remove(c));
                changed = true;
            }
        }
        //for all angle params
        for (String name : new ArrayList<>(generatedAngleParams)){
            //remove old unused angle params
            if (!angleNames.contains(name)){
                paramContainerCopy.removeParam(name);
                systemContext.getParamConfiguration().removeParameterDefinition(name);
                generatedAngleParams.remove(name);
                changed = true;
            }
        }

        if (changed){
            //force PropertyList reset
//            FractalsGdxMain.mainStage.getParamUI().setServerParameterConfiguration(this, paramContainerCopy, config);
            params = paramContainerCopy;
            systemContext.setParameters(paramContainerCopy);
            FractalsGdxMain.mainStage.getParamUI().setServerParameterConfiguration(this, null, config);
            paramsChanged(systemContext.getParamContainer());
        }

        charsDraw.clear();
        charsTurn.clear();
        charsPush.clear();
        charsPop.clear();
        angles.clear();

//        List<Double> anglesList = new ArrayList<>();
        for (ParamSupplier supp : params.getParameters()){
            if (supp.getUID().startsWith("action ") && supp.getUID().length() == 8){
                Character c = supp.getUID().charAt(7);
                String action = supp.getGeneral(String.class);
                if (action.startsWith("turn(") && action.endsWith(")")) {
                    charsTurn.add(c);

                    String angleParamName = action.substring(5, action.length() - 1).trim();
                    ParamSupplier angleSupplier = params.getParam(angleParamName);
                    if (angleSupplier != null) {
                        double angle = angleSupplier.getGeneral(Number.class).toDouble();
                        angles.put(c, angle);
//                        anglesList.add(angle);
                    }
                }
                else if (action.equals("draw")){
                    charsDraw.add(c);
                }
                else if (action.equals("push")){
                    charsPush.add(c);
                }
                else if (action.equals("pop")){
                    charsPop.add(c);
                }
            }
        }

        return changed;
    }

//    float[] arr;

    List<List<Float>> vertices = new ArrayList<>();
    List<Integer> depths = new ArrayList<>();
    Integer maxDepth = -1;

    private void getVertices(ParamContainer params, int iterations){


        int characterCount = lsystem.length();

        float posX = 0;
        float posY = 0;
        double angle = params.getParam(TurtleGraphicsSystemContext.UID_START_ANGLE).getGeneral(Number.class).toDouble();

        int lineCount = 0;

        float segmentLength = getHeight()/2;
        for (int i = 0 ; i < iterations ; i++)
            segmentLength *= 0.5f;

        int accumulatedSegments = 1;

        vertices.clear();
        depths.clear();

        Map<Character, Integer> actionMap = new HashMap<>();
        charsDraw.forEach(c -> actionMap.put(c, ACTIONID_DRAW));
        charsTurn.forEach(c -> actionMap.put(c, ACTIONID_TURN));
        charsPush.forEach(c -> actionMap.put(c, ACTIONID_PUSH));
        charsPop.forEach(c -> actionMap.put(c, ACTIONID_POP));

        Integer depth = 0;
        maxDepth = -1;

        float variationSeed = (float)getSystemContext().getParamContainer().getParam(PARAM_VARIATION_SEED).getGeneral(Number.class).toDouble();
        float variationAngle = (float)getSystemContext().getParamContainer().getParam(PARAM_VARIATION_ANGLE).getGeneral(Number.class).toDouble();
        float variationLength = (float)getSystemContext().getParamContainer().getParam(PARAM_VARIATION_LENGTH).getGeneral(Number.class).toDouble();
        boolean doVariationAngle = variationAngle != 0f;
        boolean doVariationLength = variationLength != 0f;

        float randInterpolation = variationSeed - (int)variationSeed;
        Random randomAngle = new Random((int)Math.floor(variationSeed));
        Random randomLength = new Random(randomAngle.nextInt());
        Random randomAngle2 = new Random((int)Math.ceil(variationSeed));
        Random randomLength2 = new Random(randomAngle2.nextInt());

        for (int i = 0 ; i < characterCount ; i++){
            char c = lsystem.charAt(i);
            switch ((int)actionMap.getOrDefault(c, -1)) {
                case (ACTIONID_DRAW):
                    if (i + 1 < lsystem.length()) {
                        char c2 = lsystem.charAt(i + 1);
                        if (charsDraw.contains(c2)) {
                            accumulatedSegments++;
                            continue;
                        }
                    }

                    float length = !doVariationLength ? 1f : 1f + variationLength*(MathUtils.lerp(randomLength.nextFloat(), randomLength2.nextFloat(), randInterpolation)*0.5f-0.5f);
                    length *= segmentLength * accumulatedSegments;
                    float dX = (float) Math.cos(angle * Math.PI / 180f) * length;
                    float dY = (float) Math.sin(angle * Math.PI / 180f) * length;
                    float newPosX = posX + dX;
                    float newPosY = posY + dY;

                    if (depth > maxDepth) {
                        maxDepth = depth;
                        vertices.add(new ArrayList<>());
                    }
                    List<Float> vertexList = vertices.get(depth);
//                    depths.add(depth);
                    depth++;
                    vertexList.add(posX);
                    vertexList.add(posY);
                    vertexList.add(newPosX);
                    vertexList.add(newPosY);


                    posX = newPosX;
                    posY = newPosY;
                    accumulatedSegments = 1;

                    lineCount++;
                    break;
                case (ACTIONID_TURN):
                    float varAngle = !doVariationAngle ? 1f : 1f + variationAngle*(MathUtils.lerp(randomAngle.nextFloat(), randomAngle2.nextFloat(), randInterpolation)*0.5f-0.5f);
                    angle += angles.get(c)*varAngle;
                    break;
                case (ACTIONID_PUSH):
                    posXStack.add(posX);
                    posYStack.add(posY);
                    angleStack.add(angle);
                    depthStack.add(depth);
                    break;
                case (ACTIONID_POP):
                    int index = posXStack.size() - 1;
                    angle = angleStack.remove(index);
                    posX = posXStack.remove(index);
                    posY = posYStack.remove(index);
                    depth = depthStack.remove(index);
                    break;
                default:
                    break;
            }
        }

//        if (arr == null || arr.length != vertices.size())
//            arr = new float[vertices.size()];
//        for (int i = 0 ; i < arr.length ; i++)
//            arr[i] = vertices.get(i);
//        return arr;

        System.out.println("drawn lines: "+lineCount);
    }

    private String getLSystem(int iterations) {

        Map<Character, String> rules = new HashMap<>();

        String axiom = systemContext.getParamContainer().getParam(TurtleGraphicsSystemContext.UID_AXIOM).getGeneral(String.class);
        for (ParamSupplier supp : systemContext.getParamContainer().getParameters()){
            if (supp.getUID().startsWith("rule ") && supp.getUID().length() == 6){
                Character c = supp.getUID().charAt(5);
                String text = supp.getGeneral(String.class);
                rules.put(c, text);
            }
        }

        String s = axiom;
        for (int i = 0 ; i < iterations; i++){
            StringBuilder sb = new StringBuilder();
            for (int j = 0 ; j < s.length() ; j++){
                Character c = s.charAt(j);
                if (rules.containsKey(c)){
                    sb.append(rules.get(c));
                }
                else {
                    sb.append(c);
                }
            }
            s = sb.toString();
        }
        return s;
    }

    private void prepareShapeRenderer(Batch batch) {
        if (shapeRenderer == null)
            shapeRenderer = new ShapeRenderer(50000000);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
//        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        shapeRenderer.setProjectionMatrix(cam.combined);
//        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());

        Rectangle scissors = new Rectangle();
        Rectangle clipBounds = new Rectangle(getX(), getY(), getWidth(), getHeight());
        ScissorStack.calculateScissors(getStage().getCamera(), batch.getTransformMatrix(), clipBounds, scissors);
        ScissorStack.pushScissors(scissors);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void closeShapeRenderer() {
        shapeRenderer.end();
        ScissorStack.popScissors();
    }

    @Override
    public int getPixelCount() {
        return (int)(Math.ceil(getWidth())*Math.ceil(getHeight()));
    }

    @Override
    public float getScreenX(double real) {
        return getScreenX(systemContext.getNumberFactory().createNumber(real));
    }

    @Override
    public float getScreenY(double imag) {
        return getScreenY(systemContext.getNumberFactory().createNumber(imag));
    }

//    public float getScreenXFast(float mappedX, float midpointX){
//
//        return (mappedX-midpointX) * screenCoordFactor + screenCoordOffsetX;
//    }
//
//    public float getScreenYFast(float mappedY, float midpointY){
//
//        return (mappedY-midpointY) * screenCoordFactor + screenCoordOffsetY;
//    }

    @Override
    public float getScreenX(Number real) {
        Number zoom = (Number) systemContext.getParamValue(TurtleGraphicsSystemContext.UID_SCALE);
        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue(CommonFractalParameters.PARAM_MIDPOINT));

        NumberFactory nf = systemContext.getNumberFactory();
        Number res = real.copy();
        res.sub(midpoint.getReal());
        res.mult(nf.createNumber(getHeight()));
        res.div(zoom);

        res.add(nf.createNumber(getX()));
        res.add(nf.createNumber(getWidth()*0.5f));
        return (float)res.toDouble();
    }

    @Override
    public float getScreenY(Number imag) {
        Number zoom = (Number) systemContext.getParamValue(TurtleGraphicsSystemContext.UID_SCALE);
        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue(CommonFractalParameters.PARAM_MIDPOINT));

        NumberFactory nf = systemContext.getNumberFactory();
        Number res = imag.copy();
        res.sub(midpoint.getImag());
        res.mult(nf.createNumber(getHeight()));
        res.div(zoom);

        res.add(nf.createNumber(getY()));
        res.add(nf.createNumber(getHeight()*0.5f));
        return (float)res.toDouble();
    }

    @Override
    public Number getReal(float screenX) {

        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue(CommonFractalParameters.PARAM_MIDPOINT));
        screenX -= getX()+getWidth()/2;

        NumberFactory nf = systemContext.getNumberFactory();
        Number zoomNumber = (Number) systemContext.getParamValue(TurtleGraphicsSystemContext.UID_SCALE);
        Number resultNumber = nf.createNumber(screenX);
//        Number heightNumber = nf.createNumber(getHeight());
//        resultNumber.div(heightNumber);
        resultNumber.mult(zoomNumber);
        resultNumber.add(midpoint.getReal());
        return resultNumber;
    }

    @Override
    public Number getImag(float screenY) {
        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue(CommonFractalParameters.PARAM_MIDPOINT));
        screenY += getY();
        screenY += getHeight()/2;

        NumberFactory nf = systemContext.getNumberFactory();
        Number zoomNumber = (Number) systemContext.getParamValue(TurtleGraphicsSystemContext.UID_SCALE);
        Number resultNumber = nf.createNumber(Gdx.graphics.getHeight()-screenY);
//        Number heightNumber = nf.createNumber(getHeight());
//        resultNumber.div(heightNumber);
        resultNumber.mult(zoomNumber);
        resultNumber.add(midpoint.getImag());
        return resultNumber;
    }

    @Override
    public SystemContext getSystemContext() {
        return systemContext;
    }

    @Override
    public void setRefreshColoring() {

    }

    @Override
    public void reset() {

    }

    @Override
    public void removed() {

    }

    @Override
    public void focusChanged(boolean focusedNow) {
        if (focusedNow)
            focus();
    }

    @Override
    public double getXShift() {
        return 0;
    }

    @Override
    public double getYShift() {
        return 0;
    }
}
