package de.felixp.fractalsgdx.rendering.rendererparams;

import com.badlogic.gdx.graphics.Color;

import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.numbers.impl.DoubleComplexNumber;
import de.felixperko.fractals.system.numbers.impl.DoubleNumber;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;

public class DrawParamsTurtleGraphics extends AbstractParamTemplate {

    public final static String PARAM_START_COLOR = "LiAajT";
    public final static String PARAM_START_ALPHA = "IM9j5J";
    public final static String PARAM_END_COLOR = "n2tDdt";
    public final static String PARAM_END_ALPHA = "NEIyDu";

    public final static NumberFactory nf = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

    @Override
    public ParamConfiguration getParamConfig(){
        ParamConfiguration config = new ParamConfiguration("-DRHaD", 1.0);

        config.addParamDefStatic(CommonFractalParameters.PARAM_NUMBERFACTORY, "nf", "Calculator", CommonFractalParameters.numberfactoryType, 1.0, nf);

        String cat_coloring = "Coloring";
        config.addParamDefStatic(PARAM_START_COLOR, "start color", cat_coloring, ClientParamsEscapeTime.TYPE_COLOR, 1.0, Color.ORANGE);
        config.addParamDefStatic(PARAM_START_ALPHA, "start alpha", cat_coloring, CommonFractalParameters.numberType, 1.0, nf.cn(1.0));
        config.addParamDefStatic(PARAM_END_COLOR, "end color", cat_coloring, ClientParamsEscapeTime.TYPE_COLOR, 1.0, Color.GREEN);
        config.addParamDefStatic(PARAM_END_ALPHA, "end alpha", cat_coloring, CommonFractalParameters.numberType, 1.0, nf.cn(0.2));

        //no renderer reset on param change
        for (ParamDefinition paramDef : config.getParameters())
            paramDef.setResetRendererOnChange(false);
        return config;
    }
}
