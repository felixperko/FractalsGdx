package de.felixp.fractalsgdx.animation.interpolationTypes;

import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;

public class LinearInterpolationFunction extends AbstractInterpolationFunction{

    @Override
    public boolean isPathBased() {
        return true;
    }

    @Override
    public Number getInterpolatedValue(int currentStartIndex, Number progressBetween) {
        Number value0 = getValueForIndex(currentStartIndex);
        Number value1 = getValueForIndex(currentStartIndex+1);
        Number res = value1.copy();
        res.sub(value0);
        res.mult(progressBetween);
        res.add(value0);
        return res;
    }
}
