package de.felixp.fractalsgdx.rendering;

import de.felixperko.fractals.system.numbers.ComplexNumber;

public interface ClickedListener {
    void clicked(float mouseX, float mouseY, int button, ComplexNumber mappedValue);
}
