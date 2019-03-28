package de.felixp.fractalsgdx;

import com.badlogic.gdx.graphics.g2d.Batch;

interface Renderer {
    void init();

    void draw(Batch batch, float parentAlpha);
}
