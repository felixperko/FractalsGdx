package de.felixp.fractalsgdx.ui.entries;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixp.fractalsgdx.ui.actors.TraversableGroup;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
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

    String prefListView = VIEW_LIST;

    boolean submitValue = false;

    //e.g. force side menu refresh when switching controls
    boolean forceReset = false;

    protected Class<? extends ParamSupplier> selectedSupplierClass;
    List<Class<? extends ParamSupplier>> possibleSupplierClasses;

    TraversableGroup traversableGroup;

    public AbstractPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, boolean submitValue){
        this.parameterDefinition = parameterDefinition;
        this.propertyName = parameterDefinition.getName();
        this.node = node;
        this.paramContainer = paramContainer;
        this.submitValue = submitValue;

        this.possibleSupplierClasses = parameterDefinition.getPossibleClasses();
        ParamSupplier param = paramContainer.getClientParameter(parameterDefinition.getName());
        if (param != null && possibleSupplierClasses.contains(param.getClass()))
            this.selectedSupplierClass = param.getClass();
        else
            this.selectedSupplierClass = possibleSupplierClasses.get(0);
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

            Gdx.app.postRunnable(() ->
                    view.addToTable(table)
            );
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

    public ParamContainer getParamContainer(){
        return paramContainer;
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
        return prefListView;
    }

    public void setCurrentControlView(String prefListView) {
        if (prefListView != this.prefListView)
            forceReset = true;
        this.prefListView = prefListView;
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
                boolean setDefaultValuePossible = defaultObject != null && selectedSupplierClass.equals(StaticParamSupplier.class);
                if (setDefaultValuePossible){
                    MenuItem setDefaultValueItem = new MenuItem("Set "+getDefaultObjectName());
                    setDefaultValueItem.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            setValue(defaultObject);
                            getParamContainer().addClientParameter(getSupplier());
                            submit();
                        }
                    });

                    menu.addItem(setDefaultValueItem);
                }

                boolean staticAndVariablePossible = possibleSupplierClasses.contains(StaticParamSupplier.class)
                        && possibleSupplierClasses.contains(CoordinateBasicShiftParamSupplier.class);
                if (staticAndVariablePossible) {

                    if (!(selectedSupplierClass.equals(StaticParamSupplier.class))) {
                        MenuItem typeStaticItem = new MenuItem("Set constant value");
//                    typeStaticItem.setDisabled(selectedSupplierClass == StaticParamSupplier.class);
                        typeStaticItem.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                selectedSupplierClass = StaticParamSupplier.class;
                                submit();
//                            typeStaticItem.setDisabled(true);
//                            typeVariableItem.setDisabled(false);
                            }
                        });
//                    menu.addSeparator();
                        menu.addItem(typeStaticItem);
                    }

                    if (!(selectedSupplierClass.equals(CoordinateBasicShiftParamSupplier.class))) {
                        MenuItem typeVariableItem = new MenuItem("Set map value");
//                        typeVariableItem.setDisabled(selectedSupplierClass == CoordinateBasicShiftParamSupplier.class);
                        typeVariableItem.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                selectedSupplierClass = CoordinateBasicShiftParamSupplier.class;
                                submit();
//                                typeStaticItem.setDisabled(false);
//                                typeVariableItem.setDisabled(true);
                            }
                        });
                        menu.addItem(typeVariableItem);
                    }
                }

                if (!prefListView.equals(VIEWNAME_FIELDS)) {
                    MenuItem controlFieldsItem = new MenuItem("Use text controls");
                    controlFieldsItem.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            for (EntryView view : views.values()) {
                                view.setInvalid();
                            }
                            setCurrentControlView(VIEWNAME_FIELDS);
                            generateViews();
                            MainStage stage = (MainStage) FractalsGdxMain.stage;
                            stage.getParamUI().refreshServerParameterUI(stage.getFocusedRenderer());
                            stage.getParamUI().refreshClientParameterUI(stage.getFocusedRenderer());
                        }
                    });
                    menu.addItem(controlFieldsItem);
                }

                if (!prefListView.equals(VIEWNAME_SLIDERS)){
                    MenuItem controlSlidersItem = new MenuItem("Use slider controls");
                    controlSlidersItem.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            for (EntryView view : views.values()) {
                                view.setInvalid();
                            }
                            setCurrentControlView(VIEWNAME_SLIDERS);
                            generateViews();
                            MainStage stage = (MainStage) FractalsGdxMain.stage;
                            stage.getParamUI().refreshServerParameterUI(stage.getFocusedRenderer());
                            stage.getParamUI().refreshClientParameterUI(stage.getFocusedRenderer());
                        }
                    });
                    menu.addItem(controlSlidersItem);
                }

                menu.showMenu(optionButton.getStage(), Gdx.input.getX(), Gdx.graphics.getHeight()-Gdx.input.getY());
            }
        });
    }

    protected void submit() {
        if (submitValue)
            ((MainStage) FractalsGdxMain.stage).submitServer(((MainStage) FractalsGdxMain.stage).getFocusedRenderer(), paramContainer);
    }

    public void addSubmitListener(Actor actor) {
        actor.addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER){
                    submit();
                    if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)){
                        ((MainStage)FractalsGdxMain.stage).getParamUI().getServerParamsSideMenu().getCollapseButton().toggle();
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
                return super.keyDown(event, keycode);
            }
        });
    }

    public void setTraversableGroup(TraversableGroup traversableGroup) {
        this.traversableGroup = traversableGroup;
    }

    public TraversableGroup getTraversableGroup(){
        return traversableGroup;
    }
}
