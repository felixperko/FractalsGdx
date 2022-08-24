package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.FocusManager;
import com.kotcrab.vis.ui.Focusable;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;
import com.kotcrab.vis.ui.widget.VisWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.rendering.ClickedListener;
import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.rendering.MouseMovedListener;
import de.felixp.fractalsgdx.ui.CollapsiblePropertyList;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixp.fractalsgdx.ui.ParamControlState;
import de.felixp.fractalsgdx.ui.actors.FractalsWindow;
import de.felixp.fractalsgdx.ui.actors.TraversableGroup;
import de.felixp.fractalsgdx.ui.propertyattribute.AbstractPropertyAttributeAdapterUI;
import de.felixp.fractalsgdx.ui.propertyattribute.PropertyAttributeAdapterUI;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.numbers.impl.DoubleComplexNumber;
import de.felixperko.fractals.system.numbers.impl.DoubleNumber;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.attributes.ComplexNumberParamAttribute;
import de.felixperko.fractals.system.parameters.attributes.NumberParamAttribute;
import de.felixperko.fractals.system.parameters.attributes.ParamAttribute;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateDiscreteParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateModuloParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public abstract class AbstractPropertyEntry {

    private static Logger LOG = LoggerFactory.getLogger(AbstractPropertyEntry.class);

    public static final String VIEW_LIST = "LIST";
    public static final String VIEW_COLUMN = "COLUMN";
    public static final String VIEW_DETAILS = "DETAILS";

    public static final String VIEWNAME_SLIDERS = "SLIDERS";
    public static final String VIEWNAME_FIELDS = "FIELDS";

    Tree.Node node;
    ParamContainer paramContainer;

    String propertyUID;
    String propertyName;

    ParamDefinition parameterDefinition;

    Map<String, EntryView> views = new HashMap<>();

    String activeView = null;

    List<AbstractPropertyEntry> subEntries = null;

//    @Deprecated
//    String prefListView = VIEW_LIST;

    ParamControlState paramControlState;

    boolean submitValue = true;

    //e.g. force side menu refresh when switching controls
    boolean forceReset = false;

    protected Class<? extends ParamSupplier> selectedSupplierClass;
    List<Class<? extends ParamSupplier>> possibleSupplierClasses;
    boolean sliderControlsEnabled = false;
    boolean sliderLogarithmic = false;

    TraversableGroup traversableGroup;
    float prefControlWidth = -1;

    CollapsiblePropertyList parentPropertyList;


    public AbstractPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, boolean submitValue){
        this.parameterDefinition = parameterDefinition;
        this.propertyUID = parameterDefinition.getUID();
        this.propertyName = parameterDefinition.getName();
        this.node = node;
        this.paramContainer = paramContainer;
        this.submitValue = submitValue;
        this.getState().setControlView(VIEW_LIST);

        this.possibleSupplierClasses = parameterDefinition.getPossibleClasses();
        ParamSupplier supp = paramContainer.getParam(parameterDefinition.getUID());
        if (supp == null)
            this.selectedSupplierClass = possibleSupplierClasses.get(0);
        else if (possibleSupplierClasses.contains(supp.getClass()))
            this.selectedSupplierClass = supp.getClass();
        else
            throw new IllegalStateException("Unsupported ParamSupplier class: "+supp.getClass().getName());
    }

    public void init(){
        generateViews();
//        openView(VIEW_LIST, table);
    }

    public void addSubEntry(AbstractPropertyEntry subEntry){
        subEntries.add(subEntry);
    }

    protected abstract void generateViews();

    public EntryView openView(String viewName, Table table) {
        EntryView view = views.get(viewName);
        if (view != null) {
            view.addToTable(table);
            if (paramContainer != null) {
                ParamSupplier supp = paramContainer.getParam(getPropertyUID());
                Gdx.app.postRunnable(() -> {
                    if (supp instanceof StaticParamSupplier)
                        setValue(supp.getGeneral());
                });
            }
        }
        return view;
    }

    public void closeView(String viewName){
        EntryView view = views.get(viewName);
        closeView(view);
    }

    public void closeView(EntryView view){
        if (view != null){
            Gdx.app.postRunnable(() -> view.removeFromTable());
        }
    }

    public ParamControlState getState(){
        if (parentPropertyList != null)
            return parentPropertyList.getParamControlState(getPropertyUID());
        if (paramControlState == null)
            paramControlState = new ParamControlState();
        return paramControlState;
    }

    public ParamContainer getParamContainer(){
        return paramContainer;
    }

    public void setParamContainer(ParamContainer paramContainer) {
        this.paramContainer = paramContainer;
    }

    public String getPropertyUID() {
        return propertyUID;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void applyClientValue() {
        applyClientValue(getParamContainer());
    }

    public void applyClientValue(ParamContainer container) {
        try {
            ParamSupplier supplier = getSupplier();
            if (supplier != null)
                container.getParamMap().put(getPropertyUID(), supplier);
        } catch (Exception e){
            LOG.error("couldn't get supplier for uid: "+getPropertyUID()+" name: "+getPropertyName());
            throw e;
        }
    }

    public abstract ParamSupplier getSupplier();

    public void addSubEntries(List<AbstractPropertyEntry> subEntries) {
        this.subEntries = subEntries;
    }

    public String getPrefListView() {
        return getState().getControlView();
    }

    public void setCurrentControlView(String prefListView, boolean forceResetIfChanged) {
        boolean changed = !prefListView.equals(getState().getControlView());
        if (changed) {
            if (forceResetIfChanged)
                setForceReset(true);
            this.getState().setControlView(prefListView);
        }
    }

    public boolean isForceReset(boolean resetForceReset){
        boolean res = forceReset;
        if (resetForceReset)
            forceReset = false;
        return res;
    }

    public ParamDefinition getParameterDefinition() {
        return parameterDefinition;
    }

    public abstract void addChangeListener(ChangeListener changeListener);
    public abstract void removeChangeListener(ChangeListener changeListener);

    public void setValue(Object newValue){
        if (checkValue(newValue))
            setCheckedValue(newValue);
    }

    protected void applyValueToViews(Object newValue) {
        for (EntryView view : views.values()) {
            view.applyValue(newValue);
        }
    }
    protected void readFields(){
        if (activeView != null)
            views.get(activeView).readFields();
    }

    protected abstract boolean checkValue(Object valueObj);
    protected abstract void setCheckedValue(Object newValue);

    protected abstract Object getDefaultObject();

    protected String getDefaultObjectName(){
        return getDefaultObject() == null ? null : getDefaultObject().toString();
    }

    VisWindow setValueWindow;
    boolean autoClose = true;
    boolean autoSubmit = false;

    protected void supplierClassChanged() {
        if (setValueWindow != null)
            setValueWindow.remove();
    }

    protected void setOptionButtonListener(Button optionButton) {
        optionButton.addListener(new ChangeListener(){
            @Override
            public void changed(ChangeEvent event, Actor actor) {
//                        if (event.getButton() == Input.Buttons.RIGHT){

                PopupMenu menu = new PopupMenu();

                Object defaultObject = getDefaultObject();
                List<Object> presets = new ArrayList<>();
                if (defaultObject != null)
                    presets.add(defaultObject);
                boolean setValuePossible = !presets.isEmpty() && selectedSupplierClass.equals(StaticParamSupplier.class);
                if (setValuePossible) {
                    MenuItem setValueItem = new MenuItem("Set...");
                    PopupMenu subMenu = new PopupMenu();

                    MenuItem setDefaultValueItem = new MenuItem("Set "+getDefaultObjectName());
                    setDefaultValueItem.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            setValue(defaultObject);
                            getParamContainer().addParam(getSupplier());
                            submit();
                            menu.remove();
                        }
                    });

//                    subMenu.addItem(setDefaultValueItem);
//                    setValueItem.setSubMenu(subMenu);

                    setValueItem.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {

                            ParamSupplier supp = AbstractPropertyEntry.this.getSupplier();
                            if (!(supp instanceof StaticParamSupplier))
                                return;
                            Object val = supp.getGeneral();
                            if (!(val instanceof Number || val instanceof ComplexNumber))
                                return;

                            if (setValueWindow != null)
                                setValueWindow.remove();
                            setValueWindow = new FractalsWindow("Set value for "+getPropertyName());

                            VisTable svTable = new VisTable(true);

                            ParamAttribute attr = null;
                            if (val instanceof Number){
                                attr = new NumberParamAttribute("", "value") {
                                    @Override
                                    public Number getValue() {
                                        return (Number)val;
                                    }

                                    @Override
                                    public void applyValue(Object o) {
                                        AbstractPropertyEntry.this.setValue(o);
                                    }
                                };
                            }
                            else {
                                attr = new ComplexNumberParamAttribute("", "value") {
                                    @Override
                                    public ComplexNumber getValue() {
                                        return (ComplexNumber)val;
                                    }

                                    @Override
                                    public void applyValue(Object o) {
                                        AbstractPropertyEntry.this.setValue(o);
                                        if (autoSubmit){
                                            submit();
                                        }
                                    }
                                };
                            }
                            NumberFactory nf = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);
                            PropertyAttributeAdapterUI adapterUI = AbstractPropertyAttributeAdapterUI.getAdapterUI(attr, nf);

                            VisCheckBox autoCloseCheckbox = new VisCheckBox("close on enter", autoClose);
                            VisCheckBox autoSubmitCheckbox = new VisCheckBox("auto apply", autoSubmit);
                            autoCloseCheckbox.addListener(new ChangeListener() {
                                @Override
                                public void changed(ChangeEvent changeEvent, Actor actor) {
                                    autoClose = autoCloseCheckbox.isChecked();
                                }
                            });
                            autoSubmitCheckbox.addListener(new ChangeListener() {
                                @Override
                                public void changed(ChangeEvent changeEvent, Actor actor) {
                                    autoSubmit = autoSubmitCheckbox.isChecked();
                                }
                            });

                            VisTextButton pickValueButton = new VisTextButton("select on plane...", new ChangeListener() {
                                @Override
                                public void changed(ChangeEvent event, Actor actor) {
                                    FractalRenderer focusedRenderer = FractalsGdxMain.mainStage.getFocusedRenderer();
                                    final MouseMovedListener movedListener = focusedRenderer.getRendererContext().addMouseMovedListener(new MouseMovedListener() {
                                        @Override
                                        public void moved(float screenX, float screenY, ComplexNumber mappedValue) {
                                            setValue(mappedValue);
                                            submit();
                                        }
                                    });
                                    focusedRenderer.getRendererContext().addClickedListener(new ClickedListener() {
                                        @Override
                                        public void clicked(float mouseX, float mouseY, int button, ComplexNumber mappedValue) {
                                            focusedRenderer.getRendererContext().removeMouseMovedListener(movedListener);
                                        }
                                    }, true);
                                    setValueWindow.remove();
                                }
                            });

                            VisTextButton closeButton = new VisTextButton("cancel", new ChangeListener() {
                                @Override
                                public void changed(ChangeEvent changeEvent, Actor actor) {
                                    setValueWindow.remove();
                                }
                            });
                            VisTextButton applyButton = new VisTextButton("apply", new ChangeListener() {
                                @Override
                                public void changed(ChangeEvent changeEvent, Actor actor) {
                                    submit();
                                    if (autoClose) {
                                        setValueWindow.remove();
                                        parentPropertyList.focusFirstFocusableControl();
                                    }
                                }
                            });

                            adapterUI.setTraversableGroup(new TraversableGroup());
                            adapterUI.addToTable(svTable);

                            VisTable optionsTable = new VisTable(true);
                            optionsTable.add(autoCloseCheckbox).left();
                            optionsTable.add(autoSubmitCheckbox).left().row();
                            svTable.add(optionsTable).left().row();

                            svTable.add(pickValueButton).left().row();

                            VisTable buttonsTable = new VisTable(true);
                            buttonsTable.add(closeButton);
                            buttonsTable.add(applyButton);
                            svTable.add(buttonsTable);

                            adapterUI.addListenerToFields(new InputListener(){
                                @Override
                                public boolean keyDown(InputEvent event, int keycode) {
                                    if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)){
                                        submit();
                                        if (autoClose)
                                            setValueWindow.remove();
                                        return true;
                                    }
                                    if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)){
                                        setValueWindow.remove();
                                        FractalsGdxMain.mainStage.escapeHandled();
                                        FractalRenderer focusedRenderer = FractalsGdxMain.mainStage.getFocusedRenderer();
                                        if (focusedRenderer instanceof Focusable)
                                            FocusManager.switchFocus(FractalsGdxMain.mainStage, (Focusable)focusedRenderer);
                                    }
                                    return false;
                                }
                            });

                            setValueWindow.add(svTable);

                            setValueWindow.addCloseButton();
                            FractalsGdxMain.mainStage.addActor(setValueWindow);
                            setValueWindow.pack();
                            setValueWindow.centerWindow();

                            Actor focusable = adapterUI.getFirstFocusable();
                            if (focusable != null) {
                                FractalsGdxMain.mainStage.setKeyboardFocus(focusable);
                                if (focusable instanceof Focusable)
                                    ((Focusable) focusable).focusGained();
                            }
                        }
                    });

                    menu.addItem(setValueItem);
                    menu.addItem(setDefaultValueItem);
                    menu.addSeparator();
                }

                String controlView = getState().getControlView();
                if (!VIEWNAME_FIELDS.equals(controlView)) {
                    MenuItem controlFieldsItem = new MenuItem("use text input");
                    controlFieldsItem.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            for (EntryView view : views.values()) {
                                view.setInvalid();
                            }
                            setCurrentControlView(VIEWNAME_FIELDS, true);
                            generateViews();
                            MainStage stage = (MainStage) FractalsGdxMain.stage;
                            stage.getParamUI().refreshServerParameterUI(stage.getFocusedRenderer());
                            stage.getParamUI().refreshClientParameterUI(stage.getFocusedRenderer());
                        }
                    });
                    menu.addItem(controlFieldsItem);
                }

                if (sliderControlsEnabled) {
                    if (!VIEWNAME_SLIDERS.equals(controlView) && selectedSupplierClass.equals(StaticParamSupplier.class)) {
                        MenuItem controlSlidersItem = new MenuItem("use slider input");
                        controlSlidersItem.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                for (EntryView view : views.values()) {
                                    view.setInvalid();
                                }
                                setCurrentControlView(VIEWNAME_SLIDERS, true);
                                generateViews();
                                MainStage stage = (MainStage) FractalsGdxMain.stage;
                                stage.getParamUI().refreshServerParameterUI(stage.getFocusedRenderer());
                                stage.getParamUI().refreshClientParameterUI(stage.getFocusedRenderer());
                            }
                        });
                        menu.addItem(controlSlidersItem);

                        menu.addSeparator();
                    } else if (VIEWNAME_SLIDERS.equals(controlView)){
                        MenuItem sliderScalingItem = new MenuItem(sliderLogarithmic ? "use linear scaling" : "use logarithmic scaling");
                        sliderScalingItem.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                sliderLogarithmic = !sliderLogarithmic;
                                getState().setSliderScaling(sliderLogarithmic ? 1 : 0);
                            }
                        });
                        menu.addItem(sliderScalingItem);

                        menu.addSeparator();
                    }
                }

                boolean staticAndVariablePossible = possibleSupplierClasses.contains(StaticParamSupplier.class)
                        && possibleSupplierClasses.contains(CoordinateBasicShiftParamSupplier.class);
                if (staticAndVariablePossible) {

                    if (!(selectedSupplierClass.equals(StaticParamSupplier.class))) {
                        MenuItem typeStaticItem = new MenuItem("map constant");
//                    typeStaticItem.setDisabled(selectedSupplierClass == StaticParamSupplier.class);
                        typeStaticItem.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                selectedSupplierClass = StaticParamSupplier.class;
                                for (EntryView view : views.values()) {
                                    view.setInvalid();
                                }
                                generateViews();
                                setForceReset(true);
                                submit();
                                MainStage stage = (MainStage) FractalsGdxMain.stage;
                                stage.getParamUI().refreshServerParameterUI(stage.getFocusedRenderer());
                                stage.getParamUI().refreshClientParameterUI(stage.getFocusedRenderer());
                                supplierClassChanged();
//                            typeStaticItem.setDisabled(true);
//                            typeVariableItem.setDisabled(false);
                            }
                        });
