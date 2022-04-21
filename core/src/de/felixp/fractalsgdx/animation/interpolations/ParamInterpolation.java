package de.felixp.fractalsgdx.animation.interpolations;

import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;

import java.util.List;

import de.felixp.fractalsgdx.animation.interpolationTypes.InterpolationFunction;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;

public interface ParamInterpolation<T> {

    String getParamName();
    /**
     * the attribute name if interpolation is for a ParamAttribute, null if interpolation is for a param object.
     * @return
     */
    String getAttributeName();
    String getParamContainerKey();
    String getParamType();
    ParamInterpolation getControlPointParent();
    void setParam(String paramName, String paramType, String paramContainer, String attributeName);

    InterpolationFunction setInterpolationFunction(Class<? extends InterpolationFunction> interpolationFunctionClass);
    InterpolationFunction getInterpolationFunction();
    Class<? extends InterpolationFunction> getInterpolationFunctionClass();
    boolean isPathBased();

    boolean isAutomaticTimings();
    void setAutomaticTimings(boolean automaticTimings);
    List<Double> getTimings(boolean inheritedValue);
    void setTiming(int index, double time);

    T getInterpolatedValue(double progressInLoop, NumberFactory numberFactory);
    T getInterpolatedValue(int cpIndex0, int cpIndex1, Number progressBetween);
    T getControlPointValue(int controlPointIndex);
    T getDefaultValue();

    void setControlPointParent(ParamInterpolation controlPointParent);
    void setControlPointParent(ParamInterpolation controlPointParent, boolean imagPart);
    void addControlPointChild(ParamInterpolation controlPointChild);
    void removeControlPointChild(ParamInterpolation controlPointChild);

    void setDefValues(List<Number> defValues);
    List<Number> getDefValues(boolean inheritedValue);
    List<T> getControlPoints(boolean inheritedValue);
    List<T> getControlDerivatives(boolean inheritedValue);
    void setNumberFactory(NumberFactory numberFactory);
    NumberFactory getNumberFactory();

    void setControlPoints(List<Number> defValues, List<T> controlPoints, List<T> derivatives, NumberFactory numberFactory);
    void addControlPoint(T controlPoint, T derivative, NumberFactory numberFactory);
    void insertControlPoint(int index, T controlPoint, T derivative, NumberFactory numberFactory);
    void setControlPoint(int index, T controlPoint, T derivative, NumberFactory numberFactory);
    void moveControlPoint(T oldControlPoint, T newControlPoint, T newDerivative, boolean setIfInheritedValue);
    void removeControlPoint(int index);
    void clearControlPoints();

    void dispose();

    List<VisTextField> addValueFieldsToTable(VisTable table, Object controlPoint, int index);
}
