package de.felixp.fractalsgdx.rendering.rendererlink;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.rendering.GPUSystemContext;
import de.felixperko.expressions.ComputeExpressionBuilder;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.calculator.ComputeExpression;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateDiscreteModuloParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateModuloParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.MappedParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.common.BFOrbitCommon;

public class JuliasetRendererLink extends DefaultRendererLink{

    public JuliasetRendererLink(FractalRenderer sourceRenderer, FractalRenderer targetRenderer) {
        super(sourceRenderer, targetRenderer, Arrays.asList("limit", "iterations", BFOrbitCommon.PARAM_EXPRESSION, "condition", GPUSystemContext.PARAMNAME_ORBITTRAPS));
    }

    @Override
    public void switchRenderers() {

        ComplexNumber preservedSourceMidpoint = getSourceParamContainer().getClientParameter("midpoint").getGeneral(ComplexNumber.class);
        ComplexNumber preservedTargetMidpoint = getTargetParamContainer().getClientParameter("midpoint").getGeneral(ComplexNumber.class);

        Number preservedSourceZoom = getSourceParamContainer().getClientParameter("zoom").getGeneral(Number.class);
        Number preservedTargetZoom = getTargetParamContainer().getClientParameter("zoom").getGeneral(Number.class);

        super.switchRenderers();

//        ComplexNumber newSourceStart = getSourceRenderer().getSystemContext().getNumberFactory().createComplexNumber(0, 0);
        ParamSupplier newSourceStartSupp = getTargetParamContainer().getClientParameter(BFOrbitCommon.PARAM_ZSTART);
        NumberFactory nf = getSourceRenderer().getSystemContext().getNumberFactory();
        ComplexNumber newSourceStart = newSourceStartSupp instanceof StaticParamSupplier ? newSourceStartSupp.getGeneral(ComplexNumber.class) : nf.createComplexNumber(0,0);
        getSourceParamContainer().addClientParameter(new StaticParamSupplier(BFOrbitCommon.PARAM_ZSTART, newSourceStart));
        getSourceParamContainer().addClientParameter(new CoordinateBasicShiftParamSupplier("c"));
        getSourceParamContainer().addClientParameter(new StaticParamSupplier("midpoint", preservedSourceMidpoint.copy()));
        getSourceParamContainer().addClientParameter(new StaticParamSupplier("zoom", preservedSourceZoom.copy()));

        getTargetParamContainer().addClientParameter(new CoordinateBasicShiftParamSupplier(BFOrbitCommon.PARAM_ZSTART));
        getTargetParamContainer().addClientParameter(new StaticParamSupplier("c", preservedSourceMidpoint.copy()));
        getTargetParamContainer().addClientParameter(new StaticParamSupplier("midpoint", preservedTargetMidpoint.copy()));
        getTargetParamContainer().addClientParameter(new StaticParamSupplier("zoom", preservedTargetZoom.copy()));

        resetRenderer(getSourceRenderer());
//        resetRenderer(getTargetRenderer());
    }

    @Override
    protected boolean syncParams() {

        boolean changed = super.syncParams();

        String formula = getSourceParamContainer().getClientParameter(BFOrbitCommon.PARAM_EXPRESSION).getGeneral(String.class);
        ComputeExpression computeExpression = new ComputeExpressionBuilder(formula, "z", new HashMap<>()).getComputeExpression();
        if (computeExpression != null){
            for (String name : computeExpression.getConstantNames()){
                ParamSupplier actualSupp = (ParamSupplier) getSourceParamContainer().getClientParameter(name);
                ParamSupplier prevTargetSupp = (ParamSupplier) getTargetParamContainer().getClientParameter(name);
                if (actualSupp != null && (prevTargetSupp == null || !actualSupp.equals(prevTargetSupp))) {
                    getTargetParamContainer().addClientParameter(actualSupp);
                    changed = true;
                }
            }
        }

        boolean setTargetJuliaset = getSourceParamContainer().getClientParameter(BFOrbitCommon.PARAM_ZSTART) instanceof StaticParamSupplier;
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
        if (!(targetContainer.getClientParameter(BFOrbitCommon.PARAM_ZSTART) instanceof CoordinateBasicShiftParamSupplier)){
            targetContainer.addClientParameter(new CoordinateBasicShiftParamSupplier(BFOrbitCommon.PARAM_ZSTART));
            changed = true;
        }

        return changed;
    }
}
