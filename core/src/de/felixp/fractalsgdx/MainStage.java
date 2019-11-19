package de.felixp.fractalsgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisRadioButton;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisWindow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import de.felixp.fractalsgdx.client.ClientSystem;
import de.felixp.fractalsgdx.client.SystemInterfaceGdx;
import de.felixp.fractalsgdx.ui.CollapsiblePropertyList;
import de.felixp.fractalsgdx.ui.entries.AbstractPropertyEntry;
import de.felixp.fractalsgdx.ui.entries.PropertyEntryFactory;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.network.interfaces.ClientMessageInterface;
import de.felixperko.fractals.system.numbers.impl.DoubleComplexNumber;
import de.felixperko.fractals.system.numbers.impl.DoubleNumber;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.ParameterConfiguration;
import de.felixperko.fractals.system.parameters.ParameterDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BFSystemContext;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.util.NumberUtil;

import static de.felixp.fractalsgdx.FractalsGdxMain.UI_PREFS_NAME;
import static de.felixp.fractalsgdx.FractalsGdxMain.UI_PREFS_SCALE;

public class MainStage extends Stage {

    final static String POSITIONS_PREFS_NAME = "de.felixp.fractalsgdx.MainStage.positions";

    public final static String PARAMS_COLOR_ADD = "color shift";
    public final static String PARAMS_COLOR_MULT = "color period";
    public final static String PARAMS_SOBEL_FACTOR = "glow sensitivity";
    public final static String PARAMS_SOBEL_GLOW_LIMIT = "glow brightness";
    public final static String PARAMS_SOBEL_DIM_PERIOD = "glow dim period";
    public final static String PARAMS_AMBIENT_GLOW = "ambient light";


    AbstractRenderer renderer;

//    private VisTable collapsibleTable;

//    CollapsibleWidget collapsibleWidget;
//    VisTextButton collapseButton;

//    VisTextButton submitButton;

    CollapsiblePropertyList serverParamsSideMenu;
    PropertyEntryFactory serverPropertyEntryFactory;
//    List<AbstractPropertyEntry> propertyEntryList = new ArrayList<>();

    CollapsiblePropertyList clientParamsSideMenu;
    PropertyEntryFactory clientPropertyEntryFactory;
    ParamContainer clientParams;

    Table stateBar;

    Preferences positions_prefs;
    Map<String, ParamContainer> locations = new HashMap<>();


    public MainStage(Viewport viewport, Batch batch){
        super(viewport, batch);
    }

    public void create(){

//        Gdx.graphics.setContinuousRendering(false);

        renderer = new RemoteRenderer();
        renderer.init();

        renderer.setFillParent(true);

        positions_prefs = Gdx.app.getPreferences(POSITIONS_PREFS_NAME);
        Map<String, ?> map = positions_prefs.get();
        for (Map.Entry<String, ?> e : map.entrySet()) {
            if (!(e.getValue() instanceof String))
                throw new IllegalStateException("Preferences "+POSITIONS_PREFS_NAME+" contain non-String objects.");
            String name = e.getKey();

            ParamContainer paramContainer = null;
            try {
                paramContainer = ParamContainer.deserializeBase64((String)e.getValue());
            } catch (IOException | ClassNotFoundException e1) {
                e1.printStackTrace();
//                throw new IllegalStateException("Exception: \n"+e1.getMessage());
            }
            locations.put(name, paramContainer);
        }

        VisTable ui = new VisTable();
        ui.align(Align.topLeft);
        ui.setFillParent(true);

        //init menus at sides
        //
        serverParamsSideMenu = new CollapsiblePropertyList();
        clientParamsSideMenu = new CollapsiblePropertyList();
        serverPropertyEntryFactory = new PropertyEntryFactory(serverParamsSideMenu.getCollapsibleTable(), new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class));//TODO dynamic number factory
        clientPropertyEntryFactory = new PropertyEntryFactory(clientParamsSideMenu.getCollapsibleTable(), new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class));//TODO dynamic number factory

        //ParameterConfiguration for client parameters:
        ParameterConfiguration clientParameterConfiguration = new ParameterConfiguration();

        ParamValueType doubleType = new ParamValueType("double");
        clientParameterConfiguration.addValueType(doubleType);

        clientParameterConfiguration.addParameterDefinition(new ParameterDefinition(PARAMS_COLOR_MULT, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0.02 max=10"));
        clientParameterConfiguration.addParameterDefinition(new ParameterDefinition(PARAMS_COLOR_ADD, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0 max=1"));
        clientParameterConfiguration.addParameterDefinition(new ParameterDefinition(PARAMS_SOBEL_FACTOR, "coloring", StaticParamSupplier.class, doubleType));
        clientParameterConfiguration.addParameterDefinition(new ParameterDefinition(PARAMS_AMBIENT_GLOW, "coloring", StaticParamSupplier.class, doubleType));
        clientParameterConfiguration.addParameterDefinition(new ParameterDefinition(PARAMS_SOBEL_GLOW_LIMIT, "coloring", StaticParamSupplier.class, doubleType));
        clientParameterConfiguration.addParameterDefinition(new ParameterDefinition(PARAMS_SOBEL_DIM_PERIOD, "coloring", StaticParamSupplier.class, doubleType));

        //create suppliers
        clientParams = new ParamContainer();
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_COLOR_MULT, 3.));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_COLOR_ADD, 0.));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_SOBEL_FACTOR, 1.));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_AMBIENT_GLOW, 0.2));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_SOBEL_GLOW_LIMIT, 0.8));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_SOBEL_DIM_PERIOD, 1.));

        clientParamsSideMenu.setParameterConfiguration(clientParams, clientParameterConfiguration, clientPropertyEntryFactory);
        ChangeListener listener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                for (AbstractPropertyEntry e : clientParamsSideMenu.getPropertyEntries())
                    e.applyClientValue();
            }
        };
        clientParamsSideMenu.addAllListener(listener);

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
        VisTextButton setWindowSize = new VisTextButton("Window", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openWindowMenu();
            }
        });

        topButtons.add(connect).pad(2);
        topButtons.add(screenshot).pad(2);
        topButtons.add(positions).pad(2);
        topButtons.add(setWindowSize).pad(2);

        ui.add(topButtons).align(Align.top).expandX().colspan(3).row();

        serverParamsSideMenu.addToTable(ui, Align.left);
        ui.add().expandX();
        clientParamsSideMenu.addToTable(ui, Align.right);
        ui.row();

        //collapsible left
