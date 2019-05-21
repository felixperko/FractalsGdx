package de.felixp.fractalsgdx.ui.entries;

import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.Numbers.infra.NumberFactory;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class NumberTextPropertyEntry extends AbstractSingleTextPropertyEntry {

    NumberFactory numberFactory;

    public NumberTextPropertyEntry(VisTable table, SystemClientData systemClientData, ParameterDefinition parameterDefinition, NumberFactory numberFactory, InputValidator inputValidator) {
        super(table, systemClientData, parameterDefinition, inputValidator);
        this.numberFactory = numberFactory;
    }

    @Override
    protected ParamSupplier getSupplier() {
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyName(), numberFactory.createNumber(text));
        supplier.setLayerRelevant(true);
        return supplier;
    }
}
