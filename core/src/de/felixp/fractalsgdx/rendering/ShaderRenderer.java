package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.animation.ParamAnimation;
import de.felixp.fractalsgdx.animation.interpolations.ComplexNumberParamInterpolation;
import de.felixp.fractalsgdx.animation.interpolations.ParamInterpolation;
import de.felixp.fractalsgdx.rendering.orbittrap.OrbittrapContainer;
import de.felixp.fractalsgdx.rendering.rendererlink.RendererLink;
import de.felixp.fractalsgdx.ui.AnimationsUI;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.fractals.data.AbstractArrayChunk;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.LayerConfiguration;
import de.felixperko.fractals.system.calculator.ComputeExpression;
import de.felixperko.fractals.system.calculator.EscapeTime.EscapeTimeCpuCalculatorNew;
import de.felixperko.fractals.system.calculator.infra.FractalsCalculator;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.statistics.IStats;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstLayer;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstUpsampleLayer;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.system.systems.stateinfo.TaskState;
import de.felixperko.fractals.system.systems.stateinfo.TaskStateInfo;
import de.felixperko.fractals.system.task.FractalsTask;
import de.felixperko.fractals.system.task.Layer;
import de.felixperko.fractals.system.task.TaskManager;
import de.felixperko.fractals.system.thread.CalculateFractalsThread;
import de.felixperko.expressions.ComputeExpressionBuilder;
import de.felixperko.fractals.util.NumberUtil;

public class ShaderRenderer extends AbstractFractalRenderer {

    private static Logger LOG = LoggerFactory.getLogger(ShaderRenderer.class);

    final static String shader1 = "CalcExpressionFragmentTemplate.glsl";
    final static String shader2 = "SobelDecodeFragment.glsl";
    final static String vertexPassthrough = "PassthroughVertex.glsl";

    boolean refresh = true;
    boolean paramsChanged = false;

    FrameBuffer fboImage;
    FrameBuffer fboDataFirst;
    FrameBuffer fboDataSecond;
    FrameBuffer fboSampleCount;
    boolean useFirstDataFbo = true;
    boolean prevDataFboOutdated = false;

//    Texture palette;

    boolean reuseEnabled = true;
    boolean renderedPart = false;
    float pannedDeltaX = 0;
    float pannedDeltaY = 0;

//    int currentRefreshes = 0;
//    float decode_factor = 1f;

    ShaderProgram computeShader;
    ShaderProgram coloringShader;
    ShaderProgram passthroughShader;

//    Texture computeBufferTexture;

    Matrix3 matrix = new Matrix3(new float[] {1,0,0, 0,1,0, 0,0,1, 0,0,0});

    float colorShift = 0f;

    boolean burningship = false;
    boolean juliaset = false;

    double xPos;
    double yPos;
//    float biasReal = 0f;
//    float biasImag = 0f;

    int width;
    int height;

    double resolutionScale = 1.0;
    int resolutionX = 0;
    int resolutionY = 0;


//    int iterations = 1000;

//    float scale = 3f;

    int sampleLimit = 100;

    SystemContext systemContext = new GPUSystemContext(this);
    ComplexNumber anchor = systemContext.getNumberFactory().createComplexNumber(0,0);

    ComputeExpression expression;
    String expressionString;

    ShaderBuilder shaderBuilder;

    boolean drawTraces = true;
    float mouseX, mouseY;

    PanListener panListener;

    int progressiveRenderingMissingFrames;

    public ShaderRenderer(RendererContext rendererContext){
        super(rendererContext);
        rendererContext.setRenderer(this);
    }

    @Override
    protected String processShadertemplateLine(String templateLine) {
        return shaderBuilder.processShadertemplateLine(templateLine, getScaledHeight());
    }

    boolean isFocused = false;

