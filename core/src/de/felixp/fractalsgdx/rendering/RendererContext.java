package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.animation.PathParamAnimation;
import de.felixp.fractalsgdx.animation.ParamAnimation;
import de.felixp.fractalsgdx.animation.interpolations.ParamInterpolation;
import de.felixp.fractalsgdx.rendering.links.RendererLink;
import de.felixp.fractalsgdx.rendering.renderers.FractalRenderer;
import de.felixp.fractalsgdx.ui.AnimationsUI;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.attributes.ParamAttribute;
import de.felixperko.fractals.system.parameters.attributes.ParamAttributeContainer;
import de.felixperko.fractals.system.parameters.attributes.ParamAttributeHolder;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.util.NumberUtil;

/**
 * Holds the state and (planned: persistable) properties of a renderer instance.
 * Designed to be able to swap between different renderer instances while keeping the
 * - state/parameters
 * - size
 * - animations
 * - listeners
 * - renderer links
 * as seamless as possible (if the renderer implements the functionality/parameters can be mapped).
 */
public class RendererContext {

    RendererProperties properties;

    ParamContainer paramContainer = null;

    List<ClickedListener> clickedListeners = new ArrayList<>();
    List<ClickedListener> singleClickedListeners = new ArrayList<>();
    List<MouseMovedListener> mouseMovedListeners = new ArrayList<>();

    List<ScreenshotListener> screenshotListeners = new ArrayList<>();
    List<ScreenshotListener> singleScreenshotListeners = new ArrayList<>();

    List<RendererLink> sourceLinks = new ArrayList<>();
    List<RendererLink> targetLinks = new ArrayList<>();

    List<PanListener> panListeners = new ArrayList<>();

    List<ParamAnimation> paramAnimations = new ArrayList<>();

    String selectedPathAnimation = "path";
    ComplexNumber defaultNormal;

    double time;
    long lastTimestamp = -1;

    boolean disableContinuousRendering = false;

    int rendererId = -1;

    /**
     * Create a renderer context with the given start properties.
     * @param x
     * @param y
     * @param w
     * @param h
     * @param orientation
     */
    public RendererContext(float x, float y, float w, float h, int orientation){
        properties = new RendererProperties(x, y, w, h, orientation);
//        String param = BFOrbitCommon.PARAM_ZSTART;
        String param = null;
        String containerKey = "server";
        PathParamAnimation pathAnimation = new PathParamAnimation(selectedPathAnimation);
//        pathAnimation.setTimeFactor(60.0);
        pathAnimation.setFrameCount(60*10);
        addParamAnimation(pathAnimation);
    }

    public void setRenderer(FractalRenderer renderer){
        rendererId = renderer.getId();
        properties.setRendererClass(renderer.getClass());
        for (RendererLink link : getSourceLinks()){
            link.setSourceRenderer(renderer);
        }
        for (RendererLink link : targetLinks){
            link.setTargetRenderer(renderer);
        }
    }

    public void setLinkSource(RendererLink rendererLink){
        if (!sourceLinks.contains(rendererLink))
            sourceLinks.add(rendererLink);
    }

    public void setLinkTarget(RendererLink rendererLink) {
        if (!targetLinks.contains(rendererLink))
            targetLinks.add(rendererLink);
    }

    public void removeLinkSource(RendererLink rendererLink, boolean cascade){
        sourceLinks.remove(rendererLink);
        if (cascade)
            rendererLink.getTargetRenderer().getRendererContext().removeLinkTarget(rendererLink, false);
    }

    public void removeLinkTarget(RendererLink rendererLink, boolean cascade){
        targetLinks.remove(rendererLink);
        if (cascade)
            rendererLink.getSourceRenderer().getRendererContext().removeLinkSource(rendererLink, false);
    }

    public RendererProperties getProperties() {
        return properties;
    }

    public synchronized ClickedListener addClickedListener(ClickedListener clickedListener, boolean singleUse){
        if (!singleUse)
            this.clickedListeners.add(clickedListener);
        else
            this.singleClickedListeners.add(clickedListener);
        return clickedListener;
    }

    public synchronized boolean removeClickedListener(ClickedListener clickedListener){
        boolean removed = this.clickedListeners.remove(clickedListener);
        if (!removed)
            removed = this.singleClickedListeners.remove(clickedListener);
        return removed;
    }

    public synchronized MouseMovedListener addMouseMovedListener(MouseMovedListener mouseMovedListener){
        this.mouseMovedListeners.add(mouseMovedListener);
        return mouseMovedListener;
    }

