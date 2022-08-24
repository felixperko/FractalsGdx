package de.felixp.fractalsgdx.rendering.rendererlink;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.remoteclient.ClientSystem;
import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.rendering.RemoteRenderer;
import de.felixp.fractalsgdx.rendering.ShaderRenderer;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class DefaultRendererLink implements RendererLink {

    FractalRenderer sourceRenderer;
    FractalRenderer targetRenderer;

    List<String> syncedParameterNames = new ArrayList<>();

    public DefaultRendererLink(FractalRenderer sourceRenderer, FractalRenderer targetRenderer, List<String> syncedParameterNames){
        this.sourceRenderer = sourceRenderer;
        this.targetRenderer = targetRenderer;
        if (syncedParameterNames != null)
            this.syncedParameterNames.addAll(syncedParameterNames);

        this.sourceRenderer.getRendererContext().setLinkSource(this);
        this.targetRenderer.getRendererContext().setLinkTarget(this);
    }

    public void setSyncedParameter(String paramName, boolean syncNow){
        if (!this.syncedParameterNames.contains(paramName))
            this.syncedParameterNames.add(paramName);
        if (syncNow)
            syncTargetRenderer();
    }

    public void removeSyncedParameter(String paramName){
        this.syncedParameterNames.remove(paramName);
    }

    @Override
    public void syncTargetRenderer() {

        boolean changed = syncParams();

        if (changed) {
            resetRenderer(targetRenderer);
        }
    }

    public void resetRenderer(FractalRenderer renderer) {
        renderer.reset();
        if (renderer instanceof ShaderRenderer) {
//            ((ShaderRenderer) renderer).paramsChanged();
        } else if (renderer instanceof RemoteRenderer) {
            ClientSystem clientSystem = ((RemoteRenderer) renderer).getSystemInterface().getClientSystem();
            clientSystem.incrementJobId();
            renderer.getSystemContext().getParamContainer().addParam(new StaticParamSupplier("view", clientSystem.getSystemContext().getViewId()));
            clientSystem.updateConfiguration();
            clientSystem.resetAnchor();//TODO integrate...
        }
    }

    /**
     * Actually syncs the parameters. Can be overwritten.
     * @return whether target Parameters were changed
     */
    protected boolean syncParams(){

        if (!isActive())
            return false;

        boolean changed = false;
        for (String paramName : syncedParameterNames){
            ParamSupplier sourceSupp = getSourceParamContainer().getParam(paramName);
            if (sourceSupp == null)
                continue;
            ParamSupplier targetSupp = getTargetParamContainer().getParam(paramName);
            if (!sourceSupp.equals(targetSupp)) {
                getTargetParamContainer().addParam(sourceSupp);
                changed = true;
            }
        }
        return changed;
    }

    public boolean isActive(){
        return true;
    }

    @Override
    public synchronized void switchRenderers() {
        sourceRenderer.getRendererContext().removeLinkSource(this, false);
        targetRenderer.getRendererContext().removeLinkTarget(this, false);

        FractalRenderer temp = sourceRenderer;
        sourceRenderer = targetRenderer;
        targetRenderer = temp;

        sourceRenderer.getRendererContext().setLinkSource(this);
        targetRenderer.getRendererContext().setLinkTarget(this);
    }

    protected ParamContainer getSourceParamContainer(){
        return sourceRenderer.getSystemContext().getParamContainer();
    }

    protected ParamContainer getTargetParamContainer(){
        return targetRenderer.getSystemContext().getParamContainer();
    }

    @Override
    public FractalRenderer getSourceRenderer() {
        return sourceRenderer;
    }

    @Override
    public void setSourceRenderer(FractalRenderer renderer) {
        this.sourceRenderer = renderer;
    }

    @Override
    public FractalRenderer getTargetRenderer() {
        return targetRenderer;
    }

    @Override
    public void setTargetRenderer(FractalRenderer renderer) {
        this.targetRenderer = renderer;
    }
}
