package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.ScreenUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import de.felixp.fractalsgdx.interpolation.ParameterInterpolation;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.systems.infra.SystemContext;

abstract class AbstractFractalRenderer extends WidgetGroup implements FractalRenderer {

    private static int ID_COUNTER = 1;

    protected static void setColoringParams(ShaderProgram shader, double xPos, double yPos, float width, float height, MainStage stage, SystemContext systemContext, RendererContext rendererContext, ComplexNumber anchor) {
        rendererContext.applyParameterInterpolations(systemContext.getParamContainer(), stage.getClientParameters(), systemContext.getNumberFactory());
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

        ComplexNumber anchorCpy = anchor.copy();
        //anchorCpy.multValues(systemContext.getNumberFactory().createComplexNumber(-1, 1));
        Number zoom = (Number) systemContext.getParamValue("zoom", Number.class);
        ComplexNumber pos = systemContext.getNumberFactory().createComplexNumber(-xPos, yPos);
        ComplexNumber dims = systemContext.getNumberFactory().createComplexNumber(-height, height);
        anchorCpy.divNumber(zoom);
        anchorCpy.multValues(dims);
        //pos.divNumber(zoom);

        shader.setUniformf("axisWidth", 1.5f);
        shader.setUniformf("axisColor", 1.0f, 1.0f, 1.0f, (float)(double)stage.getClientParameter(MainStage.PARAMS_AXIS_ALPHA).getGeneral(Double.class));
        shader.setUniformf("axisTexCoords", (float)((anchorCpy.realDouble()-pos.realDouble())/width)+0.5f, (float)((anchorCpy.imagDouble()-pos.imagDouble())/height)+0.5f);
    }

    int position = -1;

    //TODO replace with position?
    int id = ID_COUNTER++;

    boolean screenshot;

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
        try {
            List<String> vertexStringTemplate = Files.readAllLines(Paths.get(Gdx.files.internal(vertexPath).path()));
            List<String> fragmentStringTemplate = Files.readAllLines(Paths.get(Gdx.files.internal(fragmentPath).path()));
            vertexString = fillShaderTemplate(vertexStringTemplate);
            fragmentString = fillShaderTemplate(fragmentStringTemplate);
            shader = new ShaderProgram(vertexString, fragmentString);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    public void setScreenshot(boolean screenshot) {
        this.screenshot = screenshot;
    }

    @Override
    public boolean isScreenshot(boolean reset) {
        boolean curr = screenshot;
        if (reset)
            screenshot = false;
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
        //get pixels
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);
        // this loop makes sure the whole screenshot is opaque and looks exactly like what the user is seeing
        for(int i = 4; i < pixels.length; i += 4) {
            pixels[i - 1] = (byte) 255;
        }
        rendererContext.madeScreenshot(pixels);
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

    public void addPanListener(PanListener panListener){
        rendererContext.addPanListener(panListener);
    }

    public void removePanListener(PanListener panListener){
        rendererContext.removePanListener(panListener);
    }

    public void addParameterInterpolationServer(ParameterInterpolation parameterInterpolation){
        rendererContext.addParameterInterpolationServer(parameterInterpolation);
    }

    public void removeParameterInterpolationServer(ParameterInterpolation parameterInterpolation){
        rendererContext.removeParameterInterpolationServer(parameterInterpolation);
    }

    public void addParameterInterpolationClient(ParameterInterpolation parameterInterpolation){
        rendererContext.addParameterInterpolationClient(parameterInterpolation);
    }

    public void removeParameterInterpolationClient(ParameterInterpolation parameterInterpolation){
        rendererContext.removeParameterInterpolationClient(parameterInterpolation);
    }

    @Override
    public void applyParameterInterpolations(ParamContainer serverParamContainer, ParamContainer clientParamContainer, NumberFactory numberFactory) {
        rendererContext.applyParameterInterpolations(serverParamContainer, clientParamContainer, numberFactory);
    }

    //    public boolean isRefresh(boolean reset){
//        boolean val = refresh;
//        if (reset)
//            refresh = false;
//        return val;
//    }
}