    @Override
    public void init(){

        ((GPUSystemContext)systemContext).init();
        updateExpression();
        compileShaders();

        ShaderRenderer thisRenderer = this;

        getStage().addListener(new ClickListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                isFocused = event.getTarget() == thisRenderer;
                drawTraces = isFocused;
                return super.touchDown(event, x, y, pointer, button);
            }
        });

        addListener(new ActorGestureListener(){
            @Override
            public void tap(InputEvent event, float x, float y, int count, int button) {

                boolean clickedControlPoint = handleClickControlPoints(x, y, button);
                if (clickedControlPoint)
                    return;

//                else {
                    boolean changed = false;
                    ParamContainer paramContainer = systemContext.getParamContainer();
                    NumberFactory nf = paramContainer.getClientParameter("numberFactory").getGeneral(NumberFactory.class);
                    Number zoom = paramContainer.getClientParameter("zoom").getGeneral(Number.class);
                    Number factor = null;
                    if (button == Input.Buttons.LEFT) {
                        factor = nf.createNumber(0.5);
                        changed = true;
                    } else if (button == Input.Buttons.RIGHT) {
                        factor = nf.createNumber(2);
                        changed = true;
                    }

                    if (factor != null) {
                        zoom.mult(factor);
                        paramsChanged(paramContainer);
                    }

                    if (changed) {
                        LOG.debug("tapped");
                        anchor = paramContainer.getClientParameter("midpoint").getGeneral(ComplexNumber.class);
                        reset();
                    }
//                }
            }

            @Override
            public void touchDown(InputEvent event, float x, float y, int pointer, int button) {
                getStage().setKeyboardFocus(thisRenderer);
                ParamInterpolation selectedInterpolation = AnimationsUI.getSelectedInterpolation();
                if (selectedInterpolation instanceof ComplexNumberParamInterpolation){
                    List<ComplexNumber> controlPoints = selectedInterpolation.getControlPoints(true);
                    for (ComplexNumber controlPoint : controlPoints){
                        if (controlPointCollision(x, y, controlPoint) && isControlPointSelected(controlPoint)) {
                            movingControlPoint = controlPoint;
                            LOG.warn("movingControlPoint: "+movingControlPoint);
                            return;
                        }
                    }
                }
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
                boolean movedControlPoint = dragMovingControlPoint(x, y);
                if (!movedControlPoint)
                    move(deltaX, deltaY, 0.1f);
            }

            @Override
            public void zoom(InputEvent event, float initialDistance, float distance) {
                super.zoom(event, initialDistance, distance);
            }

            @Override
            public boolean longPress(Actor actor, float x, float y) {
                boolean left = Gdx.input.isButtonPressed(0);
                boolean right = Gdx.input.isButtonPressed(1);
                boolean middle = Gdx.input.isButtonPressed(2);
                return super.longPress(actor, x, y);
            }

            ParamAnimation zoomAnimation = null;
            boolean disableContinuousRendering = false;

//            @Override
//            public void touchDown(InputEvent event, float x, float y, int pointer, int button) {
//                float currentZoom = (float)paramContainer.getClientParameter("zoom").getGeneral(Number.class).toDouble();
//                if (zoomAnimation == null) {
//                    zoomAnimation = new LinearNumberParamAnimation("zoom", 10, currentZoom+"", currentZoom/10000000000d+""){
////                        @Override
////                        public Number getInterpolatedValue(double progress, NumberFactory nf) {
//////                            Number interpolatedValue = super.getInterpolatedValue(progress, nf);
////                            Number differenceOfScale = getEndValue(nf).copy();
////                            differenceOfScale.div(getStartValue(nf));
////                            double logDifferenceOfScale = Math.log10(differenceOfScale.toDouble());
////                            double progressInCycle = scaleProgress(progress);
////                            double valueDifferenceInScale = logDifferenceOfScale * progressInCycle;
////                            double valueFactor = Math.pow(valueDifferenceInScale, 10);
////                            Number result = getEndValue(nf).copy();
////                            result.sub(getStartValue(nf)); //diff
////                            result.mult(nf.createNumber(valueFactor)); //diff*factorAtProgression
////                            result.add(getStartValue(nf)); //add baseline
////                            return result;
////                        }
//
//                        @Override
//                        protected double scaleProgress(double progress) {
//                            double linearProgress = super.scaleProgress(progress);
//                            //(1 - e^(-x)) / (1 - e^-1)
//                            //double logarithmicProgress = Math.exp(1 / linearProgress - 1.0);
//                            double logarithmicProgress = (1 - Math.pow(-linearProgress, 10)) / (1-Math.pow(-1, 10));
//                            return logarithmicProgress;
//                        }
//                    };
//
//                    addParamAnimationServer(zoomAnimation);
//                    if (!Gdx.graphics.isContinuousRendering()) {
//                        Gdx.graphics.setContinuousRendering(true);
//                        disableContinuousRendering = true;
//                    }
//                }
//            }


            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                movingControlPoint = null;
//                if (zoomAnimation != null){
//                    addParamAnimationServer(zoomAnimation);
//                    zoomAnimation = null;
//                    if (disableContinuousRendering) {
//                        Gdx.graphics.setContinuousRendering(false);
//                        disableContinuousRendering = false;
//                    }
//                }
            }
        });

        panListener = new PanListener() {
            @Override
            public void panned(ComplexNumber midpoint) {
                double oldScreenX = getScreenX(xPos);
                double oldScreenY = getScreenY(yPos);
                double newScreenX = getScreenX(midpoint.realDouble());
                double newScreenY = getScreenY(midpoint.imagDouble());
                double deltaX = newScreenX - oldScreenX;
                double deltaY = newScreenY - oldScreenY;
                xPos = midpoint.realDouble();
                yPos = midpoint.imagDouble();
                pannedDeltaX += deltaX;
                pannedDeltaY -= deltaY;
                paramsChanged(getSystemContext().getParamContainer());
            }
        };
        addPanListener(panListener);
    }

    protected boolean dragMovingControlPoint(float x, float y) {
        ParamInterpolation selectedInterpolation = AnimationsUI.getSelectedInterpolation();
        if (movingControlPoint == null || selectedInterpolation == null)
            return false;
        float newScreenX = x;
        float newScreenY = y;
        ComplexNumber newControlPoint = getComplexMapping(newScreenX, getHeight()-newScreenY);
        int selectedIndex = -1;
        for (int i = 0 ; i < selectedControlPoints.size() ; i++){
            if (selectedControlPoints.get(i).equals(movingControlPoint)){
                selectedIndex = i;
                break;
            }
        }
        selectedInterpolation.moveControlPoint(movingControlPoint, newControlPoint, null, true);
        movingControlPoint = newControlPoint;
        if (selectedIndex != -1){
            selectedControlPoints.set(selectedIndex, movingControlPoint);
        }
        return true;
    }

    protected boolean handleClickControlPoints(float x, float y, int button) {

        if (button != Input.Buttons.LEFT)
            return false;

        ParamInterpolation selectedInterpolation = AnimationsUI.getSelectedInterpolation();
        if (!(selectedInterpolation instanceof ComplexNumberParamInterpolation))
            return false;
        List<ComplexNumber> controlPoints = selectedInterpolation.getControlPoints(true);
        for (ComplexNumber controlPoint : controlPoints){
            boolean mouseHover = controlPointCollision(x, y, controlPoint);
            if (mouseHover){
                boolean wasSelected = isControlPointSelected(controlPoint);
                selectedControlPoints.clear();
                if (!wasSelected)
                    selectedControlPoints.add(controlPoint);
                return true;
            }
        }
        boolean removedSelection = selectedControlPoints.size() > 0;
        selectedControlPoints.clear();
        return removedSelection;
    }

    @Deprecated
    protected boolean handleMoveControlPoint(float x, float y) {


//        for (ComplexNumber controlPoint : selectedControlPoints){
//            if (isControlPointSelected(controlPoint)){
//                if (controlPointCollision(x, y, controlPoint)) {
//                    selectedControlPoints.remove(controlPoint);
//                    ComplexNumber newControlPoint = getComplexMapping(x, y);
//                    selectedControlPoints.add(newControlPoint);
//                    AnimationsUI.getSelectedInterpolation().moveControlPoint(controlPoint, newControlPoint, true);
//                    return true;
//                }
//            }
//        }
        return false;
    }

    protected boolean controlPointCollision(float x, float y, ComplexNumber controlPoint) {
        float screenX = getScreenX(controlPoint.realDouble());
        float screenY = getScreenY(controlPoint.imagDouble());
        float dx = x - screenX;
        float dy = y - screenY;
        double dist = Math.sqrt(dx*dx + dy*dy);
        return dist <= controlPointSelectionRadius;
    }

    float getScaledWidth(){
        return (float) Math.ceil(getWidth()*resolutionScale);
    }

    float getScaledHeight(){
        return (float) (getHeight()*resolutionScale);
    }

    float panPartOffsetX = 0;
    float panPartOffsetY = 0;

    protected void move(float deltaX, float deltaY, float roundMaxDelta) {
        if (deltaX == 0 && deltaY == 0)
            return;

        deltaX += panPartOffsetX;
        deltaY += panPartOffsetY;
        float requestedDeltaX = deltaX;
        float requestedDeltaY = deltaY;
        //fit pixel to pixel for optimizations
        if (Math.abs(deltaX-Math.round(deltaX))*resolutionScale < roundMaxDelta)
            deltaX = (float)(Math.round(deltaX*resolutionScale)/resolutionScale);
        if (Math.abs(deltaY-Math.round(deltaY))*resolutionScale < roundMaxDelta)
            deltaY = (float)(Math.round(deltaY*resolutionScale)/resolutionScale);
        panPartOffsetX = requestedDeltaX-deltaX;
        panPartOffsetY = requestedDeltaY-deltaY;

        ParamContainer paramContainer = systemContext.getParamContainer();
        NumberFactory nf = paramContainer.getClientParameter("numberFactory").getGeneral(NumberFactory.class);
        Number zoom = paramContainer.getClientParameter("zoom").getGeneral(Number.class);
        ComplexNumber midpoint = paramContainer.getClientParameter("midpoint").getGeneral(ComplexNumber.class);
        ComplexNumber delta = nf.createComplexNumber(deltaX, -deltaY);
        delta.divNumber(nf.createNumber(getHeight()));
        delta.multNumber(zoom);
        midpoint.sub(delta);
        systemContext.setMidpoint(midpoint);
        rendererContext.panned(systemContext.getParamContainer());
        resetProgressiveRendering();
        //see panned listener added at the end of init()...
    }

    private void paramsChanged(ParamContainer paramContainer) {
        ((MainStage) FractalsGdxMain.stage).getParamUI().setServerParameterConfiguration(this, paramContainer, ((GPUSystemContext) systemContext).paramConfiguration);
    }

    public void compileShaders() {
        compileComputeShader();
        coloringShader = compileShader(vertexPassthrough, shader2);
        passthroughShader = compileShader(vertexPassthrough, "PassthroughFragment.glsl");
        renderedPart = false;
//        width = Gdx.graphics.getWidth();
//        height = Gdx.graphics.getHeight();
    }

    public void compileComputeShader() {
        ShaderProgram.pedantic = false;
        computeShader = compileShader(vertexPassthrough, shader1);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (fboDataFirst == null) //init() not called
            return;

        if (isProgressiveRenderingFinished())
            applyParameterAnimations(systemContext.getParamContainer(), ((MainStage)FractalsGdxMain.stage).getClientParameters(), systemContext.getNumberFactory());

        handleInput();

        updateExpression();

//        boolean changing = rendererContext.containsResetAnimations();
//        if (changing)
//            setRefresh();

        renderImage(batch);

    }

    float panSpeed = 0.5f;
    float maxTimestep = 5.0f/Gdx.graphics.getDisplayMode().refreshRate;

    private void handleInput() {

        if (!isFocused)
            return;

        float deltaTime = Gdx.graphics.getDeltaTime();
        if (deltaTime > maxTimestep && !Gdx.input.isKeyPressed(Input.Keys.F))
            deltaTime = maxTimestep;

        int multX = 0;
        int multY = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A))
            multX++;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D))
            multX--;
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W))
            multY--;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S))
            multY++;

        float currentPanSpeed = this.panSpeed;
        if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))
            currentPanSpeed *= 2.0f;

        move(multX* currentPanSpeed * deltaTime * getHeight(), multY* currentPanSpeed * deltaTime *getHeight(), 0.5f);

        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            rendererContext.addPathPoint(systemContext.getNumberFactory().createComplexNumber(getReal(mouseX), getImag(mouseY)), systemContext.getNumberFactory());
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)){
            rendererContext.getParameterAnimations().forEach(a -> a.setPaused(true));
            rendererContext.clearPath();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)){
            ParamInterpolation interp = AnimationsUI.getSelectedInterpolation();
            List controlPoints = new ArrayList<>(interp.getControlPoints(true));
            for (ComplexNumber selected : selectedControlPoints){
                int i = 0;
                for (Object cp : controlPoints){
                    if (selected.equals(cp)){
                        interp.removeControlPoint(i);
                        if (selected.equals(movingControlPoint))
                            movingControlPoint = null;
                    } else {
                        i++;
                    }
                }
            }
            selectedControlPoints.clear();
        }
    }

    String lastCondition = "";
    OrbittrapContainer lastOrbittrapContainer = null;

    public void updateExpression(){
//        String expr =
//                "cos(z)^2+c"
//                "absr(negatei(z))^2+c"
//                ;
        String newExpressionString = (String) systemContext.getParamValue("f(z)=", String.class);

        String currentCondition = (String) systemContext.getParamValue("condition");
        boolean conditionChanged = !currentCondition.equals(lastCondition);
        lastCondition = currentCondition;

        ParamSupplier otSupp = systemContext.getParamContainer().getClientParameter(GPUSystemContext.PARAMNAME_ORBITTRAPS);
        OrbittrapContainer cont = null;
        boolean trapsChanged = false;
        if (otSupp != null){
            cont = otSupp.getGeneral(OrbittrapContainer.class);
            if (lastOrbittrapContainer != null && !cont.equals(lastOrbittrapContainer))
                trapsChanged = true;
        }

        boolean update = expressionString == null || !newExpressionString.equals(expressionString) || paramsChanged || conditionChanged || trapsChanged;
        if (!update){
            for (ParamSupplier supp : systemContext.getParamContainer().getParameters()){
                if (supp.isChanged()){
                    update = true;
                    break;
                }
            }
        }

        if (update) {
            expressionString = newExpressionString;
            try {
                ComputeExpression newExpression = new ComputeExpressionBuilder(expressionString, "z", systemContext.getParameters()).getComputeExpression();
                boolean expressionChanged = !newExpression.equals(expression);
                expression = newExpression;
                if (expressionChanged || conditionChanged || (trapsChanged && (cont == null || cont.needsShaderRecompilation(lastOrbittrapContainer)))) {
                    shaderBuilder = new ShaderBuilder(expression, systemContext);
                    compileComputeShader();
                }
//                if (paramsChanged || expressionChanged || conditionChanged)
//                    setRefresh();
                if (paramsChanged || expressionChanged || conditionChanged || trapsChanged)
                    reset();
            } catch (IllegalArgumentException e){
                LOG.info("Couldn't parse expression: \n"+e.getMessage());
            }
        }

        lastOrbittrapContainer = cont.copy();
        paramsChanged = false;
    }

    private void setShaderUniforms() {
        float currentWidth = getWidth();
        float currentHeight = getHeight();

//        setColoringParams();

        ParamContainer paramContainer = systemContext.getParamContainer();

        computeShader.setUniformMatrix("u_projTrans", matrix);
        computeShader.setUniformf("ratio", currentWidth/(float)currentHeight);
        computeShader.setUniformf("resolution", getScaledWidth(), getScaledHeight());

//		long t = System.currentTimeMillis();
//		if (t-lastIncrease > 1) {
//			lastIncrease = t;
//			iterations++;
//		}
        computeShader.setUniformf("iterations", (float)paramContainer.getClientParameter("iterations").getGeneral(Integer.class));
        computeShader.setUniformf("limit", (float)paramContainer.getClientParameter("limit").getGeneral(Number.class).toDouble());
        Integer frameSamples = paramContainer.getClientParameter(GPUSystemContext.PARAMNAME_SAMPLESPERFRAME).getGeneral(Integer.class);
        if (frameSamples < 1)
            frameSamples = 1;
        computeShader.setUniformf("maxSamplesPerFrame", (float) frameSamples);

        double scale = paramContainer.getClientParameter("zoom").getGeneral(Number.class).toDouble();
        ComplexNumber midpoint = systemContext.getMidpoint();

        List<ParamSupplier> paramList = expression.getParameterList();
        float[] params = new float[paramList.size()*3];
        for (int i = 0 ; i <  paramList.size() ; i++){
            ParamSupplier supp = paramList.get(i);
            if (supp instanceof StaticParamSupplier){
                ComplexNumber val = (ComplexNumber)((StaticParamSupplier)supp).getObj();
                params[i*3] = (float) val.realDouble();
                params[i*3+1] = (float) val.imagDouble();
                params[i*3+2] = 0;
            }
            else if (supp instanceof CoordinateBasicShiftParamSupplier){
//                CoordinateBasicShiftParamSupplier shiftSupp = (CoordinateBasicShiftParamSupplier) supp;
                params[i*3] = (float)midpoint.realDouble();
                params[i*3+1] = (float)midpoint.imagDouble();
                params[i*3+2] = (float)scale;
            }
            else
                throw new IllegalArgumentException("Unsupported ParamSupplier "+supp.getName()+": "+supp.getClass().getName());
        }
        computeShader.setUniform1fv("params", params, 0, params.length);

        //TODO remove instance variable
        computeShader.setUniformf("scale", (float)scale);

//        ComplexNumber c = paramContainer.getClientParameter("c").getGeneral(ComplexNumber.class);
        double centerR = midpoint.getReal().toDouble();
        double centerI = midpoint.getImag().toDouble();
        computeShader.setUniformf("center", (float) centerR, (float) centerI);
        float centerFp64LowR = (float) (centerR - (float) centerR);
        float centerFp64LowI = (float) (centerI - (float) centerI);
        computeShader.setUniformf("centerFp64Low", centerFp64LowR, centerFp64LowI);
        computeShader.setUniformf("logPow", (float)expression.getSmoothstepConstant());
        computeShader.setUniformf("maxBorderSamples", paramContainer.getClientParameter(GPUSystemContext.PARAMNAME_MAXBORDERSAMPLES).getGeneral(Integer.class));
        computeShader.setUniformi("sampleCountRoot", paramContainer.getClientParameter(GPUSystemContext.PARAMNAME_SUPERSAMPLING).getGeneral(Integer.class));

        shaderBuilder.setUniforms(computeShader);
    }


    protected void setColoringParams(){
//        coloringShader.setUniformf("resolution", width, height);
        coloringShader.setUniformf("samples", 1f);
        coloringShader.setUniformf("colorShift", colorShift);
        //			for (int i = 0 ; i < currentRefreshes ; i++){
        //				decode_factor += (byte)(1f/(i+1));
        //			}
        coloringShader.setUniformf("decode_factor", 1f);

//        ComplexNumber anchor = systemContext.getMidpoint();
//        ComplexNumber anchor = systemContext.getNumberFactory().createComplexNumber(0,0);
        super.setColoringParams(coloringShader, resolutionX, resolutionY, (MainStage)getStage(), getSystemContext(), getRendererContext());
    }

    protected void setResolutionScale(double resolutionScale){
        if (this.resolutionScale != resolutionScale && resolutionScale > 0) {
            this.resolutionScale = resolutionScale;
            sizeChanged();
        }
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        this.width = (int)getWidth();
        this.height = (int)getHeight();
        this.resolutionX = (int)Math.ceil(getScaledWidth());
        this.resolutionY = (int)Math.ceil(getScaledHeight());
        ((GPUSystemContext)this.systemContext).updateSize(resolutionX, resolutionY);
        resetFramebuffers();
        setRefresh();
    }

    private void resetFramebuffers() {
        disposeFramebuffers();

        GLFrameBuffer.FrameBufferBuilder fbb = new GLFrameBuffer.FrameBufferBuilder(resolutionX, resolutionY);
        fbb.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888);
        int glFormat = Pixmap.Format.toGlFormat(Pixmap.Format.RGBA8888);
        int glType = Pixmap.Format.toGlType(Pixmap.Format.RGBA8888);
        fbb.addColorTextureAttachment(glFormat, glFormat, glType);

        fboDataFirst = new FrameBuffer(fbb){};
        fboDataSecond = new FractalsFrameBuffer(fbb);
        fboImage = new FrameBuffer(Pixmap.Format.RGBA8888, resolutionX, resolutionY, false);

