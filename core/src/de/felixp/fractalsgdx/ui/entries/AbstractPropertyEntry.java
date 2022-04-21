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
import com.kotcrab.vis.ui.Focusable;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.ui.CollapsiblePropertyList;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixp.fractalsgdx.ui.ParamControlState;
import de.felixp.fractalsgdx.ui.actors.TraversableGroup;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
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
        this.propertyName = parameterDefinition.getName();
        this.node = node;
        this.paramContainer = paramContainer;
        this.submitValue = submitValue;
        this.getState().setControlView(VIEW_LIST);

        this.possibleSupplierClasses = parameterDefinition.getPossibleClasses();
        ParamSupplier supp = paramContainer.getClientParameter(parameterDefinition.getName());
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
                ParamSupplier supp = paramContainer.getClientParameter(getPropertyName());
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
            return parentPropertyList.getParamControlState(getPropertyName());
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
                container.getClientParameters().put(getPropertyName(), supplier);
        } catch (Exception e){
            LOG.error("couldn't get supplier for name: "+getPropertyName());
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
        if (forceResetIfChanged && !prefListView.equals(getState().getControlView()))
            setForceReset(true);
        this.getState().setControlView(prefListView);
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

                    MenuItem setDefaultValueItem = new MenuItem(getDefaultObjectName());
                    setDefaultValueItem.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            setValue(defaultObject);
                            getParamContainer().addClientParameter(getSupplier());
                            submit();
                        }
                    });

                    subMenu.addItem(setDefaultValueItem);

                    setValueItem.setSubMenu(subMenu);
                    menu.addItem(setValueItem);
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
            this.parentPropertyList.getParamControlState(propertyName).copyValuesIfNull(this.paramControlState);
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
