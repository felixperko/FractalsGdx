package de.felixp.fractalsgdx.rendering.orbittrap;

import java.util.List;

import de.felixperko.fractals.system.parameters.attributes.ParamAttribute;

public interface Orbittrap {

    int getId();

    String getName();
    void setName(String name);

    String getTypeName();

    List<ParamAttribute> getParamAttributes();
    ParamAttribute getParamAttribute(String attrName);

    Orbittrap copy();
}
