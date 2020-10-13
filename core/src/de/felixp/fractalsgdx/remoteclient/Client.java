package de.felixp.fractalsgdx.remoteclient;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.manager.client.ClientManagers;
import de.felixperko.fractals.network.ClientConfiguration;
import de.felixperko.fractals.network.infra.connection.ClientConnection;
import de.felixperko.fractals.network.infra.connection.ServerConnection;
import de.felixperko.fractals.network.interfaces.NetworkInterfaceFactory;
import de.felixperko.fractals.network.interfaces.ServerLocalMessageable;
import de.felixperko.fractals.network.messages.ConnectedMessage;
import de.felixperko.fractals.network.messages.SessionInitRequestMessage;

public class Client {

    boolean connectOnStart =
//            true;
            false;

    boolean local =
            true;
//            false;

    final String ip =
            "localhost";
//                "192.168.0.13";
//                "192.168.2.117";

    final int port =
            3141;
//                80;

    ClientManagers managers;
    FractalsGdxMain fractalsGdxMain;

    ClientConfiguration clientConfiguration;
    ServerConnection serverConnection;
    MessageInterfaceGdx messageInterface;

    List<ClientSystem> clientSystems = new ArrayList<>();

    List<ChangedResourcesListener> changedResourcesListeners = new ArrayList<>();

    public Client(FractalsGdxMain fractalsGdxMain) {
        this.fractalsGdxMain = fractalsGdxMain;
    }

    public void start(){
        managers = new ClientManagers(new NetworkInterfaceFactory(MessageInterfaceGdx.class, SystemInterfaceGdx.class));

        clientConfiguration = new ClientConfiguration();

        addChangedResourcesListener(new ChangedResourcesListener() {
            @Override
            public void changedResources(int cpuCores, int maxCpuCores, Map<String, Float> gpus) {
//                renderer.changedResources();
            }
        });

        if (connectOnStart)
            connect(ip, port, local);
//        else
//            ((MainStage)FractalsGdxMain.stage).openConnectWindow(null);
    }

    public void connectLocal(){
        connect(null, -1, true);
    }

    public void connect(String ip, int port){
        connect(ip, port, false);
    }

    private boolean connect(String ip, int port, boolean local) {

        if (serverConnection != null) {
            if (this.local)
                managers.getThreadManager().stopLocalServer();
            managers.getClientNetworkManager().closeServerConnection(serverConnection);
            serverConnection = null;
        }

        this.local = local;

        LocalMessageableGdx messageable = new LocalMessageableGdx();

        if (local){

            int tryConnects = 3;
            long tryTimeout = 10;

            for (int i = 0 ; i < tryConnects ; i++) {
                try {
                    serverConnection = managers.getClientNetworkManager().connectToLocalServer(messageable, true);
                } catch (NullPointerException e){
                    if (i == tryConnects-1)
                        throw e;
                    else{
                        try {
                            Thread.sleep(tryTimeout);
                        } catch (InterruptedException e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            }
        } else {
            try {
                serverConnection = managers.getClientNetworkManager().connectToServer(ip, port);
                serverConnection.getWriteToServer().addConnectionClosedRunnable(
                        () -> ((MainStage)FractalsGdxMain.stage).openConnectWindow("Connection lost."));
            } catch (ConnectException e){
                ((MainStage)FractalsGdxMain.stage).openConnectWindow(null);
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
                System.exit(0);
            }
        }

        if (serverConnection == null)
            return false;

        serverConnection.getWriteToServer().setConnection(serverConnection);
        messageable.setServerConnection(serverConnection);

        if (local)
            messageable.writeMessage(new ConnectedMessage((ClientConnection) messageable.getConnection()));

        while (serverConnection.getClientInfo() == null) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        messageInterface = (MessageInterfaceGdx) managers.getNetworkManager().getMessageInterface(serverConnection);

        ClientSystem clientSystem = clientSystems.get(clientSystems.size() - 1);
        clientConfiguration.addRequest(clientSystem.systemId, clientSystem.systemContext.getParamContainer());
        serverConnection.writeMessage(new SessionInitRequestMessage(new ClientConfiguration(clientConfiguration, true)));
        return true;
    }

    public void createdSystem(ClientConfiguration configuration, SystemInterfaceGdx systemInterface){
        setClientConfiguration(clientConfiguration);
        getClientSystemById(systemInterface.getSystemId()).createdSystem(systemInterface, configuration);
//        clientSystem.createdSystem(systemInterface, configuration);
        if (local)
            ((ServerLocalMessageable)serverConnection.getWriteToServer()).registerViewContainerListener(systemInterface.systemId, systemInterface);
    }

    public ClientManagers getManagers() {
        return managers;
    }

    public ClientConfiguration getClientConfiguration() {
        return clientConfiguration;
    }

    public MessageInterfaceGdx getMessageInterface() {
        return messageInterface;
    }

    public void setClientConfiguration(ClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
    }

    public FractalsGdxMain getFractalsGdxMain() {
        return fractalsGdxMain;
    }

    public ServerConnection getServerConnection() {
        return serverConnection;
    }

    public void addChangedResourcesListener(ChangedResourcesListener changedResourcesListener){
        this.changedResourcesListeners.add(changedResourcesListener);
    }

    public void removeChangedResourcesListener(ChangedResourcesListener changedResourcesListener){
        this.changedResourcesListeners.remove(changedResourcesListener);
    }

    public ClientSystem getClientSystemById(UUID systemId){
        for (ClientSystem system : clientSystems)
            if (system.getSystemId().equals(systemId))
                return system;
        return null;
    }

    public void addClientSystemRequest(UUID systemId, ClientSystem clientSystem){
        this.clientSystems.add(clientSystem);
    }
}
