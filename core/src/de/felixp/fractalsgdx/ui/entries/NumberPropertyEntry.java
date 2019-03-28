package de.felixp.fractalsgdx.ui.entries;

import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.Numbers.infra.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamSupplier;
import de.felixperko.fractals.system.parameters.StaticParamSupplier;

public class NumberPropertyEntry extends AbstractSinglePropertyEntry {

    NumberFactory numberFactory;

    public NumberPropertyEntry(VisTable table, SystemClientData systemClientData, String propertyName, NumberFactory numberFactory, InputValidator inputValidator) {
        super(table, systemClientData, propertyName, inputValidator);
        this.numberFactory = numberFactory;
    }

    @Override
    protected ParamSupplier getSupplier() {
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyName(), numberFactory.createNumber(field.getText()));
        supplier.setLayerRelevant(true);
        return supplier;
    }
}
