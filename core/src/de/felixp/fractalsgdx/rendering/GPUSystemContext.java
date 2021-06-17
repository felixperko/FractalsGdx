package de.felixp.fractalsgdx.rendering;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstUpsampleLayer;
import de.felixperko.fractals.system.systems.common.BFOrbitCommon;
import de.felixperko.fractals.system.systems.infra.Selection;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.system.systems.infra.ViewContainer;
import de.felixperko.fractals.system.systems.stateinfo.SystemStateInfo;
import de.felixperko.fractals.system.systems.stateinfo.TaskState;
import de.felixperko.fractals.system.systems.stateinfo.TaskStateInfo;
import de.felixperko.fractals.system.task.Layer;

public class GPUSystemContext implements SystemContext {

    public static final String PARAMNAME_LAYER_CONFIG = "layerConfiguration";
    public static final String PARAMNAME_SUPERSAMPLING = "supersampling";
    public static final String PARAMNAME_MAXBORDERSAMPLES = "maxBorderSamples";

    public static final String TEXT_COND_ABS = "|z| > limit";
    public static final String TEXT_COND_ABS_R = "|re(z)| > limit";
    public static final String TEXT_COND_ABS_I = "|im(z)| > limit";

    ParamConfiguration paramConfiguration;

    LayerConfiguration layerConfig;

    ParamContainer paramContainer;
    NumberFactory nf;

    ShaderRenderer renderer;

