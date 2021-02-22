package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisRadioButton;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;
import com.kotcrab.vis.ui.widget.VisWindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.animation.ParamAnimation;
import de.felixp.fractalsgdx.animation.interpolationTypes.CardioidInterpolationFunction;
import de.felixp.fractalsgdx.animation.interpolationTypes.CircleInterpolationFunction;
import de.felixp.fractalsgdx.animation.interpolationTypes.InterpolationFunction;
import de.felixp.fractalsgdx.animation.interpolationTypes.LinearInterpolationFunction;
import de.felixp.fractalsgdx.animation.interpolations.ComplexNumberParamInterpolation;
import de.felixp.fractalsgdx.animation.interpolations.NumberParamInterpolation;
import de.felixp.fractalsgdx.animation.interpolations.ParamInterpolation;
import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class AnimationsUI {

    static ParamInterpolation selectedInterpolation = null;
    static FractalRenderer selectedRenderer = null;
    static Map<ParamAnimation, VisSlider> sliders = new HashMap<>();
    static boolean updatingSliders = false;

    public static void openAnimationsWindow(MainStage stage){

        selectedRenderer = ((MainStage)FractalsGdxMain.stage).focusedRenderer;
        sliders.clear();

        VisWindow window = new VisWindow("Animations");
        VisTable mainTable = new VisTable(true);

        for (ParamAnimation animation : selectedRenderer.getRendererContext().getParameterAnimations()){
            VisTable table = new VisTable(true);
            addAnimationRow(table, animation, selectedRenderer.getSystemContext().getParamContainer(), stage, window);
            mainTable.add(table).row();
        }

        VisTable buttonTable = new VisTable(true);
        VisTextButton addAnimationButton = new VisTextButton("add animation");
        addAnimationButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openAddAnimationWindow(stage, window, selectedRenderer);
            }
        });
        buttonTable.add(addAnimationButton);

        mainTable.add(buttonTable);

        window.add(mainTable);

        window.addCloseButton();
        window.pack();
        window.centerWindow();
        stage.addActor(window);
    }

    private static void addAnimationRow(Table outerTable, ParamAnimation animation, ParamContainer paramContainer, MainStage stage, VisWindow animationsWindow){


        VisLabel nameLabel = new VisLabel(animation.getName());
//        VisLabel paramLabel = new VisLabel("Parameter: ");

//        VisSelectBox<String> paramSelect = new VisSelectBox<>();
//        Map<String, String> paramSelectValues = new LinkedHashMap<>();
//        paramSelectValues.put("(empty)", null);
//        for (ParamSupplier supp : paramContainer.getClientParameters().values()) {
//            String paramName = supp.getName();
//            paramSelectValues.put(paramName, paramName);
//        }
//        Set<String> selectableKeys = paramSelectValues.keySet();
//        Array<String> selectableKeysArray = new Array<>(selectableKeys.toArray(new String[paramSelectValues.size()]));
//        paramSelect.setItems(selectableKeysArray);
//        String selectedParam = animation.getParameterName();
//        for (Map.Entry<String, String> e : paramSelectValues.entrySet()){
//            if ((e.getValue() == null && selectedParam == null) || (e.getValue() != null && e.getValue().equals(selectedParam))){
//                paramSelect.setSelected(e.getKey());
//                break;
//            }
//        }
//        paramSelect.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                animation.setParameterName(paramSelectValues.get(paramSelect.getSelected()));
//            }
//        });

        VisTextButton startButton = new VisTextButton("Start");
        VisTextButton stopButton = new VisTextButton("Pause");
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                animation.setPaused(false);
            }
        });
        stopButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                animation.setPaused(true);
            }
        });
        VisSlider progressSlider = new VisSlider(0.0f, 1.0f, 0.001f, false);
        sliders.put(animation, progressSlider);
        progressSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!updatingSliders)
                    animation.setProgress(progressSlider.getValue());
            }
        });

        VisRadioButton timeProgressButton = new VisRadioButton("time (s):");
        VisRadioButton frameProgressButton = new VisRadioButton("frames:");
        timeProgressButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (timeProgressButton.isChecked())
                    animation.setUsingTimeBasedProgress();
                else
                    animation.setUsingFrameBasedProgress();
            }
        });
        ButtonGroup<VisRadioButton> progressModeGroup = new ButtonGroup<>(timeProgressButton, frameProgressButton);
        if (animation.isUsingTimeBasedProgress())
            timeProgressButton.setChecked(true);
        else
            frameProgressButton.setChecked(true);
        VisTextField timeField = new VisValidatableTextField(Validators.FLOATS);
        timeField.setText(""+animation.getTimeFactor());
        timeField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (timeField.isInputValid())
                    animation.setTimeFactor(Double.parseDouble(timeField.getText()));
            }
        });
        VisTextField frameField = new VisValidatableTextField(Validators.INTEGERS);
        frameField.setText(""+animation.getFrameCount());
        frameField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (frameField.isInputValid())
                    animation.setFrameCount(Integer.parseInt(frameField.getText()));
            }
        });

        VisTable table1 = new VisTable(true);
        table1.add("Animation: ");
        table1.add(nameLabel);
