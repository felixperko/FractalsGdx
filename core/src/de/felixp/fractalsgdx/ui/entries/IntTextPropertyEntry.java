package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class IntTextPropertyEntry extends AbstractSingleTextPropertyEntry {

    public IntTextPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition) {
        super(node, paramContainer, parameterDefinition, Validators.INTEGERS);
    }

    @Override
    public ParamSupplier getSupplier() {
        int val = text == null ? 0 : Integer.parseInt(text);
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyName(), val);
        supplier.setLayerRelevant(true);
        return supplier;
    }

}
