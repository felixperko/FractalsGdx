package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.rendering.FractalRenderer;

public abstract class AbstractRendererConfig implements RendererConfig{

    List<FractalRenderer> renderers = new ArrayList<>();

    boolean renderersInitialized = false;

    @Override
    public void addRenderers(MainStage mainStage) {
        for (FractalRenderer renderer : renderers)
            mainStage.addFractalRenderer(renderer);
    }

    @Override
    public void removeRenderers(MainStage mainStage) {
        for (FractalRenderer renderer : renderers)
            mainStage.removeFractalRenderer(renderer);
    }

    @Override
    public boolean initRenderers() {
        if (renderersInitialized)
            return false;
        renderersInitialized = true;

        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                for (FractalRenderer renderer : renderers)
                    renderer.initRenderer();
            }
        });
        return true;
    }

    @Override
    public List<FractalRenderer> getRenderers() {
        return renderers;
    }
}
