package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.systems.infra.SystemContext;

abstract class AbstractFractalRenderer extends WidgetGroup implements FractalRenderer {

    private static Logger LOG = LoggerFactory.getLogger(AbstractFractalRenderer.class);
    private static int ID_COUNTER = 1;

    protected static void setColoringParams(ShaderProgram shader, float width, float height, MainStage stage, SystemContext systemContext, RendererContext rendererContext) {

        boolean scaleMagnitude2ByDims = true;
        double magnitude2Factor = 1.0;
        if (scaleMagnitude2ByDims){
            magnitude2Factor = Math.max(Gdx.graphics.getWidth()/1920.0, Gdx.graphics.getHeight()/1080.0);
//            double scaleFactor = Math.sqrt(Math.sqrt(systemContext.getParamContainer().getParam(ShaderSystemContext.PARAM_ZOOM).getGeneral(Number.class).toDouble()));
//            if (scaleFactor > 0.0) {
//                magnitude2Factor /= Math.min(scaleFactor, 100.0);
//            }
        }

//        rendererContext.applyParameterAnimations(systemContext, systemContext.getParamContainer(), stage.getParamMap(), systemContext.getNumberFactory());
        shader.setUniformi("usePalette", stage.getClientParam(MainStage.PARAMS_PALETTE).getGeneral(String.class).equalsIgnoreCase(MainStage.PARAMVALUE_PALETTE_DISABLED) ? 0 : 1);
        shader.setUniformi("usePalette2", stage.getClientParam(MainStage.PARAMS_PALETTE2).getGeneral(String.class).equalsIgnoreCase(MainStage.PARAMVALUE_PALETTE_DISABLED) ? 0 : 1);
        shader.setUniformf("colorAdd", (float)stage.getClientParam(MainStage.PARAMS_COLOR_ADD).getGeneral(Number.class).toDouble());
        shader.setUniformf("colorAdd2", (float)stage.getClientParam(MainStage.PARAMS_FALLBACK_COLOR_ADD).getGeneral(Number.class).toDouble());
        shader.setUniformf("colorMult", (float)(double)stage.getClientParam(MainStage.PARAMS_COLOR_MULT).getGeneral(Number.class).toDouble());
        shader.setUniformf("colorMult2", (float)(double)stage.getClientParam(MainStage.PARAMS_FALLBACK_COLOR_MULT).getGeneral(Number.class).toDouble());
        shader.setUniformf("colorSaturation", (float)(double)stage.getClientParam(MainStage.PARAMS_COLOR_SATURATION).getGeneral(Number.class).toDouble());
        shader.setUniformf("colorSaturation2", (float)(double)stage.getClientParam(MainStage.PARAMS_FALLBACK_COLOR_SATURATION).getGeneral(Number.class).toDouble());shader.setUniformf("light_ambient", (float)(double)stage.getClientParam(MainStage.PARAMS_AMBIENT_LIGHT).getGeneral(Number.class).toDouble());
        shader.setUniformf("light_ambient2", (float)(double)stage.getClientParam(MainStage.PARAMS_FALLBACK_AMBIENT_LIGHT).getGeneral(Number.class).toDouble());
        shader.setUniformf("light_sobel_magnitude", (float)(double)stage.getClientParam(MainStage.PARAMS_SOBEL_GLOW_LIMIT).getGeneral(Number.class).toDouble());
        shader.setUniformf("light_sobel_magnitude2", (float)(magnitude2Factor*stage.getClientParam(MainStage.PARAMS_FALLBACK_SOBEL_GLOW_LIMIT).getGeneral(Number.class).toDouble()));
        shader.setUniformf("light_sobel_period", (float)((double)stage.getClientParam(MainStage.PARAMS_SOBEL_GLOW_FACTOR).getGeneral(Number.class).toDouble()));
        shader.setUniformf("light_sobel_period2", (float)(1.0/(double)stage.getClientParam(MainStage.PARAMS_FALLBACK_SOBEL_GLOW_FACTOR).getGeneral(Number.class).toDouble()));
        shader.setUniformi("extractChannel", (int)(int)stage.getClientParam(MainStage.PARAMS_EXTRACT_CHANNEL).getGeneral());
        Object color = stage.getClientParam(MainStage.PARAMS_MAPPING_COLOR).getGeneral();
//        float[] hsv = ((Color)color).toHsv(new float[4]);
//        shader.setUniformf("mappingColorR", hsv[0]);
//        shader.setUniformf("mappingColorG", hsv[1]);
//        shader.setUniformf("mappingColorB", hsv[2]);
        shader.setUniformf("mappingColorR", ((Color)color).r);
        shader.setUniformf("mappingColorG", ((Color)color).g);
        shader.setUniformf("mappingColorB", ((Color)color).b);

        shader.setUniformf("resolution", width, height);
//        float defaultWidth = 1920;
//        shader.setUniformi("sobelSpan", (int)Math.round(Gdx.graphics.getWidth()/defaultWidth));
//        shader.setUniformi("sobelSpan", Gdx.graphics.getWidth() >= defaultWidth*2 ? 2 : 1 );
        shader.setUniformi("sobelSpan", 1);
    }

