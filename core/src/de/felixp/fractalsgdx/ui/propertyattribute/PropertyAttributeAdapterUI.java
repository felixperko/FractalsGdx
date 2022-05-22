package de.felixp.fractalsgdx.ui.propertyattribute;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import de.felixp.fractalsgdx.ui.actors.TraversableGroup;

public interface PropertyAttributeAdapterUI<T> {

    /**
     * Adds the needed actors to the table.
     * returns the actor that needs to be removed to clear the table entry.
     * @param table
     * @return
     */
    Actor addToTable(Table table);

    void valueChanged(T newVal, T min, T max);

    void setTraversableGroup(TraversableGroup traversableGroup);

    void unregisterFields();

    void addListenerToFields(EventListener eventListener);
}
