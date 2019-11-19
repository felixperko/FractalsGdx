package de.felixp.fractalsgdx.ui.entries;

import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class IntTextPropertyEntry extends AbstractSingleTextPropertyEntry {

    public IntTextPropertyEntry(VisTable table, ParamContainer paramContainer, ParameterDefinition parameterDefinition) {
        super(table, paramContainer, parameterDefinition, Validators.INTEGERS);
    }

    @Override
    public ParamSupplier getSupplier() {
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyName(), Integer.parseInt(text));
        supplier.setLayerRelevant(true);
        return supplier;
    }

}
