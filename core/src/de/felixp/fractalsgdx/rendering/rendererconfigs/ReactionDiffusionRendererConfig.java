package de.felixp.fractalsgdx.rendering.rendererconfigs;

import de.felixp.fractalsgdx.rendering.rendererparams.DrawParamsReactionDiffusion;
import de.felixp.fractalsgdx.rendering.renderers.FractalRenderer;
import de.felixp.fractalsgdx.rendering.renderers.ReactionDiffusionRenderer;
import de.felixp.fractalsgdx.rendering.RendererContext;
import de.felixp.fractalsgdx.rendering.RendererProperties;
import de.felixperko.fractals.data.ParamContainer;

public class ReactionDiffusionRendererConfig extends AbstractRendererConfig {

    FractalRenderer renderer;
    FractalRenderer renderer2;

    ParamContainer paramContainer = new DrawParamsReactionDiffusion().getParamContainer();

    @Override
    public boolean createRenderers() {
        if (renderer != null)
            return false;
        renderer = new ReactionDiffusionRenderer(
                new RendererContext(0, 0, 1, 1, RendererProperties.ORIENTATION_FULLSCREEN)
        );
        renderers.add(renderer);
//        renderer2 = new ReactionDiffusionRenderer(
//                new RendererContext(0.05f, 0.05f, 0.3f, 0.3f, RendererProperties.ORIENTATION_BOTTOM_RIGHT)
//        );
//        renderers.add(renderer2);
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
