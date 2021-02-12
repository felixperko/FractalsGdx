package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.animation.PathParamAnimation;
import de.felixp.fractalsgdx.animation.ParamAnimation;
import de.felixp.fractalsgdx.animation.interpolations.ParamInterpolation;
import de.felixp.fractalsgdx.ui.AnimationsUI;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.infra.SystemContext;
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

//    public static final String CONTAINERKEY_CLIENT = "client";
//    public static final String CONTAINERKEY_SERVER = "server";
    RendererProperties properties;

    ParamContainer paramContainer = null;

    List<ScreenshotListener> screenshotListeners = new ArrayList<>();
    List<ScreenshotListener> singleScreenshotListeners = new ArrayList<>();

    List<PanListener> panListeners = new ArrayList<>();

//    List<ParamAnimation> parameterAnimationsServer = new ArrayList<>();
//    List<ParamAnimation> parameterAnimationsClient = new ArrayList<>();

    List<ParamAnimation> paramAnimations = new ArrayList<>();

//    List<ComplexNumber> path = new ArrayList<>();
//    List<ComplexNumber> normals = new ArrayList<>();
    String selectedPathAnimation = "path";
    ComplexNumber defaultNormal;

    double time;
    long lastTimestamp = -1;

    public RendererContext(float x, float y, float w, float h){
        properties = new RendererProperties(x, y, w, h);
//        String param = "start";
        String param = null;
        String containerKey = "server";
        PathParamAnimation pathAnimation = new PathParamAnimation(selectedPathAnimation);
//        pathAnimation.setTimeFactor(60.0);
        pathAnimation.setFrameCount(60*10);
        addParamAnimation(pathAnimation);
    }

    public void init(FractalRenderer renderer){
        properties.setRendererClass(renderer.getClass());
        defaultNormal = renderer.getSystemContext().getNumberFactory().createComplexNumber(1, 0);
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

    public void addParamAnimation(ParamAnimation paramAnimation){
        paramAnimations.add(paramAnimation);
    }

    public void removeParamAnimation(ParamAnimation paramAnimation){
        paramAnimations.remove(paramAnimation);
    }

    public ParamAnimation getParamAnimation(String name){
        for (ParamAnimation animation : paramAnimations){
            if (animation.getName().equals(name))
                return animation;
        }
        return null;
    }

    boolean disableContinuousRendering = false;

    public boolean applyParameterAnimations(SystemContext systemContext, ParamContainer serverParamContainer, ParamContainer clientParamContainer, NumberFactory numberFactory){

        boolean reset = containsResetAnimations();
        if (!Gdx.graphics.isContinuousRendering()){
            if (reset) {
                Gdx.graphics.setContinuousRendering(true);
                disableContinuousRendering = true;
            }
        }
        if (Gdx.graphics.isContinuousRendering() && disableContinuousRendering && !reset){
            disableContinuousRendering = false;
            Gdx.graphics.setContinuousRendering(false);
        }

        if (lastTimestamp != -1)
            time += (System.nanoTime()-lastTimestamp)* NumberUtil.NS_TO_S;
        lastTimestamp = System.nanoTime();

        boolean changed = false;
        if (serverParamContainer != null){
            for (ParamAnimation ani : paramAnimations){
                ani.updateProgress();
                boolean applied = applyAnimation(serverParamContainer, clientParamContainer, numberFactory, ani);
                if (applied)
                    changed = true;
            }
        }
//        if (clientParamContainer != null) {
//            for (ParamAnimation ani : paramAnimationsClient) {
//                ani.updateProgress();
//                boolean applied = applyAnimation(, numberFactory, ani);
//                if (applied)
//                    changed = true;
//            }
//        }
        AnimationsUI.updateSliders();
        if (changed) {
            systemContext.setParameters(serverParamContainer);
        }
        return changed;
    }

    protected boolean applyAnimation(ParamContainer paramContainerCalculate, ParamContainer paramContainerDraw, NumberFactory numberFactory, ParamAnimation animation) {
        if (!animation.isApplyValue())
            return false;
        boolean changed = false;
        for (ParamInterpolation interpolation : animation.getInterpolations().values()){
            String paramName = interpolation.getParamName();
            if (paramName != null) {
                Object interpolatedValue = interpolation.getInterpolatedValue(animation.getLoopProgress(), numberFactory);
                ParamContainer paramContainer = null;
                if (interpolation.getParamContainerKey().equals(AnimationsUI.PARAM_CONTAINERKEY_SERVER))
                    paramContainer = paramContainerCalculate;
                else if (interpolation.getParamContainerKey().equals(AnimationsUI.PARAM_CONTAINERKEY_CLIENT))
                    paramContainer = paramContainerDraw;
                if (paramContainer == null)
                    throw new IllegalStateException("Unknown param container key: "+interpolation.getParamContainerKey());
                ParamSupplier currentSupplier = paramContainer.getClientParameter(paramName);
                if (currentSupplier instanceof StaticParamSupplier && !interpolatedValue.equals(currentSupplier.getGeneral())) {
                    paramContainer.addClientParameter(new StaticParamSupplier(paramName, interpolatedValue));
                    changed = true;
                }
            }
        }
        if (changed)
            panned(paramContainerCalculate);
        return changed;
    }

    public boolean containsResetAnimations(){
            if (containsResetAnimations(paramAnimations))
                return true;
        return false;
    }

    protected boolean containsResetAnimations(List<ParamAnimation> animationList){
        for (ParamAnimation animation : animationList){
            for (ParamInterpolation interpolation : animation.getInterpolations().values()){
                if (interpolation.getParamContainerKey().equals(AnimationsUI.PARAM_CONTAINERKEY_SERVER)){
                    return true;
                }
            }
        }
        return false;
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

//    public List<ParamAnimation> getParameterAnimationsServer() {
//        return getParamAnimationsForContainerKey(CONTAINERKEY_SERVER);
//    }
//
//    public List<ParamAnimation> getParameterAnimationsClient() {
//        return getParamAnimationsForContainerKey(CONTAINERKEY_CLIENT);
//    }

    public double getTime() {
        return time;
    }

    public void panned(ParamContainer paramContainer) {
        for (PanListener panListener : panListeners) {
            panListener.panned(paramContainer.getClientParameter("midpoint").getGeneral(ComplexNumber.class));
        }
    }

    public void addPathPoint(ComplexNumber point, NumberFactory numberFactory){
        addPathPoint(point, defaultNormal, numberFactory);
    }

    public void addPathPoint(ComplexNumber point, ComplexNumber normal, NumberFactory numberFactory){
        ParamInterpolation pathPI = getSelectedParamInterpolation();
        if (pathPI != null)
            pathPI.addControlPoint(point, normal, numberFactory);
    }

//    public void movePathPoint(int index, ComplexNumber point){
//        PathParamAnimation pathPI = getSelectedPathAnimation();
//        pathPI.movePathPoint(index, point);
//    }
//
//    public void setNormal(int index, ComplexNumber normal){
//        PathParamAnimation pathPI = getSelectedPathAnimation();
//        pathPI.setNormal(index, normal);
//    }

    public void clearPath(){
        ParamInterpolation pathPI = getSelectedParamInterpolation();
        if (pathPI != null)
            pathPI.clearControlPoints();
    }

//    public List<ComplexNumber> getPath() {
//        ParamInterpolation pathPI = getSelectedParamInterpolation();
//        if (pathPI == null)
//            return new ArrayList<>();
//        return pathPI.getControlPoints(false);
//    }
//
//    public List<ComplexNumber> getTangents() {
//        ParamInterpolation pathPI = getSelectedParamInterpolation();
//        if (pathPI == null)
//            return new ArrayList<>();
//        return pathPI.getControlDerivatives(false);
//    }

    public ParamInterpolation getSelectedParamInterpolation(){
        return AnimationsUI.getSelectedInterpolation();
//        Collection<ParamInterpolation> interpolations = getSelectedPathAnimation().getInterpolations().values();
//        if (interpolations.isEmpty())
//            return null;
//        return interpolations.iterator().next();
    }

//    public PathParamAnimation getSelectedPathAnimation(){
//        ParamAnimation animation = getParameterAnimation(CONTAINERKEY_CLIENT, selectedPathAnimation);
//        if (animation == null)
//            animation = getParameterAnimation(CONTAINERKEY_SERVER, selectedPathAnimation);
//        return (PathParamAnimation) animation;
//    }

    public ParamContainer getParamContainer() {
        return paramContainer;
    }

    public void setParamContainer(ParamContainer paramContainer) {
        this.paramContainer = paramContainer;
    }

    public List<ParamAnimation> getParameterAnimations() {
        return paramAnimations;
    }
}
