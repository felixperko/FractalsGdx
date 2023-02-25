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

public class NumberParamInterpolation extends AbstractParamInterpolation<Number> {

    public NumberParamInterpolation(String paramUid, String paramName, String paramType, String paramContainerKey, String attributeUid, Class<? extends InterpolationFunction> interpolationFunctionClass) {
        super(paramUid, paramName, paramType, paramContainerKey, attributeUid, interpolationFunctionClass);
    }

    @Override
    protected InterpolationFunction getDefaultInterpolationFunction() {
        return new LinearInterpolationFunction();
    }

    @Override
    public List<VisTextField> addValueFieldsToTable(VisTable table, Object controlPoint, int index) {

        List<VisTextField> fields = new ArrayList<>();
        VisTextField valueField  = new VisTextField(controlPoint == null ? "" : controlPoint.toString());
        fields.add(valueField);

        table.add(valueField);

        valueField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String real = valueField.getText();
                Number newVal;
                try {
                    newVal = getNumberFactory().createNumber(real);
                } catch (NumberFormatException e){
                    return;
                }
                setControlPoint(index, newVal, null, null);
            }
        });

        return fields;
    }

    @Override
    protected void updateInterpolationFunction(boolean resetDefValues) {
//        if (!isPathBased()) {
//            return;
//        }
        List<Number> realValues = new ArrayList<>();
        List<Number> realDerivatives = new ArrayList<>();

        for (Number controlPoint : getControlPoints(true)){
            realValues.add(controlPoint);
        }
        for (Number deriv : getControlDerivatives(true)){
            realDerivatives.add(deriv);
        }
        if (resetDefValues || getDefValues(false).isEmpty())
            setDefaultDefValues(interpolationFunction);

        interpolationFunction.setValues(getNumberFactory(), getDefValues(true), realValues, realDerivatives);
    }

    @Override
    protected void resetProgressMapping(List<Number> controlPoints, List<Number> derivatives) {
        LinkedHashMap<Number, Integer> lengthPointIndexMapping = new LinkedHashMap<>();
        if (controlPoints.size() > 0) {
            Number lastPoint = controlPoints.get(0);
            lengthPointIndexMapping.put(getNumberFactory().createNumber(0.0), 0);

            for (int i = 1; i < controlPoints.size(); i++) {
                Number currentPoint = controlPoints.get(i);
                Number delta = currentPoint.copy();
                delta.sub(lastPoint);
                if (delta.toDouble() < 0)
                    delta = getNumberFactory().createNumber(-delta.toDouble());

                totalLength.add(delta);
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
    public Number getInterpolatedValue(int cpIndex0, int cpIndex1, Number progressBetween) {
        Number res = interpolationFunction.getInterpolatedValue(cpIndex0, progressBetween);
        return res;
    }

    @Override
    public Number getDefaultValue() {
        return getNumberFactory().createNumber(0);
    }
}
