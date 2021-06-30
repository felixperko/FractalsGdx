package de.felixp.fractalsgdx.rendering.valuereference;

import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.parameters.attributes.ParamAttribute;

public class ParamAttributeValueReference implements ValueReference {

    ParamAttribute attribute;
    boolean realPart;
    boolean imagPart;

    public ParamAttributeValueReference(ParamAttribute attribute){
        this.attribute = attribute;
    }

    public ParamAttributeValueReference(ParamAttribute attribute, boolean realPart, boolean imagPart){
        this.attribute = attribute;
        this.realPart = realPart;
        this.imagPart = imagPart;
    }

    @Override
    public Object getValue() {
        Object obj = attribute.getValue();
        if (obj instanceof ComplexNumber){
            if (realPart && !imagPart)
                return ((ComplexNumber)obj).getReal();
            if (imagPart && !realPart)
                return ((ComplexNumber)obj).getImag();
            return obj;
        }
        return obj;
    }
}
