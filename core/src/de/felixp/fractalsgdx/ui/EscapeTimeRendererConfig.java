package de.felixp.fractalsgdx.ui;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.params.ClientParamsEscapeTime;
import de.felixp.fractalsgdx.params.DrawParamsTurtleGraphics;
import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.rendering.RendererContext;
import de.felixp.fractalsgdx.rendering.RendererProperties;
import de.felixp.fractalsgdx.rendering.ShaderRenderer;
import de.felixp.fractalsgdx.rendering.rendererlink.JuliasetRendererLink;
import de.felixp.fractalsgdx.rendering.rendererlink.RendererLink;
import de.felixperko.fractals.data.ParamContainer;

public class EscapeTimeRendererConfig extends AbstractRendererConfig{

    FractalRenderer renderer;
    FractalRenderer renderer2;

    RendererLink juliasetLink;

    @Override
    public boolean createRenderers() {
        if (renderer != null)
            return false;
        renderer = new ShaderRenderer(
                new RendererContext(0, 0, 1f, 1, RendererProperties.ORIENTATION_FULLSCREEN)
        );
        renderer2 = new ShaderRenderer(
                new RendererContext(0.05f, 0.05f, 0.3f, 0.3f, RendererProperties.ORIENTATION_BOTTOM_RIGHT)
        );
        renderers.add(renderer);
        renderers.add(renderer2);
        return true;
    }

    @Override
    public boolean initRenderers() {
        if (super.initRenderers()) {
            juliasetLink = new JuliasetRendererLink(renderer, renderer2);
            juliasetLink.syncTargetRenderer();
            return true;
        }
        return false;
    }

    @Override
    public void switchRenderers() {
        juliasetLink.switchRenderers();
    }

    @Override
    public ParamContainer getDrawParamContainer() {
        return ClientParamsEscapeTime.getParamContainer(MainStage.palettes);
    }
}
