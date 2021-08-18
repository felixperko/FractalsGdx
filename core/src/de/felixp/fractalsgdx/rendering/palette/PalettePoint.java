package de.felixp.fractalsgdx.rendering.palette;

import com.badlogic.gdx.graphics.Color;

public class PalettePoint implements Comparable<PalettePoint>{

    Color color;
    double relativePos;

    public PalettePoint(Color color, double relativePos) {
        this.color = color;
        this.relativePos = relativePos;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public double getRelativePos() {
        return relativePos;
    }

    public void setRelativePos(double relativePos) {
        this.relativePos = relativePos;
    }

    @Override
    public int compareTo(PalettePoint other) {
        return Double.compare(relativePos, other.relativePos);
    }

    public PalettePoint copy() {
        return new PalettePoint(color.cpy(), relativePos);
    }
}