//        collapsibleTable.add().expand(false, true).fill(false, true);
//        collapsibleWidget = new CollapsibleWidget(collapsibleTable, true);
//
//        collapseButton = new VisTextButton(">", new ChangeListener() {
//            @Override
//            public void changed(ChangeEvent event, Actor actor) {
//                collapsibleWidget.setCollapsed(!collapsibleWidget.isCollapsed());
//                collapsibleWidget.pack();
//                collapseButton.layout();
//                if (collapsibleWidget.isCollapsed()){
////                    collapseButton.setPosition(0, Gdx.graphics.getHeight()/2, Align.left);
//                    collapseButton.setText(">");
//                } else {
////                    collapseButton.setPosition(collapsibleWidget.getWidth(), Gdx.graphics.getHeight()/2, Align.left);
//                    collapseButton.setText("<");
//////                    collapsibleWidget.setPosition(0, (Gdx.graphics.getHeight())*0.5f, Align.left);
//                }
//            }
//        });
//
//        collapsibleTable.row();
//
//        //add
//        ui.add(collapseButton).align(Align.left);
//        ui.add(collapsibleWidget).align(Align.left).expandY();


        stateBar = new Table();
        stateBar.align(Align.left);

        ui.add(stateBar).align(Align.bottomLeft).colspan(3);

        addActor(renderer);
        addActor(ui);
    }

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
                SystemContext systemContext = systemInterface.getSystemContext();
                ClientSystem clientSystem = systemInterface.getClientSystem();
                ParamContainer container = locations.get(selection.getSelected());
                if (container != null) {
                    Integer viewId = systemContext.getViewId();
                    Integer newViewId = viewId + 1;
                    ParamContainer newContainer = new ParamContainer(container, true);
                    newContainer.addClientParameter(new StaticParamSupplier("view", newViewId));
                    boolean reset = systemContext.setParameters(newContainer);
                    if (reset) {
//                        systemContext.setViewId(newViewId);
                        renderer.reset();
                    }
                    clientSystem.updateConfiguration();
                    clientSystem.resetAnchor();
                    setParameterConfiguration(serverParamsSideMenu, systemContext.getParamContainer(), ((RemoteRenderer) renderer).getSystemInterface().getParamConfiguration(), serverPropertyEntryFactory);//TODO put in updateConfiguration()?
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

    private void openWindowMenu(){

        Window window = new VisWindow("Window settings");
        ((VisWindow) window).addCloseButton();

        VisRadioButton fullscreenBtn = new VisRadioButton("Fullscreen");

        VisSelectBox<String> displayModeSelect = new VisSelectBox();
        Map<Graphics.DisplayMode, String> names = new HashMap<>();
        Map<String, Graphics.DisplayMode> modes = new HashMap<>();
        Map<String, Graphics.DisplayMode> modePerResolution = new HashMap<>();
        for (Graphics.DisplayMode mode : Gdx.graphics.getDisplayModes()){
            String res = mode.width+"x"+mode.height;
            Graphics.DisplayMode oldMode = modePerResolution.get(res);
            if (oldMode == null || oldMode.refreshRate < mode.refreshRate)
                modePerResolution.put(res, mode);
        }
        List<Graphics.DisplayMode> items = new ArrayList<>();
        nextResolution:
        for (Graphics.DisplayMode mode : modePerResolution.values()){
            String name = mode.width+"x"+mode.height+" @"+mode.refreshRate+"hz";
            names.put(mode, name);
            modes.put(name, mode);
            for (int i = 0 ; i < items.size() ; i++) {
                Graphics.DisplayMode other = items.get(i);
                if (mode.width > other.width) {
                    items.add(i, mode);
                    continue nextResolution;
                }
            }
            items.add(mode);
        }
        String[] itemArray = new String[items.size()];
        for (int i = 0 ; i < itemArray.length ; i++)
            itemArray[i] = names.get(items.get(i));
        displayModeSelect.setItems(itemArray);
        Graphics.DisplayMode current = Gdx.graphics.getDisplayMode();
        displayModeSelect.setSelected(names.get(modePerResolution.get(current.width+"x"+current.height)));

        int resolutionWidthPx = 50;
        VisRadioButton windowedBtn = new VisRadioButton("Windowed");

        ButtonGroup group = new ButtonGroup();
        group.add(fullscreenBtn);
        group.add(windowedBtn);

        fullscreenBtn.setChecked(Gdx.graphics.isFullscreen());
        windowedBtn.setChecked(!Gdx.graphics.isFullscreen());

        VisTextField xResultionField = new VisTextField(""+(int)Gdx.graphics.getWidth())
//        {
//            @Override
//            public float getPrefWidth() {
//                return resolutionWidthPx;
//            }
//        }
        ;
        VisLabel resultionMultLbl = new VisLabel("x");
        VisTextField yResultionField = new VisTextField(""+(int)Gdx.graphics.getHeight())
//        {
//            @Override
//            public float getPrefWidth() {
//                return resolutionWidthPx;
//            }
//        }
        ;
        yResultionField.setWidth(5);

        //
        // UI scale
        //

        Preferences uiPrefs = Gdx.app.getPreferences(UI_PREFS_NAME);
        int scale = 1;
        if (uiPrefs.contains(UI_PREFS_SCALE)){
            scale = uiPrefs.getInteger(UI_PREFS_SCALE);
        }

        VisLabel scaleDescLbl = new VisLabel("Interface scale (requires restart)");

        VisRadioButton scale1Btn = new VisRadioButton("100%");
        VisRadioButton scale2Btn = new VisRadioButton("200%");
        scale1Btn.setChecked(scale != 2);
        scale2Btn.setChecked(scale == 2);
        scale1Btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                uiPrefs.putInteger(UI_PREFS_SCALE, 1);
            }
        });
        scale2Btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                uiPrefs.putInteger(UI_PREFS_SCALE, 2);
            }
        });

        ButtonGroup group2 = new ButtonGroup();
        group2.add(scale1Btn);
        group2.add(scale2Btn);

        //
        // Actions
        //

        VisTextButton applyBtn = new VisTextButton("Apply");
        VisTextButton cancelBtn = new VisTextButton("Cancel");

        applyBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                uiPrefs.flush();
                boolean setFullscreen = fullscreenBtn.isChecked();
                int width = Integer.parseInt(xResultionField.getText());
                int height = Integer.parseInt(yResultionField.getText());
                if (setFullscreen){
                    Gdx.graphics.setFullscreenMode(modes.get(displayModeSelect.getSelected()));
                }
                else if (Gdx.graphics.isFullscreen() || width != Gdx.graphics.getWidth() ||  height != Gdx.graphics.getHeight()){
                    Gdx.graphics.setWindowedMode(width, height);
                }
                window.remove();
            }
        });
        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                window.remove();
            }
        });

        //
        // add all
        //
        VisTable fullscreenRow = new VisTable();
        fullscreenRow.add(fullscreenBtn).pad(2);
        fullscreenRow.add(displayModeSelect).pad(2);
        window.add(fullscreenRow).left().row();

        VisTable windowedRow = new VisTable();
        windowedRow.add(windowedBtn).left().pad(2);
        windowedRow.add(xResultionField).pad(2);
        windowedRow.add(resultionMultLbl).pad(2);
        windowedRow.add(yResultionField).pad(2);
        window.add(windowedRow).left().row();

        window.add().row();
        window.add(scaleDescLbl).left().row();
        window.add(scale1Btn).left().row();
        window.add(scale2Btn).left().row();

        window.add().row();
        VisTable btnRow = new VisTable();
        btnRow.add(applyBtn).pad(2).center();
        btnRow.add(cancelBtn).pad(2).center();
        window.add(btnRow);

        addActor(window);
        window.pack();
        ((VisWindow)window).centerWindow();
    }

    private void updateParamSelectBox(VisSelectBox selection) {
        Array array = new Array();
        for (String name : locations.keySet())
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
            if (!locations.containsKey(generated_name)){
                nameFld.setText(generated_name);
                break;
            }
        }
        nameFld.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean disable = nameFld.isEmpty() || locations.containsKey(nameFld.getText());
                saveBtn.setDisabled(disable);
            }
        });
        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                window.remove();
            }
        });
        saveBtn.setDisabled(nameFld.isEmpty() || locations.containsKey(nameFld.getText()));
        saveBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ParamContainer data = ((RemoteRenderer)renderer).getSystemInterface().getParamContainer();
                ParamContainer container = new ParamContainer(data, true);
                container.getClientParameters().remove("view");
                locations.put(nameFld.getText(), container);

                try {
                    positions_prefs.putString(nameFld.getText(), container.serializeBase64());
                    positions_prefs.flush();
                } catch (IOException e) {
                    throw new IllegalStateException("couldn't serialize locations");
                }
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

