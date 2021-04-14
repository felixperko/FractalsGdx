package de.felixp.fractalsgdx.animation.interpolations;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.animation.interpolationTypes.InterpolationFunction;
import de.felixp.fractalsgdx.animation.interpolationTypes.LinearInterpolationFunction;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;

public class ComplexNumberParamInterpolation extends AbstractParamInterpolation<ComplexNumber>{

    InterpolationFunction interpolationFunction2;

    public ComplexNumberParamInterpolation(String paramName, String paramType, String paramContainerKey, Class<? extends InterpolationFunction> interpolationFunctionClass) {
        super(paramName, paramType, paramContainerKey, interpolationFunctionClass);
        this.interpolationFunction2 = initInterpolationFunction(interpolationFunctionClass);
        this.interpolationFunction2.setImag(true);
    }

    @Override
    public List<VisTextField> addValueFieldsToTable(VisTable table, Object controlPoint, int index) {
        ComplexNumber val = (ComplexNumber)controlPoint;
        List<VisTextField> fields = new ArrayList<>();
        VisTextField field = new VisTextField(controlPoint == null ? "" : val.getReal().toString());
        fields.add(field);
        VisTextField field2 = new VisTextField(controlPoint == null ? "" : val.getImag().toString());
        fields.add(field2);

        field.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String real = field.getText();
                String imag = field2.getText();
                ComplexNumber newVal;
                try {
                    newVal = getNumberFactory().createComplexNumber(real, imag);
                } catch (NumberFormatException e){
                    return;
                }
                setControlPoint(index, newVal, null, null);
            }
        });
        field2.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String real = field.getText();
                String imag = field2.getText();
                ComplexNumber newVal;
                try {
                    newVal = getNumberFactory().createComplexNumber(real, imag);
                } catch (NumberFormatException e){
                    return;
                }
                setControlPoint(index, newVal, null, null);
            }
        });

        VisTable innerTable = new VisTable(true);
        innerTable.add(field);
        innerTable.add(field2);
        table.add(innerTable);

        return fields;
    }

    @Override
    protected InterpolationFunction getDefaultInterpolationFunction() {
        return new LinearInterpolationFunction();
    }

    @Override
    protected void updateInterpolationFunction(boolean resetDefValues) {
//        if (!isPathBased()) {
//            return;
//        }
        List<Number> realValues = new ArrayList<>();
        List<Number> realDerivatives = new ArrayList<>();
        List<Number> imagValues = new ArrayList<>();
        List<Number> imagDerivatives = new ArrayList<>();

        for (ComplexNumber controlPoint : getControlPoints(true)){
            realValues.add(controlPoint.getReal());
            imagValues.add(controlPoint.getImag());
        }
        for (ComplexNumber deriv : getControlDerivatives(true)){
            realDerivatives.add(deriv == null ? null : deriv.getReal());
            imagDerivatives.add(deriv == null ? null : deriv.getImag());
        }

        if (resetDefValues || getDefValues(false).isEmpty())
            setDefaultDefValues(interpolationFunction);
        List<Number> defValues = getDefValues(true);
        interpolationFunction.setValues(getNumberFactory(), defValues, realValues, realDerivatives);
        interpolationFunction2.setValues(getNumberFactory(), defValues, imagValues, imagDerivatives);
    }

    @Override
    protected void resetProgressMapping(List<ComplexNumber> controlPoints, List<ComplexNumber> derivatives) {
        if (!isAutomaticTimings())
            return;
        LinkedHashMap<Number, Integer> lengthPointIndexMapping = new LinkedHashMap<>();
        if (controlPoints.size() > 0) {
            ComplexNumber lastPoint = controlPoints.get(0);
            lengthPointIndexMapping.put(getNumberFactory().createNumber(0.0), 0);

            for (int i = 1; i < controlPoints.size(); i++) {
                ComplexNumber currentPoint = controlPoints.get(i);
                ComplexNumber delta = currentPoint.copy();
                delta.sub(lastPoint);
                Number pointLength = delta.abs();

                totalLength.add(pointLength);
                lengthPointIndexMapping.put(totalLength.copy(), i);

                lastPoint = currentPoint;
            }

            for (Map.Entry<Number, Integer> e : lengthPointIndexMapping.entrySet()) {
                Number startProgress = e.getKey();
                if (totalLength.toDouble() > 0.0)
                    startProgress.div(totalLength);
                progressPointIndexMapping.put(startProgress.toDouble(), e.getValue());
                pointIndexProgressMapping.add(startProgress.toDouble());
            }
        }
    }

    @Override
    public ComplexNumber getInterpolatedValue(int cpIndex0, int cpIndex1, Number progressBetween) {
        Number real = interpolationFunction.getInterpolatedValue(cpIndex0, progressBetween);
        Number imag = interpolationFunction2.getInterpolatedValue(cpIndex0, progressBetween);
        return getNumberFactory().createComplexNumber(real, imag);
    }

    @Override
    public ComplexNumber getDefaultValue() {
        return getNumberFactory().createComplexNumber(0,0);
    }

    @Override
    public void setNumberFactory(NumberFactory numberFactory) {
        super.setNumberFactory(numberFactory);
        if (numberFactory != null && interpolationFunction2 != null)
            interpolationFunction2.setNumberFactory(numberFactory);
    }

    @Override
    public InterpolationFunction setInterpolationFunction(Class<? extends InterpolationFunction> interpolationFunctionClass) {
        super.setInterpolationFunction(interpolationFunctionClass);
        interpolationFunction2 = initNewInterpolationFunction(interpolationFunctionClass);
        interpolationFunction2.setImag(true);
        return interpolationFunction;
    }
}
