package de.felixp.fractalsgdx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixperko.fractals.data.AbstractArrayChunk;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.network.infra.connection.ServerConnection;
import de.felixperko.fractals.system.LayerConfiguration;
import de.felixperko.fractals.system.calculator.infra.FractalsCalculator;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.numbers.impl.DoubleComplexNumber;
import de.felixperko.fractals.system.numbers.impl.DoubleNumber;
import de.felixperko.fractals.system.parameters.ParameterConfiguration;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.common.BFOrbitCommon;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.system.systems.infra.ViewContainer;
import de.felixperko.fractals.system.systems.stateinfo.SystemStateInfo;
import de.felixperko.fractals.system.systems.stateinfo.TaskState;
import de.felixperko.fractals.system.systems.stateinfo.TaskStateInfo;
import de.felixperko.fractals.system.task.Layer;

public class GPUSystemContext implements SystemContext {

    ParameterConfiguration parameterConfiguration;

    ParamContainer paramContainer;
    NumberFactory nf;

    public GPUSystemContext(){

        parameterConfiguration = new ParameterConfiguration();

        List<ParameterDefinition> defs = new ArrayList<>();

        parameterConfiguration.addValueType(BFOrbitCommon.numberType);
        parameterConfiguration.addValueType(BFOrbitCommon.complexnumberType);

        defs.add(new ParameterDefinition("iterations", "Calculator", StaticParamSupplier.class, BFOrbitCommon.integerType));
        defs.add(new ParameterDefinition("zoom", "Position", StaticParamSupplier.class, BFOrbitCommon.numberType));
        defs.add(new ParameterDefinition("midpoint", "Position", StaticParamSupplier.class, BFOrbitCommon.complexnumberType));
        defs.add(new ParameterDefinition("c", "Calculator", StaticParamSupplier.class, BFOrbitCommon.complexnumberType));

        parameterConfiguration.addParameterDefinitions(defs);


        Map<String, ParamSupplier> map = new HashMap<>();
        paramContainer = new ParamContainer(map);

        nf = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

        paramContainer.addClientParameter(new StaticParamSupplier("numberFactory", nf));
        paramContainer.addClientParameter(new StaticParamSupplier("iterations", 1000));
        paramContainer.addClientParameter(new StaticParamSupplier("midpoint", nf.createComplexNumber(0,0)));
        paramContainer.addClientParameter(new StaticParamSupplier("c", nf.createComplexNumber(0,0)));
        paramContainer.addClientParameter(new StaticParamSupplier("zoom", nf.createNumber(3)));
    }

    public void init() {
        ((MainStage)FractalsGdxMain.stage).setServerParameterConfiguration(paramContainer, this.parameterConfiguration);
    }

    @Override
    public boolean setParameters(ParamContainer paramContainer) {
        this.paramContainer = paramContainer;
        return false;
    }

    @Override
    public Layer getLayer(int i) {
        return null;
    }

    @Override
    public NumberFactory getNumberFactory() {
        return paramContainer.getClientParameter("numberFactory").getGeneral(NumberFactory.class);
    }

    @Override
    public void taskStateUpdated(TaskStateInfo taskStateInfo, TaskState taskState) {

    }

    @Override
    public void setServerConnection(ServerConnection serverConnection) {

    }

    @Override
    public FractalsCalculator createCalculator() {
        return null;
    }

    @Override
    public AbstractArrayChunk createChunk(int i, int i1) {
        return null;
    }

    @Override
    public ParamContainer getParamContainer() {
        return paramContainer;
    }

    @Override
    public Map<String, ParamSupplier> getParameters() {
        return paramContainer.getClientParameters();
    }

    @Override
    public Object getParamValue(String s) {
        return paramContainer.getClientParameter(s).getGeneral();
    }

    @Override
    public ViewContainer getViewContainer() {
        return null;
    }

    @Override
    public LayerConfiguration getLayerConfiguration() {
        return null;
    }

    @Override
    public Number getPixelzoom() {
        return null;
    }

    @Override
    public int getChunkSize() {
        return 0;
    }

    @Override
    public void incrementViewId() {

    }

    @Override
    public void setMidpoint(ComplexNumber midpoint) {
        StaticParamSupplier supplier = new StaticParamSupplier("midpoint", midpoint);
        supplier.updateChanged(paramContainer.getClientParameter("midpoint"));
        paramContainer.addClientParameter(supplier);
    }

    @Override
    public ComplexNumber getMidpoint() {
        return paramContainer.getClientParameter("midpoint").getGeneral(ComplexNumber.class);
    }

    @Override
    public int getViewId() {
        return 0;
    }

    @Override
    public void setViewId(Integer integer) {

    }

    @Override
    public SystemStateInfo getSystemStateInfo() {
        return null;
    }

    @Override
    public Object getParamValue(String s, Class aClass, ComplexNumber complexNumber, int i, int i1) {
        return paramContainer.getClientParameter(s).get(this, complexNumber, i, i1);
    }

    @Override
    public Object getParamValue(String s, Class aClass) {
        return paramContainer.getClientParameter(s).getGeneral(aClass);
    }
}
