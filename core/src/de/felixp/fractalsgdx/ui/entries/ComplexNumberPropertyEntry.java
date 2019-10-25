package de.felixp.fractalsgdx.ui.entries;

import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.network.ParamContainer;
import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class ComplexNumberPropertyEntry extends AbstractDoubleTextPropertyEntry {

    NumberFactory numberFactory;

    public ComplexNumberPropertyEntry(VisTable table, ParamContainer paramContainer, ParameterDefinition parameterDefinition, NumberFactory numberFactory) {
        super(table, paramContainer, parameterDefinition, Validators.FLOATS, Validators.FLOATS); //TODO custom Validator
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
    public ParamSupplier getSupplier() {
        ParamSupplier supplier = null;

        if (selectedSupplierClass.isAssignableFrom(CoordinateBasicShiftParamSupplier.class)){
            supplier = new CoordinateBasicShiftParamSupplier(propertyName);
        }
        else {
            String real = (text1 == null || text1.length() == 0) ? "0" : text1;
            String imag = (text2 == null || text2.length() == 0) ? "0" : text2;
            supplier = new StaticParamSupplier(propertyName, numberFactory.createComplexNumber(real, imag));
        }

        supplier.setLayerRelevant(true);
        return supplier;
    }
}
