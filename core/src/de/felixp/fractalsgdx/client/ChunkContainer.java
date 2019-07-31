package de.felixp.fractalsgdx.client;

import de.felixperko.fractals.data.CompressedChunk;
import de.felixperko.fractals.system.systems.stateinfo.TaskState;

public class ChunkContainer {

    CompressedChunk compressedChunk;

    TaskState taskState = TaskState.PLANNED;
    double progress = 0.;
    private int layerId;

    public ChunkContainer(CompressedChunk compressedChunk){
        this.compressedChunk = compressedChunk;
    }

    public CompressedChunk getCompressedChunk() {
        return compressedChunk;
    }

    public TaskState getTaskState() {
        return taskState;
    }

    public void setTaskState(TaskState taskState) {
        this.taskState = taskState;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public void setLayerId(int layerId) {
        this.layerId = layerId;
    }

    public int getLayerId() {
        return layerId;
    }
}
