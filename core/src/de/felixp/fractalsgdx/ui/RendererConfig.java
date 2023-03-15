package de.felixp.fractalsgdx.ui;

import java.util.List;

import de.felixp.fractalsgdx.rendering.renderers.FractalRenderer;
import de.felixperko.fractals.data.ParamContainer;

public interface RendererConfig {

    boolean createRenderers();
    boolean initRenderers();

    void addRenderers(MainStage mainStage);
    void removeRenderers(MainStage mainStage);

    void switchRenderers();

    List<FractalRenderer> getRenderers();

    ParamContainer getDrawParamContainer();
}
