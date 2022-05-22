package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.HashMap;
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
import de.felixperko.fractals.system.parameters.suppliers.CoordinateDiscreteParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateModuloParamSupplier;
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

public class ShaderSystemContext implements SystemContext {

    public static final String PARAM_LAYER_CONFIG = "u2EpDL";
    public static final String PARAM_SUPERSAMPLING = "KVZXq5";
    public static final String PARAM_MAXBORDERSAMPLES = "l0q4Zw";
    public static final String PARAM_ORBITTRAPS = "F19y7E";
    public static final String PARAM_SAMPLESPERFRAME = "RUz-Io";
    public static final String PARAM_RESOLUTIONSCALE = "82OM9P";
    public static final String PARAM_FIRSTITERATIONS = "VE0UQY";
    public static final String PARAM_GRID_PERIOD = "WwYfLG";
    public static final String PARAM_MODULO_PERIOD = "NjALQA";
    public static final String PARAM_UNSTABLE_OUTPUT = "ByPJDe";
    public static final String PARAM_STABLE_OUTPUT = "RkXg81";
    public static final String PARAM_TARGET_FRAMERATE = "db2FEt";
    public static final String PARAM_CALCULATOR = "DVm4Oz";
    public static final String PARAM_PRIORITY = "yIHzft";
    public static final String PARAM_PRECISION = "wOLGBM";
    public static final String PARAM_CONDITION = "7QB43U";
    public static final String PARAM_LIMIT = "DReboF";
    public static final String PARAM_ZOOM = "MlDwyj";

    public static final String PARAMNAME_LAYER_CONFIG = "layerConfiguration";
    public static final String PARAMNAME_SUPERSAMPLING = "supersampling";
    public static final String PARAMNAME_MAXBORDERSAMPLES = "fallback samples";
    public static final String PARAMNAME_ORBITTRAPS = "orbit traps";
    public static final String PARAMNAME_SAMPLESPERFRAME = "samples per frame";
    public static final String PARAMNAME_RESOLUTIONSCALE = "resolution scale";
    public static final String PARAMNAME_FIRSTITERATIONS = "first sample %";
    public static final String PARAMNAME_GRID_PERIOD = "grid size";
    public static final String PARAMNAME_MODULO_PERIOD = "cell range";
    public static final String PARAMNAME_CONDITION = "condition";
    public static final String PARAMNAME_LIMIT = "limit";
    public static final String PARAMNAME_UNSTABLE_OUTPUT = "output (condition)";
    public static final String PARAMNAME_STABLE_OUTPUT = "output (fallback)";
    public static final String PARAMNAME_TARGET_FRAMERATE = "min target fps";
    public static final String PARAMNAME_CALCULATOR = "calculator";
    public static final String PARAMNAME_PRIORITY = "renderer priority";
    public static final String PARAMNAME_PRECISION = "precision";
    public static final String PARAMNAME_ITERATIONS = "iterations";
    public static final String PARAMNAME_C = "c";
    public static final String PARAMNAME_ZOOM = "zoom";
    public static final String PARAMNAME_MIDPOINT = "midpoint";

    public static final String TEXT_PRECISION_AUTO = "auto";
    public static final String TEXT_PRECISION_32 = "32 bit";
    public static final String TEXT_PRECISION_64 = "64 bit (double)";
    public static final String TEXT_PRECISION_64_EMULATED = "64 bit (float-float)";
//    public static final String TEXT_PRECISION_64_FAST = "64 bit (fast)";

    public static final String TEXT_CALCULATOR_ESCAPETIME = "iteration fractal";
    public static final String TEXT_CALCULATOR_NEWTONFRACTAL = "newton fractal";
    public static final String TEXT_CALCULATOR_GRAPH_REAL = "graph (real)";
    public static final String TEXT_CALCULATOR_GRAPH_COMPLEX = "graph (complex)";

    public static final String TEXT_COND_ABS = "|z| > limit";
    public static final String TEXT_COND_ABS_R = "|re(z)| > limit";
    public static final String TEXT_COND_ABS_I = "|im(z)| > limit";
    public static final String TEXT_COND_ABS_MULT_RI = "|re(z)*im(z)| > limit";
    public static final String TEXT_COND_MULT_RI = "re(z)*im(z) > limit";

    public static final String TEXT_UNSTABLE_OUTPUT_ITERATIONS = "smoothed iterations";
    public static final String TEXT_STABLE_OUTPUT_NONE = "none";
    public static final String TEXT_STABLE_OUTPUT_MOVED = "moved distance";
    public static final String TEXT_STABLE_OUTPUT_ANGLE = "average angle";

