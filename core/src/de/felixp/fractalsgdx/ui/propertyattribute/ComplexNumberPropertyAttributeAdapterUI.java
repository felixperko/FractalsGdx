package de.felixp.fractalsgdx.ui.propertyattribute;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixp.fractalsgdx.ui.actors.TabTraversableTextField;
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
        TabTraversableTextField valueField = new TabTraversableTextField(startVal.getReal().toString());
        TabTraversableTextField valueField2 = new TabTraversableTextField(startVal.getImag().toString());
        registerField(valueField);
        registerField(valueField2);
        valueField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try{
                    ComplexNumber newVal = nf.createComplexNumber(valueField.getText(), valueField2.getText());
                    valueChanged(newVal, null, null);
                } catch (NumberFormatException e){

                }
            }
        });
        valueField2.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try{
                    ComplexNumber newVal = nf.createComplexNumber(valueField.getText(), valueField2.getText());
                    valueChanged(newVal, null, null);
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
    public void valueChanged(ComplexNumber newVal, ComplexNumber min, ComplexNumber max) {

    }

}
