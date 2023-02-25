package de.felixp.fractalsgdx.ui;

import de.felixp.fractalsgdx.params.ClientParamsEscapeTime;
import de.felixp.fractalsgdx.params.DrawParamsReactionDiffusion;
import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.rendering.ReactionDiffusionRenderer;
import de.felixp.fractalsgdx.rendering.RendererContext;
import de.felixp.fractalsgdx.rendering.RendererProperties;
import de.felixp.fractalsgdx.rendering.TurtleGraphicsRenderer;
import de.felixperko.fractals.data.ParamContainer;

public class ReactionDiffusionRendererConfig extends AbstractRendererConfig {

    FractalRenderer renderer;

    ParamContainer paramContainer = new DrawParamsReactionDiffusion().getParamContainer();

    @Override
    public boolean createRenderers() {
        if (renderer != null)
            return false;
        renderer = new ReactionDiffusionRenderer(
                new RendererContext(0, 0, 1, 1, RendererProperties.ORIENTATION_FULLSCREEN)
        );
        renderers.add(renderer);
        return true;
    }

    @Override
    public void switchRenderers() {

    }

    @Override
    public ParamContainer getDrawParamContainer() {
        return paramContainer;
    }
}
