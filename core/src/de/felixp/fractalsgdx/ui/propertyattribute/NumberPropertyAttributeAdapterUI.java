package de.felixp.fractalsgdx.ui.propertyattribute;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixp.fractalsgdx.ui.actors.TabTraversableTextField;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;

public class NumberPropertyAttributeAdapterUI extends AbstractPropertyAttributeAdapterUI<Number> {

    String name;
    NumberFactory nf;
    Number startVal;
    Number minValue;
    Number maxValue;

    boolean updatingValue = false;

    TabTraversableTextField valueField;
    VisSlider slider;

    public NumberPropertyAttributeAdapterUI(String name, NumberFactory numberFactory, Number startVal, Number minValue, Number maxValue){
        this.name = name;
        this.nf = numberFactory;
        this.startVal = startVal;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public Actor addToTable(Table table) {

        VisTable innerTable = new VisTable(true);

        VisLabel nameLbl = new VisLabel(name);

        slider = new VisSlider(0f, 1f, 0.01f, false);

        valueField = new TabTraversableTextField(startVal.toString());
        registerField(valueField);
        valueField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    Number newVal = nf.createNumber(valueField.getText());
                    valueChanged(newVal, minValue, maxValue);
                    if (isSliderEnabled()){
                        updatingValue = true;
                        slider.setValue(getRawSliderValue(newVal));
                        updatingValue = false;
                    }
                } catch (NumberFormatException e) {

                }
            }
        });

        innerTable.add(nameLbl).left();
        innerTable.add(valueField).left();

        if (isSliderEnabled()) {
            TabTraversableTextField minField = new TabTraversableTextField(minValue.toString());
            TabTraversableTextField maxField = new TabTraversableTextField(maxValue.toString());

            minField.setPrefWidth(50);
            maxField.setPrefWidth(50);

            minField.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    try {
                        Number newMin = nf.createNumber(minField.getText());
                        minValue = newMin;
                        Number value = nf.createNumber(valueField.getText());
                        valueChanged(value, minValue, maxValue);
                        slider.setValue(getRawSliderValue(value));
                        minField.setInputValid(true);
                    } catch (NumberFormatException e){
                        minField.setInputValid(false);
                    }
                }
            });
            maxField.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    try {
                        Number newMax = nf.createNumber(maxField.getText());
                        maxValue = newMax;
                        Number value = nf.createNumber(valueField.getText());
                        valueChanged(value, minValue, maxValue);
                        slider.setValue(getRawSliderValue(value));
                        maxField.setInputValid(true);
                    } catch (NumberFormatException e){
                        maxField.setInputValid(false);
                    }
                }
            });

            slider.setValue(getRawSliderValue(startVal));
            slider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    if (!updatingValue) {
                        Number newVal = getScaledSliderValue(slider);
                        valueField.setText(newVal + "");
                    }
                }
            });

            innerTable.add(minField);
            innerTable.add(slider);
            innerTable.add(maxField);
        }

        table.add(innerTable).left().row();

        return innerTable;
    }

    private boolean isSliderEnabled() {
        return minValue != null && maxValue != null && maxValue.toDouble() > minValue.toDouble();
    }

    public Number getScaledSliderValue(VisSlider slider){

        Number res = maxValue.copy();
        res.sub(minValue); //delta

        res.mult(nf.createNumber(slider.getValue()));
        res.add(minValue);

        return res;
    }

    public float getRawSliderValue(Number scaledValue){

        Number delta = maxValue.copy();
        delta.sub(minValue);

        Number val = scaledValue.copy();
        val.sub(minValue);
        val.div(delta);

        float res = (float) val.toDouble();
        res = Math.max(0f, Math.min(res, 1f));

        return res;
    }

    @Override
    public void valueChanged(Number newVal, Number min, Number max) {

    }

    @Override
    public void addListenerToFields(EventListener listener) {
        slider.addListener(listener);
        valueField.addListener(listener);
    }
}
