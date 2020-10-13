package de.felixp.fractalsgdx.rendering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.fractals.data.AbstractArrayChunk;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.network.infra.connection.ServerConnection;
import de.felixperko.fractals.system.LayerConfiguration;
import de.felixperko.fractals.system.PadovanLayerConfiguration;
import de.felixperko.fractals.system.calculator.infra.DeviceType;
import de.felixperko.fractals.system.calculator.infra.FractalsCalculator;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.numbers.impl.DoubleComplexNumber;
import de.felixperko.fractals.system.numbers.impl.DoubleNumber;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.ParamValueField;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstLayer;
import de.felixperko.fractals.system.systems.common.BFOrbitCommon;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.system.systems.infra.ViewContainer;
import de.felixperko.fractals.system.systems.stateinfo.SystemStateInfo;
import de.felixperko.fractals.system.systems.stateinfo.TaskState;
import de.felixperko.fractals.system.systems.stateinfo.TaskStateInfo;
import de.felixperko.fractals.system.task.Layer;

public class GPUSystemContext implements SystemContext {

    public static final String PARAM_NAME_LAYER_CONFIG = "layerConfiguration";

    ParamConfiguration paramConfiguration;

    LayerConfiguration layerConfig;

    ParamContainer paramContainer;
    NumberFactory nf;

    ShaderRenderer renderer;

    public GPUSystemContext(ShaderRenderer renderer){

        this.renderer = renderer;

        paramConfiguration = new ParamConfiguration();

        List<ParamDefinition> defs = new ArrayList<>();

        paramConfiguration.addValueType(BFOrbitCommon.numberType);
        paramConfiguration.addValueType(BFOrbitCommon.complexnumberType);
        paramConfiguration.addValueType(BFOrbitCommon.stringType);
        paramConfiguration.addValueType(BFOrbitCommon.doubleType);
        paramConfiguration.addValueType(BFOrbitCommon.integerType);
        paramConfiguration.addValueType(BFOrbitCommon.listType);
        ParamValueType layerconfigurationType = new ParamValueType("LayerConfiguration",
                new ParamValueField("layers", BFOrbitCommon.listType),
                new ParamValueField("simStep", BFOrbitCommon.doubleType, 0.05),
                new ParamValueField("simCount", BFOrbitCommon.integerType, 20),
                new ParamValueField("seed", BFOrbitCommon.integerType, 42));
        paramConfiguration.addValueType(layerconfigurationType);

        List<Class<? extends ParamSupplier>> supplierClasses = new ArrayList<>();
        supplierClasses.add(StaticParamSupplier.class);
        supplierClasses.add(CoordinateBasicShiftParamSupplier.class);

        defs.add(new ParamDefinition("iterations", "Calculator", StaticParamSupplier.class, BFOrbitCommon.integerType));
        defs.add(new ParamDefinition("zoom", "Position", StaticParamSupplier.class, BFOrbitCommon.numberType));
        defs.add(new ParamDefinition("midpoint", "Position", StaticParamSupplier.class, BFOrbitCommon.complexnumberType));
        defs.add(new ParamDefinition("c", "Calculator", supplierClasses, BFOrbitCommon.complexnumberType));
        defs.add(new ParamDefinition("start", "Calculator", supplierClasses, BFOrbitCommon.complexnumberType));
        defs.add(new ParamDefinition("f(z)=", "Calculator", StaticParamSupplier.class, BFOrbitCommon.stringType));
        defs.add(new ParamDefinition("limit", "Calculator", StaticParamSupplier.class, BFOrbitCommon.doubleType));
        defs.add(new ParamDefinition("resolutionScale", "Calculator", StaticParamSupplier.class, BFOrbitCommon.doubleType));
        defs.add(new ParamDefinition("width", "Calculator", StaticParamSupplier.class, BFOrbitCommon.integerType));
        defs.add(new ParamDefinition("height", "Calculator", StaticParamSupplier.class, BFOrbitCommon.integerType));
        defs.add(new ParamDefinition(PARAM_NAME_LAYER_CONFIG, "Calculator", StaticParamSupplier.class, layerconfigurationType));

        paramConfiguration.addParameterDefinitions(defs);

        Map<String, ParamSupplier> map = new HashMap<>();
        paramContainer = new ParamContainer(map);

        nf = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

        paramContainer.addClientParameter(new StaticParamSupplier("numberFactory", nf));
        paramContainer.addClientParameter(new StaticParamSupplier("iterations", 1000));
        paramContainer.addClientParameter(new StaticParamSupplier("midpoint", nf.createComplexNumber(0,0)));
        paramContainer.addClientParameter(new CoordinateBasicShiftParamSupplier("c"));
        paramContainer.addClientParameter(new StaticParamSupplier("zoom", nf.createNumber(3)));
        if (renderer.juliaset)
            paramContainer.addClientParameter(new CoordinateBasicShiftParamSupplier("start"));
        else
            paramContainer.addClientParameter(new StaticParamSupplier("start", nf.createComplexNumber(0,0)));
        paramContainer.addClientParameter(new StaticParamSupplier("f(z)=", "z^2+c"));
        paramContainer.addClientParameter(new StaticParamSupplier("limit", 256.0));
        paramContainer.addClientParameter(new StaticParamSupplier("resolutionScale", 1.0));
        List<Layer> layers = new ArrayList<>();
        layers.add(new BreadthFirstLayer(1));
        paramContainer.addClientParameter(new StaticParamSupplier(PARAM_NAME_LAYER_CONFIG, new PadovanLayerConfiguration(layers)));

        updateLayerConfig(paramContainer, PARAM_NAME_LAYER_CONFIG, null, paramContainer.getClientParameters());
    }

