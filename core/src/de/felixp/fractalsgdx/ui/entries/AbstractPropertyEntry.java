package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.parameters.ParamSupplier;
import de.felixperko.fractals.system.parameters.StaticParamSupplier;

public abstract class AbstractPropertyEntry extends WidgetGroup {

    VisTable table;
    SystemClientData systemClientData;

    String propertyName;

    public AbstractPropertyEntry(VisTable table, SystemClientData systemClientData, String propertyName){
        this.propertyName = propertyName;
        this.table = table;
        this.systemClientData = systemClientData;
    }

    public abstract void dispose();

    public SystemClientData getSystemClientData(){
        return systemClientData;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void applyValue() {
        ParamSupplier supplier = getSupplier();
        getSystemClientData().getClientParameters().put(getPropertyName(), supplier);
    }

    protected abstract ParamSupplier getSupplier();
}
