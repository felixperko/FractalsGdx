package de.felixp.fractalsgdx.animation.interpolations;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.animation.interpolationTypes.InterpolationFunction;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;

abstract class AbstractParamInterpolation<T> implements ParamInterpolation<T>{

    private NumberFactory numberFactory;

    boolean automaticTimings = true;

    String paramName;
    String paramType;
    String paramContainerKey;
    String attributeName;

    InterpolationFunction interpolationFunction;

    LinkedHashMap<Double, Integer> progressPointIndexMapping = new LinkedHashMap<>();
    List<Double> pointIndexProgressMapping = new ArrayList<>();

    ParamInterpolation controlPointParent = this;
    List<ParamInterpolation> controlPointChildren = new ArrayList<>();
    private List<Number> defValues = new ArrayList<>();
    private List<T> controlPoints = new ArrayList<>();
    private List<T> derivatives = new ArrayList<>();

//    Map<Integer, T> controlPointsById;
//    Map<T, Integer> controlPointIds;

    Number totalLength = null;

    public AbstractParamInterpolation(String paramName, String paramType, String paramContainerKey, String attributeName, Class<? extends InterpolationFunction> interpolationFunctionClass){
        this.paramName = paramName;
        this.paramType = paramType;
        this.paramContainerKey = paramContainerKey;
        this.interpolationFunction = initInterpolationFunction(interpolationFunctionClass);
//        updateInterpolationFunction();
    }

    @Override
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Initializes and returns interpolation function with the specified or default class
     * @param interpolationFunctionClass
     * @return
     */
    protected InterpolationFunction initInterpolationFunction(Class<? extends InterpolationFunction> interpolationFunctionClass) {
        InterpolationFunction interpolationFunction;
        if (interpolationFunctionClass != null)
            interpolationFunction = initNewInterpolationFunction(interpolationFunctionClass);
        else
            interpolationFunction = getDefaultInterpolationFunction();
        return interpolationFunction;
    }

    @Override
    public Class<? extends InterpolationFunction> getInterpolationFunctionClass() {
        return interpolationFunction.getClass();
    }

    protected abstract InterpolationFunction getDefaultInterpolationFunction();
    protected abstract void resetProgressMapping(List<T> controlPoints, List<T> derivatives);
    protected abstract void updateInterpolationFunction(boolean resetDefValues);

    @Override
    public InterpolationFunction setInterpolationFunction(Class<? extends InterpolationFunction> interpolationFunctionClass) {
        interpolationFunction = initNewInterpolationFunction(interpolationFunctionClass);
        return interpolationFunction;
    }

    @Override
    public void setDefValues(List<Number> defValues) {
        this.defValues = defValues;
        updateInterpolationFunction(false);
    }

    protected void setDefaultDefValues(InterpolationFunction interpolationFunction) {
        defValues.clear();
        for (Number value : interpolationFunction.getDefValueDefaultsForActiveSet().values()){
            defValues.add(value);
        }
//        updateInterpolationFunction();
    }

    @Override
    public InterpolationFunction getInterpolationFunction() {
        return interpolationFunction;
    }

