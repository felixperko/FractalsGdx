package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.Focusable;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixp.fractalsgdx.ui.actors.TabTraversableTextField;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.util.NumberUtil;

public abstract class AbstractDoubleTextPropertyEntry extends AbstractPropertyEntry {

    static float RANGE_TEXTFIELD_SCALE = 0.5f;

    public static VisTextField createCollapsedIfInvisibleTextField(String text) {
        return createCollapsedIfInvisibleTextField(text, RANGE_TEXTFIELD_SCALE);
    }

    public static VisTextField createCollapsedIfInvisibleTextField(String text, float scale) {
        return new VisTextField(text) {
            @Override
            public float getPrefWidth() {
                if (!isVisible())
                    return 0;
                return super.getPrefWidth() * scale;
            }
        };
    }

    boolean showMinMax = true;
    boolean showValue = true;
    int sliderValueLabelPrecision = 2;

    InputValidator validator1;
    InputValidator validator2;

    boolean inputDisabled = false;

    String text1;
    String text2;

    Double min1;
    Double max1;
    Double min2;
    Double max2;

    int selectedIntex = 0;

    List<Actor> contentFields = new ArrayList<Actor>();
    List<ChangeListener> listeners = new ArrayList<>();

    public AbstractDoubleTextPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, InputValidator validator1, InputValidator validator2, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, submitValue);

        setCurrentControlView(VIEWNAME_FIELDS, false);

        this.validator1 = validator1;
        this.validator2 = validator2;

    }

    public abstract String getParameterValue1(StaticParamSupplier paramSupplier);
    public abstract String getParameterValue2(StaticParamSupplier paramSupplier);

    @Override
    protected void setCheckedValue(Object newValue) {
        for (EntryView view : views.values()){
            view.applyValue(newValue);
        }
    }

    @Override
    protected void generateViews() {
        views.put(VIEWNAME_FIELDS, new EntryView() {

            protected TabTraversableTextField field1;
            protected TabTraversableTextField field2;
            VisLabel label;
            VisTextButton optionButton;

            @Override
            public void applyValue(Object value) {
                ComplexNumber num = (ComplexNumber)value;
                if (field1 != null && field2 != null) {
                    field1.setText(num.getReal().toString());
                    field2.setText(num.getImag().toString());
                }
            }

            @Override
            public void addToTable(Table table) {

                label = new VisLabel(propertyName);


                optionButton = new VisTextButton("...");

                field1 = new TabTraversableTextField(validator1);
                addSubmitListener(field1);
                field2 = new TabTraversableTextField(validator2);
                addSubmitListener(field2);


                ParamSupplier paramSupplier = paramContainer.getClientParameter(propertyName);

                field1.setTraversalPaused(!(paramSupplier instanceof StaticParamSupplier));
                field2.setTraversalPaused(!(paramSupplier instanceof StaticParamSupplier));
                traversableGroup.addField(field1);
                traversableGroup.addField(field2);

                inputDisabled = !(paramSupplier instanceof StaticParamSupplier);
                if (!inputDisabled) {

                    field1.setText(text1 = getParameterValue1((StaticParamSupplier) paramSupplier));
                    field2.setText(text2 = getParameterValue2((StaticParamSupplier) paramSupplier));

                    field1.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            text1 = field1.getText();
                        }
                    });
                    field2.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            text2 = field2.getText();
                        }
                    });

                    for (ChangeListener listener : listeners) {
                        field1.addListener(listener);
                        field2.addListener(listener);
                    }
                }

                contentFields.add(field1);
                contentFields.add(field2);

                table.add(label).left().padRight(3);
                table.add(optionButton).padRight(10);
                if (!inputDisabled)
                    table.add(field1).fillX().expandX().row();
                else
                    table.add("map x").fillX().expandX().row();
                table.add();
                table.add();
                if (!inputDisabled)
                    table.add(field2).fillX().expandX().padBottom(2).row();
                else
                    table.add("map y * i").fillX().expandX().padBottom(2).row();

                setOptionButtonListener(optionButton);
            }

            @Override
            public void removeFromTable() {
                traversableGroup.removeField(field1);
                traversableGroup.removeField(field2);
                contentFields.remove(field1);
                contentFields.remove(field2);
                field1.remove();
                field2.remove();
                label.remove();
                optionButton.remove();
            }
        });
        views.put(VIEWNAME_SLIDERS, new EntryView() {
            protected VisSlider slider1;
            protected VisSlider slider2;
            VisLabel valueLabel1;
            VisLabel valueLabel2;
            VisTextField minField1, minField2;
            VisTextField maxField1, maxField2;
            VisLabel label;
            VisTextButton optionButton;

            @Override
            public void applyValue(Object value) {
                ComplexNumber num = (ComplexNumber)value;
                if (slider1 != null && slider2 != null) {

                    double min1 = Double.parseDouble(minField1.getText());
                    double max1 = Double.parseDouble(maxField1.getText());
                    double min2 = Double.parseDouble(minField2.getText());
                    double max2 = Double.parseDouble(maxField2.getText());
                    slider1.setValue(getSliderPositionFromValue(num.realDouble(), min1, max1));
                    slider2.setValue(getSliderPositionFromValue(num.imagDouble(), min2, max2));
                }
            }

            @Override
            public void addToTable(Table table) {

                label = new VisLabel(propertyName);
                optionButton = new VisTextButton("...");

                min1 = getState().getMin();
                min2 = getState().getMin2();
                max1 = getState().getMax();
                max2 = getState().getMax2();

                if (min1 == null || max1 == null || min2 == null || max2 == null) {
                    Double minD = parameterDefinition.getHintAttributeDoubleValue("ui-element[default]:slider", "min");
                    Double maxD = parameterDefinition.getHintAttributeDoubleValue("ui-element[default]:slider", "max");
                    if (minD == null)
                        minD = parameterDefinition.getHintAttributeDoubleValue("ui-element:slider", "min");
                    if (maxD == null)
                        maxD = parameterDefinition.getHintAttributeDoubleValue("ui-element:slider", "max");

                    min1 = minD != null ? (double) minD : 0;
                    max1 = maxD != null ? (double) maxD : (min1 < 1 ? 1 : min1 + 1);
                    min2 = minD != null ? (double) minD : 0;
                    max2 = maxD != null ? (double) maxD : (min2 < 1 ? 1 : min2 + 1);
                }

                float step = 0.001f;

                slider1 = new TestSlider(0, 1, 0.001f, false);
                slider2 = new TestSlider(0, 1, 0.001f, false);

                ParamSupplier paramSupplier = paramContainer.getClientParameter(propertyName);

                inputDisabled = !(paramSupplier instanceof StaticParamSupplier);
                if (!inputDisabled) {
                    ComplexNumber cn = paramSupplier.getGeneral(ComplexNumber.class);
                    slider1.setValue(getSliderPositionFromValue(cn.realDouble(), min1, max1));
                    slider2.setValue(getSliderPositionFromValue(cn.imagDouble(), min2, max2));
                }
                else{
                    slider1.setDisabled(true);
                    slider2.setDisabled(true);
                }

                slider1.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (inputDisabled) {
                            slider1.setValue(0);
                            return;
                        }
                        double newVal = getValueFromSlider(slider1, min1, max1);
                        text1 = ""+newVal;
                        updateLabelText();
                        applyClientValue();
                        submit();
                        ((MainStage) FractalsGdxMain.stage).resetKeyboardFocus();
                    }

                });
                slider2.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (inputDisabled) {
                            slider2.setValue(0);
                            return;
                        }
                        double newVal = getValueFromSlider(slider2, min2, max2);
                        text2 = ""+newVal;
                        updateLabelText();
                        applyClientValue();
                        submit();
                        ((MainStage)FractalsGdxMain.stage).resetKeyboardFocus();
                    }
                });

                for (ChangeListener listener : listeners) {
                    slider1.addListener(listener);
                    slider2.addListener(listener);
                }
                contentFields.add(slider1);
                contentFields.add(slider2);

                minField1 = createCollapsedIfInvisibleTextField(min1 + "");
                maxField1 = createCollapsedIfInvisibleTextField(max1 + "");
                minField2 = createCollapsedIfInvisibleTextField(min2 + "");
                maxField2 = createCollapsedIfInvisibleTextField(max2 + "");
                valueLabel1 = new VisLabel(){
                    @Override
                    public float getPrefWidth() {
                        float minWidth = 50;
                        float prefWidth = super.getPrefWidth();
                        return prefWidth > minWidth ? prefWidth : minWidth;
                    }
                };
                valueLabel2 = new VisLabel(){
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
                        double oldVal = min1;
                        String rawVal = textField.getText();
                        try {
                            if (rawVal != null && rawVal.length() > 0)
                                getState().setMin(min1 = Double.parseDouble(rawVal));
                        } catch (NumberFormatException e){//NaN
                            return;
                        }
                        updateSlider(oldVal, max1, min2, max2);
                    }
                });
                maxField1.setTextFieldListener(new VisTextField.TextFieldListener() {
                    @Override
                    public void keyTyped(VisTextField textField, char c) {
                        double oldVal = max1;
                        String rawVal = textField.getText();
                        try {
                            if (rawVal != null && rawVal.length() > 0)
                                getState().setMax(max1 = Double.parseDouble(rawVal));
                        } catch (NumberFormatException e){//NaN
                            return;
                        }
                        updateSlider(min1, oldVal, min2, max2);
                    }
                });
                minField2.setTextFieldListener(new VisTextField.TextFieldListener() {
                    @Override
                    public void keyTyped(VisTextField textField, char c) {
                        double oldVal = min2;
                        String rawVal = textField.getText();
                        try {
                            if (rawVal != null && rawVal.length() > 0)
                                getState().setMin2(min2 = Double.parseDouble(rawVal));
                        } catch (NumberFormatException e){//NaN
                            return;
                        }
                        updateSlider(min1, max1, oldVal, max2);
                    }
                });
                maxField2.setTextFieldListener(new VisTextField.TextFieldListener() {
                    @Override
                    public void keyTyped(VisTextField textField, char c) {
                        double oldVal = max2;
                        String rawVal = textField.getText();
                        try {
                            if (rawVal != null && rawVal.length() > 0)
                                getState().setMax2(max2 = Double.parseDouble(rawVal));
                        } catch (NumberFormatException e){//NaN
                            return;
                        }
                        updateSlider(min1, max1, min2, oldVal);
                    }
                });
                updateLabelText();
                //update disabled in case of invalid range
                updateSlider(min1, max1, min2, max2);

                boolean limitsVisible = parentPropertyList == null ? true : parentPropertyList.isSliderLimitsVisible();

                VisTable innerTable1 = new VisTable();
                if (limitsVisible)
                    innerTable1.add(minField1);
                innerTable1.add(slider1);
                innerTable1.add(valueLabel1).minWidth(70).padRight(3);
                if (limitsVisible)
                    innerTable1.add(maxField1).row();

