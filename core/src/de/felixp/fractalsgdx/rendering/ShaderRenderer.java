package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.Application;
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
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.animation.interpolations.ComplexNumberParamInterpolation;
import de.felixp.fractalsgdx.animation.interpolations.ParamInterpolation;
import de.felixp.fractalsgdx.rendering.orbittrap.OrbittrapContainer;
import de.felixp.fractalsgdx.rendering.rendererlink.RendererLink;
import de.felixp.fractalsgdx.ui.AnimationsUI;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.expressions.ChainExpression;
import de.felixperko.expressions.ComputeExpressionDomain;
import de.felixperko.expressions.FractalsExpression;
import de.felixperko.expressions.MultExpression;
import de.felixperko.expressions.VariableExpression;
import de.felixperko.fractals.data.AbstractArrayChunk;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.LayerConfiguration;
import de.felixperko.fractals.system.calculator.ComputeExpression;
import de.felixperko.fractals.system.calculator.ComputeInstruction;
import de.felixperko.fractals.system.calculator.EscapeTime.EscapeTimeCpuCalculatorNew;
import de.felixperko.fractals.system.calculator.infra.FractalsCalculator;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ExpressionsParam;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateDiscreteParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateModuloParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.statistics.IStats;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstLayer;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstUpsampleLayer;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;
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

    static boolean showDebugFBOs = false;

    int progressiveRenderingMinFramesIfFloodfill = 1;

    boolean refresh = true;
    boolean refreshColoring = false;
    boolean paramsChanged = false;
    int selectPatternIndex = 0;
    int selectPatternX = 0;
    int selectPatternY = 0;
    int selectDivs = 1;
    int selectCycleLength = selectDivs * selectDivs;

    boolean reshowLastFrameEnabled = true;
    FrameBuffer fboImageFirst;
    FrameBuffer fboImageSecond;
    boolean useFirstImageFbo = true;
    FrameBuffer fboDataFirst;
    FrameBuffer fboDataSecond;
//    FrameBuffer fboSampleCount;
    boolean useFirstDataFbo = true;
    boolean prevDataFboOutdated = false;

    List<FrameBuffer> fbosReduceRemainingSamples = new ArrayList<>();
    List<FrameBuffer> fbosDownsample = new ArrayList<>();
    List<FrameBuffer> fbosUpsample = new ArrayList<>();
    float reduceStep = 2f;
    int reduceDimThresholdCpu = 50;

    boolean reuseDataEnabled = true;
    boolean renderedPart = false;
    boolean extractMaxRemainingSamples = false;
    double extractMaxRemainingSamplesInterval = 5.0; //in s
    long extractMaxRemainingSamplesLastRefresh = 0;

    float pannedDeltaX = 0;
    float pannedDeltaY = 0;

    ShaderProgram computeShader;
    ShaderProgram coloringShader;
    ShaderProgram passthroughShader;
    ShaderProgram reduceShader;
    ShaderProgram combineShader;
    ShaderProgram selectShader;

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
    float samplesPerFrame = 1f;
    int resolutionX = 0;
    int resolutionY = 0;

    SystemContext systemContext = new ShaderSystemContext(this);
    ComplexNumber anchor = systemContext.getNumberFactory().createComplexNumber(0,0);

    ComputeExpression expression;
    ExpressionsParam expressionsParam;

    ShaderBuilder shaderBuilder;

    float mouseX, mouseY;

    PanListener panListener;

    float progressiveRenderingMissingFrames;

    float panSpeed = 0.5f;
    float maxTimestep = 5.0f/Gdx.graphics.getDisplayMode().refreshRate;

    boolean shaderCompilationFailed = false;

    boolean highPrecisionUnsupported = Gdx.app.getType().equals(Application.ApplicationType.Android);

    double maxSamplingDifference = 1.0;
    double maxSampleDifferenceMaxThrottleFactor = 10.0;

    public ShaderRenderer(RendererContext rendererContext){
        super(rendererContext);
        rendererContext.setRenderer(this);
    }

    @Override
    protected String processShadertemplateLine(String templateLine) {
        return shaderBuilder.processShadertemplateLine(templateLine, getScaledHeight(), isNewtonFractalEnabled());
    }

    @Override
    public void init(){

        ((ShaderSystemContext)systemContext).init();
        updateExpression();
        compileShaders();

        ShaderRenderer thisRenderer = this;

        addListener(new ActorGestureListener(){
            @Override
            public void tap(InputEvent event, float x, float y, int count, int button) {

                boolean clickedControlPoint = handleClickControlPoints(x, y, button);
                if (clickedControlPoint)
                    return;

                if (Gdx.app.getType() == Application.ApplicationType.Android)
                    return;

                Number factor = null;
                NumberFactory nf = systemContext.getNumberFactory();
                if (button == Input.Buttons.LEFT) {
                    factor = nf.createNumber(0.5);
                } else if (button == Input.Buttons.RIGHT) {
                    factor = nf.createNumber(2);
                }
                if (factor != null)
                    thisRenderer.zoom(factor);
            }

            @Override
            public void touchDown(InputEvent event, float x, float y, int pointer, int button) {
                getStage().setKeyboardFocus(thisRenderer);
                ((MainStage)getStage()).setFocusedRenderer(thisRenderer);
                ParamInterpolation selectedInterpolation = AnimationsUI.getSelectedInterpolation();
                if (selectedInterpolation instanceof ComplexNumberParamInterpolation){
                    List<ComplexNumber> controlPoints = selectedInterpolation.getControlPoints(true);
                    for (ComplexNumber controlPoint : controlPoints){
                        if (controlPointCollision(x, y, controlPoint) && isControlPointSelected(controlPoint)) {
                            movingControlPoint = controlPoint;
                            return;
                        }
                    }
                }
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
                boolean movedControlPoint = dragMovingControlPoint(x, y);
                if (!movedControlPoint) {
                    //if android -> get delta specifically for last multitouch input
                    if (Gdx.app.getType() == Application.ApplicationType.Android){
                        for (int i = 0 ; i < Gdx.input.getMaxPointers() ; i++){
                            if (Gdx.input.isTouched(i)) {
                                deltaX = Gdx.input.getDeltaX(i);
                                deltaY = -Gdx.input.getDeltaY(i);
                            }
                        }
                    }
                    move(deltaX, deltaY, 0.1f);
                }
            }

            float lastZoomInitialDistance = -1;

            @Override
            public void zoom(InputEvent event, float initialDistance, float distance) {
                if (Gdx.app.getType() == Application.ApplicationType.Android){
                    if (Gdx.input.isTouched(2))
                        return;
                    if (initialDistance == lastZoomInitialDistance)
                        return;
                    NumberFactory nf = thisRenderer.getSystemContext().getNumberFactory();
                    if (distance > initialDistance*1.5) {
                        lastZoomInitialDistance = initialDistance;
                        thisRenderer.zoom(nf.createNumber(0.5));
                    }
                    else if (distance < initialDistance*0.75){
                        lastZoomInitialDistance = initialDistance;
                        thisRenderer.zoom(nf.createNumber(2.0));
                    }
                }
            }

            @Override
            public boolean longPress(Actor actor, float x, float y) {
                boolean left = Gdx.input.isButtonPressed(0);
                boolean right = Gdx.input.isButtonPressed(1);
                boolean middle = Gdx.input.isButtonPressed(2);
//                if (left && Gdx.app.getType() == Application.ApplicationType.Android) {
//                    NumberFactory nf = systemContext.getNumberFactory();
//                    thisRenderer.zoom(nf.createNumber(2));
//                    return true;
//                }
                return super.longPress(actor, x, y);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                movingControlPoint = null;
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

    protected void zoom(Number factor) {
        ParamContainer paramContainer = systemContext.getParamContainer();
        Number zoom = paramContainer.getParam(ShaderSystemContext.PARAM_ZOOM).getGeneral(Number.class);

        zoom.mult(factor);
        paramsChanged(paramContainer);
        anchor = paramContainer.getParam(CommonFractalParameters.PARAM_MIDPOINT).getGeneral(ComplexNumber.class);
        reset();
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
        return (float) Math.ceil(getWidth()*getResolutionScale(true));
    }

    float getScaledHeight(){
        return (float) Math.ceil(getHeight()*getResolutionScale(true));
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
        //TODO fit pixel to pixel doesn't always work correctly
        float resScale = getResolutionScale(true);
        if (Math.abs(deltaX-Math.round(deltaX))*resScale < roundMaxDelta)
            deltaX = (float)(Math.round(deltaX*resScale)/resScale);
        if (Math.abs(deltaY-Math.round(deltaY))*resScale < roundMaxDelta)
            deltaY = (float)(Math.round(deltaY*resScale)/resScale);
        panPartOffsetX = requestedDeltaX-deltaX;
        panPartOffsetY = requestedDeltaY-deltaY;

        ParamContainer paramContainer = systemContext.getParamContainer();
        NumberFactory nf = systemContext.getNumberFactory();
        Number zoom = paramContainer.getParam(ShaderSystemContext.PARAM_ZOOM).getGeneral(Number.class);
        ComplexNumber midpoint = paramContainer.getParam(CommonFractalParameters.PARAM_MIDPOINT).getGeneral(ComplexNumber.class);
        ComplexNumber delta = nf.createComplexNumber(deltaX, -deltaY);
        delta.divNumber(nf.createNumber(getHeight()));
        delta.multNumber(zoom);
        midpoint.sub(delta);
        systemContext.setMidpoint(midpoint);
        rendererContext.panned(systemContext.getParamContainer());
        resetProgressiveRendering();
        //see panned listener added at the end of initRenderer()...
    }

    boolean paramsChangedCalled = false;
    public void paramsChanged(ParamContainer paramContainer) {
        if (isFocused && !paramsChangedCalled) {
            paramsChangedCalled = true;
            ((MainStage) FractalsGdxMain.stage).getParamUI().setServerParameterConfiguration(this, paramContainer, ((ShaderSystemContext) systemContext).paramConfig);
            paramsChangedCalled = false;
        }
    }

    public void compileShaders() {
        compileComputeShader(false);
        coloringShader = compileShader(vertexPassthrough, shader2);
        passthroughShader = compileShader(vertexPassthrough, "PassthroughFragment.glsl");
        selectShader = compileShader(vertexPassthrough, "DepthSelectionFragment.glsl");
        reduceShader = compileShader(vertexPassthrough, "ReduceFragment.glsl");
        combineShader = compileShader(vertexPassthrough, "CombineFragment.glsl");
        renderedPart = false;
//        width = Gdx.graphics.getWidth();
//        height = Gdx.graphics.getHeight();
    }

    public void compileComputeShader(boolean changedPrecision) {
        if (computeShader != null)
            computeShader.dispose();
        ShaderProgram.pedantic = false;
        try {
            ShaderProgram newComputeShader = compileShader(vertexPassthrough, shader1);
            this.computeShader = newComputeShader;
            shaderCompilationFailed = false;
        } catch (IllegalStateException e){
            //TODO indicate failed shader compilation
            shaderCompilationFailed = true;
            if (changedPrecision){
                highPrecisionUnsupported = true;
                systemContext.getParamContainer().addParam(new StaticParamSupplier(ShaderSystemContext.PARAM_PRECISION, lastPrecision));
                shaderBuilder.setPrecision(getActivePrecision());
                compileComputeShader(false);
            }
            e.printStackTrace();
        }
    }

    protected boolean isNewtonFractalEnabled(){
        return systemContext.getParamContainer().getParam(ShaderSystemContext.PARAM_CALCULATOR).getGeneral(String.class).equals(ShaderSystemContext.TEXT_CALCULATOR_NEWTONFRACTAL);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (fboDataFirst == null) //initRenderer() not called
            return;

        if (isProgressiveRenderingFinished())
            applyParameterAnimations(systemContext.getParamContainer(), ((MainStage)FractalsGdxMain.stage).getClientParams(), systemContext.getNumberFactory());

        handleInput();

        updateExpression();

//        boolean changing = rendererContext.containsResetAnimations();
//        if (changing)
//            setRefresh();

        renderImage(batch);

    }

    private void handleInput() {

        boolean alt = Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT);
        boolean ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        if (alt || shift) {
            progressiveRenderingMissingFrames = Math.max(1, progressiveRenderingMissingFrames);
//            resetProgressiveRendering();
            setRefresh();
        }

        if (!isFocused) {
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.B))
            showDebugFBOs = !showDebugFBOs;

        float deltaTime = Gdx.graphics.getDeltaTime();
        if (deltaTime > maxTimestep && !Gdx.input.isKeyPressed(Input.Keys.F)) {
            deltaTime = maxTimestep;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP)) {
            zoom(systemContext.getNumberFactory().createNumber("0.5"));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN)) {
            zoom(systemContext.getNumberFactory().createNumber("2.0"));
        }


        int panMultX = 0;
        int panMultY = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A))
            panMultX++;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D))
            panMultX--;