    protected InterpolationFunction initNewInterpolationFunction(Class<? extends InterpolationFunction> interpolationFunctionClass) {
        try {
            Constructor<?> declaredConstructor = interpolationFunctionClass.getDeclaredConstructors()[0];
            InterpolationFunction interpolationFunction = (InterpolationFunction) declaredConstructor.newInstance(null);
            setNumberFactory(getNumberFactory());
            return interpolationFunction;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setControlPoints(List<Number> defValues, List<T> controlPoints, List<T> derivatives, NumberFactory numberFactory) {
        this.controlPoints = controlPoints;
        this.derivatives = derivatives;
        if (this.derivatives == null)
            this.derivatives = new ArrayList<>();
        setNumberFactory(numberFactory);
        controlPointsChanged();
    }


    @Override
    public List<Double> getTimings(boolean inheritedValue) {
        return pointIndexProgressMapping;
    }

    @Override
    public void setTiming(int index, double time) {
        Double oldTime = pointIndexProgressMapping.get(index);
        if (oldTime != null) {
            if (oldTime == time)
                return;

            LinkedHashMap<Double, Integer> newTimings = new LinkedHashMap<>();
            int i = 0;
            for (Map.Entry<Double, Integer> e : progressPointIndexMapping.entrySet()){
                if (i != index)
                    newTimings.put(e.getKey(), e.getValue());
                else
                    newTimings.put(time, index);
                i++;
            }
            progressPointIndexMapping = newTimings;
            pointIndexProgressMapping.set(index, time);
//            progressPointIndexMapping.repl
        }
    }

    @Override
    public boolean isAutomaticTimings() {
        return automaticTimings;
    }

    @Override
    public void setAutomaticTimings(boolean automaticTimings) {
        this.automaticTimings = automaticTimings;
    }

    @Override
    public String getParamName() {
        return paramName;
    }


    @Override
    public void setParam(String paramName, String paramType, String paramContainerKey, String attributeName){
        this.paramName = paramName;
        this.paramType = paramType;
        this.paramContainerKey = paramContainerKey;
        this.attributeName = attributeName;
    }

    @Override
    public String getParamType() {
        return paramType;
    }

    @Override
    public String getParamContainerKey() {
        return paramContainerKey;
    }

    @Override
    public ParamInterpolation getControlPointParent() {
        return controlPointParent;
    }

    protected void controlPointsChanged(){
        updateProgressMapping();
        updateInterpolationFunction(false);
        for (ParamInterpolation controlPointChild : controlPointChildren){
            controlPointChild.setControlPoints(defValues, controlPoints, derivatives, numberFactory);
        }
    }

    @Override
    public void moveControlPoint(T oldControlPoint, T newControlPoint, T newDerivative, boolean setIfInheritedValue) {
        if (controlPointParent != this){
            if (setIfInheritedValue) {
                controlPointParent.moveControlPoint(oldControlPoint, newControlPoint, newDerivative, setIfInheritedValue);
                return;
            }
        }

        int i = 0;
        for (T c : controlPoints){
            if (c.equals(oldControlPoint)){
                controlPoints.set(i, newControlPoint);
                if (newDerivative != null)
                    derivatives.set(i, newDerivative);
                controlPointsChanged();
                return;
            }
            i++;
        }
    }

    private void updateProgressMapping() {
        //reset progress to control point mapping
        progressPointIndexMapping.clear();
        pointIndexProgressMapping.clear();

        totalLength = numberFactory.createNumber(0.0);
        resetProgressMapping(controlPoints, derivatives);
    }

    @Override
    public T getInterpolatedValue(double progressInLoop, NumberFactory numberFactory) {
        setNumberFactory(numberFactory);
        Number prog = getNumberFactory().createNumber(progressInLoop);

        if (!isPathBased())
            return getInterpolatedValue(-1, -1, prog);

        int index0 = getControlPointIndex(progressInLoop);
        int index1 = index0 + 1;
        if (index0 == -1)
            return getDefaultValue();
        int controlPointCount = getControlPoints(true).size();
        if (index0 == 0 && controlPointCount == 1)
            return getControlPointValue(0);
        if (progressInLoop == 1.0) {
            return getControlPointValue(controlPointCount -1);
        }
        double loopProgressAt0 = getProgressAtControlPoint(index0);
        double loopProgressAt1 = getProgressAtControlPoint(index1);
        double progressBetween = (progressInLoop-loopProgressAt0)/(loopProgressAt1-loopProgressAt0);
        Number progBetween = getNumberFactory().createNumber(progressBetween);
        return getInterpolatedValue(index0, index1, progBetween);
    }

    @Override
    public boolean isPathBased() {
        return interpolationFunction != null && interpolationFunction.isPathBased();
    }

    @Override
    public NumberFactory getNumberFactory() {
        return numberFactory;
    }

    @Override
    public void setNumberFactory(NumberFactory numberFactory) {
        if (numberFactory != null) {
            NumberFactory oldFactory = this.numberFactory;
            this.numberFactory = numberFactory;
            if (this.interpolationFunction != null){
                this.interpolationFunction.setNumberFactory(numberFactory);
                updateInterpolationFunction(!numberFactory.equals(oldFactory));
            }
        }
    }

    public double getProgressAtControlPoint(int index){
        if (!controlPointParent.equals(this))
            return ((AbstractParamInterpolation)controlPointParent).getProgressAtControlPoint(index);
        return pointIndexProgressMapping.get(index);
    }

    public int getControlPointIndex(double progressInLoop){
        if (!controlPointParent.equals(this))
            return ((AbstractParamInterpolation)controlPointParent).getControlPointIndex(progressInLoop);
        int index1 = -1;
        for (Map.Entry<Double, Integer> e : progressPointIndexMapping.entrySet()){
            if (e.getKey() <= progressInLoop)
                index1++;
            else {
                return index1;
            }
        }
        return index1;
    }

    @Override
    public List<Number> getDefValues(boolean inheritedValue) {
        if (inheritedValue && !controlPointParent.equals(this))
            return controlPointParent.getDefValues(true);
        return defValues;
    }

    @Override
    public List<T> getControlPoints(boolean inheritedValue) {
        if (inheritedValue && !controlPointParent.equals(this))
            return controlPointParent.getControlPoints(true);
        return controlPoints;
    }

    @Override
    public List<T> getControlDerivatives(boolean inheritedValue) {
        if (inheritedValue && !controlPointParent.equals(this))
            return controlPointParent.getControlDerivatives(true);
        return derivatives;
    }

    @Override
    public T getControlPointValue(int controlPointIndex) {
        if (!controlPointParent.equals(this))
            return (T)controlPointParent.getControlPointValue(controlPointIndex);
        return controlPoints.get(controlPointIndex);
    }

    @Override
    public void setControlPoint(int index, T controlPoint, T derivative, NumberFactory numberFactory) {
        controlPoints.set(index, controlPoint);
        derivatives.set(index, derivative);
        setNumberFactory(numberFactory);
        controlPointsChanged();
    }

    @Override
    public void removeControlPoint(int index) {
        controlPoints.remove(index);
        derivatives.remove(index);
        controlPointsChanged();
    }

    @Override
    public void clearControlPoints() {
        controlPoints.clear();
        derivatives.clear();
        controlPointsChanged();
    }

    @Override
    public void addControlPoint(T controlPoint, T derivative, NumberFactory numberFactory) {
        controlPoints.add(controlPoint);
        derivatives.add(derivative);
        setControlPoints(defValues, controlPoints, derivatives, numberFactory);
    }

    @Override
    public void insertControlPoint(int index, T controlPoint, T derivative, NumberFactory numberFactory) {
        controlPoints.add(index, controlPoint);
        derivatives.add(index, derivative);
        controlPointsChanged();
    }

    @Override
    public void setControlPointParent(ParamInterpolation controlPointParent) {
        if (this.controlPointParent != null)
            this.controlPointParent.removeControlPointChild(this);
        this.controlPointParent = controlPointParent;
        controlPointParent.addControlPointChild(this);
        setControlPoints(
                controlPointParent.getDefValues(true),
                controlPointParent.getControlPoints(true),
                controlPointParent.getControlDerivatives(true),
                controlPointParent.getNumberFactory());
    }

    @Override
    public void addControlPointChild(ParamInterpolation controlPointChild) {
        this.controlPointChildren.add(controlPointChild);
    }

    @Override
    public void removeControlPointChild(ParamInterpolation controlPointChild) {
        this.controlPointChildren.remove(controlPointChild);
    }

    @Override
    public void dispose() {
        if (this.controlPointParent != null)
            this.controlPointParent.removeControlPointChild(this);
    }
}
