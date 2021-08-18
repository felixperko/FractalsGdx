package de.felixp.fractalsgdx.rendering;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.rendering.orbittrap.Orbittrap;
import de.felixp.fractalsgdx.rendering.orbittrap.OrbittrapContainer;
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
import de.felixperko.fractals.system.parameters.ExpressionsParam;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.ParamValueField;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateDiscreteModuloParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstLayer;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstUpsampleLayer;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;
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
    public static final String PARAMNAME_MAXBORDERSAMPLES = "max attempts";
    public static final String PARAMNAME_ORBITTRAPS = "orbit traps";
    public static final String PARAMNAME_SAMPLESPERFRAME = "frame samples";
    public static final String PARAMNAME_RESOLUTIONSCALE = "resolution scale";;
    public static final String PARAMNAME_FIRSTITERATIONS = "first it. %";

    public static final String TEXT_COND_ABS = "|z| > limit";
    public static final String TEXT_COND_ABS_R = "|re(z)| > limit";
    public static final String TEXT_COND_ABS_I = "|im(z)| > limit";
    public static final String TEXT_COND_ABS_MULT_RI = "|re(z)*im(z)| > limit";
    public static final String TEXT_COND_MULT_RI = "re(z)*im(z) > limit";

    ParamConfiguration paramConfig;

    LayerConfiguration layerConfig;

    ParamContainer paramContainer;
    NumberFactory nf;

    ShaderRenderer renderer;

    public GPUSystemContext(ShaderRenderer renderer){

        this.renderer = renderer;

        paramConfig = new ParamConfiguration();

        List<ParamDefinition> defs = new ArrayList<>();
        List<ParamSupplier> defaultValues = new ArrayList<>();

        paramConfig.addValueType(CommonFractalParameters.numberType);
        paramConfig.addValueType(CommonFractalParameters.complexnumberType);
        paramConfig.addValueType(CommonFractalParameters.stringType);
        paramConfig.addValueType(CommonFractalParameters.doubleType);
        paramConfig.addValueType(CommonFractalParameters.integerType);
        paramConfig.addValueType(CommonFractalParameters.listType);
        ParamValueType layerconfigurationType = new ParamValueType("LayerConfiguration",
                new ParamValueField("layers", CommonFractalParameters.listType),
                new ParamValueField("simStep", CommonFractalParameters.doubleType, 0.05),
                new ParamValueField("simCount", CommonFractalParameters.integerType, 20),
                new ParamValueField("seed", CommonFractalParameters.integerType, 42));
        paramConfig.addValueType(layerconfigurationType);
        ParamValueType orbittrapContainerType = new ParamValueType(PARAMNAME_ORBITTRAPS, new ParamValueField[0]);
        paramConfig.addValueType(orbittrapContainerType);

        List<Class<? extends ParamSupplier>> supplierClasses = new ArrayList<>();
        supplierClasses.add(StaticParamSupplier.class);
        supplierClasses.add(CoordinateBasicShiftParamSupplier.class);
        supplierClasses.add(CoordinateDiscreteModuloParamSupplier.class);

        nf = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

        defs.add(new ParamDefinition(CommonFractalParameters.PARAM_EXPRESSIONS, "Calculator", StaticParamSupplier.class, CommonFractalParameters.expressionsType));
        defs.add(new ParamDefinition("iterations", "Calculator", StaticParamSupplier.class, CommonFractalParameters.integerType).withHints("ui-element:slider min=1 max=10000"));
        defs.add(new ParamDefinition("zoom", "Position", StaticParamSupplier.class, CommonFractalParameters.numberType).withHints("ui-element:slider min=0.0001 max=10"));
        ParamDefinition midpointDef = new ParamDefinition("midpoint", "Position", StaticParamSupplier.class, CommonFractalParameters.complexnumberType);
        midpointDef.setResetRendererOnChange(false);
        defs.add(midpointDef);
        defs.add(new ParamDefinition("c", "Calculator", supplierClasses, CommonFractalParameters.complexnumberType).withHints("ui-element[default]:slider min=-2 max=2"));
        defs.add(new ParamDefinition(CommonFractalParameters.PARAM_ZSTART, "Calculator", supplierClasses, CommonFractalParameters.complexnumberType).withHints("ui-element:slider min=-1 max=1"));
        defs.add(new ParamDefinition("condition", "Calculator", StaticParamSupplier.class, CommonFractalParameters.selectionType));
        defs.add(new ParamDefinition("limit", "Calculator", StaticParamSupplier.class, CommonFractalParameters.numberType).withHints("ui-element:slider min=1 max=256"));
        defs.add(new ParamDefinition(PARAMNAME_FIRSTITERATIONS, "Quality", StaticParamSupplier.class, CommonFractalParameters.numberType).withHints("ui-element:slider min=1 max=100"));
        defaultValues.add(new StaticParamSupplier(PARAMNAME_FIRSTITERATIONS, nf.createNumber("20.0")));
        defs.add(new ParamDefinition(PARAMNAME_SUPERSAMPLING, "Quality", StaticParamSupplier.class, CommonFractalParameters.integerType).withHints("ui-element:slider min=1 max=200"));
        defaultValues.add(new StaticParamSupplier(PARAMNAME_SUPERSAMPLING, 1));
        defs.add(new ParamDefinition(PARAMNAME_RESOLUTIONSCALE, "Quality", StaticParamSupplier.class, CommonFractalParameters.doubleType).withHints("ui-element:slider min=0.0 max=2"));
        defaultValues.add(new StaticParamSupplier(PARAMNAME_RESOLUTIONSCALE, 1.0));
        defs.add(new ParamDefinition(PARAMNAME_SAMPLESPERFRAME, "Quality", StaticParamSupplier.class, CommonFractalParameters.integerType));
        defaultValues.add(new StaticParamSupplier(PARAMNAME_SAMPLESPERFRAME, 1));
        defs.add(new ParamDefinition(PARAMNAME_MAXBORDERSAMPLES, "Quality", StaticParamSupplier.class, CommonFractalParameters.integerType));
        defaultValues.add(new StaticParamSupplier(PARAMNAME_MAXBORDERSAMPLES, 5));
//        defs.add(new ParamDefinition("width", "Calculator", StaticParamSupplier.class, CommonFractalParameters.integerType));
//        defs.add(new ParamDefinition("height", "Calculator", StaticParamSupplier.class, CommonFractalParameters.integerType));
        defs.add(new ParamDefinition(PARAMNAME_LAYER_CONFIG, "Calculator", StaticParamSupplier.class, layerconfigurationType));
        defs.add(new ParamDefinition(PARAMNAME_ORBITTRAPS, "Calculator", StaticParamSupplier.class, orbittrapContainerType));

        paramConfig.addParameterDefinitions(defs);
        paramConfig.addDefaultValues(defaultValues);

        Selection<String> conditionSelection = new Selection<String>("condition");
        conditionSelection.addOption(TEXT_COND_ABS, TEXT_COND_ABS, "");
        conditionSelection.addOption(TEXT_COND_ABS_R, TEXT_COND_ABS_R, "");
        conditionSelection.addOption(TEXT_COND_ABS_I, TEXT_COND_ABS_I, "");
        conditionSelection.addOption(TEXT_COND_ABS_MULT_RI, TEXT_COND_ABS_MULT_RI, "");
        conditionSelection.addOption(TEXT_COND_MULT_RI, TEXT_COND_MULT_RI, "");
        paramConfig.addSelection(conditionSelection);

        LinkedHashMap<String, ParamSupplier> map = new LinkedHashMap<>();
        paramContainer = new ParamContainer(map);

        paramContainer.addClientParameter(new StaticParamSupplier("numberFactory", nf));
        paramContainer.addClientParameter(new StaticParamSupplier("iterations", 1000));
        paramContainer.addClientParameter(new StaticParamSupplier("midpoint", nf.createComplexNumber(0,0)));
        paramContainer.addClientParameter(new CoordinateBasicShiftParamSupplier("c"));
        paramContainer.addClientParameter(new StaticParamSupplier("zoom", nf.createNumber(3)));
        if (renderer.juliaset)
            paramContainer.addClientParameter(new CoordinateBasicShiftParamSupplier(CommonFractalParameters.PARAM_ZSTART));
        else
            paramContainer.addClientParameter(new StaticParamSupplier(CommonFractalParameters.PARAM_ZSTART, nf.createComplexNumber(0,0)));
        ExpressionsParam expressions = new ExpressionsParam("z^2+c", "z");
        expressions.putExpression("c", "c");
        paramContainer.addClientParameter(new StaticParamSupplier(CommonFractalParameters.PARAM_EXPRESSIONS, expressions));
        paramContainer.addClientParameter(new StaticParamSupplier("condition", TEXT_COND_ABS));
        paramContainer.addClientParameter(new StaticParamSupplier("limit", nf.createNumber(256.0)));
        paramContainer.addClientParameter(new StaticParamSupplier(PARAMNAME_FIRSTITERATIONS, nf.createNumber("20.0")));
        paramContainer.addClientParameter(new StaticParamSupplier(PARAMNAME_SUPERSAMPLING, 3));
        paramContainer.addClientParameter(new StaticParamSupplier(PARAMNAME_SAMPLESPERFRAME, 1));
        paramContainer.addClientParameter(new StaticParamSupplier(PARAMNAME_RESOLUTIONSCALE, 1.0));
        paramContainer.addClientParameter(new StaticParamSupplier(PARAMNAME_MAXBORDERSAMPLES, 1));
        paramContainer.addClientParameter(new StaticParamSupplier("calculator", "CustomCalculator")); //TODO add only when changed to RemoteRenderer
        List<Layer> layers = new ArrayList<>();
        layers.add(new BreadthFirstUpsampleLayer(16, CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(1).with_rendering(true).with_priority_shift(0));
        layers.add(new BreadthFirstUpsampleLayer(8, CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(1).with_rendering(true).with_priority_shift(10));
        layers.add(new BreadthFirstUpsampleLayer(4, CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(1).with_rendering(true).with_priority_shift(20));
        layers.add(new BreadthFirstUpsampleLayer(2, CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(1).with_rendering(true).with_priority_shift(30));
        layers.add(new BreadthFirstLayer(CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(1).with_rendering(true).with_priority_shift(40));
        layers.add(new BreadthFirstLayer(CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(4).with_rendering(true).with_priority_shift(50));
        layers.add(new BreadthFirstLayer(CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(16).with_rendering(true).with_priority_shift(60));
        layers.add(new BreadthFirstLayer(CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(49).with_rendering(true).with_priority_shift(70));
        layers.add(new BreadthFirstLayer(CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(100).with_rendering(true).with_priority_shift(80));
        layers.add(new BreadthFirstLayer(CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(400).with_rendering(true).with_priority_shift(90));
        paramContainer.addClientParameter(new StaticParamSupplier(PARAMNAME_LAYER_CONFIG, new PadovanLayerConfiguration(layers)));
        List<Orbittrap> orbittraps = new ArrayList<>();
//        orbittraps.add(new AxisOrbittrap(1, nf, nf.createNumber(0.1), nf.createNumber(0.05), nf.createNumber("0.0"), true));
//        orbittraps.add(new AxisOrbittrap(2, nf, nf.createNumber(0.2), nf.createNumber(0.05), nf.createNumber("0.0"), false));
//        orbittraps.add(new CircleOrbittrap(3, nf.createComplexNumber(-0.2, -0.3), nf.createNumber(0.01)));
        paramContainer.addClientParameter(new StaticParamSupplier(PARAMNAME_ORBITTRAPS, new OrbittrapContainer(orbittraps)));

        updateLayerConfig(paramContainer, PARAMNAME_LAYER_CONFIG, null, paramContainer.getClientParameters());
    }

    public void init() {
        RendererContext rendererContext = renderer.getRendererContext();
        if (rendererContext.getParamContainer() != null){
            this.paramContainer = rendererContext.getParamContainer();
        } else {
            rendererContext.setParamContainer(this.paramContainer);
        }
        this.paramContainer.setParamConfiguration(paramConfig);
        ((MainStage) FractalsGdxMain.stage).getParamUI().setServerParameterConfiguration(renderer, paramContainer, this.paramConfig);
    }

    public void updateSize(int width, int height){
        paramContainer.addClientParameter(new StaticParamSupplier("width", width));
        paramContainer.addClientParameter(new StaticParamSupplier("height", height));
    }

    @Override
    public boolean setParameters(ParamContainer paramContainer) {
        if (this.paramContainer != null){
            boolean changed = paramContainer.updateChangedFlag(this.paramContainer.getClientParameters());
            ParamSupplier midpointSupp = paramContainer.getClientParameter("midpoint");
            updateLayerConfig(paramContainer, PARAMNAME_LAYER_CONFIG, this.paramContainer.getClientParameters(), paramContainer.getClientParameters());
            paramContainer.setParamConfiguration(paramConfig);
            boolean sameInstance = this.paramContainer == paramContainer;
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
        if (newLayerConfigSupplier == null)
            return;
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
        return paramConfig;
    }

    @Override
    public void setParamConfiguration(ParamConfiguration paramConfiguration) {
        this.paramConfig = paramConfiguration;
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
