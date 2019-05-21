package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;

import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;

abstract class AbstractSingleTextPropertyEntry extends AbstractPropertyEntry {

    InputValidator validator;

    String text;

    public AbstractSingleTextPropertyEntry(VisTable table, SystemClientData systemClientData, ParameterDefinition parameterDefinition, InputValidator validator) {
        super(table, systemClientData, parameterDefinition);
        this.validator = validator;
    }

    @Override
    protected void generateViews() {
        views.put(VIEW_LIST, new EntryView() {

            protected VisValidatableTextField field;
            VisLabel label;

            @Override
            public void drawOnTable(Table table) {
                label = new VisLabel(propertyName);
                field = new VisValidatableTextField(validator);

                ParamSupplier textSupplier = systemClientData.getClientParameter(propertyName);

                if (textSupplier != null)
                    field.setText(text = textSupplier.get(0,0).toString());
                field.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        text = field.getText();
                    }
                });

                table.add(label);
                table.add(field).padBottom(2).row();
            }

            @Override
            public void removeFromTable() {
                label.remove();
                field.remove();
            }
        });
    }
}
