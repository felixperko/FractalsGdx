package de.felixp.fractalsgdx.rendering.rendererconfigs;

import de.felixp.fractalsgdx.rendering.rendererparams.DrawParamsTurtleGraphics;
import de.felixp.fractalsgdx.rendering.renderers.FractalRenderer;
import de.felixp.fractalsgdx.rendering.RendererContext;
import de.felixp.fractalsgdx.rendering.RendererProperties;
import de.felixp.fractalsgdx.rendering.renderers.TurtleGraphicsRenderer;
import de.felixperko.fractals.data.ParamContainer;

public class TurtleGraphicsRendererConfig extends AbstractRendererConfig {

    FractalRenderer turtleRenderer;

    ParamContainer drawParamContainer = new DrawParamsTurtleGraphics().getParamContainer();

    @Override
    public boolean createRenderers() {
        if (turtleRenderer != null)
            return false;
        turtleRenderer = new TurtleGraphicsRenderer(
                new RendererContext(0, 0, 1, 1, RendererProperties.ORIENTATION_FULLSCREEN)
        );
        renderers.add(turtleRenderer);
        return true;
    }

    @Override
    public void switchRenderers() {

    }

    @Override
    public ParamContainer getDrawParamContainer() {
        return drawParamContainer;
    }
}
