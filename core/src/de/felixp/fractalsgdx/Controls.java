package de.felixp.fractalsgdx;

public interface Controls {
    void updatePosition(double deltaX, double deltaY);
    void updateZoom(double zoom_factor);
    void incrementJobId();
}
