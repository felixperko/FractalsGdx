package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.util.Validators;

import de.felixperko.expressions.FractalsExpression;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.expressions.FractalsExpressionParser;

public class ExpressionTextPropertyEntry extends AbstractSingleTextPropertyEntry {

    public ExpressionTextPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, new InputValidator() {
            @Override
            public boolean validateInput(String input) {
                try {
                    FractalsExpression expr = FractalsExpressionParser.parse(input);
                    if (expr == null)
                        return false;
                    //TODO update dynamic parameters
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        }, submitValue);
        ParamSupplier clientParameter = paramContainer.getClientParameter(propertyName);
        if (clientParameter != null)
            text = clientParameter.getGeneral(String.class);
    }

    @Override
    public ParamSupplier getSupplier() {
        StaticParamSupplier supplier = new StaticParamSupplier(getPropertyName(), text);
        supplier.setLayerRelevant(true);
        return supplier;
    }

    @Override
    protected boolean checkValue(Object valueObj) {
        return valueObj instanceof String;
    }

    @Override
    protected void setCheckedValue(Object newValue) {
        text = (String)newValue;
    }

}

