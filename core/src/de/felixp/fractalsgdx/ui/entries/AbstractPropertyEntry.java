package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;

public abstract class AbstractPropertyEntry {

    public static final String VIEW_LIST = "LIST";
    public static final String VIEW_COLUMN = "COLUMN";
    public static final String VIEW_DETAILS = "DETAILS";

    Tree.Node node;
    ParamContainer paramContainer;

    String propertyName;

    ParameterDefinition parameterDefinition;

    Map<String, EntryView> views = new HashMap<>();

    String activeView = null;

    List<AbstractPropertyEntry> subEntries = null;

    public AbstractPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParameterDefinition parameterDefinition){
        this.parameterDefinition = parameterDefinition;
        this.propertyName = parameterDefinition.getName();
        this.node = node;
        this.paramContainer = paramContainer;
    }

    public void init(){
        generateViews();
//        openView(VIEW_LIST, table);
    }

    public void addSubEntry(AbstractPropertyEntry subEntry){
        subEntries.add(subEntry);
    }

    protected abstract void generateViews();

    public void openView(String viewName, Table table) {
        EntryView view = views.get(viewName);
        if (view != null) {

//            Gdx.app.postRunnable(() ->
                    view.addToTable(table)
                    ;
//            );
        }
    }

    public void closeView(String viewName){
        EntryView view = views.get(viewName);
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
        ParamSupplier supplier = getSupplier();
        if (supplier != null)
            container.getClientParameters().put(getPropertyName(), supplier);
    }

    public abstract ParamSupplier getSupplier();

    public void addSubEntries(List<AbstractPropertyEntry> subEntries) {
        this.subEntries = subEntries;
    }

    public ParameterDefinition getParameterDefinition() {
        return parameterDefinition;
    }

    public abstract void addChangeListener(ChangeListener changeListener);
    public abstract void removeChangeListener(ChangeListener changeListener);
}
