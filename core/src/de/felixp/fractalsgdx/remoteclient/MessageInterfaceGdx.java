package de.felixp.fractalsgdx.remoteclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.data.shareddata.DataContainer;
import de.felixperko.fractals.data.shareddata.MappedSharedDataUpdate;
import de.felixperko.fractals.data.shareddata.SharedDataUpdate;
import de.felixperko.fractals.manager.client.ClientManagers;
import de.felixperko.fractals.network.ClientConfiguration;
import de.felixperko.fractals.network.infra.connection.ServerConnection;
import de.felixperko.fractals.network.interfaces.ClientMessageInterface;
import de.felixperko.fractals.network.messages.ResourceRequestMessage;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.systems.stateinfo.ComplexNumberUpdate;
import de.felixperko.fractals.system.systems.stateinfo.ServerStateInfo;
import de.felixperko.fractals.system.systems.stateinfo.TaskStateUpdate;
import de.felixperko.fractals.system.task.FractalsTask;

public class MessageInterfaceGdx extends ClientMessageInterface {

    ClientManagers managers;
    Client client;

    Map<String, List<ISharedDataListener>> sharedDataListeners = new HashMap<>();

    @Override
    public ResourceRequestMessage requestResources() {
        List<Float> requestedGpuUsages = new ArrayList<>();
//        requestedGpuUsages.add(1f);
//        requestedGpuUsages.add(1f);
        return new ResourceRequestMessage(1, requestedGpuUsages, null);
    }

    public MessageInterfaceGdx(ServerConnection serverConnection) {
        super(serverConnection);
        this.client = FractalsGdxMain.client;
        managers = client.getManagers();
        addSharedDataListener(new SharedDataListener("*") {
            @Override
            public void receivedDataContainer(DataContainer dataContainer) {
                System.out.println("received "+dataContainer.getIdentifier()+" ("+dataContainer.getUpdates().size()+")");
            }
        });
        addSharedDataListener(new SharedDataListener("taskStates") {
            @Override
            public void receivedDataContainer(DataContainer dataContainer) {
                for (SharedDataUpdate mapped : dataContainer.getUpdates()){
                    System.out.println("TaskStateUpdates: "+((MappedSharedDataUpdate)mapped).getUpdates().size());
                    for (Object item : ((MappedSharedDataUpdate)mapped).getUpdates()) {
                        TaskStateUpdate update = (TaskStateUpdate) item;
                        SystemInterfaceGdx systemInterface = (SystemInterfaceGdx) getSystemInterface(update.getSystemId());
                        if (systemInterface != null) {
                            //TODO
//                            ChunkContainer chunkContainer = systemInterface.getChunkData().getChunkContainer(update.getTaskId());
//                            if (chunkContainer != null) {
//                                chunkContainer.setTaskState(update.getTaskState());
//                                chunkContainer.setTimeProgress(update.getTimeProgress());
//                                chunkContainer.setLayerId(update.getLayerId());
//                            }
                        }
                    }
//                    System.out.println(update.getTaskId()+" "+update.getTaskState().toString()+" "+update.getLayerId()+" "+update.getTimeProgress());
                }
            }
        });
        addSharedDataListener(new SharedDataListener("currentMidpoint") {
            @Override
            public void receivedDataContainer(DataContainer dataContainer) {
                for (SharedDataUpdate update : dataContainer.getUpdates()) {
                    for (Object item : ((MappedSharedDataUpdate) update).getUpdates()) {
                        ComplexNumberUpdate complexNumberUpdate = (ComplexNumberUpdate) item;
                        System.out.println("systemId: " + complexNumberUpdate.getSystemId() + " " + getRegisteredSystems().iterator().next());
                        SystemInterfaceGdx systemInterface = (SystemInterfaceGdx) getSystemInterface(complexNumberUpdate.getSystemId());
                        if (systemInterface != null)
                            systemInterface.setCurrentMidpoint(complexNumberUpdate.getNumber());
                    }
                }
            }
        });
    }

    public void setManagers(ClientManagers managers){
        this.managers = managers;
    }

