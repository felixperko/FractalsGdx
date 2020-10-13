package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;

import java.util.ArrayList;
import java.util.List;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;

abstract class AbstractSingleTextPropertyEntry extends AbstractPropertyEntry {

    InputValidator validator;

    String text;

    public AbstractSingleTextPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, InputValidator validator) {
        super(node, paramContainer, parameterDefinition);
        this.validator = validator;
    }

    List<Actor> contentFields = new ArrayList<Actor>();
    List<ChangeListener> listeners = new ArrayList<>();

    @Override
    protected void generateViews() {
        views.put(VIEW_LIST, new EntryView() {

            protected VisValidatableTextField field;
            VisLabel label;

            @Override
            public void addToTable(Table table) {
                label = new VisLabel(propertyName);
                field = new VisValidatableTextField(validator);

                ParamSupplier textSupplier = paramContainer.getClientParameter(propertyName);

                if (textSupplier != null)
                    field.setText(text = textSupplier.getGeneral().toString());
                field.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (field.isInputValid())
                            text = field.getText();
                    }
                });
                for (ChangeListener listener : listeners)
                    field.addListener(listener);
                contentFields.add(field);

                table.add(label).left().padRight(3);
                table.add().pad(3);
                table.add(field).fillX().expandX().padBottom(2).row();
            }

            @Override
            public void removeFromTable() {
                label.remove();
                field.remove();
                contentFields.remove(field);
            }
        });
    }

    @Override
    public void addChangeListener(ChangeListener changeListener) {
        listeners.add(changeListener);
        for (Actor field : contentFields)
            field.addListener(changeListener);
    }

    @Override
    public void removeChangeListener(ChangeListener changeListener) {
        listeners.remove(changeListener);
        for (Actor field : contentFields)
            field.removeListener(changeListener);
    }
}
