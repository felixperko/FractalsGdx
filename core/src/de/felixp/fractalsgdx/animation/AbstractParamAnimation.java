package de.felixp.fractalsgdx.animation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import de.felixp.fractalsgdx.animation.interpolations.ParamInterpolation;
import de.felixperko.fractals.util.NumberUtil;

abstract class AbstractParamAnimation<T> implements ParamAnimation {

//    public static void main(String[] args){
//        PathParamAnimation animation = new PathParamAnimation("dummy", null);
//        NumberFactory nf = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);
//        ArrayList<ComplexNumber> controlPoints = new ArrayList<>();
//        controlPoints.add(nf.createComplexNumber(0,0));
//        controlPoints.add(nf.createComplexNumber(0,1));
//        controlPoints.add(nf.createComplexNumber(-2,1));
//        animation.setControlPoints(controlPoints, nf);
////               AbstractParamAnimation animation = new AbstractParamAnimation("dummy") {
////            @Override
////            public Object getInterpolatedValueInLoop(double progressInLoop, NumberFactory numberFactory) {
////                return progressInLoop;
////            }
////        };
//        animation.setReturnBack(true);
//        animation.setRepeating(true);
//        animation.setTimeFactor(2);
//        double progress = 0.0;
//        for (int i = 0 ; i < 100 ; i++){
//            ComplexNumber rawVal = animation.getInterpolatedValue(progress, null);
//            double roundedValR = NumberUtil.getRoundedDouble(rawVal.realDouble(), 3);
//            double roundedValI = NumberUtil.getRoundedDouble(rawVal.imagDouble(), 3);
//            String textValR = ("" + roundedValR).replace('.', ',');
//            String textValI = ("" + roundedValI).replace('.', ',');
//            System.out.println(textValR+";"+textValI);
//            progress += 0.1;
//        }
//    }

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

//    @Override
//    public T getInterpolatedValue(double progress, NumberFactory numberFactory) {
//        double loopProgress = getLoopProgress();
//        this.progress = loopProgress;
//        return getInterpolatedValueInLoop(loopProgress, numberFactory);
//    }

//    public abstract T getInterpolatedValueInLoop(double progressInLoop, NumberFactory numberFactory);

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
    public void changeInterpolationParamName(ParamInterpolation interpolation, String newName, String paramType, String paramContainer) {
        removeInterpolation(interpolation.getParamName());
        interpolation.setParam(newName, paramType, paramContainer);
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
    public ParamInterpolation getInterpolation(String paramName) {
        return interpolations.get(paramName);
    }

    @Override
    public Map<String, ParamInterpolation> getInterpolations() {
        return interpolations;
    }

    @Override
    public void setInterpolation(ParamInterpolation interpolation) {
        interpolations.put(interpolation.getParamName(), interpolation);
    }

    @Override
    public ParamInterpolation removeInterpolation(String paramName) {
        return interpolations.remove(paramName);
    }

    @Override
    public int getFrameCounter() {
        return frameCounter;
    }

    @Override
    public void setFrameCounter(int frame){
        System.out.println("set frame: "+this.frameCounter+" -> "+frame);
//        Thread.dumpStack();
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
        this.timeFactor = timeFactor;
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
