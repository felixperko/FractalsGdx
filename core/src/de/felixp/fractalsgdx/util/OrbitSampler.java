package de.felixp.fractalsgdx.util;

import java.util.UUID;

import de.felixp.fractalsgdx.rendering.renderers.ShaderSystemContext;
import de.felixperko.fractals.data.AbstractArrayChunk;
import de.felixperko.fractals.system.LayerConfiguration;
import de.felixperko.fractals.system.calculator.EscapeTime.EscapeTimeCpuCalculatorNew;
import de.felixperko.fractals.system.calculator.infra.FractalsCalculator;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.statistics.IStats;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstLayer;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstUpsampleLayer;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.system.systems.stateinfo.TaskState;
import de.felixperko.fractals.system.systems.stateinfo.TaskStateInfo;
import de.felixperko.fractals.system.task.FractalsTask;
import de.felixperko.fractals.system.task.Layer;
import de.felixperko.fractals.system.task.TaskManager;
import de.felixperko.fractals.system.thread.CalculateFractalsThread;

public class OrbitSampler {

    static EscapeTimeCpuCalculatorNew defaultCalculator = new EscapeTimeCpuCalculatorNew();

    public EscapeTimeCpuCalculatorNew traceCalculator = defaultCalculator;
    private SystemContext systemContext;
    public int arrayFillSize = 0;
    public double[] tracesReal = null;
    public double[] tracesImag = null;
    public double[] tracesIterations = null;
    public int iterationCount = 0;
    public ComplexNumber coords = null;

    public void updateOrbitArrays(SystemContext systemContext, int traceCount, boolean tracePerInstruction, ComplexNumber coords) {

        this.systemContext = systemContext;

        this.coords = coords;
        //calculate sample on cpu and save traces
        double[][] traces = sampleCoordsOnCpu(traceCount, tracePerInstruction, coords);
        if (traces.length == 4) {
            tracesReal = traces[0];
            tracesImag = traces[1];
            tracesIterations = traces[2];
            iterationCount = (int) Math.round(traces[3][0]);
        } else {
            tracesReal = new double[0];
            tracesImag = new double[0];
            tracesIterations = new double[0];
            iterationCount = 0;
        }

        //determine last iteration
        arrayFillSize = traceCount;
        for (int i = 0; i < traces[0].length; i++) {
            if (i > 0 && traces[0][i] == 0.0 && traces[1][i] == 0.0) {
                arrayFillSize = i;
                break;
            }
        }
    }

    protected double[][] sampleCoordsOnCpu(int traceCount, boolean tracePerInstruction, ComplexNumber coords) {
        //prepare calculator
        AbstractArrayChunk traceChunk = new TraceChunk();
        traceChunk.setCurrentTask(new TraceTask(systemContext));
//        BreadthFirstLayer layer = new BreadthFirstLayer().with_samples(1).with_rendering(false);
        boolean layerSet = false;
        for (Layer layer : ((LayerConfiguration) systemContext.getParamValue(ShaderSystemContext.PARAM_LAYER_CONFIG)).getLayers()) {
            if (layer instanceof BreadthFirstLayer && !(layer instanceof BreadthFirstUpsampleLayer)) {
                traceChunk.getCurrentTask().getStateInfo().setLayer(layer);
                layerSet = true;
                break;
            }
        }
        if (!layerSet)
            throw new IllegalStateException("Couldn't find applicable layer for tracing");

        traceChunk.chunkPos = coords;
        traceCalculator.setContext(systemContext);
        traceCalculator.setTraceCount(traceCount, tracePerInstruction);

        //calculate traces
        try {
            traceCalculator.calculate(traceChunk, null, null);
        } catch (Exception e){
            e.printStackTrace();
        }
        return traceCalculator.getTraces();
    }

    private static class TraceChunk extends AbstractArrayChunk {
        public TraceChunk() {
            super(null, 0, 0, 1);
        }

        @Override
        protected void removeFlag(int i) {
        }

        @Override
        public int getStartIndex() {
            return 0;
        }

        @Override
        public double getValue(int i) {
            return 0;
        }

        @Override
        public double getValue(int i, boolean b) {
            return 0;
        }

        @Override
        public void addSample(int i, double v, int i1) {
        }

        @Override
        public int getSampleCount(int i) {
            return 0;
        }

        @Override
        public int getFailedSampleCount(int i) {
            return 0;
        }
    }

    private class TraceTask implements FractalsTask {
        private final TaskStateInfo taskStateInfo;

        public TraceTask(SystemContext systemContext) {
            taskStateInfo = new TaskStateInfo(0, UUID.randomUUID(), systemContext);
            taskStateInfo.setLayer(systemContext.getLayer(0));
        }

        @Override
        public TaskManager<?> getTaskManager() {
            return null;
        }

        @Override
        public Integer getId() {
            return 0;
        }

        @Override
        public Integer getJobId() {
            return 0;
        }

        @Override
        public UUID getSystemId() {
            return null;
        }

        @Override
        public TaskStateInfo getStateInfo() {
            return taskStateInfo;
        }

        @Override
        public TaskState getState() {
            return null;
        }

        @Override
        public void setThread(CalculateFractalsThread calculateFractalsThread) {

        }

        @Override
        public FractalsCalculator getCalculator() {
            return null;
        }

        @Override
        public void run() throws InterruptedException {

        }

        @Override
        public IStats getTaskStats() {
            return null;
        }

        @Override
        public void setTaskStats(IStats iStats) {

        }

        @Override
        public void setStateInfo(TaskStateInfo taskStateInfo) {

        }

        @Override
        public SystemContext getContext() {
            return systemContext;
        }

        @Override
        public void applyLocalState(FractalsTask fractalsTask) {

        }

        @Override
        public Double getPriority() {
            return null;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}