package de.felixp.fractalsgdx.rendering.orbittrap;

import java.util.List;

import de.felixperko.fractals.system.parameters.attributes.ParamAttribute;

public interface Orbittrap {
    int getId();
    String getName();
    void setName(String name);
    String getTypeName();
    public List<ParamAttribute> getParamAttributes();
    public Orbittrap copy();
}
