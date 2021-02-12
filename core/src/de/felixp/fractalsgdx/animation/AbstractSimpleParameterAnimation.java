package de.felixp.fractalsgdx.animation;

@Deprecated
public abstract class AbstractSimpleParameterAnimation<T> extends AbstractParamAnimation<T> {

    double timeFactor;

    public AbstractSimpleParameterAnimation(String name, String parameterName, double timeFactor) {
        super(name);
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
