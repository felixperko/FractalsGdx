package de.felixp.fractalsgdx.ui.entries;

import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;

import de.felixperko.fractals.network.SystemClientData;

abstract class AbstractSinglePropertyEntry extends AbstractPropertyEntry {

    protected final VisValidatableTextField field;
    VisLabel label;

    public AbstractSinglePropertyEntry(VisTable table, SystemClientData systemClientData, String propertyName, InputValidator validator) {
        super(table, systemClientData, propertyName);
        label = new VisLabel(propertyName);
        field = new VisValidatableTextField(validator);

        field.setText(systemClientData.getClientParameter(propertyName).get(0,0).toString());

        table.add(label);
        table.add(field).row();
    }

    @Override
    public void dispose() {
        field.remove();
        label.remove();
    }
}
