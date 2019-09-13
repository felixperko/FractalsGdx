package de.felixp.fractalsgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.kotcrab.vis.ui.widget.CollapsibleWidget;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisWindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import de.felixp.fractalsgdx.client.ClientSystem;
import de.felixp.fractalsgdx.client.SystemInterfaceGdx;
import de.felixp.fractalsgdx.ui.entries.AbstractPropertyEntry;
import de.felixp.fractalsgdx.ui.entries.PropertyEntryFactory;
import de.felixperko.fractals.network.ParamContainer;
import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.network.interfaces.ClientMessageInterface;
import de.felixperko.fractals.system.Numbers.DoubleComplexNumber;
import de.felixperko.fractals.system.Numbers.DoubleNumber;
import de.felixperko.fractals.system.Numbers.infra.ComplexNumber;
import de.felixperko.fractals.system.Numbers.infra.NumberFactory;
import de.felixperko.fractals.system.parameters.ParameterConfiguration;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.util.NumberUtil;

public class MainStage extends Stage {

    private VisTable collapsibleTable;

    AbstractRenderer renderer;

    CollapsibleWidget collapsibleWidget;
    VisTextButton collapseButton;

    VisTextButton submitButton;

    PropertyEntryFactory propertyEntryFactory;
    List<AbstractPropertyEntry> propertyEntryList = new ArrayList<>();

    Table stateBar;

    public MainStage(Viewport viewport, Batch batch){
        super(viewport, batch);
    }

    public void create(){

        Gdx.graphics.setContinuousRendering(false);

        renderer = new RemoteRenderer();
        renderer.init();


        VisTable ui = new VisTable();
        ui.align(Align.topLeft);
        ui.setFillParent(true);
//        addActor(ui);

        collapsibleTable = new VisTable();

        propertyEntryFactory = new PropertyEntryFactory(collapsibleTable, new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class));//TODO dynamic number factory

        //Topline
        VisTable topButtons = new VisTable();
        //topButtons.align(Align.top);

