package de.felixp.fractalsgdx.ui.propertyattribute;

import com.badlogic.gdx.scenes.scene2d.Actor;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.ui.actors.TraversableGroup;
import de.felixp.fractalsgdx.ui.actors.TabTraversableTextField;

public abstract class AbstractPropertyAttributeAdapterUI<T> implements PropertyAttributeAdapterUI<T> {

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
}
