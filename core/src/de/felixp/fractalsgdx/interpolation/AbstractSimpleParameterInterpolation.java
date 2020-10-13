package de.felixp.fractalsgdx.interpolation;

public abstract class AbstractSimpleParameterInterpolation<T> extends AbstractParameterInterpolation<T> {

    double timeFactor;

    public AbstractSimpleParameterInterpolation(String parameterName, double timeFactor) {
        super(parameterName);
        this.timeFactor = timeFactor;
    }

    @Override
    public double getTimeFactor() {
        return timeFactor;
    }

    protected double scaleProgress(double progress) {
        return (progress % timeFactor)/timeFactor;
    }
}
