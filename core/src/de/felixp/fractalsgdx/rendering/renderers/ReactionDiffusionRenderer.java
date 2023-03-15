package de.felixp.fractalsgdx.rendering.renderers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Array;

import java.util.List;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.rendering.rendererparams.DrawParamsReactionDiffusion;
import de.felixp.fractalsgdx.rendering.FractalsFloatFrameBuffer;
import de.felixp.fractalsgdx.rendering.RendererContext;
import de.felixp.fractalsgdx.ui.CollapsiblePropertyListButton;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.infra.SystemContext;

import static de.felixp.fractalsgdx.rendering.renderers.ReactionDiffusionSystemContext.*;

public class ReactionDiffusionRenderer extends AbstractFractalRenderer{

    final static String computeFragmentShaderPath = "ReactionDiffusionFragment.glsl";
    final static String setColorFragmentShaderPath = "SetColorFragment.glsl";
    final static String passthroughVertexShaderPath = "PassthroughVertex130.glsl";
    final static String passthroughFragmentShaderPath = "PassthroughFragment130.glsl";

    ReactionDiffusionSystemContext systemContext;

    FrameBuffer fbo0;
//    FrameBuffer fbo1;

    ShaderProgram computeShader;
    ShaderProgram setColorShader;
    ShaderProgram passthroughShader;

    ShapeRenderer shapeRenderer;

    boolean initTextures = true;
    int drawX = -1;
    int drawY = -1;

    boolean paused = false;
    int lastSteps = 1;

    int simulationWidth = 0;
    int simulationHeight = 0;

    public ReactionDiffusionRenderer(RendererContext rendererContext) {
        super(rendererContext);
    }

    @Override
    public void init() {
//        fbo1 = new FractalsFrameBuffer(fbb);
        compileShaders();
        systemContext = new ReactionDiffusionSystemContext();
        systemContext.addParamListener((newSupp, oldSupp) -> {
            if (newSupp.getUID().equals(PARAM_WIDTH) || newSupp.getUID().equals(PARAM_HEIGHT)){
                simulationWidth = systemContext.getParamContainer().getParam(PARAM_WIDTH).getGeneral(Integer.class);
                simulationHeight = systemContext.getParamContainer().getParam(PARAM_HEIGHT).getGeneral(Integer.class);
                initFbo();
            }
            paramsChanged(systemContext.getParamContainer());
        });
        simulationWidth = systemContext.getParamContainer().getParam(PARAM_WIDTH).getGeneral(Integer.class);
        simulationHeight = systemContext.getParamContainer().getParam(PARAM_HEIGHT).getGeneral(Integer.class);
        initFbo();


        ActorGestureListener gestureListener = new ActorGestureListener(0.001f, 0.4f, 1.1f, Integer.MAX_VALUE) {

            @Override
            public void touchDown(InputEvent event, float x, float y, int pointer, int button) {
                focus();
                drawX = Gdx.input.getX();
                drawY = Gdx.input.getY();
                super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
                drawX = Gdx.input.getX();
                drawY = Gdx.input.getY();
                super.pan(event, x, y, deltaX, deltaY);
            }
        };
        addListener(gestureListener);


    }