    public synchronized boolean removeMouseMovedListener(MouseMovedListener mouseMovedListener){
        return this.mouseMovedListeners.remove(mouseMovedListener);
    }

    public synchronized void addScreenshotListener(ScreenshotListener screenshotListener, boolean singleUse){
        if (!singleUse)
            this.screenshotListeners.add(screenshotListener);
        else
            this.singleScreenshotListeners.add(screenshotListener);
    }

    public synchronized boolean removeScreenshotListener(ScreenshotListener screenshotListener){
        boolean removed = this.screenshotListeners.remove(screenshotListener);
        if (!removed)
            removed = this.singleClickedListeners.remove(screenshotListener);
        return removed;
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

    boolean changedMidpoint = false;

    /**
     * applies the (active) animations and returns whether changes occurred and if a reset is advised.
     * @param systemContext
     * @param serverParamContainer
     * @param clientParamContainer
     * @param numberFactory
     * @return changed, reset
     */
    public boolean[] applyParameterAnimations(SystemContext systemContext, ParamContainer serverParamContainer, ParamContainer clientParamContainer, NumberFactory numberFactory){
        defaultNormal = systemContext.getNumberFactory().createComplexNumber(1, 0);

        MainStage stage = (MainStage) FractalsGdxMain.stage;
        boolean reset = containsResetAnimations(systemContext.getParamConfiguration(), stage.getClientParamConfiguration(), true);
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
        changedMidpoint = false;
        if (serverParamContainer != null){
            for (ParamAnimation ani : paramAnimations){
                ani.updateProgress();
                boolean applied = applyAnimation(serverParamContainer, clientParamContainer, numberFactory, ani, systemContext);
                if (applied)
                    changed = true;
            }
        }

        AnimationsUI.updateSliders();
        if (changed) {
            if (changedMidpoint)
                panned(serverParamContainer);

            systemContext.setParameters(serverParamContainer);

            if (stage.getFocusedRenderer().getId() == rendererId){
                //update displayed values
                stage.getParamUI().refreshServerParameterUI(stage.getFocusedRenderer());
            }

            for (RendererLink link : getSourceLinks())
                link.syncTargetRenderer();
        }
        return new boolean[]{changed, reset};
    }

    protected boolean applyAnimation(ParamContainer paramContainerCalculate, ParamContainer paramContainerDraw, NumberFactory numberFactory, ParamAnimation animation, SystemContext systemContext) {
        if (!animation.isApplyValue())
            return false;
        boolean changed = false;
        for (ParamInterpolation interpolation : animation.getInterpolations().values()){
            String paramUid = interpolation.getParamUid();
            if (paramUid != null) {
                Object interpolatedValue = interpolation.getInterpolatedValue(animation.getLoopProgress(), numberFactory);

                if (interpolatedValue == null) //interpolated parameter/attribute probably deleted, ignore
                    continue;

                ParamContainer paramContainer = null;
                ParamConfiguration paramConfiguration = null;
                boolean serverParamContainer = interpolation.getParamContainerKey().equals(AnimationsUI.PARAM_CONTAINERKEY_SERVER);
                boolean clientParamContainer = interpolation.getParamContainerKey().equals(AnimationsUI.PARAM_CONTAINERKEY_CLIENT);

                if (serverParamContainer) {
                    paramContainer = paramContainerCalculate;
                    paramConfiguration = systemContext.getParamConfiguration();
                } else if (clientParamContainer) {
                    paramContainer = paramContainerDraw;
                    paramConfiguration = FractalsGdxMain.mainStage.getClientParamConfiguration();
                }
                if (paramContainer == null)
                    throw new IllegalStateException("Unknown param container key: "+interpolation.getParamContainerKey());

                ParamSupplier currentSupplier = paramContainer.getParam(paramUid);
                Object currentValue = currentSupplier != null ? currentSupplier.getGeneral() : null;

                if (interpolation.getAttributeName() != null){ //set attribute
                    if (!(currentValue instanceof ParamAttributeHolder))
                        throw new IllegalStateException("Can't set ParamAttribute "+interpolation.getAttributeName()+
                                " for param "+interpolation.getParamUid()+": Param not a ParamAttributeHolder");
                    ParamAttributeContainer attrCont = ((ParamAttributeHolder)currentValue).getParamAttributeContainer();
                    ParamAttribute<?> attribute = attrCont.getAttribute(interpolation.getAttributeName());
                    //TODO NPE! removing an orbit trap a second time (?) results in attribute == null
                    attribute.applyValue(interpolatedValue);
                } else { //set parameter
                    if (currentSupplier instanceof StaticParamSupplier && !interpolatedValue.equals(currentValue)) {
                        StaticParamSupplier paramSupplier = new StaticParamSupplier(currentSupplier.getUID(), interpolatedValue);
//                    paramSupplier.setLayerRelevant(true);
                        paramSupplier.setChanged(true);
                        paramContainer.addParam(paramSupplier);
                        changed = true;
                        if (paramUid.equalsIgnoreCase(CommonFractalParameters.PARAM_MIDPOINT))
                            changedMidpoint = true;
                    }
                }
            }
        }
        return changed;
    }

    public boolean containsResetAnimations(ParamConfiguration paramConfigurationCalculate, ParamConfiguration paramConfigurationDraw, boolean ignorePaused){
        if (containsResetAnimations(paramAnimations, paramConfigurationCalculate, paramConfigurationDraw, ignorePaused))
            return true;
        return false;
    }

    protected boolean containsResetAnimations(List<ParamAnimation> animationList, ParamConfiguration paramConfigurationCalculate, ParamConfiguration paramConfigurationDraw, boolean ignorePaused){

        for (ParamAnimation animation : animationList){
            if (ignorePaused && animation.isPaused())
                continue;

            for (ParamInterpolation interpolation : animation.getInterpolations().values()){
                String uid = interpolation.getParamUid();
                ParamConfiguration paramConfiguration = null;
                if (interpolation.getParamContainerKey().equals(AnimationsUI.PARAM_CONTAINERKEY_SERVER)){
                    paramConfiguration = paramConfigurationCalculate;
                }else if (interpolation.getParamContainerKey().equals(AnimationsUI.PARAM_CONTAINERKEY_CLIENT)) {
                    paramConfiguration = paramConfigurationDraw;
                }

                boolean found = false;
                for (ParamDefinition paramDef : paramConfiguration.getParameters()){
                    if (paramDef.getUID().equalsIgnoreCase(uid)){
                        if (paramDef.isResetRendererOnChange())
                            return true;
                        found = true;
                        break;
                    }
                }

                if (!found)
                    return true;
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

    public List<ClickedListener> getClickedListeners(){
        return clickedListeners;
    }

    public List<MouseMovedListener> getMouseMovedListeners(){
        return mouseMovedListeners;
    }

    public List<ScreenshotListener> getScreenshotListeners() {
        return screenshotListeners;
    }

    public List<ScreenshotListener> getSingleScreenshotListeners() {
        return singleScreenshotListeners;
    }

    public double getTime() {
        return time;
    }

    public void panned(ParamContainer paramContainer) {
        ComplexNumber midpoint = paramContainer.getParam(CommonFractalParameters.PARAM_MIDPOINT).getGeneral(ComplexNumber.class);
        for (PanListener panListener : panListeners) {
            panListener.panned(midpoint);
        }
        syncLinks();
    }

    public void syncLinks(){
        for (RendererLink link : getSourceLinks()){
            link.syncTargetRenderer();
        }
    }

    public void addPathPoint(ComplexNumber point, NumberFactory numberFactory){
        addPathPoint(point, defaultNormal, numberFactory);
    }

    public void addPathPoint(ComplexNumber point, ComplexNumber normal, NumberFactory numberFactory){
        ParamInterpolation pathPI = getSelectedParamInterpolation();
        if (pathPI != null && pathPI.getParamType().equals(AnimationsUI.PARAM_TYPE_COMPLEXNUMBER))
            pathPI.addControlPoint(point, normal, numberFactory);
    }

    public void clearPath(){
        ParamInterpolation pathPI = getSelectedParamInterpolation();
        if (pathPI != null)
            pathPI.clearControlPoints();
    }

    public ParamInterpolation getSelectedParamInterpolation(){
        return AnimationsUI.getSelectedInterpolation();
    }

    public ParamContainer getParamContainer() {
        return paramContainer;
    }

    public void setParamContainer(ParamContainer paramContainer) {
        this.paramContainer = paramContainer;
    }

    public List<ParamAnimation> getParameterAnimations() {
        return paramAnimations;
    }

    public List<RendererLink> getTargetLinks() {
        return targetLinks;
    }

    public List<RendererLink> getSourceLinks() {
        return new ArrayList<>(sourceLinks);
    }

    public void clicked(float mouseX, float mouseY, int button, ComplexNumber mappedValue) {
        clickedListeners.forEach(l -> l.clicked(mouseX, mouseY, button, mappedValue));
        singleClickedListeners.forEach(l -> l.clicked(mouseX, mouseY, button, mappedValue));
        singleClickedListeners.clear();
    }
}
