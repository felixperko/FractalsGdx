package de.felixp.fractalsgdx.animation.interpolationTypes;

import java.util.List;
import java.util.Map;

import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;

public interface InterpolationFunction {

    boolean isPathBased();

    void setValues(NumberFactory numberFactory, List<Number> defValues, List<Number> controlValues, List<Number> controlDerivatives);
    void setNumberFactory(NumberFactory numberFactory);
    int getActiveDefValueSet();
    Map<String, Number> getDefValueDefaultsForActiveSet();
    String[][] getDefValueNames();
    Number[][] getDefValueDefaults();

    Number getInterpolatedValue(int currentStartIndex, Number progressBetween);

//    boolean isDrawCullingEnabled();
//    List<Double> getRangeIntersectionProgresses(Number rangeStart, Number rangeEnd);
//    int getMaxIntersectionProgresses();
//    Number getLowerBound();
//    Number getUpperBound();

    int getNoCullingDrawSegments();

    void setImag(boolean imag);
//    int getCulledPixelPrecision();
}