    @Override
    protected void initButtons(List<CollapsiblePropertyListButton> list) {
        CollapsiblePropertyListButton resetBtn = new CollapsiblePropertyListButton("clear", "Calculator", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                initFbo();
            }
        });
        list.add(resetBtn);
        CollapsiblePropertyListButton pauseBtn = new CollapsiblePropertyListButton("pause", "Calculator");
        pauseBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int steps = (int)systemContext.getParamValue(PARAM_SIMULATIONSPEED);
                if (paused) {
                    systemContext.getParamContainer().addParam(new StaticParamSupplier(PARAM_SIMULATIONSPEED, lastSteps));
                    pauseBtn.setText("pause");
                } else {
                    lastSteps = steps;
                    systemContext.getParamContainer().addParam(new StaticParamSupplier(PARAM_SIMULATIONSPEED, 0));
                    pauseBtn.setText("resume");
                }
                paused = !paused;
            }
        });
        list.add(pauseBtn);
    }

    protected void initFbo(){
        GLFrameBuffer.FrameBufferBuilder fbb = getFboBuilderData(simulationWidth, simulationHeight);
        if (fbo0 != null)
            fbo0.dispose();
        fbo0 = new FractalsFloatFrameBuffer(fbb);
    }

    protected GLFrameBuffer.FrameBufferBuilder getFboBuilderData(int width, int height){
        GLFrameBuffer.FrameBufferBuilder fboBuilderData = new GLFrameBuffer.FrameBufferBuilder(width, height);
//        fboBuilderData.addBasicColorTextureAttachment(Pixmap.Format.RGB888);
//        fboBuilderData.addBasicColorTextureAttachment(Pixmap.Format.RGB888);
        fboBuilderData.addFloatAttachment(GL30.GL_RGBA32F, GL30.GL_RGBA, GL30.GL_FLOAT, false);
        fboBuilderData.addFloatAttachment(GL30.GL_RGBA32F, GL30.GL_RGBA, GL30.GL_FLOAT, false);
        return fboBuilderData;
    }

    protected void compileShaders(){
        ShaderProgram newComputeShader = compileShader(passthroughVertexShaderPath, computeFragmentShaderPath);
        computeShader = newComputeShader;
        setColorShader = compileShader(passthroughVertexShaderPath, setColorFragmentShaderPath);
        passthroughShader = compileShader(passthroughVertexShaderPath, passthroughFragmentShaderPath);
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
    }

    @Override
    public int getPixelCount() {
        return 0;
    }

    @Override
    protected void zoom(Number zoomFactor) {

    }

    private void prepareShapeRenderer(Batch batch) {
        if (shapeRenderer == null)
            shapeRenderer = new ShapeRenderer(1000);

        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());

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

    Matrix4 projectionMatrix = new Matrix4();

    @Override
    public void render(Batch batch, float parentAlpha) {

        FrameBuffer fboRender = fbo0;

        batch.setShader(setColorShader);
        fboRender.begin();

//        prepareShapeRenderer(batch);
        if (initTextures) {
            initTextures = false;
            Texture tex1 = fboRender.getTextureAttachments().get(1);
            Texture tex0 = fboRender.getTextureAttachments().get(0);
            tex1.bind(1);
            setColorShader.setUniformi("texture1", 1);
            tex0.bind(0);
            setColorShader.setUniformi("texture0", 0);

            setColorShader.setUniformi("channel", 0);
            setColorShader.setUniformf("color", 1f, 0f, 0f, 1f);
            batch.draw(tex0, 0, 0, getWidth(), getHeight());
            batch.flush();
        }
        if (drawX != -1) {
            float sizeFactor = 0.5f*(Gdx.graphics.getWidth()/1280f + Gdx.graphics.getHeight()/720f);
            Texture tex0 = fboRender.getTextureAttachments().get(0);
            setColorShader.setUniformi("channel", 1);
            setColorShader.setUniformf("color", 1f, 0f, 0f, 1f);
            batch.draw(tex0, drawX - 2f*sizeFactor, (Gdx.graphics.getHeight()-drawY) - 2f*sizeFactor, 4f*sizeFactor, 4f*sizeFactor);
            batch.flush();
            drawX = -1;
            drawY = -1;
        }

        batch.flush();



        fboRender.end();
        batch.setShader(computeShader);
        int loops = (int)systemContext.getParamValue(PARAM_SIMULATIONSPEED);
        for (int i = 0 ; i < loops ; i++) {
            fboRender.begin();

            Texture tex1 = fboRender.getTextureAttachments().get(1);
            tex1.bind(1);
            computeShader.setUniformi("textureG", 1);
            Texture tex0 = fboRender.getTextureAttachments().get(0);
            tex0.bind(0);
            computeShader.setUniformi("textureR", 0);

            //apply current fbo resolution to batch
            projectionMatrix.setToOrtho2D(0, 0, tex0.getWidth(), tex0.getHeight());
            batch.setProjectionMatrix(projectionMatrix);

            computeShader.setUniformf("timeStep", 1f);
            computeShader.setUniformf("resolution", tex0.getWidth(), tex0.getHeight());
            computeShader.setUniformf("ratio", tex0.getWidth()/tex0.getHeight());
            computeShader.setUniformf("diffRate1", (float)((Number)systemContext.getParamValue(PARAM_DIFFUSIONRATE_1)).toDouble());
            computeShader.setUniformf("diffRate2", (float)((Number)systemContext.getParamValue(PARAM_DIFFUSIONRATE_2)).toDouble());
            computeShader.setUniformf("reactionRate", (float)((Number)systemContext.getParamValue(PARAM_REACTIONRATE)).toDouble());
            computeShader.setUniformf("feedRateConst", (float)((Number)systemContext.getParamValue(PARAM_FEEDRATE)).toDouble());
            computeShader.setUniformf("feedRateVarFactorX", 0.2f);
            computeShader.setUniformf("feedRateVarFactorY", 0f);
            computeShader.setUniformf("killRateConst", (float)((Number)systemContext.getParamValue(PARAM_KILLRATE)).toDouble());
            computeShader.setUniformf("killRateVarFactorX", 0f);
            computeShader.setUniformf("killRateVarFactorY", 0.1f);

            batch.draw(tex0, 0, 0, tex0.getWidth(), tex0.getHeight(), 0, 0, tex0.getWidth(), tex0.getHeight(), false, true);

            batch.flush();
            fboRender.end();
        }
        batch.setShader(passthroughShader);
        projectionMatrix.setToOrtho2D(0, 0, getWidth(), getHeight());
        batch.setProjectionMatrix(projectionMatrix);

        ParamContainer clientParams = FractalsGdxMain.mainStage.getParamUI().getClientParamsSideMenu().getParamContainer();

        String bufferParamValue = clientParams.getParam(DrawParamsReactionDiffusion.PARAM_SOURCEBUFFER).getGeneral(String.class);
        int displayBuffer = 0;
        if (bufferParamValue.equals(DrawParamsReactionDiffusion.OPTIONVALUE_SOURCEBUFFER_1))
            displayBuffer = 1;

        Texture renderedTexture = fboRender.getTextureAttachments().get(displayBuffer);
        renderedTexture.bind(0);
        batch.draw(renderedTexture, getX(), getY(), getWidth(), getHeight(), 0, 0, renderedTexture.getWidth(), renderedTexture.getHeight(), false, true);

//        debugDrawFBOs(batch);
        batch.flush();
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setProjectionMatrix(projectionMatrix);

        if (isScreenshot(true))
            makeScreenshot();
    }

    private void debugDrawFBOs(Batch batch) {

        Array<Texture> textureAttachments = fbo0.getTextureAttachments();
        if (textureAttachments.size == 1)
            return;

        Texture tex0 = fbo0.getTextureAttachments().get(0);
        TextureRegion texReg0 = new TextureRegion(tex0, 0, 0, (int) tex0.getWidth(), (int) tex0.getHeight());
        texReg0.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        texReg0.flip(false, true);
        batch.draw(texReg0, getX(), getY() + getHeight() * 2 / 3, getWidth() / 3, getHeight() / 3);

        Texture tex1 = fbo0.getTextureAttachments().get(1);
        TextureRegion texReg1 = new TextureRegion(tex1, 0, 0, (int) tex0.getWidth(), (int) tex0.getHeight());
        texReg1.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        texReg1.flip(false, true);
        batch.draw(texReg1, getX()+getWidth()/3, getY() + getHeight() * 2 / 3, getWidth() / 3, getHeight() / 3);
    }

    @Override
    public float getScreenX(double real) {
        return 0;
    }

    @Override
    public float getScreenY(double imag) {
        return 0;
    }

    @Override
    public float getScreenX(Number real) {
        return 0;
    }

    @Override
    public float getScreenY(Number imag) {
        return 0;
    }

    @Override
    public Number getReal(float screenX) {
        return null;
    }

    @Override
    public Number getImag(float screenY) {
        return null;
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
