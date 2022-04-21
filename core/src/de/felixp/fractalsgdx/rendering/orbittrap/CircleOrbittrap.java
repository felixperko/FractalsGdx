package de.felixp.fractalsgdx.rendering.orbittrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.parameters.attributes.ComplexNumberParamAttribute;
import de.felixperko.fractals.system.parameters.attributes.NumberParamAttribute;
import de.felixperko.fractals.system.parameters.attributes.ParamAttribute;

public class CircleOrbittrap extends AbstractOrbittrap{

    ComplexNumber center;
    Number radius;

    public CircleOrbittrap(int id, ComplexNumber center, Number radius) {
        super(id, "Circle trap #"+id);
        this.center = center;
        this.radius = radius;
        attrs.add(new ComplexNumberParamAttribute(name, "center") {
            @Override
            public ComplexNumber getValue() {
                return getCenter();
            }

            @Override
            public void applyValue(Object number) {
                setCenter((ComplexNumber)number);
            }
        });
        attrs.add(new NumberParamAttribute(name, "radius") {
            @Override
            public Number<?> getValue() {
                return getRadius();
            }

            @Override
            public void applyValue(Object number) {
                setRadius((Number)number);
            }
        });
    }

    @Override
    public String getTypeName() {
        return "circle";
    }

    @Override
    public List<ParamAttribute> getParamAttributes() {
        return attrs;
    }

    @Override
    public Orbittrap copy() {
        CircleOrbittrap circleOrbittrap = new CircleOrbittrap(id, center.copy(), radius.copy());
        circleOrbittrap.setName(getName());
        return circleOrbittrap;
    }

    public ComplexNumber getCenter() {
        return center;
    }

    public void setCenter(ComplexNumber center) {
        this.center = center;
    }

    public Number getRadius() {
        return radius;
    }

    public void setRadius(Number radius) {
        this.radius = radius;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CircleOrbittrap that = (CircleOrbittrap) o;
        return Objects.equals(center, that.center) &&
                Objects.equals(radius, that.radius);
    }

    @Override
    public int hashCode() {
        return Objects.hash(center, radius);
    }
}
