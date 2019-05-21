package de.felixp.fractalsgdx.ui.entries;

import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class DoubleTextPropertyEntry extends AbstractSingleTextPropertyEntry {

    public DoubleTextPropertyEntry(VisTable table, SystemClientData systemClientData, ParameterDefinition parameterDefinition) {
        super(table, systemClientData, parameterDefinition, Validators.FLOATS);
    }

    @Override
    protected ParamSupplier getSupplier() {
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyName(), Double.parseDouble(text));
        supplier.setLayerRelevant(true);
        return supplier;
    }
}
