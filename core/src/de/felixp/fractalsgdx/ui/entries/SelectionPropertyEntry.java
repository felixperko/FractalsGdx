package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
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

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.infra.Selection;

public class SelectionPropertyEntry extends AbstractPropertyEntry {

    Selection<?> selection;

    public SelectionPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, boolean submitValue) {

        super(node, paramContainer, parameterDefinition, submitValue);

        selection = parameterDefinition.getConfiguration().getSelection(propertyName);

        ParamSupplier clientParameter = paramContainer.getClientParameter(propertyName);
        if (clientParameter != null)
            selectedValue = clientParameter.getGeneral().toString();
    }

    Object selectedValue = null;

    List<Actor> contentFields = new ArrayList<Actor>();
    List<ChangeListener> listeners = new ArrayList<>();

    @Override
    protected void generateViews() {
        views.put(VIEW_LIST, new EntryView() {

            private VisLabel label;
            private SelectBox box;

            @Override
            public void readFields() {
                selectedValue = selection.getOption(box.getSelected().toString());
                applyClientValue();
            }

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
                if (selectedValue == null && box.getSelected() != null)
                    selectedValue = box.getSelected();
                ParamSupplier supplier = paramContainer.getClientParameter(propertyName);
                if (supplier == null)
                    throw new IllegalArgumentException("Parameter missing: " + propertyName);
                contentFields.add(box);
                Object newSelectedObj = supplier.getGeneral();
                if (checkValue(newSelectedObj))
                    setCheckedValue(newSelectedObj);
                else if (newSelectedObj != null)
                    throw new IllegalArgumentException("Parameter invalid: '" + propertyName + "' selected: " + newSelectedObj.toString());

                box.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        readFields();
                        ((MainStage)FractalsGdxMain.stage).resetKeyboardFocus();
                    }
                });
//                for (String name : selection.getOptionNames()){
//                    if (name.equalsIgnoreCase(selectedValue)) {
//                        box.setSelected(name);
//                        break;
//                    }
//                }
                for (ChangeListener listener : listeners)
                    box.addListener(listener);

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
        StaticParamSupplier staticParamSupplier = new StaticParamSupplier(propertyName, selectedValue);
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

    @Override
    protected boolean checkValue(Object valueObj) {
        for (String optionName : selection.getOptionNames()){
            if (selection.getOption(optionName).equals(valueObj))
                return true;
        }
        try {
            int index = Integer.parseInt(valueObj.toString());
            return index >= 0 && index < selection.getOptionNames().size();
        } catch (NumberFormatException e){
        }
        return false;
    }

    @Override
    protected void setCheckedValue(Object newSelectedObj) {
        String newSelectedName = null;
        Object newSelectedValue = null;
        for (String s : selection.getOptionNames()){
            Object obj = selection.getOption(s);
            if (obj.equals(newSelectedObj)) {
                newSelectedName = s;
                newSelectedValue = obj;
            }
        }

        if (newSelectedValue == null){
            try {
                int index = Integer.parseInt(newSelectedObj.toString());
                newSelectedName = selection.getOptionNames().get(index);
                newSelectedValue = selection.getOption(newSelectedName);
            } catch (NumberFormatException e){
            }
        }

        if (newSelectedValue != null) {
            selectedValue = newSelectedValue;
            for (Actor contentField : contentFields)
                ((VisSelectBox) contentField).setSelected(newSelectedName);
        }
    }

    @Override
    protected Object getDefaultObject() {
        return null;
    }
}
