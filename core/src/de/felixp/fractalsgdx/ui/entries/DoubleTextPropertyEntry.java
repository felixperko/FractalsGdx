package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class DoubleTextPropertyEntry extends AbstractSingleTextPropertyEntry {

    public DoubleTextPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition) {
        super(node, paramContainer, parameterDefinition, Validators.FLOATS);
    }

    @Override
    public ParamSupplier getSupplier() {
        Double val = 0.D;
        try {
            val = text == null ? 0.0 : Double.parseDouble(text);
        } catch (NumberFormatException e){
        }
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyName(), val);
        supplier.setLayerRelevant(true);
        supplier.setChanged(true);
        return supplier;
    }
}
