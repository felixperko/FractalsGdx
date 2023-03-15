package de.felixp.fractalsgdx.rendering.renderers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.rendering.ParamListener;
import de.felixperko.fractals.data.AbstractArrayChunk;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.network.infra.connection.ServerConnection;
import de.felixperko.fractals.system.LayerConfiguration;
import de.felixperko.fractals.system.calculator.infra.DeviceType;
import de.felixperko.fractals.system.calculator.infra.FractalsCalculator;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.system.systems.infra.ViewContainer;
import de.felixperko.fractals.system.systems.stateinfo.SystemStateInfo;
import de.felixperko.fractals.system.systems.stateinfo.TaskState;
import de.felixperko.fractals.system.systems.stateinfo.TaskStateInfo;
import de.felixperko.fractals.system.task.Layer;

public abstract class AbstractSystemContext implements SystemContext {

    ParamConfiguration paramConfig;
    ParamContainer paramContainer;
    NumberFactory nf;

    List<ParamListener> paramListenersAll = new ArrayList<>();
    Map<String, List<ParamListener>> paramListenersSingle = new HashMap<>();

    @Override
    public boolean setParameters(ParamContainer paramContainer) {
        boolean changed = false;
        if (this.paramContainer != null){
            changed = paramContainer.updateChangedFlag(this.paramContainer.getParamMap());
            paramContainer.setParamConfiguration(paramConfig);
            for (ParamSupplier supp : paramContainer.getParameters()){
                if (supp.isChanged()){
                    ParamSupplier oldSupp = this.paramContainer.getParam(supp.getUID());
                    paramListenersAll.forEach(listener -> listener.changed(supp, oldSupp));
                    if (paramListenersSingle.containsKey(supp.getUID())){
                        paramListenersSingle.get(supp.getUID()).forEach(listener -> listener.changed(supp, oldSupp));
                    }
                }
            }
//            paramContainer.setParameters(paramContainer.getParameters(), true);
        }
        this.paramContainer = new ParamContainer(paramContainer, true);
        return changed;
    }

    public void addParamListener(ParamListener paramListener){
        if (paramListener.getUid() == null){
            paramListenersAll.add(paramListener);
        }
        else {
            List<ParamListener> listeners = paramListenersSingle.get(paramListener.getUid());
            if (listeners == null){
                listeners = new ArrayList<>();
                paramListenersSingle.put(paramListener.getUid(), listeners);
            }
            listeners.add(paramListener);
        }
    }

    public void removeParamListener(ParamListener paramListener){
        if (paramListener.getUid() == null){
            paramListenersAll.remove(paramListener);
        }
        else {
            paramListenersSingle.get(paramListener.getUid()).remove(paramListener);
        }
    }

    @Override
    public NumberFactory getNumberFactory() {
        return paramContainer.getParam(CommonFractalParameters.PARAM_NUMBERFACTORY).getGeneral(NumberFactory.class);
    }

    @Override
    public void taskStateUpdated(TaskStateInfo taskStateInfo, TaskState taskState) {

    }

    @Override
    public void setServerConnection(ServerConnection serverConnection) {

    }

    @Override
    public FractalsCalculator createCalculator(DeviceType deviceType) {
        return null;
    }

    @Override
    public AbstractArrayChunk createChunk(int i, int i1) {
        return null;
    }

    @Override
    public ParamContainer getParamContainer() {
        return paramContainer;
    }

    @Override
    public Map<String, ParamSupplier> getParameters() {
        return paramContainer.getParamMap();
    }

    @Override
    public Map<String, ParamSupplier> getParametersByName() {
        Map<String, ParamSupplier> paramsByUid = getParametersByUID();
        Map<String, ParamSupplier> paramsByName = new HashMap<>();
        for (String uid : paramsByUid.keySet()){
            ParamSupplier supp = paramsByUid.get(uid);
            String name = paramConfig.getName(uid);
            paramsByName.put(name, supp);
        }
        return paramsByName;
    }

    @Override
    public Map<String, ParamSupplier> getParametersByUID() {
        return paramContainer.getParamMap();
    }

    @Override
    public Object getParamValue(String s) {
        ParamSupplier supp = paramContainer.getParam(s);
        if (supp != null)
            return supp.getGeneral();
        return null;
    }

    @Override
    public ViewContainer getViewContainer() {
        return null;
    }

    @Override
    public LayerConfiguration getLayerConfiguration() {
        return null;
    }

    @Override
    public Number getPixelzoom() {
        return null;
    }

    @Override
    public int getChunkSize() {
        return 0;
    }

    @Override
    public void incrementViewId() {

    }

    @Override
    public void setMidpoint(ComplexNumber midpoint) {
        StaticParamSupplier supplier = new StaticParamSupplier(CommonFractalParameters.PARAM_MIDPOINT, midpoint);
        supplier.updateChanged(paramContainer.getParam(CommonFractalParameters.PARAM_MIDPOINT));
        paramContainer.addParam(supplier);
    }

    @Override
    public ComplexNumber getMidpoint() {
        return paramContainer.getParam(CommonFractalParameters.PARAM_MIDPOINT).getGeneral(ComplexNumber.class);
    }

    @Override
    public int getViewId() {
        return 0;
    }

    @Override
    public void setViewId(Integer integer) {

    }

    @Override
    public SystemStateInfo getSystemStateInfo() {
        return null;
    }

    @Override
    public ParamConfiguration getParamConfiguration() {
        return paramConfig;
    }

    @Override
    public void setParamConfiguration(ParamConfiguration paramConfiguration) {
        this.paramConfig = paramConfiguration;
    }

    @Override
    public Object getParamValue(String s, Class aClass, ComplexNumber complexNumber, int i, int i1) {
        return paramContainer.getParam(s).get(this, complexNumber, i, i1);
    }

    @Override
    public Object getParamValue(String s, Class aClass) {
        return paramContainer.getParam(s).getGeneral(aClass);
    }

    @Override
    public Layer getLayer(int layerId) {
        return null;
    }
}
