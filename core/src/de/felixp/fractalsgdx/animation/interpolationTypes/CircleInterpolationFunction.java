package de.felixp.fractalsgdx.animation.interpolationTypes;

import java.util.ArrayList;
import java.util.List;

import de.felixperko.fractals.system.numbers.Number;

public class CircleInterpolationFunction extends AbstractInterpolationFunction{

    @Override
    public boolean isPathBased() {
        return false;
    }

    @Override
    public Number getInterpolatedValue(int currentStartIndex, Number progressBetween) {
        double timeAngle = (progressBetween.toDouble() * Math.PI * 2);
        double radius = 1.0;

        Number val = null;
        if (isReal()){
            val = getNumberFactory().createNumber(Math.cos(timeAngle)*radius);
        }
        else if (isImag()){
            val = getNumberFactory().createNumber(Math.sin(timeAngle)*radius);
        }
        return val;
    }
}