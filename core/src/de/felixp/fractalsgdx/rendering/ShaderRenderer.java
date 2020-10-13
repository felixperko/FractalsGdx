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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.interpolation.ParameterInterpolation;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.fractals.data.AbstractArrayChunk;
import de.felixperko.fractals.data.ArrayChunkFactory;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.data.ReducedNaiveChunk;
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
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.system.systems.infra.ViewData;
import de.felixperko.fractals.system.systems.stateinfo.TaskState;
import de.felixperko.fractals.system.systems.stateinfo.TaskStateInfo;
import de.felixperko.fractals.system.task.FractalsTask;
import de.felixperko.fractals.system.task.TaskManager;
import de.felixperko.fractals.system.thread.CalculateFractalsThread;
import de.felixperko.fractals.util.NumberUtil;
import de.felixperko.fractals.util.expressions.ComputeExpressionBuilder;

public class ShaderRenderer extends AbstractFractalRenderer {

    private static Logger LOG = LoggerFactory.getLogger(ShaderRenderer.class);

    final static String shader1 = "CalcExpressionFragmentTemplate.glsl";
    final static String shader2 = "SobelDecodeFragment.glsl";

    boolean refresh = true;
    boolean paramsChanged = false;

    boolean screenshot = false;

    FrameBuffer fbo_data;
    FrameBuffer fbo_image;

    int currentRefreshes = 0;
    float decode_factor = 1f;

    ShaderProgram computeShader;
    ShaderProgram coloringShader;
    ShaderProgram passthroughShader;

    Matrix3 matrix = new Matrix3(new float[] {1,0,0, 0,1,0, 0,0,1, 0,0,0});

    float colorShift = 0f;

    boolean burningship = false;
    boolean juliaset = false;

    float xPos;
    float yPos;
//    float biasReal = 0f;
//    float biasImag = 0f;

    int width;
    int height;

    double resolutionScale = 0.5;
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

    public ShaderRenderer(RendererContext rendererContext){
        super(rendererContext);
    }

    public ShaderRenderer(RendererContext rendererContext, boolean juliaset){
        super(rendererContext);
        this.juliaset = juliaset;
    }

    @Override
    protected String processShadertemplateLine(String templateLine) {
        return shaderBuilder.processShadertemplateLine(templateLine, getScaledHeight());
    }

    boolean isFocused = false;

    @Override
    public void init(){
//		palette = new Texture("palette.png");
//		palette.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        fbo_data = new FrameBuffer(Pixmap.Format.RGBA8888, (int)getScaledWidth(), (int)getScaledHeight(), false);
        fbo_image = new FrameBuffer(Pixmap.Format.RGBA8888, (int)getScaledWidth(), (int)getScaledHeight(), false);

        ((GPUSystemContext)systemContext).init();
        updateExpression();
        setupShaders();

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

                if (factor != null)
                    zoom.mult(factor);

                if (changed) {
                    anchor = paramContainer.getClientParameter("midpoint").getGeneral(ComplexNumber.class);
                    reset();
                }
            }



            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
                move(deltaX, deltaY);
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

