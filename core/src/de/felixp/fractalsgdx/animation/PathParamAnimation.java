package de.felixp.fractalsgdx.animation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.animation.interpolations.ComplexNumberParamInterpolation;
import de.felixp.fractalsgdx.animation.interpolations.ParamInterpolation;
import de.felixp.fractalsgdx.ui.AnimationsUI;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;

public class PathParamAnimation extends AbstractParamAnimation<ComplexNumber> {

    NumberFactory numberFactory;

//    List<ComplexNumber> controlPoints = new ArrayList<>();
//    List<ComplexNumber> tangents = new ArrayList<>();

    public PathParamAnimation(String name) {
        super(name);
//        setInterpolation(new ComplexNumberParamInterpolation(parameterName, AnimationsUI.PARAM_TYPE_COMPLEXNUMBER, AnimationsUI.PARAM_CONTAINERKEY_SERVER));
    }

    public void setControlPoints(String interpolationParamName, String attrName, List<ComplexNumber> controlPoints, NumberFactory numberFactory){
        setControlPoints(interpolationParamName, attrName, controlPoints, null, numberFactory);
    }

    public void setControlPoints(String interpolationParamName, String attrName, List<ComplexNumber> controlPoints, List<ComplexNumber> tangents, NumberFactory numberFactory){

        if (numberFactory != null)
            this.numberFactory = numberFactory;
        else if (this.numberFactory == null)
            throw new IllegalStateException("first setControlPoints() needs to supply a NumberFactory");

        ParamInterpolation interpolation = getInterpolation(interpolationParamName, attrName);
        interpolation.setControlPoints(interpolation.getDefValues(false), controlPoints, tangents, this.numberFactory);

    }

    public void addControlPoint(String interpolationParamName, String attrName, ComplexNumber controlPoint, ComplexNumber tangent, NumberFactory numberFactory){
//        controlPoints.add(controlPoint);
//        tangents.add(tangent);
//        setControlPoints(controlPoints, tangents, numberFactory);
        getInterpolation(interpolationParamName, attrName).addControlPoint(controlPoint, tangent, numberFactory);
    }

//    @Override
//    public ComplexNumber getInterpolatedValueInLoop(double progressInLoop, NumberFactory numberFactory) {
//        ParamInterpolation interpolation = getInterpolation(parameterName);
//        Object interpolatedValue = interpolation.getInterpolatedValue(progressInLoop, numberFactory);
//        return (ComplexNumber) interpolatedValue;
////        int index0 = getControlPointIndex(progressInLoop);
////        int index1 = index0+1;
////        if (index0 == -1)
////            return numberFactory.createComplexNumber(0,0);
////        if (index0 == 0 && controlPoints.size() == 1)
////            return controlPoints.get(0).copy();
////        if (progressInLoop == 1.0)
////            return controlPoints.get(controlPoints.size()-1).copy();
////        double loopProgressAt0 = getProgressAtControlPoint(index0);
////        double loopProgressAt1 = getProgressAtControlPoint(index1);
////        double progressBetween = (progressInLoop-loopProgressAt0)/(loopProgressAt1-loopProgressAt0);
////        return getInterpolatedPathValue(index0, index1, progressBetween);
//    }

//    public ComplexNumber getInterpolatedPathValue(int index0, int index1, double progressBetween) {
//        return (ComplexNumber) getInterpolation(parameterName).getInterpolatedValue(index0, index1, progressBetween);
//    }
//
//    public ComplexNumber getControlPoint(int index){
//        return controlPoints.get(index);
//    }
//
//    public int getControlPointIndex(double progressInLoop){
//        int index1 = -1;
//        for (Map.Entry<Double, Integer> e : progressPointIndexMapping.entrySet()){
//            if (e.getKey() <= progressInLoop)
//                index1++;
//            else {
//                return index1;
//            }
//        }
//        return index1;
//    }

    public void movePathPoint(int index, ComplexNumber point){

    }

//    public void clearControlPoints(){
//        setControlPoints(new ArrayList<>(), numberFactory);
//    }
//
//    public ComplexNumber getNormal(int index){
//        if (tangents == null || index < 0 || index >= tangents.size())
//            return null;
//        return tangents.get(index);
//    }
//
//    public NumberFactory getNumberFactory(){
//        return numberFactory;
//    }
//
//    public List<ComplexNumber> getControlPoints(){
//        return controlPoints;
//    }
//
//    public List<ComplexNumber> getControlDerivatives(){
//        return tangents;
//    }
}
