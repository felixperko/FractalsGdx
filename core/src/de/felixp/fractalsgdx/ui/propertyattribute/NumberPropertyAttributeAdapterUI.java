package de.felixp.fractalsgdx.ui.propertyattribute;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;

import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;

public class NumberPropertyAttributeAdapterUI extends AbstractPropertyAttributeAdapterUI<Number> {

    String name;
    NumberFactory nf;
    Number startVal;

    public NumberPropertyAttributeAdapterUI(String name, NumberFactory numberFactory, Number startVal){
        this.name = name;
        this.nf = numberFactory;
        this.startVal = startVal;
    }

    @Override
    public Actor addToTable(Table table) {
        VisTable innerTable = new VisTable(true);
        VisLabel nameLbl = new VisLabel(name);
        VisTextField valueField = new VisTextField(startVal.toString());
        valueField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try{
                    Number newVal = nf.createNumber(valueField.getText());
                    valueChanged(newVal);
                } catch (NumberFormatException e){

                }
            }
        });

        innerTable.add(nameLbl).left();
        innerTable.add(valueField).left();
        table.add(innerTable).left().row();
        return innerTable;
    }

    @Override
    public void valueChanged(Number newVal) {

    }
}