//        resetDebugFramebuffers();

//        fboDataFirst = new FrameBuffer(Pixmap.Format.RGBA8888, resolutionX, resolutionY, false);
//        fboDataSecond = new FrameBuffer(Pixmap.Format.RGBA8888, resolutionX, resolutionY, false);
//        fboSampleCount = new FrameBuffer(Pixmap.Format.RGBA8888, resolutionX, resolutionY, false);
    }

    private void disposeFramebuffers() {
        renderedPart = false;
        if (fboDataFirst != null) {
            fboDataFirst.dispose();
            fboDataSecond.dispose();
            fboImage.dispose();
        }
    }

    private boolean isRenderingDone(){

        //TODO check if rendering done
        return ((Integer)systemContext.getParamValue(GPUSystemContext.PARAMNAME_SUPERSAMPLING)) == 1;

//        Texture samplesTexture = getFboDataPrevious().getTextureAttachments().get(1);
//        if (!samplesTexture.getTextureData().isPrepared()) {
//            samplesTexture.getTextureData().prepare();
//        }
//        Pixmap pixmap = samplesTexture.getTextureData().consumePixmap();
//        int w = pixmap.getWidth();
//        int h = pixmap.getHeight();
//        int maxSamples = ((Integer)systemContext.getParamValue(GPUSystemContext.PARAMNAME_SUPERSAMPLING))^2;
//        for (int x = 0 ; x < w ; x++){
//            int samples0 = pixmapPixelToInt(pixmap, x, 0);
//            int samples1 = pixmapPixelToInt(pixmap, x, h);
//            if (samples0 < maxSamples || samples1 < maxSamples)
//                return false;
//        }
//        for (int y = 0 ; y < h ; y++){
//            int samples0 = pixmapPixelToInt(pixmap, 0, y);
//            int samples1 = pixmapPixelToInt(pixmap, w, y);
//            if (samples0 < maxSamples || samples1 < maxSamples)
//                return false;
//        }
//        return true;
    }

    private int pixmapPixelToInt(Pixmap pixmap, int x, int y) {
        return 0;
    }

    protected void renderImage(Batch batch) {

        long t1 = System.nanoTime();

        setResolutionScale((double)systemContext.getParamValue(GPUSystemContext.PARAMNAME_RESOLUTIONSCALE, Double.class));
        float resolutionScaleF = (float) this.resolutionScale;

        Gdx.gl.glClearColor( 0, 0, 0, 1 );

        updateMousePos();

        //apply current renderer resolution to batch
        Matrix4 matrix = new Matrix4();
        matrix.setToOrtho2D(0, 0, (float)(getScaledWidth()), (float)(getScaledHeight()));
        batch.setProjectionMatrix(matrix);

        //determine if existing pixels can be reused
        boolean reusePixels = reuseEnabled && renderedPart && !prevDataFboOutdated;
        float shiftedX = ((int)(pannedDeltaX*resolutionScaleF))/resolutionScaleF;
        float shiftedY = ((int)(pannedDeltaY*resolutionScaleF))/resolutionScaleF;
        pannedDeltaX = pannedDeltaX-shiftedX;
        pannedDeltaY = pannedDeltaY-shiftedY;

        //alternate between two data fbos on refresh
        FrameBuffer fboDataCurr = getFboDataCurrent();
        FrameBuffer fboDataPrev = getFboDataPrevious();

        batch.end();

//        fboSampleCount.begin();
//        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
//        fboSampleCount.end();

        //calculate (and reuse) data and store in current data fbo
        fboDataCurr.begin();

        long t2 = System.nanoTime();

        if (refresh) {

            if (reusePixels && isRenderingDone())
                refresh = false;
            renderedPart = true;

//            if (getId() == 1)
//                LOG.warn("render with framebuffers "+(useFirstDataFbo ? 1 : 0)+" "+(useFirstDataFbo ? 0 : 1)+" prev outdated="+ prevDataFboOutdated);
            //clear current data fbo

            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            computeShader.begin();
            setShaderUniforms();
//            fboSampleCount.getColorBufferTexture().bind(1);
//            fboDataCurr.bind();

            Texture computeBufferTexture = fboDataPrev.getColorBufferTexture();
            computeBufferTexture.bind(0);

            Texture samplesTexture = fboDataPrev.getTextureAttachments().get(1);
            samplesTexture.bind(1);

//            currTexture.bind(0);
            computeShader.setUniformi("u_texture", 0);
            computeShader.setUniformi("samplesTexture", 1);
            computeShader.setUniformf("bufferOffset", (float)Math.round(shiftedX* resolutionScaleF), (float)Math.round(shiftedY* resolutionScaleF));
            computeShader.setUniformi("discardBuffer", !reusePixels || prevDataFboOutdated  ? 1 : 0);

//            if (reusePixels)
                prevDataFboOutdated = false;

            batch.begin();
            batch.setShader(computeShader);

            Color c = batch.getColor();
            batch.setColor(c.r, c.g, c.b, 1.0f);

            Texture tex = samplesTexture;
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            TextureRegion texReg = new TextureRegion(tex);
//            if (refresh)
//                texReg.flip(false, true);
            batch.draw(texReg, 0, 0);
            batch.end();

            computeShader.end();

//            batch.begin();
////            passthroughShader.begin();
////            batch.setShader(passthroughShader);
////            debugColorTexture.bind(0);
////            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
//            batch.draw(debugColorTexture, 1, 1, fboDataCurr.getWidth(), fboDataCurr.getHeight());
//            batch.end();
////            passthroughShader.end();

            fboDataCurr.end();
            useFirstDataFbo = !useFirstDataFbo;

//            int width = fboImage.getWidth();
//            int sampleAmount = 10;
//            int step = 4;
//            int step = 4*(height/sampleAmount);
//            byte[] pixels = ScreenUtils.getFrameBufferPixels((int)0, (int)0, 1, height, false);
//            for (int i = 0 ; i < sampleAmount ; i++) {
//                int offset = i * step;
//                byte[] debugColorOutputData = new byte[]{pixels[offset], pixels[offset+1], pixels[offset+2]};
//                float debugColorOutputValue = decodeV3(pixels, offset);
//                LOG.warn("decodedValue["+i+"]: " + debugColorOutputValue+" ("+ Arrays.toString(debugColorInputData)+") -> ("+Arrays.toString(debugColorOutputData)+")");
//            }
        }

        long t3 = System.nanoTime();

//        debugColorEncoding();

        //Pass 2: render fboData content to fboImage using coloringShader
        fboImage.begin();

//            if (refresh)
//                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        coloringShader.begin();

//            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE1);
//            fboImage.bind();
//            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
//            coloringShader.setUniformi("u_texture", 1);

        setColoringParams();

        batch.setShader(coloringShader);
//        batch.setShader(passthroughShader);
        Texture dataTexture = getFboDataPrevious().getColorBufferTexture();
        Texture palette = ((MainStage)getStage()).getPaletteTexture();
        if (palette != null) {
            palette.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            palette.bind(1);
        }
        dataTexture.bind(0);
//            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        coloringShader.setUniformi("u_texture", 0);
        coloringShader.setUniformi("palette", 1);
        batch.begin();

        batch.draw(dataTexture, 0, 0, fboImage.getWidth(), fboImage.getHeight());

//			batch.disableBlending();
        batch.end();

        coloringShader.end();

        fboImage.end();

        long t4 = System.nanoTime();


//        }

//        matrix.setToOrtho2D(0, 0, getWidth(), getHeight()); // here is the actual size you want
        matrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); // here is the actual size you want
        batch.setProjectionMatrix(matrix);