            ParameterInterpolation zoomInterpolation = null;
            boolean disableContinuousRendering = false;

//            @Override
//            public void touchDown(InputEvent event, float x, float y, int pointer, int button) {
//                float currentZoom = (float)paramContainer.getClientParameter("zoom").getGeneral(Number.class).toDouble();
//                if (zoomInterpolation == null) {
//                    zoomInterpolation = new LinearNumberParameterInterpolation("zoom", 10, currentZoom+"", currentZoom/10000000000d+""){
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
//                    addParameterInterpolationServer(zoomInterpolation);
//                    if (!Gdx.graphics.isContinuousRendering()) {
//                        Gdx.graphics.setContinuousRendering(true);
//                        disableContinuousRendering = true;
//                    }
//                }
//            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
//                if (zoomInterpolation != null){
//                    addParameterInterpolationServer(zoomInterpolation);
//                    zoomInterpolation = null;
//                    if (disableContinuousRendering) {
//                        Gdx.graphics.setContinuousRendering(false);
//                        disableContinuousRendering = false;
//                    }
//                }
            }
        });
    }

    float getScaledWidth(){
        return (float) (getWidth()*resolutionScale);
    }

    float getScaledHeight(){
        return (float) (getHeight()*resolutionScale);
    }

    public void updateExpression(){
//        String expr =
//                "cos(z)^2+c"
//                "absr(negatei(z))^2+c"
//                ;
        String newExpressionString = (String) systemContext.getParamValue("f(z)=", String.class);

        boolean update = expressionString == null || !newExpressionString.equals(expressionString) || paramsChanged;
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
                if (expressionChanged) {
                    shaderBuilder = new ShaderBuilder(expression, systemContext);
                    setupShaders();
                }
                if (paramsChanged || expressionChanged)
                    setRefresh();
            } catch (IllegalArgumentException e){
                LOG.info("Couldn't parse expression: \n"+e.getMessage());
            }
        }
        paramsChanged = false;
    }

    protected void move(float deltaX, float deltaY) {
        if (deltaX == 0 && deltaY == 0)
            return;
        xPos += deltaX;
        yPos += deltaY;
        ParamContainer paramContainer = systemContext.getParamContainer();
        NumberFactory nf = paramContainer.getClientParameter("numberFactory").getGeneral(NumberFactory.class);
        Number zoom = paramContainer.getClientParameter("zoom").getGeneral(Number.class);
        ComplexNumber midpoint = paramContainer.getClientParameter("midpoint").getGeneral(ComplexNumber.class);
        ComplexNumber delta = nf.createComplexNumber(deltaX, -deltaY);
        delta.divNumber(nf.createNumber(getHeight()));
        delta.multNumber(zoom);
        midpoint.sub(delta);
        systemContext.setMidpoint(midpoint);
        ((MainStage) FractalsGdxMain.stage).getParamUI().setServerParameterConfiguration(this, paramContainer, ((GPUSystemContext) systemContext).paramConfiguration);
        rendererContext.panned(systemContext.getParamContainer());
    }

    public void setupShaders() {
        ShaderProgram.pedantic = false;
        String vertexPassthrough = "PassthroughVertex.glsl";
        computeShader = compileShader(vertexPassthrough, shader1);
        coloringShader = compileShader(vertexPassthrough, shader2);
        passthroughShader = compileShader(vertexPassthrough, "PassthroughFragment.glsl");

//        width = Gdx.graphics.getWidth();
//        height = Gdx.graphics.getHeight();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (fbo_data == null) //init() not called
            return;

        applyParameterInterpolations(systemContext.getParamContainer(), null, systemContext.getNumberFactory());

        handleInput();

        updateExpression();

        boolean changing = !rendererContext.getParameterInterpolationsServer().isEmpty() || !rendererContext.getParameterInterpolationsClient().isEmpty();
        if (changing)
            setRefresh();

        renderImage(batch);

    }

    float panSpeed = 0.25f;
    float maxTimestep = 2.0f/Gdx.graphics.getDisplayMode().refreshRate;

    private void handleInput() {
        if (((MainStage)getStage()).getFocusedRenderer() != this)
            return;
        if (!isFocused)
            return;
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
        float deltaTime = Math.min(Gdx.graphics.getDeltaTime(), maxTimestep);
        move(multX*panSpeed* deltaTime *getHeight(), multY*panSpeed* deltaTime *getHeight());
    }

    private void setShaderUniforms() {
        float currentWidth = getWidth();
        float currentHeight = getHeight();

//        setColoringParams();

        ParamContainer paramContainer = systemContext.getParamContainer();

        computeShader.setUniformMatrix("u_projTrans", matrix);
        computeShader.setUniformf("ratio", currentWidth/(float)currentHeight);
        computeShader.setUniformf("resolution", getScaledWidth(), getScaledHeight());

        computeShader.setUniformf("colorShift", colorShift);

//		long t = System.currentTimeMillis();
//		if (t-lastIncrease > 1) {
//			lastIncrease = t;
//			iterations++;
//		}
        computeShader.setUniformi("iterations", paramContainer.getClientParameter("iterations").getGeneral(Integer.class));
        computeShader.setUniformf("limit", (float)(double)paramContainer.getClientParameter("limit").getGeneral(Double.class));

        float scale = (float)(paramContainer.getClientParameter("zoom").getGeneral(Number.class).toDouble());
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
                params[i*3+2] = scale;
            }
            else
                throw new IllegalArgumentException("Unsupported ParamSupplier "+supp.getName()+": "+supp.getClass().getName());
        }
        computeShader.setUniform1fv("params", params, 0, params.length);

        //TODO remove instance variable
        computeShader.setUniformf("scale", scale);

        float xVariation = (float)((Math.random()-0.5)*(scale/width));
        float yVariation = (float)((Math.random()-0.5)*(scale/width));

        xVariation = 0;
        yVariation = 0;

