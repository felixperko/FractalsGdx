package de.felixp.fractalsgdx.rendering.orbittrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.attributes.NumberParamAttribute;
import de.felixperko.fractals.system.parameters.attributes.ParamAttribute;

public class AxisOrbittrap extends AbstractOrbittrap{

    Number num_360;
    Number num_90;

    List<ParamAttribute> attrs = new ArrayList<>();
    NumberFactory nf;
    Number offset;
    Number width;
    Number angle;

    public AxisOrbittrap(int id, NumberFactory nf, Number offset, Number width, Number angle) {
        super(id, "Line trap #"+id);
        this.nf = nf;
        this.num_360 = nf.createNumber("360.0");
        this.num_90 = nf.createNumber("90.0");
        this.offset = offset;
        this.width = width;
        setAngle(angle);
        attrs.add(new NumberParamAttribute(name, "width") {
            @Override
            public Number<?> getValue() {
                return getWidth();
            }

            @Override
            public void applyValue(Object number) {
                setWidth((Number)number);
            }
        });
        attrs.add(new NumberParamAttribute(name, "offset") {
            @Override
            public Number<?> getValue() {
                return getOffset();
            }

            @Override
            public void applyValue(Object number) {
                setOffset((Number)number);
            }
        });
        attrs.add(new NumberParamAttribute(name, "angle") {
            @Override
            public Number<?> getValue() {
                return getAngle();
            }

            @Override
            public void applyValue(Object number) {
                setAngle((Number)number);
            }
        });
    }

    @Override
    public String getTypeName() {
        return "line";
    }

    @Override
    public List<ParamAttribute> getParamAttributes() {
        return attrs;
    }

    @Override
    public Orbittrap copy() {
        AxisOrbittrap axisOrbittrap = new AxisOrbittrap(id, nf, offset.copy(), width.copy(), angle.copy());
        axisOrbittrap.setName(getName());
        return axisOrbittrap;
    }

    public Number getOffset() {
        return offset;
    }

    public void setOffset(Number offset) {
        this.offset = offset;
    }

    public Number getWidth() {
        return width;
    }

    public void setWidth(Number width) {
        this.width = width;
    }

    public Number getAngle() {
        return angle;
    }

    public void setAngle(Number angle) {
        Number correctedAngle = angle.copy();
        correctedAngle.mod(num_360);
        this.angle = correctedAngle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AxisOrbittrap that = (AxisOrbittrap) o;
        return  Objects.equals(offset, that.offset) &&
                Objects.equals(width, that.width) &&
                Objects.equals(angle, that.angle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, width, angle);
    }
}