//        drawPalette(batch);

        batch.begin();
        batch.setShader(passthroughShader);
        Texture tex2 = fboImage.getColorBufferTexture();
        TextureRegion texReg2 = new TextureRegion(tex2, 0, 0, (int) fboImage.getWidth(), (int) fboImage.getHeight());
        texReg2.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        texReg2.flip(false, true);
        batch.draw(texReg2, getX(), getY(), getWidth(), getHeight());

        debugDrawFBOs(batch);

        batch.flush();

        long t5 = System.nanoTime();

        ParamContainer clientParams = ((MainStage)getStage()).getClientParameters();
        boolean drawPath =      clientParams.getClientParameter(MainStage.PARAMS_DRAW_PATH).getGeneral(Boolean.class);
        boolean drawAxis =      clientParams.getClientParameter(MainStage.PARAMS_DRAW_AXIS).getGeneral(Boolean.class);
        boolean drawMidpoint =  clientParams.getClientParameter(MainStage.PARAMS_DRAW_MIDPOINT).getGeneral(Boolean.class);
        boolean drawOrigin =    clientParams.getClientParameter(MainStage.PARAMS_DRAW_ZERO).getGeneral(Boolean.class);
        boolean pathVisible = drawPath && rendererContext.getSelectedParamInterpolation() != null;

        if (drawTraces || pathVisible || drawAxis || drawMidpoint || drawOrigin) {
            batch.end();

            drawShapes(batch);

            batch.begin();
        }

        long t6 = System.nanoTime();

        updateProgressiveRendering();
        if (isScreenshot(true) && isProgressiveRenderingFinished()) {
            makeScreenshot();
        }

        long t7 = System.nanoTime();

        long t_setup = t2-t1;
        long t_refresh = t3-t2;
        long t_combine = t4-t3;
        long t_color = t5-t4;
        long t_resample = t6-t5;
        long t_shapes = t7-t6;
        double t_total = t7-t1;

        double warn_limit = 40.0/NumberUtil.NS_TO_MS;

        if (t_total > warn_limit) {

            double r_setup = t_setup / t_total;
            double r_refresh = t_refresh / t_total;
            double r_combine = t_combine / t_total;
            double r_color = t_color / t_total;
            double r_resample = t_resample / t_total;
            double r_shapes = t_shapes / t_total;

            double factor = 1000.;
            int prec = 4;
            String tf_setup = NumberUtil.getTimeInS(t_setup, prec) * factor + "";
            String tf_refresh = NumberUtil.getTimeInS(t_refresh, prec) * factor + "";
            String tf_combine = NumberUtil.getTimeInS(t_combine, prec) * factor + "";
            String tf_color = NumberUtil.getTimeInS(t_color, prec) * factor + "";
            String tf_resample = NumberUtil.getTimeInS(t_resample, prec) * factor + "";
            String tf_shapes = NumberUtil.getTimeInS(t_shapes, prec) * factor + "";
            String tf_total = NumberUtil.getTimeInS((long) t_total, prec) * factor + "";

            String s1 = "setup";
            String s2 = "refresh";
            String s3 = "combine";
            String s4 = "color";
            String s5 = "resample";
            String s6 = "shapes";

            LOG.warn("renderer " + id + " took " + tf_total + "ms  "
                    + s1 + ": " + tf_setup + " "
                    + s2 + ": " + tf_refresh + " "
                    + s3 + ": " + tf_combine + " "
                    + s4 + ": " + tf_color + " "
                    + s5 + ": " + tf_resample + " "
                    + s6 + ": " + tf_shapes
            );
        }

