package de.felixp.fractalsgdx.ui.propertyattribute;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.ui.actors.TraversableGroup;
import de.felixp.fractalsgdx.ui.actors.TabTraversableTextField;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.attributes.ParamAttribute;

public abstract class AbstractPropertyAttributeAdapterUI<T> implements PropertyAttributeAdapterUI<T> {

    public static PropertyAttributeAdapterUI getAdapterUI(ParamAttribute attr, NumberFactory nf){
        if (attr.getAttributeClass().isAssignableFrom(Number.class)){
            return new NumberPropertyAttributeAdapterUI(attr.getName(), nf, (Number) attr.getValue(),
                    (Number) attr.getMinValue(), (Number) attr.getMaxValue()){
                @Override
                public void valueChanged(Number newVal, Number min, Number max) {
                    attr.applyValue(newVal);
                    attr.setRange(min, max);
                }
            };
        }
        else if (attr.getAttributeClass().isAssignableFrom(ComplexNumber.class)){
            return new ComplexNumberPropertyAttributeAdapterUI(attr.getName(), nf, (ComplexNumber) attr.getValue()){
                @Override
                public void valueChanged(ComplexNumber newVal, ComplexNumber min, ComplexNumber max) {
                    attr.applyValue(newVal);
                    attr.setRange(min, max);
                }
            };
        }
        return null;
    }

    protected TraversableGroup traversableGroup;
    List<Actor> registeredFields = new ArrayList<>();

    public TraversableGroup getTraversableGroup(){
        return traversableGroup;
    }

    @Override
    public void setTraversableGroup(TraversableGroup traversableGroup) {
        this.traversableGroup = traversableGroup;
    }

    protected void registerField(Actor field){
        registeredFields.add(field);
        if (traversableGroup != null && field instanceof TabTraversableTextField)
            traversableGroup.addField((TabTraversableTextField)field);
    }

    @Override
    public void unregisterFields() {
        for (Actor actor : registeredFields){
            if (actor instanceof TabTraversableTextField){
                traversableGroup.removeField((TabTraversableTextField)actor);
            }
        }
        registeredFields.clear();
    }

    public abstract void addListenerToFields(EventListener listener);
}
