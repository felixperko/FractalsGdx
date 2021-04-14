package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;

public abstract class AbstractPropertyEntry {

    private static Logger LOG = LoggerFactory.getLogger(AbstractPropertyEntry.class);

    public static final String VIEW_LIST = "LIST";
    public static final String VIEW_COLUMN = "COLUMN";
    public static final String VIEW_DETAILS = "DETAILS";

    Tree.Node node;
    ParamContainer paramContainer;

    String propertyName;

    ParamDefinition parameterDefinition;

    Map<String, EntryView> views = new HashMap<>();

    String activeView = null;

    List<AbstractPropertyEntry> subEntries = null;

    String prefListView = VIEW_LIST;

    boolean submitValue = false;

    public AbstractPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, boolean submitValue){
        this.parameterDefinition = parameterDefinition;
        this.propertyName = parameterDefinition.getName();
        this.node = node;
        this.paramContainer = paramContainer;
        this.submitValue = submitValue;
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

    public void setPrefListView(String prefListView) {
        this.prefListView = prefListView;
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
}
