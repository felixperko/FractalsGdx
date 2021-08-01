package de.felixp.fractalsgdx.animation;

import de.felixperko.fractals.system.numbers.NumberFactory;

@Deprecated
public class LinearDoubleParameterAnimation extends AbstractSimpleParameterAnimation {

    double start;
    double end;

    public LinearDoubleParameterAnimation(String name, String parameterName, double timeFactor, double start, double end) {
        super(name, parameterName, timeFactor);
        this.start = start;
        this.end = end;
    }

//    @Override
//    public Object getInterpolatedValueInLoop(double progress, NumberFactory nf) {
//        double prog = scaleProgress(progress);
//        return prog*(end-start)+start;
//    }
}