//        if (!alt) {
            if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W))
                panMultY--;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S))
                panMultY++;
//        } else {
//            if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W))
//                zoom(systemContext.getNumberFactory().createNumber("0.5"));
//            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S))
//                zoom(systemContext.getNumberFactory().createNumber("2.0"));
//        }

//        if (Gdx.input.isKeyJustPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.ALT_RIGHT))
//            zoom(systemContext.getNumberFactory().createNumber("2.0"));


        float currentPanSpeed = this.panSpeed;
        if (shift)
            currentPanSpeed *= 2.0f;

        move(panMultX* currentPanSpeed * deltaTime * getHeight(), panMultY* currentPanSpeed * deltaTime *getHeight(), 0.5f);


        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            zoom(systemContext.getNumberFactory().createNumber(ctrl ? "2.0" : "0.5"));
        }


        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            rendererContext.addPathPoint(systemContext.getNumberFactory().createComplexNumber(getReal(mouseX), getImag(mouseY)), systemContext.getNumberFactory());
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)){
            rendererContext.getParameterAnimations().forEach(a -> a.setPaused(true));
            rendererContext.clearPath();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)){
            ParamInterpolation interp = AnimationsUI.getSelectedInterpolation();
            if (interp != null) {
                List controlPoints = new ArrayList<>(interp.getControlPoints(true));
                for (ComplexNumber selected : selectedControlPoints) {
                    int i = 0;
                    for (Object cp : controlPoints) {
                        if (selected.equals(cp)) {
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.HOME)){
            //reset params? key probably not registered. (os-controlled?)
        }
    }

    String lastCondition = "";
    OrbittrapContainer lastOrbittrapContainer = null;
    ParamContainer lastParams = null;
    String lastPrecision = null;

    public void updateExpression(){

        ExpressionsParam expressions = (ExpressionsParam) systemContext.getParamValue(CommonFractalParameters.PARAM_EXPRESSIONS, ExpressionsParam.class);

        String currentCondition = (String) systemContext.getParamValue(ShaderSystemContext.PARAM_CONDITION);
        boolean conditionChanged = !currentCondition.equals(lastCondition);
        lastCondition = currentCondition;

        ParamSupplier otSupp = systemContext.getParamContainer().getParam(ShaderSystemContext.PARAM_ORBITTRAPS);
        OrbittrapContainer cont = null;
        boolean trapsChanged = false;
        if (otSupp != null){
            cont = otSupp.getGeneral(OrbittrapContainer.class);
            if (lastOrbittrapContainer != null && !cont.equals(lastOrbittrapContainer))
                trapsChanged = true;
        }

        boolean recompileShaders = conditionChanged;
        boolean changedPrecision = false;
        if (lastParams != null) {
            for (ParamSupplier supp : systemContext.getParamContainer().getParameters()) {
                if (!lastParams.getParamMap().containsKey(supp.getUID()) || !supp.getClass().isInstance(lastParams.getParam(supp.getUID()))){
                    recompileShaders = true;
                    supp.updateChanged(lastParams.getParam(supp.getUID()));
                }
            }
            if (lastPrecision != null && !lastPrecision.equals(getActivePrecision())) { //precision changed
                recompileShaders = true;
                changedPrecision = true;
            }
        }
        lastParams = new ParamContainer(systemContext.getParamContainer(), true);

        boolean update = expressionsParam == null || !(expressionsParam.equals(expressions)) || recompileShaders || trapsChanged || paramsChanged;

//        if (!update){
//            for (ParamSupplier supp : systemContext.getParamContainer().getParameters()){
//                supp.updateChanged(supp);
//                if (supp.isChanged()){
//                    update = true;
//                }
//            }
//        }

        if (update) {
            this.expressionsParam = expressions;
            try {
                ComputeExpressionBuilder computeExpressionBuilder = new ComputeExpressionBuilder(expressions, ((ShaderSystemContext)systemContext).getParametersByUID(), systemContext.getParamConfiguration().getUIDsByName());
                List<FractalsExpression> expressions2 = computeExpressionBuilder.getFractalsExpressions();
                if (isNewtonFractalEnabled()){
                    expressionAddNewtonMethod(expressions2);
                }
                ComputeExpressionDomain expressionDomain = computeExpressionBuilder.getComputeExpressionDomain(false, expressions2);
                ComputeExpression newExpression = expressionDomain.getMainExpressions().get(0);
                boolean expressionChanged = !newExpression.equals(expression);
                expression = newExpression;
                if (expressionChanged || recompileShaders || (trapsChanged && (cont == null || cont.needsShaderRecompilation(lastOrbittrapContainer)))) {
                    shaderBuilder = new ShaderBuilder(expressionDomain, systemContext);
                    shaderBuilder.setPrecision(getActivePrecision());
                    highPrecisionUnsupported = Gdx.app.getType().equals(Application.ApplicationType.Android);
                    compileComputeShader(changedPrecision);
                    lastPrecision = getActivePrecision();
                }

                if (paramsChanged || expressionChanged || conditionChanged || trapsChanged)
                    reset();
            } catch (IllegalArgumentException e){
                LOG.info("Couldn't parse firstExpression: \n"+e.getMessage());
            }
        }
        lastOrbittrapContainer = cont.copy();
        paramsChanged = false;
    }

    private void expressionAddNewtonMethod(List<FractalsExpression> expressions) {
        FractalsExpression firstExpr = expressions.get(0);
        String mainInputVar = expressionsParam.getMainInputVar();
        FractalsExpression deriv = firstExpr.getDerivative(mainInputVar);
        List<FractalsExpression> newExprParts = new ArrayList<>();
        List<Integer> newExprPartLinks = new ArrayList<>();
        List<Integer> newExprComplexLinks = new ArrayList<>();
        newExprParts.add(new VariableExpression(mainInputVar));

        List<FractalsExpression> divExprParts = new ArrayList<>();
        List<Integer> divExprPartLinks = new ArrayList<>();
        List<Integer> divExprComplexLinks = new ArrayList<>();
        divExprParts.add(firstExpr);
        divExprParts.add(deriv);
        divExprPartLinks.add(-1);
        divExprComplexLinks.add(ComputeInstruction.INSTR_DIV_COMPLEX);
        newExprParts.add(new MultExpression(divExprParts, divExprPartLinks, divExprComplexLinks));

        newExprPartLinks.add(-1);
        newExprComplexLinks.add(ComputeInstruction.INSTR_SUB_COMPLEX);
        FractalsExpression newExpr = new ChainExpression(newExprParts, newExprPartLinks, newExprComplexLinks);
        expressions.set(0, newExpr);
    }

    protected int getParamFloatCount(){
        return getActivePrecision().equals(ShaderSystemContext.TEXT_PRECISION_32) ? 3 : 6;
    }

    private String getActivePrecision() {
        String precisionSetting = systemContext.getParamContainer().getParam(ShaderSystemContext.PARAM_PRECISION).getGeneral(String.class);
        if (!ShaderSystemContext.TEXT_PRECISION_AUTO.equals(precisionSetting))
            return precisionSetting;
        if (highPrecisionUnsupported)
            return ShaderSystemContext.TEXT_PRECISION_32;

        double zoomBorder64bit = 1E-4;
        double zoom = systemContext.getParamContainer().getParam(ShaderSystemContext.PARAM_ZOOM).getGeneral(Number.class).toDouble();
        if (zoom < zoomBorder64bit){
            return ShaderSystemContext.TEXT_PRECISION_64;
        }

        return ShaderSystemContext.TEXT_PRECISION_32;
    }

    private void setComputeShaderUniforms() {
        float currentWidth = getWidth();
        float currentHeight = getHeight();

//        setColoringParams();

        ParamContainer paramContainer = systemContext.getParamContainer();

        computeShader.setUniformMatrix("u_projTrans", matrix);
        computeShader.setUniformf("ratio", currentWidth/(float)currentHeight);
        computeShader.setUniformf("resolution", getScaledWidth()*(float)getCalcScaleFactor(), getScaledHeight()*(float)getCalcScaleFactor());

//		long t = System.currentTimeMillis();
//		if (t-lastIncrease > 1) {
//			lastIncrease = t;
//			iterations++;
//		}
        computeShader.setUniformf("iterations", (float)paramContainer.getParam(CommonFractalParameters.PARAM_ITERATIONS).getGeneral(Integer.class));
        computeShader.setUniformf("firstIterations", (float)paramContainer.getParam(ShaderSystemContext.PARAM_FIRSTITERATIONS).getGeneral(Number.class).toDouble()/100f);
        computeShader.setUniformf("limit", (float)paramContainer.getParam(ShaderSystemContext.PARAM_LIMIT).getGeneral(Number.class).toDouble());
        Integer frameSamples = paramContainer.getParam(ShaderSystemContext.PARAM_SAMPLESPERFRAME).getGeneral(Integer.class);
        if (multisampleByRepeating || frameSamples < 1)
            frameSamples = 1;
        computeShader.setUniformf("maxSamplesPerFrame", (float) frameSamples);
        computeShader.setUniformi("colour3Output", (int)paramContainer.getParam(ShaderSystemContext.PARAM_STABLE_OUTPUT).getGeneral());

        double scale = paramContainer.getParam(ShaderSystemContext.PARAM_ZOOM).getGeneral(Number.class).toDouble();
        ComplexNumber midpoint = systemContext.getMidpoint();

        List<ParamSupplier> paramList = expression.getParameterList();
        int paramFloatCount = getParamFloatCount();
        float[] params = new float[paramList.size()*paramFloatCount];
        for (int i = 0 ; i <  paramList.size() ; i++){
            ParamSupplier supp = paramList.get(i);
            if (supp instanceof StaticParamSupplier || supp instanceof CoordinateDiscreteParamSupplier){
                ComplexNumber val;
                if (supp instanceof  StaticParamSupplier) {
                    val = (ComplexNumber) ((StaticParamSupplier) supp).getObj();
                    if (getActivePrecision().equals(ShaderSystemContext.TEXT_PRECISION_32)) {
                        params[i*paramFloatCount] = (float) val.realDouble();
                        params[i*paramFloatCount+1] = (float) val.imagDouble();
                        params[i*paramFloatCount+2] = 0f;
                    }
                    else {
                        double valReal = val.realDouble();
                        double valImag = val.imagDouble();
                        float[] valRealF = splitDouble(valReal);
                        float[] valImagF = splitDouble(valImag);
                        float[] scaleF = splitDouble(scale);
                        params[i * paramFloatCount] = valRealF[0];
                        params[i * paramFloatCount + 1] = valRealF[1];
                        params[i * paramFloatCount + 2] = valImagF[0];
                        params[i * paramFloatCount + 3] = valImagF[1];
                        params[i * paramFloatCount + 4] = 0f;
                        params[i * paramFloatCount + 5] = 0f;
                    }
                } else if (supp instanceof CoordinateDiscreteParamSupplier) {
                    if (getActivePrecision().equals(ShaderSystemContext.TEXT_PRECISION_32)) {
                        params[i*paramFloatCount] = (float)midpoint.realDouble();
                        params[i*paramFloatCount+1] = (float)midpoint.imagDouble();
                        params[i*paramFloatCount+2] = (float)scale;
                    }
                    else {
                        double midReal = midpoint.realDouble();
                        double midImag = midpoint.imagDouble();
                        float[] midRealF = splitDouble(midReal);
                        float[] midImagF = splitDouble(midImag);
                        float[] scaleF = splitDouble(scale);
                        params[i * paramFloatCount] = midRealF[0];
                        params[i * paramFloatCount + 1] = midRealF[1];
                        params[i * paramFloatCount + 2] = midImagF[0];
                        params[i * paramFloatCount + 3] = midImagF[1];
                        params[i * paramFloatCount + 4] = scaleF[0];
                        params[i * paramFloatCount + 5] = scaleF[1];
                    }
                }

            }
            else if (supp instanceof CoordinateBasicShiftParamSupplier || supp instanceof CoordinateModuloParamSupplier){
                if (getActivePrecision().equals(ShaderSystemContext.TEXT_PRECISION_32)) {
                    params[i*paramFloatCount] = (float)midpoint.realDouble();
                    params[i*paramFloatCount+1] = (float)midpoint.imagDouble();
                    params[i*paramFloatCount+2] = (float)scale;
                }
                else {
                    double midReal = midpoint.realDouble();
                    double midImag = midpoint.imagDouble();
                    float[] midRealF = splitDouble(midReal);
                    float[] midImagF = splitDouble(midImag);
                    float[] scaleF = splitDouble(scale);
                    params[i * paramFloatCount] = midRealF[0];
                    params[i * paramFloatCount + 1] = midRealF[1];
                    params[i * paramFloatCount + 2] = midImagF[0];
                    params[i * paramFloatCount + 3] = midImagF[1];
                    params[i * paramFloatCount + 4] = scaleF[0];
                    params[i * paramFloatCount + 5] = scaleF[1];
                }
            }
            else
                throw new IllegalArgumentException("Unsupported ParamSupplier for "+supp.getUID()+": "+supp.getClass().getName());
        }
        computeShader.setUniform1fv("params", params, 0, params.length);

        //TODO remove instance variable
        computeShader.setUniformf("scale", (float)scale);

        double centerR = midpoint.getReal().toDouble();
        double centerI = midpoint.getImag().toDouble();
        computeShader.setUniformf("center", (float) centerR, (float) centerI);
        float centerFp64LowR = (float) (centerR - (float) centerR);
        float centerFp64LowI = (float) (centerI - (float) centerI);
        computeShader.setUniformf("centerFp64Low", centerFp64LowR, centerFp64LowI);
        computeShader.setUniformf("smoothstepScaling", (float)expression.getSmoothstepConstant());
        computeShader.setUniformf("smoothstepShift", (float)0);
        computeShader.setUniformf("maxBorderSamples", paramContainer.getParam(ShaderSystemContext.PARAM_MAXBORDERSAMPLES).getGeneral(Integer.class));
        Integer maxSampleCount = paramContainer.getParam(ShaderSystemContext.PARAM_SUPERSAMPLING).getGeneral(Integer.class);
        computeShader.setUniformi("maxSampleCount", maxSampleCount);
        computeShader.setUniformi("sampleCount", updateAndGetSampleCountLimit(maxSampleCount));
        computeShader.setUniformf("gridFrequency", (float)paramContainer.getParam(ShaderSystemContext.PARAM_GRID_PERIOD).getGeneral(Number.class).toDouble());
        computeShader.setUniformf("moduloFrequency", (float)paramContainer.getParam(ShaderSystemContext.PARAM_MODULO_PERIOD).getGeneral(Number.class).toDouble());

        shaderBuilder.setUniforms(computeShader);
    }

    private int updateAndGetSampleCountLimit(Integer maxSampleCount) {
        double fps = 1.0/Gdx.graphics.getDeltaTime();
        int targetFps = systemContext.getParamContainer().getParam(ShaderSystemContext.PARAM_TARGET_FRAMERATE).getGeneral(Integer.class);
        if (targetFps <= 0){ //disabled
            maxSamplingDifference = maxSampleCount;
            return maxSampleCount;
        }
        if (fps > targetFps && maxSamplingDifference < maxSampleCount)
            maxSamplingDifference++;
        if (fps < targetFps && maxSamplingDifference > 1) {
            double throttleFactor = Math.min(targetFps/fps, maxSampleDifferenceMaxThrottleFactor);
            maxSamplingDifference /= throttleFactor;
        }
        if (maxSamplingDifference < 1.0)
            maxSamplingDifference = 1.0;
        if (maxSamplingDifference > maxSampleCount)
            maxSamplingDifference = maxSampleCount;
        return (int)(maxSampleCount-getSamplesLeft()+maxSamplingDifference);
    }

    private float[] splitDouble(double val) {
        int s = 32;
        double c = (Math.pow(2.0, s)+1.0)*val;
        double big = c-val;
        float hi = (float)(c-big);
        float lo = (float)(val-hi);
        return new float[] {hi, lo};
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
        ((ShaderSystemContext)this.systemContext).updateSize(resolutionX, resolutionY);
        resetFramebuffers();
//        setRefresh();
        reset();
    }

    @Override
    public void focusChanged(boolean focusedNow) {
//        if(focusedNow)
//            ((MainStage)getStage()).setFocusedRenderer(this);
    }

    protected GLFrameBuffer.FrameBufferBuilder getFboBuilderData(int width, int height){
        GLFrameBuffer.FrameBufferBuilder fboBuilderData = new GLFrameBuffer.FrameBufferBuilder(width, height);
        fboBuilderData.addBasicColorTextureAttachment(Pixmap.Format.RGB888);
        fboBuilderData.addBasicColorTextureAttachment(Pixmap.Format.RGB888);
        int glFormat = Pixmap.Format.toGlFormat(Pixmap.Format.RGB888);
        int glType = Pixmap.Format.toGlType(Pixmap.Format.RGB888);
        fboBuilderData.addColorTextureAttachment(glFormat, glFormat, glType);
//        fboBuilderData.addBasicStencilRenderBuffer();
//        fboBuilderData.addBasicDepthRenderBuffer();
        return fboBuilderData;
    }

    protected FrameBuffer getActiveDataFbo(){
        if (getCalcScaleFactor() >= 1){
            return getFboDataCurrent();
        }
        return getFboUpsample(getActiveUpsampleIndex());
    }

    protected double getCalcScaleFactor(){
        return 1.0/selectDivs;
    }

    protected int getActiveUpsampleIndex(){
        int exp = (int)Math.round(Math.log(getCalcScaleFactor())/Math.log(0.5));
        return exp - 1;
    }

    protected FrameBuffer getFboUpsample(int index){
        //create if needed
        while (fbosUpsample.size() <= index){
            int index2 = fbosUpsample.size();
            double scaleFactor = Math.pow(0.5, index2+1);
            int resX = (int) Math.ceil(resolutionX * scaleFactor);
            int resY = (int) Math.ceil(resolutionY * scaleFactor);
            GLFrameBuffer.FrameBufferBuilder fboBuilderData = getFboBuilderData(resX, resY);
            fbosUpsample.add(new FractalsFrameBuffer(fboBuilderData));
        }
        return fbosUpsample.get(index);
    }

    private void resetFramebuffers() {
        disposeFramebuffers();

        GLFrameBuffer.FrameBufferBuilder fboBuilderData = getFboBuilderData(resolutionX, resolutionY);
        fboDataFirst = new FractalsFrameBuffer(fboBuilderData);
        fboDataSecond = new FractalsFrameBuffer(fboBuilderData);
        float resImgX = getScaledWidth();
        float resImgY = getScaledHeight();
        float remainingScale = (float)resolutionScale;
        while (remainingScale >= 2.0){
            resImgX /= 2;
            resImgY /= 2;
            remainingScale /= 2;
        }
        fboImageFirst = new FrameBuffer(Pixmap.Format.RGB888, (int)resImgX, (int)resImgY, false);

        FrameBuffer firstDsSFbo = fbosReduceRemainingSamples.isEmpty() ? null : fbosReduceRemainingSamples.get(0);

        if (firstDsSFbo == null || firstDsSFbo.getWidth() != resolutionX || firstDsSFbo.getHeight() != resolutionY) {

            for (FrameBuffer fbo : fbosReduceRemainingSamples) {
                fbo.dispose();
            }
            fbosReduceRemainingSamples.clear();
            for (FrameBuffer fbo : fbosUpsample) {
                fbo.dispose();
            }
            fbosUpsample.clear();

            int dimMax = resolutionX > resolutionY ? resolutionX : resolutionY;
            dimMax = (int) Math.ceil(dimMax / reduceStep);
            int dsX = (int) Math.ceil(resolutionX / reduceStep);
            int dsY = (int) Math.ceil(resolutionY / reduceStep);
            while (dimMax >= reduceDimThresholdCpu) {
                FrameBuffer fboDs = new FrameBuffer(Pixmap.Format.RGB888, dsX, dsY, false);
                fbosReduceRemainingSamples.add(fboDs);

                if (dimMax == 1)
                    break;
                dsX = (int) Math.ceil(dsX / reduceStep);
                dsY = (int) Math.ceil(dsY / reduceStep);
                dimMax = (int) Math.ceil(dimMax / reduceStep);
            }
        }

        int dsX = (int) Math.ceil(resolutionX / reduceStep);
        int dsY = (int) Math.ceil(resolutionY / reduceStep);

        if (fbosDownsample.size() == 0 || (fbosDownsample.get(0).getWidth() != dsX || fbosDownsample.get(0).getHeight() != dsY)) {
            for (FrameBuffer fbo : fbosDownsample) {
                fbo.dispose();
            }
            fbosDownsample.clear();
            for (FrameBuffer fbo : fbosUpsample){
                fbo.dispose();
            }
            fbosUpsample.clear();
            double remainingResScale = resolutionScale;
            if (remainingResScale >= 2.0) {
                GLFrameBuffer.FrameBufferBuilder fbb2 = new GLFrameBuffer.FrameBufferBuilder(dsX, dsY);
                fbb2.addBasicColorTextureAttachment(Pixmap.Format.RGB888);
                fbb2.addBasicColorTextureAttachment(Pixmap.Format.RGB888);
                fbb2.addBasicColorTextureAttachment(Pixmap.Format.RGB888);
                while (remainingResScale >= 2.0) {
                    FrameBuffer fboDs = new FractalsFrameBuffer(fbb2);
                    fbosDownsample.add(fboDs);
                    dsX = (int) Math.ceil(dsX / reduceStep);
                    dsY = (int) Math.ceil(dsY / reduceStep);
                    remainingResScale /= 2.0;
                }
            }
        }

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
            fboImageFirst.dispose();
//            if (fboImageSecond != null)
//                fboImageSecond.dispose();
        }
    }

    private boolean isRenderingDone(){
        return isProgressiveRenderingFinished();
    }

    protected boolean isDepthShaderEnabled(){
        return true;
    }

    protected float getResolutionScale(boolean calcScale){
        //TODO reactivate when ready
//        double factor = calcScale ? getCalcScaleFactor() : 1.0;
        double factor = 1.0;
        return (float)(resolutionScale*factor);
    }

    protected void renderImage(Batch batch) {

        long t1 = System.nanoTime();

        setResolutionScale((double)systemContext.getParamValue(ShaderSystemContext.PARAM_RESOLUTIONSCALE, Double.class));
        float resolutionScaleF = getResolutionScale(true);
        this.samplesPerFrame = (float)(int)systemContext.getParamValue(ShaderSystemContext.PARAM_SAMPLESPERFRAME);

        updateMousePos();

        boolean reshowLastFrame = isRenderingDone() && reshowLastFrameEnabled && renderedPart && !refresh && !refreshColoring && !isScreenshot(false);
        refreshColoring = false;

        Matrix4 projectionMatrix = new Matrix4();
        if (!reshowLastFrame) {
            //apply current renderer resolution to batch
            projectionMatrix.setToOrtho2D(0, 0, (float) (getScaledWidth()), (float) (getScaledHeight()));
            batch.setProjectionMatrix(projectionMatrix);
        }

        //determine if existing pixels can be reused
        boolean reusePixels = reuseDataEnabled && renderedPart && !prevDataFboOutdated;
        float calcScaleFactor = (float)getCalcScaleFactor();
        float shiftedX = ((int)(pannedDeltaX*(resolutionScaleF)))/(resolutionScaleF);
        float shiftedY = ((int)(pannedDeltaY*(resolutionScaleF)))/(resolutionScaleF);
        pannedDeltaX = pannedDeltaX-shiftedX;
        pannedDeltaY = pannedDeltaY-shiftedY;

        //alternate between two data fbos on refresh
        FrameBuffer fboDataCurr = getFboDataCurrent();
        FrameBuffer fboDataPrev = getFboDataPrevious();

//        fboSampleCount.begin();
//        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
//        fboSampleCount.end();


        long t2 = System.nanoTime();

        Integer supersampling = systemContext.getParamContainer().getParam(
                ShaderSystemContext.PARAM_SUPERSAMPLING).getGeneral(Integer.class);

        FrameBuffer activeDataFbo = getActiveDataFbo();
        Array<Texture> newDataTextures = activeDataFbo.getTextureAttachments();
        Array<Texture> completeDataTextures = getFboDataCurrent().getTextureAttachments();
        Array<Texture> oldDataTextures = getFboDataPrevious().getTextureAttachments();

        if (refresh) {

            batch.end();
            batch.flush();

            //calculate (and reuse) data and store in current data fbo

            renderedPart = true;

            int frameSamples = !multisampleByRepeating ? 1 :
                    (Integer)systemContext.getParamValue(ShaderSystemContext.PARAM_SAMPLESPERFRAME);

//            projectionMatrix.setToOrtho2D(0, 0, (float) (getScaledWidth()/2), (float) (getScaledHeight()/2));
//            batch.setProjectionMatrix(projectionMatrix);

            getActiveDataFbo().begin();

            if (getActiveDataFbo() != getFboDataCurrent()) {
                Gdx.gl.glClearColor(1, 0, 0, 1);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            }
//            getFboDataCurrent().begin();

            float bufferOffsetR = (float) Math.round(shiftedX * resolutionScaleF);
            float bufferOffsetI = (float) Math.round(shiftedY * resolutionScaleF);

            boolean discardBuffer = !reusePixels || prevDataFboOutdated;

            for (int frameSample = 0; frameSample < frameSamples ; frameSample++) {


//                if (isDepthShaderEnabled()){
//                if (false){
//
////                    Gdx.gl.glClearDepthf(1f);
////                    Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
//
////                    Gdx.gl.glEnable(GL20.GL_STENCIL_TEST);
////                    Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_REPLACE); //TODO value is probably also replaced if discarded in fragment shader
////                    Gdx.gl.glStencilFunc(GL20.GL_ALWAYS, 0, 0xFF);
////                    Gdx.gl.glStencilMask(0xFF);
//
////                    Gdx.gl.glClearDepthf(0f);
////                    Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
////                    Gdx.gl.glColorMask(false, false, false, false);
//
//                    Texture valueTexture = fboDataPrev.getColorBufferTexture();
//                    valueTexture.bind(2);
//                    Texture missingSamplesTexture = fboDataPrev.getTextureAttachments().get(2);
//                    missingSamplesTexture.bind(1);
//                    Texture samplesTexture = fboDataPrev.getTextureAttachments().get(1);
//                    samplesTexture.bind(0);
//                    selectShader.setUniformi("u_texture", 0);
//                    selectShader.setUniformi("samplesTexture", 1);
//                    selectShader.setUniformi("valueTexture", 2);
//                    selectShader.setUniformi("selectParams", selectPatternX, selectPatternY, selectDivs);
//                    selectShader.setUniformf("bufferOffset", bufferOffsetR, bufferOffsetI);
//                    selectShader.setUniformi("samplesPerPixel", (int)supersampling);
//                    selectShader.setUniformi("discardBuffer", !reusePixels || prevDataFboOutdated ? 1 : 0);
//                    selectShader.setUniformMatrix("u_projTrans", matrix);
////                    depthShader.setUniformf("ratio", currentWidth/(float)currentHeight);
//                    selectShader.setUniformf("resolution", getScaledWidth(), getScaledHeight());
//                    selectShader.setUniformf("bufferOffset", bufferOffsetR, bufferOffsetI);
//
//                    batch.begin();
//
//                    batch.setShader(selectShader);
//
////                    selectPattern = (selectPattern +1)% selectCycleLength;
//
//                    TextureRegion texReg = new TextureRegion(samplesTexture);
//                    batch.draw(texReg, getX(), getY());
//                    batch.end();
//
//                }
//                else {
//                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
//                }

                computeShader.begin();
                setComputeShaderUniforms();
//            fboSampleCount.getColorBufferTexture().bind(1);
//            fboDataCurr.bind();

                Array<Texture> drawDataTextures = null;
                if (calcScaleFactor < 1){
                    drawDataTextures = completeDataTextures;
                } else {
                    drawDataTextures = oldDataTextures;
                }

                Texture computeBufferTexture = drawDataTextures.get(0);

                Texture samplesTexture = drawDataTextures.get(1);

                Texture missingSamplesTexture = drawDataTextures.get(2);

                if (System.currentTimeMillis() - extractMaxRemainingSamplesLastRefresh > extractMaxRemainingSamplesInterval*1000_000) {
                    extractMaxRemainingSamples = true;
                }
                boolean extractRemainingSamples = extractMaxRemainingSamples;
                missingSamplesTexture.bind(3);
                computeShader.setUniformi("missingSamplesTexture", 3);
                samplesTexture.bind(2);
                computeShader.setUniformi("samplesTexture", 2);
                computeBufferTexture.bind(1);
                computeShader.setUniformi("escapeTimeTexture", 1);
                drawDataTextures.get(0).bind(0);
                computeShader.setUniformi("currTexture", 0);

                computeShader.setUniformi("extractRemainingSamples", extractRemainingSamples ? 1 : 0);
                computeShader.setUniformf("bufferOffset", bufferOffsetR, bufferOffsetI);
                computeShader.setUniformi("discardBuffer", discardBuffer ? 1 : 0);
                computeShader.setUniformi("upscaleFactor", selectDivs);
                computeShader.setUniformf("upscaleShift", selectPatternX, selectPatternY);

                prevDataFboOutdated = false;

                batch.begin();
                batch.setShader(computeShader);

                Color c = batch.getColor();
                batch.setColor(c.r, c.g, c.b, 1.0f);

                Texture tex = drawDataTextures.get(0);
//                tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                batch.draw(tex, 0, 0, getScaledWidth(), getScaledHeight(), 0, 0, tex.getWidth(), tex.getHeight(), false, false);

//                computeShader.end();
                batch.end();
                batch.flush();
            }

//            getFboDataCurrent().end();
            getActiveDataFbo().end();

            projectionMatrix.setToOrtho2D(0, 0, (float) (getScaledWidth()), (float) (getScaledHeight()));
            batch.setProjectionMatrix(projectionMatrix);

//            projectionMatrix.setToOrtho2D(0, 0, (float) (getScaledWidth()), (float) (getScaledHeight()));
//            batch.setProjectionMatrix(projectionMatrix);

            if (calcScaleFactor < 1){
                getFboDataCurrent().begin();
                oldDataTextures.get(2).bind(5);
                combineShader.setUniformi("textureExtra", 5);
                oldDataTextures.get(1).bind(4);
                combineShader.setUniformi("textureSamples", 4);
                oldDataTextures.get(0).bind(3);
                combineShader.setUniformi("textureCondition", 3);
                newDataTextures.get(2).bind(2);
                combineShader.setUniformi("textureExtraFrame", 2);
                newDataTextures.get(1).bind(1);
                combineShader.setUniformi("textureSamplesFrame", 1);
                newDataTextures.get(0).bind(0);
                combineShader.setUniformi("textureConditionFrame", 0);
                Texture tex = newDataTextures.get(0);
                combineShader.setUniformi("divs", selectDivs);
                combineShader.setUniformf("bufferOffset", bufferOffsetR, bufferOffsetI);
                combineShader.setUniformf("patternX", selectPatternX);
                combineShader.setUniformf("patternY", selectPatternY);
                combineShader.setUniformi("discardBuffer", discardBuffer ? 1 : 0);
//                combineShader.bind();

                batch.begin();
                batch.setShader(combineShader);

                batch.draw(tex, 0, 0, getScaledWidth(), getScaledHeight(),
                        0, 0, tex.getWidth()*selectDivs, tex.getHeight()*selectDivs, false, false);

//                Texture tex2 = newDataTextures.get(1);
//                newDataTextures.get(1).bind(1);
//                combineShader.setUniformi("textureConditionFrame", 1);
//                completeDataTextures.get(1).bind(0);
//                combineShader.setUniformi("textureCondition", 0);
//                batch.draw(tex2, 0, 0, getScaledWidth(), getScaledHeight(),
//                        0, 0, tex2.getWidth(), tex2.getHeight(), false, false);

                batch.end();
                getFboDataCurrent().end();
            }

            batch.begin();

            selectPatternIndex++;
            if (selectPatternIndex >= selectCycleLength)
                selectPatternIndex = 0;
            selectPatternX = selectPatternIndex % selectDivs;
            selectPatternY = selectPatternIndex / selectDivs;
            useFirstDataFbo = !useFirstDataFbo;

            if (extractMaxRemainingSamples && !fbosReduceRemainingSamples.isEmpty()){
                Texture lastTexture = getFboDataPrevious().getTextureAttachments().get(1);
                int sourceWidth = (int)getScaledWidth();
                int sourceHeight = (int)getScaledHeight();
                batch.setShader(reduceShader);

                for (int i = 0; i < fbosReduceRemainingSamples.size() ; i++){
                    FrameBuffer dsFbo = fbosReduceRemainingSamples.get(i);
                    boolean extractData = i == fbosReduceRemainingSamples.size()-1;

                    dsFbo.begin();
                    reduceShader.setUniformi("texture0", 0);
                    reduceShader.setUniformi("inputDataType", 0);
                    reduceShader.setUniformi("scalingMode", 0);
                    reduceShader.setUniformi("outputMode", 2);
                    reduceShader.setUniformi("maxB", 1);
                    reduceShader.setUniformf("minBorder", 0.0001f);
                    lastTexture.bind(0);
                    lastTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    reduceShader.bind();
                    batch.draw(lastTexture, 0, 0, getScaledWidth(), getScaledHeight()
//                            , 0, 0, sourceWidth, sourceHeight, false, false
                    );
                    batch.flush();

                    if (extractData){
                        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, dsFbo.getWidth(), dsFbo.getHeight(), false);
                        int lowestValue = Integer.MAX_VALUE;
                        int i2 = 0;
                        for (int y = 0 ; y < dsFbo.getHeight() ; y++){
                            for (int x = 0 ; x < dsFbo.getWidth() ; x++){
                                int val = getIntValue(pixels, i2);
                                if (val < lowestValue && val > 0)
                                    lowestValue = val;
                                i2++;
                            }
                        }
                        if (lowestValue == Integer.MAX_VALUE)
                            progressiveRenderingMissingFrames = 0;
                        else
                            progressiveRenderingMissingFrames = Math.max(progressiveRenderingMissingFrames, supersampling-lowestValue-1);
                    }

                    dsFbo.end();

                    sourceWidth = (int)Math.ceil(sourceWidth/2);
                    sourceHeight = (int)Math.ceil(sourceHeight/2);
                    lastTexture = dsFbo.getColorBufferTexture();
                }

                extractMaxRemainingSamples = false;
                extractMaxRemainingSamplesLastRefresh = System.currentTimeMillis();
            }
        }

        long t3 = System.nanoTime();
        long t4 = System.nanoTime();

//        debugColorEncoding();
        FrameBuffer fboImage = getFboImageCurrent();
        FrameBuffer lastFbo = getFboDataPrevious();

        if (!reshowLastFrame) {
            if (getResolutionScale(false) >= 2.0){
                batch.setShader(reduceShader);
                int sourceWidth = (int)getScaledWidth();
                int sourceHeight = (int)getScaledHeight();
                boolean first = true;
                for (FrameBuffer dsFbo : fbosDownsample){
                    dsFbo.begin();
                    reduceShader.setUniformi("texture0", 0);
                    reduceShader.setUniformi("texture1", 1);
                    reduceShader.setUniformi("inputDataType", 1);
                    reduceShader.setUniformi("scalingMode", 0);
                    reduceShader.setUniformi("outputMode", 1);
                    reduceShader.setUniformi("maxB", 0);
                    reduceShader.setUniformf("minBorder", -1f);
                    Texture lastTexture2 = lastFbo.getTextureAttachments().get(2);
                    lastTexture2.bind(1);
                    Texture lastTexture = lastFbo.getColorBufferTexture();
                    lastTexture.bind(0);
                    lastTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    reduceShader.bind();
                    batch.draw(lastTexture, 0, 0, getScaledWidth(), getScaledHeight(),
                            0, 0, (int)Math.ceil(sourceWidth/2), (int)Math.ceil(sourceHeight/2), false, false);
                    batch.flush();
                    dsFbo.end();
                    lastFbo = dsFbo;
                    first = false;

                    sourceWidth = (int)Math.ceil(sourceWidth/2);
                    sourceHeight = (int)Math.ceil(sourceHeight/2);
                }
                batch.end();
                batch.begin();
            }
            batch.setShader(coloringShader);
            fboImage.begin();
            //Pass 2: render fboData content to fboImage using coloringShader

            setColoringParams();

            Texture paletteFallback = ((MainStage) getStage()).getPaletteTexture(MainStage.PARAMS_PALETTE2, 1);
            paletteFallback.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.MipMapLinearLinear);
            paletteFallback.bind(3);

            Texture paletteEscaped = ((MainStage) getStage()).getPaletteTexture(MainStage.PARAMS_PALETTE, 0);
            paletteEscaped.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.MipMapLinearLinear);
            paletteEscaped.bind(2);

            Texture altColorTexture = lastFbo.getTextureAttachments().get(2);
            altColorTexture.bind(1);

            Texture dataTexture = lastFbo.getColorBufferTexture();
            dataTexture.bind(0);

            coloringShader.setUniformi("u_texture", 0);
            coloringShader.setUniformi("altColorTexture", 1);
            coloringShader.setUniformi("paletteEscaped", 2);
            coloringShader.setUniformi("paletteFallback", 3);
