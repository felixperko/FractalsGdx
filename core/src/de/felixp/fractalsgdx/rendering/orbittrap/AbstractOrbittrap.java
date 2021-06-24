package de.felixp.fractalsgdx.rendering.orbittrap;

import java.io.Serializable;

import de.felixperko.fractals.system.numbers.Number;

public abstract class AbstractOrbittrap implements Orbittrap, Serializable {

    int id;
    String name;

    public AbstractOrbittrap(int id, String name){
        this.id = id;
        this.name = name;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
