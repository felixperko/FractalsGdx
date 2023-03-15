package de.felixp.fractalsgdx.rendering.links;

import de.felixp.fractalsgdx.rendering.renderers.FractalRenderer;

public interface RendererLink {

    void syncTargetRenderer();

    FractalRenderer getSourceRenderer();
    void setSourceRenderer(FractalRenderer renderer);

    FractalRenderer getTargetRenderer();
    void setTargetRenderer(FractalRenderer renderer);

    void switchRenderers();
}
