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
import de.felixperko.fractals.network.messages.SessionInitRequestMessage;
import de.felixperko.fractals.network.messages.UpdateConfigurationMessage;
import de.felixperko.fractals.system.Numbers.DoubleComplexNumber;
import de.felixperko.fractals.system.Numbers.DoubleNumber;
import de.felixperko.fractals.system.Numbers.infra.ComplexNumber;
import de.felixperko.fractals.system.Numbers.infra.Number;
import de.felixperko.fractals.system.Numbers.infra.NumberFactory;
import de.felixperko.fractals.system.parameters.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.ParamSupplier;
import de.felixperko.fractals.system.parameters.StaticParamSupplier;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstLayer;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstUpsampleLayer;

public class Client {

    ClientManagers managers;

    ClientConfiguration clientConfiguration;

    MessageInterface messageInterface;

    FractalsGdxMain fractalsGdxMain;

    ComplexNumber midpoint = new DoubleComplexNumber(new DoubleNumber(0), new DoubleNumber(0));
    Number zoom;
    NumberFactory numberFactory;

    public int jobId = 0;

    public Integer chunkSize = 128;

    public Client(FractalsGdxMain fractalsGdxMain) {
        this.fractalsGdxMain = fractalsGdxMain;
    }

    public void start(){
        numberFactory = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);
        Map<String, ParamSupplier> params = new HashMap<>();
        int samplesDim = 1;
        params.put("width", new StaticParamSupplier("width", (Integer)Gdx.graphics.getWidth()));
        params.put("height", new StaticParamSupplier("height", (Integer)Gdx.graphics.getHeight()));
        params.put("chunkSize", new StaticParamSupplier("chunkSize", chunkSize));
//		params.put("midpoint", new StaticParamSupplier("midpoint", new DoubleComplexNumber(new DoubleNumber(0.251), new DoubleNumber(0.00004849892910689283399687005))));
        params.put("midpoint", new StaticParamSupplier("midpoint", midpoint));
//        zoom = numberFactory.createNumber(5./200000.);
        zoom = numberFactory.createNumber(30.);
        params.put("zoom", new StaticParamSupplier("zoom", zoom));
        params.put("iterations", new StaticParamSupplier("iterations", (Integer)5000));
        params.put("samples", new StaticParamSupplier("samples", (Integer)(samplesDim*samplesDim)));

//		params.put("midpoint", new StaticParamSupplier("midpoint", new DoubleComplexNumber(new DoubleNumber(.0), new DoubleNumber(0.0))));
//		params.put("zoom", new StaticParamSupplier("zoom", numberFactory.createNumber(4./50000.)));
//		params.put("zoom", new StaticParamSupplier("zoom", numberFactory.createNumber(3.)));
        List<BreadthFirstLayer> layers = new ArrayList<>();
        layers.add(new BreadthFirstUpsampleLayer(0, 16, chunkSize));
        layers.add(new BreadthFirstUpsampleLayer(1, 4, chunkSize).with_priority_shift(10));
        layers.add(new BreadthFirstLayer(2).with_samples(1).with_priority_shift(20));
//        layers.add(new BreadthFirstLayer(2).with_samples(8).with_priority_shift(20));
//        layers.add(new BreadthFirstLayer(2).with_samples(8));
        params.put("layers", new StaticParamSupplier("layers", layers));
        params.put("border_generation", new StaticParamSupplier("border_generation", (Double) 0.));
        params.put("border_dispose", new StaticParamSupplier("border_dispose", (Double) 7.));
        params.put("task_buffer", new StaticParamSupplier("task_buffer", (Integer) 5));


        params.put("calculator", new StaticParamSupplier("calculator", "MandelbrotCalculator"));
        params.put("systemName", new StaticParamSupplier("systemName", "BreadthFirstSystem"));
        params.put("numberFactory", new StaticParamSupplier("numberFactory", numberFactory));
        params.put("chunkFactory", new StaticParamSupplier("chunkFactory", new ArrayChunkFactory(ReducedNaiveChunk.class, chunkSize)));


        params.put("start", new StaticParamSupplier("start", new DoubleComplexNumber(new DoubleNumber(0.0), new DoubleNumber(0.0))));
        params.put("c", new CoordinateBasicShiftParamSupplier("c", numberFactory, samplesDim));
        params.put("pow", new StaticParamSupplier("pow", new DoubleComplexNumber(new DoubleNumber(2), new DoubleNumber(0))));
        params.put("limit", new StaticParamSupplier("limit", (Double)(double)(2 << 10)));

//		params.put("c", new StaticParamSupplier("c", new DoubleComplexNumber(new DoubleNumber(0.5), new DoubleNumber(0.3))));
//		params.put("start", new CoordinateParamSupplier("start", numberFactory));

        clientConfiguration = new ClientConfiguration();
        clientConfiguration.addRequest(new SystemClientData(params, 0));


        messageInterface = new MessageInterface(this);
        managers = new ClientManagers(messageInterface);
        messageInterface.setManagers(managers);

        managers.getClientNetworkManager().connectToServer("89.16.136.152", 3141);
//		managers.getClientNetworkManager().connectToServer("192.168.0.13", 3141);
//        managers.getClientNetworkManager().connectToServer("localhost", 3141);
        while (managers.getClientNetworkManager().getClientInfo() == null) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        managers.getClientNetworkManager().getServerConnection().writeMessage(new SessionInitRequestMessage(clientConfiguration));
    }

    public ClientManagers getManagers() {
        return managers;
    }

    public ClientConfiguration getClientConfiguration() {
        return clientConfiguration;
    }

    public MessageInterface getMessageInterface() {
        return messageInterface;
    }

    public void setClientConfiguration(ClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
    }

    public FractalsGdxMain getFractalsGdxMain() {
        return fractalsGdxMain;
    }

    public void updatePosition(float deltaX, float deltaY) {
        //TODO scaling
        ComplexNumber shift = numberFactory.createComplexNumber(deltaX/ Gdx.graphics.getHeight(), deltaY/Gdx.graphics.getHeight());
        shift.multNumber(zoom);
//        System.out.println(shift.toString());
//        midpoint.sub(shift);
        midpoint = midpoint.copy();
        midpoint.sub(shift);
//        System.out.println(midpoint.toString());
        for(SystemClientData data : clientConfiguration.getSystemClientData().values()){
            data.getClientParameters().put("midpoint", new StaticParamSupplier("midpoint", midpoint));
        }
        updateConfiguration();
    }

    public void updateZoom(float zoomFactor){
        zoom.mult(numberFactory.createNumber(zoomFactor));
        for(SystemClientData data : clientConfiguration.getSystemClientData().values()){
            StaticParamSupplier supplier = new StaticParamSupplier("zoom", this.zoom);
            supplier.setLayerRelevant(true);
            data.getClientParameters().put("zoom", supplier);
        }
        updateConfiguration();
    }

    public void updateConfiguration(){
        managers.getNetworkManager().getServerConnection().writeMessage(new UpdateConfigurationMessage(clientConfiguration));
    }
}
