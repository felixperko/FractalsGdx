package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class IntTextPropertyEntry extends AbstractSingleTextPropertyEntry {

    public IntTextPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParameterDefinition parameterDefinition) {
        super(node, paramContainer, parameterDefinition, Validators.INTEGERS);
    }

    @Override
    public ParamSupplier getSupplier() {
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyName(), Integer.parseInt(text));
        supplier.setLayerRelevant(true);
        return supplier;
    }

}
