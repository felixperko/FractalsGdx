package de.felixp.fractalsgdx.client;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixperko.fractals.data.ArrayChunkFactory;
import de.felixperko.fractals.data.ReducedNaiveChunk;
import de.felixperko.fractals.manager.common.Managers;
import de.felixperko.fractals.network.ClientConfiguration;
import de.felixperko.fractals.network.ParamContainer;
import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.network.infra.connection.ServerConnection;
import de.felixperko.fractals.network.interfaces.ClientMessageInterface;
import de.felixperko.fractals.network.interfaces.ClientSystemInterface;
import de.felixperko.fractals.network.messages.UpdateConfigurationMessage;
import de.felixperko.fractals.system.Numbers.DoubleComplexNumber;
import de.felixperko.fractals.system.Numbers.DoubleNumber;
import de.felixperko.fractals.system.Numbers.infra.ComplexNumber;
import de.felixperko.fractals.system.Numbers.infra.Number;
import de.felixperko.fractals.system.Numbers.infra.NumberFactory;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BFSystemContext;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstLayer;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstUpsampleLayer;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.LayerConfiguration;
import de.felixperko.fractals.system.systems.infra.SystemContext;

public class ClientSystem {

    Managers managers;
    Client client;

    public ClientSystem(Managers managers, Client client){
        this.managers = managers;
        this.client = client;
        this.systemContext = createDefaultSystemConfiguration();
    }

    SystemInterfaceGdx systemInterface;

    BFSystemContext systemContext;
    public Integer chunkSize = 128*2;

    ComplexNumber anchor;

    public ParamContainer getParamContainer() {
        return systemContext.getParamContainer();
    }

    public void setSystemClientData(SystemClientData systemClientData){
        this.systemContext.setParameters(systemClientData);
    }

    private BFSystemContext createDefaultSystemConfiguration() {
        NumberFactory numberFactory = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);
        Map<String, ParamSupplier> params = new HashMap<>();
        int samplesDim = 1;
        params.put("width", new StaticParamSupplier("width", (Integer) Gdx.graphics.getWidth()));
        params.put("height", new StaticParamSupplier("height", (Integer)Gdx.graphics.getHeight()));
        params.put("chunkSize", new StaticParamSupplier("chunkSize", chunkSize));
//		params.put("midpoint", new StaticParamSupplier("midpoint", new DoubleComplexNumber(new DoubleNumber(0.251), new DoubleNumber(0.00004849892910689283399687005))));
        ComplexNumber midpoint = numberFactory.createComplexNumber(0, 0);
        this.anchor = midpoint.copy();
        params.put("midpoint", new StaticParamSupplier("midpoint", midpoint));
//        zoom = numberFactory.createNumber(5./200000.);
        Number zoom = numberFactory.createNumber(3.);
        params.put("zoom", new StaticParamSupplier("zoom", zoom));
        params.put("iterations", new StaticParamSupplier("iterations", (Integer)5000));
        params.put("samples", new StaticParamSupplier("samples", (Integer)(samplesDim*samplesDim)));

        List<BreadthFirstLayer> layers = new ArrayList<>();
        layers.add(new BreadthFirstUpsampleLayer(64, chunkSize).with_culling(true).with_rendering(false));
        layers.add(new BreadthFirstUpsampleLayer(64, chunkSize).with_culling(true).with_rendering(true).with_priority_shift(0));
        layers.add(new BreadthFirstUpsampleLayer(16, chunkSize).with_culling(true).with_rendering(true).with_priority_shift(10));
        layers.add(new BreadthFirstUpsampleLayer(8, chunkSize).with_culling(true).with_rendering(true).with_priority_shift(30));
        layers.add(new BreadthFirstUpsampleLayer(4, chunkSize).with_culling(true).with_rendering(true).with_priority_shift(50));
        layers.add(new BreadthFirstUpsampleLayer(2, chunkSize).with_culling(true).with_rendering(true).with_priority_shift(70));
        layers.add(new BreadthFirstLayer().with_samples(1).with_rendering(true).with_priority_shift(90));
        layers.add(new BreadthFirstLayer().with_samples(9).with_rendering(true).with_priority_shift(110));
        layers.add(new BreadthFirstLayer().with_samples(25).with_priority_shift(130));
        layers.add(new BreadthFirstLayer().with_samples(100).with_priority_shift(150));

        LayerConfiguration layerConfiguration = new LayerConfiguration(layers, 0.05D, 20, 42L);
        layerConfiguration.prepare(numberFactory);