    int position = -1;

    //TODO replace with position?
    int id = ID_COUNTER++;

    boolean singleScreenshotScheduled;
    boolean recordingScreenshots;

    RendererContext rendererContext;

    protected boolean isFocused = false;

    boolean multisampleByRepeating = false;

    double timeBudgetS = 0.0;

    public AbstractFractalRenderer(RendererContext rendererContext){
        this.rendererContext = rendererContext;
    }


    @Override
    public void initRenderer() {
        AbstractFractalRenderer thisRenderer = this;
        getStage().addListener(new ClickListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                boolean wasFocused = isFocused;
                isFocused = event.getTarget() == thisRenderer;
                if (isFocused != wasFocused)
                    focusChanged(isFocused);
                return super.touchDown(event, x, y, pointer, button);
            }
        });
        init();
    }

    public abstract void init();
    public abstract int getPixelCount();

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
            shader = new ShaderProgram(vertexString, fragmentString);
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
        if (!shader.isCompiled()) {
            System.out.println("----VERTEX----");
            printShaderLines(vertexString);
            System.out.println("---FRAGMENT---");
            printShaderLines(fragmentString);
            throw new IllegalStateException("Error compiling shaders ("+vertexPath+", "+fragmentPath+"): "+shader.getLog());
        }
        return shader;
    }

    /**
     * Override for scroll functionality
     * @param amountX
     * @param amountY
     * @return true if scroll was handled
     */
    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    private void printShaderLines(String lines) {
        String[] l = lines.split(System.lineSeparator());
        for (int i = 0 ; i < l.length ; i++){
            System.out.println((i+1)+": "+l[i]);
        }
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
    public abstract void focusChanged(boolean focusedNow);

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

    protected synchronized void makeScreenshot() {

        long t1 = System.nanoTime();

        //get pixels
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);
//        byte[] pixels = new byte[1];

        long t2 = System.nanoTime();

        // this loop makes sure the whole screenshot is opaque and looks exactly like what the user is seeing
//        for(int i = 4; i < pixels.length; i += 4) {
//            pixels[i - 1] = (byte) 255;
//        }

        rendererContext.madeScreenshot(pixels);

        long t3 = System.nanoTime();
        System.out.println("Screenshot processing times: " + (t2 - t1) + ", " + (t3 - t2));
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

    public int getOrientation() {
        return (int)rendererContext.getProperties().getOrientation();
    }

    public void setOrientation(int orientation) {
        rendererContext.getProperties().setOrientation(orientation);
//        updateSize();
    }

    public void addScreenshotListener(ScreenshotListener screenshotListener, boolean singleUse){
        rendererContext.addScreenshotListener(screenshotListener, singleUse);
    }

    public boolean removeScreenshotListener(ScreenshotListener screenshotListener){
        return rendererContext.removeScreenshotListener(screenshotListener);
    }

    @Override
    public void applyParameterAnimations(ParamContainer serverParamContainer, ParamContainer clientParamContainer, NumberFactory numberFactory) {
        boolean[] res = rendererContext.applyParameterAnimations(getSystemContext(), serverParamContainer, clientParamContainer, numberFactory);
        if (res[1])
            reset();
        else if (res[0])
            setRefresh();
    }

    public void addPanListener(PanListener panListener){
        rendererContext.addPanListener(panListener);
    }

    public void removePanListener(PanListener panListener){
        rendererContext.removePanListener(panListener);
    }

    @Override
    public void setTimeBudget(double newTimeBudgetS) {
        this.timeBudgetS = newTimeBudgetS;
    }

    @Override
    public boolean isFocused() {
        return isFocused;
    }

    @Override
    public void setFocused(boolean focused){
        this.isFocused = focused;
    }

    protected void mousePosChanged(float newMouseX, float newMouseY) {
        ComplexNumber mapped = getComplexMapping(newMouseX, newMouseY);
        rendererContext.getMouseMovedListeners().forEach(l -> l.moved(newMouseX, newMouseY, mapped));
    }

    protected void clicked(float mouseX, float mouseY, int button) {
        ComplexNumber mapped = getComplexMapping(mouseX, mouseY);
        rendererContext.clicked(mouseX, mouseY, button, mapped);
    }
}
