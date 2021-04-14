package de.felixp.fractalsgdx.animation.interpolationTypes;

import de.felixperko.fractals.system.numbers.Number;

public class LogarithmicInterpolationFunction extends  LinearInterpolationFunction{

    public static final double LOG2 = Math.log(2);

    @Override
    public Number getInterpolatedValue(int currentStartIndex, Number progressBetween) {

        Number value0 = getValueForIndex(currentStartIndex);
        Number value1 = getValueForIndex(currentStartIndex+1);

        Number number0 = getNumber("0.0");
        if (value0.equals(number0))
            value0 = numberFactory.createNumber(Double.MIN_VALUE);
        if (value1.equals(number0))
            value1 = numberFactory.createNumber(Double.MIN_VALUE);

        if (value0.equals(value1))
            return value0.copy();

        boolean logarithmicGrowth = value1.toDouble() > value0.toDouble();
        if (logarithmicGrowth){
            Number valueTemp = value0;
            value0 = value1;
            value1 = valueTemp;
        }

        Number ratio = value0.copy();
        ratio.div(value1);

        //steps = log2(value0 / value1)
        double steps = Math.log(ratio.toDouble())/ LOG2;
        if (steps < 0)
            steps = 0;
        Number progressBetweenNormalized = getNumberFactory().createNumber(steps);
        progressBetweenNormalized.mult(progressBetween);

        //f(x) = value0 * 0.5 ^ (x * steps)
        Number exponentialFactor = getNumberFactory().createNumber(logarithmicGrowth ? 2.0 : 0.5);
        exponentialFactor.pow(progressBetweenNormalized);

        Number res = logarithmicGrowth ? value1.copy() : value0.copy();
        res.mult(exponentialFactor);
        return res;
    }
}
