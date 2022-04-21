package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.FocusManager;
import com.kotcrab.vis.ui.widget.VisTable;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.ui.entries.AbstractPropertyEntry;
import de.felixp.fractalsgdx.ui.entries.PropertyEntryFactory;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.numbers.impl.DoubleComplexNumber;
import de.felixperko.fractals.system.numbers.impl.DoubleNumber;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.infra.SystemContext;

public class ParamUI {

    public static boolean showSliderLimits = false;

    PropertyEntryFactory serverPropertyEntryFactory;
//    List<AbstractPropertyEntry> propertyEntryList = new ArrayList<>();

    CollapsiblePropertyList serverParamsSideMenu;
    CollapsiblePropertyList clientParamsSideMenu;
    PropertyEntryFactory clientPropertyEntryFactory;

//    boolean switchIsJulia = false;
//    Number switchMandelbrotZoom = null;
//    ComplexNumber switchMandelbrotMidpoint = null;
//    ComplexNumber switchJuliasetC = null;

    protected MainStage stage;

    public ParamUI(MainStage mainStage){
        this.stage = mainStage;
    }

    public void init(ParamConfiguration clientParameterConfiguration, ParamContainer clientParams){
        //extra button to switch mandelbrot <-> juliaset
        //

        CollapsiblePropertyListButton switchJuliasetMandelbrotButton = getSwitchRenderersButton();

        //initRenderer menus at sides
        //

        serverParamsSideMenu = new CollapsiblePropertyList().addButton(switchJuliasetMandelbrotButton);
        clientParamsSideMenu = new CollapsiblePropertyList();

//        clientParamsSideMenu.setSliderLimitsVisible(false);
        clientParamsSideMenu.setControlWidthScreenScaling(0.1f);
        clientParamsSideMenu.setMaxControlWidth(400f);
        clientParamsSideMenu.setMinControlWidth(100f);

//                .addButton(getSliderLimitsButton());
        serverPropertyEntryFactory = new PropertyEntryFactory(serverParamsSideMenu.getCategoryNodes(), new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class), true);//TODO dynamic number factory
        clientPropertyEntryFactory = new PropertyEntryFactory(clientParamsSideMenu.getCategoryNodes(), new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class), false);//TODO dynamic number factory

        clientParamsSideMenu.setParameterConfiguration(clientParams, clientParameterConfiguration, clientPropertyEntryFactory);
        ChangeListener listener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                for (AbstractPropertyEntry e : clientParamsSideMenu.getPropertyEntries()) {
//                    e.setParamContainer(clientParams);
                    e.applyClientValue(clientParams);
                }
                stage.refreshRenderers();
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

    public CollapsiblePropertyList getServerParamsSideMenu() {
        return serverParamsSideMenu;
    }

    public CollapsiblePropertyList getClientParamsSideMenu() {
        return clientParamsSideMenu;
    }

    public void updateClientParamConfiguration(ParamConfiguration configuration){
        clientParamsSideMenu.getParamContainer().setParamConfiguration(configuration);
        clientParamsSideMenu.setParameterConfiguration(clientParamsSideMenu.getParamContainer(), configuration, clientPropertyEntryFactory);
    }

//    public CollapsiblePropertyListButton getSliderLimitsButton() {
//        return new CollapsiblePropertyListButton("toggle slider limits", "settings", new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                showSliderLimits = !showSliderLimits;
//            }
//        });
//    }

    public CollapsiblePropertyListButton getSwitchRenderersButton() {
        return new CollapsiblePropertyListButton("switch sets", "Calculator", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

                stage.pressedSwitchRenderers();
                stage.resetKeyboardFocus();
            }
        });
    }

    public void addToUiTable(VisTable ui) {
//        ui.debug();
        VisTable serverParamTable = new VisTable();
        VisTable clientParamTable = new VisTable();

        serverParamsSideMenu.addToTable(serverParamTable, Align.left);
        clientParamsSideMenu.addToTable(clientParamTable, Align.right);

        ui.add(serverParamTable).left().expandY();
        ui.add().expandX();
        ui.add(clientParamTable).right().expandY();
        ui.row();
    }

    public void refreshServerParameterUI(FractalRenderer renderer){
        setParameterConfiguration(renderer, serverParamsSideMenu, renderer.getSystemContext().getParamContainer(),
                renderer.getSystemContext().getParamConfiguration(), serverPropertyEntryFactory);
    }

    public void refreshClientParameterUI(FractalRenderer renderer){
        setParameterConfiguration(renderer, clientParamsSideMenu, stage.clientParams,
                stage.clientParamConfiguration, clientPropertyEntryFactory);
    }

    public void setServerParameterConfiguration(FractalRenderer renderer, ParamContainer paramContainer, ParamConfiguration parameterConfiguration){
        setParameterConfiguration(renderer, serverParamsSideMenu, paramContainer, parameterConfiguration, serverPropertyEntryFactory);
    }

    public void setClientParameterConfiguration(FractalRenderer renderer, ParamContainer paramContainer, ParamConfiguration parameterConfiguration){
        setParameterConfiguration(renderer, clientParamsSideMenu, paramContainer, parameterConfiguration, clientPropertyEntryFactory);
    }

    ChangeListener oldSubmitListener = null;

    public void setParameterConfiguration(FractalRenderer focusedRenderer, CollapsiblePropertyList list,
                                          ParamContainer paramContainer, ParamConfiguration parameterConfiguration, PropertyEntryFactory propertyEntryFactory){

        ChangeListener submitListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                stage.submitServer(focusedRenderer, paramContainer);
                stage.resetKeyboardFocus();
//                }
            }
        };

        list.setParameterConfiguration(paramContainer, parameterConfiguration, propertyEntryFactory);
        if (oldSubmitListener != null)
            list.removeSubmitListener(oldSubmitListener);
        oldSubmitListener = submitListener;
        list.addSubmitListener(submitListener);

        if (paramContainer == null)
            return;

        boolean reset = false;
        for (ParamSupplier supp : paramContainer.getParameters()){
            ParamDefinition paramDefinition = parameterConfiguration != null ? parameterConfiguration.getParamDefinition(supp.getName()) : null;
            if (supp.isChanged() && paramDefinition != null && paramDefinition.isResetRendererOnChange()) {
                reset = true;
                supp.updateChanged(supp);
            }
        }
        if (reset)
            focusedRenderer.reset();
        else
            focusedRenderer.setRefresh();
    }
}