//            coloringShader.setUniformi("extraTexture", 3);
//            coloringShader.setUniformf("useExtraData", lastFbo == getFboDataPrevious() ? 0f : 1f);
            coloringShader.setUniformf("useExtraData", 0f);
            coloringShader.setUniformf("resolution", dataTexture.getWidth(), dataTexture.getHeight());

            batch.draw(dataTexture, 0, 0, getScaledWidth(), getScaledHeight(),
                    0, 0, dataTexture.getWidth(), dataTexture.getHeight(), false, false);

            batch.end();

            coloringShader.end();

            fboImage.end();

            t4 = System.nanoTime();


            projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); // here is the actual size you want
            batch.setProjectionMatrix(projectionMatrix);

            batch.begin();
//            useFirstImageFbo = !useFirstImageFbo;
            fboImage = getFboImageCurrent();
        }

        batch.setShader(passthroughShader);
//        if (reshowLastFrame) {
//            fboImage.begin();
//            Texture tex2 = fboImage.getColorBufferTexture();
//            batch.draw(tex2, getX(), getY(), getWidth(), getHeight());
//            fboImage.end();
//        }

        Texture tex2 = fboImage.getColorBufferTexture();
        TextureRegion texReg2 = new TextureRegion(tex2, 0, 0, (int) fboImage.getWidth(), (int) fboImage.getHeight());
        texReg2.flip(false, true);
        batch.draw(texReg2, getX(), getY(), getWidth(), getHeight());

        if (showDebugFBOs)
            debugDrawFBOs(batch);

        long t5 = System.nanoTime();

        ParamContainer clientParams = ((MainStage)getStage()).getClientParams();
        boolean drawPath =      clientParams.getParam(MainStage.PARAMS_DRAW_PATH).getGeneral(Boolean.class);
        boolean drawAxis =      clientParams.getParam(MainStage.PARAMS_DRAW_AXIS).getGeneral(Boolean.class);
        boolean drawMidpoint =  clientParams.getParam(MainStage.PARAMS_DRAW_MIDPOINT).getGeneral(Boolean.class);
        boolean drawOrigin =    clientParams.getParam(MainStage.PARAMS_DRAW_ZERO).getGeneral(Boolean.class);
        boolean tracesEnabled = clientParams.getParam(MainStage.PARAMS_DRAW_ORBIT).getGeneral(Boolean.class);
        boolean inFocus = ((MainStage) getStage()).getFocusedRenderer() == this;
        boolean pathVisible = drawPath && inFocus && rendererContext.getSelectedParamInterpolation() != null;

        boolean useShaperenderer = (tracesEnabled && inFocus) || pathVisible || drawAxis || drawMidpoint || drawOrigin;
        if (useShaperenderer) {
            batch.end();

            drawShapes(batch);

            batch.begin();
        }

        long t6 = System.nanoTime();

        updateProgressiveRendering();
        if (reusePixels && isRenderingDone())
            refresh = false;
        if (isScreenshot(true) && isRenderingDone()) {
            if (!useShaperenderer)
                batch.end();
            makeScreenshot();
            if (!useShaperenderer)
                batch.begin();
        }

        long t7 = System.nanoTime();

        long t_setup = t2-t1;
        long t_refresh = t3-t2;
        long t_combine = t4-t3;
        long t_color = t5-t4;
        long t_resample = t6-t5;
        long t_shapes = t7-t6;
        double t_total = t7-t1;

        double warn_limit = 100.0/NumberUtil.NS_TO_MS;

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

    private int getIntValue(byte[] pixels, int index) {
        int baseIndex = index * 4;
        return getIntByteValue(pixels, baseIndex) + getIntByteValue(pixels, baseIndex + 1)*256;
    }

    private int getIntByteValue(byte[] pixels, int i) {
        byte rawVal = pixels[i];
        int val = rawVal;
        if (val < 0)
            val = 128 - val;
        return val;
    }

    private boolean isProgressiveRenderingFinished(){
        if (progressiveRenderingMissingFrames == 1)
            extractMaxRemainingSamples = true;
        return progressiveRenderingMissingFrames == 0;
    }

    private void updateProgressiveRendering(){
        float newSampleCount = (Integer) systemContext.getParamValue(ShaderSystemContext.PARAM_SAMPLESPERFRAME, Integer.class);
        newSampleCount /= selectCycleLength;
        progressiveRenderingMissingFrames = progressiveRenderingMissingFrames - newSampleCount;
        if (progressiveRenderingMissingFrames < 0)
            progressiveRenderingMissingFrames = 0;
    }

    private void resetProgressiveRendering(){
        int samples = (Integer) systemContext.getParamValue(ShaderSystemContext.PARAM_SUPERSAMPLING, Integer.class);
        progressiveRenderingMissingFrames = Math.max(samples, getProgressiveRenderingMinFrames());
    }

    protected int getProgressiveRenderingMinFrames() {
        ParamSupplier suppFirstIt = systemContext.getParamContainer().getParam(ShaderSystemContext.PARAM_FIRSTITERATIONS);
        Number number = suppFirstIt == null ? null : suppFirstIt.getGeneral(Number.class);
        ParamSupplier suppMultisample = systemContext.getParamContainer().getParam(ShaderSystemContext.PARAM_SUPERSAMPLING);
        int samplesPerFrame = (int) systemContext.getParamContainer().getParam(ShaderSystemContext.PARAM_SAMPLESPERFRAME).getGeneral();
        return (number != null && number.toDouble() == 100.0) || (suppMultisample != null && suppMultisample.getGeneral(Integer.class) == 1) ? 0 : progressiveRenderingMinFramesIfFloodfill;
    }

    protected FrameBuffer getFboDataPrevious() {
        if (getCalcScaleFactor() < 1)
            return fboDataFirst;
        return useFirstDataFbo ? fboDataSecond : fboDataFirst;
    }

    protected FrameBuffer getFboDataCurrent() {
        if (getCalcScaleFactor() < 1)
            return fboDataFirst;
        return useFirstDataFbo ? fboDataFirst : fboDataSecond;
    }

    protected FrameBuffer getFboImagePrevious() {
//        return fboImageFirst;
        return useFirstImageFbo ? fboImageSecond : fboImageFirst;
    }

    protected FrameBuffer getFboImageCurrent() {
        return fboImageFirst;
//            return useFirstImageFbo ? fboImageFirst : fboImageSecond;
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

        Array<Texture> textureAttachments = fboDataFirst.getTextureAttachments();
        if (textureAttachments.size == 1)
            return;

        Texture tex5 = textureAttachments.get(2);
////        Texture tex5 = fboSampleCount.getColorBufferTexture();
        TextureRegion texReg5 = new TextureRegion(tex5, 0, 0, (int) fboDataSecond.getWidth(), (int) fboDataSecond.getHeight());
        texReg5.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
//        texReg5.flip(false, true);
        batch.draw(texReg5, getX(), getY()+getHeight()*2/3, getWidth() / 3, getHeight() / 3);

        Texture tex4 = textureAttachments.get(1);
////        Texture tex4 = fboSampleCount.getColorBufferTexture();
        TextureRegion texReg4 = new TextureRegion(tex4, 0, 0, (int) fboDataSecond.getWidth(), (int) fboDataSecond.getHeight());
        texReg4.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
//        texReg4.flip(false, true);
        batch.draw(texReg4, getX() + getWidth() / 3, getY()+getHeight()*2/3, getWidth() / 3, getHeight() / 3);

        Texture tex3 = fboDataFirst.getColorBufferTexture();
        TextureRegion texReg3 = new TextureRegion(tex3, 0, 0, (int) fboDataFirst.getWidth(), (int) fboDataFirst.getHeight());
        texReg3.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
//        texReg3.flip(false, true);
        batch.draw(texReg3, getX() + getWidth() * 2 / 3, getY()+getHeight()*2/3, getWidth()/3f, getHeight()/3f);

        Texture tex8 = getActiveDataFbo().getTextureAttachments().get(2);
        TextureRegion texReg8 = new TextureRegion(tex8, 0, 0, (int) tex8.getWidth(), (int) tex8.getHeight());
        texReg8.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
//        texReg8.flip(false, true);
        batch.draw(texReg8, getX(), getY(), tex8.getWidth()/3f, tex8.getHeight()/3f);

        Texture tex7 = getActiveDataFbo().getTextureAttachments().get(1);
        TextureRegion texReg7 = new TextureRegion(tex7, 0, 0, (int) tex7.getWidth(), (int) tex7.getHeight());
        texReg7.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
//        texReg7.flip(false, true);
        batch.draw(texReg7, getX()+tex8.getWidth()/3f, getY(), tex7.getWidth()/3f, tex7.getHeight()/3f);

        Texture tex6 = getActiveDataFbo().getColorBufferTexture();
        TextureRegion texReg6 = new TextureRegion(tex6, 0, 0, (int) tex6.getWidth(), (int) tex6.getHeight());
        texReg6.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
//        texReg6.flip(false, true);
        batch.draw(texReg6, getX()+tex8.getWidth()/3f+tex7.getWidth()/3f, getY(), tex6.getWidth()/3f, tex6.getHeight()/3f);

        float baseX = getX();
        float baseY = getY()+getHeight()*2/3;
        for (FrameBuffer fbo : fbosReduceRemainingSamples){
            boolean isLast = fbo == fbosReduceRemainingSamples.get(Math.min(fbosReduceRemainingSamples.size()-1, 0));
//            float scale = fbo.getHeight() < 10f ? 1f : 3f;
            float scale = 3f*(float)resolutionScale;
            float displayWidth = fbo.getWidth()/scale;
            float displayHeight = fbo.getHeight()/scale;
            Texture dsTex = fbo.getColorBufferTexture();
            TextureRegion dsTexReg = new TextureRegion(dsTex);
            baseY -= displayHeight;
            batch.draw(dsTexReg, baseX, baseY, displayWidth, displayHeight);
        }

        baseX = getX() + getWidth()*2/3;
        baseY = getY() + getHeight()*2/3;
        for (FrameBuffer fbo : fbosDownsample){
            float scale = 3f*(float)resolutionScale;
            float displayWidth = fbo.getWidth()/scale;
            float displayHeight = fbo.getHeight()/scale;
            Texture dsTex = fbo.getColorBufferTexture();
            TextureRegion dsTexReg = new TextureRegion(dsTex);
            Texture dsTex2 = fbo.getTextureAttachments().get(2);
            TextureRegion dsTexReg2 = new TextureRegion(dsTex2);
            baseY -= displayHeight;
            batch.draw(dsTexReg, baseX, baseY, displayWidth, displayHeight);
            batch.draw(dsTexReg2, baseX+getWidth()/6, baseY, displayWidth, displayHeight);
        }
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
    double[] tracesIterations = null;
    EscapeTimeCpuCalculatorNew traceCalculator = new EscapeTimeCpuCalculatorNew();

    List<ComplexNumber> selectedControlPoints = new ArrayList<>();
    ComplexNumber movingControlPoint = null;

    double controlPointSelectionRadius = 5.0;
    Color controlPointSelectedColor = Color.SKY;
    Color controlPointColor = Color.BLUE;

    protected void drawShapes(Batch batch) {

        ParamContainer clientParams = ((MainStage)getStage()).getClientParams();
        boolean drawPath =              clientParams.getParam(MainStage.PARAMS_DRAW_PATH).getGeneral(Boolean.class);
        boolean drawAxis =              clientParams.getParam(MainStage.PARAMS_DRAW_AXIS).getGeneral(Boolean.class);
        boolean drawMidpoint =          clientParams.getParam(MainStage.PARAMS_DRAW_MIDPOINT).getGeneral(Boolean.class);
        boolean drawOrigin =            clientParams.getParam(MainStage.PARAMS_DRAW_ZERO).getGeneral(Boolean.class);
        float lineWidth = (float)(double)clientParams.getParam(MainStage.PARAMS_TRACES_LINE_WIDTH).getGeneral(Number.class).toDouble();
        float pointSize = (float)(double)clientParams.getParam(MainStage.PARAMS_TRACES_POINT_SIZE).getGeneral(Number.class).toDouble();
        boolean orbitEnabled =         clientParams.getParam(MainStage.PARAMS_DRAW_ORBIT).getGeneral(Boolean.class);
        int orbitIterations =                clientParams.getParam(MainStage.PARAMS_ORBIT_TRACES).getGeneral(Integer.class);
        boolean inFocus = ((MainStage) getStage()).getFocusedRenderer() == this;
        boolean tracesVisible = orbitEnabled && inFocus && orbitIterations > 0 && (lineWidth > 0 || pointSize > 0);
        boolean disabled = !drawMidpoint && !drawPath && !drawAxis && !tracesVisible;
        if (disabled)
            return;

        prepareShapeRenderer(batch);

        if (drawPath)
            drawAnimationPath();

        if (orbitEnabled && orbitIterations > 0) {
            updateOrbitArrays();
            drawOrbit(batch, clientParams, lineWidth, pointSize);
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

//    float timePassed = 0;

    private void updateOrbitArrays() {

        ParamContainer clientParams = ((MainStage)getStage()).getClientParams();
        int traceCount = clientParams.getParam(MainStage.PARAMS_ORBIT_TRACES).getGeneral(Integer.class);

        //determine coordinates
        ComplexNumber coords = null;
        String posVarName = clientParams.getParam(MainStage.PARAMS_ORBIT_TARGET).getGeneral(String.class);
        if (posVarName.equals("path")) {
            coords = clientParams.getParam(MainStage.PARAMS_TRACES_VALUE).getGeneral(ComplexNumber.class);
//            timePassed += Gdx.graphics.getDeltaTime();
        }
        else {
            ParamSupplier posVar = systemContext.getParamContainer().getParam(posVarName);
            boolean useMousePos = posVarName.equals("mouse") || posVar == null || !(posVar instanceof StaticParamSupplier) || (((StaticParamSupplier) posVar).getGeneral() instanceof ComplexNumber);
            if (useMousePos)
                coords = getComplexMapping(mouseX, mouseY);
        }


        boolean tracePerInstruction = clientParams.getParam(MainStage.PARAMS_ORBIT_TRACE_PER_INSTRUCTION).getGeneral(Boolean.class);
        //calculate sample on cpu and save traces
        double[][] traces = sampleCoordsOnCpu(traceCount, tracePerInstruction, coords);
        if (traces.length == 3) {
            tracesReal = traces[0];
            tracesImag = traces[1];
            tracesIterations = traces[2];
        } else {
            tracesReal = new double[0];
            tracesImag = new double[0];
            tracesIterations = new double[0];
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

    private double[][] sampleCoordsOnCpu(int traceCount, boolean tracePerInstruction, ComplexNumber coords) {
        //prepare calculator
        AbstractArrayChunk traceChunk = new TraceChunk();
        traceChunk.setCurrentTask(new TraceTask(systemContext));
//        BreadthFirstLayer layer = new BreadthFirstLayer().with_samples(1).with_rendering(false);
        boolean layerSet = false;
        for (Layer layer : ((LayerConfiguration)systemContext.getParamValue(ShaderSystemContext.PARAM_LAYER_CONFIG)).getLayers()){
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
        traceCalculator.setTraceCount(traceCount, tracePerInstruction);

        //calculate traces
        traceCalculator.calculate(traceChunk, null, null);
        return traceCalculator.getTraces();
    }

    private void drawOrbit(Batch batch, ParamContainer clientParams, float lineWidth, float pointSize) {

        //draw traces
        float lineTransparency = (float)(double)clientParams.getParam(MainStage.PARAMS_TRACES_LINE_TRANSPARENCY).getGeneral(Number.class).toDouble();
        float pointTransparency = (float)(double)clientParams.getParam(MainStage.PARAMS_TRACES_POINT_TRANSPARENCY).getGeneral(Number.class).toDouble();

        int nextIteration = 0;

        for (int i = 0 ; i < traceArrayFilledSize ; i++){

            double real = tracesReal[i];
            double imag = tracesImag[i];
            float x = getScreenX(real);
            float y = getScreenY(imag);
            int iteration = nextIteration;
            nextIteration = i < traceArrayFilledSize-1 ? (int)tracesIterations[i+1] : Integer.MAX_VALUE;

            //determine color
            float prog = i/(float)traceArrayFilledSize;
            float r = prog;
            float g = 1f-(prog*0.5f);
            float b = 0f;
            shapeRenderer.setColor(r, g, b, lineTransparency);

            //draw trace line
            if (lineWidth > 0 && i > 0) {
                float xPrev = getScreenX(tracesReal[i - 1]);
                float yPrev = getScreenY(tracesImag[i - 1]);

                shapeRenderer.rectLine(x, y, xPrev, yPrev, lineWidth);
            }

            //draw z_i
            if (pointSize > 0 && (i == 0 || iteration != nextIteration)) {
                if (pointTransparency != lineTransparency)
                    shapeRenderer.setColor(r, g, b, pointTransparency);

//                shapeRenderer.rect(x-pointSize/2f, y-pointSize/2f, pointSize, pointSize);
                shapeRenderer.circle(x, y, pointSize, 4);
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
        else if (path.size() > 0) {
            for (int i = 0; i < path.size() - 1; i++) {
                drawAnimationPathSegment(i == 0, radius, lineWidth, path.get(i), path.get(i + 1));
            }
//            ComplexNumber lastPoint = path.get(path.size()-1);
//            if (isControlPointSelected(lastPoint))
//                shapeRenderer.setColor(controlPointSelectedColor);
//            float p2ScreenX = getScreenX(lastPoint.getReal().toDouble());
//            float p2ScreenY = getScreenY(lastPoint.getImag().toDouble());
//            if (radius > 0)
//                shapeRenderer.circle(p2ScreenX, p2ScreenY, radius, 2);
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
        Number zoom = (Number) systemContext.getParamValue(ShaderSystemContext.PARAM_ZOOM);
        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue(CommonFractalParameters.PARAM_MIDPOINT));

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
        Number zoom = (Number) systemContext.getParamValue(ShaderSystemContext.PARAM_ZOOM);
        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue(CommonFractalParameters.PARAM_MIDPOINT));

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

        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue(CommonFractalParameters.PARAM_MIDPOINT));
        screenX -= getX()+getWidth()/2;

        NumberFactory nf = systemContext.getNumberFactory();
        Number zoomNumber = (Number) systemContext.getParamValue(ShaderSystemContext.PARAM_ZOOM);
        Number resultNumber = nf.createNumber(screenX);
        Number heightNumber = nf.createNumber(getHeight());
        resultNumber.div(heightNumber);
        resultNumber.mult(zoomNumber);
        resultNumber.add(midpoint.getReal());
        return resultNumber;

//        double zoom = ((Number) systemContext.getParamValue(ShaderSystemContext.PARAM_ZOOM)).toDouble();
//        double midpointReal = midpoint.realDouble();
//        return (screenX-getX()-getWidth()/2)*(zoom/getHeight())+midpointReal;
    }

    @Override
    public Number getImag(float screenY) {
        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue(CommonFractalParameters.PARAM_MIDPOINT));
        screenY -= ((Gdx.graphics.getHeight()-getHeight()-getY()));
        screenY -= getHeight()/2;

        NumberFactory nf = systemContext.getNumberFactory();
        Number zoomNumber = (Number) systemContext.getParamValue(ShaderSystemContext.PARAM_ZOOM);
        Number resultNumber = nf.createNumber(screenY);
        Number heightNumber = nf.createNumber(getHeight());
        resultNumber.div(heightNumber);
        resultNumber.mult(zoomNumber);
        resultNumber.add(midpoint.getImag());
        return resultNumber;

//        double zoom = ((Number) systemContext.getParamValue(ShaderSystemContext.PARAM_ZOOM)).toDouble();
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
            batch.draw(((MainStage)getStage()).getPaletteTexture(MainStage.PARAMS_PALETTE, 0), Gdx.graphics.getWidth() - 300, Gdx.graphics.getHeight() - 300, 250, 250);
            batch.end();
        }
        passthroughShader.end();
    }

    @Override
    public int getPixelCount(){
        return (int)(Math.ceil(getScaledWidth())*Math.ceil(getScaledHeight()));
    }

    @Override
    public void setRefresh(){
        refresh = true;
//        setRefreshColoring();
        super.setRefresh();
    }

    @Override
    public void setRefreshColoring(){
        refreshColoring = true;
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
        ComplexNumber midpoint = getSystemContext().getParamContainer().getParam(CommonFractalParameters.PARAM_MIDPOINT).getGeneral(ComplexNumber.class);
        xPos = midpoint.realDouble();
        yPos = midpoint.imagDouble();
        pannedDeltaX = 0f;
        pannedDeltaY = 0f;
        selectPatternX = 0;
        selectPatternY = 0;
        useFirstDataFbo = true;
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
        paramsChanged = true;
//        systemContext.setParameters(systemContext.getParamContainer());
        paramsChanged(systemContext.getParamContainer());
        rendererContext.setParamContainer(systemContext.getParamContainer());
        for (RendererLink link : rendererContext.getSourceLinks())
            link.syncTargetRenderer();
    }

    public int getSamplesLeft() {
        return (int)Math.ceil(progressiveRenderingMissingFrames);
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
