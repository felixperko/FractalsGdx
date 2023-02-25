package de.felixp.fractalsgdx.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OrbitSampleManager extends Thread{

    int workerThreadCount;
    List<OrbitSampleThread> workerThreads = new ArrayList<>();
    Set<OrbitSampleThread> idleWorkerThreads = new HashSet<>();

    public OrbitSampleManager(int workerThreadCount){
        this.workerThreadCount = workerThreadCount;
        initWorkerThreads();
        this.start();
    }

    private void initWorkerThreads() {
        for (int i = 0 ; i < workerThreadCount ; i++){
            OrbitSampleThread thread = new OrbitSampleThread(this);
            workerThreads.add(thread);
            thread.start();
        }
    }

    public synchronized void threadIdle(OrbitSampleThread sampleThread) {
        idleWorkerThreads.add(sampleThread);
    }

    @Override
    public void run() {
        while (!Thread.interrupted()){

            //TODO Renderer - workerThread assignment
            synchronized (this){
                for (OrbitSampleThread thread : idleWorkerThreads){
                    //TODO assign systemContext and sampleHolder to workerThreads
                }
                idleWorkerThreads.clear();
            }
            try {
                sleep(1L);
            } catch (InterruptedException e){

            }
        }
    }
}
