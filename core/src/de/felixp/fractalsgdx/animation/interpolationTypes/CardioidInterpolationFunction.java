package de.felixp.fractalsgdx.animation.interpolationTypes;

import java.util.ArrayList;
import java.util.List;

import de.felixperko.fractals.system.numbers.Number;

public class CardioidInterpolationFunction extends AbstractInterpolationFunction{

    @Override
    public boolean isPathBased() {
        return false;
    }

    @Override
    public Number getInterpolatedValue(int currentStartIndex, Number progressBetween) {

        double timeAngle = (progressBetween.toDouble() * Math.PI * 2);
        double cardioidScale = defValues.get(0).toDouble();
        double separation = 0.25;
        double shiftReal = defValues.get(1).toDouble();
        double shiftImag = defValues.get(2).toDouble();

        shiftReal += cardioidScale*separation;

        Number val = null;
        if (isReal()){
//            Number n1 = getNumber("1");
//            Number n2 = getNumber("2");
//            Number nAngle = nf.createNumber(timeAngle);
//            Number nCardioidScale = defValues.get(0);
//            Number nShiftReal = defValues.get(1);
//            Number nSeparation = defValues.get(3);
//            val = n1.copy().sub(nAngle.cos()).mult(nAngle.cos()).mult(n2).mult(nSeparation).mult(nCardioidScale).add(nShiftReal);
            val = getNumberFactory().createNumber(((1-Math.cos(timeAngle)) * Math.cos(timeAngle) * 2 * separation) * cardioidScale + shiftReal);
        }
        else if (isImag()){
            val = getNumberFactory().createNumber(((1-Math.cos(timeAngle)) * Math.sin(timeAngle) * 2 * separation) * cardioidScale + shiftImag);
        }
        return val;
    }

    @Override
    public String[][] getDefValueNames() {
        return new String[][]{{
            "cardioid scale",
//                "separation",
                "shift real",
                "shift imag"
        }};
    }

    @Override
    public Number[][] getDefValueDefaults() {
        return new Number[][]{{
            getNumber("1"),
//            getNumber("0.25"),
            getNumber("0"),
            getNumber("0")
        }};
    }
}
