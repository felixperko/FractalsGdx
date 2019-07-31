package de.felixp.fractalsgdx.client;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixperko.fractals.data.ArrayChunkFactory;
import de.felixperko.fractals.data.ReducedNaiveChunk;
import de.felixperko.fractals.manager.client.ClientManagers;
import de.felixperko.fractals.network.ClientConfiguration;
import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.network.infra.connection.ServerConnection;
import de.felixperko.fractals.network.interfaces.ClientMessageInterface;
import de.felixperko.fractals.network.interfaces.ClientSystemInterface;
import de.felixperko.fractals.network.interfaces.NetworkInterfaceFactory;
import de.felixperko.fractals.network.messages.SessionInitRequestMessage;
import de.felixperko.fractals.network.messages.UpdateConfigurationMessage;
import de.felixperko.fractals.system.Numbers.DoubleComplexNumber;
import de.felixperko.fractals.system.Numbers.DoubleNumber;
import de.felixperko.fractals.system.Numbers.infra.ComplexNumber;
import de.felixperko.fractals.system.Numbers.infra.Number;
import de.felixperko.fractals.system.Numbers.infra.NumberFactory;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstLayer;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstUpsampleLayer;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.LayerConfiguration;

public class Client {

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
        clientConfiguration.addRequest(clientSystem.getSystemClientData());

        serverConnection =
//        managers.getClientNetworkManager().connectToServer("95.168.135.138", 80);
//		  managers.getClientNetworkManager().connectToServer("192.168.0.13", 3141);
//        managers.getClientNetworkManager().connectToServer("192.168.0.11", 3141);
//        managers.getClientNetworkManager().connectToServer("192.168.137.1", 3141);
        managers.getClientNetworkManager().connectToServer("localhost", 3141);

        while (serverConnection.getClientInfo() == null) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        messageInterface = (MessageInterfaceGdx) managers.getNetworkManager().getMessageInterface(serverConnection);

        serverConnection.writeMessage(new SessionInitRequestMessage(clientConfiguration));
    }

    public void createdSystem(ClientConfiguration configuration, SystemInterfaceGdx systemInterface){
        //TODO multi system
        setClientConfiguration(clientConfiguration);
        clientSystem.createdSystem(systemInterface, configuration);
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