//        table.add(paramSelect);
        table1.add(startButton);
        table1.add(stopButton);
        table1.add(progressSlider).fillX();
        outerTable.add(table1).fillX().left().row();

        VisTable table2 = new VisTable(true);
        table2.add(timeProgressButton);
        table2.add(timeField);
        table2.add(frameProgressButton);
        table2.add(frameField);
        outerTable.add(table2).left().row();

        for (ParamInterpolation interpolation : animation.getInterpolations().values()){
            addInterpolationRow(outerTable, animation, paramContainer, interpolation, stage, animationsWindow);
        }

        VisTextButton addInterpolationButton = new VisTextButton("add interpolation");
        addInterpolationButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openInterpolationAddWindow(stage, animation, animationsWindow, selectedRenderer);
            }
        });
        outerTable.add(addInterpolationButton).left();
        outerTable.row();

        VisTextButton recordButton = new VisTextButton("record animation");
        recordButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ScreenshotUI.recordScreenshots(animation.getName());
            }
        });
        outerTable.add(recordButton).left();
        outerTable.row();

    }

    public static String PARAM_TYPE_COMPLEXNUMBER = "complexnumber";
    public static String PARAM_TYPE_NUMBER = "number";
    public static final String PARAM_CONTAINERKEY_SERVER = "server";
    public static final String PARAM_CONTAINERKEY_CLIENT = "client";

    private static void addInterpolationRow(Table outerTable, ParamAnimation animation, ParamContainer paramContainer, ParamInterpolation interpolation, MainStage mainStage, VisWindow animationsWindow) {
        Table table = new VisTable(true);

//        VisSelectBox<String> containerSelect = new VisSelectBox();
//        Map<String, String> containers = new LinkedHashMap<>();
//        containers.put("calculation", PARAM_CONTAINERKEY_SERVER);
//        containers.put("drawing", PARAM_CONTAINERKEY_CLIENT);
//        containerSelect.setItems(containers.keySet().toArray(new String[containers.size()]));
//        VisSelectBox<String> paramSelect = new VisSelectBox<>();
//        Map<String, String> paramSelectValues = new LinkedHashMap<>();
////        paramSelectValues.put("(empty)", null);
//        for (ParamSupplier supp : paramContainer.getClientParameters().values()) {
//            String paramName = supp.getName();
//            paramSelectValues.put(paramName, paramName);
//        }
//        Set<String> selectableKeys = paramSelectValues.keySet();
//        Array<String> selectableKeysArray = new Array<>(selectableKeys.toArray(new String[paramSelectValues.size()]));
//        paramSelect.setItems(selectableKeysArray);
//        String selectedParam = interpolation.getParamName();
//        containerSelect.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                selectedContainer = containers.get(containerSelect.getSelected());
//                updateSelectableParams(paramSelect, selectedContainer, (MainStage)FractalsGdxMain.stage, selectedRenderer, interpolation.getParamType());
//            }
//        });
//        updateSelectableParams(paramSelect, selectedContainer, (MainStage)FractalsGdxMain.stage, selectedRenderer, interpolation.getParamType());
//        for (Map.Entry<String, String> e : paramSelectValues.entrySet()){
//            if ((e.getValue() == null && selectedParam == null) || (e.getValue() != null && e.getValue().equals(selectedParam))){
//                paramSelect.setSelected(e.getKey());
//                break;
//            }
//        }

//        paramSelect.addListener(new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                animation.changeInterpolationParamName(interpolation, paramSelectValues.get(paramSelect.getSelected()));
//            }
//        });

        VisTextButton editButton = new VisTextButton("...");
        editButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openInterpolationSettingsWindow(mainStage, animation, interpolation, animationsWindow, selectedRenderer);
            }
        });

