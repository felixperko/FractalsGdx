package de.felixp.fractalsgdx.ui.propertyattribute;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.Focusable;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisRadioButton;
import com.kotcrab.vis.ui.widget.VisTable;

public class BooleanPropertyAttributeAdapterUI extends AbstractPropertyAttributeAdapterUI<Boolean> {

    String name;
    String option1Name, option2Name;
    boolean startVal;

    public BooleanPropertyAttributeAdapterUI(String name, String option1Name, String option2Name, boolean startVal) {
        this.name = name;
        this.option1Name = option1Name;
        this.option2Name = option2Name;
        this.startVal = startVal;
    }

    @Override
    public Actor addToTable(Table table) {

        Table innerTable = new VisTable(true);

        VisLabel nameLbl = new VisLabel(name);
        VisRadioButton o1Btn = new VisRadioButton(option1Name);
        VisRadioButton o2Btn = new VisRadioButton(option2Name);
        new ButtonGroup<>(o1Btn, o2Btn);
        if (startVal)
            o1Btn.setChecked(true);
        else
            o2Btn.setChecked(true);

        innerTable.add(nameLbl).left();
        innerTable.add(o1Btn);
        innerTable.add(o2Btn);

        o1Btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                valueChanged(o1Btn.isChecked(), null, null);
            }
        });
        o2Btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                valueChanged(!o2Btn.isChecked(), null, null);
            }
        });

        table.add(innerTable).left().row();
        return innerTable;
    }

    @Override
    public void valueChanged(Boolean newVal, Boolean min, Boolean max) {

    }

    @Override
    public void addListenerToFields(EventListener listener) {
    }

    @Override
    public Actor getFirstFocusable() {
        return null;
    }
}
