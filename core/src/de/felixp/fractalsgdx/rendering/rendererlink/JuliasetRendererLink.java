package de.felixp.fractalsgdx.rendering.rendererlink;

import java.util.Arrays;
import java.util.HashMap;

import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.rendering.ShaderSystemContext;
import de.felixperko.expressions.ComputeExpressionBuilder;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.calculator.ComputeExpression;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ExpressionsParam;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;

public class JuliasetRendererLink extends DefaultRendererLink{

    public JuliasetRendererLink(FractalRenderer sourceRenderer, FractalRenderer targetRenderer) {
        super(sourceRenderer, targetRenderer, Arrays.asList("limit", ShaderSystemContext.PARAMNAME_ITERATIONS, CommonFractalParameters.PARAM_EXPRESSIONS, "condition", ShaderSystemContext.PARAMNAME_ORBITTRAPS));
    }

    boolean switching = false;

    @Override
    public void switchRenderers() {

        if (switching)
            return;
        switching = true;

        ComplexNumber preservedSourceMidpoint = getSourceParamContainer().getClientParameter("midpoint").getGeneral(ComplexNumber.class);
        ComplexNumber preservedTargetMidpoint = getTargetParamContainer().getClientParameter("midpoint").getGeneral(ComplexNumber.class);

        Number preservedSourceZoom = getSourceParamContainer().getClientParameter("zoom").getGeneral(Number.class);
        Number preservedTargetZoom = getTargetParamContainer().getClientParameter("zoom").getGeneral(Number.class);

        super.switchRenderers();

//        ComplexNumber newSourceStart = getSourceRenderer().getSystemContext().getNumberFactory().createComplexNumber(0, 0);
        ParamSupplier newSourceStartSupp = getTargetParamContainer().getClientParameter(CommonFractalParameters.PARAM_ZSTART);
        NumberFactory nf = getSourceRenderer().getSystemContext().getNumberFactory();
        ComplexNumber newSourceStart = newSourceStartSupp instanceof StaticParamSupplier ? newSourceStartSupp.getGeneral(ComplexNumber.class) : nf.createComplexNumber(0,0);
        getSourceParamContainer().addClientParameter(new StaticParamSupplier(CommonFractalParameters.PARAM_ZSTART, newSourceStart));
        getSourceParamContainer().addClientParameter(new CoordinateBasicShiftParamSupplier("c"));
        getSourceParamContainer().addClientParameter(new StaticParamSupplier("midpoint", preservedSourceMidpoint.copy()));
        getSourceParamContainer().addClientParameter(new StaticParamSupplier("zoom", preservedSourceZoom.copy()));

        getTargetParamContainer().addClientParameter(new CoordinateBasicShiftParamSupplier(CommonFractalParameters.PARAM_ZSTART));
        getTargetParamContainer().addClientParameter(new StaticParamSupplier("c", preservedSourceMidpoint.copy()));
        getTargetParamContainer().addClientParameter(new StaticParamSupplier("midpoint", preservedTargetMidpoint.copy()));
        getTargetParamContainer().addClientParameter(new StaticParamSupplier("zoom", preservedTargetZoom.copy()));

        resetRenderer(getSourceRenderer());
        resetRenderer(getTargetRenderer());

//        getTargetParamContainer().getClientParameter("c").updateChanged(getTargetParamContainer().getClientParameter("c"));
//        getSourceParamContainer().getClientParameter("c").updateChanged(getSourceParamContainer().getClientParameter("c"));

        switching = false;
    }

    @Override
    protected boolean syncParams() {

        if (!isActive())
            return false;

        boolean changed = super.syncParams();

        ExpressionsParam expressions = getSourceParamContainer().getClientParameter(CommonFractalParameters.PARAM_EXPRESSIONS).getGeneral(ExpressionsParam.class);
        ComputeExpression computeExpression;
        try {
            computeExpression = new ComputeExpressionBuilder(expressions, new HashMap<>()).getComputeExpression();
        } catch (IllegalArgumentException e){
            getSourceParamContainer().addClientParameter(new StaticParamSupplier(CommonFractalParameters.PARAM_EXPRESSIONS, new ExpressionsParam("z^2+c", "z")));
            e.printStackTrace();
            return false;
        }
        if (computeExpression != null){
            for (String name : computeExpression.getConstantNames()){
                ParamSupplier actualSupp = (ParamSupplier) getSourceParamContainer().getClientParameter(name);
                ParamSupplier prevTargetSupp = (ParamSupplier) getTargetParamContainer().getClientParameter(name);
                if (actualSupp != null && (prevTargetSupp == null || !actualSupp.equals(prevTargetSupp))) {
                    String nameFromSupp = actualSupp.getName();
                    if (!name.equals("c") && !name.equals(CommonFractalParameters.PARAM_ZSTART)) {
                        getTargetParamContainer().addClientParameter(actualSupp);
                        changed = true;
                    }
                }
            }
        }

        boolean setTargetJuliaset = getSourceParamContainer()
                .getClientParameter(CommonFractalParameters.PARAM_ZSTART) instanceof StaticParamSupplier;

        if (!setTargetJuliaset) {
            switchRenderers();
            return false; //updated renderers already after switching
        }

        ParamContainer sourceContainer = getSourceParamContainer();
        ParamContainer targetContainer = getTargetParamContainer();

        ComplexNumber sourceMidpoint = sourceContainer.getClientParameter("midpoint").getGeneral(ComplexNumber.class);
        ParamSupplier targetCSupp = targetContainer.getClientParameter("c");
        if (!(targetCSupp instanceof StaticParamSupplier) || !sourceMidpoint.equals(targetCSupp.getGeneral(ComplexNumber.class))) {
            targetContainer.addClientParameter(new StaticParamSupplier("c", sourceMidpoint.copy()));
            changed = true;
        }
        if (!(targetContainer.getClientParameter(CommonFractalParameters.PARAM_ZSTART) instanceof CoordinateBasicShiftParamSupplier)){
            targetContainer.addClientParameter(new CoordinateBasicShiftParamSupplier(CommonFractalParameters.PARAM_ZSTART));
            changed = true;
        }

        return changed;
    }

    @Override
    public boolean isActive() {
        ParamSupplier sourceStart = getSourceParamContainer().getClientParameter(CommonFractalParameters.PARAM_ZSTART);
        ParamSupplier sourceC = getSourceParamContainer().getClientParameter("c");
        ParamSupplier targetStart = getTargetParamContainer().getClientParameter(CommonFractalParameters.PARAM_ZSTART);
        ParamSupplier targetC = getTargetParamContainer().getClientParameter("c");

        ParamSupplier[] checkParams = new ParamSupplier[]{sourceStart, sourceC, targetStart, targetC};
        for (ParamSupplier supp : checkParams)
            if (!(supp instanceof StaticParamSupplier) && !(supp instanceof CoordinateBasicShiftParamSupplier))
                return false;

        return true;
    }
}
