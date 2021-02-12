package de.felixp.fractalsgdx.animation.interpolationTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;

public class CardioidInterpolationFunction extends AbstractInterpolationFunction{

    @Override
    public boolean isPathBased() {
        return false;
    }

    @Override
    public Number getInterpolatedValue(int currentStartIndex, Number progressBetween) {
        float separation = 0.25f;

        double timeAngle = (progressBetween.toDouble() * Math.PI * 2);
        double cardioidScale = defValues.get(0).toDouble();
        double shiftReal = 0.0;
        double shiftImag = 0.0;

        shiftReal += cardioidScale*separation;

        Number val = null;
        if (isReal()){
//            Number n1 = getNumber("1");
//            Number n2 = getNumber("2");
//            Number nAngle = numberFactory.createNumber(timeAngle);
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
    public List<List<String>> getDefValueNames() {
        List<List<String>> list = new ArrayList<>();
        List<String> sublist1 = new ArrayList<>();
        sublist1.add("cardioid scale");
        list.add(sublist1);
        return list;
    }

    @Override
    public List<List<Number>> getDefValueDefaults() {
        List<List<Number>> list = new ArrayList<>();
        List<Number> sublist1 = new ArrayList<>();
        sublist1.add(numberFactory.createNumber(1));
        list.add(sublist1);
        return list;
    }
}
