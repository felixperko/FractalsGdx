package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class NumberTextPropertyEntry extends AbstractSingleTextPropertyEntry {

    NumberFactory numberFactory;

    public NumberTextPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, NumberFactory numberFactory, InputValidator inputValidator, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, inputValidator, submitValue);
        this.numberFactory = numberFactory;
        showMenu = true;
    }

    @Override
    public ParamSupplier getSupplier() {
        Number val = text == null ? numberFactory.createNumber(0.0) : numberFactory.createNumber(text);
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyName(), val);
        supplier.setLayerRelevant(true);
        return supplier;
    }

    @Override
    protected boolean checkValue(Object valueObj) {
        return valueObj instanceof Number;
    }

    @Override
    protected void setCheckedValue(Object newValue) {
        text = ((Number)newValue).toString();
    }

    @Override
    protected Object getDefaultObject() {
        return numberFactory.createNumber("0");
    }

    @Override
    protected String getDefaultObjectName() {
        return "("+getDefaultObject().toString()+")";
    }
}