//        matrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); // here is the actual size you want
//        batch.setProjectionMatrix(matrix);

//        batch.draw(texReg2, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//        batch.end();
//        super.draw(batch, parentAlpha);
    }

    private boolean isProgressiveRenderingFinished(){
        return progressiveRenderingMissingFrames == 0;
    }

    private void updateProgressiveRendering(){
        int newSampleCount = (Integer) systemContext.getParamValue(GPUSystemContext.PARAMNAME_SAMPLESPERFRAME, Integer.class);
        progressiveRenderingMissingFrames = progressiveRenderingMissingFrames - newSampleCount;
        if (progressiveRenderingMissingFrames < 0)
            progressiveRenderingMissingFrames = 0;
    }

    private void resetProgressiveRendering(){
        int samples = (Integer) systemContext.getParamValue(GPUSystemContext.PARAMNAME_SUPERSAMPLING, Integer.class);
        progressiveRenderingMissingFrames = samples;
    }

    private FrameBuffer getFboDataPrevious() {
        return useFirstDataFbo ? fboDataSecond : fboDataFirst;
    }

    private FrameBuffer getFboDataCurrent() {
        return useFirstDataFbo ? fboDataFirst : fboDataSecond;
    }

    byte[] encodeV3(float value){
        int exponent = (int) (Math.log(Math.abs(value))/Math.log(2));
        double exponentScaling = Math.pow(2, exponent);
        value /= exponentScaling;
        byte[] encoded = new byte[3];
        value = value % 1f;
        encoded[0] = (byte)(value*256f+128f);
        encoded[1] = (byte)((value-(encoded[0]-128f)/256f)*256f+128f);
        encoded[2] = (byte) (exponent);
        return encoded;
    }

    float decodeV3(byte[] encoded, int offset){
        float mantissa = decodeMantissaV2(encoded, offset);
        int exponent = decodeExponent(encoded, offset+2);
        float exponentScaling = (float) Math.pow(2, exponent);
        return mantissa * exponentScaling;
    }

    private float decodeMantissaV2(byte[] encoded, int offset) {
        float mantissa = 1f;
        mantissa += (encoded[0+offset]+128)/256f;
//        mantissa += (encoded[1+offset]-128f)/(256f*256f);
        return mantissa;
    }

    private int decodeExponent(byte[] encoded, int offset) {
        return (int) (encoded[offset]);
    }

    //
    // Debug
    //

    Texture debugColorTexture = null;
    float initDebugColorInputValue = 1f;
    float debugColorInputValue = initDebugColorInputValue;
    byte[] debugColorInputData;
    int debugWidth = 1280;
    int debugHeight = 720;
//    int debugWidth = 10;
//    int debugHeight = 10;

    protected void resetDebugFramebuffers() {
        Pixmap debugColorPixmap = new Pixmap(debugWidth, debugHeight, Pixmap.Format.RGBA8888);
        for (int y = 0 ; y < debugColorPixmap.getHeight() ; y++){
            for (int x = 0 ; x < debugColorPixmap.getWidth() ; x++){
//                debugColorInputData = encodeV3(debugColorInputValue);
                float debugValue = getDebugValue(x, y, debugColorPixmap.getWidth());
                debugColorInputData = encodeV3(debugValue);
//                LOG.warn("encode "+debugValue+" as "+Arrays.toString(debugColorInputData)+" -> "+decodeV3(debugColorInputData, 0));
//                debugColorInputValue *= 1.0001;
//                debugColorInputValue += .0039062/10;
                debugColorInputValue += 1;
                debugColorPixmap.setColor(debugColorInputData[0], debugColorInputData[1], debugColorInputData[2], 1f);
                debugColorPixmap.drawPixel(x,y);
            }
        }
        debugColorInputValue = initDebugColorInputValue;
        debugColorTexture = new Texture(debugColorPixmap);
    }

    private float getDebugValue(int x, int y, int width){
        return (x+y*width)*0.0001f;
    }

