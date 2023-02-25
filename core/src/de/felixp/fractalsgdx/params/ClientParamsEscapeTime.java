package de.felixp.fractalsgdx.params;

import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.rendering.palette.IPalette;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.numbers.impl.DoubleComplexNumber;
import de.felixperko.fractals.system.numbers.impl.DoubleNumber;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;
import de.felixperko.fractals.system.systems.infra.Selection;

public class ClientParamsEscapeTime {
    private final static String PARAMNAME_NUMBERFACTORY = "numberFactory";

    private final static String PARAMNAME_COLOR_ADD = "color offset";
    private final static String PARAMNAME_COLOR_MULT = "color period";
    private final static String PARAMNAME_COLOR_SATURATION = "saturation";
    private final static String PARAMNAME_SOBEL_GLOW_LIMIT = "edge brightness";
    private final static String PARAMNAME_SOBEL_GLOW_FACTOR = "dim period";
    private final static String PARAMNAME_AMBIENT_LIGHT = "ambient light";

    private final static String PARAMNAME_FALLBACK_COLOR_ADD = "color offset 2";
    private final static String PARAMNAME_FALLBACK_COLOR_MULT = "color period 2";
    private final static String PARAMNAME_FALLBACK_COLOR_SATURATION = "saturation 2";
    private final static String PARAMNAME_FALLBACK_SOBEL_GLOW_LIMIT = "edge brightness 2";
    private final static String PARAMNAME_FALLBACK_SOBEL_GLOW_FACTOR = "glow sensitivity 2";
    private final static String PARAMNAME_FALLBACK_AMBIENT_LIGHT = "ambient light 2";

    private final static String PARAMNAME_SOBEL_RADIUS = "glow filter radius";
    private final static String PARAMNAME_PALETTE = "palette (condition)";
    private final static String PARAMNAME_PALETTE2 = "palette (fallback)";
    private final static String PARAMNAME_EXTRACT_CHANNEL = "monochrome source";
    private final static String PARAMNAME_MAPPING_COLOR = "monochrome color";

    private final static String PARAMNAME_DRAW_AXIS = "draw axis";
    private final static String PARAMNAME_DRAW_ORBIT = "draw orbit";
    private final static String PARAMNAME_DRAW_PATH = "draw current path";
    private final static String PARAMNAME_DRAW_MIDPOINT = "draw midpoint";
    private final static String PARAMNAME_DRAW_ZERO = "draw (0+0*i)";

    private final static String PARAMNAME_ORBIT_TRACES = "orbit traces";
    private final static String PARAMNAME_ORBIT_TRACE_PER_INSTRUCTION = "trace instructions";
    private final static String PARAMNAME_ORBIT_TARGET = "orbit target";

    private final static String PARAMNAME_TRACES_VALUE = "trace position";
    private final static String PARAMNAME_TRACES_LINE_WIDTH = "line width";
    private final static String PARAMNAME_TRACES_POINT_SIZE = "point size";
    private final static String PARAMNAME_TRACES_LINE_TRANSPARENCY = "line transparency";
    private final static String PARAMNAME_TRACES_POINT_TRANSPARENCY = "point transparency";
    private final static String PARAMNAME_TRACES_START_COLOR = "";
    private final static String PARAMNAME_TRACES_END_COLOR = "";

    //    public final static String PARAMS_NUMBERFACTORY = "0oizcO";
    public final static String PARAMS_NUMBERFACTORY = CommonFractalParameters.PARAM_NUMBERFACTORY;

    public final static String PARAMS_COLOR_ADD = "UgHgR5";
    public final static String PARAMS_COLOR_MULT = "OFTMFw";
    public final static String PARAMS_COLOR_SATURATION = "EgDJY4";
    //    public final static String PARAMS_SOBEL_FACTOR = "glow sensitivity";
    public final static String PARAMS_SOBEL_GLOW_LIMIT = "bW0VpF";
    public final static String PARAMS_SOBEL_GLOW_FACTOR = "iOOW84";
    public final static String PARAMS_AMBIENT_LIGHT = "0IH3rK";

