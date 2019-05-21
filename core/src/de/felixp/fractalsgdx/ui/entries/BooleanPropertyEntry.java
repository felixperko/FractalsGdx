package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;

public class BooleanPropertyEntry extends AbstractPropertyEntry {

    public BooleanPropertyEntry(VisTable table, SystemClientData systemClientData, ParameterDefinition parameterDefinition) {
        super(table, systemClientData, parameterDefinition);
    }

    VisLabel label;
    VisCheckBox checkBox;

    @Override
    protected void generateViews() {
        views.put(VIEW_LIST, new EntryView() {
            @Override
            public void drawOnTable(Table table) {
                if (label == null) {
                    label = new VisLabel(getPropertyName());
                    checkBox = new VisCheckBox("enabled");
                }

                ParamSupplier supplier = systemClientData.getClientParameter(propertyName);
                if (supplier != null)
                    checkBox.setChecked(supplier.getGeneral(Boolean.class));

                table.add(label);
                table.add(checkBox).padBottom(2).row();
            }

            @Override
            public void removeFromTable() {
                label.remove();
                checkBox.remove();
            }
        });
    }

    @Override
    protected ParamSupplier getSupplier() {
        return null;
    }
}
