package de.felixp.fractalsgdx.interpolation;

import com.badlogic.gdx.math.Interpolation;

import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;

public class LinearNumberParameterInterpolation extends AbstractSimpleParameterInterpolation<Number> {

    NumberFactory nf;

    String startValueString;
    String endValueString;

    Number startValue;
    Number endValue;

    public LinearNumberParameterInterpolation(String parameterName, double timeFactor, String startValue, String endValue) {
        super(parameterName, timeFactor);
        startValueString = startValue;
        endValueString = endValue;
    }

    @Override
    public Number getInterpolatedValue(double progress, NumberFactory nf) {
        if (this.nf == null || !this.nf.equals(nf) || startValue == null || endValue == null){
            this.nf = nf;
            startValue = nf.createNumber(startValueString);
            endValue = nf.createNumber(endValueString);
        }
        this.nf = nf;
        Number res = nf.createNumber(scaleProgress(progress));
        Number diff = endValue.copy();
        diff.sub(startValue);
        res.mult(diff);
        res.add(startValue);
        return res;
    }

    public Number getStartValue(NumberFactory nf) {
        if (nf == null)
            nf = this.nf;
        if (startValue == null && nf != null && startValueString != null)
            startValue = nf.createNumber(startValueString);
        return startValue;
    }

    public void setStartValue(Number startValue) {
        this.startValue = startValue;
    }

    public void setStartValue(String startValueString) {
        this.startValueString = startValueString;
        this.startValue = null;
    }

    public Number getEndValue(NumberFactory nf) {
        if (nf == null)
            nf = this.nf;
        if (endValue == null && nf != null && endValueString != null)
            endValue = nf.createNumber(endValueString);
        return endValue;
    }

    public void setEndValue(Number endValue) {
        this.endValue = endValue;
    }

    public void setEndValue(String endValueString) {
        this.endValueString = endValueString;
        this.endValue = null;
    }
}
