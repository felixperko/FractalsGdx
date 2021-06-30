package de.felixp.fractalsgdx.rendering.orbittrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.parameters.attributes.ParamAttribute;

public abstract class AbstractOrbittrap implements Orbittrap, Serializable {

    int id;
    String name;

    List<ParamAttribute> attrs = new ArrayList<>();

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

    @Override
    public List<ParamAttribute> getParamAttributes() {
        return attrs;
    }

    @Override
    public ParamAttribute getParamAttribute(String attrName) {
        for (ParamAttribute attr : attrs)
            if (attr.getName().equalsIgnoreCase(attrName))
                return attr;
        return null;
    }
}