    public final static String PARAMS_FALLBACK_COLOR_ADD = "12f1jD";
    public final static String PARAMS_FALLBACK_COLOR_MULT = "56al2K";
    public final static String PARAMS_FALLBACK_COLOR_SATURATION = "pDD1eC";
    //    public final static String PARAMS_FALLBACK_SOBEL_FACTOR = "glow sensitivity";
    public final static String PARAMS_FALLBACK_SOBEL_GLOW_LIMIT = "VujYT7";
    public final static String PARAMS_FALLBACK_SOBEL_GLOW_FACTOR = "2NgNhI";
    public final static String PARAMS_FALLBACK_AMBIENT_LIGHT = "PWfG3I";

    public final static String PARAMS_SOBEL_RADIUS = "xNxrIO";
    public final static String PARAMS_PALETTE = "pLrKY-";
    public final static String PARAMS_PALETTE2 = "3YtNML";
    public final static String PARAMS_EXTRACT_CHANNEL = "xbMt0u";
    public final static String PARAMS_MAPPING_COLOR = "gS2lYj";

    public final static String PARAMS_DRAW_AXIS = "CGrDXZ";
    public final static String PARAMS_DRAW_ORBIT = "m9TYl2";
    public final static String PARAMS_DRAW_PATH = "ENQY4_";
    public final static String PARAMS_DRAW_MIDPOINT = "LHvVgb";
    public final static String PARAMS_DRAW_ZERO = "Vwr62J";

    public final static String PARAMS_ORBIT_TRACES = "aW_XS3";
    public final static String PARAMS_ORBIT_TRACE_PER_INSTRUCTION = "cqTeeI";
    public final static String PARAMS_ORBIT_TARGET = "mzwBNO";

    public final static String PARAMS_TRACES_VALUE = "mCZoSI";
    public final static String PARAMS_TRACES_LINE_WIDTH = "Rr79wb";
    public final static String PARAMS_TRACES_POINT_SIZE = "ZEMbVL";
    public final static String PARAMS_TRACES_LINE_TRANSPARENCY = "jcENk2";
    public final static String PARAMS_TRACES_POINT_TRANSPARENCY = "2fXEPs";
    public final static String PARAMS_TRACES_START_COLOR = "";
    public final static String PARAMS_TRACES_END_COLOR = "";

    public final static String TYPEID_COLOR = "BlsZQD";
    public static ParamValueType TYPE_COLOR = new ParamValueType(TYPEID_COLOR, "color");

    private static final String OPTIONNAME_PALETTE_DISABLED = "hue";
    private final static String OPTIONNAME_EXTRACT_CHANNEL_DISABLED = "disabled";
    private final static String OPTIONNAME_EXTRACT_CHANNEL_R = "r";
    private final static String OPTIONNAME_EXTRACT_CHANNEL_G = "g";
    private final static String OPTIONNAME_EXTRACT_CHANNEL_B = "b";
    private final static String OPTIONNAME_ORBIT_TARGET_MOUSE = "mouse";
    private final static String OPTIONNAME_ORBIT_TARGET_PATH = "path";

    public static final String OPTIONVALUE_PALETTE_DISABLED = "DISABLED";
    public final static String OPTIONVALUE_EXTRACT_CHANNEL_DISABLED = "DISABLED";
    public final static String OPTIONVALUE_EXTRACT_CHANNEL_R = "R";
    public final static String OPTIONVALUE_EXTRACT_CHANNEL_G = "G";
    public final static String OPTIONVALUE_EXTRACT_CHANNEL_B = "B";
    public final static String OPTIONVALUE_ORBIT_TARGET_MOUSE = "MOUSE";
    public final static String OPTIONVALUE_ORBIT_TARGET_PATH = "PATH";

    public final static NumberFactory nf = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

    public static ParamContainer getParamContainer(Map<String, IPalette> palettes){
        ParamConfiguration paramConfig = getParamConfig(palettes);
        ParamContainer container = new ParamContainer(paramConfig);
        for (ParamSupplier supp : getDefaultParams())
            container.addParam(supp);
        return container;
    }

