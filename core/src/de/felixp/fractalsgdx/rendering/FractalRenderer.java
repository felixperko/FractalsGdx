package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.graphics.g2d.Batch;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.systems.infra.SystemContext;

public interface FractalRenderer {

    int getId();

    RendererContext getRendererContext();

    void init();

    void draw(Batch batch, float parentAlpha);

    abstract void setSingleScreenshotScheduled(boolean singleScreenshotScheduled);

    void setScreenshotRecording(boolean screenshotRecording);

    abstract boolean isScreenshot(boolean reset);

    public SystemContext getSystemContext();

    void setRefresh();

    float getPrefWidth();

    float getPrefHeight();

    float getPrefX();

    float getPrefY();

    void updateSize();

    void addScreenshotListener(ScreenshotListener screenshotListener, boolean singleUse);

    boolean removeScreenshotListener(ScreenshotListener screenshotListener);

    void addPanListener(PanListener panListener);

    void removePanListener(PanListener panListener);

    void applyParameterAnimations(ParamContainer serverParamContainer, ParamContainer clientParamContainer, NumberFactory numberFactory);

    void setRelativeX(float v);

    void setRelativeY(float v);

    void setRelativeWidth(float v);

    void setRelativeHeight(float v);

    float getRelativeX();

    float getRelativeY();

    float getRelativeWidth();

    float getRelativeHeight();

    void reset();

    void removed();
}
