package de.felixp.fractalsgdx.rendering;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.params.ComputeParamsCommon;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.numbers.impl.DoubleComplexNumber;
import de.felixperko.fractals.system.numbers.impl.DoubleNumber;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;

public class ReactionDiffusionSystemContext extends AbstractSystemContext{

    public static final String UID_PARAMCONFIG = "p1njJf";

    public static final String PARAM_SIMULATIONSPEED = "MD3p6n";
    public static final String PARAM_DIFFUSIONRATE_1 = "6-rjiE";
    public static final String PARAM_DIFFUSIONRATE_2 = "ltldWb";
    public static final String PARAM_REACTIONRATE = "YcIm1t";
    public static final String PARAM_FEEDRATE = "RNfVSS";
    public static final String PARAM_KILLRATE = "cDNeTB";

    public ReactionDiffusionSystemContext(){
        super();

        paramConfig = new ParamConfiguration(UID_PARAMCONFIG, 1.0);
        nf = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

        ParamValueType integerType = CommonFractalParameters.integerType;
        ParamValueType numberType = CommonFractalParameters.numberType;
        ParamValueType nfType = CommonFractalParameters.numberfactoryType;

        paramConfig.addParameterDefinition(ComputeParamsCommon.getCalculatorDef(), new StaticParamSupplier(ComputeParamsCommon.PARAM_CALCULATOR, ComputeParamsCommon.UID_CALCULATOR_REACTIONDIFFUSION));
        paramConfig.addSelection(ComputeParamsCommon.getCalculatorSelection());
        List<Class<? extends ParamSupplier>> classes = new ArrayList<>();
        classes.add(StaticParamSupplier.class);
        paramConfig.addParamDefStatic(CommonFractalParameters.PARAM_NUMBERFACTORY, "nf", "Technical", nfType, 1.0, nf);
        paramConfig.addParamDefStatic(PARAM_SIMULATIONSPEED, "simulation speed", "Calculator", integerType, 1.0, 20);
        paramConfig.addParamDefStatic(PARAM_DIFFUSIONRATE_1, "diffusion rate 1", "Calculator", numberType, 1.0, nf.cn(1.0)).withHints("ui-element[default]:slider min=0.0 max=1.0");
        paramConfig.addParamDefStatic(PARAM_DIFFUSIONRATE_2, "diffusion rate 2", "Calculator", numberType, 1.0, nf.cn(0.3)).withHints("ui-element[default]:slider min=0.0 max=1.0");
        paramConfig.addParamDefStatic(PARAM_REACTIONRATE, "reaction rate", "Calculator", numberType, 1.0, nf.cn(1.0)).withHints("ui-element[default]:slider min=0.0 max=2.0");
        paramConfig.addParamDefStatic(PARAM_FEEDRATE, "feed rate", "Calculator", numberType, 1.0, nf.cn(0.1)).withHints("ui-element[default]:slider min=0.0 max=0.15");
        paramConfig.addParamDefStatic(PARAM_KILLRATE, "kill rate", "Calculator", numberType, 1.0, nf.cn(0.06)).withHints("ui-element[default]:slider min=0.0 max=0.1");

        paramContainer = new ParamContainer(paramConfig);
        paramContainer.addParam(new StaticParamSupplier(ComputeParamsCommon.PARAM_CALCULATOR, ComputeParamsCommon.UID_CALCULATOR_REACTIONDIFFUSION));
        paramContainer.addParam(new StaticParamSupplier(CommonFractalParameters.PARAM_NUMBERFACTORY, nf));
    }

    @Override
    public ComplexNumber getMidpoint() {
        return nf.ccn(0, 0);
    }
}
