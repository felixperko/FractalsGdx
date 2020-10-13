package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.interpolation.ParameterInterpolation;
import de.felixp.fractalsgdx.remoteclient.SystemInterfaceGdx;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.util.NumberUtil;

/**
 * Holds the state and (planned: persistable) properties of a renderer instance.
 * Designed to be able to swap between different renderer implementations while keeping the
 * - renderer size
 * - (planned) listeners
 * - (planned) state
 * as seamless as possible (if the renderer implements the functionality/parameters can be mapped).
 */
public class RendererContext {

    RendererProperties properties;

    ParamContainer paramContainer = null;

    List<ScreenshotListener> screenshotListeners = new ArrayList<>();
    List<ScreenshotListener> singleScreenshotListeners = new ArrayList<>();

    List<PanListener> panListeners = new ArrayList<>();

    List<ParameterInterpolation> parameterInterpolationsServer = new ArrayList<>();
    List<ParameterInterpolation> parameterInterpolationsClient = new ArrayList<>();

    double time;
    long lastTimestamp = -1;

    public RendererContext(float x, float y, float w, float h){
        properties = new RendererProperties(x, y, w, h);
    }

    public void init(FractalRenderer renderer){
        properties.setRendererClass(renderer.getClass());
    }

    public RendererProperties getProperties() {
        return properties;
    }

    public synchronized void addScreenshotListener(ScreenshotListener screenshotListener, boolean singleUse){
        if (!singleUse)
            this.screenshotListeners.add(screenshotListener);
        else
            this.singleScreenshotListeners.add(screenshotListener);
    }

    public synchronized boolean removeScreenshotListener(ScreenshotListener screenshotListener){
        return this.screenshotListeners.remove(screenshotListener);
    }

    public void addPanListener(PanListener panListener){
        this.panListeners.add(panListener);
    }

    public void removePanListener(PanListener panListener){
        this.panListeners.remove(panListener);
    }

    public void addParameterInterpolationServer(ParameterInterpolation parameterInterpolation){
        this.parameterInterpolationsServer.add(parameterInterpolation);
    }

    public void removeParameterInterpolationServer(ParameterInterpolation parameterInterpolation){
        this.parameterInterpolationsServer.remove(parameterInterpolation);
    }

    public void addParameterInterpolationClient(ParameterInterpolation parameterInterpolation){
        this.parameterInterpolationsClient.add(parameterInterpolation);
    }

    public void removeParameterInterpolationClient(ParameterInterpolation parameterInterpolation){
        this.parameterInterpolationsClient.remove(parameterInterpolation);
    }

    boolean disableContinuousRendering = false;

    public void applyParameterInterpolations(ParamContainer serverParamContainer, ParamContainer clientParamContainer, NumberFactory numberFactory){

        if (!Gdx.graphics.isContinuousRendering() && !parameterInterpolationsClient.isEmpty() || !parameterInterpolationsServer.isEmpty()){
            Gdx.graphics.setContinuousRendering(true);
            disableContinuousRendering = true;
        }
        if (Gdx.graphics.isContinuousRendering() && disableContinuousRendering && parameterInterpolationsServer.isEmpty() && parameterInterpolationsClient.isEmpty()){
            disableContinuousRendering = false;
            Gdx.graphics.setContinuousRendering(false);
        }

        if (lastTimestamp != -1)
            time += (System.nanoTime()-lastTimestamp)* NumberUtil.NS_TO_S;
        lastTimestamp = System.nanoTime();

        if (serverParamContainer != null){
            for (ParameterInterpolation interServer : parameterInterpolationsServer){
                serverParamContainer.addClientParameter(new StaticParamSupplier(interServer.getParameterName(), interServer.getInterpolatedValue(time, numberFactory)));
            }
        }
        if (clientParamContainer != null) {
            for (ParameterInterpolation interClient : parameterInterpolationsClient) {
                clientParamContainer.addClientParameter(new StaticParamSupplier(interClient.getParameterName(), interClient.getInterpolatedValue(time, numberFactory)));
            }
        }
    }

    public void madeScreenshot(byte[] pixels) {
        //notify listeners
        for (ScreenshotListener listener : new ArrayList<>(screenshotListeners)){
            listener.madeScreenshot(pixels);
        }
        for (ScreenshotListener listener : singleScreenshotListeners){
            listener.madeScreenshot(pixels);
        }
        singleScreenshotListeners.clear();
    }

    public List<ScreenshotListener> getScreenshotListeners() {
        return screenshotListeners;
    }

    public List<ScreenshotListener> getSingleScreenshotListeners() {
        return singleScreenshotListeners;
    }

//    public List<PanListener> getPanListeners() {
//        return panListeners;
//    }

    public List<ParameterInterpolation> getParameterInterpolationsServer() {
        return parameterInterpolationsServer;
    }

    public List<ParameterInterpolation> getParameterInterpolationsClient() {
        return parameterInterpolationsClient;
    }

    public double getTime() {
        return time;
    }

    public void panned(ParamContainer paramContainer) {
        for (PanListener panListener : panListeners) {
            panListener.panned(paramContainer.getClientParameter("midpoint").getGeneral(ComplexNumber.class));
        }
    }

    public ParamContainer getParamContainer() {
        return paramContainer;
    }

    public void setParamContainer(ParamContainer paramContainer) {
        this.paramContainer = paramContainer;
    }
}
