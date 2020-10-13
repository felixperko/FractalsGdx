package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.rendering.RemoteRenderer;
import de.felixp.fractalsgdx.rendering.ShaderRenderer;
import de.felixp.fractalsgdx.remoteclient.ClientSystem;
import de.felixp.fractalsgdx.ui.entries.AbstractPropertyEntry;
import de.felixp.fractalsgdx.ui.entries.PropertyEntryFactory;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.numbers.impl.DoubleComplexNumber;
import de.felixperko.fractals.system.numbers.impl.DoubleNumber;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.infra.SystemContext;

public class ParamUI {

    static CollapsiblePropertyList serverParamsSideMenu;

    //TODO move!
    public static void submitServer(FractalRenderer renderer, ParamContainer paramContainer){
        for (AbstractPropertyEntry entry : serverParamsSideMenu.getPropertyEntries()){
            entry.applyClientValue();
        }
        if (renderer instanceof RemoteRenderer) {
            ClientSystem clientSystem = ((RemoteRenderer) renderer).getSystemInterface().getClientSystem();
            if (paramContainer.needsReset(clientSystem.getOldParams())) {
                clientSystem.incrementJobId();
                paramContainer.getClientParameters().put("view", new StaticParamSupplier("view", clientSystem.getSystemContext().getViewId()));
                renderer.reset();
            }
            clientSystem.setOldParams(paramContainer.getClientParameters());
            clientSystem.getSystemContext().setParameters(paramContainer);
            clientSystem.updateConfiguration();
            clientSystem.resetAnchor();//TODO integrate...
        }
    }

    PropertyEntryFactory serverPropertyEntryFactory;
//    List<AbstractPropertyEntry> propertyEntryList = new ArrayList<>();

    CollapsiblePropertyList clientParamsSideMenu;
    PropertyEntryFactory clientPropertyEntryFactory;

    boolean switchIsJulia = false;
    Number switchMandelbrotZoom = null;
    ComplexNumber switchMandelbrotMidpoint = null;

    MainStage stage;

    public ParamUI(MainStage mainStage){
        this.stage = mainStage;
    }

    public void init(ParamConfiguration clientParameterConfiguration, ParamContainer clientParams){
        //extra button to switch mandelbrot <-> juliaset
        //

        CollapsiblePropertyListButton switchJuliasetMandelbrotButton = getJuliasetButton();

        //init menus at sides
        //

        serverParamsSideMenu = new CollapsiblePropertyList().addButton(switchJuliasetMandelbrotButton);
        clientParamsSideMenu = new CollapsiblePropertyList();
        serverPropertyEntryFactory = new PropertyEntryFactory(serverParamsSideMenu.getCategoryNodes(), new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class));//TODO dynamic number factory
        clientPropertyEntryFactory = new PropertyEntryFactory(clientParamsSideMenu.getCategoryNodes(), new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class));//TODO dynamic number factory

        clientParamsSideMenu.setParameterConfiguration(clientParams, clientParameterConfiguration, clientPropertyEntryFactory);
        ChangeListener listener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                for (AbstractPropertyEntry e : clientParamsSideMenu.getPropertyEntries())
                    e.applyClientValue();
            }
        };
        clientParamsSideMenu.addAllListener(listener);
        //TODO test ShaderRenderer: apply parameters
