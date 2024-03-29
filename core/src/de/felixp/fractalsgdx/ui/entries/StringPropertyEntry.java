package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.util.Validators;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class StringPropertyEntry extends AbstractSingleTextPropertyEntry{

    public StringPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, new InputValidator() {
            @Override
            public boolean validateInput(String input) {
                return true;
            }
        }, submitValue);
    }

    @Override
    public ParamSupplier getSupplier() {
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyUID(), text);
        supplier.setLayerRelevant(true);
        return supplier;
    }

    @Override
    protected boolean checkValue(Object valueObj) {
        return valueObj instanceof String;
    }

//    @Override
//    protected void setCheckedValue(Object newValue) {
//        text = ""+newValue;
//    }

    @Override
    protected Object getDefaultObject() {
        return null;
    }
}
