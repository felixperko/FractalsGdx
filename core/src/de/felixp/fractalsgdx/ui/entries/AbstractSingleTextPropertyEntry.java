package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.util.NumberUtil;

abstract class AbstractSingleTextPropertyEntry extends AbstractPropertyEntry {

    InputValidator validator;

    String text;

    Double min;
    Double max;

    boolean inputDisabled = false;

    protected Class<? extends ParamSupplier> selectedSupplierClass;

    boolean showMinMax = true;
    boolean showValue = true;
    boolean showMenu = false;
    int sliderValueLabelPrecision = 2;

    public AbstractSingleTextPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, InputValidator validator, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, submitValue);
        this.validator = validator;

        List<String> hints = parameterDefinition.getHints();
        if (parameterDefinition.getHintValue("ui-element[default]:slider", false) != null)
            setCurrentControlView(VIEWNAME_SLIDERS);
        else
            setCurrentControlView(VIEWNAME_FIELDS);
    }

    List<Actor> contentFields = new ArrayList<Actor>();
    List<ChangeListener> listeners = new ArrayList<>();

    @Override
    protected void generateViews() {
        views.put(VIEWNAME_FIELDS, new EntryView() {

            protected VisValidatableTextField field;
            VisLabel label;
            VisTextButton optionButton;

            @Override
            public void addToTable(Table table) {
                label = new VisLabel(propertyName);
                field = new VisValidatableTextField(validator);
                optionButton = new VisTextButton("...");

                ParamSupplier textSupplier = paramContainer.getClientParameter(propertyName);

                if (textSupplier != null)
                    field.setText(text = textSupplier.getGeneral().toString());
                field.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (field.isInputValid())
                            text = field.getText();
                    }
                });
                for (ChangeListener listener : listeners)
                    field.addListener(listener);
                contentFields.add(field);

                table.add(label).left().padRight(3);
                if (showMenu)
                    table.add(optionButton).padRight(10);
                else
                    table.add().padRight(3);
                table.add(field).fillX().expandX().padBottom(2).row();

                setOptionButtonListener(optionButton);
            }

            @Override
            public void removeFromTable() {
                label.remove();
                field.remove();
                contentFields.remove(field);
            }
        });


        views.put(VIEWNAME_SLIDERS, new EntryView() {
            protected VisSlider slider1;
            VisLabel valueLabel1;
            VisTextField minField1;
            VisTextField maxField1;
            VisLabel label;
            VisTextButton optionButton;

            @Override
            public void addToTable(Table table) {

                label = new VisLabel(propertyName);
                optionButton = new VisTextButton("...");

                if (min == null || max == null) {
                    Double minD = parameterDefinition.getHintAttributeDoubleValue("ui-element[default]:slider", "min");
                    Double maxD = parameterDefinition.getHintAttributeDoubleValue("ui-element[default]:slider", "max");
                    if (minD == null)
                        minD = parameterDefinition.getHintAttributeDoubleValue("ui-element:slider", "min");
                    if (maxD == null)
                        maxD = parameterDefinition.getHintAttributeDoubleValue("ui-element:slider", "max");

                    min = minD != null ? (double) minD : 0;
                    max = maxD != null ? (double) maxD : (min < 1 ? 1 : min + 1);
                }

                float step = 0.001f;

                slider1 = new VisSlider(0, 1, 0.001f, false);

                ParamSupplier paramSupplier = paramContainer.getClientParameter(propertyName);

                inputDisabled = !(paramSupplier instanceof StaticParamSupplier);
                if (!inputDisabled) {
                    Object val = paramSupplier.getGeneral();
                    double parsedVal = 0;
                    if (val instanceof Number) {
                        Number n = paramSupplier.getGeneral(Number.class);
                        parsedVal = n.toDouble();
                    } else if (val instanceof  Double){
                        parsedVal = (Double) val;
                    }
                    slider1.setValue(getSliderPositionFromValue(parsedVal, min, max));
                } else {
                    slider1.setDisabled(true);
                }

                text = "" + getValueFromSlider(slider1, min, max);
                slider1.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (inputDisabled) {
                            slider1.setValue(0);
                            return;
                        }
                        double newVal = getValueFromSlider(slider1, min, max);
                        text = "" + newVal;
                        updateLabelText();
                        applyClientValue();
                        submit();
                    }

                });

                for (ChangeListener listener : listeners) {
                    slider1.addListener(listener);
                }
                contentFields.add(slider1);

                minField1 = AbstractDoubleTextPropertyEntry.createCollapsedIfInvisibleTextField(min + "");
                maxField1 = AbstractDoubleTextPropertyEntry.createCollapsedIfInvisibleTextField(max + "");
                valueLabel1 = new VisLabel() {
                    @Override
                    public float getPrefWidth() {
                        float minWidth = 50;
                        float prefWidth = super.getPrefWidth();
                        return prefWidth > minWidth ? prefWidth : minWidth;
                    }
                };
                updateVisibilities();

                minField1.setTextFieldListener(new VisTextField.TextFieldListener() {
                    @Override
                    public void keyTyped(VisTextField textField, char c) {
                        double oldVal = min;
                        String rawVal = textField.getText();
                        try {
                            if (rawVal != null && rawVal.length() > 0)
                                min = Double.parseDouble(rawVal);
                        } catch (NumberFormatException e) {//NaN
                            return;
                        }
                        updateSlider(oldVal, max);
                    }
                });
                maxField1.setTextFieldListener(new VisTextField.TextFieldListener() {
                    @Override
                    public void keyTyped(VisTextField textField, char c) {
                        double oldVal = max;
                        String rawVal = textField.getText();
                        try {
                            if (rawVal != null && rawVal.length() > 0)
                                max = Double.parseDouble(rawVal);
                        } catch (NumberFormatException e) {//NaN
                            return;
                        }
                        updateSlider(min, oldVal);
                    }
                });
                updateLabelText();
                //update disabled in case of invalid range
                updateSlider(min, max);

                VisTable innerTable1 = new VisTable();
                innerTable1.add(minField1);
                innerTable1.add(slider1);
                innerTable1.add(valueLabel1).minWidth(70).padRight(3);
                innerTable1.add(maxField1);

                table.add(label).left().padRight(3);
                if (showMenu)
                    table.add(optionButton).padRight(10);
                else
                    table.add().padRight(3);
                table.add(innerTable1).padBottom(2).row();