    public static final int SAMPLES_DEFAULT = 50;
    public static final int SAMPLES_DEFAULT_ANDROID = 10;

    ParamConfiguration paramConfig;

    LayerConfiguration layerConfig;

    ParamContainer paramContainer;
    NumberFactory nf;

    ShaderRenderer renderer;

    int samples = 50;

    public ShaderSystemContext(ShaderRenderer renderer){

        this.renderer = renderer;

        setSamples(getDefaultSamples());

        paramConfig = new ParamConfiguration();

        List<ParamDefinition> defs = new ArrayList<>();
        List<ParamSupplier> defaultValues = new ArrayList<>();

        paramConfig.addValueType(CommonFractalParameters.numberfactoryType);
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
        supplierClasses.add(CoordinateDiscreteParamSupplier.class);
        supplierClasses.add(CoordinateModuloParamSupplier.class);

        nf = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

        String cat_calc = "Calculator";
        defs.add(new ParamDefinition(PARAM_CALCULATOR, PARAMNAME_CALCULATOR, cat_calc, StaticParamSupplier.class, CommonFractalParameters.selectionType));
        defs.add(new ParamDefinition(CommonFractalParameters.PARAM_NUMBERFACTORY, "nf", cat_calc, StaticParamSupplier.class, CommonFractalParameters.numberfactoryType));
        defs.add(new ParamDefinition(CommonFractalParameters.PARAM_EXPRESSIONS, CommonFractalParameters.PARAMNAME_EXPRESSIONS, cat_calc, StaticParamSupplier.class, CommonFractalParameters.expressionsType));
        defs.add(new ParamDefinition(CommonFractalParameters.PARAM_ITERATIONS, PARAMNAME_ITERATIONS, cat_calc, StaticParamSupplier.class, CommonFractalParameters.integerType).withHints("ui-element:slider min=1 max=10000"));
        defs.add(new ParamDefinition(CommonFractalParameters.PARAM_C, PARAMNAME_C, cat_calc, supplierClasses, CommonFractalParameters.complexnumberType).withHints("ui-element[default]:slider min=-2 max=2"));
        defs.add(new ParamDefinition(CommonFractalParameters.PARAM_ZSTART, CommonFractalParameters.PARAMNAME_ZSTART, cat_calc, supplierClasses, CommonFractalParameters.complexnumberType).withHints("ui-element[default]:slider min=-1 max=1"));

        String cat_quality = "Quality/Performance";
        ParamDefinition def_supersampling = new ParamDefinition(PARAM_SUPERSAMPLING, PARAMNAME_SUPERSAMPLING, cat_quality, StaticParamSupplier.class, CommonFractalParameters.integerType).withHints("ui-element:slider min=1 max=200");
        def_supersampling.setResetRendererOnChange(false);
        defs.add(def_supersampling);
        defaultValues.add(new StaticParamSupplier(PARAM_SUPERSAMPLING, 1));
        defs.add(new ParamDefinition(PARAM_MAXBORDERSAMPLES, PARAMNAME_MAXBORDERSAMPLES, cat_quality, StaticParamSupplier.class, CommonFractalParameters.integerType));
        defaultValues.add(new StaticParamSupplier(PARAM_MAXBORDERSAMPLES, 1));
        defs.add(new ParamDefinition(PARAM_RESOLUTIONSCALE, PARAMNAME_RESOLUTIONSCALE, cat_quality, StaticParamSupplier.class, CommonFractalParameters.doubleType).withHints("ui-element:slider min=0.0 max=2"));
        defaultValues.add(new StaticParamSupplier(PARAM_RESOLUTIONSCALE, 1.0));
        defs.add(new ParamDefinition(PARAM_TARGET_FRAMERATE, PARAMNAME_TARGET_FRAMERATE, cat_quality, StaticParamSupplier.class, CommonFractalParameters.integerType));
        defaultValues.add(new StaticParamSupplier(PARAM_TARGET_FRAMERATE, 30));
        defs.add(new ParamDefinition(PARAM_FIRSTITERATIONS, PARAMNAME_FIRSTITERATIONS, cat_quality, StaticParamSupplier.class, CommonFractalParameters.numberType).withHints("ui-element:slider min=1 max=100"));
        defaultValues.add(new StaticParamSupplier(PARAM_FIRSTITERATIONS, nf.createNumber("100.0")));
        defs.add(new ParamDefinition(PARAM_SAMPLESPERFRAME, PARAMNAME_SAMPLESPERFRAME, cat_quality, StaticParamSupplier.class, CommonFractalParameters.integerType));
        defaultValues.add(new StaticParamSupplier(PARAM_SAMPLESPERFRAME, 1));

        String cat_advanced = "Advanced";
        defs.add(new ParamDefinition(PARAM_CONDITION, PARAMNAME_CONDITION, cat_advanced, StaticParamSupplier.class, CommonFractalParameters.selectionType));
        defs.add(new ParamDefinition(PARAM_LIMIT, PARAMNAME_LIMIT, cat_advanced, StaticParamSupplier.class, CommonFractalParameters.numberType).withHints("ui-element:slider min=1 max=256"));
        defs.add(new ParamDefinition(PARAM_UNSTABLE_OUTPUT, PARAMNAME_UNSTABLE_OUTPUT, cat_advanced, StaticParamSupplier.class, CommonFractalParameters.selectionType));
        defaultValues.add(new StaticParamSupplier(PARAM_UNSTABLE_OUTPUT, 0));
        defs.add(new ParamDefinition(PARAM_STABLE_OUTPUT, PARAMNAME_STABLE_OUTPUT, cat_advanced, StaticParamSupplier.class, CommonFractalParameters.selectionType));
        defs.add(new ParamDefinition(PARAM_PRECISION, PARAMNAME_PRECISION, cat_advanced, StaticParamSupplier.class, CommonFractalParameters.selectionType));
        defs.add(new ParamDefinition(PARAM_ORBITTRAPS, PARAMNAME_ORBITTRAPS, cat_advanced, StaticParamSupplier.class, orbittrapContainerType));
        defs.add(new ParamDefinition(PARAM_PRIORITY, PARAMNAME_PRIORITY, cat_advanced, StaticParamSupplier.class, CommonFractalParameters.numberType));
        defaultValues.add(new StaticParamSupplier(PARAM_PRIORITY, nf.createNumber(10.0)));

        String cat_mapping = "Mapping";
        defs.add(new ParamDefinition(PARAM_ZOOM, PARAMNAME_ZOOM, cat_mapping, StaticParamSupplier.class, CommonFractalParameters.numberType).withHints("ui-element:slider min=0.0001 max=10"));
        ParamDefinition midpointDef = new ParamDefinition(CommonFractalParameters.PARAM_MIDPOINT, PARAMNAME_MIDPOINT, cat_mapping, StaticParamSupplier.class, CommonFractalParameters.complexnumberType);
        midpointDef.setResetRendererOnChange(false);
        defs.add(midpointDef);
        defs.add(new ParamDefinition(PARAM_GRID_PERIOD, PARAMNAME_GRID_PERIOD, cat_mapping, StaticParamSupplier.class, CommonFractalParameters.numberType).withHints("ui-element:slider min=1 max=1000"));
        defs.add(new ParamDefinition(PARAM_MODULO_PERIOD, PARAMNAME_MODULO_PERIOD, cat_mapping, StaticParamSupplier.class, CommonFractalParameters.numberType).withHints("ui-element:slider min=0.001 max=3"));

//        defs.add(new ParamDefinition("width", "Calculator", StaticParamSupplier.class, CommonFractalParameters.integerType));
//        defs.add(new ParamDefinition("height", "Calculator", StaticParamSupplier.class, CommonFractalParameters.integerType));
        defs.add(new ParamDefinition(PARAM_LAYER_CONFIG, PARAMNAME_LAYER_CONFIG, cat_mapping, StaticParamSupplier.class, layerconfigurationType));

        paramConfig.addParameterDefinitions(defs);
        paramConfig.addDefaultValues(defaultValues);

        Selection<String> calculatorSelection = new Selection<String>(PARAM_CALCULATOR);
        calculatorSelection.addOption(TEXT_CALCULATOR_ESCAPETIME, TEXT_CALCULATOR_ESCAPETIME, "");
        calculatorSelection.addOption(TEXT_CALCULATOR_NEWTONFRACTAL, TEXT_CALCULATOR_NEWTONFRACTAL, "");
//        calculatorSelection.addOption(TEXT_CALCULATOR_GRAPH_REAL, TEXT_CALCULATOR_GRAPH_REAL, "");
//        calculatorSelection.addOption(TEXT_CALCULATOR_GRAPH_COMPLEX, TEXT_CALCULATOR_GRAPH_COMPLEX, "");
        paramConfig.addSelection(calculatorSelection);

        Selection<String> precisionSelection = new Selection<String>(PARAM_PRECISION);
        precisionSelection.addOption(TEXT_PRECISION_AUTO, TEXT_PRECISION_AUTO, "");
        precisionSelection.addOption(TEXT_PRECISION_32, TEXT_PRECISION_32, "");
//        precisionSelection.addOption(TEXT_PRECISION_64_EMULATED, TEXT_PRECISION_64_EMULATED, "");
        precisionSelection.addOption(TEXT_PRECISION_64, TEXT_PRECISION_64, "");
        paramConfig.addSelection(precisionSelection);

        Selection<String> conditionSelection = new Selection<String>(PARAM_CONDITION);
        conditionSelection.addOption(TEXT_COND_ABS, TEXT_COND_ABS, "");
        conditionSelection.addOption(TEXT_COND_ABS_R, TEXT_COND_ABS_R, "");
        conditionSelection.addOption(TEXT_COND_ABS_I, TEXT_COND_ABS_I, "");
        conditionSelection.addOption(TEXT_COND_ABS_MULT_RI, TEXT_COND_ABS_MULT_RI, "");
        conditionSelection.addOption(TEXT_COND_MULT_RI, TEXT_COND_MULT_RI, "");
        paramConfig.addSelection(conditionSelection);

        Selection<Integer> unstableOutputSelection = new Selection<Integer>(PARAM_UNSTABLE_OUTPUT);
        unstableOutputSelection.addOption(TEXT_UNSTABLE_OUTPUT_ITERATIONS, 0, "");
        paramConfig.addSelection(unstableOutputSelection);

        Selection<Integer> stableOutputSelection = new Selection<Integer>(PARAM_STABLE_OUTPUT);
        stableOutputSelection.addOption(TEXT_STABLE_OUTPUT_NONE, -1, "");
        stableOutputSelection.addOption(TEXT_STABLE_OUTPUT_MOVED, 0, "");
        stableOutputSelection.addOption(TEXT_STABLE_OUTPUT_ANGLE, 1, "");
        paramConfig.addSelection(stableOutputSelection);

        paramContainer = new ParamContainer(paramConfig);
        paramContainer.addParam(new StaticParamSupplier(CommonFractalParameters.PARAM_NUMBERFACTORY, nf));
        paramContainer.addParam(new StaticParamSupplier(CommonFractalParameters.PARAM_ITERATIONS, 1000));
        paramContainer.addParam(new StaticParamSupplier(CommonFractalParameters.PARAM_MIDPOINT, nf.createComplexNumber(0,0)));
        paramContainer.addParam(new CoordinateBasicShiftParamSupplier(CommonFractalParameters.PARAM_C));
        paramContainer.addParam(new StaticParamSupplier(PARAM_ZOOM, nf.createNumber(3)));
        if (renderer.juliaset)
            paramContainer.addParam(new CoordinateBasicShiftParamSupplier(CommonFractalParameters.PARAM_ZSTART));
        else
            paramContainer.addParam(new StaticParamSupplier(CommonFractalParameters.PARAM_ZSTART, nf.createComplexNumber(0,0)));
        ExpressionsParam expressions = new ExpressionsParam("z^2+c", "z");
        expressions.putExpression(PARAMNAME_C, ""+PARAMNAME_C);
        paramContainer.addParam(new StaticParamSupplier(CommonFractalParameters.PARAM_EXPRESSIONS, expressions));
        paramContainer.addParam(new StaticParamSupplier(PARAM_PRECISION, TEXT_PRECISION_AUTO));
        paramContainer.addParam(new StaticParamSupplier(PARAM_CONDITION, TEXT_COND_ABS));
        paramContainer.addParam(new StaticParamSupplier(PARAM_LIMIT, nf.createNumber(32.0)));
        paramContainer.addParam(new StaticParamSupplier(PARAM_CALCULATOR, TEXT_CALCULATOR_ESCAPETIME));
        paramContainer.addParam(new StaticParamSupplier(PARAM_STABLE_OUTPUT, 0));
        paramContainer.addParam(new StaticParamSupplier(PARAM_UNSTABLE_OUTPUT, 0));
        paramContainer.addParam(new StaticParamSupplier(PARAM_FIRSTITERATIONS, nf.createNumber("100.0")));
        paramContainer.addParam(new StaticParamSupplier(PARAM_SUPERSAMPLING, samples));
        paramContainer.addParam(new StaticParamSupplier(PARAM_SAMPLESPERFRAME, 1));
        paramContainer.addParam(new StaticParamSupplier(PARAM_RESOLUTIONSCALE, 1.0));
        paramContainer.addParam(new StaticParamSupplier(PARAM_MAXBORDERSAMPLES, 1));
        paramContainer.addParam(new StaticParamSupplier(PARAM_GRID_PERIOD, nf.createNumber(100)));
        paramContainer.addParam(new StaticParamSupplier(PARAM_MODULO_PERIOD, nf.createNumber(2)));

//        paramContainer.addParam(new StaticParamSupplier("calculator", "CustomCalculator")); //TODO add only when changed to RemoteRenderer
        List<Layer> layers = new ArrayList<>();
//        layers.add(new BreadthFirstUpsampleLayer(16, CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(1).with_rendering(true).with_priority_shift(0));
//        layers.add(new BreadthFirstUpsampleLayer(8, CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(1).with_rendering(true).with_priority_shift(10));
//        layers.add(new BreadthFirstUpsampleLayer(4, CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(1).with_rendering(true).with_priority_shift(20));
//        layers.add(new BreadthFirstUpsampleLayer(2, CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(1).with_rendering(true).with_priority_shift(30));
        layers.add(new BreadthFirstLayer(CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(1).with_rendering(true).with_priority_shift(40));
//        layers.add(new BreadthFirstLayer(CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(4).with_rendering(true).with_priority_shift(50));
//        layers.add(new BreadthFirstLayer(CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(16).with_rendering(true).with_priority_shift(60));
//        layers.add(new BreadthFirstLayer(CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(49).with_rendering(true).with_priority_shift(70));
//        layers.add(new BreadthFirstLayer(CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(100).with_rendering(true).with_priority_shift(80));
//        layers.add(new BreadthFirstLayer(CommonFractalParameters.DEFAULT_CHUNK_SIZE).with_samples(400).with_rendering(true).with_priority_shift(90));
        paramContainer.addParam(new StaticParamSupplier(PARAM_LAYER_CONFIG, new PadovanLayerConfiguration(layers)));
        List<Orbittrap> orbittraps = new ArrayList<>();
        paramContainer.addParam(new StaticParamSupplier(PARAM_ORBITTRAPS, new OrbittrapContainer(orbittraps)));

        updateLayerConfig(paramContainer, PARAM_LAYER_CONFIG, null, paramContainer.getParamMap());
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

    public int getDefaultSamples(){
        if (Gdx.app.getType() == Application.ApplicationType.Android)
            return SAMPLES_DEFAULT_ANDROID;
        return SAMPLES_DEFAULT;

    }

    public void updateSize(int width, int height){
        paramContainer.addParam(new StaticParamSupplier("width", width));
        paramContainer.addParam(new StaticParamSupplier("height", height));
    }

    @Override
    public boolean setParameters(ParamContainer paramContainer) {
        if (this.paramContainer != null){
            boolean changed = paramContainer.updateChangedFlag(this.paramContainer.getParamMap());
            updateLayerConfig(paramContainer, PARAMNAME_LAYER_CONFIG, this.paramContainer.getParamMap(), paramContainer.getParamMap());
            paramContainer.setParamConfiguration(paramConfig);
            boolean sameInstance = this.paramContainer == paramContainer;
            this.paramContainer = paramContainer;
            if (changed)
                renderer.paramsChanged();
//            paramContainer.setParameters(paramContainer.getParameters(), true);
        }
        return false;
    }

    @Deprecated
    protected void updateLayerConfig(ParamContainer paramContainer, String layerConfigParamUID, Map<String, ParamSupplier> oldParams, Map<String, ParamSupplier> newParams) {
        LayerConfiguration oldLayerConfig = null;
        ParamSupplier paramSupplier = oldParams != null ? oldParams.get(layerConfigParamUID) : null;
        oldLayerConfig = paramSupplier != null ? paramSupplier.getGeneral(LayerConfiguration.class) : null;
        ParamSupplier newLayerConfigSupplier = paramContainer.getParam(layerConfigParamUID);
        if (newLayerConfigSupplier == null)
            return;
        LayerConfiguration newLayerConfig = newLayerConfigSupplier.getGeneral(LayerConfiguration.class);
        if (oldLayerConfig == null || newLayerConfigSupplier.isChanged()) {
            layerConfig = newLayerConfig;
            layerConfig.prepare(nf);
        } else {
            newParams.put(layerConfigParamUID, paramSupplier);
        }
    }

    @Override
    public Layer getLayer(int i) {
        return layerConfig.getLayer(i);
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
        return paramContainer.getParam(s).getGeneral();
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
        Number zoom = ((Number)getParamValue(ShaderSystemContext.PARAM_ZOOM)).copy();
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

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }
}
