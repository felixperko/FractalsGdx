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
        double radius = defValues.get(0).toDouble();
        double shiftReal = defValues.get(1).toDouble();
        double shiftImag = defValues.get(2).toDouble();

        Number val = null;
        if (isReal()){
            val = getNumberFactory().createNumber(Math.cos(timeAngle)*radius + shiftReal);
        }
        else if (isImag()){
            val = getNumberFactory().createNumber(Math.sin(timeAngle)*radius + shiftImag);
        }
        return val;
    }

    @Override
    public String[][] getDefValueNames() {
        return new String[][]{{
                "radius",
                "shift real",
                "shift imag"
        }};
    }

    @Override
    public Number[][] getDefValueDefaults() {
        return new Number[][]{{
                getNumber("0.5"),
                getNumber("0"),
                getNumber("0")
        }};
    }
}