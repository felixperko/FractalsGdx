package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;

import java.util.ArrayList;
import java.util.List;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.util.NumberUtil;

@Deprecated
public class DoubleSliderPropertyEntry extends AbstractPropertyEntry {

    public static final String NAME_LIST_TEXTFIELD = "LIST_TEXTFIELD";

    //TODO arbitrary precision
    double value;
    double min;
    double max;

    List<Actor> contentFields = new ArrayList<Actor>();
    List<ChangeListener> listeners = new ArrayList<>();

    public DoubleSliderPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, submitValue);
    }

    @Override
    protected void generateViews() {

        views.put(VIEW_LIST, new EntryView() {

            @Override
            public void readFields() {
            }

            protected VisLabel label;
            protected VisTable controlTable;
            protected VisSlider slider;
            protected VisTextField minField;
            protected VisTextField maxField;
//            protected VisLabel minusOffsetLabel; //reserves space for the minus sign to prevent list rescale
            protected VisLabel valueLabel;

            boolean showMinMax = true;
            boolean showValue = true;

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

                float step = 0.001f;
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
//                        minusOffsetLabel.invalidate();
                        updateSideLabelText();
                    }
                });

                for (ChangeListener listener : listeners)
                    slider.addListener(listener);
                contentFields.add(slider);

                minField = AbstractDoubleTextPropertyEntry.createCollapsedIfInvisibleTextField(min+"");
                maxField = AbstractDoubleTextPropertyEntry.createCollapsedIfInvisibleTextField(max+"");
                valueLabel = new VisLabel();
                updateVisibilities();

//                minusOffsetLabel = new VisLabel("-"){
//                    @Override
//                    public float getPrefWidth() {
//                        if (!valueLabel.isVisible())
//                            return 0;
//
//                        return slider.getValue() < 0 ? super.getPrefWidth() : 0;
//                    }
//                };
//                minusOffsetLabel.setVisible(false);

                minField.setTextFieldListener(new VisTextField.TextFieldListener() {
                    @Override
                    public void keyTyped(VisTextField textField, char c) {
                        String rawVal = textField.getText();
                        try {
                            if (rawVal != null && rawVal.length() > 0)
                                min = Double.parseDouble(rawVal);
                        } catch (NumberFormatException e){//NaN
                            return;
                        }
                        updateSlider();
                    }
                });
                maxField.setTextFieldListener(new VisTextField.TextFieldListener() {
                    @Override
                    public void keyTyped(VisTextField textField, char c) {
                        String rawVal = textField.getText();
                        try {
                            if (rawVal != null && rawVal.length() > 0)
                                max = Double.parseDouble(rawVal);
                        } catch (NumberFormatException e){//NaN
                            return;
                        }
                        updateSlider();
                    }
                });
                updateSideLabelText();

                controlTable = new VisTable();
                controlTable.add(minField).padRight(2);
                controlTable.add(slider).padRight(2);
//                controlTable.add(minusOffsetLabel);
                controlTable.add(valueLabel).minWidth(70).expandX().fillX().right().padRight(2);
                controlTable.add(maxField).padRight(2);

                table.add(label).left().padRight(3);
                table.add();
                table.add(controlTable).expandX().fillX().padBottom(2).row();
            }

            private void updateVisibilities() {
                minField.setVisible(showMinMax);
                minField.invalidate();
                maxField.setVisible(showMinMax);
                maxField.invalidate();
                valueLabel.setVisible(showValue);
                valueLabel.invalidate();
            }

            /**
             * update the side label in slider mode
             */
            private void updateSideLabelText(){
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

        views.put(NAME_LIST_TEXTFIELD, new EntryView() {

            protected VisValidatableTextField field;
            VisLabel label;

            @Override
            public void readFields() {
            }

            @Override
            public void addToTable(Table table) {
                label = new VisLabel(propertyName);
                field = new VisValidatableTextField(Validators.FLOATS);

                ParamSupplier textSupplier = paramContainer.getClientParameter(propertyName);

                if (textSupplier != null) {
                    //TODO arbitrary precision
                    value = textSupplier.getGeneral(Double.class);
                    field.setText(value+"");
                }
                field.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (field.isInputValid()) {
                            //TODO arbitrary precision
                            value = Double.parseDouble(field.getText());
                            field.setText(value+"");
                        }
                    }
                });
                for (ChangeListener listener : listeners)
                    field.addListener(listener);
                contentFields.add(field);

                table.add(label).left().padRight(3);
                table.add().pad(3);
                table.add(field).fillX().expandX().padBottom(2).row();
            }

            @Override
            public void removeFromTable() {
                label.remove();
                field.remove();
                contentFields.remove(field);
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

    @Override
    protected boolean checkValue(Object valueObj) {
        return valueObj instanceof Double;
    }

    @Override
    protected void setCheckedValue(Object newValue) {
        double val = (Double)newValue;
        if (val >= min && value <= max){
            value = val;
        }
    }

    @Override
    protected Object getDefaultObject() {
        return null;
    }
}