//        ComplexNumber c = paramContainer.getClientParameter("c").getGeneral(ComplexNumber.class);

        computeShader.setUniformf("center", (float) midpoint.getReal().toDouble() + xVariation, (float) midpoint.getImag().toDouble() + yVariation);
//        computeShader.setUniformf("biasReal", (float)c.getReal().toDouble());
//        computeShader.setUniformf("biasImag", (float)c.getImag().toDouble());
        computeShader.setUniformf("biasReal", (float)0); //TODO remove
        computeShader.setUniformf("biasImag", (float)0);
        computeShader.setUniformf("burningship", 0f);
        computeShader.setUniformf("juliaset", 0f);
        computeShader.setUniformf("samples", currentRefreshes+1);
        computeShader.setUniformf("flip", currentRefreshes%2 == 1 ? -1 : 1);
        computeShader.setUniformf("logPow", (float)expression.getSmoothstepConstant());
        //computeShader.setUniformi("u_texture", 0);
        //palette.bind(1);
//        computeShader.setUniformi("palette", 0);
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
        super.setColoringParams(coloringShader, xPos, yPos, resolutionX, resolutionY, (MainStage)getStage(), getSystemContext(), getRendererContext(), anchor);
    }

    protected void setResolutionScale(double resolutionScale){
        if (this.resolutionScale != resolutionScale) {
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
        if (fbo_data != null) {
            fbo_data.dispose();
            fbo_image.dispose();
        }
        fbo_data = new FrameBuffer(Pixmap.Format.RGBA8888, resolutionX, resolutionY, false);
        fbo_image = new FrameBuffer(Pixmap.Format.RGBA8888, resolutionX, resolutionY, false);
        setRefresh();
    }

    protected void renderImage(Batch batch) {

        setResolutionScale((double)systemContext.getParamValue("resolutionScale", Double.class));

        Gdx.gl.glClearColor( 0, 0, 0, 1 );

        updateMousePos();

        fbo_data.begin();

        //apply current renderer resolution to batch
        Matrix4 matrix = new Matrix4();
        matrix.setToOrtho2D(0, 0, resolutionX, resolutionY); // here is the actual size you want
        batch.setProjectionMatrix(matrix);

//        System.out.println("ShaderRenderer.renderImage() id="+id+" refresh="+refresh);

        if (refresh) {
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            currentRefreshes = 0;
            decode_factor = 1f;
        }
        batch.end();

//        if (currentRefreshes < sampleLimit){
        if (refresh) {
            refresh = false;
            currentRefreshes++;

            computeShader.begin();
            setShaderUniforms();
            batch.begin();
            batch.setShader(computeShader);

            Color c = batch.getColor();
            batch.setColor(c.r, c.g, c.b, 1.0f);

            Texture tex = fbo_data.getColorBufferTexture();
            TextureRegion texReg = new TextureRegion(tex);
            texReg.flip(false, true);
            batch.draw(texReg, 0, 0, fbo_data.getWidth(), fbo_data.getHeight());
            batch.end();

            computeShader.end();

//            drawPalette(batch);

            fbo_data.end();

            //TODO fbo_traces, encode iterates of current formula (at cursor)
        }

            //Pass 2: render fbo_data content to fbo_image using coloringShader
            fbo_image.begin();

//            if (refresh)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            coloringShader.begin();
            setColoringParams();

            batch.begin();
            batch.setShader(coloringShader);

            //Texture texture = fbo_data.getColorBufferTexture();
            //TextureRegion textureRegion = new TextureRegion(texture);
            //textureRegion.flip(false, true);

            //palette.bind();

//            batch.draw(fbo_data.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.draw(fbo_data.getColorBufferTexture(), 0, 0, fbo_image.getWidth(), fbo_image.getHeight());
//			batch.disableBlending();
            batch.end();

            coloringShader.end();
            fbo_image.end();

//        }

        matrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); // here is the actual size you want
        batch.setProjectionMatrix(matrix);

        batch.begin();
        batch.setShader(passthroughShader);
        Texture tex2 = fbo_image.getColorBufferTexture();
        TextureRegion texReg2 = new TextureRegion(tex2, 0, 0, (int)fbo_image.getWidth(), (int)fbo_image.getHeight());
        texReg2.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        texReg2.flip(false, true);
        batch.draw(texReg2, getX(), getY(), getWidth(), getHeight());

        batch.flush();

        if (drawTraces) {
            batch.end();

            drawTraceShapeRenderer(batch);

            batch.begin();
        }

//        batch.draw(texReg2, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//        batch.end();
//        super.draw(batch, parentAlpha);
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

    ShapeRenderer traceShapeRenderer = null;
    int traceArrayFilledSize = 0;
    double[] tracesReal = null;
    double[] tracesImag = null;
    EscapeTimeCpuCalculatorNew traceCalculator = new EscapeTimeCpuCalculatorNew();

    protected void drawTraceShapeRenderer(Batch batch) {

        ParamContainer clientParams = ((MainStage)getStage()).getClientParameters();
        float lineWidth = (float)(double)clientParams.getClientParameter(MainStage.PARAMS_TRACES_LINE_WIDTH).getGeneral(Double.class);
        float pointSize = (float)(double)clientParams.getClientParameter(MainStage.PARAMS_TRACES_POINT_SIZE).getGeneral(Double.class);
        int traceCount = clientParams.getClientParameter(MainStage.PARAMS_TRACES_ITERATIONS).getGeneral(Integer.class);
        boolean disabled = traceCount < 1 || (lineWidth <= 0 && pointSize <= 0);
        if (disabled)
            return;

        updateTraceArrays();
        drawTraces(batch, clientParams, lineWidth, pointSize);
    }

    float timePassed = 0;

    private void updateTraceArrays() {

        ParamContainer clientParams = ((MainStage)getStage()).getClientParameters();
        int traceCount = clientParams.getClientParameter(MainStage.PARAMS_TRACES_ITERATIONS).getGeneral(Integer.class);

        //prepare calculator
        AbstractArrayChunk traceChunk = new TraceChunk();
        traceChunk.setCurrentTask(new TraceTask(systemContext));
        String posVarName = clientParams.getClientParameter(MainStage.PARAMS_TRACES_VARIABLE).getGeneral(String.class);
        ParamSupplier posVar = systemContext.getParamContainer().getClientParameter(posVarName);
        boolean useMousePos = posVarName.equals("mouse") || posVar == null || !(posVar instanceof StaticParamSupplier) || (((StaticParamSupplier)posVar).getGeneral() instanceof ComplexNumber);
        if (useMousePos)
            traceChunk.chunkPos = getComplex(mouseX, mouseY);
        else {
            float time = ((timePassed/100) % 1)*getWidth();
            traceChunk.chunkPos = getComplex(getWidth()-time, mouseY);
            timePassed += Gdx.graphics.getDeltaTime();
        }
        traceCalculator.setContext(systemContext);
        traceCalculator.setTraceCount(traceCount);

        //calculate traces
        traceCalculator.calculate(traceChunk, null, null);

        //get results
        double[][] traces = traceCalculator.getTraces();
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

    private void drawTraces(Batch batch, ParamContainer clientParams, float lineWidth, float pointSize) {
        //prepare trace renderer
        if (traceShapeRenderer == null)
            traceShapeRenderer = new ShapeRenderer(64);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        traceShapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());

        Rectangle scissors = new Rectangle();
        Rectangle clipBounds = new Rectangle(getX(),getY(),getWidth(),getHeight());
        ScissorStack.calculateScissors(getStage().getCamera(), batch.getTransformMatrix(), clipBounds, scissors);
        ScissorStack.pushScissors(scissors);

        traceShapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

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
            traceShapeRenderer.setColor(r, g, b, lineTransparency);

            //draw trace line
            if (i > 0 && lineWidth > 0) {
                float xPrev = getScreenX(tracesReal[i - 1]);
                float yPrev = getScreenY(tracesImag[i - 1]);

                traceShapeRenderer.rectLine(x, y, xPrev, yPrev, lineWidth);
            }

            //draw z_i
            if (pointSize > 0) {
                if (pointTransparency != lineTransparency)
                    traceShapeRenderer.setColor(r, g, b, pointTransparency);

                traceShapeRenderer.circle(x, y, pointSize);
            }
        }

        traceShapeRenderer.end();
        ScissorStack.popScissors();
    }

    private float getScreenX(double real) {
        double zoom = ((Number) systemContext.getParamValue("zoom")).toDouble();
        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue("midpoint"));
        double midpointReal = midpoint.realDouble();
        return (float) (getX() + (real-midpointReal)/(zoom/getHeight()) + getWidth()/2);
    }

    private float getScreenY(double imag) {
        double zoom = ((Number) systemContext.getParamValue("zoom")).toDouble();
        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue("midpoint"));
        double midpointImag = midpoint.imagDouble();
        return (float) (getY() + getHeight()-((imag-midpointImag)/(zoom/getHeight()) + getHeight()/2));
    }

    private ComplexNumber getComplex(float screenX, float screenY){
        return systemContext.getNumberFactory().createComplexNumber(getReal(screenX), getImag(screenY));
    }

    private double getReal(float screenX) {
        double zoom = ((Number) systemContext.getParamValue("zoom")).toDouble();
        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue("midpoint"));
        double midpointReal = midpoint.realDouble();
        return (screenX-getX()-getWidth()/2)*(zoom/getHeight())+midpointReal;
    }

    private double getImag(float screenY) {
        double zoom = ((Number) systemContext.getParamValue("zoom")).toDouble();
        ComplexNumber midpoint = ((ComplexNumber)systemContext.getParamValue("midpoint"));
        double midpointImag = midpoint.imagDouble();
        return ((screenY-(Gdx.graphics.getHeight()-getHeight()-getY()))-getHeight()/2)*(zoom/getHeight())+midpointImag;
    }

    /** for testing the palette */
    private void drawPalette(Batch batch) {
        computeShader.begin();
        computeShader.setUniformf("scale", 5);
        computeShader.setUniformf("center", (float) 0, (float) 0);
        computeShader.setUniformf("resolution", (float) 250, (float) 250);
        if (Gdx.graphics.getWidth() > 600 && Gdx.graphics.getHeight() > 600) {
            batch.begin();
//            batch.draw(palette, Gdx.graphics.getWidth() - 300, Gdx.graphics.getHeight() - 300, 250, 250);
            batch.end();
        }
        computeShader.end();
    }

    public void setRefresh(){
        refresh = true;
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
    public boolean isScreenshot(boolean reset) {
        boolean curr = screenshot;
        if (reset)
            screenshot = false;
        return curr;
    }

    @Override
    public SystemContext getSystemContext() {
        return systemContext;
    }

    @Override
    public void reset() {
        setRefresh();
    }

    @Override
    public void removed() {
        //TODO
    }

    @Override
    public void setScreenshot(boolean screenshot) {
        this.screenshot = screenshot;
    }

    public void paramsChanged() {
        paramsChanged = true;
        super.rendererContext.setParamContainer(systemContext.getParamContainer());
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
