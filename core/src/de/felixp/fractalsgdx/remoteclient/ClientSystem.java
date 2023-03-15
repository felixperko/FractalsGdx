package de.felixp.fractalsgdx.remoteclient;

import com.badlogic.gdx.Gdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.fractals.data.ArrayChunkFactory;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.data.ReducedNaiveChunk;
import de.felixperko.fractals.manager.common.Managers;
import de.felixperko.fractals.network.ClientConfiguration;
import de.felixperko.fractals.network.messages.UpdateConfigurationMessage;
import de.felixperko.fractals.system.LayerConfiguration;
import de.felixperko.fractals.system.numbers.impl.DoubleComplexNumber;
import de.felixperko.fractals.system.numbers.impl.DoubleNumber;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.PadovanLayerConfiguration;
import de.felixperko.fractals.system.parameters.ExpressionsParam;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BFSystemContext;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BreadthFirstLayer;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.system.task.Layer;

public class ClientSystem {

    private static Logger LOG = LoggerFactory.getLogger(ClientSystem.class);

    Managers managers;
    Client client;
    UUID systemId;

    public ClientSystem(Managers managers, Client client, UUID systemId){
        this.managers = managers;
        this.client = client;
        this.systemId = systemId;
        this.systemContext = createDefaultSystemConfiguration();
    }

    SystemInterfaceGdx systemInterface;

    BFSystemContext systemContext;
//    public Integer chunkSize = 128*2*2*2;
//    public Integer chunkSize = 128*2*2;
    public Integer chunkSize = 128*2;
//    public Integer chunkSize = 128; ///TODO doesn't work

    ComplexNumber anchor;
    
    ParamConfiguration paramConfiguration = null;

    public ParamContainer getParamContainer() {
        return systemContext.getParamContainer();
    }

    public void setParamContainer(ParamContainer paramContainer){
//        this.systemContext.setParameters(paramContainer);
        ((MainStage) FractalsGdxMain.stage).submitServer(systemInterface.getRenderer(), paramContainer);
    }

    private BFSystemContext createDefaultSystemConfiguration() {
        NumberFactory numberFactory = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);
        LinkedHashMap<String, ParamSupplier> params = new LinkedHashMap<>();
        int samplesDim = 1;
        params.put("width", new StaticParamSupplier("width", (Integer) Gdx.graphics.getWidth()));
        params.put("height", new StaticParamSupplier("height", (Integer)Gdx.graphics.getHeight()));
        params.put("chunkSize", new StaticParamSupplier("chunkSize", chunkSize));
        params.put("precision", new StaticParamSupplier("precision", "32"));
//		params.put("midpoint", new StaticParamSupplier("midpoint", new DoubleComplexNumber(new DoubleNumber(0.251), new DoubleNumber(0.00004849892910689283399687005))));
        ComplexNumber midpoint = numberFactory.createComplexNumber(0, 0);
        this.anchor = midpoint.copy();
        params.put("midpoint", new StaticParamSupplier("midpoint", midpoint));
//        zoom = nf.createNumber(5./200000.);
        Number zoom = numberFactory.createNumber(3.);
        params.put("zoom", new StaticParamSupplier("zoom", zoom));
        params.put("iterations", new StaticParamSupplier("iterations", (Integer)1000));
        params.put("samples", new StaticParamSupplier("samples", (Integer)(samplesDim*samplesDim)));

//        params.put(BFOrbitCommon.PARAM_EXPRESSION, new StaticParamSupplier(BFOrbitCommon.PARAM_EXPRESSION, "(re(z)+abs(im(z))*i)^pow+c"));
//        params.put(BFOrbitCommon.PARAM_EXPRESSION, new StaticParamSupplier(BFOrbitCommon.PARAM_EXPRESSION, "(re(z)+tanh(im(z))*i)^pow+c"));
        params.put(CommonFractalParameters.PARAM_EXPRESSIONS, new StaticParamSupplier(CommonFractalParameters.PARAM_EXPRESSIONS, new ExpressionsParam("z^pow+c", "z")));

