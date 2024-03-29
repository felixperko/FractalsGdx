package de.felixp.fractalsgdx.animation;

import java.util.Map;

import de.felixp.fractalsgdx.animation.AnimationListener;
import de.felixp.fractalsgdx.animation.interpolations.ParamInterpolation;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.NumberFactory;

public interface ParamAnimation {

    String getName();

    double getLoopProgress();

    double getTimeFactor();
    void setTimeFactor(double parseDouble);

    boolean addAnimationListener(AnimationListener animationListener);

    boolean removeAnimationListener(Object o);

    void clearAnimationListeners();

    double getTimeProgress();
    void setTimeProgress(double progress);

    void setProgress(double progress);
    void updateProgress();
    boolean isPaused();
    void setPaused(boolean paused);

    int getFrameCount();
    void setFrameCount(int totalFrames);
    int getFrameCounter();
    void setFrameCounter(int frame);
    void incrementFrameCounter();

    int getAnimationFrameCount(int targetFramerateIfTimeBased);

    boolean isUsingTimeBasedProgress();
    void setUsingTimeBasedProgress();

    boolean isUsingFrameBasedProgress();
    void setUsingFrameBasedProgress();

    ParamInterpolation getInterpolation(String paramName, String attributeName);
    Map<String, ParamInterpolation> getInterpolations();
    void setInterpolation(ParamInterpolation interpolation);
    ParamInterpolation removeInterpolation(String paramName, String attributeName);

    void addKeyframe(ParamContainer paramContainer);

    void changeInterpolationParamName(ParamInterpolation interpolation, String newParamUid, String newParamName, String paramType, String paramContainer, String newAttrUid, String newAttrName);

    boolean isApplyValue();
    void setSingleApply();
}
