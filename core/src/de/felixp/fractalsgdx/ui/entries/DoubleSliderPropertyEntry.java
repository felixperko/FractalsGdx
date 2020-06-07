package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;

import java.util.ArrayList;
import java.util.List;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.util.NumberUtil;

public class DoubleSliderPropertyEntry extends AbstractPropertyEntry {

    double value;

    double min;
    double max;

    List<Actor> contentFields = new ArrayList<Actor>();
    List<ChangeListener> listeners = new ArrayList<>();

    public DoubleSliderPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParameterDefinition parameterDefinition) {
        super(node, paramContainer, parameterDefinition);
    }

    @Override
    protected void generateViews() {

        views.put(VIEW_LIST, new EntryView() {

            protected VisLabel label;
            protected VisTable controlTable;
            protected VisSlider slider;
            protected VisTextField minField;
            protected VisTextField maxField;
            protected VisLabel valueLabel;

            @Override
            public void addToTable(Table table) {
                label = new VisLabel(propertyName);

                List<String> hints = parameterDefinition.getHints();

                Double minD = parameterDefinition.getHintAttributeDoubleValue("ui-element[default]:slider", "min");
                Double maxD = parameterDefinition.getHintAttributeDoubleValue("ui-element[default]:slider", "max");
                if (minD == null)
                    minD = parameterDefinition.getHintAttributeDoubleValue("ui-element:slider", "min");
                if (maxD == null)
                    maxD = parameterDefinition.getHintAttributeDoubleValue("ui-element:slider", "max");

                min = minD != null ? (double)minD : 0;
                max = maxD != null ? (double)maxD : (min < 1 ? 1 : min+1);

                float step = 0.005f;
                slider = new VisSlider(0, 1, step, false);

                ParamSupplier supplier = paramContainer.getClientParameter(propertyName);
                if (supplier != null) {
                    value = (double) supplier.getGeneral(Double.class);
                    slider.setValue((float)((value-min)/(max-min)));
                }

                slider.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        value = slider.getValue()*(max-min)+min;
                        updateLabelText();
                    }
                });

                for (ChangeListener listener : listeners)
                    slider.addListener(listener);
                contentFields.add(slider);

                minField = new VisTextField(min+"");
                maxField = new VisTextField(max+"");
                minField.setTextFieldListener(new VisTextField.TextFieldListener() {
                    @Override
                    public void keyTyped(VisTextField textField, char c) {
                        min = Double.parseDouble(textField.getText());
                        updateSlider();
                    }
                });
                maxField.setTextFieldListener(new VisTextField.TextFieldListener() {
                    @Override
                    public void keyTyped(VisTextField textField, char c) {
                        max = Double.parseDouble(textField.getText());
                        updateSlider();
                    }
                });
                valueLabel = new VisLabel();
                updateLabelText();

                controlTable = new VisTable();
                controlTable.add(minField).padRight(2);
                controlTable.add(slider).padRight(2);
                controlTable.add(valueLabel).padRight(2);
                controlTable.add(maxField).padRight(2);

                table.add(label).left().padRight(3);
                table.add();
                table.add(controlTable).expandX().fillX().padBottom(2).row();
            }

            private void updateLabelText(){
                String labelText = String.format("%.2f", NumberUtil.getRoundedDouble(value, 3));
                if (!labelText.startsWith("-"))
                    labelText = " "+labelText;
                valueLabel.setText(labelText);
            }

            private void updateSlider(){
                if (max <= min){
                    slider.setDisabled(true);
                    return;
                } else {
                    slider.setDisabled(false);
                }
                float sliderVal = (float)((value-min)/(max-min));
                slider.setValue(sliderVal < 0 ? 0 : (sliderVal > 1 ? 1 : sliderVal));
            }

            @Override
            public void removeFromTable() {
                label.remove();
                minField.remove();
                slider.remove();
                maxField.remove();
                valueLabel.remove();

                controlTable.remove();
                contentFields.remove(slider);
            }
        });
    }

    @Override
    public ParamSupplier getSupplier() {
        return new StaticParamSupplier(propertyName, (Double)value);
    }

    @Override
    public void addChangeListener(ChangeListener changeListener) {
        listeners.add(changeListener);
        for (Actor field : contentFields)
            field.addListener(changeListener);
    }

    @Override
    public void removeChangeListener(ChangeListener changeListener) {
        listeners.remove(changeListener);
        for (Actor field : contentFields)
            field.removeListener(changeListener);
    }
}
