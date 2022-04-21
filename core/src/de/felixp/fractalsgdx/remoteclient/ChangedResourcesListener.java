package de.felixp.fractalsgdx.remoteclient;

import java.util.Map;

public interface ChangedResourcesListener {
    public void changedResources(int cpuCores, int maxCpuCores, Map<String, Float> gpus);
}
