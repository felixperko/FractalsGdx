package de.felixp.fractalsgdx.client;

import de.felixp.fractalsgdx.FractalsGdxMain;
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

    boolean local =
//            true
            false
            ;
    final String ip =
//            "localhost"
//                "192.168.0.13"
                "192.168.2.117"
            ;
    final int port =
            3141
//                80
            ;

    ClientManagers managers;
    FractalsGdxMain fractalsGdxMain;

    ClientConfiguration clientConfiguration;
    ServerConnection serverConnection;
    MessageInterfaceGdx messageInterface;

    //TODO multi system
    ClientSystem clientSystem;

    public Client(FractalsGdxMain fractalsGdxMain) {
        this.fractalsGdxMain = fractalsGdxMain;
    }

    public void start(){
        managers = new ClientManagers(new NetworkInterfaceFactory(MessageInterfaceGdx.class, SystemInterfaceGdx.class));
        clientConfiguration = new ClientConfiguration();

        connect(ip, port, local);
    }

    public void connectLocal(){
        connect(null, -1, true);
    }

    public void connect(String ip, int port){
        connect(ip, port, false);
    }

    private void connect(String ip, int port, boolean local) {
        if (serverConnection != null){
            managers.getClientNetworkManager().closeServerConnection(serverConnection);
            serverConnection = null;
        }

        ClientSystem clientSystem = new ClientSystem(managers, this);
        this.clientSystem = clientSystem;
        clientConfiguration.addRequest(new ParamContainer(clientSystem.getParamContainer(), false));

        LocalMessageableGdx messageable = new LocalMessageableGdx();

        if (local)
            serverConnection = managers.getClientNetworkManager().connectToLocalServer(messageable, true);
        else
            serverConnection = managers.getClientNetworkManager().connectToServer(ip, port);

        serverConnection.getWriteToServer().setConnection(serverConnection);
        messageable.setServerConnection(serverConnection);

        if (local)
            messageable.writeMessage(new ConnectedMessage((ClientConnection)messageable.getConnection()));

        while (serverConnection.getClientInfo() == null) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        messageInterface = (MessageInterfaceGdx) managers.getNetworkManager().getMessageInterface(serverConnection);

        serverConnection.writeMessage(new SessionInitRequestMessage(new ClientConfiguration(clientConfiguration, true)));
    }

    public void createdSystem(ClientConfiguration configuration, SystemInterfaceGdx systemInterface){
        //TODO multi system
        setClientConfiguration(clientConfiguration);
        clientSystem.createdSystem(systemInterface, configuration);
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

    public ClientSystem getFocusedClientSystem(){
        return clientSystem;
    }
}
