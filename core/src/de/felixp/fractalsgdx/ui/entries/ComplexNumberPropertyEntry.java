package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class ComplexNumberPropertyEntry extends AbstractDoubleTextPropertyEntry {

    NumberFactory numberFactory;

    public ComplexNumberPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, NumberFactory numberFactory, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, Validators.FLOATS, Validators.FLOATS, submitValue); //TODO custom Validator
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
        supplier.setLayerRelevant(true);//TODO only if in definition! other flags!
        return supplier;
    }

    @Override
    protected boolean checkValue(Object valueObj) {
        return valueObj instanceof ComplexNumber;
    }

    @Override
    protected void setCheckedValue(Object newValue) {
        ComplexNumber cn = (ComplexNumber)newValue;
        text1 = cn.getReal().toString();
        text2 = cn.getImag().toString();
        super.setCheckedValue(newValue);
    }

    @Override
    protected Object getDefaultObject() {
        return numberFactory.createComplexNumber("0", "0");
    }

    @Override
    protected String getDefaultObjectName() {
        return "("+getDefaultObject().toString()+")";
    }
}
