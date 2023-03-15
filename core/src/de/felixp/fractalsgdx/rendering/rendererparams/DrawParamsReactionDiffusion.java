package de.felixp.fractalsgdx.rendering.rendererparams;

import com.badlogic.gdx.graphics.Color;

import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.numbers.impl.DoubleComplexNumber;
import de.felixperko.fractals.system.numbers.impl.DoubleNumber;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;
import de.felixperko.fractals.system.systems.infra.Selection;

public class DrawParamsReactionDiffusion extends AbstractParamTemplate {
    
    public final static String PARAM_COLOR = "sDRstX";
    public final static String PARAM_SOURCEBUFFER = "iRLUrZ";

    public final static String OPTIONVALUE_SOURCEBUFFER_0 = "RnwROG";
    public final static String OPTIONVALUE_SOURCEBUFFER_1 = "Ep2ccB";

    public final static NumberFactory nf = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

    @Override
    public ParamConfiguration getParamConfig(){
        ParamConfiguration config = new ParamConfiguration("o8g6BW", 1.0);

        config.addParamDefStatic(CommonFractalParameters.PARAM_NUMBERFACTORY, "nf", "Calculator", CommonFractalParameters.numberfactoryType, 1.0, nf);

        String cat_coloring = "Coloring";
        config.addParamDefStatic(PARAM_COLOR, "color", cat_coloring, ClientParamsEscapeTime.TYPE_COLOR, 1.0, Color.WHITE);
        config.addParamDefStatic(PARAM_SOURCEBUFFER, "display buffer", cat_coloring, CommonFractalParameters.selectionType, 1.0, OPTIONVALUE_SOURCEBUFFER_1);

        Selection<String> sourcebufferSelection = new Selection<String>(PARAM_SOURCEBUFFER);
        sourcebufferSelection.addOption("Buffer 0", OPTIONVALUE_SOURCEBUFFER_0, "");
        sourcebufferSelection.addOption("Buffer 1", OPTIONVALUE_SOURCEBUFFER_1, "");
        config.addSelection(sourcebufferSelection);

        //no renderer reset on param change
        for (ParamDefinition paramDef : config.getParameters())
            paramDef.setResetRendererOnChange(false);
        return config;
    }
}
