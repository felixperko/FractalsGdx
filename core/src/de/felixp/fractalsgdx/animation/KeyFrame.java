package de.felixp.fractalsgdx.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.animation.interpolationTypes.LinearInterpolationFunction;
import de.felixp.fractalsgdx.animation.interpolations.ComplexNumberParamInterpolation;
import de.felixp.fractalsgdx.animation.interpolations.NumberParamInterpolation;
import de.felixp.fractalsgdx.animation.interpolations.ParamInterpolation;
import de.felixp.fractalsgdx.ui.AnimationsUI;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;

public class KeyFrame {

    ParamContainer params;

    public KeyFrame(ParamContainer parentParamContainer){
        this.params = new ParamContainer(parentParamContainer, true);
    }

    public List<ParamInterpolation> getInterpolationsBetween(KeyFrame beginKeyFrame){
        Map<String, ParamSupplier> beginSuppliers = new HashMap<>();
        Map<String, ParamSupplier> endSuppliers = new HashMap<>();

        for (ParamSupplier beginSupp : beginKeyFrame.params.getParameters()){
            ParamSupplier endSupp = params.getParam(beginSupp.getUID());

            //skip dynamic
            if (!(beginSupp instanceof StaticParamSupplier) || !(endSupp instanceof StaticParamSupplier))
                continue;

            if (beginSupp.getGeneral().equals(endSupp.getGeneral()))
                continue;

            beginSuppliers.put(beginSupp.getUID(), beginSupp);
            endSuppliers.put(beginSupp.getUID(), endSupp);
        }

        //TODO add interpolations for attributes
        //add interpolations for new/removed params using default value?

        List<ParamInterpolation> interpolations = new ArrayList<>();
        for (Map.Entry<String, ParamSupplier> e : beginSuppliers.entrySet()){
            String uid = e.getKey();
            ParamSupplier beginSupp = e.getValue();
            ParamSupplier endSupp = endSuppliers.get(uid);


            Object valBegin = beginSupp.getGeneral();
            Object valEnd = endSupp.getGeneral();
            ParamInterpolation interpolation = null;

            String name = null;
            ParamConfiguration paramConfig = params.getParamConfiguration();
            if (paramConfig != null)
                name = paramConfig.getName(uid);
            if (name == null)
                name = uid;
            if (valBegin instanceof ComplexNumber && valEnd instanceof ComplexNumber){
                interpolation = new ComplexNumberParamInterpolation(uid, name, AnimationsUI.PARAM_TYPE_NUMBER, AnimationsUI.PARAM_CONTAINERKEY_SERVER, null, LinearInterpolationFunction.class);
            }
            if (valBegin instanceof Number && valEnd instanceof Number){
                interpolation = new NumberParamInterpolation(uid, name, AnimationsUI.PARAM_TYPE_NUMBER, AnimationsUI.PARAM_CONTAINERKEY_SERVER, null, LinearInterpolationFunction.class);
            }

            if (interpolation != null){
                NumberFactory nf = beginKeyFrame.params.getParam(CommonFractalParameters.PARAM_NUMBERFACTORY).getGeneral(NumberFactory.class);
                List<Object> controlPoints = new ArrayList<>();
                controlPoints.add(valBegin);
                controlPoints.add(valEnd);
                interpolation.setControlPoints(null, controlPoints, null, nf);
                interpolations.add(interpolation);
            }
        }

        return interpolations;
    }
}
