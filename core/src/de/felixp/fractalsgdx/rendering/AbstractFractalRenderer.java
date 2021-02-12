package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.ScreenUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.systems.infra.SystemContext;

abstract class AbstractFractalRenderer extends WidgetGroup implements FractalRenderer {

    private static Logger LOG = LoggerFactory.getLogger(AbstractFractalRenderer.class);
    private static int ID_COUNTER = 1;

    protected static void setColoringParams(ShaderProgram shader, float width, float height, MainStage stage, SystemContext systemContext, RendererContext rendererContext) {
//        rendererContext.applyParameterAnimations(systemContext, systemContext.getParamContainer(), stage.getClientParameters(), systemContext.getNumberFactory());
        shader.setUniformi("usePalette", stage.getClientParameter(MainStage.PARAMS_COLOR_USE_PALETTE).getGeneral(Boolean.class) ? 1 : 0);
        shader.setUniformf("colorAdd", (float)(double)stage.getClientParameter(MainStage.PARAMS_COLOR_ADD).getGeneral(Double.class));
        shader.setUniformf("colorMult", (float)(double)stage.getClientParameter(MainStage.PARAMS_COLOR_MULT).getGeneral(Double.class));
        shader.setUniformf("colorSaturation", (float)(double)stage.getClientParameter(MainStage.PARAMS_COLOR_SATURATION).getGeneral(Double.class));
//        shader.setUniformf("sobelLuminance", (float)(double)stage.getClientParameter(MainStage.PARAMS_SOBEL_FACTOR).getGeneral(Double.class));
        shader.setUniformf("sobel_ambient", (float)(double)stage.getClientParameter(MainStage.PARAMS_AMBIENT_GLOW).getGeneral(Double.class));
        shader.setUniformf("sobel_magnitude", (float)(double)stage.getClientParameter(MainStage.PARAMS_SOBEL_GLOW_LIMIT).getGeneral(Double.class));
        shader.setUniformf("sobelPeriod", (float)(double)stage.getClientParameter(MainStage.PARAMS_SOBEL_DIM_PERIOD).getGeneral(Double.class));
        shader.setUniformi("extractChannel", (int)(int)stage.getClientParameter(MainStage.PARAMS_EXTRACT_CHANNEL).getGeneral());
        shader.setUniformf("mappingColorR", (float)(double)stage.getClientParameter(MainStage.PARAMS_MAPPING_COLOR_R).getGeneral(Double.class));
        shader.setUniformf("mappingColorG", (float)(double)stage.getClientParameter(MainStage.PARAMS_MAPPING_COLOR_G).getGeneral(Double.class));
        shader.setUniformf("mappingColorB", (float)(double)stage.getClientParameter(MainStage.PARAMS_MAPPING_COLOR_B).getGeneral(Double.class));
//        shader.setUniformf("colorAdd", (float)(double)stage.colorAddSupplier.getGeneral(Double.class) + timeCounter*-0.2f);
//        shader.setUniformf("colorMult", (float)(double)stage.colorMultSupplier.getGeneral(Double.class));
//        shader.setUniformf("sobelLuminance", (float)(double)stage.glowFactorSupplier.getGeneral(Double.class));

        shader.setUniformf("resolution", width, height);
    }

    int position = -1;

    //TODO replace with position?
    int id = ID_COUNTER++;

    boolean singleScreenshotScheduled;
    boolean recordingScreenshots;

    RendererContext rendererContext;

    public AbstractFractalRenderer(RendererContext rendererContext){
        this.rendererContext = rendererContext;
//        this.relativeX = relativeX;
//        this.relativeY = relativeY;
//        this.relativeWidth = relativeWidth;
//        this.relativeHeight = relativeHeight;
    }

    public ShaderProgram compileShader(String vertexPath, String fragmentPath){
//        ShaderProgram shader = new ShaderProgram(Gdx.files.internal(vertexPath),
//                Gdx.files.internal(fragmentPath));
        ShaderProgram shader = null;
        String vertexString = null;
        String fragmentString = null;
//        try {
            FileHandle vertexTemplateHandle = Gdx.files.internal(vertexPath);
            FileHandle fragmentTemplateHandle = Gdx.files.internal(fragmentPath);
            String vertexTemplate = vertexTemplateHandle.readString();
            String fragmentTemplate = fragmentTemplateHandle.readString();
            vertexString = fillShaderTemplate(Arrays.asList(vertexTemplate));
            fragmentString = fillShaderTemplate(Arrays.asList(fragmentTemplate));
//            List<String> vertexStringTemplate = Files.readAllLines(Paths.get(vertexShaderPath));
//            List<String> fragmentStringTemplate = Files.readAllLines(Paths.get(fragmentShaderPath));
//            LOG.warn("compiling shaders "+vertexShaderPath+", "+fragmentShaderPath);
//            List<String> vertexStringTemplate = Files.readAllLines(Paths.get(vertexShaderPath));
//            List<String> fragmentStringTemplate = Files.readAllLines(Paths.get(fragmentShaderPath));
//            vertexString = fillShaderTemplate(vertexStringTemplate);
//            fragmentString = fillShaderTemplate(fragmentStringTemplate);
            shader = new ShaderProgram(vertexString, fragmentString);
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
        if (!shader.isCompiled()) {
            System.out.println("----VERTEX----");
            System.out.println(vertexString);
            System.out.println("---FRAGMENT---");
            System.out.println(fragmentString);
            throw new IllegalStateException("Error compiling shaders ("+vertexPath+", "+fragmentPath+"): "+shader.getLog());
        }
        return shader;
    }

