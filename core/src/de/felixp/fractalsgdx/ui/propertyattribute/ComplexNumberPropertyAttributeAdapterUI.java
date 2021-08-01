package de.felixp.fractalsgdx.ui.propertyattribute;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;

import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.NumberFactory;

public class ComplexNumberPropertyAttributeAdapterUI extends AbstractPropertyAttributeAdapterUI<ComplexNumber> {
    String name;
    NumberFactory nf;
    ComplexNumber startVal;

    public ComplexNumberPropertyAttributeAdapterUI(String name, NumberFactory numberFactory, ComplexNumber startVal){
        this.name = name;
        this.nf = numberFactory;
        this.startVal = startVal;
    }

    @Override
    public Actor addToTable(Table table) {
        VisTable innerTable = new VisTable(true);
        VisLabel nameLbl = new VisLabel(name);
        VisTextField valueField = new VisTextField(startVal.getReal().toString());
        VisTextField valueField2 = new VisTextField(startVal.getImag().toString());
        valueField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try{
                    ComplexNumber newVal = nf.createComplexNumber(valueField.getText(), valueField2.getText());
                    valueChanged(newVal);
                } catch (NumberFormatException e){

                }
            }
        });
        valueField2.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try{
                    ComplexNumber newVal = nf.createComplexNumber(valueField.getText(), valueField2.getText());
                    valueChanged(newVal);
                } catch (NumberFormatException e){

                }
            }
        });

        innerTable.add(nameLbl).left();
        innerTable.add(valueField);
        innerTable.add(valueField2);
        table.add(innerTable).left().row();
        return innerTable;
    }

    @Override
    public void valueChanged(ComplexNumber newVal) {

    }
}