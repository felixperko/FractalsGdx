package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;

import java.util.ArrayList;

import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.Selection;

public class SelectionPropertyEntry extends AbstractPropertyEntry {

    Selection<?> selection;

    public SelectionPropertyEntry(VisTable table, SystemClientData systemClientData, ParameterDefinition parameterDefinition) {

        super(table, systemClientData, parameterDefinition);

        selection = parameterDefinition.getConfiguration().getSelection(propertyName);
    }

    String selectedValue = null;

    @Override
    protected void generateViews() {
        views.put(VIEW_LIST, new EntryView() {

            private VisLabel label;
            private VisSelectBox box;

            @Override
            public void drawOnTable(Table table) {

                label = new VisLabel(propertyName);
                box = new VisSelectBox();

                Array arr = new Array();
                for (String option : selection.getOptionNames())
                    arr.add(option);
                box.setItems(arr);
                Object current = systemClientData.getClientParameter(propertyName).get(0, 0);
                String currentName = null;
                for (String s : selection.getOptionNames()){
                    Object obj = selection.getOption(s);
                    if (obj.equals(current))
                        currentName = s;
                }
                selectedValue = currentName;
                box.setSelected(currentName);

                box.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        selectedValue = (String)box.getSelected();
                    }
                });

                table.add(label);
                table.add(box).padBottom(2).row();
            }

            @Override
            public void removeFromTable() {
                label.remove();
                box.remove();
            }
        });
    }

    @Override
    protected ParamSupplier getSupplier() {
        StaticParamSupplier staticParamSupplier = new StaticParamSupplier(propertyName, selection.getOption(selectedValue));
        staticParamSupplier.setSystemRelevant(true);
        return staticParamSupplier;
    }
}
