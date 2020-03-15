package de.felixp.fractalsgdx;

import com.badlogic.gdx.graphics.g2d.Batch;

import de.felixperko.fractals.system.systems.infra.SystemContext;

interface Renderer {
    void init();

    void draw(Batch batch, float parentAlpha);

    void setScreenshot(boolean screenshot);
    boolean isScreenshot(boolean reset);

    public SystemContext getSystemContext();
}
