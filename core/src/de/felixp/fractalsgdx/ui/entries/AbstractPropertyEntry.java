package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.widget.VisTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;

public abstract class AbstractPropertyEntry {

    public static final String VIEW_LIST = "LIST";
    public static final String VIEW_COLUMN = "COLUMN";
    public static final String VIEW_DETAILS = "DETAILS";

    VisTable table;
    SystemClientData systemClientData;

    String propertyName;

    Map<String, EntryView> views = new HashMap<>();

    String activeView = null;

    List<AbstractPropertyEntry> subEntries = null;

    public AbstractPropertyEntry(VisTable table, SystemClientData systemClientData, ParameterDefinition parameterDefinition){
        this.propertyName = parameterDefinition.getName();
        this.table = table;
        this.systemClientData = systemClientData;
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
                    view.drawOnTable(table)
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

    public SystemClientData getSystemClientData(){
        return systemClientData;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void applyValue() {
        ParamSupplier supplier = getSupplier();
//        if (supplier.isSystemRelevant())
//            FractalsGdxMain.client.jobId = 0;
        if (supplier != null)
            getSystemClientData().getClientParameters().put(getPropertyName(), supplier);
    }

    protected abstract ParamSupplier getSupplier();

    public void addSubEntries(List<AbstractPropertyEntry> subEntries) {
        this.subEntries = subEntries;
    }
}