//        table.add("- Target:");
//        table.add(containerSelect);
//        table.add("Param:");
//        table.add(paramSelect);

        table.add(" - "+interpolation.getParamContainerKey()+"."+interpolation.getParamName());
        table.add(editButton);

        outerTable.add(table).left().row();
    }

    public static void updateSliders() {
        updatingSliders = true;
        for (Map.Entry<ParamAnimation, VisSlider> e : sliders.entrySet()){
            ParamAnimation animation = e.getKey();
            if (animation.isPaused())
                continue;
            if (animation.isUsingTimeBasedProgress())
                e.getValue().setValue((float) animation.getLoopProgress());
            else if (animation.isUsingFrameBasedProgress())
                e.getValue().setValue((float) animation.getLoopProgress());
        }
        updatingSliders = false;
    }

    static String selectedParamType;
    static String selectedContainer = PARAM_CONTAINERKEY_SERVER;
    static Class<? extends InterpolationFunction> selectedInterpolationFunctionClass;
    static ParamInterpolation dummyInterpolationFunction;
    static List<VisTextField> defValueFields = new ArrayList<>();

    private static void openInterpolationAddWindow(MainStage stage, ParamAnimation animation, VisWindow animationsWindow, FractalRenderer selectedRenderer){
        openInterpolationSettingsWindow(stage, animation, null, animationsWindow, selectedRenderer);
    }

    private static void openInterpolationSettingsWindow(MainStage stage, ParamAnimation animation, ParamInterpolation interpolation, VisWindow animationsWindow, FractalRenderer selectedRenderer){

        boolean createNew =  interpolation == null;
        Map<Integer, List<VisTextField>> valueFields = new HashMap<>();

        VisWindow window = new VisWindow(createNew ? "Add interpolation to "+animation.getName() : "Edit interpolation of "+animation.getName()+": "+interpolation.getParamContainerKey()+"."+interpolation.getParamName());
        VisTable table = new VisTable(true);
        VisTable interpolationValuesTable = new VisTable(true);
        NumberFactory numberFactory = selectedRenderer.getSystemContext().getNumberFactory();

        Map<String, String> paramTypes = new LinkedHashMap<>();
        paramTypes.put("complex number", PARAM_TYPE_COMPLEXNUMBER);
        paramTypes.put("number", PARAM_TYPE_NUMBER);

        if (createNew){
            selectedParamType = PARAM_TYPE_COMPLEXNUMBER;
            selectedContainer = PARAM_CONTAINERKEY_SERVER;
        }
        else {
            selectedParamType = interpolation.getParamType();
            selectedContainer = interpolation.getParamContainerKey();
        }

        VisSelectBox<String> paramTypeSelect = new VisSelectBox();
        VisSelectBox<String> paramSelect = new VisSelectBox();
        paramTypeSelect.setItems(paramTypes.keySet().toArray(new String[paramTypes.size()]));
        paramTypeSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedParamType = paramTypes.get(paramTypeSelect.getSelected());
                updateSelectableParams(paramSelect, selectedContainer, stage, selectedRenderer, selectedParamType);
                window.pack();
            }
        });

        VisRadioButton serverTargetButton = new VisRadioButton("calculation");
        VisRadioButton clientTargetButton = new VisRadioButton("drawing");
        ButtonGroup<VisRadioButton> targetButtonGroup = new ButtonGroup<>();
        targetButtonGroup.add(serverTargetButton);
        targetButtonGroup.add(clientTargetButton);
        serverTargetButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedContainer = PARAM_CONTAINERKEY_SERVER;
                updateSelectableParams(paramSelect, selectedContainer, stage, selectedRenderer, selectedParamType);
                window.pack();
            }
        });
        clientTargetButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedContainer = PARAM_CONTAINERKEY_CLIENT;
                updateSelectableParams(paramSelect, selectedContainer, stage, selectedRenderer, selectedParamType);
                window.pack();
            }
        });
        updateSelectableParams(paramSelect, selectedContainer, stage, selectedRenderer, selectedParamType);

        VisSelectBox<Map.Entry<String, ParamInterpolation>> inheritControlPointSelect = new VisSelectBox(){
            @Override
            protected String toString(Object item) {
                return ((Map.Entry<String, ParamInterpolation>)item).getKey();
            }
        };

        Map<String, ParamInterpolation> inheritControlPointOptions = new LinkedHashMap<>();
        inheritControlPointOptions.put("no inheritance", null);
        for (ParamInterpolation interpolation2 : animation.getInterpolations().values()){
            if (interpolation2.getParamType().equals(selectedParamType))
                inheritControlPointOptions.put(animation.getName()+": "+interpolation2.getParamContainerKey()+"."+interpolation2.getParamName(), interpolation2);
        }
        inheritControlPointSelect.setItems(new Array<Map.Entry<String, ParamInterpolation>>(
                inheritControlPointOptions.entrySet().toArray(new Map.Entry[inheritControlPointOptions.size()]))
        );

        Map<String, Class<? extends InterpolationFunction>> interpolationFunctionOptions = new HashMap<>();
        interpolationFunctionOptions.put("linear", LinearInterpolationFunction.class);
        interpolationFunctionOptions.put("circle", CircleInterpolationFunction.class);
        interpolationFunctionOptions.put("cardioid", CardioidInterpolationFunction.class);
        VisSelectBox interpolationTypeSelect = new VisSelectBox();
        interpolationTypeSelect.setItems(interpolationFunctionOptions.keySet().toArray(new String[interpolationFunctionOptions.size()]));