//        if (stage.getRenderer() instanceof ShaderRenderer){
//            ChangeListener listener2 = new ChangeListener() {
//                @Override
//                public void changed(ChangeEvent event, Actor actor) {
//                    for (AbstractPropertyEntry e : serverParamsSideMenu.getPropertyEntries())
//                        e.applyClientValue();
//                }
//            };
//            serverParamsSideMenu.addAllListener(listener2);
//        }
    }

    public CollapsiblePropertyListButton getJuliasetButton() {
        return new CollapsiblePropertyListButton("switch juliaset", "Calculator", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                switchIsJulia = !switchIsJulia;
                SystemContext systemContext = stage.focusedRenderer.getSystemContext();
//                systemContext.incrementViewId(); //TODO integrate... (why do i need this here? Does the copy really work?)
                ParamContainer serverParamContainer = new ParamContainer(systemContext.getParamContainer(), true);
                if (switchIsJulia) {
                    switchMandelbrotZoom = serverParamContainer.getClientParameter("zoom").getGeneral(Number.class).copy();
                    switchMandelbrotMidpoint = serverParamContainer.getClientParameter("midpoint").getGeneral(ComplexNumber.class).copy();
                    CoordinateBasicShiftParamSupplier newStartSupp = new CoordinateBasicShiftParamSupplier("start");
//                    newStartSupp.setChanged(true);
//                    serverParamContainer.getClientParameters().put("start", newStartSupp);
//                    ComplexNumber pos = serverParamContainer.getClientParameter("midpoint").getGeneral(ComplexNumber.class);
//                    StaticParamSupplier newStartSupp = new StaticParamSupplier("start", pos);
                    newStartSupp.setChanged(true);
                    serverParamContainer.getClientParameters().put("start", newStartSupp);

                    StaticParamSupplier newCSupp = new StaticParamSupplier("c", switchMandelbrotMidpoint);
                    newCSupp.setChanged(true);
                    serverParamContainer.getClientParameters().put("c", newCSupp);

                    StaticParamSupplier midpointSupp = new StaticParamSupplier("midpoint", systemContext.getNumberFactory().createComplexNumber(0, 0));
                    midpointSupp.setChanged(true);
                    midpointSupp.setLayerRelevant(true);
                    serverParamContainer.getClientParameters().put("midpoint", midpointSupp);

                    StaticParamSupplier zoomSupp = new StaticParamSupplier("zoom", systemContext.getNumberFactory().createNumber(3.0));
                    zoomSupp.setChanged(true);
                    zoomSupp.setLayerRelevant(true);
                    serverParamContainer.getClientParameters().put("zoom", zoomSupp);
                } else {
                    StaticParamSupplier newStartSupp = new StaticParamSupplier("start", systemContext.getNumberFactory().createComplexNumber(0, 0));
//                    newStartSupp.setChanged(true);
//                    serverParamContainer.getClientParameters().put("start", newStartSupp);
//                    ComplexNumber pos = serverParamContainer.getClientParameter("midpoint").getGeneral(ComplexNumber.class);
//                    StaticParamSupplier newStartSupp = new StaticParamSupplier("start", pos);
                    newStartSupp.setChanged(true);
                    serverParamContainer.getClientParameters().put("start", newStartSupp);

                    CoordinateBasicShiftParamSupplier newCSupp = new CoordinateBasicShiftParamSupplier("c");
                    newCSupp.setChanged(true);
                    serverParamContainer.getClientParameters().put("c", newCSupp);

                    StaticParamSupplier midpointSupp = new StaticParamSupplier("midpoint", switchMandelbrotMidpoint);
                    midpointSupp.setChanged(true);
                    midpointSupp.setLayerRelevant(true);
                    serverParamContainer.getClientParameters().put("midpoint", midpointSupp);

                    StaticParamSupplier zoomSupp = new StaticParamSupplier("zoom", switchMandelbrotZoom);
                    zoomSupp.setChanged(true);//TODO test if required -> integrate...
                    zoomSupp.setLayerRelevant(true);//TODO integrate...
                    serverParamContainer.getClientParameters().put("zoom", zoomSupp);
                }
//                renderer.reset();//TODO I shouldn't need this, its in submitServer(). Still doesnt reset old tiles
                submitServer(((MainStage)FractalsGdxMain.stage).focusedRenderer, serverParamContainer);
            }
        });
    }

    public void addToUiTable(VisTable ui) {
//        ui.debug();
        VisTable serverParamTable = new VisTable();
        VisTable clientParamTable = new VisTable();

        serverParamsSideMenu.addToTable(serverParamTable, Align.left);
        clientParamsSideMenu.addToTable(clientParamTable, Align.right);

        ui.add(serverParamTable).left().expandY().expandX().fillX();
        ui.add(clientParamTable).right().expandY();
        ui.row();
    }

    public void setServerParameterConfiguration(FractalRenderer renderer, ParamContainer paramContainer, ParamConfiguration parameterConfiguration){
        setParameterConfiguration(renderer, serverParamsSideMenu, paramContainer, parameterConfiguration, serverPropertyEntryFactory);
    }

    public void setParameterConfiguration(FractalRenderer focusedRenderer, CollapsiblePropertyList list,
                                          ParamContainer paramContainer, ParamConfiguration parameterConfiguration, PropertyEntryFactory propertyEntryFactory){

        ChangeListener submitListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                submitServer(focusedRenderer, paramContainer);
//                }
            }
        };

        list.setParameterConfiguration(paramContainer, parameterConfiguration, propertyEntryFactory);
        list.addSubmitListener(submitListener);

        focusedRenderer.setRefresh();
//        stage.updateStateBar();
//        for (AbstractPropertyEntry entry : propertyEntryList) {
//            entry.closeView(AbstractPropertyEntry.VIEW_LIST);
//        }
//        propertyEntryList.clear();
//
//        VisTable tree = serverParamsSideMenu.getTree();
//
//        tree.clear();
//
//        //NumberFactory numberFactory = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);
//
//        //addEntry(new IntTextPropertyEntry(tree, systemClientData, "iterations"));
//        //addEntry(new ComplexNumberPropertyEntry(tree, systemClientData, "pow", numberFactory));
//
//        if (submitButton != null)
//            submitButton.remove();
//
//        List<ParameterDefinition> parameterDefinitions = new ArrayList<>(parameterConfiguration.getParameters());
//        List<ParameterDefinition> calculatorParameterDefinitions = parameterConfiguration.getCalculatorParameters(paramContainer.getClientParameter("calculator").getGeneral(String.class));
//        parameterDefinitions.addAll(calculatorParameterDefinitions);
//        for (ParameterDefinition parameterDefinition : parameterDefinitions) {
//            AbstractPropertyEntry entry = serverPropertyEntryFactory.getPropertyEntry(parameterDefinition, paramContainer);
//            if (entry != null) {
//                entry.init();
//                entry.openView(AbstractPropertyEntry.VIEW_LIST, tree);
//                addEntry(entry);
//            }
//        }
//
//        ClientSystem clientSystem = ((RemoteRenderer)renderer).getSystemInterface().getClientSystem();
//
//        submitButton = new VisTextButton("Submit", new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                clientSystem.setOldParams(paramContainer.getClientParameters());
//                for (AbstractPropertyEntry entry : propertyEntryList){
//                    entry.applyClientValue();
//                }
//                if (paramContainer.needsReset(clientSystem.getOldParams())) {
//                    clientSystem.incrementJobId();
//                    renderer.reset();
//                }
//                clientSystem.updateConfiguration();
////                }
//            }
//        });
//
//        tree.add();
//        tree.add(submitButton).row();
//
//        renderer.setRefresh();
//        updateStateBar();
    }
}