//    float debugLastMousePosX = 0;
//    float debugLastMousePosY = 0;
    float debugLastDecodedValue = 0;

    private void debugColorEncoding(){
        if (!isFocused)
            return;
//        float value = 2.5f;
//        LOG.warn(value+" -> "+decodeV3(encodeV3(value)));
//        LOG.warn(decodeV3(new byte[]{(byte)127, (byte)127, (byte)127})+"");
        int posX = (int)(mouseX-getX());
        int posY = (int)(Gdx.graphics.getHeight()-mouseY-getY());
//        byte[] pixels = ScreenUtils.getFrameBufferPixels(posX, posY, 1, 1, false);
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, (int)getWidth(), (int)getHeight(), false);
        int offset = ((int)(posX+posY*getWidth()))*4;
        if (offset < 0 || offset > pixels.length || posX < 0 || posX > getWidth())
            return;
        float mantissa = decodeMantissaV2(pixels, offset);
        int exponent = 128+decodeExponent(pixels, offset+2);
        float exponentScaling = (float) Math.pow(2, exponent);
        float decodedValue = mantissa * exponentScaling;
        float expectedValue = getDebugValue((int) (debugWidth * posX / getWidth()), (int) (debugHeight * posY / getHeight()), debugWidth);
        int expectedExponent = (int)(Math.log(expectedValue)/Math.log(2));
        float expectedMantissa = expectedValue/(float)Math.pow(2, expectedExponent);
        byte[] expectedColor = encodeV3(expectedValue);
//        if (mouseX != debugLastMousePosX || mouseY != debugLastMousePosY)
        if (decodedValue != debugLastDecodedValue)
            LOG.warn("r="+(pixels[offset])+" g="+(pixels[offset+1])+" b="+(pixels[offset+2])+" value="+decodedValue+"("+mantissa+" * 2^"+exponent+")"+" expected="+ expectedValue+" ("+expectedMantissa+" * 2^"+expectedExponent+")"+" r="+(expectedColor[0])+" g="+(expectedColor[1])+" b="+(expectedColor[2])+" at "+posX+", "+posY);
        debugLastDecodedValue = decodedValue;