    public static List<ParamSupplier> getDefaultParams(){
        List<ParamSupplier> params = new ArrayList<>();

        params.add(new StaticParamSupplier(PARAMS_NUMBERFACTORY, nf));
        params.add(new StaticParamSupplier(PARAMS_COLOR_MULT, nf.createNumber(2.5)));
        params.add(new StaticParamSupplier(PARAMS_COLOR_ADD, nf.createNumber(0.0)));
        params.add(new StaticParamSupplier(PARAMS_COLOR_SATURATION, nf.createNumber(0.5)));
        params.add(new StaticParamSupplier(PARAMS_AMBIENT_LIGHT, nf.createNumber(0.2)));
        params.add(new StaticParamSupplier(PARAMS_SOBEL_GLOW_LIMIT, nf.createNumber(0.8)));
        params.add(new StaticParamSupplier(PARAMS_SOBEL_GLOW_FACTOR, nf.createNumber(4.0)));

        params.add(new StaticParamSupplier(PARAMS_FALLBACK_COLOR_MULT, nf.createNumber(1.0)));
        params.add(new StaticParamSupplier(PARAMS_FALLBACK_COLOR_ADD, nf.createNumber(0.0)));
        params.add(new StaticParamSupplier(PARAMS_FALLBACK_COLOR_SATURATION, nf.createNumber(0.5)));
        params.add(new StaticParamSupplier(PARAMS_FALLBACK_AMBIENT_LIGHT, nf.createNumber(0.0)));
        params.add(new StaticParamSupplier(PARAMS_FALLBACK_SOBEL_GLOW_LIMIT, nf.createNumber(1.0)));
        params.add(new StaticParamSupplier(PARAMS_FALLBACK_SOBEL_GLOW_FACTOR, nf.createNumber(1.0)));

        params.add(new StaticParamSupplier(PARAMS_SOBEL_RADIUS, 2));
        params.add(new StaticParamSupplier(PARAMS_PALETTE, OPTIONVALUE_PALETTE_DISABLED));
        params.add(new StaticParamSupplier(PARAMS_PALETTE2, OPTIONVALUE_PALETTE_DISABLED));
        params.add(new StaticParamSupplier(PARAMS_EXTRACT_CHANNEL, OPTIONVALUE_EXTRACT_CHANNEL_DISABLED));
        params.add(new StaticParamSupplier(PARAMS_MAPPING_COLOR, Color.WHITE));

        params.add(new StaticParamSupplier(PARAMS_DRAW_PATH, true));
        params.add(new StaticParamSupplier(PARAMS_DRAW_AXIS, false));
        params.add(new StaticParamSupplier(PARAMS_DRAW_MIDPOINT, false));
        params.add(new StaticParamSupplier(PARAMS_DRAW_ZERO, false));

        params.add(new StaticParamSupplier(PARAMS_DRAW_ORBIT, false));
        params.add(new StaticParamSupplier(PARAMS_ORBIT_TRACES, 1000));
        params.add(new StaticParamSupplier(PARAMS_ORBIT_TRACE_PER_INSTRUCTION, true));
        params.add(new StaticParamSupplier(PARAMS_ORBIT_TARGET, OPTIONVALUE_ORBIT_TARGET_MOUSE));
        params.add(new StaticParamSupplier(PARAMS_TRACES_VALUE, nf.createComplexNumber(0,0)));

        params.add(new StaticParamSupplier(PARAMS_TRACES_LINE_WIDTH, nf.createNumber(1.0)));
        params.add(new StaticParamSupplier(PARAMS_TRACES_POINT_SIZE, nf.createNumber(3.0)));
        params.add(new StaticParamSupplier(PARAMS_TRACES_LINE_TRANSPARENCY, nf.createNumber(0.33)));
        params.add(new StaticParamSupplier(PARAMS_TRACES_POINT_TRANSPARENCY, nf.createNumber(0.75)));
        return params;
    }

