package de.felixp.fractalsgdx.ui.entries;

import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.parameters.ParamSupplier;
import de.felixperko.fractals.system.parameters.StaticParamSupplier;

public class IntPropertyEntry extends AbstractSinglePropertyEntry {

    public IntPropertyEntry(VisTable table, SystemClientData systemClientData, String propertyName) {
        super(table, systemClientData, propertyName, Validators.INTEGERS);
    }

    @Override
    protected ParamSupplier getSupplier() {
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyName(), Integer.parseInt(field.getText()));
        supplier.setLayerRelevant(true);
        return supplier;
    }

}