        params.put("layerConfiguration", new StaticParamSupplier("layerConfiguration", layerConfiguration));
        params.put("border_generation", new StaticParamSupplier("border_generation", (Double) 0.));
        params.put("border_dispose", new StaticParamSupplier("border_dispose", (Double) 7.));
        params.put("task_buffer", new StaticParamSupplier("task_buffer", (Integer) 5));


        params.put("calculator", new StaticParamSupplier("calculator", "MandelbrotCalculator"));
        params.put("systemName", new StaticParamSupplier("systemName", "BreadthFirstSystem"));
        params.put("numberFactory", new StaticParamSupplier("numberFactory", numberFactory));
        params.put("chunkFactory", new StaticParamSupplier("chunkFactory", new ArrayChunkFactory(ReducedNaiveChunk.class, chunkSize)));


        params.put("start", new StaticParamSupplier("start", new DoubleComplexNumber(new DoubleNumber(0.0), new DoubleNumber(0.0))));
        params.put("c", new CoordinateBasicShiftParamSupplier("c"));
//
//        params.put("start", new CoordinateBasicShiftParamSupplier("start"));
//        params.put("c", new StaticParamSupplier("c", new DoubleComplexNumber(new DoubleNumber(0.0), new DoubleNumber(0.0))));


        params.put("pow", new StaticParamSupplier("pow", new DoubleComplexNumber(new DoubleNumber(2), new DoubleNumber(0.000*Math.PI*2))));
//        params.put("pow", new CoordinateBasicShiftParamSupplier("pow"));
        params.put("limit", new StaticParamSupplier("limit", (Double)(double)(2 << 12)));

        params.put("view", new StaticParamSupplier("view", 0));

        BFSystemContext systemContext = new BFSystemContext(null);
        systemContext.setParameters(new SystemClientData(params, 0));
        return systemContext;

    }

    public void updatePosition(float deltaX, float deltaY) {
        //TODO scaling
        ComplexNumber shift = systemContext.getNumberFactory().createComplexNumber(deltaX/ Gdx.graphics.getHeight(), -deltaY/Gdx.graphics.getHeight());
        shift.multNumber(systemContext.getZoom());
//        System.out.println(shift.toString());
//        midpoint.sub(shift);
        ComplexNumber midpoint = systemContext.getMidpoint();
        midpoint.sub(shift);
        systemContext.setMidpoint(midpoint);
        //systemClientData.getClientParameters().put("midpoint", new StaticParamSupplier("midpoint", midpoint));
        updateConfiguration();
    }

    public void updateZoom(float zoomFactor){
        Number zoom = systemContext.getZoom();
        zoom.mult(systemContext.getNumberFactory().createNumber(zoomFactor));
        setOldParams(systemContext.getParamContainer().getClientParameters()); //TODO !
        systemContext.setZoom(zoom);
        //systemClientData.getClientParameters().put("zoom", supplier);
        incrementJobId();
        updateConfiguration();
        resetAnchor();
    }

    public void incrementJobId(){
        //ParamSupplier viewSupplier = systemClientData.getClientParameters().get("view");
        //Integer view = viewSupplier != null ? viewSupplier.getGeneral(Integer.class) : -1;
        //systemClientData.getClientParameters().put("view", new StaticParamSupplier("view", view + 1));
        systemContext.incrementViewId();
        //jobId = view+1;
    }

    Map<String, ParamSupplier> oldParams = null;

    /**
     * needs to be called before updating the configuration
     * @param oldParams
     */
    public void setOldParams(Map<String, ParamSupplier> oldParams){
        this.oldParams = new HashMap<>();
        for (Map.Entry<String, ParamSupplier> e : oldParams.entrySet()){
            this.oldParams.put(e.getKey(), e.getValue().copy());
        }
    }

    public Map<String, ParamSupplier> getOldParams(){
        return oldParams;
    }

    public void updateConfiguration(){

        UpdateConfigurationMessage updateConfigurationMessage = new UpdateConfigurationMessage(new ClientConfiguration(client.clientConfiguration));
        client.serverConnection.writeMessage(updateConfigurationMessage);

        systemInterface.setSystemClientData(systemContext.getParamContainer()); //TODO remove
        systemInterface.updateParameterConfiguration(systemContext.getParamContainer(), null);
    }

    public ComplexNumber getAnchor(){
        return anchor;
    }

    public void resetAnchor() {
        anchor = systemContext.getMidpoint().copy();
    }

    public void createdSystem(SystemInterfaceGdx systemInterface, ClientConfiguration clientConfiguration) {
        this.systemInterface = systemInterface;
        setSystemClientData(clientConfiguration.getSystemClientData(systemInterface.systemId));
        systemInterface.setClientSystem(this);
    }

    public SystemContext getSystemContext() {
        return systemContext;
    }
}
