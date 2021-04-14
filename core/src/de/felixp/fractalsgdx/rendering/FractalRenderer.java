package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.graphics.g2d.Batch;

import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.systems.infra.SystemContext;

public interface FractalRenderer {

    int getId();

    RendererContext getRendererContext();

    void init();

    void draw(Batch batch, float parentAlpha);

    float getScreenX(double real);
    float getScreenY(double imag);
    float getScreenX(Number real);
    float getScreenY(Number imag);
    ComplexNumber getComplexMapping(float screenX, float screenY);
    Number getReal(float screenX);
    Number getImag(float screenY);

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

    void setRelativeX(float x);

    void setRelativeY(float y);

    void setRelativeWidth(float w);

    void setRelativeHeight(float h);

    void setOrientation(int orientation);

    float getRelativeX();

    float getRelativeY();

    float getRelativeWidth();

    float getRelativeHeight();

    int getOrientation();

    void reset();

    void removed();
}
