package de.felixp.fractalsgdx.ui.entries;

import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.Numbers.infra.ComplexNumber;
import de.felixperko.fractals.system.Numbers.infra.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamSupplier;
import de.felixperko.fractals.system.parameters.StaticParamSupplier;

public class ComplexNumberPropertyEntry extends AbstractDoublePropertyEntry {

    NumberFactory numberFactory;

    public ComplexNumberPropertyEntry(VisTable table, SystemClientData systemClientData, String propertyName, NumberFactory numberFactory) {
        super(table, systemClientData, propertyName, Validators.FLOATS, Validators.FLOATS); //TODO custom Validator
        this.numberFactory = numberFactory;
    }

    @Override
    public String getParameterValue1(StaticParamSupplier paramSupplier) {
        return paramSupplier.getGeneral(ComplexNumber.class).getReal().toString();
    }

    @Override
    public String getParameterValue2(StaticParamSupplier paramSupplier) {
        return paramSupplier.getGeneral(ComplexNumber.class).getImag().toString();
    }

    @Override
    protected ParamSupplier getSupplier() {
        ParamSupplier supplier = new StaticParamSupplier(propertyName, numberFactory.createComplexNumber(field1.getText(), field2.getText()));
        supplier.setLayerRelevant(true);
        return supplier;
    }
}
