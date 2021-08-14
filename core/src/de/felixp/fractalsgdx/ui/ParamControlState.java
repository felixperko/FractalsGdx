package de.felixp.fractalsgdx.ui;

import de.felixperko.fractals.system.numbers.Number;

/**
 * Shared state of parameter ui controls.
 * Managed by CollapsiblePropertyList
 */
public class ParamControlState {

    String controlView;
    Double minLimit;
    Double maxLimit;
    Double minLimit2;
    Double maxLimit2;

    public void copyLimits(ParamControlState other){
        this.minLimit = other.minLimit;
        this.maxLimit = other.maxLimit;
        this.minLimit2 = other.minLimit2;
        this.maxLimit2 = other.maxLimit2;
    }

    public void copyValuesIfNull(ParamControlState other) {
        if (this.controlView == null)
            this.controlView = other.controlView;
        if (this.minLimit == null)
            this.minLimit = other.minLimit;
        if (this.maxLimit == null)
            this.maxLimit = other.maxLimit;
    }


    public String getControlView() {
        return controlView;
    }

    public void setControlView(String controlView) {
        this.controlView = controlView;
    }

    public Double getMin() {
        return minLimit;
    }

    public Double getMin2(){
        return minLimit2;
    }

    public void setMin(Double minLimit) {
        this.minLimit = minLimit;
    }

    public void setMin2(Double minLimit2){
        this.minLimit2 = minLimit2;
    }

    public Double getMax() {
        return maxLimit;
    }

    public Double getMax2(){
        return maxLimit2;
    }

    public void setMax(Double maxLimit) {
        this.maxLimit = maxLimit;
    }

    public void setMax2(Double maxLimit2){
        this.maxLimit2 = maxLimit2;
    }

}