    public static ParamConfiguration getParamConfig(Map<String, IPalette> palettes){
        ParamConfiguration config = new ParamConfiguration("AxW_QG", 1.0);

        ParamValueType integerType = CommonFractalParameters.integerType;
        config.addValueType(integerType);
        ParamValueType doubleType = CommonFractalParameters.doubleType;
        config.addValueType(doubleType);
        ParamValueType booleanType = CommonFractalParameters.booleanType;
        config.addValueType(booleanType);
        ParamValueType selectionType = CommonFractalParameters.selectionType;
        config.addValueType(selectionType);
        ParamValueType stringType = CommonFractalParameters.stringType;
        config.addValueType(stringType);
        ParamValueType complexNumberType = CommonFractalParameters.complexnumberType;
        config.addValueType(complexNumberType);
        ParamValueType numberType = CommonFractalParameters.numberType;
        config.addValueType(numberType);
        ParamValueType numberFactoryType = CommonFractalParameters.numberfactoryType;
        config.addValueType(numberFactoryType);
        config.addValueType(TYPE_COLOR);

        Map<String, ParamSupplier> paramMap = new HashMap<>();
        List<ParamSupplier> params = getDefaultParams();
        params.stream().map(e -> paramMap.put(e.getUID(), e));

        config.addParameterDefinition(new ParamDefinition(PARAMS_NUMBERFACTORY, PARAMNAME_NUMBERFACTORY, "Calculator", StaticParamSupplier.class, numberFactoryType, 1.0),
                paramMap.get(PARAMS_NUMBERFACTORY));

        String cat_coloring_reached = "Coloring (condition reached)";
        config.addParameterDefinition(new ParamDefinition(PARAMS_COLOR_MULT, PARAMNAME_COLOR_MULT, cat_coloring_reached, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=0.02 max=10"), paramMap.get(PARAMS_COLOR_MULT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_COLOR_ADD, PARAMNAME_COLOR_ADD, cat_coloring_reached, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=0 max=1"), paramMap.get(PARAMS_COLOR_ADD));
        config.addParameterDefinition(new ParamDefinition(PARAMS_COLOR_SATURATION, PARAMNAME_COLOR_SATURATION, cat_coloring_reached, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=0 max=1"), paramMap.get(PARAMS_COLOR_SATURATION));
//        config.addParameterDefinition(new ParameterDefinition(PARAMS_SOBEL_FACTOR, "coloring (reached)", StaticParamSupplier.class, doubleType));
        config.addParameterDefinition(new ParamDefinition(PARAMS_AMBIENT_LIGHT, PARAMNAME_AMBIENT_LIGHT, cat_coloring_reached, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=-1 max=1"), paramMap.get(PARAMS_AMBIENT_LIGHT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_SOBEL_GLOW_LIMIT, PARAMNAME_SOBEL_GLOW_LIMIT, cat_coloring_reached, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=-1 max=5"), paramMap.get(PARAMS_SOBEL_GLOW_LIMIT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_SOBEL_GLOW_FACTOR, PARAMNAME_SOBEL_GLOW_FACTOR, cat_coloring_reached, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=0 max=10"), paramMap.get(PARAMS_SOBEL_GLOW_FACTOR));

        String cat_coloring_fallback = "Coloring (fallback)";
        config.addParameterDefinition(new ParamDefinition(PARAMS_FALLBACK_COLOR_MULT, PARAMNAME_FALLBACK_COLOR_MULT, cat_coloring_fallback, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=0.01 max=2"), paramMap.get(PARAMS_COLOR_MULT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_FALLBACK_COLOR_ADD, PARAMNAME_FALLBACK_COLOR_ADD, cat_coloring_fallback, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=0 max=1"), paramMap.get(PARAMS_COLOR_ADD));
        config.addParameterDefinition(new ParamDefinition(PARAMS_FALLBACK_COLOR_SATURATION, PARAMNAME_FALLBACK_COLOR_SATURATION, cat_coloring_fallback, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=0 max=1"), paramMap.get(PARAMS_COLOR_SATURATION));
//        config.addParameterDefinition(new ParameterDefinition(PARAMS_SOBEL_FACTOR, "coloring", StaticParamSupplier.class, doubleType));
        config.addParameterDefinition(new ParamDefinition(PARAMS_FALLBACK_AMBIENT_LIGHT, PARAMNAME_FALLBACK_AMBIENT_LIGHT, cat_coloring_fallback, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=-1 max=1"), paramMap.get(PARAMS_AMBIENT_LIGHT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_FALLBACK_SOBEL_GLOW_LIMIT, PARAMNAME_FALLBACK_SOBEL_GLOW_LIMIT, cat_coloring_fallback, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=-10 max=10"), paramMap.get(PARAMS_SOBEL_GLOW_LIMIT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_FALLBACK_SOBEL_GLOW_FACTOR, PARAMNAME_FALLBACK_SOBEL_GLOW_FACTOR, cat_coloring_fallback, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=0 max=2"), paramMap.get(PARAMS_SOBEL_GLOW_FACTOR));

        String cat_coloring_palettes = "Palettes";
        config.addParameterDefinition(new ParamDefinition(PARAMS_SOBEL_RADIUS, PARAMNAME_SOBEL_RADIUS, cat_coloring_palettes, StaticParamSupplier.class, integerType, 1.0),
                paramMap.get(PARAMS_SOBEL_RADIUS));
        config.addParameterDefinition(new ParamDefinition(PARAMS_PALETTE, PARAMNAME_PALETTE, cat_coloring_palettes, StaticParamSupplier.class, selectionType, 1.0),
                paramMap.get(PARAMS_PALETTE));
        config.addParameterDefinition(new ParamDefinition(PARAMS_PALETTE2, PARAMNAME_PALETTE2, cat_coloring_palettes, StaticParamSupplier.class, selectionType, 1.0),
                paramMap.get(PARAMS_PALETTE2));

        Selection<String> paletteSelection = new Selection<>(PARAMS_PALETTE);
        Selection<String> paletteSelection2 = new Selection<>(PARAMS_PALETTE2);
        paletteSelection.addOption(OPTIONNAME_PALETTE_DISABLED, OPTIONVALUE_PALETTE_DISABLED, "No predefined palette");
        paletteSelection2.addOption(OPTIONNAME_PALETTE_DISABLED, OPTIONVALUE_PALETTE_DISABLED, "No predefined palette");
        for (String paletteName : palettes.keySet()) {
            paletteSelection.addOption(paletteName, paletteName, "Palette '" + paletteName + "'");
            paletteSelection2.addOption(paletteName, paletteName, "Palette '" + paletteName + "'");
        }
        config.addSelection(paletteSelection);
        config.addSelection(paletteSelection2);

        config.addParameterDefinition(new ParamDefinition(PARAMS_EXTRACT_CHANNEL, PARAMNAME_EXTRACT_CHANNEL, cat_coloring_palettes, StaticParamSupplier.class, selectionType, 1.0),
                paramMap.get(PARAMS_EXTRACT_CHANNEL));
        Selection<String> extractChannelSelection = new Selection<String>(PARAMS_EXTRACT_CHANNEL);
        extractChannelSelection.addOption(OPTIONNAME_EXTRACT_CHANNEL_DISABLED, OPTIONVALUE_EXTRACT_CHANNEL_DISABLED, "No channel remapping");
        extractChannelSelection.addOption(OPTIONNAME_EXTRACT_CHANNEL_R, OPTIONVALUE_EXTRACT_CHANNEL_R, "Remap red channel to tint color");
        extractChannelSelection.addOption(OPTIONNAME_EXTRACT_CHANNEL_G, OPTIONVALUE_EXTRACT_CHANNEL_G, "Remap green channel to tint color");
        extractChannelSelection.addOption(OPTIONNAME_EXTRACT_CHANNEL_B, OPTIONVALUE_EXTRACT_CHANNEL_B, "Remap blue channel to tint color");
        config.addSelection(extractChannelSelection);
        config.addParameterDefinition(new ParamDefinition(PARAMS_MAPPING_COLOR, PARAMNAME_MAPPING_COLOR, cat_coloring_palettes, StaticParamSupplier.class, TYPE_COLOR, 1.0),
                paramMap.get(PARAMS_MAPPING_COLOR));

        String cat_shape_drawing = "Shape drawing";
        config.addParameterDefinition(new ParamDefinition(PARAMS_DRAW_AXIS, PARAMNAME_DRAW_AXIS, cat_shape_drawing, StaticParamSupplier.class, booleanType, 1.0),
                paramMap.get(PARAMS_DRAW_AXIS));

        config.addParameterDefinition(new ParamDefinition(PARAMS_DRAW_ORBIT, PARAMNAME_DRAW_ORBIT, cat_shape_drawing, StaticParamSupplier.class, booleanType, 1.0)
                , paramMap.get(PARAMS_DRAW_ORBIT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_DRAW_PATH, PARAMNAME_DRAW_PATH, cat_shape_drawing, StaticParamSupplier.class, booleanType, 1.0),
                paramMap.get(PARAMS_DRAW_PATH));
        config.addParameterDefinition(new ParamDefinition(PARAMS_DRAW_MIDPOINT, PARAMNAME_DRAW_MIDPOINT, cat_shape_drawing, StaticParamSupplier.class, booleanType, 1.0),
                paramMap.get(PARAMS_DRAW_MIDPOINT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_DRAW_ZERO, PARAMNAME_DRAW_ZERO, cat_shape_drawing, StaticParamSupplier.class, booleanType, 1.0),
                paramMap.get(PARAMS_DRAW_ZERO));
        config.addParameterDefinition(new ParamDefinition(PARAMS_ORBIT_TARGET, PARAMNAME_ORBIT_TARGET, cat_shape_drawing, StaticParamSupplier.class, selectionType, 1.0)
                , paramMap.get(PARAMS_ORBIT_TARGET));
        Selection<String> traceTargetSelection = new Selection<String>(PARAMS_ORBIT_TARGET);
        traceTargetSelection.addOption(OPTIONNAME_ORBIT_TARGET_MOUSE, OPTIONVALUE_ORBIT_TARGET_MOUSE, "The trace target is set to the current mouse position");
        traceTargetSelection.addOption(OPTIONNAME_ORBIT_TARGET_PATH, OPTIONVALUE_ORBIT_TARGET_PATH, "The trace target is set to the animation named 'path'");
        config.addSelection(traceTargetSelection);
        config.addParameterDefinition(new ParamDefinition(PARAMS_ORBIT_TRACES, PARAMNAME_ORBIT_TRACES, cat_shape_drawing, StaticParamSupplier.class, integerType, 1.0)
                , paramMap.get(PARAMS_ORBIT_TRACES));
        config.addParameterDefinition(new ParamDefinition(PARAMS_ORBIT_TRACE_PER_INSTRUCTION, PARAMNAME_ORBIT_TRACE_PER_INSTRUCTION, cat_shape_drawing, StaticParamSupplier.class, booleanType, 1.0)
                , paramMap.get(PARAMS_ORBIT_TRACE_PER_INSTRUCTION));
        config.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_VALUE, PARAMNAME_TRACES_VALUE, cat_shape_drawing, StaticParamSupplier.class, complexNumberType, 1.0)
                .withVisible(false), paramMap.get(PARAMS_TRACES_VALUE));

        String cat_shape_settings = "Shape settings";
        config.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_LINE_WIDTH, PARAMNAME_TRACES_LINE_WIDTH, cat_shape_settings, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=0 max=3"), paramMap.get(PARAMS_TRACES_LINE_WIDTH));
        config.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_POINT_SIZE, PARAMNAME_TRACES_POINT_SIZE, cat_shape_settings, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=0 max=10"), paramMap.get(PARAMS_TRACES_POINT_SIZE));
        config.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_LINE_TRANSPARENCY, PARAMNAME_TRACES_LINE_TRANSPARENCY, cat_shape_settings, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=0 max=1"), paramMap.get(PARAMS_TRACES_LINE_TRANSPARENCY));
        config.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_POINT_TRANSPARENCY, PARAMNAME_TRACES_POINT_TRANSPARENCY, cat_shape_settings, StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=0 max=1"), paramMap.get(PARAMS_TRACES_POINT_TRANSPARENCY));

        //no renderer reset on param change
        for (ParamDefinition paramDef : config.getParameters())
            paramDef.setResetRendererOnChange(false);
        return config;
    }
}
