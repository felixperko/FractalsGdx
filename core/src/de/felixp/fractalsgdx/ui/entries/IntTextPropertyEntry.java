package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class IntTextPropertyEntry extends AbstractSingleTextPropertyEntry {

    public IntTextPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, Validators.INTEGERS, submitValue);
        sliderValueLabelPrecision = 0;
//        showMenu = true;
    }

    @Override
    public ParamSupplier getSupplier() {
        int val = text == null ? 0 : (int)Double.parseDouble(text);
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyName(), val);
        supplier.setLayerRelevant(true);
        return supplier;
    }

    @Override
    protected boolean checkValue(Object valueObj) {
        return valueObj instanceof Integer;
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
