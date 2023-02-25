package de.felixp.fractalsgdx.util;

import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;
import de.felixperko.fractals.system.systems.infra.SystemContext;

public class OrbitSampleThread extends Thread{

    OrbitSampleManager sampleManager;

    OrbitSampleHolder assignedHolder = null;
    SystemContext systemContext = null;

    public OrbitSampleThread(OrbitSampleManager orbitSampleManager) {
        this.sampleManager = orbitSampleManager;
    }

    public void setSystemContext(SystemContext systemContext){
        this.systemContext = systemContext;
    }

    public void setAssignedHolder(OrbitSampleHolder sampleHolder){
        assignedHolder = sampleHolder;
    }

    @Override
    public void run() {

        OrbitSampler tempSampler = new OrbitSampler();

        while (!isInterrupted()){

            if (isWaiting()){
                sampleManager.threadIdle(this);
                try {
                    sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            else {
                //work on the same holder even if the assignedHolder is changed
                OrbitSampleHolder holder = assignedHolder;

                ComplexNumber candidateCoord = holder.getCandidateCoords(this);

                int traceCount = (int) systemContext.getParamValue(CommonFractalParameters.PARAM_ITERATIONS);
                tempSampler.updateOrbitArrays(systemContext, traceCount, false, candidateCoord);
                tempSampler = holder.applyCandidateIfBetter(this, tempSampler);
            }
        }
    }

    private boolean isWaiting(){
        return assignedHolder == null || !assignedHolder.moreSamplesRequired() || systemContext == null;
    }
}
