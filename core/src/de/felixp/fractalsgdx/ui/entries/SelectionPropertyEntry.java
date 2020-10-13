package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.Tooltip;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;

import java.util.ArrayList;
import java.util.List;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.infra.Selection;

public class SelectionPropertyEntry extends AbstractPropertyEntry {

    Selection<?> selection;

    public SelectionPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition) {

        super(node, paramContainer, parameterDefinition);

        selection = parameterDefinition.getConfiguration().getSelection(propertyName);
    }

    String selectedValue = null;

    List<Actor> contentFields = new ArrayList<Actor>();
    List<ChangeListener> listeners = new ArrayList<>();

    @Override
    protected void generateViews() {
        views.put(VIEW_LIST, new EntryView() {

            private VisLabel label;
            private SelectBox box;

            @Override
            public void addToTable(Table table) {

                label = new VisLabel(propertyName);
                box = new VisSelectBox();

//                String tooltipText = parameterDefinition.getDescription();
//                if (tooltipText != null)
//                    new Tooltip.Builder(tooltipText).target(box).build();

                Array arr = new Array();
                for (String option : selection.getOptionNames())
                    arr.add(option);
                box.setItems(arr);
                ParamSupplier supplier = paramContainer.getClientParameter(propertyName);
                if (supplier == null)
                    throw new IllegalArgumentException("Parameter missing: "+propertyName);
                Object current = supplier.getGeneral();
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
                for (ChangeListener listener : listeners)
                    box.addListener(listener);
                contentFields.add(box);

                table.add(label).left().padRight(3);
                table.add();
                table.add(box).fillX().expandX().padBottom(2).row();
            }

            @Override
            public void removeFromTable() {
                label.remove();
                box.remove();
                contentFields.remove(box);
            }
        });
    }

    @Override
    public ParamSupplier getSupplier() {
        StaticParamSupplier staticParamSupplier = new StaticParamSupplier(propertyName, selection.getOption(selectedValue));
        staticParamSupplier.setSystemRelevant(true); //TODO only set system relevant if it really is!
        return staticParamSupplier;
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