    protected String fillShaderTemplate(List<String> vertexStringTemplate) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String templateLine : vertexStringTemplate){
            String line = processShadertemplateLine(templateLine);
            stringBuilder.append(line).append("\n");
        }
        return stringBuilder.toString();
    }

    /**
     * Can be overwritten to modify the shader string before compiling.
     * //TODO refresh functionality
     * @param templateLine the line from the shader template
     * @return the parsed line to compile
     */
    protected String processShadertemplateLine(String templateLine) {
        return templateLine;
    }

    @Override
    public RendererContext getRendererContext() {
        return rendererContext;
    }

    //    boolean refresh = true;

    public abstract void reset();

    @Override
    public void setSingleScreenshotScheduled(boolean singleScreenshotScheduled) {
        this.singleScreenshotScheduled = singleScreenshotScheduled;
    }

    @Override
    public void setScreenshotRecording(boolean screenshotRecording) {
        this.recordingScreenshots = screenshotRecording;
    }

    @Override
    public boolean isScreenshot(boolean resetSingle) {
        if (recordingScreenshots)
            return true;
        boolean curr = singleScreenshotScheduled;
        if (resetSingle)
            singleScreenshotScheduled = false;
        return curr;
    }

    @Override
    public void setRefresh(){
//        this.refresh = true;
        if (!Gdx.graphics.isContinuousRendering())
            Gdx.graphics.requestRendering();
    }

    @Override
    public float getPrefWidth() {
        return getRelativeWidth()*Gdx.graphics.getWidth();
    }

    @Override
    public float getPrefHeight() {
        return getRelativeHeight()*Gdx.graphics.getHeight();
    }

    @Override
    public float getPrefX(){
        return getRelativeX()*Gdx.graphics.getWidth();
    }

    @Override
    public float getPrefY(){
        return getRelativeY()*Gdx.graphics.getHeight();
    }

    @Override
    public void updateSize(){
        setBounds(getPrefX(), getPrefY(), getPrefWidth(), getPrefHeight());
    }

    protected synchronized void makeScreenshot(){

        long t1 = System.nanoTime();

        //get pixels
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

        long t2 = System.nanoTime();

        // this loop makes sure the whole screenshot is opaque and looks exactly like what the user is seeing
//        for(int i = 4; i < pixels.length; i += 4) {
//            pixels[i - 1] = (byte) 255;
//        }

        rendererContext.madeScreenshot(pixels);

        long t3 = System.nanoTime();
        System.out.println("Screenshot processing times: "+(t2-t1)+", "+(t3-t2));
    }

    public abstract double getXShift();
    public abstract double getYShift();

    @Override
    public int getId() {
        return id;
    }

    public float getRelativeX() {
        return rendererContext.getProperties().getX();
    }

    public void setRelativeX(float relativeX) {
        rendererContext.getProperties().setX(relativeX);
        updateSize();
    }

    public float getRelativeY() {
        return rendererContext.getProperties().getY();
    }

    public void setRelativeY(float relativeY) {
        rendererContext.getProperties().setY(relativeY);
        updateSize();
    }

    public float getRelativeWidth() {
        return rendererContext.getProperties().getW();
    }

    public void setRelativeWidth(float relativeWidth) {
        rendererContext.getProperties().setW(relativeWidth);
        updateSize();
    }

    public float getRelativeHeight() {
        return rendererContext.getProperties().getH();
    }

    public void setRelativeHeight(float relativeHeight) {
        rendererContext.getProperties().setH(relativeHeight);
        updateSize();
    }

    public void addScreenshotListener(ScreenshotListener screenshotListener, boolean singleUse){
        rendererContext.addScreenshotListener(screenshotListener, singleUse);
    }

    public boolean removeScreenshotListener(ScreenshotListener screenshotListener){
        return rendererContext.removeScreenshotListener(screenshotListener);
    }

    @Override
    public void applyParameterAnimations(ParamContainer serverParamContainer, ParamContainer clientParamContainer, NumberFactory numberFactory) {
        boolean changed = rendererContext.applyParameterAnimations(getSystemContext(), serverParamContainer, clientParamContainer, numberFactory);
        if (changed)
            reset();
    }

    public void addPanListener(PanListener panListener){
        rendererContext.addPanListener(panListener);
    }

    public void removePanListener(PanListener panListener){
        rendererContext.removePanListener(panListener);
    }
}