    @Override
    protected SystemInterfaceGdx createSystemInterface(ClientConfiguration clientConfiguration) {
//        if (managers == null)
//            throw new IllegalStateException();
//        SystemInterfaceGdx systemInterface = new EncodePixmapThread(this, managers);
//        return systemInterface;
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public void assignedTasks(List<FractalsTask> list) {

    }

    @Override
    public void createdSystem(UUID systemId, ClientConfiguration clientConfiguration, ParamConfiguration parameterConfiguration) {
        if (managers == null)
            throw new IllegalStateException();
        SystemInterfaceGdx systemInterface = new SystemInterfaceGdx(systemId, this, managers);
        //client.setClientConfiguration(clientConfiguration);
        int chunkSize = clientConfiguration.getParameterGeneralValue(systemId, "chunkSize", Integer.class);
        int width = clientConfiguration.getParameterGeneralValue(systemId, "width", Integer.class);
        int height = clientConfiguration.getParameterGeneralValue(systemId, "height", Integer.class);
        if (width % chunkSize > 0)
            width += chunkSize; //'normalized' ++ because of /= chunkSize
        if (height % chunkSize > 0)
            height += chunkSize;
        width /= chunkSize;
        height /= chunkSize;
        client.clientConfiguration = clientConfiguration;
        ParamContainer paramContainer = clientConfiguration.getParamContainer(systemId);
        systemInterface.setParamContainer(paramContainer);
        systemInterface.updateParameterConfiguration(client.getClientSystemById(systemId).getParamContainer(), parameterConfiguration);
        client.createdSystem(clientConfiguration, systemInterface);
//        systemInterface.getClientSystem().setParamContainer(paramContainer);
        addSystemInterface(systemId, systemInterface);
    }

    public static boolean TEST_FINISH = false;

    @Override
    public void serverStateUpdated(ServerStateInfo serverStateInfo) {
//        for (UUID systemId : getRegisteredSystems()) {
//            SystemStateInfo ssi = serverStateInfo.getSystemState(systemId);
//            System.out.println(ssi.getUpdateTime());
//            if (ssi.getTaskListForState(TaskState.OPEN).size() == 0 && ssi.getTaskListForState(TaskState.ASSIGNED).size() == 0 && ssi.getTaskListForState(TaskState.STARTED).size() == 0 && ssi.getTaskListForState(TaskState.PLANNED).size() == 0 && ssi.getTaskListForState(TaskState.DONE).size() > 0)
//                TEST_FINISH = true;
////            for (TaskState state : TaskState.values())
////                System.out.println(state.name()+": "+ssi.getTaskListForState(state).size());
//        }
    }

    @Deprecated
    public FractalsGdxMain getFractalsGdxMain() {
        return client.getFractalsGdxMain();
    }

    @Override
    public void updateSharedData(DataContainer dataContainer) {
        for (ISharedDataListener listener : getSharedDataListenerList(dataContainer.getIdentifier()))
            listener.receivedDataContainer(dataContainer);
        for (ISharedDataListener listener : getSharedDataListenerList("*"))
            listener.receivedDataContainer(dataContainer);
//        for (DataContainer container : dataCont){
//        System.out.println("received data container");
//        }
    }

    public boolean addSharedDataListener(ISharedDataListener sharedDataListener) {
        return getSharedDataListenerList(sharedDataListener.getIdentifier()).add(sharedDataListener);
    }

    public boolean removeSharedDataListener(ISharedDataListener sharedDataListener) {
        List<ISharedDataListener> listeners = getSharedDataListenerList(sharedDataListener.getIdentifier());
        listeners.remove(sharedDataListener);
        return true;
    }

    private List<ISharedDataListener> getSharedDataListenerList(String identifier){
        List<ISharedDataListener> listeners = sharedDataListeners.get(identifier);
        if (listeners == null){
            listeners = new ArrayList<>();
            sharedDataListeners.put(identifier,listeners);
        }
        return listeners;
    }

    @Override
    public void changedResources(int cpuCores, int maxCpuCores, Map<String, Float> gpus) {
        super.changedResources(cpuCores, maxCpuCores, gpus);
        for (ChangedResourcesListener listener : client.changedResourcesListeners)
            listener.changedResources(cpuCores, maxCpuCores, gpus);
    }
}
