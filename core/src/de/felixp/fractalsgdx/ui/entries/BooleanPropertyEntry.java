package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

import java.util.ArrayList;
import java.util.List;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class BooleanPropertyEntry extends AbstractPropertyEntry {

    public BooleanPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, submitValue);
    }

    VisLabel label;
    VisCheckBox checkBox;

    List<Actor> contentFields = new ArrayList<Actor>();
    List<ChangeListener> listeners = new ArrayList<>();

    @Override
    protected void generateViews() {
        views.put(VIEW_LIST, new EntryView() {

            @Override
            public void readFields() {
            }

            @Override
            public void addToTable(Table table) {
                if (label == null) {
                    label = new VisLabel(getPropertyName());
                    checkBox = new VisCheckBox("");
                }

                ParamSupplier supplier = paramContainer.getParam(propertyUID);
                if (supplier != null)
                    checkBox.setChecked(supplier.getGeneral(Boolean.class));

                for (ChangeListener listener : listeners)
                    checkBox.addListener(listener);
                contentFields.add(checkBox);

                table.add(label).left();
                table.add();
                table.add(checkBox).left().padBottom(2).row();
            }

            @Override
            public void removeFromTable() {
                label.remove();
                checkBox.remove();
                contentFields.remove(checkBox);
            }
        });
    }

    @Override
    public ParamSupplier getSupplier() {
        return new StaticParamSupplier(propertyUID, checkBox == null ? false : checkBox.isChecked());
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

    @Override
    protected boolean checkValue(Object valueObj) {
        return valueObj instanceof Boolean;
    }

    @Override
    protected void setCheckedValue(Object newValue) {
        checkBox.setChecked((Boolean)newValue);
    }

    @Override
    protected Object getDefaultObject() {
        return null;
    }
}