//    public void addEntry(AbstractPropertyEntry entry){
//        propertyEntryList.add(entry);
//    }

    public void setServerParameterConfiguration(ParamContainer paramContainer, ParameterConfiguration parameterConfiguration){
        setParameterConfiguration(serverParamsSideMenu, paramContainer, parameterConfiguration, serverPropertyEntryFactory);
    }

    public void submitServer(ParamContainer paramContainer){
        ClientSystem clientSystem = ((RemoteRenderer)renderer).getSystemInterface().getClientSystem();
        clientSystem.setOldParams(paramContainer.getClientParameters());
        for (AbstractPropertyEntry entry : serverParamsSideMenu.getPropertyEntries()){
            entry.applyClientValue();
        }
        if (paramContainer.needsReset(clientSystem.getOldParams())) {
            clientSystem.incrementJobId();
            renderer.reset();
        }
        clientSystem.updateConfiguration();
    }

    public void setParameterConfiguration(CollapsiblePropertyList list, ParamContainer paramContainer, ParameterConfiguration parameterConfiguration, PropertyEntryFactory propertyEntryFactory){

        ChangeListener submitListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                submitServer(paramContainer);
//                }
            }
        };

        list.setParameterConfiguration(paramContainer, parameterConfiguration, propertyEntryFactory);
        list.addSubmitListener(submitListener);

        renderer.setRefresh();
        updateStateBar();