//                    menu.addSeparator();
                        menu.addItem(typeStaticItem);
                    }

                    if (!(selectedSupplierClass.equals(CoordinateBasicShiftParamSupplier.class))) {
                        MenuItem typeVariableItem = new MenuItem("map complex plane");
//                        typeVariableItem.setDisabled(selectedSupplierClass == CoordinateBasicShiftParamSupplier.class);
                        typeVariableItem.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                selectedSupplierClass = CoordinateBasicShiftParamSupplier.class;
                                for (EntryView view : views.values()) {
                                    view.setInvalid();
                                }
                                generateViews();
                                setForceReset(true);
                                submit();
                                MainStage stage = (MainStage) FractalsGdxMain.stage;
                                stage.getParamUI().refreshServerParameterUI(stage.getFocusedRenderer());
                                stage.getParamUI().refreshClientParameterUI(stage.getFocusedRenderer());
                                supplierClassChanged();
//                                typeStaticItem.setDisabled(false);
//                                typeVariableItem.setDisabled(true);
                            }
                        });
                        menu.addItem(typeVariableItem);
                    }

                    if (!(selectedSupplierClass.equals(CoordinateModuloParamSupplier.class))) {
                        MenuItem typeVariableItem = new MenuItem("map grid of planes");
//                        typeVariableItem.setDisabled(selectedSupplierClass == CoordinateBasicShiftParamSupplier.class);
                        typeVariableItem.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                selectedSupplierClass = CoordinateModuloParamSupplier.class;
                                for (EntryView view : views.values()) {
                                    view.setInvalid();
                                }
                                generateViews();
                                setForceReset(true);
                                submit();
                                MainStage stage = (MainStage) FractalsGdxMain.stage;
                                stage.getParamUI().refreshServerParameterUI(stage.getFocusedRenderer());
                                stage.getParamUI().refreshClientParameterUI(stage.getFocusedRenderer());
                                supplierClassChanged();
//                                typeStaticItem.setDisabled(false);
//                                typeVariableItem.setDisabled(true);
                            }
                        });
                        menu.addItem(typeVariableItem);
                    }

                    if (!(selectedSupplierClass.equals(CoordinateDiscreteParamSupplier.class))) {
                        MenuItem typeVariableItem = new MenuItem("map grid constant");
//                        typeVariableItem.setDisabled(selectedSupplierClass == CoordinateBasicShiftParamSupplier.class);
                        typeVariableItem.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                selectedSupplierClass = CoordinateDiscreteParamSupplier.class;
                                for (EntryView view : views.values()) {
                                    view.setInvalid();
                                }
                                generateViews();
                                setForceReset(true);
                                submit();
                                MainStage stage = (MainStage) FractalsGdxMain.stage;
                                stage.getParamUI().refreshServerParameterUI(stage.getFocusedRenderer());
                                stage.getParamUI().refreshClientParameterUI(stage.getFocusedRenderer());
                                supplierClassChanged();
//                                typeStaticItem.setDisabled(false);
//                                typeVariableItem.setDisabled(true);
                            }
                        });
                        menu.addItem(typeVariableItem);
                    }
                }

                menu.showMenu(optionButton.getStage(), Gdx.input.getX(), Gdx.graphics.getHeight()-Gdx.input.getY());
            }
        });
    }

    public void setForceReset(boolean forceReset) {
        this.forceReset = forceReset;
    }

    public void submit() {
        if (submitValue) {
//            applyClientValue();
            ((MainStage) FractalsGdxMain.stage).submitServer(((MainStage) FractalsGdxMain.stage).getFocusedRenderer(), paramContainer);
        }
    }

    public void addSubmitListenerToField(Actor actor) {
        actor.addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER){
                    if (actor instanceof VisValidatableTextField && ((VisValidatableTextField) actor).isInputValid()){
                        readFields();
                        submit();
                        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
                            ((MainStage) FractalsGdxMain.stage).getParamUI().getServerParamsSideMenu().getCollapseButton().toggle();
                        }
                        if (Gdx.app.getType() == Application.ApplicationType.Android){
                            ((MainStage)FractalsGdxMain.stage).resetKeyboardFocus();
                        }
                        return true;
                    }
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE){
                    if (actor instanceof Focusable)
                        ((Focusable)actor).focusLost();
                    MainStage stage = (MainStage) FractalsGdxMain.stage;
                    stage.escapeHandled();
                    stage.resetKeyboardFocus();
                    return true;
                }
                return true;
            }
        });
    }

    public boolean isSliderControlsEnabled() {
        return sliderControlsEnabled;
    }

    public void setSliderControlsEnabled(boolean sliderControlsEnabled) {
        this.sliderControlsEnabled = sliderControlsEnabled;
    }

    public void setTraversableGroup(TraversableGroup traversableGroup) {
        this.traversableGroup = traversableGroup;
    }

    public TraversableGroup getTraversableGroup(){
        return traversableGroup;
    }

    public CollapsiblePropertyList getParentPropertyList() {
        return parentPropertyList;
    }

    public void setParentPropertyList(CollapsiblePropertyList parentPropertyList) {
        this.parentPropertyList = parentPropertyList;
        if (this.paramControlState != null){
            this.parentPropertyList.getParamControlState(propertyUID).copyValuesIfNull(this.paramControlState);
            this.paramControlState = null;
        }
    }

    public float getPrefControlWidth() {
        return prefControlWidth;
    }

    public void setPrefControlWidth(float prefControlWidth) {
        this.prefControlWidth = prefControlWidth;
    }
}
