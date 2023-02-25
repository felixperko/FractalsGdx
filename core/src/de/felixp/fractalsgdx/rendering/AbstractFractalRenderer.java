package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.params.ClientParamsEscapeTime;
import de.felixp.fractalsgdx.params.ComputeParamsCommon;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.infra.SystemContext;

public abstract class AbstractFractalRenderer extends WidgetGroup implements FractalRenderer {

    private static Logger LOG = LoggerFactory.getLogger(AbstractFractalRenderer.class);
    private static int ID_COUNTER = 1;

    protected static void setColoringParams(ShaderProgram shader, float width, float height, MainStage stage, SystemContext systemContext, RendererContext rendererContext) {

        boolean scaleMagnitude2ByDims = false;
        double magnitude2Factor = 1.0;
        if (scaleMagnitude2ByDims){
            magnitude2Factor = Math.max(Gdx.graphics.getWidth()/1920.0, Gdx.graphics.getHeight()/1080.0);
        }
        double scaleFactor = Math.sqrt(Math.sqrt(systemContext.getParamContainer().getParam(ShaderSystemContext.PARAM_ZOOM).getGeneral(Number.class).toDouble()));
        if (scaleFactor > 0.0) {
            magnitude2Factor /= Math.min(scaleFactor, 100.0);
        }
        int extractChannel = 0;
        ParamSupplier supp = stage.getClientParam(ClientParamsEscapeTime.PARAMS_EXTRACT_CHANNEL);
        String extractChannelUid = (String) supp.getGeneral();
        switch (extractChannelUid){
            case ClientParamsEscapeTime.OPTIONVALUE_EXTRACT_CHANNEL_R:
                extractChannel = 1;
                break;
            case ClientParamsEscapeTime.OPTIONVALUE_EXTRACT_CHANNEL_G:
                extractChannel = 2;
                break;
            case ClientParamsEscapeTime.OPTIONVALUE_EXTRACT_CHANNEL_B:
                extractChannel = 3;
                break;
            default:
                extractChannel = 0;
        }

//        rendererContext.applyParameterAnimations(systemContext, systemContext.getParamContainer(), stage.getParamMap(), systemContext.getNumberFactory());
        shader.setUniformi("usePalette", stage.getClientParam(ClientParamsEscapeTime.PARAMS_PALETTE).getGeneral(String.class).equalsIgnoreCase(ClientParamsEscapeTime.OPTIONVALUE_PALETTE_DISABLED) ? 0 : 1);
        shader.setUniformi("usePalette2", stage.getClientParam(ClientParamsEscapeTime.PARAMS_PALETTE2).getGeneral(String.class).equalsIgnoreCase(ClientParamsEscapeTime.OPTIONVALUE_PALETTE_DISABLED) ? 0 : 1);
        shader.setUniformf("colorAdd", (float)stage.getClientParam(ClientParamsEscapeTime.PARAMS_COLOR_ADD).getGeneral(Number.class).toDouble());
        shader.setUniformf("colorAdd2", (float)stage.getClientParam(ClientParamsEscapeTime.PARAMS_FALLBACK_COLOR_ADD).getGeneral(Number.class).toDouble());
        shader.setUniformf("colorMult", (float)(double)stage.getClientParam(ClientParamsEscapeTime.PARAMS_COLOR_MULT).getGeneral(Number.class).toDouble());
        shader.setUniformf("colorMult2", (float)(double)stage.getClientParam(ClientParamsEscapeTime.PARAMS_FALLBACK_COLOR_MULT).getGeneral(Number.class).toDouble());
        shader.setUniformf("colorSaturation", (float)(double)stage.getClientParam(ClientParamsEscapeTime.PARAMS_COLOR_SATURATION).getGeneral(Number.class).toDouble());
        shader.setUniformf("colorSaturation2", (float)(double)stage.getClientParam(ClientParamsEscapeTime.PARAMS_FALLBACK_COLOR_SATURATION).getGeneral(Number.class).toDouble());shader.setUniformf("light_ambient", (float)(double)stage.getClientParam(ClientParamsEscapeTime.PARAMS_AMBIENT_LIGHT).getGeneral(Number.class).toDouble());
        shader.setUniformf("light_ambient2", (float)(double)stage.getClientParam(ClientParamsEscapeTime.PARAMS_FALLBACK_AMBIENT_LIGHT).getGeneral(Number.class).toDouble());
        shader.setUniformf("light_sobel_magnitude", (float)(double)stage.getClientParam(ClientParamsEscapeTime.PARAMS_SOBEL_GLOW_LIMIT).getGeneral(Number.class).toDouble());
        shader.setUniformf("light_sobel_magnitude2", (float)(magnitude2Factor*stage.getClientParam(ClientParamsEscapeTime.PARAMS_FALLBACK_SOBEL_GLOW_LIMIT).getGeneral(Number.class).toDouble()));
        shader.setUniformf("light_sobel_period", (float)((double)stage.getClientParam(ClientParamsEscapeTime.PARAMS_SOBEL_GLOW_FACTOR).getGeneral(Number.class).toDouble()));
        shader.setUniformf("light_sobel_period2", (float)(1.0/(double)stage.getClientParam(ClientParamsEscapeTime.PARAMS_FALLBACK_SOBEL_GLOW_FACTOR).getGeneral(Number.class).toDouble()));
        shader.setUniformi("extractChannel", extractChannel);
        shader.setUniformi("kernelRadius", (int)stage.getClientParam(ClientParamsEscapeTime.PARAMS_SOBEL_RADIUS).getGeneral());
        Object color = stage.getClientParam(ClientParamsEscapeTime.PARAMS_MAPPING_COLOR).getGeneral();
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

    boolean initialized = false;

    ClickListener clickListener = null;

    boolean disabled = false;

    public AbstractFractalRenderer(RendererContext rendererContext){
        this.rendererContext = rendererContext;
    }

    @Override
    public void initRenderer() {
        if (initialized)
            return;
        initialized = true;
        AbstractFractalRenderer thisRenderer = this;
        clickListener = new ClickListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                boolean wasFocused = isFocused;
                isFocused = event.getTarget() == thisRenderer;
                if (isFocused != wasFocused)
                    focusChanged(isFocused);
                return super.touchDown(event, x, y, pointer, button);
            }
        };
        getStage().addListener(clickListener);
        init();
    }

    @Override
    public void disposeRenderer() {
        FractalsGdxMain.mainStage.removeListener(clickListener);
    }

    public abstract void init();
    public abstract int getPixelCount();

    @Override
    public void draw(Batch batch, float parentAlpha) {

        if (handleSwitchRendererConfigs())
            return;

        if (isDisabled()) {
            return;
        }

        render(batch, parentAlpha);
    }

    protected abstract void render(Batch batch, float parentAlpha);

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

    public void zoomAndPan(Number zoomFactor, float anchorScreenX, float anchorScreenY) {
        ComplexNumber clickedPoint = getComplexMapping(anchorScreenX, anchorScreenY);
        zoomAndPan(zoomFactor, clickedPoint);
    }

    public void zoomAndPan(Number zoomFactor, ComplexNumber clickedPoint) {
        ComplexNumber newMidpoint = getZoomAnchor(zoomFactor, clickedPoint);
        zoom(zoomFactor);
        getSystemContext().setMidpoint(newMidpoint);
        rendererContext.panned(getSystemContext().getParamContainer());
    }

    protected abstract void zoom(Number zoomFactor);

    public ComplexNumber getZoomAnchor(Number zoomFactor, ComplexNumber clickedPoint){
        if (zoomFactor.toDouble() < 1.0){
            Number deltaScaleFactor = getSystemContext().getNumberFactory().createNumber(1.0);
            deltaScaleFactor.sub(zoomFactor);

            ComplexNumber currMidpoint = getSystemContext().getMidpoint();
            ComplexNumber newMidpoint = clickedPoint.copy();
            newMidpoint.sub(currMidpoint); //delta
            newMidpoint.multNumber(deltaScaleFactor);
            newMidpoint.add(currMidpoint);
            return newMidpoint;
        }
        else if (zoomFactor.toDouble() > 1.0){
            Number deltaScaleFactor = zoomFactor.copy();
            deltaScaleFactor.sub(getSystemContext().getNumberFactory().createNumber(1.0));

            ComplexNumber currMidpoint = getSystemContext().getMidpoint();
            ComplexNumber newMidpoint = currMidpoint.copy();
            newMidpoint.sub(clickedPoint); //delta
            newMidpoint.multNumber(deltaScaleFactor);
            newMidpoint.add(currMidpoint);
            return newMidpoint;
        }
        return getSystemContext().getMidpoint().copy();
    }

    public void focus(){
        FractalsGdxMain.mainStage.setKeyboardFocus(this);
        FractalsGdxMain.mainStage.setFocusedRenderer(this);
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
        for(int i = 4; i < pixels.length; i += 4) {
            pixels[i - 1] = (byte) 255;
        }

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
    public boolean[] applyParameterAnimations(ParamContainer serverParamContainer, ParamContainer clientParamContainer, NumberFactory numberFactory) {
        boolean[] res = rendererContext.applyParameterAnimations(getSystemContext(), serverParamContainer, clientParamContainer, numberFactory);
        if (res[1])
            reset();
        else if (res[0])
            setRefresh();
        return res;
    }

    String lastUidCalc = null;

    protected boolean handleSwitchRendererConfigs() {
        String uidCalc = getSystemContext().getParamContainer().getParam(ComputeParamsCommon.PARAM_CALCULATOR).getGeneral(String.class);
        if (lastUidCalc == null)
            lastUidCalc = uidCalc;
        if (uidCalc.equals(lastUidCalc)){
            return false;
        }
        getSystemContext().getParamContainer().removeParam(ComputeParamsCommon.PARAM_CALCULATOR);
        lastUidCalc = uidCalc;
        boolean changed = true;
        if (uidCalc.equals(ComputeParamsCommon.UID_CALCULATOR_ESCAPETIME) || uidCalc.equals(ComputeParamsCommon.UID_CALCULATOR_NEWTONFRACTAL))
            changed = FractalsGdxMain.mainStage.switchRendererConfigs(ShaderSystemContext.UID_PARAMCONFIG);
        else if (uidCalc.equals(ComputeParamsCommon.UID_CALCULATOR_TURTLEGRAPHICS))
            changed = FractalsGdxMain.mainStage.switchRendererConfigs(TurtleGraphicsSystemContext.UID_PARAMCONFIG);
        else if (uidCalc.equals(ComputeParamsCommon.UID_CALCULATOR_REACTIONDIFFUSION))
            changed = FractalsGdxMain.mainStage.switchRendererConfigs(ReactionDiffusionSystemContext.UID_PARAMCONFIG);
        else
            changed = false;

        if (changed)
            setDisabled(true);
        if (!changed)
            getSystemContext().getParamContainer().addParam(new StaticParamSupplier(ComputeParamsCommon.PARAM_CALCULATOR, uidCalc));

//        if (changed){
////            getSystemContext().getParamContainer().addParam(new StaticParamSupplier(ComputeParamsCommon.PARAM_CALCULATOR, uidCalc));
//            Gdx.app.postRunnable(new Runnable() {
//                @Override
//                public void run() {
//                    FractalRenderer newFirstRenderer = FractalsGdxMain.mainStage.getRenderers().get(0);
//                    if (newFirstRenderer != null) {
//                        if (newFirstRenderer.getSystemContext().getParamConfiguration().getParamDefinitionByUID(ComputeParamsCommon.PARAM_CALCULATOR) != null) {
//                            newFirstRenderer.getSystemContext().getParamContainer().addParam(new StaticParamSupplier(ComputeParamsCommon.PARAM_CALCULATOR, uidCalc));
////                            if (newFirstRenderer instanceof AbstractFractalRenderer)
////                                ((AbstractFractalRenderer)newFirstRenderer).paramsChanged(newFirstRenderer.getSystemContext().getParamContainer());
//                        }
//                    }
//                }
//            });
//        }
        return changed;
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
        setDisabled(false);
    }

    protected void mousePosChanged(float newMouseX, float newMouseY) {
        ComplexNumber mapped = getComplexMapping(newMouseX, newMouseY);
        rendererContext.getMouseMovedListeners().forEach(l -> l.moved(newMouseX, newMouseY, mapped));
    }

    protected void clicked(float mouseX, float mouseY, int button) {
        ComplexNumber mapped = getComplexMapping(mouseX, mouseY);
        rendererContext.clicked(mouseX, mouseY, button, mapped);
    }

    @Override
    public ComplexNumber getComplexMapping(float screenX, float screenY) {
        return getSystemContext().getNumberFactory().createComplexNumber(getReal(screenX), getImag(screenY));
    }

    boolean paramsChangedCalled = false;

    public void paramsChanged(ParamContainer paramContainer) {
        if (isFocused && !paramsChangedCalled) {
            paramsChangedCalled = true;
            ((MainStage) FractalsGdxMain.stage).getParamUI().setServerParameterConfiguration(this, paramContainer, getSystemContext().getParamConfiguration());
            paramsChangedCalled = false;
        }
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