        List<Layer> layers = new ArrayList<>();
//        layers.add(new BreadthFirstUpsampleLayer(64, chunkSize).with_culling(true).with_rendering(false));
//        layers.add(new BreadthFirstUpsampleLayer(64, chunkSize).with_culling(true).with_rendering(true).with_priority_shift(0));
//        layers.add(new BreadthFirstUpsampleLayer(16, chunkSize).with_culling(true).with_rendering(true).with_priority_shift(10));
//        layers.add(new BreadthFirstUpsampleLayer(8, chunkSize).with_culling(true).with_rendering(true).with_priority_shift(30));
//        layers.add(new BreadthFirstUpsampleLayer(4, chunkSize).with_culling(true).with_rendering(true).with_priority_shift(50));
//        layers.add(new BreadthFirstUpsampleLayer(2, chunkSize).with_culling(true).with_rendering(true).with_priority_shift(70));
        layers.add(new BreadthFirstLayer(chunkSize).with_samples(1).with_rendering(true).with_priority_shift(100));
//        layers.add(new BreadthFirstLayer(chunkSize).with_samples(2).with_rendering(true).with_priority_shift(130));
//        layers.add(new BreadthFirstLayer(chunkSize).with_samples(4).with_rendering(true).with_priority_shift(160));
//        layers.add(new BreadthFirstLayer(chunkSize).with_samples(9).with_rendering(true).with_priority_shift(190));
//        layers.add(new BreadthFirstLayer(chunkSize).with_samples(25).with_priority_shift(210));
//        layers.add(new BreadthFirstLayer(chunkSize).with_samples(100).with_priority_shift(240));
//        layers.add(new BreadthFirstLayer(chunkSize).with_samples(400).with_priority_shift(270));

        LayerConfiguration layerConfiguration = new PadovanLayerConfiguration(layers);
//        LayerConfiguration layerConfiguration = new LayerConfiguration(layers, 0.025, 20, 42);
        layerConfiguration.prepare(numberFactory);

        params.put("layerConfiguration", new StaticParamSupplier("layerConfiguration", layerConfiguration));
        params.put("border_generation", new StaticParamSupplier("border_generation", (Double) 0.));
        params.put("border_dispose", new StaticParamSupplier("border_dispose", (Double) 7.));
        params.put("task_buffer", new StaticParamSupplier("task_buffer", (Integer) 5));


        params.put("calculator", new StaticParamSupplier("calculator", "CustomCalculator"));
        params.put("systemName", new StaticParamSupplier("systemName", "BreadthFirstSystem"));
        params.put("numberFactory", new StaticParamSupplier("numberFactory", numberFactory));
        params.put("chunkFactory", new StaticParamSupplier("chunkFactory", new ArrayChunkFactory(ReducedNaiveChunk.class, chunkSize)));


        params.put(CommonFractalParameters.PARAM_ZSTART, new StaticParamSupplier(CommonFractalParameters.PARAM_ZSTART,
                new DoubleComplexNumber(new DoubleNumber(0.0), new DoubleNumber(0.0))));
        params.put("c", new CoordinateBasicShiftParamSupplier("c"));
//
//        params.put(BFOrbitCommon.PARAM_ZSTART, new CoordinateBasicShiftParamSupplier(BFOrbitCommon.PARAM_ZSTART));
//        params.put("c", new StaticParamSupplier("c", new DoubleComplexNumber(new DoubleNumber(0.0), new DoubleNumber(0.0))));


        params.put("pow", new StaticParamSupplier("pow", new DoubleComplexNumber(new DoubleNumber(2), new DoubleNumber(0.000*Math.PI*2))));
//        params.put("pow", new CoordinateBasicShiftParamSupplier("pow"));
        params.put("limit", new StaticParamSupplier("limit", (Double)(double)(2 << 12)));

