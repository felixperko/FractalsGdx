package de.felixp.fractalsgdx.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;

public class OrbitSampleHolder {

    int iterationCount = -1;
    int bufferedSamplerCount = 10;

    NumberFactory nf;
    ComplexNumber midpoint;
    Number zoom;

    Map<OrbitSampleThread, Integer> candidateSlotMap = new HashMap<>();
    int candidateCounter = 0;

    List<OrbitSampler> samplers = new ArrayList<>();

    public OrbitSampleHolder(NumberFactory nf, int iterationCount, int bufferedSamplerCount){
        this.nf = nf;
        this.iterationCount = iterationCount;
        this.bufferedSamplerCount = bufferedSamplerCount;
        initSamplers();
    }

    private void initSamplers() {
        for (int i = 0 ; i < bufferedSamplerCount ; i++){
            samplers.add(new OrbitSampler());
        }
    }

    public void setIterationCount(int iterationCount){
        this.iterationCount = iterationCount;
    }

    public void setMidpoint(ComplexNumber midpoint){
        this.midpoint = midpoint;
    }

    public void setZoom(Number zoom){
        this.zoom = zoom;
    }

    public List<OrbitSampler> getSamplers() {
        return samplers;
    }

    public boolean moreSamplesRequired() {
        int samplesFinished = 0;
        for (OrbitSampler sampler : samplers){
            if (sampler.iterationCount >= iterationCount)
                samplesFinished++;
        }
        return samplesFinished >= bufferedSamplerCount;
    }


    private ComplexNumber getRandomCandidate(OrbitSampler orbitSampler){
        double rFactor = Math.random()*Math.random();
        double iFactor = Math.random()*Math.random();
        ComplexNumber num = nf.ccn(rFactor, iFactor);
        num.multNumber(zoom);
        num.add(midpoint);
        return num;
//        int traceCount = iterationCount;
//        float x = (float)(Math.random()*range + shift)*getWidth();
//        float y = (float)(Math.random()*range + shift)*getHeight();
//        ComplexNumber coords = getComplexMapping(x,y);
//        orbitSampler.updateOrbitArrays(systemContext, traceCount, false, coords);
    }

    public synchronized ComplexNumber getCandidateCoords(OrbitSampleThread thread) {
        int skipped = 0;
        while (samplers.get(candidateCounter).iterationCount == iterationCount){
            incrementCandidateCounter();
            skipped++;
            if (skipped == bufferedSamplerCount)
                return null;
        }
        candidateSlotMap.put(thread, candidateCounter);

        return samplers.get(candidateCounter).coords;
    }

    /**
     * Keeps the original or replaces with temp sampler
     * @return the temp sampler for reuse
     */
    public OrbitSampler applyCandidateIfBetter(OrbitSampleThread thread, OrbitSampler tempSampler){
        int index = candidateSlotMap.remove(thread);
        OrbitSampler currentSampler = samplers.get(index);

        if (tempSampler.iterationCount <= currentSampler.iterationCount){
            return tempSampler;
        }

        samplers.set(index, tempSampler);
        return currentSampler;
    }

    private void incrementCandidateCounter() {
        candidateCounter++;
        if (candidateCounter >= bufferedSamplerCount)
            candidateCounter = 0;
    }
}