//        debugLastMousePosX = mouseX;
//        debugLastMousePosY = mouseY;
    }

    private void debugDrawFBOs(Batch batch) {
        Texture tex3 = fboDataFirst.getColorBufferTexture();
        TextureRegion texReg3 = new TextureRegion(tex3, 0, 0, (int) fboDataFirst.getWidth(), (int) fboDataFirst.getHeight());
        texReg3.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
//        texReg3.flip(false, true);
        batch.draw(texReg3, getX(), getY(), getWidth()/3f, getHeight()/3f);

        Array<Texture> textureAttachments = fboDataFirst.getTextureAttachments();
        if (textureAttachments.size == 1)
            return;
        Texture tex4 = textureAttachments.get(1);
////        Texture tex4 = fboSampleCount.getColorBufferTexture();
        TextureRegion texReg4 = new TextureRegion(tex4, 0, 0, (int) fboDataSecond.getWidth(), (int) fboDataSecond.getHeight());
        texReg4.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
//        texReg4.flip(false, true);
        batch.draw(texReg4, getX() + getWidth() / 3, getY(), getWidth() / 3, getHeight() / 3);
    }

    private void updateMousePos() {
        float newMouseX = Gdx.input.getX();
        float newMouseY = Gdx.input.getY();
        if (isFocused && (newMouseX != mouseX || newMouseY != mouseY))
            mousePosChanged(newMouseX, newMouseY);
    }

    private void mousePosChanged(float newMouseX, float newMouseY) {
        this.mouseX = newMouseX;
        this.mouseY = newMouseY;
    }

    ShapeRenderer shapeRenderer = null;
    int traceArrayFilledSize = 0;
    double[] tracesReal = null;
    double[] tracesImag = null;
    EscapeTimeCpuCalculatorNew traceCalculator = new EscapeTimeCpuCalculatorNew();

    List<ComplexNumber> selectedControlPoints = new ArrayList<>();
    ComplexNumber movingControlPoint = null;

    double controlPointSelectionRadius = 5.0;
    Color controlPointSelectedColor = Color.SKY;
    Color controlPointColor = Color.BLUE;

    protected void drawShapes(Batch batch) {

        ParamContainer clientParams = ((MainStage)getStage()).getClientParameters();
        boolean drawPath =              clientParams.getClientParameter(MainStage.PARAMS_DRAW_PATH).getGeneral(Boolean.class);
        boolean drawAxis =              clientParams.getClientParameter(MainStage.PARAMS_DRAW_AXIS).getGeneral(Boolean.class);
        boolean drawMidpoint =          clientParams.getClientParameter(MainStage.PARAMS_DRAW_MIDPOINT).getGeneral(Boolean.class);
        boolean drawOrigin =            clientParams.getClientParameter(MainStage.PARAMS_DRAW_ZERO).getGeneral(Boolean.class);
        float lineWidth = (float)(double)clientParams.getClientParameter(MainStage.PARAMS_TRACES_LINE_WIDTH).getGeneral(Double.class);
        float pointSize = (float)(double)clientParams.getClientParameter(MainStage.PARAMS_TRACES_POINT_SIZE).getGeneral(Double.class);
        boolean tracesEnabled =         clientParams.getClientParameter(MainStage.PARAMS_TRACES_ENABLED).getGeneral(Boolean.class);
        int traceCount =                clientParams.getClientParameter(MainStage.PARAMS_TRACES_ITERATIONS).getGeneral(Integer.class);
        boolean disabled = !drawMidpoint && !drawPath && !drawAxis && (!tracesEnabled || traceCount < 1 || (lineWidth <= 0 && pointSize <= 0));
        if (disabled)
            return;

        prepareShapeRenderer(batch);

        if (drawPath)
            drawAnimationPath();

        if (tracesEnabled && traceCount > 0) {
            updateTraceArrays();
            drawTraces(batch, clientParams, lineWidth, pointSize);
        }

        if (drawAxis){
            shapeRenderer.setColor(new Color(1f, 1f, 1f, 0.3f));
            float originX = getScreenX(0);
            float originY = getScreenY(0);
            float minX = getX();
            float maxX = getWidth() + minX;
            float minY = getY();
            float maxY = getHeight() + minY;
            shapeRenderer.line(minX, originY, maxX, originY);
            shapeRenderer.line(originX, minY, originX, maxY);
        }

        if (drawOrigin){
            shapeRenderer.setColor(Color.ROYAL);
            ComplexNumber midpoint = systemContext.getMidpoint();
            shapeRenderer.circle(getScreenX(0), getScreenY(0), 3);
        }
        if (drawMidpoint) {
            shapeRenderer.setColor(Color.RED);
            ComplexNumber midpoint = systemContext.getMidpoint();
            shapeRenderer.circle(getScreenX(midpoint.getReal().toDouble()), getScreenY(midpoint.getImag().toDouble()), 3);
        }

        closeShapeRenderer();
    }

    private void prepareShapeRenderer(Batch batch) {
        //prepare trace renderer
        if (shapeRenderer == null)
            shapeRenderer = new ShapeRenderer(64);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());

        Rectangle scissors = new Rectangle();
        Rectangle clipBounds = new Rectangle(getX(),getY(),getWidth(),getHeight());
        ScissorStack.calculateScissors(getStage().getCamera(), batch.getTransformMatrix(), clipBounds, scissors);
        ScissorStack.pushScissors(scissors);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void closeShapeRenderer() {
        shapeRenderer.end();
        ScissorStack.popScissors();
    }

    float timePassed = 0;

    private void updateTraceArrays() {

        ParamContainer clientParams = ((MainStage)getStage()).getClientParameters();
        int traceCount = clientParams.getClientParameter(MainStage.PARAMS_TRACES_ITERATIONS).getGeneral(Integer.class);

        //determine coordinates
        ComplexNumber coords = null;
        String posVarName = clientParams.getClientParameter(MainStage.PARAMS_TRACES_VARIABLE).getGeneral(String.class);
        if (posVarName.equals("path")) {
//            ParamAnimation animation = rendererContext.getSelectedPathAnimation();
//            NumberFactory numberFactory = systemContext.getNumberFactory();
//            coords = (ComplexNumber) animation.getInterpolatedValue(timePassed, numberFactory);
            coords = clientParams.getClientParameter(MainStage.PARAMS_TRACES_VALUE).getGeneral(ComplexNumber.class);
            timePassed += Gdx.graphics.getDeltaTime();
        }
        else {
            ParamSupplier posVar = systemContext.getParamContainer().getClientParameter(posVarName);
            boolean useMousePos = posVarName.equals("mouse") || posVar == null || !(posVar instanceof StaticParamSupplier) || (((StaticParamSupplier) posVar).getGeneral() instanceof ComplexNumber);
            if (useMousePos)
                coords = getComplexMapping(mouseX, mouseY);
            else {
                float periodInS = 30;
                float periodInS2 = 17;
                float time = ((timePassed / periodInS) % 1);
                float time2 = ((timePassed / periodInS2) % 1);
//            timePassed *= getWidth();

//            traceChunk.chunkPos = getComplexMapping(getWidth()-time, mouseY);

                float a = 0.2f;

                float timeAngle = (float) (time * Math.PI * 2);

//            traceChunk.chunkPos = getComplexMapping(getScreenX((2*a*(1-Math.cos(timeAngle))*Math.cos(timeAngle))),
//                                             getScreenY((2*a*(1-Math.cos(timeAngle))*Math.sin(timeAngle))));

//            float cardioidScale = (float)-Math.cos(time2*Math.PI*2)*0.2f+0.9f;
                float cardioidScale = 0.9f;

                coords = systemContext.getNumberFactory().createComplexNumber(
                        cardioidScale * ((2 * 1.25 * a * (1 - Math.cos(timeAngle)) * Math.cos(timeAngle)) + 0.25),
                        cardioidScale * (2 * 1.25 * a * (1 - Math.cos(timeAngle)) * Math.sin(timeAngle)));
//            traceChunk.chunkPos = systemContext.getNumberFactory().createComplexNumber((Math.sin(timeAngle)),
//                    (Math.cos(timeAngle)));

                timePassed += Gdx.graphics.getDeltaTime();
            }
        }


        //calculate sample on cpu and save traces
        double[][] traces = sampleCoordsOnCpu(traceCount, coords);
        if (traces.length == 2) {
            tracesReal = traces[0];
            tracesImag = traces[1];
        } else {
            tracesReal = new double[0];
            tracesImag = new double[0];
        }

        //determine last iteration
        traceArrayFilledSize = traceCount;
        for (int i = 0 ; i < traces[0].length ; i++){
            if (i > 0 && traces[0][i] == 0.0 && traces[1][i] == 0.0) {
                traceArrayFilledSize = i;
                break;
            }
        }
    }

    private double[][] sampleCoordsOnCpu(int traceCount, ComplexNumber coords) {
        //prepare calculator
        AbstractArrayChunk traceChunk = new TraceChunk();
        traceChunk.setCurrentTask(new TraceTask(systemContext));
//        BreadthFirstLayer layer = new BreadthFirstLayer().with_samples(1).with_rendering(false);
        boolean layerSet = false;
        for (Layer layer : ((LayerConfiguration)systemContext.getParamValue(GPUSystemContext.PARAMNAME_LAYER_CONFIG)).getLayers()){
            if (layer instanceof BreadthFirstLayer && !(layer instanceof BreadthFirstUpsampleLayer)){
                traceChunk.getCurrentTask().getStateInfo().setLayer(layer);
                layerSet = true;
                break;
            }
        }
        if (!layerSet)
            throw new IllegalStateException("Couldn't find applicable layer for tracing");

        traceChunk.chunkPos = coords;
        traceCalculator.setContext(systemContext);
        traceCalculator.setTraceCount(traceCount);

        //calculate traces
        traceCalculator.calculate(traceChunk, null, null);
        return traceCalculator.getTraces();
    }

    private void drawTraces(Batch batch, ParamContainer clientParams, float lineWidth, float pointSize) {

        //draw traces
        float lineTransparency = (float)(double)clientParams.getClientParameter(MainStage.PARAMS_TRACES_LINE_TRANSPARENCY).getGeneral(Double.class);
        float pointTransparency = (float)(double)clientParams.getClientParameter(MainStage.PARAMS_TRACES_POINT_TRANSPARENCY).getGeneral(Double.class);

        for (int i = 0 ; i < traceArrayFilledSize ; i++){

            float x = getScreenX(tracesReal[i]);
            float y = getScreenY(tracesImag[i]);

            //determine color
            float prog = i/(float)traceArrayFilledSize;
            float r = prog;
            float g = 1f-(prog*0.5f);
            float b = 0f;
            shapeRenderer.setColor(r, g, b, lineTransparency);

            //draw trace line
            if (i > 0 && lineWidth > 0) {
                float xPrev = getScreenX(tracesReal[i - 1]);
                float yPrev = getScreenY(tracesImag[i - 1]);

                shapeRenderer.rectLine(x, y, xPrev, yPrev, lineWidth);
            }

            //draw z_i
            if (pointSize > 0) {
                if (pointTransparency != lineTransparency)
                    shapeRenderer.setColor(r, g, b, pointTransparency);

                shapeRenderer.circle(x, y, pointSize);
            }
        }
    }

    private void drawAnimationPath() {

        ParamInterpolation interpolation = rendererContext.getSelectedParamInterpolation();
        if (interpolation == null)
            return;
        List<ComplexNumber> controlPoints = interpolation.getControlPoints(true);
        List<ComplexNumber> derivatives = interpolation.getControlDerivatives(true);
        List<ComplexNumber> path = new ArrayList<>();

        int radius = interpolation.isPathBased() ? 5 : 0;
        int lineWidth = 1;

        NumberFactory nf = systemContext.getNumberFactory();

        boolean isComplexNumber = interpolation.getParamType().equals(AnimationsUI.PARAM_TYPE_COMPLEXNUMBER);
        boolean isNumber = interpolation.getParamType().equals(AnimationsUI.PARAM_TYPE_NUMBER);

        if (interpolation.isPathBased()){
            //TODO steps for other than linear
            if (isComplexNumber) {
                for (ComplexNumber controlPoint : controlPoints) {
                    path.add(controlPoint);
                }
            } else {
                if (isNumber){
                    List<Number> controlPointsNumber = interpolation.getControlPoints(true);
                    for (Number controlPoint : controlPointsNumber) {
                        path.add(nf.createComplexNumber(controlPoint, nf.createNumber(0.0)));
                    }
                }
            }
        }
        else {
            int steps = 100;
            for (int i = 0 ; i < steps ; i++){
                double prog = (i/(steps-1.0));
                if (isComplexNumber) {
                    ComplexNumber interpolated = (ComplexNumber) interpolation.getInterpolatedValue(prog, nf);
                    path.add(interpolated);
                } else if (isNumber){
                    Number interpolated = (Number)interpolation.getInterpolatedValue(prog, nf);
                    path.add(nf.createComplexNumber(interpolated, nf.createNumber(0.0)));
                }
            }
        }

        if (path.size() == 1){
            ComplexNumber controlPoint = path.get(0);
            shapeRenderer.setColor(isControlPointSelected(controlPoint) ? controlPointSelectedColor : controlPointColor);
            float p1ScreenX = getScreenX(controlPoint.getReal().toDouble());
            float p1ScreenY = getScreenY(controlPoint.getImag().toDouble());
            shapeRenderer.circle(p1ScreenX, p1ScreenY, radius);
        }
        else {
            for (int i = 0; i < path.size() - 1; i++) {
                drawAnimationPathSegment(i == 0, radius, lineWidth, path.get(i), path.get(i + 1));
            }
        }
    }

    private boolean isControlPointSelected(ComplexNumber controlPoint) {
        for (ComplexNumber c : selectedControlPoints){
            if (c.equals(controlPoint))
                return true;
        }
        return false;
    }

    private void drawAnimationPathSegment(boolean drawFirstPoint, int radius, int lineWidth, ComplexNumber p1, ComplexNumber p2) {
        float p1ScreenX = getScreenX(p1.getReal().toDouble());
        float p1ScreenY = getScreenY(p1.getImag().toDouble());
        float p2ScreenX = getScreenX(p2.getReal().toDouble());
        float p2ScreenY = getScreenY(p2.getImag().toDouble());

        boolean p1Selected = isControlPointSelected(p1);
        boolean p2Selected = isControlPointSelected(p2); //TODO performance
        if (drawFirstPoint) {
            if (p1Selected)
                shapeRenderer.setColor(controlPointSelectedColor);
            if (radius > 0)
                shapeRenderer.circle(p1ScreenX, p1ScreenY, radius);
            if (!p2Selected)
                shapeRenderer.setColor(controlPointColor);
        }
        if (p2Selected)
            shapeRenderer.setColor(controlPointSelectedColor);
        if (radius > 0)
            shapeRenderer.circle(p2ScreenX, p2ScreenY, radius);
        if (!p1Selected || !p2Selected)
            shapeRenderer.setColor(controlPointColor);
        shapeRenderer.rectLine(p1ScreenX, p1ScreenY, p2ScreenX, p2ScreenY, lineWidth);
        if (p1Selected && p2Selected)
            shapeRenderer.setColor(controlPointColor);
    }

    @Override
    public float getScreenX(double real) {
        return getScreenX(systemContext.getNumberFactory().createNumber(real));
    }

    @Override
    public float getScreenY(double imag) {
        return getScreenY(systemContext.getNumberFactory().createNumber(imag));
    }

    @Override
    public float getScreenX(Number real) {
        Number zoom = (Number) systemContext.getParamValue("zoom");
        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue("midpoint"));

        NumberFactory nf = systemContext.getNumberFactory();
        Number res = real.copy();
        res.sub(midpoint.getReal());
        res.mult(nf.createNumber(getHeight()));
        res.div(zoom);

        res.add(nf.createNumber(getX()));
        res.add(nf.createNumber(getWidth()*0.5f));
        return (float)res.toDouble();