        params.put("view", new StaticParamSupplier("view", 0));

        BFSystemContext systemContext = new BFSystemContext(null, paramConfiguration);
        ParamContainer paramContainer = new ParamContainer(paramConfiguration);
        paramContainer.setParams(params, null);
        systemContext.setParameters(paramContainer);
        return systemContext;

    }

    //TODO Number for arbitrary precision
    public void updatePosition(double deltaX, double deltaY) {
        //TODO scaling
        ComplexNumber shift = systemContext.getNumberFactory().createComplexNumber(deltaX/ Gdx.graphics.getHeight(), -deltaY/Gdx.graphics.getHeight());
        shift.multNumber(systemContext.getZoom());
//        System.out.println(shift.toString());
//        midpoint.sub(shift);
        ComplexNumber midpoint = systemContext.getMidpoint().copy();
        midpoint.sub(shift);

        LOG.warn("updating midpoint from "+systemContext.getMidpoint()+" to "+midpoint);
        systemContext.setMidpoint(midpoint);
        //systemClientData.getParamMap().put("midpoint", new StaticParamSupplier("midpoint", midpoint));
        updateConfiguration();
    }

    public void updateZoom(Number zoomFactor){
        Number zoom = systemContext.getZoom();
        zoom.mult(zoomFactor);
        setOldParams(systemContext.getParamContainer().getParamMap()); //TODO !
        systemContext.setZoom(zoom);
//        systemClientData.getParamMap().put("zoom", supplier);
        incrementJobId();
        updateConfiguration();
        resetAnchor();
    }

    public void incrementJobId(){
        //ParamSupplier viewSupplier = systemClientData.getParamMap().get("view");
        //Integer view = viewSupplier != null ? viewSupplier.getGeneral(Integer.class) : -1;
        //systemClientData.getParamMap().put("view", new StaticParamSupplier("view", view + 1));
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
//        ClientConfiguration newConfiguration = new ClientConfiguration(client.clientConfiguration);
        client.clientConfiguration.setParamContainer(systemInterface.getSystemId(), systemContext.getParamContainer());
        UpdateConfigurationMessage updateConfigurationMessage = new UpdateConfigurationMessage(client.clientConfiguration);
        client.serverConnection.writeMessage(updateConfigurationMessage);

        systemInterface.setParamContainer(systemContext.getParamContainer()); //TODO remove
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

        systemInterface.setClientSystem(this);

        //apply params from context
        ParamContainer contextParamContainer = this.systemInterface.getRenderer().getRendererContext().getParamContainer();
        if (contextParamContainer != null) {
            contextParamContainer.setParamConfiguration(getParamConfiguration());
            //add additional params
            for (Map.Entry<String, ParamSupplier> e : this.systemContext.getParameters().entrySet()){
                Map<String, ParamSupplier> parameters = contextParamContainer.getParamMap();
                if (!parameters.containsKey(e.getKey()))
                    parameters.put(e.getKey(), e.getValue());
            }
            setParamContainer(contextParamContainer);
            systemContext.setViewId(0);
        } else {
            setParamContainer(clientConfiguration.getParamContainer(systemInterface.systemId));
        }
//        clientConfiguration.addRequest(systemId, new ParamContainer(getParamContainer(), false));
//        client.serverConnection.writeMessage(new UpdateConfigurationMessage(clientConfiguration));
    }

    public SystemContext getSystemContext() {
        return systemContext;
    }

    public UUID getSystemId() {
        return systemId;
    }

    public SystemInterfaceGdx getSystemInterface() {
        return systemInterface;
    }

    public void setParamConfiguration(ParamConfiguration paramConfiguration) {
        this.paramConfiguration = paramConfiguration;
        systemContext.setParamConfiguration(paramConfiguration);
    }

    public ParamConfiguration getParamConfiguration() {
        return paramConfiguration;
    }
}