    public GPUSystemContext(ShaderRenderer renderer){

        this.renderer = renderer;

        paramConfiguration = new ParamConfiguration();

        List<ParamDefinition> defs = new ArrayList<>();
        List<ParamSupplier> defaultValues = new ArrayList<>();

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

        defs.add(new ParamDefinition("f(z)=", "Calculator", StaticParamSupplier.class, BFOrbitCommon.stringType));
        defs.add(new ParamDefinition("iterations", "Calculator", StaticParamSupplier.class, BFOrbitCommon.integerType).withHints("ui-element:slider min=1 max=10000"));
        defs.add(new ParamDefinition("zoom", "Position", StaticParamSupplier.class, BFOrbitCommon.numberType).withHints("ui-element:slider min=0.0001 max=10"));
        ParamDefinition midpointDef = new ParamDefinition("midpoint", "Position", StaticParamSupplier.class, BFOrbitCommon.complexnumberType);
        midpointDef.setResetRendererOnChange(false);
        defs.add(midpointDef);
        defs.add(new ParamDefinition("c", "Calculator", supplierClasses, BFOrbitCommon.complexnumberType).withHints("ui-element[default]:slider min=-2 max=2"));
        defs.add(new ParamDefinition("start", "Calculator", supplierClasses, BFOrbitCommon.complexnumberType));
        defs.add(new ParamDefinition("condition", "Calculator", StaticParamSupplier.class, BFOrbitCommon.selectionType));
        defs.add(new ParamDefinition("limit", "Calculator", StaticParamSupplier.class, BFOrbitCommon.numberType).withHints("ui-element:slider min=1 max=256"));
        defs.add(new ParamDefinition(PARAMNAME_SUPERSAMPLING, "Quality", StaticParamSupplier.class, BFOrbitCommon.integerType).withHints("ui-element:slider min=1 max=10"));
        defaultValues.add(new StaticParamSupplier(PARAMNAME_SUPERSAMPLING, 1));
        defs.add(new ParamDefinition("resolutionScale", "Quality", StaticParamSupplier.class, BFOrbitCommon.doubleType).withHints("ui-element:slider min=0.0 max=2"));
        defaultValues.add(new StaticParamSupplier("resolutionScale", 1.0));
        defs.add(new ParamDefinition(PARAMNAME_MAXBORDERSAMPLES, "Quality", StaticParamSupplier.class, BFOrbitCommon.integerType));
        defaultValues.add(new StaticParamSupplier(PARAMNAME_MAXBORDERSAMPLES, 5));
//        defs.add(new ParamDefinition("width", "Calculator", StaticParamSupplier.class, BFOrbitCommon.integerType));
//        defs.add(new ParamDefinition("height", "Calculator", StaticParamSupplier.class, BFOrbitCommon.integerType));
        defs.add(new ParamDefinition(PARAMNAME_LAYER_CONFIG, "Calculator", StaticParamSupplier.class, layerconfigurationType));

        paramConfiguration.addParameterDefinitions(defs);
        paramConfiguration.addDefaultValues(defaultValues);

        Selection<String> conditionSelection = new Selection<String>("condition");
        conditionSelection.addOption(TEXT_COND_ABS, TEXT_COND_ABS, "");
        conditionSelection.addOption(TEXT_COND_ABS_R, TEXT_COND_ABS_R, "");
        conditionSelection.addOption(TEXT_COND_ABS_I, TEXT_COND_ABS_I, "");
        paramConfiguration.addSelection(conditionSelection);

        LinkedHashMap<String, ParamSupplier> map = new LinkedHashMap<>();
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
        paramContainer.addClientParameter(new StaticParamSupplier("condition", TEXT_COND_ABS));
        paramContainer.addClientParameter(new StaticParamSupplier("limit", nf.createNumber(256.0)));
        paramContainer.addClientParameter(new StaticParamSupplier(PARAMNAME_SUPERSAMPLING, 3));
        paramContainer.addClientParameter(new StaticParamSupplier("resolutionScale", 1.0));
        paramContainer.addClientParameter(new StaticParamSupplier(PARAMNAME_MAXBORDERSAMPLES, 3));
        paramContainer.addClientParameter(new StaticParamSupplier("calculator", "CustomCalculator")); //TODO add only when changed to RemoteRenderer
        List<Layer> layers = new ArrayList<>();
        layers.add(new BreadthFirstUpsampleLayer(16, BFOrbitCommon.DEFAULT_CHUNK_SIZE).with_samples(1).with_rendering(true).with_priority_shift(0));
        layers.add(new BreadthFirstUpsampleLayer(8, BFOrbitCommon.DEFAULT_CHUNK_SIZE).with_samples(1).with_rendering(true).with_priority_shift(10));
        layers.add(new BreadthFirstUpsampleLayer(4, BFOrbitCommon.DEFAULT_CHUNK_SIZE).with_samples(1).with_rendering(true).with_priority_shift(20));
        layers.add(new BreadthFirstUpsampleLayer(2, BFOrbitCommon.DEFAULT_CHUNK_SIZE).with_samples(1).with_rendering(true).with_priority_shift(30));
        layers.add(new BreadthFirstLayer(BFOrbitCommon.DEFAULT_CHUNK_SIZE).with_samples(1).with_rendering(true).with_priority_shift(40));
        layers.add(new BreadthFirstLayer(BFOrbitCommon.DEFAULT_CHUNK_SIZE).with_samples(4).with_rendering(true).with_priority_shift(50));
        layers.add(new BreadthFirstLayer(BFOrbitCommon.DEFAULT_CHUNK_SIZE).with_samples(16).with_rendering(true).with_priority_shift(60));
        layers.add(new BreadthFirstLayer(BFOrbitCommon.DEFAULT_CHUNK_SIZE).with_samples(49).with_rendering(true).with_priority_shift(70));
        layers.add(new BreadthFirstLayer(BFOrbitCommon.DEFAULT_CHUNK_SIZE).with_samples(100).with_rendering(true).with_priority_shift(80));
        layers.add(new BreadthFirstLayer(BFOrbitCommon.DEFAULT_CHUNK_SIZE).with_samples(400).with_rendering(true).with_priority_shift(90));
        paramContainer.addClientParameter(new StaticParamSupplier(PARAMNAME_LAYER_CONFIG, new PadovanLayerConfiguration(layers)));

        updateLayerConfig(paramContainer, PARAMNAME_LAYER_CONFIG, null, paramContainer.getClientParameters());
    }

    public void init() {
        RendererContext rendererContext = renderer.getRendererContext();
        if (rendererContext.getParamContainer() != null){
            this.paramContainer = rendererContext.getParamContainer();
        } else {
            rendererContext.setParamContainer(this.paramContainer);
        }
        this.paramContainer.setParamConfiguration(paramConfiguration);
        ((MainStage) FractalsGdxMain.stage).getParamUI().setServerParameterConfiguration(renderer, paramContainer, this.paramConfiguration);
    }

    public void updateSize(int width, int height){
        paramContainer.addClientParameter(new StaticParamSupplier("width", width));
        paramContainer.addClientParameter(new StaticParamSupplier("height", height));
    }

    @Override
    public boolean setParameters(ParamContainer paramContainer) {
        if (this.paramContainer != null){
            boolean changed = paramContainer.updateChangedFlag(this.paramContainer.getClientParameters());
            ParamSupplier midpointSupp = this.paramContainer.getClientParameter("midpoint");
            updateLayerConfig(paramContainer, PARAMNAME_LAYER_CONFIG, this.paramContainer.getClientParameters(), paramContainer.getClientParameters());
            paramContainer.setParamConfiguration(paramConfiguration);
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
