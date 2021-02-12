package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.kotcrab.vis.ui.Focusable;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;
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
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.util.NumberUtil;

public abstract class AbstractDoubleTextPropertyEntry extends AbstractPropertyEntry {

    public static final String VIEWNAME_SLIDERS = "SLIDERS";
    public static final String VIEWNAME_FIELDS = "FIELDS";

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

    protected Class<? extends ParamSupplier> selectedSupplierClass;
    int selectedIntex = 0;
    List<Class<? extends ParamSupplier>> possibleSupplierClasses;

    List<Actor> contentFields = new ArrayList<Actor>();
    List<ChangeListener> listeners = new ArrayList<>();

    public AbstractDoubleTextPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, InputValidator validator1, InputValidator validator2) {
        super(node, paramContainer, parameterDefinition);

        setPrefListView(VIEWNAME_FIELDS);

        this.validator1 = validator1;
        this.validator2 = validator2;

        this.possibleSupplierClasses = parameterDefinition.getPossibleClasses();

        ParamSupplier param = paramContainer.getClientParameter(parameterDefinition.getName());
        if (param != null && possibleSupplierClasses.contains(param.getClass()))
            this.selectedSupplierClass = param.getClass();
        else
            this.selectedSupplierClass = possibleSupplierClasses.get(0);

    }

    public abstract String getParameterValue1(StaticParamSupplier paramSupplier);
    public abstract String getParameterValue2(StaticParamSupplier paramSupplier);

    @Override
    protected void generateViews() {
        views.put(VIEWNAME_FIELDS, new EntryView() {

            protected VisValidatableTextField field1;
            protected VisValidatableTextField field2;
            VisLabel label;
            VisTextButton optionButton;

            @Override
            public void addToTable(Table table) {

                label = new VisLabel(propertyName);

                optionButton = new VisTextButton("...");

                field1 = new VisValidatableTextField(validator1);
                field2 = new VisValidatableTextField(validator2);

                ParamSupplier paramSupplier = paramContainer.getClientParameter(propertyName);

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
                    table.add("variable").fillX().expandX().row();
                table.add();
                table.add();
                if (!inputDisabled)
                    table.add(field2).fillX().expandX().padBottom(2).row();
                else
                    table.add("variable").fillX().expandX().padBottom(2).row();

                setOptionButtonListener(optionButton);
            }

            @Override
            public void removeFromTable() {
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
            public void addToTable(Table table) {

                label = new VisLabel(propertyName);
                optionButton = new VisTextButton("...");

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
                valueLabel1 = new VisLabel();
                valueLabel2 = new VisLabel();
                updateVisibilities();

                minField1.setTextFieldListener(new VisTextField.TextFieldListener() {
                    @Override
                    public void keyTyped(VisTextField textField, char c) {
                        double oldVal = min1;
                        String rawVal = textField.getText();
                        try {
                            if (rawVal != null && rawVal.length() > 0)
                                min1 = Double.parseDouble(rawVal);
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
                                max1 = Double.parseDouble(rawVal);
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
                                min2 = Double.parseDouble(rawVal);
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
                                max2 = Double.parseDouble(rawVal);
                        } catch (NumberFormatException e){//NaN
                            return;
                        }
                        updateSlider(min1, max1, min2, oldVal);
                    }
                });
                updateLabelText();
                //update disabled in case of invalid range
                updateSlider(min1, max1, min2, max2);

                VisTable innerTable1 = new VisTable(true);
                innerTable1.add(minField1);
                innerTable1.add(slider1);
                innerTable1.add(maxField1);
                innerTable1.add(valueLabel1);

                VisTable innerTable2 = new VisTable(true);
                innerTable2.add(minField2);
                innerTable2.add(slider2);
                innerTable2.add(maxField2);
                innerTable2.add(valueLabel2);

                table.add(label).left().padRight(3);
                table.add(optionButton).padRight(10);
                table.add(innerTable1).row();
                table.add();
                table.add();
                table.add(innerTable2).padBottom(2).row();

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

            protected VisTextField createCollapsedIfInvisibleTextField(String text) {
                return new VisTextField(text) {
                    @Override
                    public float getPrefWidth() {
                        return isVisible() ? super.getPrefWidth() : 0;
                    }
                };
            }

            @Override
            public void removeFromTable() {
                label.remove();
                optionButton.remove();
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

    protected void setOptionButtonListener(Button optionButton) {
        optionButton.addListener(new ChangeListener(){
            @Override
            public void changed(ChangeEvent event, Actor actor) {
//                        if (event.getButton() == Input.Buttons.RIGHT){

                PopupMenu menu = new PopupMenu();
                MenuItem controlFieldsItem = new MenuItem("Set text controls");
                MenuItem controlSlidersItem = new MenuItem("Set slider controls");
                MenuItem typeStaticItem = new MenuItem("Set static value");
                MenuItem typeViewItem = new MenuItem("Set variable value");

                typeStaticItem.setDisabled(selectedSupplierClass == StaticParamSupplier.class);
                typeViewItem.setDisabled(selectedSupplierClass == CoordinateBasicShiftParamSupplier.class);
//                            typeScreenItem.setDisabled(true);

                controlFieldsItem.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        for (EntryView view : views.values()){
                            view.setInvalid();
                        }
                        setPrefListView(VIEWNAME_FIELDS);
                        generateViews();
//                        submit();
                    }
                });
                controlSlidersItem.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        for (EntryView view : views.values()){
                            view.setInvalid();
                        }
                        setPrefListView(VIEWNAME_SLIDERS);
                        generateViews();
//                        submit();
                    }
                });

                typeStaticItem.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        selectedSupplierClass = StaticParamSupplier.class;
                        submit();

                        typeStaticItem.setDisabled(true);
                        typeViewItem.setDisabled(false);
                    }
                });
                typeViewItem.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        selectedSupplierClass = CoordinateBasicShiftParamSupplier.class;
                        submit();

                        typeStaticItem.setDisabled(false);
                        typeViewItem.setDisabled(true);
                    }
                });
//                            typeScreenItem.setDisabled(true);

                menu.addItem(controlFieldsItem);
                menu.addItem(controlSlidersItem);
                menu.addSeparator();
                menu.addItem(typeStaticItem);
                menu.addItem(typeViewItem);
//                            menu.addItem(typeScreenItem);

                menu.showMenu(optionButton.getStage(), Gdx.input.getX(), Gdx.graphics.getHeight()-Gdx.input.getY());
            }
//                    }
        });
    }

    protected void submit() {
        ((MainStage) FractalsGdxMain.stage).submitServer(((MainStage) FractalsGdxMain.stage).getFocusedRenderer(), paramContainer);
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