//                VisTable innerTable2 = new VisTable(true);
                if (limitsVisible)
                    innerTable1.add(minField2);
                innerTable1.add(slider2);
                innerTable1.add(valueLabel2).minWidth(70).padRight(3);
                if (limitsVisible)
                    innerTable1.add(maxField2);

                table.add(label).left().padRight(3);
                table.add(optionButton).padRight(10);
                table.add(innerTable1).padBottom(2).row();
//                table.add();
//                table.add();
//                table.add(innerTable2).padBottom(2).row();

                setOptionButtonListener(optionButton);
            }

            protected double getValueFromSlider(VisSlider slider, double min, double max) {
                return slider.getValue()*(max-min) + min;
            }

            private float getSliderPositionFromValue(double value, double min, double max) {
                float sliderPos = (float) ((value - min) / (max - min));
                return sliderPos <= 0f ? 0f : (sliderPos >= 1f ? 1f : sliderPos);
            }

            private void updateVisibilities() {
                minField1.setVisible(showMinMax);
                minField1.invalidate();
                maxField1.setVisible(showMinMax);
                maxField1.invalidate();
                minField2.setVisible(showMinMax);
                minField2.invalidate();
                maxField2.setVisible(showMinMax);
                maxField2.invalidate();
                valueLabel1.setVisible(showValue);
                valueLabel1.invalidate();
                valueLabel2.setVisible(showValue);
                valueLabel2.invalidate();
            }

            private void updateLabelText(){
                float sliderVal1 = (float) getValueFromSlider(slider1, min1, max1);
                float sliderVal2 = (float) getValueFromSlider(slider2, min2, max2);
                valueLabel1.setText(parseLabelValueText(sliderVal1));
                valueLabel2.setText(parseLabelValueText(sliderVal2));
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

            private void updateSlider(double oldMin1, double oldMax1, double oldMin2, double oldMax2){
                if (max1 <= min1 || max2 <= min2){
                    slider1.setDisabled(true);
                    slider2.setDisabled(true);
                    return;
                } else {
                    slider1.setDisabled(false);
                    slider2.setDisabled(false);
                }
                double sliderVal1 = getValueFromSlider(slider1, oldMin1, oldMax1);
                double sliderVal2 = getValueFromSlider(slider2, oldMin2, oldMax2);
                slider1.setValue(getSliderPositionFromValue(sliderVal1, min1, max1));
                slider2.setValue(getSliderPositionFromValue(sliderVal2, min2, max2));
            }

            @Override
            public void removeFromTable() {
                label.remove();
                optionButton.remove();
                contentFields.remove(slider1);
                contentFields.remove(slider2);
                slider1.remove();
                slider2.remove();
                minField1.remove();
                maxField1.remove();
                minField2.remove();
                maxField2.remove();
                valueLabel1.remove();
                valueLabel2.remove();
            }
        });
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

    private class TestSlider extends VisSlider implements Focusable{
        public TestSlider(float min, float max, float stepSize, boolean vertical) {
            super(min, max, stepSize, vertical);
        }

        @Override
        public void focusLost() {
            System.err.println("AbstractPropertyEntry TestSlider focus lost");
        }

        @Override
        public void focusGained() {
            System.err.println("AbstractPropertyEntry TestSlider focus gained");
        }
    }
}
