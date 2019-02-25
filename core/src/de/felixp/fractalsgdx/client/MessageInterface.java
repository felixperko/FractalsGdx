package de.felixp.fractalsgdx.client;

import java.util.UUID;

import de.felixperko.fractals.manager.client.ClientManagers;
import de.felixperko.fractals.network.ClientConfiguration;
import de.felixperko.fractals.network.ClientMessageInterface;
import de.felixperko.fractals.network.ClientSystemInterface;
import de.felixperko.fractals.system.systems.stateinfo.ServerStateInfo;
import de.felixperko.fractals.system.systems.stateinfo.SystemStateInfo;
import de.felixperko.fractals.system.systems.stateinfo.TaskState;

public class MessageInterface extends ClientMessageInterface {

    ClientManagers managers;
    Client client;

    public MessageInterface(Client client) {
        this.client = client;
    }

    public void setManagers(ClientManagers managers){
        this.managers = managers;
    }

    @Override
    protected ClientSystemInterface createSystemInterface(ClientConfiguration clientConfiguration) {
        if (managers == null)
            throw new IllegalStateException();
        SystemInterface systemInterface = new SystemInterface(managers);
        return systemInterface;
    }

    @Override
    public void createdSystem(UUID systemId, ClientConfiguration clientConfiguration) {
        if (managers == null)
            throw new IllegalStateException();
        SystemInterface systemInterface = new SystemInterface(managers);

        int chunkSize = clientConfiguration.getParameterGeneralValue(systemId, "chunkSize", Integer.class);
        int width = clientConfiguration.getParameterGeneralValue(systemId, "width", Integer.class);
        int height = clientConfiguration.getParameterGeneralValue(systemId, "height", Integer.class);
        if (width % chunkSize > 0)
            width += chunkSize; //'normalized' ++ because of /= chunkSize
        if (height % chunkSize > 0)
            height += chunkSize;
        width /= chunkSize;
        height /= chunkSize;
        systemInterface.addChunkCount((width)*(height));
        systemInterface.setParameters(clientConfiguration.getSystemClientData(systemId).getClientParameters());
        addSystemInterface(systemId, systemInterface);

        client.setClientConfiguration(clientConfiguration);
    }

    public static boolean TEST_FINISH = false;

    @Override
    public void serverStateUpdated(ServerStateInfo serverStateInfo) {
        for (UUID systemId : getRegisteredSystems()) {
            SystemStateInfo ssi = serverStateInfo.getSystemState(systemId);
            System.out.println(ssi.getUpdateTime());
            if (ssi.getTaskListForState(TaskState.OPEN).size() == 0 && ssi.getTaskListForState(TaskState.ASSIGNED).size() == 0 && ssi.getTaskListForState(TaskState.STARTED).size() == 0 && ssi.getTaskListForState(TaskState.PLANNED).size() == 0 && ssi.getTaskListForState(TaskState.FINISHED).size() > 0)
                TEST_FINISH = true;
            for (TaskState state : TaskState.values())
                System.out.println(state.name()+": "+ssi.getTaskListForState(state).size());
        }
    }

}
