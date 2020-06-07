package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.kotcrab.vis.ui.widget.ScrollableTextArea;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisRadioButton;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextArea;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisWindow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import de.felixp.fractalsgdx.AbstractRenderer;
import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.RemoteRenderer;
import de.felixp.fractalsgdx.ShaderRenderer;
import de.felixp.fractalsgdx.client.ClientSystem;
import de.felixp.fractalsgdx.client.SystemInterfaceGdx;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.network.interfaces.ClientMessageInterface;
import de.felixperko.fractals.system.numbers.ComplexNumber;
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

    final static String POSITIONS_PREFS_NAME = "de.felixp.fractalsgdx.ui.MainStage.positions";

    public final static String PARAMS_COLOR_ADD = "color offset";
    public final static String PARAMS_COLOR_MULT = "color change rate";
//    public final static String PARAMS_SOBEL_FACTOR = "glow sensitivity";
    public final static String PARAMS_SOBEL_GLOW_LIMIT = "edge glow brightness";
    public final static String PARAMS_SOBEL_DIM_PERIOD = "edge glow sensitivity";
    public final static String PARAMS_AMBIENT_GLOW = "background brightness";

    ParamUI paramUI;

    ParamContainer clientParams;

    AbstractRenderer renderer;


//    Table stateBar;

    Preferences positions_prefs;
    Map<String, ParamContainer> locations = new HashMap<>();


    public MainStage(Viewport viewport, Batch batch){
        super(viewport, batch);
    }


    public void create(){

//        Gdx.graphics.setContinuousRendering(false);

//        renderer = new ShaderRenderer();
        renderer = new RemoteRenderer();

        renderer.setFillParent(true);

        positions_prefs = Gdx.app.getPreferences(POSITIONS_PREFS_NAME);
        Map<String, ?> map = positions_prefs.get();
        for (Map.Entry<String, ?> e : map.entrySet()) {
            if (!(e.getValue() instanceof String))
                throw new IllegalStateException("Preferences "+POSITIONS_PREFS_NAME+" contain non-String objects.");
            String name = e.getKey();

            ParamContainer paramContainer = null;
            try {
                paramContainer = ParamContainer.deserializeObjectBase64((String)e.getValue());
            } catch (IOException | ClassNotFoundException e1) {
                e1.printStackTrace();
//                throw new IllegalStateException("Exception: \n"+e1.getMessage());
            }
            locations.put(name, paramContainer);
        }

        VisTable ui = new VisTable();
        ui.align(Align.topLeft);
        ui.setFillParent(true);



        //ParameterConfiguration for client parameters:
        ParameterConfiguration clientParameterConfiguration = new ParameterConfiguration();

        ParamValueType doubleType = new ParamValueType("double");
        clientParameterConfiguration.addValueType(doubleType);

        clientParameterConfiguration.addParameterDefinition(new ParameterDefinition(PARAMS_COLOR_MULT, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0.02 max=10"));
        clientParameterConfiguration.addParameterDefinition(new ParameterDefinition(PARAMS_COLOR_ADD, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0 max=1"));
//        clientParameterConfiguration.addParameterDefinition(new ParameterDefinition(PARAMS_SOBEL_FACTOR, "coloring", StaticParamSupplier.class, doubleType));
        clientParameterConfiguration.addParameterDefinition(new ParameterDefinition(PARAMS_AMBIENT_GLOW, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=-1 max=1"));
        clientParameterConfiguration.addParameterDefinition(new ParameterDefinition(PARAMS_SOBEL_GLOW_LIMIT, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=-1 max=5"));
        clientParameterConfiguration.addParameterDefinition(new ParameterDefinition(PARAMS_SOBEL_DIM_PERIOD, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0.01 max=5"));

        //create suppliers
        clientParams = new ParamContainer();
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_COLOR_MULT, 3.0));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_COLOR_ADD, 0.0));
//        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_SOBEL_FACTOR, 1.0));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_AMBIENT_GLOW, 0.2));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_SOBEL_GLOW_LIMIT, 1.5));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_SOBEL_DIM_PERIOD, 3.0));

        //init ParamUI
        paramUI = new ParamUI(this);
        paramUI.init(clientParameterConfiguration, clientParams);

        //Topline
        VisTable topButtons = new VisTable();
        //topButtons.align(Align.top);

        VisTextButton connect = new VisTextButton("Connect to Server");

        //TODO remove test
//        new Tooltip.Builder("Coming soon").target(connect).build();
//        connect.addListener(new TextTooltip("Tooltip test", VisUI.getSkin()));


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

        ui.add(topButtons).align(Align.top).colspan(5).expandX().row();

        paramUI.addToUiTable(ui);


//        stateBar = new Table();
//        stateBar.align(Align.left);

//        ui.add(stateBar).align(Align.bottomLeft).colspan(5);

        addActor(renderer);
        addActor(ui);

        renderer.init();
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
//        updateStateBar();
        return true;
    }

//    public void addEntry(AbstractPropertyEntry entry){
//        propertyEntryList.add(entry);
//    }

    public ParamSupplier getClientParameter(String name){
        return clientParams.getClientParameter(name);
    }

//    public void updateStateBar(){
//        int mouseX = Gdx.input.getX();
//        int mouseY = Gdx.input.getY();
//
//        if (renderer instanceof RemoteRenderer) {
//            ClientMessageInterface messageInterface = FractalsGdxMain.client.getMessageInterface();
//            try {
//                SystemInterfaceGdx systemInterface = (SystemInterfaceGdx) messageInterface.getSystemInterface(messageInterface.getRegisteredSystems().iterator().next());//TODO ...
//                stateBar.clear();
//                ComplexNumber midpoint = systemInterface.getParamContainer().getClientParameter("midpoint").getGeneral(ComplexNumber.class);
//                ComplexNumber screenCoords = systemInterface.toComplex(mouseX, mouseY);
//                ComplexNumber worldCoords = systemInterface.getWorldCoords(screenCoords);
//                //ComplexNumber chunkCoords = systemInterface.getChunkGridCoords(worldCoords);
//                BFSystemContext systemContext = (BFSystemContext) systemInterface.getSystemContext();
//                ComplexNumber chunkCoords = systemContext.getNumberFactory().createComplexNumber(systemContext.getChunkX(worldCoords), systemContext.getChunkY(worldCoords));
//                stateBar.add(new VisLabel("midpoint: " + getPrintString(midpoint, 3))).left().row();
//                stateBar.add(new VisLabel("ScreenPos: " + mouseX + ", " + mouseY)).left().row();
//                stateBar.add(new VisLabel("WorldPos: " + getPrintString(worldCoords, 3))).left().row();
//                stateBar.add(new VisLabel("ChunkPos: " + getPrintString(chunkCoords, 3))).left();
//            } catch (NoSuchElementException e) {
//                return;
//            }
//        }
//    }

    private String getPrintString(ComplexNumber number, int precision){
        return NumberUtil.getRoundedDouble(number.getReal().toDouble(), precision)+", "+NumberUtil.getRoundedDouble(number.getImag().toDouble(), precision);
    }

    public void resize(int width, int height){

    }

    @Override
    public void dispose(){
        super.dispose();
    }

    public AbstractRenderer getRenderer() {
        return renderer;
    }

    public ParamUI getParamUI() {
        return paramUI;
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
                    paramUI.setParameterConfiguration(paramUI.serverParamsSideMenu, systemContext.getParamContainer(), ((RemoteRenderer) renderer).getSystemInterface().getParamConfiguration(), paramUI.serverPropertyEntryFactory);//TODO put in updateConfiguration()?
                }
            }
        });
        String paramText = "";
        try {
            ParamContainer newContainer = new ParamContainer(paramUI.serverParamsSideMenu.getParamContainer(), true);
            paramUI.serverParamsSideMenu.propertyEntryList.forEach(pe -> pe.applyClientValue(newContainer));
            paramText = newContainer.serializeJson(true).replaceAll("\r\n", "\n");
        } catch (IOException e){
            e.printStackTrace();
        }
        ScrollableTextArea parameterTextArea = new ScrollableTextArea(paramText);
        ScrollPane scrollPane = parameterTextArea.createCompatibleScrollPane();


        VisTextButton applyBtn = new VisTextButton("Apply", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    ParamContainer paramContainer = ParamContainer.deserializeJson(parameterTextArea.getText());
                    renderer.reset();
                    paramUI.submitServer(paramContainer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        window.add(cancelBtn).pad(2);
        window.add(saveBtn).pad(2);
        window.add(jumptoBtn).pad(2);
        window.row();
        window.add(scrollPane).colspan(3).width(Gdx.graphics.getWidth()*.5f).height(Gdx.graphics.getHeight()*.5f).row();
        window.add(applyBtn);
        addActor(window);
        window.pack();
        ((VisWindow) window).centerWindow();

        parameterTextArea.setText(paramText);
    }

    private void openWindowMenu(){

        Window window = new VisWindow("Window settings");
        ((VisWindow) window).addCloseButton();

        VisRadioButton fullscreenBtn = new VisRadioButton("Fullscreen");

        VisSelectBox<String> displayModeSelect = new VisSelectBox();
        Map<Graphics.DisplayMode, String> names = new HashMap<>();
        Map<String, Graphics.DisplayMode> modes = new HashMap<>();
        Map<String, Graphics.DisplayMode> modePerResolution = new HashMap<>();
        for (Graphics.Monitor monitor : Gdx.graphics.getMonitors()) {
            for (Graphics.DisplayMode mode : Gdx.graphics.getDisplayModes(monitor)) {
                String res = monitor.name + " " + mode.width + "x" + mode.height;
                Graphics.DisplayMode oldMode = modePerResolution.get(res);
                if (oldMode == null || oldMode.refreshRate < mode.refreshRate)
                    modePerResolution.put(res, mode);
            }
        }
        List<Graphics.DisplayMode> items = new ArrayList<>();
        nextResolution:
        for (Map.Entry<String, Graphics.DisplayMode> e: modePerResolution.entrySet()){
            Graphics.DisplayMode mode = e.getValue();
//            String name = e.getKey()+"@"+mode.refreshRate+"hz";
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
                    positions_prefs.putString(nameFld.getText(), container.serializeObjectBase64());
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
}
