package de.felixp.fractalsgdx.rendering.rendererparams;

import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;
import de.felixperko.fractals.system.systems.infra.Selection;

public class ComputeParamsCommon {

    public static final String PARAM_CALCULATOR = "DVm4Oz";

    public static final String PARAMNAME_CALCULATOR = "calculator";

    public static final String UID_CALCULATOR_ESCAPETIME = "y86CCJ";
    public static final String UID_CALCULATOR_NEWTONFRACTAL = "bB8ZOv";
    public static final String UID_CALCULATOR_TURTLEGRAPHICS = "pM9hEv";
    public static final String UID_CALCULATOR_REACTIONDIFFUSION = "YsQO_2";

    private static final String TEXT_CALCULATOR_ESCAPETIME = "iteration fractals";
    private static final String TEXT_CALCULATOR_NEWTONFRACTAL = "newton fractals";
    private static final String TEXT_CALCULATOR_TURTLEGRAPHICS = "lindenmayer systems";
    private static final String TEXT_CALCULATOR_REACTIONDIFFUSION = "reaction-diffusion systems";
    private static final String TEXT_CALCULATOR_GRAPH_REAL = "graph (real)";
    private static final String TEXT_CALCULATOR_GRAPH_COMPLEX = "graph (complex)";

    public static Selection<String> getCalculatorSelection() {
        Selection<String> calculatorSelection = new Selection<String>(PARAM_CALCULATOR);
        calculatorSelection.addOption(TEXT_CALCULATOR_ESCAPETIME, UID_CALCULATOR_ESCAPETIME, "");
        calculatorSelection.addOption(TEXT_CALCULATOR_NEWTONFRACTAL, UID_CALCULATOR_NEWTONFRACTAL, "");
        calculatorSelection.addOption(TEXT_CALCULATOR_TURTLEGRAPHICS, UID_CALCULATOR_TURTLEGRAPHICS, "");
        calculatorSelection.addOption(TEXT_CALCULATOR_REACTIONDIFFUSION, UID_CALCULATOR_REACTIONDIFFUSION, "");
//        calculatorSelection.addOption(TEXT_CALCULATOR_GRAPH_REAL, UID_CALCULATOR_GRAPH_REAL, "");
//        calculatorSelection.addOption(TEXT_CALCULATOR_GRAPH_COMPLEX, UID_CALCULATOR_GRAPH_COMPLEX, "");
        return calculatorSelection;
    }

    public static ParamDefinition getCalculatorDef() {
        return new ParamDefinition(PARAM_CALCULATOR, PARAMNAME_CALCULATOR, "Calculator",
                StaticParamSupplier.class, CommonFractalParameters.selectionType, 1.0);
    }

    public static StaticParamSupplier getCalculatorDefaultValue(){
        return new StaticParamSupplier(PARAM_CALCULATOR, UID_CALCULATOR_ESCAPETIME);
    }
}
