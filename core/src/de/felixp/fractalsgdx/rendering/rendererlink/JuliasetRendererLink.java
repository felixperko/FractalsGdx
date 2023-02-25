package de.felixp.fractalsgdx.rendering.rendererlink;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.rendering.ShaderSystemContext;
import de.felixperko.expressions.ComputeExpressionBuilder;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.calculator.ComputeExpression;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ExpressionsParam;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;

import static de.felixperko.fractals.system.systems.common.CommonFractalParameters.*;

public class JuliasetRendererLink extends DefaultRendererLink{

    public static boolean isSyncMouse(){
        return Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT);
    }

    public JuliasetRendererLink(FractalRenderer sourceRenderer, FractalRenderer targetRenderer) {
        super(sourceRenderer, targetRenderer,
                Arrays.asList(ShaderSystemContext.PARAM_LIMIT,
                        CommonFractalParameters.PARAM_ITERATIONS,
                        CommonFractalParameters.PARAM_EXPRESSIONS,
                        ShaderSystemContext.PARAM_CONDITION,
                        ShaderSystemContext.PARAM_ORBITTRAPS));
    }

    boolean switching = false;

    @Override
    public synchronized void switchRenderers() {

        if (switching)
            return;
        switching = true;

        ComplexNumber preservedSourceMidpoint = getSourceParamContainer().getParam(PARAM_MIDPOINT).getGeneral(ComplexNumber.class);
        ComplexNumber preservedTargetMidpoint = getTargetParamContainer().getParam(PARAM_MIDPOINT).getGeneral(ComplexNumber.class);

        Number preservedSourceZoom = getSourceParamContainer().getParam(ShaderSystemContext.PARAM_ZOOM).getGeneral(Number.class);
        Number preservedTargetZoom = getTargetParamContainer().getParam(ShaderSystemContext.PARAM_ZOOM).getGeneral(Number.class);

        super.switchRenderers();

//        ComplexNumber newSourceStart = getSourceRenderer().getSystemContext().getNumberFactory().createComplexNumber(0, 0);
        ParamSupplier newSourceStartSupp = getTargetParamContainer().getParam(PARAM_ZSTART);
        NumberFactory nf = getSourceRenderer().getSystemContext().getNumberFactory();
        ComplexNumber newSourceStart = newSourceStartSupp instanceof StaticParamSupplier ? newSourceStartSupp.getGeneral(ComplexNumber.class) : nf.createComplexNumber(0,0);
        getSourceParamContainer().addParam(new StaticParamSupplier(PARAM_ZSTART, newSourceStart));
        getSourceParamContainer().addParam(new CoordinateBasicShiftParamSupplier(PARAM_C));
        getSourceParamContainer().addParam(new StaticParamSupplier(PARAM_MIDPOINT, preservedSourceMidpoint.copy()));
        getSourceParamContainer().addParam(new StaticParamSupplier(ShaderSystemContext.PARAM_ZOOM, preservedSourceZoom.copy()));

        getTargetParamContainer().addParam(new CoordinateBasicShiftParamSupplier(PARAM_ZSTART));
        getTargetParamContainer().addParam(new StaticParamSupplier(PARAM_C, preservedSourceMidpoint.copy()));
//        if (!syncMouse)
//            getTargetParamContainer().addParam(new StaticParamSupplier(PARAM_C, preservedSourceMidpoint.copy()));
//        else
//            getTargetParamContainer().addParam(new StaticParamSupplier(PARAM_C, getSourceParamContainer().getParam(ShaderSystemContext.PARAM_MOUSE).getGeneral(ComplexNumber.class)));
        getTargetParamContainer().addParam(new StaticParamSupplier(PARAM_MIDPOINT, preservedTargetMidpoint.copy()));
        getTargetParamContainer().addParam(new StaticParamSupplier(ShaderSystemContext.PARAM_ZOOM, preservedTargetZoom.copy()));

        resetRenderer(getSourceRenderer());
        resetRenderer(getTargetRenderer());

//        getTargetParamContainer().getParam("c").updateChanged(getTargetParamContainer().getParam("c"));
//        getSourceParamContainer().getParam("c").updateChanged(getSourceParamContainer().getParam("c"));

        switching = false;
    }

    @Override
    protected boolean syncParams() {

        if (!isActive())
            return false;

        boolean changed = super.syncParams();

        ExpressionsParam expressions = getSourceParamContainer().getParam(CommonFractalParameters.PARAM_EXPRESSIONS).getGeneral(ExpressionsParam.class);
        ComputeExpression computeExpression;
        try {
            ParamConfiguration sourceConfig = sourceRenderer.getSystemContext().getParamConfiguration();
            computeExpression = new ComputeExpressionBuilder(expressions, getSourceParamContainer().getParamMap(), sourceConfig.getUIDsByName()).getComputeExpression();
        } catch (IllegalArgumentException e){
            getSourceParamContainer().addParam(new StaticParamSupplier(CommonFractalParameters.PARAM_EXPRESSIONS, new ExpressionsParam("z^2+c", "z")));
            e.printStackTrace();
            return false;
        }
        if (computeExpression != null){
            //sync static values
            for (String name : computeExpression.getConstantNames()){
                ParamSupplier actualSupp = (ParamSupplier) getSourceParamContainer().getParam(name);
                ParamSupplier prevTargetSupp = (ParamSupplier) getTargetParamContainer().getParam(name);
                if (actualSupp != null && (prevTargetSupp == null || !actualSupp.equals(prevTargetSupp))) {
                    String uid = actualSupp.getUID();
                    if (!uid.equals(PARAM_C) && !uid.equals(CommonFractalParameters.PARAM_ZSTART)) {
                        getTargetParamContainer().addParam(actualSupp);
                        changed = true;
                    }
                }
            }
        }

        boolean setTargetJuliaset = getSourceParamContainer()
                .getParam(CommonFractalParameters.PARAM_ZSTART) instanceof StaticParamSupplier;

        if (!setTargetJuliaset) {
            switchRenderers();
            return false; //updated renderers already after switching
        }

        ParamContainer sourceContainer = getSourceParamContainer();
        ParamContainer targetContainer = getTargetParamContainer();

        ComplexNumber sourceMidpoint = sourceContainer.getParam(PARAM_MIDPOINT).getGeneral(ComplexNumber.class);
        ComplexNumber sourceMouse = getSourceParamContainer().getParam(ShaderSystemContext.PARAM_MOUSE).getGeneral(ComplexNumber.class);
        ComplexNumber newC = isSyncMouse() ? sourceMouse : sourceMidpoint;
        ParamSupplier targetCSupp = targetContainer.getParam(PARAM_C);
        if (!(targetCSupp instanceof StaticParamSupplier) || !newC.equals(targetCSupp.getGeneral(ComplexNumber.class))) {
            targetContainer.addParam(new StaticParamSupplier(PARAM_C, newC.copy()));
            changed = true;
        }
        if (!(targetContainer.getParam(CommonFractalParameters.PARAM_ZSTART) instanceof CoordinateBasicShiftParamSupplier)){
            targetContainer.addParam(new CoordinateBasicShiftParamSupplier(CommonFractalParameters.PARAM_ZSTART));
            changed = true;
        }

        return changed;
    }

    @Override
    public boolean isActive() {
        ParamSupplier sourceStart = getSourceParamContainer().getParam(CommonFractalParameters.PARAM_ZSTART);
        ParamSupplier sourceC = getSourceParamContainer().getParam(PARAM_C);
        ParamSupplier targetStart = getTargetParamContainer().getParam(CommonFractalParameters.PARAM_ZSTART);
        ParamSupplier targetC = getTargetParamContainer().getParam(PARAM_C);

        ParamSupplier[] checkParams = new ParamSupplier[]{sourceStart, sourceC, targetStart, targetC};
        for (ParamSupplier supp : checkParams)
            if (!(supp instanceof StaticParamSupplier) && !(supp instanceof CoordinateBasicShiftParamSupplier))
                return false;

        return true;
    }
}
