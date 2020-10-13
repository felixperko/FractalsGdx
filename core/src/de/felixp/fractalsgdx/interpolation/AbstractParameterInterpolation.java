package de.felixp.fractalsgdx.interpolation;

import de.felixperko.fractals.system.numbers.NumberFactory;

abstract class AbstractParameterInterpolation<T> implements ParameterInterpolation{

    protected String parameterName;

    public AbstractParameterInterpolation(String parameterName){
        this.parameterName = parameterName;
    }

    @Override
    public String getParameterName() {
        return parameterName;
    }

    @Override
    public abstract T getInterpolatedValue(double progress, NumberFactory numberFactory);
}
