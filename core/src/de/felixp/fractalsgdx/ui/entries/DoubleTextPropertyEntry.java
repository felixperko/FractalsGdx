package de.felixp.fractalsgdx.ui.entries;

import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.network.ParamContainer;
import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class DoubleTextPropertyEntry extends AbstractSingleTextPropertyEntry {

    public DoubleTextPropertyEntry(VisTable table, ParamContainer paramContainer, ParameterDefinition parameterDefinition) {
        super(table, paramContainer, parameterDefinition, Validators.FLOATS);
    }

    @Override
    public ParamSupplier getSupplier() {
        Double val = 0.D;
        try {
            val = Double.parseDouble(text);
        } catch (NumberFormatException e){
        }
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyName(), val);
        supplier.setLayerRelevant(true);
        return supplier;
    }
}
