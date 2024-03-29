package de.felixp.fractalsgdx.remoteclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import de.felixperko.fractals.network.infra.Message;
import de.felixperko.fractals.network.infra.connection.ClientLocalConnection;
import de.felixperko.fractals.network.infra.connection.Connection;
import de.felixperko.fractals.network.infra.connection.ServerConnection;
import de.felixperko.fractals.network.interfaces.Messageable;

/**
 * Can be retrieved from a local server to send messages to this client
 */
public class LocalMessageableGdx implements Messageable {

    ClientLocalConnection connection;

    ServerConnection serverConnection;

    Logger log = LoggerFactory.getLogger("com/toLocalClient");

    List<Runnable> connectionClosedRunnables = new ArrayList<>();

    public LocalMessageableGdx(){
    }

    @Override
    public Connection<?> getConnection() {
        return connection;
    }

    @Override
    public void setConnection(Connection<?> connection){
        if (!(connection instanceof ClientLocalConnection))
            throw new IllegalArgumentException("LocalMessageableGdx.setConnection() only accepts ClientLocalConnection");
        this.connection = (ClientLocalConnection)connection;
    }

    public void setServerConnection(ServerConnection serverConnection){
        this.serverConnection = serverConnection;
    }

    @Override
    public void closeConnection() {
        connection.setClosed();
    }

    @Override
    public boolean isCloseConnection() {
        return connection.isClosed();
    }

    @Override
    public void prepareMessage(Message message) {
        message.setSentTime();
    }

    @Override
    public void writeMessage(Message message) {
        message.received(serverConnection, log);
    }

    @Override
    public void start() {

    }

    public void addConnectionClosedRunnable(Runnable runnable){
        this.connectionClosedRunnables.add(runnable);
    }

    public void removeConnectionClosedRunnable(Runnable runnable){
        this.connectionClosedRunnables.remove(runnable);
    }
}