//        if (!createNew) {
            selectedInterpolationFunctionClass = interpolationFunctionOptions.get(interpolationTypeSelect.getSelected());
//        }
        interpolationTypeSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedInterpolationFunctionClass = interpolationFunctionOptions.get(interpolationTypeSelect.getSelected());
                if (interpolation != null)
                    interpolation.setInterpolationFunction(selectedInterpolationFunctionClass);
                updateInterpolationValueTable(window, interpolation, interpolationValuesTable, numberFactory);
            }
        });

        VisTextButton deleteButton = null;
        if (!createNew){
            deleteButton = new VisTextButton("delete");
        }

        VisTextButton cancelButton = new VisTextButton("cancel");
        VisTextButton okButton = new VisTextButton(createNew ? "create" : "save");
        cancelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                window.remove();
            }
        });
        if (createNew) {
            okButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    String paramName = paramSelect.getSelected();
                    ParamInterpolation interpolation = createParamInterpolationAsSelected(paramName);
                    ParamInterpolation inheritValueInterpolation = inheritControlPointSelect.getSelected().getValue();
                    if (inheritValueInterpolation != null)
                        interpolation.setControlPointParent(inheritValueInterpolation);
                    animation.setInterpolation(interpolation);
                    if (inheritValueInterpolation == null)
                        selectedInterpolation = interpolation;
                    boolean applied = applyInterpolationFields(interpolation, defValueFields, numberFactory);
                    if (applied)
                        closeWindowAndReopenAnimationsWindow(window, animationsWindow, stage);
                }
            });
        }
        else {
            clientTargetButton.setChecked(interpolation.getParamContainerKey().equals(PARAM_CONTAINERKEY_CLIENT));
            serverTargetButton.setChecked(interpolation.getParamContainerKey().equals(PARAM_CONTAINERKEY_SERVER));
            Class<? extends InterpolationFunction> currFunctionClass = interpolation.getInterpolationFunctionClass();
            for (Map.Entry<String, Class<? extends InterpolationFunction>> e : interpolationFunctionOptions.entrySet()) {
                if (e.getValue().equals(currFunctionClass)) {
                    interpolationTypeSelect.setSelected(e.getKey());
                    selectedInterpolationFunctionClass = e.getValue();
                    updateInterpolationValueTable(window, interpolation, interpolationValuesTable, numberFactory);
                    break;
                }
            }
            updateSelectableParams(paramSelect, selectedContainer, stage, selectedRenderer, selectedParamType);
            paramSelect.setSelected(interpolation.getParamName());
            ParamInterpolation controlPointParent = interpolation.getControlPointParent();
            if (controlPointParent != null && controlPointParent != interpolation) {
                for (Map.Entry<String, ParamInterpolation> e : inheritControlPointOptions.entrySet()) {
                    if (e.getValue() != null && e.getValue().equals(controlPointParent)) {
                        inheritControlPointSelect.setSelected(e);
                        break;
                    }
                }
            }

            okButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    String paramName = paramSelect.getSelected();
                    interpolation.setInterpolationFunction(selectedInterpolationFunctionClass);
                    interpolation.setParam(paramName, selectedParamType, selectedContainer);
                    boolean applied = applyInterpolationFields(interpolation, defValueFields, numberFactory);
                    if (applied)
                        closeWindowAndReopenAnimationsWindow(window, animationsWindow, stage);
                }
            });
        }

        updateInterpolationValueTable(window, interpolation, interpolationValuesTable, numberFactory);

        table.add("Parameter type:");
        table.add(paramTypeSelect).expandX().fillX().row();

        table.add("Target:");
        VisTable targetButtonsTable = new VisTable(true);
        targetButtonsTable.add(serverTargetButton);
        targetButtonsTable.add(clientTargetButton);
        table.add(targetButtonsTable).row();

        table.add("Parameter:");
        table.add(paramSelect).expandX().fillX().row();

        table.add("Inherit values:");
        table.add(inheritControlPointSelect).expandX().fillX().row();

        table.add("Function:");
        table.add(interpolationTypeSelect).expandX().fillX().row();

        table.add();
        table.add(interpolationValuesTable).colspan(2).row();

        if (!createNew && interpolation.isPathBased()){
            List<Object> controlPoints = interpolation.getControlPoints(false);
            for (int i = 0 ; i < controlPoints.size() ; i++){
                Object controlPoint = controlPoints.get(i);
                List<VisTextField> pointValueFields = interpolation.addValueFieldsToTable(table, controlPoint, i);
                valueFields.put(i, pointValueFields);
            }
        }

        VisTable buttonTable = new VisTable(true);
        buttonTable.add(cancelButton);
        buttonTable.add(okButton);
        if (!createNew){
            buttonTable.add(deleteButton);
        }
        table.add(buttonTable).colspan(2);

        window.add(table);

        window.addCloseButton();
        window.pack();
        window.centerWindow();
        stage.addActor(window);
    }

    private static boolean applyInterpolationFields(ParamInterpolation interpolation, List<VisTextField> defValueFields, NumberFactory numberFactory) {
        interpolation.setNumberFactory(numberFactory);
        List<Number> defValues = new ArrayList<>();
        for (VisTextField field : defValueFields){
            try {
                Number number = numberFactory.createNumber(field.getText());
                if (number == null)
                    return false;
                defValues.add(number);
            } catch (NumberFormatException e){
                return false;
            }
        }
        interpolation.setDefValues(defValues);
        return true;
    }

    private static void updateInterpolationValueTable(VisWindow interpolationWindow, ParamInterpolation interpolation, VisTable table, NumberFactory numberFactory) {
        table.clearChildren();
        defValueFields.clear();
        boolean dummyInterpolation = interpolation == null;
        if (dummyInterpolation) {
            interpolation = createParamInterpolationAsSelected("midpoint");
            interpolation.setControlPoints(
                    interpolation.getDefValues(false),
                    interpolation.getControlPoints(false),
                    interpolation.getControlDerivatives(false), numberFactory);
        }
        int i = 0;
        final ParamInterpolation finalInterpolation = interpolation;
        for (Map.Entry<String, Number> e : interpolation.getInterpolationFunction().getDefValueDefaultsForActiveSet().entrySet()){
            String name = e.getKey();
            Number currentValue = (Number)interpolation.getDefValues(true).get(i);
            table.add(name);
            VisTextField valueField = new VisTextField(currentValue.toString());
            defValueFields.add(valueField);
            if (!dummyInterpolation) {
                valueField.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        boolean valid = true;
                        try {
                            Number val = numberFactory.createNumber(valueField.getText());
                            if (val == null)
                                valid = false;
                        } catch (NumberFormatException e){
                            valid = false;
                        }
                        valueField.setInputValid(valid);
                        applyInterpolationFields(finalInterpolation, defValueFields, numberFactory);
                    }
                });
            }
            table.add(valueField).row();
            i++;
        }
        //TODO dynamic values (control points)
        interpolationWindow.pack();
    }

    protected static ParamInterpolation createParamInterpolationAsSelected(String paramName) {
        return createParamInterpolation(paramName, selectedParamType, selectedContainer, selectedInterpolationFunctionClass);
    }

    protected static ParamInterpolation createParamInterpolation(String paramName, String paramType, String container, Class<? extends InterpolationFunction> interpolationFunctionClass) {
        boolean isNumber = selectedParamType.equals(PARAM_TYPE_NUMBER);
        boolean isComplexNumber = selectedParamType.equals(PARAM_TYPE_COMPLEXNUMBER);
        ParamInterpolation interpolation = null;
        if (isComplexNumber)
            interpolation = new ComplexNumberParamInterpolation(paramName, paramType, container, interpolationFunctionClass);
        else if (isNumber)
            interpolation = new NumberParamInterpolation(paramName, paramType, container, interpolationFunctionClass);
        return interpolation;
    }

    protected static void closeWindowAndReopenAnimationsWindow(VisWindow window, VisWindow animationsWindow, MainStage stage) {
        window.remove();
        if (animationsWindow.hasParent()) {
            animationsWindow.remove();
            openAnimationsWindow(stage);
        }
    }

    private static void openEditInterpolationWindow(MainStage stage, VisWindow animationsWindow, FractalRenderer selectedRenderer, ParamAnimation animation, ParamInterpolation interpolation){

    }

    private static void openAddAnimationWindow(MainStage stage, VisWindow animationsWindow, FractalRenderer selectedRenderer) {

        VisWindow window = new VisWindow("Add Animation");
        VisTable table = new VisTable(true);

        VisTextField nameField = new VisTextField();
        table.add("Name: ");
        table.add(nameField).row();

        window.add(table);

        window.addCloseButton();
        window.pack();
        window.centerWindow();
        stage.addActor(window);
    }

    private static Map<String, String> updateSelectableParams(VisSelectBox paramSelect, String containerKey, MainStage stage, FractalRenderer selectedRenderer, String selectedParamType){
        ParamContainer paramContainer;
        if (PARAM_CONTAINERKEY_SERVER.equals(containerKey)){
            paramContainer = selectedRenderer.getSystemContext().getParamContainer();
        }
        else if (PARAM_CONTAINERKEY_CLIENT.equals(containerKey)){
            paramContainer = stage.getClientParameters();
        }
        else
            throw new IllegalArgumentException("expected containerKey "+PARAM_CONTAINERKEY_SERVER+"/"+PARAM_CONTAINERKEY_CLIENT+" was: "+containerKey);

        List<Class<?>> checkClasses = new ArrayList<>();
        if (selectedParamType.equals(PARAM_TYPE_COMPLEXNUMBER)){
            checkClasses.add(ComplexNumber.class);
        }
        else if (selectedParamType.equals(PARAM_TYPE_NUMBER)){
            checkClasses.add(Number.class);
            checkClasses.add(Double.class);
        }

        Map<String, String> paramOptions = getParamOptions(paramContainer, checkClasses);
        paramSelect.setItems(paramOptions.keySet().toArray(new String[paramOptions.size()]));
        paramSelect.setSelectedIndex(0);
        return paramOptions;
    }

    private static Map<String, String> getParamOptions(ParamContainer paramContainer, List<Class<?>> checkClasses) {
        Map<String, String> paramOptions = new LinkedHashMap<>();
//        paramOptions.put("(empty)", null);
        for (String paramSupplierName : paramContainer.getClientParameters().keySet()){
            ParamSupplier supplier = paramContainer.getClientParameter(paramSupplierName);
            if (!(supplier instanceof StaticParamSupplier))
                continue;
            boolean valid = checkClasses.isEmpty();
            for (Class<?> cls : checkClasses){
                Object general = supplier.getGeneral();
                if (cls.isInstance(general)){
                    valid = true;
                    break;
                }
            }
            if (valid)
                paramOptions.put(paramSupplierName, paramSupplierName);
        }
        return paramOptions;
    }

    public static ParamInterpolation getSelectedInterpolation() {
        return selectedInterpolation;
    }
}
