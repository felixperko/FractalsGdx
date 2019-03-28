package de.felixp.fractalsgdx.ui.entries;

import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;

import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.parameters.ParamSupplier;
import de.felixperko.fractals.system.parameters.StaticParamSupplier;

public abstract class AbstractDoublePropertyEntry extends AbstractPropertyEntry {

    protected VisValidatableTextField field1;
    protected VisValidatableTextField field2;
    VisLabel label;

    public AbstractDoublePropertyEntry(VisTable table, SystemClientData systemClientData, String propertyName, InputValidator validator1, InputValidator validator2) {
        super(table, systemClientData, propertyName);
        label = new VisLabel(propertyName);
        field1 = new VisValidatableTextField(validator1);
        field2 = new VisValidatableTextField(validator2);

        ParamSupplier paramSupplier = systemClientData.getClientParameter(propertyName);

        if (!(paramSupplier instanceof StaticParamSupplier))
            throw new IllegalArgumentException("AbstractDoublePropertyEntry only supports StaticParamSuppliers.");

        field1.setText(getParameterValue1((StaticParamSupplier)paramSupplier));
        field2.setText(getParameterValue2((StaticParamSupplier)paramSupplier));

        table.add(label);
        table.add(field1).row();
        table.add();
        table.add(field2).row();
    }

    public abstract String getParameterValue1(StaticParamSupplier paramSupplier);
    public abstract String getParameterValue2(StaticParamSupplier paramSupplier);

    @Override
    public void dispose() {
        field1.remove();
        field2.remove();
        label.remove();
    }
}
