package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.Gdx;

import de.felixp.fractalsgdx.params.ComputeParamsCommon;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.numbers.impl.DoubleComplexNumber;
import de.felixperko.fractals.system.numbers.impl.DoubleNumber;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;

public class TurtleGraphicsSystemContext extends AbstractSystemContext {

    public static final String UID_PARAMCONFIG = "f1MZfe";

    public static final String UID_ITERATIONS = "AVgYUT";
    public static final String UID_ALPHABET = "c6Odzo";
    public static final String UID_VARIABLES = "d0tEZF";
    public static final String UID_AXIOM = "98_wfT";
    public static final String UID_START_ANGLE = "-zId9C";
//    public static final String UID_ANGLE2 = "h-JkL9";
    public static final String UID_SCALE = "cDmxD_";

    public static final String PARAM_VARIATION_SEED = "zCFj4B";
    public static final String PARAM_VARIATION_ANGLE = "H2PHs3";
    public static final String PARAM_VARIATION_LENGTH = "VsG2-8";

    public TurtleGraphicsSystemContext(){
        super();

        paramConfig = new ParamConfiguration(UID_PARAMCONFIG, 1.0);
        nf = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

        ParamValueType integerType = CommonFractalParameters.integerType;
        ParamValueType numberType = CommonFractalParameters.numberType;
        ParamValueType complexNumberType = CommonFractalParameters.complexnumberType;
        ParamValueType nfType = CommonFractalParameters.numberfactoryType;
        ParamValueType stringType = CommonFractalParameters.stringType;
        ParamValueType selectionType = CommonFractalParameters.selectionType;
        paramConfig.addValueTypes(integerType, numberType, complexNumberType, nfType, stringType, selectionType);

        paramConfig.addParameterDefinition(ComputeParamsCommon.getCalculatorDef(), new StaticParamSupplier(ComputeParamsCommon.PARAM_CALCULATOR, ComputeParamsCommon.UID_CALCULATOR_TURTLEGRAPHICS));
        paramConfig.addSelection(ComputeParamsCommon.getCalculatorSelection());
        paramConfig.addParameterDefinition(new ParamDefinition(UID_ITERATIONS, "iterations", "Calculator", StaticParamSupplier.class, integerType, 1.0)
                .withHints("ui-element:slider min=0 max=10"),
                new StaticParamSupplier(UID_ITERATIONS, 6));
        paramConfig.addParameterDefinition(new ParamDefinition(UID_ALPHABET, "alphabet", "Calculator", StaticParamSupplier.class, stringType, 1.0),
                new StaticParamSupplier(UID_ALPHABET, "AB+-[]"));
        paramConfig.addParameterDefinition(new ParamDefinition(UID_AXIOM, "axiom", "Calculator", StaticParamSupplier.class, stringType, 1.0),
                new StaticParamSupplier(UID_AXIOM, "A"));

        paramConfig.addParameterDefinition(new ParamDefinition(PARAM_VARIATION_ANGLE, "angles variation", "Calculator", StaticParamSupplier.class, numberType, 1.0)
                        .withHints("ui-element[default]:slider min=0.0 max=0.1"),
                new StaticParamSupplier(PARAM_VARIATION_ANGLE, nf.cn(0.0)));
        paramConfig.addParameterDefinition(new ParamDefinition(PARAM_VARIATION_LENGTH, "length variation", "Calculator", StaticParamSupplier.class, numberType, 1.0)
                        .withHints("ui-element[default]:slider min=0.0 max=0.1"),
                new StaticParamSupplier(PARAM_VARIATION_LENGTH, nf.cn(0.0)));
        paramConfig.addParameterDefinition(new ParamDefinition(PARAM_VARIATION_SEED, "variation seed", "Calculator", StaticParamSupplier.class, numberType, 1.0)
                        .withHints("ui-element:slider min=0.0 max=10.0"),
                new StaticParamSupplier(PARAM_VARIATION_SEED, nf.cn(1.0)));


        paramConfig.addParameterDefinition(new ParamDefinition(UID_VARIABLES, "variables", "Rules", StaticParamSupplier.class, stringType, 1.0),
                new StaticParamSupplier(UID_VARIABLES, "AB"));

        paramConfig.addParameterDefinition(new ParamDefinition(UID_START_ANGLE, "start angle", "Angles", StaticParamSupplier.class, numberType, 1.0)
                .withHints("ui-element[default]:slider min=-180 max=180"),
                 new StaticParamSupplier(UID_START_ANGLE, nf.cn(25)));
//        paramConfig.addParameterDefinition(new ParamDefinition(UID_ANGLE2, "angle 2", "Calculator", StaticParamSupplier.class, numberType, 1.0)
//                .withHints("ui-element[default]:slider min=-90 max=90"),
//                new StaticParamSupplier(UID_ANGLE2, nf.cn(25)));
        paramConfig.addParameterDefinition(new ParamDefinition(CommonFractalParameters.PARAM_NUMBERFACTORY, "numberFactory", "Calculator", StaticParamSupplier.class, nfType, 1.0),
                new StaticParamSupplier(CommonFractalParameters.PARAM_NUMBERFACTORY, nf));

        paramConfig.addParameterDefinition(new ParamDefinition(CommonFractalParameters.PARAM_MIDPOINT, "midpoint", "Mapping", StaticParamSupplier.class, complexNumberType, 1.0),
                new StaticParamSupplier(CommonFractalParameters.PARAM_MIDPOINT, nf.ccn(0,0)));
        paramConfig.addParameterDefinition(new ParamDefinition(UID_SCALE, "scale", "Mapping", StaticParamSupplier.class, numberType, 1.0),
                new StaticParamSupplier(UID_SCALE, nf.cn(1.0)));

        paramContainer = new ParamContainer(paramConfig);
        paramContainer.addParam(new StaticParamSupplier(ComputeParamsCommon.PARAM_CALCULATOR, ComputeParamsCommon.UID_CALCULATOR_TURTLEGRAPHICS));
        paramContainer.addParam(new StaticParamSupplier(CommonFractalParameters.PARAM_NUMBERFACTORY, nf));
        paramContainer.addParam(new StaticParamSupplier(UID_ITERATIONS, 6));
        paramContainer.addParam(new StaticParamSupplier(UID_ALPHABET, "ABF+-[]"));
        paramContainer.addParam(new StaticParamSupplier(UID_VARIABLES, "AB"));
//        paramContainer.addParam(new StaticParamSupplier(UID_AXIOM, "A"));
        paramContainer.addParam(new StaticParamSupplier(UID_START_ANGLE, nf.cn(0.0)));

        //plant (reed/weeds)
        paramContainer.addParam(new StaticParamSupplier("rule A", "B+[[A]-A]-B[-BA]+A"));
        paramContainer.addParam(new StaticParamSupplier("rule B", "BB"));
        paramContainer.addParam(new StaticParamSupplier(UID_START_ANGLE, nf.cn(90.0)));

        //binary tree
//        paramContainer.addParam(new StaticParamSupplier("rule A", "B[-A]+A"));
//        paramContainer.addParam(new StaticParamSupplier("rule B", "BB"));

        //sierpinski triangle
//        paramContainer.addParam(new StaticParamSupplier("rule A", "A-B+A+B-A"));
//        paramContainer.addParam(new StaticParamSupplier("rule B", "BB"));
//        paramContainer.addParam(new StaticParamSupplier(UID_AXIOM, "A-B-B"));

        //sierpinski triangle 2
//        paramContainer.addParam(new StaticParamSupplier("rule A", "B-A-B"));
//        paramContainer.addParam(new StaticParamSupplier("rule B", "A+B+A"));

        //hilbert curve
//        paramContainer.addParam(new StaticParamSupplier("rule A", "+BF-AFA-FB+"));
//        paramContainer.addParam(new StaticParamSupplier("rule B", "-AF+BFB+FA-"));
//        //hilbert curve actions
//        paramContainer.addParam(new StaticParamSupplier("action A", ""));
//        paramContainer.addParam(new StaticParamSupplier("action B", ""));
//        paramContainer.addParam(new StaticParamSupplier("action F", "draw"));

        //Koch snowflake
//        paramContainer.addParam(new StaticParamSupplier(UID_AXIOM, "A+A+A+"));
//        paramContainer.addParam(new StaticParamSupplier("rule A", "A-A+A-A"));
//        paramContainer.addParam(new StaticParamSupplier("rule B", ""));

//        paramContainer.addParam(new StaticParamSupplier("action A", "draw"));
//        paramContainer.addParam(new StaticParamSupplier("action B", "draw"));

        paramContainer.addParam(new StaticParamSupplier("action +", "turn(a1)"));
        paramContainer.addParam(new StaticParamSupplier("action -", "turn(a2)"));
        paramContainer.addParam(new StaticParamSupplier("action [", "push"));
        paramContainer.addParam(new StaticParamSupplier("action ]", "pop"));
//        paramContainer.addParam(new StaticParamSupplier("a1", nf.cn(-120)));
//        paramContainer.addParam(new StaticParamSupplier("a2", nf.cn(120)));
        paramContainer.addParam(new StaticParamSupplier("a1", nf.cn(10)));
        paramContainer.addParam(new StaticParamSupplier("a2", nf.cn(-30)));
        paramContainer.addParam(new StaticParamSupplier(CommonFractalParameters.PARAM_MIDPOINT, nf.ccn(0, 0)));
    }
}
