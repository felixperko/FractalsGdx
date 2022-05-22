package de.felixp.fractalsgdx.animation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import de.felixp.fractalsgdx.animation.interpolations.ParamInterpolation;
import de.felixperko.fractals.util.NumberUtil;

abstract class AbstractParamAnimation<T> implements ParamAnimation {

    private Map<String, ParamInterpolation> interpolations = new LinkedHashMap<>();

    private List<AnimationListener> animationListeners = new CopyOnWriteArrayList<>();

    private String name;

    private double timeFactor = 10;

    private long lastTimeProgressUpdate = -1;
    private double progress = 0.0;
    private boolean frameBasedProgress = true;
    private int frameCounter = 0;
    private int frameCount = 600;

    private boolean paused = true;
    private boolean repeating = true;
    private boolean returnBack = false;

    private boolean singleApply = false;

    public AbstractParamAnimation(String name){
        this.name = name;
    }

    /**
     * sets either time progress or frame progress depending on which is enabled.
     * @param relativeProgress
     */
    public void setProgress(double relativeProgress){
        if (isUsingTimeBasedProgress())
            setTimeProgress(relativeProgress*timeFactor);
        else
            setFrameCounter((int)(frameCount*relativeProgress));
        setSingleApply();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public void setSingleApply() {
        singleApply = true;
    }

    @Override
    public boolean isApplyValue() {
        if (!isPaused())
            return true;
        boolean singleApply = this.singleApply;
        this.singleApply = false;
        return singleApply;
    }

    @Override
    public void updateProgress() {
        if (paused)
            return;
        if (frameBasedProgress){
            setFrameCounter(frameCounter+1);
            return;
        }
        if (lastTimeProgressUpdate == -1)
            lastTimeProgressUpdate = System.nanoTime();
        long newT = System.nanoTime();
        long deltaTinNS = newT-lastTimeProgressUpdate;
        double deltaTinS = deltaTinNS*NumberUtil.NS_TO_S;
        lastTimeProgressUpdate = newT;
        setTimeProgress(progress+deltaTinS);
    }

    @Override
    public int getAnimationFrameCount(int targetFramerateIfTimeBased) {
        if (isUsingFrameBasedProgress())
            return frameCount;
        else
            return (int) (timeFactor*targetFramerateIfTimeBased);
    }

//    @Override
//    public T getInterpolatedValue(double progress, NumberFactory nf) {
//        double loopProgress = getLoopProgress();
//        this.progress = loopProgress;
//        return getInterpolatedValueInLoop(loopProgress, nf);
//    }

//    public abstract T getInterpolatedValueInLoop(double progressInLoop, NumberFactory nf);

    @Override
    public double getLoopProgress() {
        if (frameBasedProgress){
            int thisFrame = frameCounter;
//            if (repeating) {
//                if (!returnBack) {
//                    frameCounter = (frameCounter + 1) % frameCount;
                    return thisFrame / (double) frameCount;
//                } else {
//                    frameCounter = (frameCounter + 1) % (frameCount * 2);
//                    if (thisFrame < frameCount)
//                        return thisFrame / (double) frameCount;
//                    else
//                        return 1.0 - ((thisFrame - frameCount) / (double) frameCount);
//                }
//            }
//            else {
//                frameCounter = Math.min(frameCounter + 1, frameCount);
//                return thisFrame / (double) frameCount;
//            }
        }
        else {
            if (repeating) {
                if (!returnBack)
                    return (progress % timeFactor) / timeFactor;
                else {
                    double doubleIntervalProgress = 2.0 * (progress % (timeFactor * 2.0)) / (timeFactor * 2.0);
                    if (doubleIntervalProgress < 1.0)
                        return doubleIntervalProgress;
                    else
                        return 1.0 - (doubleIntervalProgress % 1.0);
                }
            } else
                return Math.min(progress / timeFactor, 1.0);
        }
    }

//    @Override
//    public String getParameterName() {
//        return parameterName;
//    }
//
//    @Override
//    public void setParameterName(String parameterName) {
//        this.parameterName = parameterName;
//    }

    @Override
    public void changeInterpolationParamName(ParamInterpolation interpolation, String newParamUid, String newParamName, String newAttrUid, String newAttrName, String paramType, String paramContainer) {
        removeInterpolation(interpolation.getParamUid(), interpolation.getAttributeName());
        interpolation.setParam(newParamUid, newParamName, paramType, paramContainer, newParamUid, newAttrName);
        setInterpolation(interpolation);
    }

    @Override
    public boolean addAnimationListener(AnimationListener animationListener) {
        return animationListeners.add(animationListener);
    }

    @Override
    public boolean removeAnimationListener(Object o) {
        return animationListeners.remove(o);
    }

    @Override
    public void clearAnimationListeners() {
        animationListeners.clear();
    }

    @Override
    public double getTimeProgress() {
        return progress;
    }

    @Override
    public void setTimeProgress(double progress) {
        this.progress = progress;
        animationProgressUpdated();
    }

    @Override
    public ParamInterpolation getInterpolation(String paramName, String attrName) {
        return interpolations.get(getKey(paramName, attrName));
    }

    @Override
    public Map<String, ParamInterpolation> getInterpolations() {
        return interpolations;
    }

    @Override
    public void setInterpolation(ParamInterpolation interpolation) {
        interpolations.put(getKey(interpolation.getParamUid(), interpolation.getAttributeName()), interpolation);
    }

    @Override
    public ParamInterpolation removeInterpolation(String paramName, String attrName) {
        return interpolations.remove(getKey(paramName, attrName));
    }

    private String getKey(String paramName, String attrName){
        if (attrName == null)
            return paramName;
        else
            return paramName+"."+attrName;
    }

    @Override
    public int getFrameCounter() {
        return frameCounter;
    }

    @Override
    public void setFrameCounter(int frame){
//        System.out.println("set frame: "+this.frameCounter+" -> "+frame);
        this.frameCounter = frame % (frameCount+1);
        animationProgressUpdated();
        if (frameCounter == frameCount)
            animationFinished();
    }

    private void animationProgressUpdated() {
        animationListeners.forEach(l -> l.animationProgressUpdated());
    }

    private void animationFinished() {
        animationListeners.forEach(l -> l.animationFinished());
    }

    @Override
    public void incrementFrameCounter(){
        setFrameCounter(getFrameCounter()+1);
    }

    @Override
    public boolean isUsingTimeBasedProgress() {
        return !frameBasedProgress;
    }

    @Override
    public void setUsingTimeBasedProgress() {
        this.frameBasedProgress = false;
        lastTimeProgressUpdate = System.nanoTime();
    }

    @Override
    public boolean isUsingFrameBasedProgress() {
        return frameBasedProgress;
    }

    @Override
    public void setUsingFrameBasedProgress() {
        this.frameBasedProgress = true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double getTimeFactor() {
        return timeFactor;
    }

    @Override
    public void setTimeFactor(double timeFactor) {
        double relProgress = getTimeProgress()/this.timeFactor;
        this.timeFactor = timeFactor;
        setTimeProgress(relProgress*timeFactor);
    }

    public boolean isRepeating() {
        return repeating;
    }

    public void setRepeating(boolean repeating) {
        this.repeating = repeating;
    }

    public boolean isReturnBack() {
        return returnBack;
    }

    public void setReturnBack(boolean returnBack) {
        this.returnBack = returnBack;
    }

    @Override
    public int getFrameCount() {
        return frameCount;
    }

    @Override
    public void setFrameCount(int frameCount) {
        this.frameCount = frameCount;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public void setPaused(boolean paused) {
        lastTimeProgressUpdate = System.nanoTime();
        this.paused = paused;
    }
}
