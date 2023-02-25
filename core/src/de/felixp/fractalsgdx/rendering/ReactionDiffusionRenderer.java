package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Array;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.params.DrawParamsReactionDiffusion;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.systems.infra.SystemContext;

import static de.felixp.fractalsgdx.rendering.ReactionDiffusionSystemContext.*;

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

    public ReactionDiffusionRenderer(RendererContext rendererContext) {
        super(rendererContext);
    }

    @Override
    public void init() {
        GLFrameBuffer.FrameBufferBuilder fbb = getFboBuilderData((int)getWidth(), (int)getHeight());
        fbo0 = new FractalsFloatFrameBuffer(fbb);
//        fbo1 = new FractalsFrameBuffer(fbb);
        compileShaders();
        systemContext = new ReactionDiffusionSystemContext();
        systemContext.addParamListener((newSupp, oldSupp) -> {

        });


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
            computeShader.setUniformi("texture1", 1);
            tex0.bind(0);
            computeShader.setUniformi("texture0", 0);

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
            Texture texG = fboRender.getTextureAttachments().get(1);
            texG.bind(1);
            computeShader.setUniformi("textureG", 1);
            Texture texR = fboRender.getTextureAttachments().get(0);
            texR.bind(0);
            computeShader.setUniformi("textureR", 0);

            computeShader.setUniformf("timeStep", 1f);
            computeShader.setUniformf("diffRate1", (float)((Number)systemContext.getParamValue(PARAM_DIFFUSIONRATE_1)).toDouble());
            computeShader.setUniformf("diffRate2", (float)((Number)systemContext.getParamValue(PARAM_DIFFUSIONRATE_2)).toDouble());
            computeShader.setUniformf("reactionRate", (float)((Number)systemContext.getParamValue(PARAM_REACTIONRATE)).toDouble());
            computeShader.setUniformf("feedRate", (float)((Number)systemContext.getParamValue(PARAM_FEEDRATE)).toDouble());
            computeShader.setUniformf("killRate", (float)((Number)systemContext.getParamValue(PARAM_KILLRATE)).toDouble());

            batch.draw(texR, 0, 0, getWidth(), getHeight(), 0, 0, texR.getWidth(), texR.getHeight(), false, true);

            batch.flush();
            fboRender.end();
        }
        batch.setShader(passthroughShader);

        ParamContainer clientParams = FractalsGdxMain.mainStage.getParamUI().getClientParamsSideMenu().getParamContainer();

        String bufferParamValue = clientParams.getParam(DrawParamsReactionDiffusion.PARAM_SOURCEBUFFER).getGeneral(String.class);
        int displayBuffer = 0;
        if (bufferParamValue.equals(DrawParamsReactionDiffusion.OPTIONVALUE_SOURCEBUFFER_1))
            displayBuffer = 1;

        Texture renderedTexture = fboRender.getTextureAttachments().get(displayBuffer);
        renderedTexture.bind(0);
        batch.draw(renderedTexture, 0, 0, getWidth(), getHeight(), 0, 0, renderedTexture.getWidth(), renderedTexture.getHeight(), false, true);

//        debugDrawFBOs(batch);
        batch.flush();

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
