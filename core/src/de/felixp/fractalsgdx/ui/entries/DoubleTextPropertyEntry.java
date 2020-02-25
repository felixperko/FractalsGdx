package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class DoubleTextPropertyEntry extends AbstractSingleTextPropertyEntry {

    public DoubleTextPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParameterDefinition parameterDefinition) {
        super(node, paramContainer, parameterDefinition, Validators.FLOATS);
    }

    @Override
    public ParamSupplier getSupplier() {
        Double val = 0.D;
        try {
            val = Double.parseDouble(text);
        } catch (NumberFormatException e){
        }
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyName(), val);
        supplier.setLayerRelevant(true);
        return supplier;
    }
}