//                table.add();
//                table.add();
//                table.add(innerTable2).padBottom(2).row();

                setOptionButtonListener(optionButton);
            }

            private void updateVisibilities() {
                minField1.setVisible(showMinMax);
                minField1.invalidate();
                maxField1.setVisible(showMinMax);
                maxField1.invalidate();
                valueLabel1.setVisible(showValue);
                valueLabel1.invalidate();
            }

            private void updateLabelText(){
                float sliderVal1 = (float) getValueFromSlider(slider1, min, max);
                valueLabel1.setText(parseLabelValueText(sliderVal1));
            }

            private String parseLabelValueText(double value) {
                if (inputDisabled){
                    return "var";
                }
                String labelText = String.format("%."+sliderValueLabelPrecision+"f", NumberUtil.getRoundedDouble(value, 3));
                if (!labelText.startsWith("-"))
                    labelText = " "+labelText;
                return labelText;
            }

            private void updateSlider(double oldMin1, double oldMax1){
                if (max <= min){
                    slider1.setDisabled(true);
                    return;
                } else {
                    slider1.setDisabled(false);
                }
                double sliderVal1 = getValueFromSlider(slider1, oldMin1, oldMax1);
                slider1.setValue(getSliderPositionFromValue(sliderVal1, min, max));
            }

            @Override
            public void removeFromTable() {
                label.remove();
                optionButton.remove();
                slider1.remove();
                minField1.remove();
                maxField1.remove();
                valueLabel1.remove();
            }
        });
    }

    @Override
    protected void submit() {
        if (submitValue)
            super.submit();
    }

    protected double getValueFromSlider(VisSlider slider, double min, double max) {
        return slider.getValue()*(max-min) + min;
    }

    private float getSliderPositionFromValue(double value, double min, double max) {
        float sliderPos = (float) ((value - min) / (max - min));
        return sliderPos <= 0f ? 0f : (sliderPos >= 1f ? 1f : sliderPos);
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
