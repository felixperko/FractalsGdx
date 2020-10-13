package de.felixp.fractalsgdx.interpolation;

import de.felixperko.fractals.system.numbers.NumberFactory;

public interface ParameterInterpolation {

    String getParameterName();
    double getTimeFactor();
    Object getInterpolatedValue(double progress, NumberFactory numberFactory);
}
