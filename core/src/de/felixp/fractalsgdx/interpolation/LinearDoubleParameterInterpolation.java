package de.felixp.fractalsgdx.interpolation;

import de.felixperko.fractals.system.numbers.NumberFactory;

public class LinearDoubleParameterInterpolation extends AbstractSimpleParameterInterpolation{

    double start;
    double end;

    public LinearDoubleParameterInterpolation(String parameterName, double timeFactor, double start, double end) {
        super(parameterName, timeFactor);
        this.start = start;
        this.end = end;
    }

    @Override
    public Object getInterpolatedValue(double progress, NumberFactory numberFactory) {
        double prog = scaleProgress(progress);
        return prog*(end-start)+start;
    }
}