    public void init() {
        RendererContext rendererContext = renderer.getRendererContext();
        if (rendererContext.getParamContainer() != null){
            this.paramContainer = rendererContext.getParamContainer();
        } else {
            rendererContext.setParamContainer(this.paramContainer);
        }
        ((MainStage) FractalsGdxMain.stage).getParamUI().setServerParameterConfiguration(renderer, paramContainer, this.paramConfiguration);
    }

    public void updateSize(int width, int height){
        paramContainer.addClientParameter(new StaticParamSupplier("width", width));
        paramContainer.addClientParameter(new StaticParamSupplier("height", height));
    }

    @Override
    public boolean setParameters(ParamContainer paramContainer) {
        if (this.paramContainer != null && paramContainer != this.paramContainer){
            boolean changed = this.paramContainer.updateChangedFlag(paramContainer.getClientParameters());
            updateLayerConfig(paramContainer, PARAM_NAME_LAYER_CONFIG, this.paramContainer.getClientParameters(), paramContainer.getClientParameters());
            this.paramContainer = paramContainer;
            if (changed)
                renderer.paramsChanged();
//            paramContainer.setParameters(paramContainer.getParameters(), true);
        }
        return false;
    }

    protected void updateLayerConfig(ParamContainer paramContainer, String layerConfigParamName, Map<String, ParamSupplier> oldParams, Map<String, ParamSupplier> newParams) {
        LayerConfiguration oldLayerConfig = null;
        if (oldParams != null)
            oldLayerConfig = oldParams.get(layerConfigParamName).getGeneral(LayerConfiguration.class);
        ParamSupplier newLayerConfigSupplier = paramContainer.getClientParameter(layerConfigParamName);
        LayerConfiguration newLayerConfig = newLayerConfigSupplier.getGeneral(LayerConfiguration.class);
        if (oldLayerConfig == null || newLayerConfigSupplier.isChanged()) {
            layerConfig = newLayerConfig;
            layerConfig.prepare(nf);
        } else {
            newParams.put(layerConfigParamName, oldParams.get(layerConfigParamName));
        }
    }

    @Override
    public Layer getLayer(int i) {
        return layerConfig.getLayer(i);
    }

    @Override
    public NumberFactory getNumberFactory() {
        return paramContainer.getClientParameter("numberFactory").getGeneral(NumberFactory.class);
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
        return paramContainer.getClientParameters();
    }

    @Override
    public Object getParamValue(String s) {
        return paramContainer.getClientParameter(s).getGeneral();
    }

    @Override
    public ViewContainer getViewContainer() {
        return null;
    }

    @Override
    public LayerConfiguration getLayerConfiguration() {
        return layerConfig;
    }

    @Override
    public Number getPixelzoom() {
        Number zoom = ((Number)getParamValue("zoom")).copy();
        Number height = nf.createNumber(renderer.getHeight());
        zoom.div(height);
        return zoom;
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
        StaticParamSupplier supplier = new StaticParamSupplier("midpoint", midpoint);
        supplier.updateChanged(paramContainer.getClientParameter("midpoint"));
        paramContainer.addClientParameter(supplier);
    }

    @Override
    public ComplexNumber getMidpoint() {
        return paramContainer.getClientParameter("midpoint").getGeneral(ComplexNumber.class);
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
        return paramConfiguration;
    }

    @Override
    public void setParamConfiguration(ParamConfiguration paramConfiguration) {
        this.paramConfiguration = paramConfiguration;
    }

    @Override
    public Object getParamValue(String s, Class aClass, ComplexNumber complexNumber, int i, int i1) {
        return paramContainer.getClientParameter(s).get(this, complexNumber, i, i1);
    }

    @Override
    public Object getParamValue(String s, Class aClass) {
        return paramContainer.getClientParameter(s).getGeneral(aClass);
    }
}
