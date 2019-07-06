package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;

import de.felixperko.fractals.network.ParamContainer;
import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public abstract class AbstractDoubleTextPropertyEntry extends AbstractPropertyEntry {

    InputValidator validator1;
    InputValidator validator2;

    String text1;
    String text2;

    public AbstractDoubleTextPropertyEntry(VisTable table, ParamContainer paramContainer, ParameterDefinition parameterDefinition, InputValidator validator1, InputValidator validator2) {
        super(table, paramContainer, parameterDefinition);

        this.validator1 = validator1;
        this.validator2 = validator2;

    }

    public abstract String getParameterValue1(StaticParamSupplier paramSupplier);
    public abstract String getParameterValue2(StaticParamSupplier paramSupplier);

    @Override
    protected void generateViews() {
        views.put(VIEW_LIST, new EntryView() {

            protected VisValidatableTextField field1;
            protected VisValidatableTextField field2;
            VisLabel label;

            @Override
            public void drawOnTable(Table table) {
                label = new VisLabel(propertyName);
                field1 = new VisValidatableTextField(validator1);
                field2 = new VisValidatableTextField(validator2);

                ParamSupplier paramSupplier = paramContainer.getClientParameter(propertyName);

                if (!(paramSupplier instanceof StaticParamSupplier))
                    throw new IllegalArgumentException("AbstractDoubleTextPropertyEntry only supports StaticParamSuppliers.");

                field1.setText(text1 = getParameterValue1((StaticParamSupplier)paramSupplier));
                field2.setText(text2 = getParameterValue2((StaticParamSupplier)paramSupplier));

                field1.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        text1 = field1.getText();
                    }
                });
                field2.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        text2 = field2.getText();
                    }
                });

                table.add(label);
                table.add(field1).row();
                table.add();
                table.add(field2).padBottom(2).row();
            }
            @Override
            public void removeFromTable() {
                field1.remove();
                field2.remove();
                label.remove();
            }
        });
    }
}
