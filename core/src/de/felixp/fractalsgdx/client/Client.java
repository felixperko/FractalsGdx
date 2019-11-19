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
            true
//            false
            ;
    final String ip =
            "localhost"
//                "192.168.0.13"
//                "192.168.0.11"
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

        ClientSystem clientSystem = new ClientSystem(managers, this);
        this.clientSystem = clientSystem;
        clientConfiguration.addRequest(new ParamContainer(clientSystem.getParamContainer(), false));

        LocalMessageableGdx messageable = new LocalMessageableGdx();

        if (local)
            serverConnection = managers.getClientNetworkManager().connectToLocalServer(messageable, true);
        else
            serverConnection = managers.getClientNetworkManager().connectToServer(ip, port);

//        serverConnection =
//        managers.getClientNetworkManager().connectToServer("95.168.135.138", 80);
//		  managers.getClientNetworkManager().connectToServer("192.168.0.13", 3141);
//        managers.getClientNetworkManager().connectToServer("192.168.0.11", 3141);
//        managers.getClientNetworkManager().connectToServer("192.168.137.1", 3141);
//        managers.getClientNetworkManager().connectToServer("localhost", 3141);
//        managers.getClientNetworkManager().connectToLocalServer(messageable, true);


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

//        if (local) {
            serverConnection.writeMessage(new SessionInitRequestMessage(new ClientConfiguration(clientConfiguration, true)));
//        }
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