        VisTextButton connect = new VisTextButton("Connect to Server");
        VisTextButton screenshot = new VisTextButton("Screenshot");
        screenshot.addListener(new ClickListener(0){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                renderer.setScreenshot(true);
                super.clicked(event, x, y);
            }
        });
        VisTextButton positions = new VisTextButton("Jump to...", new ChangeListener(){
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openJumpToWindow();
            }
        });

        topButtons.add(connect).pad(2);
        topButtons.add(screenshot).pad(2);
        topButtons.add(positions).pad(2);

        //collapsible left
        collapsibleTable.add().expand(false, true).fill(false, true);
        collapsibleWidget = new CollapsibleWidget(collapsibleTable, true);

        collapseButton = new VisTextButton(">", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                collapsibleWidget.setCollapsed(!collapsibleWidget.isCollapsed());
                collapsibleWidget.pack();
                collapseButton.layout();
                if (collapsibleWidget.isCollapsed()){
//                    collapseButton.setPosition(0, Gdx.graphics.getHeight()/2, Align.left);
                    collapseButton.setText(">");
                } else {
//                    collapseButton.setPosition(collapsibleWidget.getWidth(), Gdx.graphics.getHeight()/2, Align.left);
                    collapseButton.setText("<");
////                    collapsibleWidget.setPosition(0, (Gdx.graphics.getHeight())*0.5f, Align.left);
                }
            }
        });

        stateBar = new Table();
        stateBar.align(Align.left);

        //add
        ui.add(topButtons).align(Align.top).expandX().colspan(3).row();
        ui.add(collapseButton).align(Align.left);
        ui.add(collapsibleWidget).align(Align.left).expandY();
        ui.add().expandX().row();
        ui.add(stateBar).align(Align.bottomLeft).colspan(3);

        collapsibleTable.row();

        addActor(renderer);
        addActor(ui);
    }

    Map<String, ParamContainer> params = new HashMap<>();

    private void openJumpToWindow(){
        Window window = new VisWindow("Jump to...");
        ((VisWindow) window).addCloseButton();
        VisSelectBox selection = new VisSelectBox();
        updateParamSelectBox(selection);
        window.add(selection).colspan(3).fillX().pad(2).row();

        VisTextButton cancelBtn = new VisTextButton("cancel");
        VisTextButton saveBtn = new VisTextButton("save current");
        VisTextButton jumptoBtn = new VisTextButton("jump to");

        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                window.remove();
            }
        });
        saveBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openSaveLocationWindow(selection);
            }
        });
        jumptoBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SystemInterfaceGdx systemInterface = ((RemoteRenderer)renderer).getSystemInterface();
                SystemClientData systemClientData = systemInterface.getSystemClientData();
                ClientSystem clientSystem = systemInterface.getClientSystem();
                ParamContainer container = params.get(selection.getSelected());
                if (container != null) {
                    boolean update = systemClientData.applyParamsAndNeedsReset(container);
                    ParamSupplier viewSupplier = systemClientData.getClientParameter("view");
                    systemClientData.getClientParameters().put("view", new StaticParamSupplier("view", viewSupplier.getGeneral(Integer.class)+1));
                    //if (update)
//                        FractalsGdxMain.client.incrementJobId();
                    clientSystem.updateConfiguration();
                    clientSystem.resetAnchor();
                    setParameterConfiguration(systemClientData, ((RemoteRenderer) renderer).getSystemInterface().getParamConfiguration());//TODO put in updateConfiguration()?
                    renderer.reset();
                }
            }
        });

        window.add(cancelBtn).pad(2);
        window.add(saveBtn).pad(2);
        window.add(jumptoBtn).pad(2);
        addActor(window);
        window.pack();
        ((VisWindow) window).centerWindow();
    }

    private void updateParamSelectBox(VisSelectBox selection) {
        Array array = new Array();
        for (String name : params.keySet())
            array.add(name);
        selection.setItems(array);
    }

    private void openSaveLocationWindow(VisSelectBox selection){
        VisWindow window = new VisWindow("Save location");
        window.addCloseButton();

        VisLabel nameLbl = new VisLabel("name");
        VisTextField nameFld = new VisTextField();
        VisTextButton cancelBtn = new VisTextButton("cancel");
        VisTextButton saveBtn = new VisTextButton("save");

        for (int i = 1 ; i < 1000 ; i++){
            String generated_name = "location "+i;
            if (!params.containsKey(generated_name)){
                nameFld.setText(generated_name);
                break;
            }
        }
        nameFld.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean disable = nameFld.isEmpty() || params.containsKey(nameFld.getText());
                saveBtn.setDisabled(disable);
            }
        });
        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                window.remove();
            }
        });
        saveBtn.setDisabled(nameFld.isEmpty() || params.containsKey(nameFld.getText()));
        saveBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SystemClientData data = ((RemoteRenderer)renderer).getSystemInterface().getSystemClientData();
                ParamContainer container = data.exportParams();
                container.getClientParameters().remove("view");
                params.put(nameFld.getText(), container);
                updateParamSelectBox(selection);
                window.remove();
            }
        });

        window.add(nameLbl);
        window.add(nameFld).pad(2).row();

        VisTable btnTable = new VisTable();
        btnTable.add(cancelBtn).pad(2);
        btnTable.add(saveBtn).pad(2);
        window.add(btnTable).colspan(2);

        addActor(window);
        window.pack();
        window.centerWindow();
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        updateStateBar();
        return true;
    }

    public void addEntry(AbstractPropertyEntry entry){
        propertyEntryList.add(entry);
    }

    public void setParameterConfiguration(ParamContainer paramContainer, ParameterConfiguration parameterConfiguration){
        for (AbstractPropertyEntry entry : propertyEntryList) {
            entry.closeView(AbstractPropertyEntry.VIEW_LIST);
        }
        propertyEntryList.clear();

        collapsibleTable.clear();

        //NumberFactory numberFactory = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

        //addEntry(new IntTextPropertyEntry(collapsibleTable, systemClientData, "iterations"));
        //addEntry(new ComplexNumberPropertyEntry(collapsibleTable, systemClientData, "pow", numberFactory));

        if (submitButton != null)
            submitButton.remove();

        List<ParameterDefinition> parameterDefinitions = new ArrayList<>(parameterConfiguration.getParameters());
        List<ParameterDefinition> calculatorParameterDefinitions = parameterConfiguration.getCalculatorParameters(paramContainer.getClientParameter("calculator").getGeneral(String.class));
        parameterDefinitions.addAll(calculatorParameterDefinitions);
        for (ParameterDefinition parameterDefinition : parameterDefinitions) {
            AbstractPropertyEntry entry = propertyEntryFactory.getPropertyEntry(parameterDefinition, paramContainer);
            if (entry != null) {
                entry.init();
                entry.openView(AbstractPropertyEntry.VIEW_LIST, collapsibleTable);
                addEntry(entry);
            }
        }

        ClientSystem clientSystem = ((RemoteRenderer)renderer).getSystemInterface().getClientSystem();

        submitButton = new VisTextButton("Submit", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                clientSystem.setOldParams(paramContainer.getClientParameters());
                for (AbstractPropertyEntry entry : propertyEntryList){
                    entry.applyValue();
                }
                if (((SystemClientData)paramContainer).needsReset(clientSystem.getOldParams()))//TODO move up
                    clientSystem.incrementJobId();
                clientSystem.updateConfiguration();
                renderer.reset();
            }
        });

        collapsibleTable.add();
        collapsibleTable.add(submitButton).row();

        renderer.setRefresh();
        updateStateBar();
    }

    public void updateStateBar(){
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();

        ClientMessageInterface messageInterface = FractalsGdxMain.client.getMessageInterface();
        try {
            SystemInterfaceGdx systemInterface = (SystemInterfaceGdx) messageInterface.getSystemInterface(messageInterface.getRegisteredSystems().iterator().next());//TODO ...
            stateBar.clear();
            ComplexNumber midpoint = systemInterface.getSystemClientData().getClientParameter("midpoint").getGeneral(ComplexNumber.class);
            ComplexNumber screenCoords = systemInterface.toComplex(mouseX, mouseY);
            ComplexNumber worldCoords = systemInterface.getWorldCoords(screenCoords);
            ComplexNumber chunkCoords = systemInterface.getChunkGridCoords(worldCoords);
            stateBar.add(new VisLabel("midpoint: "+getPrintString(midpoint, 3))).left().row();
            stateBar.add(new VisLabel("ScreenPos: "+mouseX+", "+mouseY)).left().row();
            stateBar.add(new VisLabel("WorldPos: "+getPrintString(worldCoords, 3))).left().row();
            stateBar.add(new VisLabel("ChunkPos: "+getPrintString(chunkCoords, 3))).left();
        } catch (NoSuchElementException e){
            return;
        }
    }

    private String getPrintString(ComplexNumber number, int precision){
        return NumberUtil.getRoundedDouble(number.getReal().toDouble(), precision)+", "+NumberUtil.getRoundedDouble(number.getImag().toDouble(), precision);
    }

    public void resize(int width, int height){

    }

    @Override
    public void dispose(){
        super.dispose();
    }

    public RemoteRenderer getRenderer() {
        return (RemoteRenderer)renderer;//TODO replace
    }
}
