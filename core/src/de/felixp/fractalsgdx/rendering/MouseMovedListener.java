package de.felixp.fractalsgdx.rendering;

import de.felixperko.fractals.system.numbers.ComplexNumber;

public interface MouseMovedListener {
    void moved(float screenX, float screenY, ComplexNumber mappedValue);
}