//        return (float) (getX() + (real-midpointReal)/(zoom/getHeight()) + getWidth()/2);
    }

    @Override
    public float getScreenY(Number imag) {
        Number zoom = (Number) systemContext.getParamValue("zoom");
        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue("midpoint"));

        NumberFactory nf = systemContext.getNumberFactory();
        Number res0 = imag.copy();
        res0.sub(midpoint.getImag());
        res0.mult(nf.createNumber(getHeight()));
        res0.div(zoom);

        Number res = nf.createNumber(getHeight());
        res.sub(res0);
        res.add(nf.createNumber(getY()));
        res.sub(nf.createNumber(getHeight()*0.5f));
        return (float)res.toDouble();
//        return (float) (getY() + getHeight()-((imag-midpointImag)/(zoom/getHeight()) + getHeight()/2));
    }

    @Override
    public ComplexNumber getComplexMapping(float screenX, float screenY){
        return systemContext.getNumberFactory().createComplexNumber(getReal(screenX), getImag(screenY));
    }

    @Override
    public Number getReal(float screenX) {

        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue("midpoint"));
        screenX -= getX()+getWidth()/2;

        NumberFactory nf = systemContext.getNumberFactory();
        Number zoomNumber = (Number) systemContext.getParamValue("zoom");
        Number resultNumber = nf.createNumber(screenX);
        Number heightNumber = nf.createNumber(getHeight());
        resultNumber.div(heightNumber);
        resultNumber.mult(zoomNumber);
        resultNumber.add(midpoint.getReal());
        return resultNumber;

//        double zoom = ((Number) systemContext.getParamValue("zoom")).toDouble();
//        double midpointReal = midpoint.realDouble();
//        return (screenX-getX()-getWidth()/2)*(zoom/getHeight())+midpointReal;
    }

    @Override
    public Number getImag(float screenY) {
        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue("midpoint"));
        screenY -= ((Gdx.graphics.getHeight()-getHeight()-getY()));
        screenY -= getHeight()/2;

        NumberFactory nf = systemContext.getNumberFactory();
        Number zoomNumber = (Number) systemContext.getParamValue("zoom");
        Number resultNumber = nf.createNumber(screenY);
        Number heightNumber = nf.createNumber(getHeight());
        resultNumber.div(heightNumber);
        resultNumber.mult(zoomNumber);
        resultNumber.add(midpoint.getImag());
        return resultNumber;

//        double zoom = ((Number) systemContext.getParamValue("zoom")).toDouble();
//        double midpointImag = midpoint.imagDouble();
//        return ((screenY-(Gdx.graphics.getHeight()-getHeight()-getY()))-getHeight()/2)*(zoom/getHeight())+midpointImag;
    }

    /** for testing the palette */
    private void drawPalette(Batch batch) {
        passthroughShader.begin();
        passthroughShader.setUniformf("scale", 5);
        passthroughShader.setUniformf("center", (float) 0, (float) 0);
        passthroughShader.setUniformf("resolution", (float) 250, (float) 250);
        if (Gdx.graphics.getWidth() > 600 && Gdx.graphics.getHeight() > 600) {
            batch.setShader(passthroughShader);
            batch.begin();
            batch.draw(((MainStage)getStage()).getPaletteTexture(), Gdx.graphics.getWidth() - 300, Gdx.graphics.getHeight() - 300, 250, 250);
            batch.end();
        }
        passthroughShader.end();
    }

    @Override
    public void setRefresh(){
        refresh = true;
        super.setRefresh();
    }

    @Override
    public double getXShift() {
        return 0;
    }

    @Override
    public double getYShift() {
        return 0;
    }

    @Override
    public SystemContext getSystemContext() {
        return systemContext;
    }

    @Override
    public void reset() {
        setRefresh();
        paramsChanged();
        resetProgressiveRendering();
        ComplexNumber midpoint = getSystemContext().getParamContainer().getClientParameter("midpoint").getGeneral(ComplexNumber.class);
        xPos = midpoint.realDouble();
        yPos = midpoint.imagDouble();
        pannedDeltaX = 0f;
        pannedDeltaY = 0f;
//        useFirstDataFbo = true;
        prevDataFboOutdated = true;
        renderedPart = false;
    }

    @Override
    public void removed() {
        if (panListener != null) {
            rendererContext.removePanListener(panListener);
            panListener = null;
        }
    }

    public void paramsChanged() {
        for (RendererLink link : rendererContext.getSourceLinks())
            link.syncTargetRenderer();
        paramsChanged = true;
        systemContext.setParameters(systemContext.getParamContainer());
        rendererContext.setParamContainer(systemContext.getParamContainer());
    }

    private static class TraceChunk extends AbstractArrayChunk {
        public TraceChunk() {
            super(null, 0, 0, 1);
        }

        @Override
        protected void removeFlag(int i) {
        }

        @Override
        public int getStartIndex() {
            return 0;
        }

        @Override
        public double getValue(int i) {
            return 0;
        }

        @Override
        public double getValue(int i, boolean b) {
            return 0;
        }

        @Override
        public void addSample(int i, double v, int i1) {
        }

        @Override
        public int getSampleCount(int i) {
            return 0;
        }

        @Override
        public int getFailedSampleCount(int i) {
            return 0;
        }
    }

    private class TraceTask implements FractalsTask {
        private final TaskStateInfo taskStateInfo;

        public TraceTask(SystemContext systemContext) {
            taskStateInfo = new TaskStateInfo(0, UUID.randomUUID(), systemContext);
            taskStateInfo.setLayer(systemContext.getLayer(0));
        }

        @Override
        public TaskManager<?> getTaskManager() {
            return null;
        }

        @Override
        public Integer getId() {
            return 0;
        }

        @Override
        public Integer getJobId() {
            return 0;
        }

        @Override
        public UUID getSystemId() {
            return null;
        }

        @Override
        public TaskStateInfo getStateInfo() {
            return taskStateInfo;
        }

        @Override
        public TaskState getState() {
            return null;
        }

        @Override
        public void setThread(CalculateFractalsThread calculateFractalsThread) {

        }

        @Override
        public FractalsCalculator getCalculator() {
            return null;
        }

        @Override
        public void run() throws InterruptedException {

        }

        @Override
        public IStats getTaskStats() {
            return null;
        }

        @Override
        public void setTaskStats(IStats iStats) {

        }

        @Override
        public void setStateInfo(TaskStateInfo taskStateInfo) {

        }

        @Override
        public SystemContext getContext() {
            return systemContext;
        }

        @Override
        public void applyLocalState(FractalsTask fractalsTask) {

        }

        @Override
        public Double getPriority() {
            return null;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}