//        for (AbstractPropertyEntry entry : propertyEntryList) {
//            entry.closeView(AbstractPropertyEntry.VIEW_LIST);
//        }
//        propertyEntryList.clear();
//
//        VisTable collapsibleTable = serverParamsSideMenu.getCollapsibleTable();
//
//        collapsibleTable.clear();
//
//        //NumberFactory numberFactory = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);
//
//        //addEntry(new IntTextPropertyEntry(collapsibleTable, systemClientData, "iterations"));
//        //addEntry(new ComplexNumberPropertyEntry(collapsibleTable, systemClientData, "pow", numberFactory));
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
//                entry.openView(AbstractPropertyEntry.VIEW_LIST, collapsibleTable);
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
//        collapsibleTable.add();
//        collapsibleTable.add(submitButton).row();
//
//        renderer.setRefresh();
//        updateStateBar();
    }

    public ParamSupplier getClientParameter(String name){
        return clientParams.getClientParameter(name);
    }

    public void updateStateBar(){
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();

        ClientMessageInterface messageInterface = FractalsGdxMain.client.getMessageInterface();
        try {
            SystemInterfaceGdx systemInterface = (SystemInterfaceGdx) messageInterface.getSystemInterface(messageInterface.getRegisteredSystems().iterator().next());//TODO ...
            stateBar.clear();
            ComplexNumber midpoint = systemInterface.getParamContainer().getClientParameter("midpoint").getGeneral(ComplexNumber.class);
            ComplexNumber screenCoords = systemInterface.toComplex(mouseX, mouseY);
            ComplexNumber worldCoords = systemInterface.getWorldCoords(screenCoords);
            //ComplexNumber chunkCoords = systemInterface.getChunkGridCoords(worldCoords);
            BFSystemContext systemContext = (BFSystemContext) systemInterface.getSystemContext();
            ComplexNumber chunkCoords = systemContext.getNumberFactory().createComplexNumber(systemContext.getChunkX(worldCoords), systemContext.getChunkY(worldCoords));
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
