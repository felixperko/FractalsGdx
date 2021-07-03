package de.felixp.fractalsgdx.rendering.rendererlink;

import de.felixp.fractalsgdx.rendering.FractalRenderer;

public interface RendererLink {

    void syncTargetRenderer();

    FractalRenderer getSourceRenderer();
    void setSourceRenderer(FractalRenderer renderer);

    FractalRenderer getTargetRenderer();
    void setTargetRenderer(FractalRenderer renderer);

    void switchRenderers();
}
