package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.util.Validators;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.util.expressions.FractalsExpressionParser;
import de.felixperko.fractals.util.expressions.FractalsPolynom;

public class StringTextPropertyEntry extends AbstractSingleTextPropertyEntry {

    public StringTextPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParameterDefinition parameterDefinition) {
        super(node, paramContainer, parameterDefinition, new InputValidator() {
            @Override
            public boolean validateInput(String input) {
                try {
                    return FractalsExpressionParser.parse(input) != null;
                } catch (IllegalArgumentException e){
                    return false;
                }
            }
        });
        text = paramContainer.getClientParameter(propertyName).getGeneral(String.class);
    }

    @Override
    public ParamSupplier getSupplier() {
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyName(), text);
        supplier.setLayerRelevant(true);
        return supplier;
    }

}

