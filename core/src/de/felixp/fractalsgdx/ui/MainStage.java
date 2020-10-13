package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.kotcrab.vis.ui.widget.ScrollableTextArea;
import com.kotcrab.vis.ui.widget.VisLabel;
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
import java.util.UUID;

import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.rendering.PanListener;
import de.felixp.fractalsgdx.rendering.RemoteRenderer;
import de.felixp.fractalsgdx.rendering.RendererContext;
import de.felixp.fractalsgdx.remoteclient.ClientSystem;
import de.felixp.fractalsgdx.remoteclient.SystemInterfaceGdx;
import de.felixp.fractalsgdx.interpolation.LinearDoubleParameterInterpolation;
import de.felixp.fractalsgdx.rendering.ShaderRenderer;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.suppliers.CoordinateBasicShiftParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.infra.Selection;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.util.NumberUtil;

public class MainStage extends Stage {

    final static String POSITIONS_PREFS_NAME = "de.felixp.fractalsgdx.ui.MainStage.positions";

    public final static String PARAMS_COLOR_ADD = "color offset";
    public final static String PARAMS_COLOR_MULT = "color change rate";
    public final static String PARAMS_COLOR_SATURATION = "saturation";
//    public final static String PARAMS_SOBEL_FACTOR = "glow sensitivity";
    public final static String PARAMS_SOBEL_GLOW_LIMIT = "edge glow brightness";
    public final static String PARAMS_SOBEL_DIM_PERIOD = "edge glow sensitivity";
    public final static String PARAMS_AMBIENT_GLOW = "background brightness";
    public final static String PARAMS_EXTRACT_CHANNEL = "map channel";
    public final static String PARAMS_MAPPING_COLOR_R = "mapping red";
    public final static String PARAMS_MAPPING_COLOR_G = "mapping green";
    public final static String PARAMS_MAPPING_COLOR_B = "mapping blue";

    public final static String PARAMS_AXIS_ALPHA = "axis alpha";

    public final static String PARAMS_TRACES_ITERATIONS = "iterations";
    public final static String PARAMS_TRACES_VARIABLE = "position variable";
    public final static String PARAMS_TRACES_LINE_WIDTH = "line width";
    public final static String PARAMS_TRACES_POINT_SIZE = "point size";
    public final static String PARAMS_TRACES_LINE_TRANSPARENCY = "line transparency";
    public final static String PARAMS_TRACES_POINT_TRANSPARENCY = "point transparency";
    public final static String PARAMS_TRACES_START_COLOR = "";
    public final static String PARAMS_TRACES_END_COLOR = "";

    ParamUI paramUI;

    ParamContainer clientParams;

//    FractalRenderer renderer;
    FractalRenderer focusedRenderer = null;

    List<FractalRenderer> renderers = new ArrayList<>();

    Group rendererGroup;

//    Table stateBar;

    Preferences positions_prefs;
    Map<String, ParamContainer> locations = new HashMap<>();

    Table activeSettingsTable = null;

    public MainStage(Viewport viewport, Batch batch){
        super(viewport, batch);
    }


    public void create(){



//        Gdx.graphics.setContinuousRendering(false);

        FractalRenderer renderer;

//        renderer = new ShaderRenderer(new RendererContext(0, 0, 0.5f, 1));
//        renderer = new RemoteRenderer(new RendererContext(0, 0, 0.5f, 1));

        renderer = new ShaderRenderer(new RendererContext(0, 0, 1f, 1)
                , true
        );

        this.focusedRenderer = renderer;

//        renderer.setFillParent(true);

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

//        ui.setDebug(true);



        //ParameterConfiguration for client parameters:
        ParamConfiguration clientParameterConfiguration = new ParamConfiguration();

        ParamValueType doubleType = new ParamValueType("double");
        clientParameterConfiguration.addValueType(doubleType);
        ParamValueType integerType = new ParamValueType("integer");
        clientParameterConfiguration.addValueType(integerType);
        ParamValueType selectionType = new ParamValueType("selection");
        clientParameterConfiguration.addValueType(selectionType);
        ParamValueType stringType = new ParamValueType("string");
        clientParameterConfiguration.addValueType(stringType);


        //create suppliers
        clientParams = new ParamContainer();
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_COLOR_MULT, 3.0));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_COLOR_ADD, 0.0));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_COLOR_SATURATION, 0.4));
//        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_SOBEL_FACTOR, 1.0));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_AMBIENT_GLOW, 0.2));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_SOBEL_GLOW_LIMIT, 1.5));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_SOBEL_DIM_PERIOD, 3.0));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_EXTRACT_CHANNEL, 1));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_MAPPING_COLOR_R, 1.0));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_MAPPING_COLOR_G, 1.0));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_MAPPING_COLOR_B, 1.0));

        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_AXIS_ALPHA, 0.0));

        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_TRACES_ITERATIONS, 1000));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_TRACES_VARIABLE, "mouse"));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_TRACES_LINE_WIDTH, 1.0));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_TRACES_POINT_SIZE, 3.0));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_TRACES_LINE_TRANSPARENCY, 0.5));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_TRACES_POINT_TRANSPARENCY, 0.75));

        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_COLOR_MULT, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0.02 max=10"), clientParams.getClientParameter(PARAMS_COLOR_MULT));
        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_COLOR_ADD, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0 max=1"), clientParams.getClientParameter(PARAMS_COLOR_ADD));
        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_COLOR_SATURATION, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0 max=1"), clientParams.getClientParameter(PARAMS_COLOR_SATURATION));
//        clientParameterConfiguration.addParameterDefinition(new ParameterDefinition(PARAMS_SOBEL_FACTOR, "coloring", StaticParamSupplier.class, doubleType));
        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_AMBIENT_GLOW, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=-1 max=1"), clientParams.getClientParameter(PARAMS_AMBIENT_GLOW));
        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_SOBEL_GLOW_LIMIT, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=-1 max=5"), clientParams.getClientParameter(PARAMS_SOBEL_GLOW_LIMIT));
        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_SOBEL_DIM_PERIOD, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0.01 max=5"), clientParams.getClientParameter(PARAMS_SOBEL_DIM_PERIOD));
        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_EXTRACT_CHANNEL, "coloring", StaticParamSupplier.class, selectionType),
                clientParams.getClientParameter(PARAMS_EXTRACT_CHANNEL));

        Selection<Integer> extractChannelSelection = new Selection<Integer>(PARAMS_EXTRACT_CHANNEL);
        extractChannelSelection.addOption("none", 0, "No channel remapping");
        extractChannelSelection.addOption("red", 1, "Remap red channel to all (greyscale)");
        extractChannelSelection.addOption("green", 2, "Remap green channel to all (greyscale)");
        extractChannelSelection.addOption("blue", 3, "Remap blue channel to all (greyscale)");
        clientParameterConfiguration.addSelection(extractChannelSelection);
        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_MAPPING_COLOR_R, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0.0 max=1.0"), clientParams.getClientParameter(PARAMS_MAPPING_COLOR_R));
        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_MAPPING_COLOR_G, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0.0 max=1.0"), clientParams.getClientParameter(PARAMS_MAPPING_COLOR_G));
        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_MAPPING_COLOR_B, "coloring", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0.0 max=1.0"), clientParams.getClientParameter(PARAMS_MAPPING_COLOR_B));

        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_AXIS_ALPHA, "interface", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0 max=1"), clientParams.getClientParameter(PARAMS_AXIS_ALPHA));

        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_ITERATIONS, "traces", StaticParamSupplier.class, integerType)
                , clientParams.getClientParameter(PARAMS_TRACES_ITERATIONS));
        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_VARIABLE, "traces", StaticParamSupplier.class, stringType)
                .withHints("ui-element[default]:slider min=0 max=1"), clientParams.getClientParameter(PARAMS_TRACES_VARIABLE));
        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_LINE_WIDTH, "traces", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0 max=3"), clientParams.getClientParameter(PARAMS_TRACES_LINE_WIDTH));
        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_POINT_SIZE, "traces", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0 max=10"), clientParams.getClientParameter(PARAMS_TRACES_POINT_SIZE));
        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_LINE_TRANSPARENCY, "traces", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0 max=1"), clientParams.getClientParameter(PARAMS_TRACES_LINE_TRANSPARENCY));
        clientParameterConfiguration.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_POINT_TRANSPARENCY, "traces", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0 max=1"), clientParams.getClientParameter(PARAMS_TRACES_POINT_TRANSPARENCY));

        //init ParamUI
        paramUI = new ParamUI(this);
        paramUI.init(clientParameterConfiguration, clientParams);

        //Topline
        VisTable topButtons = new VisTable();
        //topButtons.align(Align.top);

        VisTextButton connectBtn = new VisTextButton("Connect to Server", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openConnectWindow(null);
            }
        });

                //TODO remove test
//        new Tooltip.Builder("Coming soon").target(connect).build();
//        connect.addListener(new TextTooltip("Tooltip test", VisUI.getSkin()));


        VisTextButton screenshotBtn = new VisTextButton("Screenshot");
        MainStage thisStage = this;
        screenshotBtn.addListener(new ClickListener(0){
            @Override
            public void clicked(InputEvent event, float x, float y) {
//                renderer.setScreenshot(true);
                ScreenshotUI.openScreenshotWindow(thisStage);
                super.clicked(event, x, y);
            }
        });
        VisTextButton positionsBtn = new VisTextButton("Jump to...", new ChangeListener(){
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openJumpToWindow();
            }
        });
        VisTextButton settingsBtn = new VisTextButton("Settings", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openSettingsWindow();
            }
        });

        topButtons.add(connectBtn).pad(2);
        topButtons.add(screenshotBtn).pad(2);
        topButtons.add(positionsBtn).pad(2);
        topButtons.add(settingsBtn).pad(2);

        ui.add(topButtons).align(Align.top).colspan(8).expandX().row();

        paramUI.addToUiTable(ui);


//        stateBar = new Table();
//        stateBar.align(Align.left);

//        ui.add(stateBar).align(Align.bottomLeft).colspan(5);

        FractalRenderer renderer2 = new ShaderRenderer(
                new RendererContext(0.65f, 0.05f, 0.3f, 0.3f)
                , true
        );

        rendererGroup = new Group();

        addActor(rendererGroup);

        addActor(ui);

        addFractalRenderer(renderer);
        renderer.init();
        addFractalRenderer(renderer2);
        renderer2.init();

//        renderer.addParameterInterpolationClient(new LinearDoubleParameterInterpolation("edge glow sensitivity", 10, 0.01, 5));
//        renderer.addParameterInterpolationClient(new LinearDoubleParameterInterpolation("color offset", 10, 1, 0));

//        renderer2.getSystemContext().getParamContainer().addClientParameter(new CoordinateBasicShiftParamSupplier("c"));

        renderer.addPanListener(new PanListener() {
            @Override
            public void panned(ComplexNumber midpoint) {
                FractalRenderer renderer1 = getRenderer(0);
                FractalRenderer renderer2 = getRenderer(1);
                if (renderer1 != null && renderer2 != null)
                    mapJuliasetParams(renderer1, renderer2);
            }
        });
        mapJuliasetParams(renderer, renderer2);


//        renderer.setWidth(Gdx.graphics.getWidth()*0.5f);
    }

    protected FractalRenderer getRenderer(int position){
        return position < 0 ? null : position >= renderers.size() ? null : renderers.get(position);
    }

    protected void mapJuliasetParams(FractalRenderer sourceRenderer, FractalRenderer targetRenderer) {
        SystemContext systemContext1 = sourceRenderer.getSystemContext();
        SystemContext systemContext2 = targetRenderer.getSystemContext();
        ParamContainer paramContainer1 = systemContext1.getParamContainer();
        ParamContainer paramContainer2 = systemContext2.getParamContainer();

        boolean setJuliaset2 = (paramContainer1.getClientParameter("start") instanceof StaticParamSupplier);

        if (setJuliaset2) {
            paramContainer2.addClientParameter(new StaticParamSupplier("c", paramContainer1.getClientParameter("midpoint").getGeneral()));
            paramContainer2.addClientParameter(new CoordinateBasicShiftParamSupplier("start"));
        }
        else {
            paramContainer2.addClientParameter(new CoordinateBasicShiftParamSupplier("c"));
            ParamSupplier existingStartSupp = paramContainer2.getClientParameter("start");
            if (existingStartSupp == null || !(existingStartSupp instanceof StaticParamSupplier))
                paramContainer2.addClientParameter(new StaticParamSupplier("start", systemContext2.getNumberFactory().createComplexNumber(0,0)));
        }
        paramContainer2.addClientParameter(new StaticParamSupplier("iterations", systemContext1.getParamValue("iterations")));
        paramContainer2.addClientParameter(new StaticParamSupplier("f(z)=", systemContext1.getParamValue("f(z)=")));
        paramContainer2.addClientParameter(new StaticParamSupplier("limit", systemContext1.getParamValue("limit")));
        ((ShaderRenderer) targetRenderer).paramsChanged();
    }

    public void addFractalRenderer(FractalRenderer renderer){
        if (!(renderer instanceof Actor))
            throw new IllegalArgumentException("Renderer has to extend "+Actor.class);
        this.renderers.add(renderer);
//        VisTable rendererTable = new VisTable(true);
//        rendererTable.add(renderer);
        rendererGroup.addActor((Actor)renderer);
        renderer.updateSize();
    }

    public void removeFractalRenderer(FractalRenderer renderer){
        renderer.removed();
        this.renderers.remove(renderer);
        if (renderer instanceof Actor)
            rendererGroup.removeActor((Actor)renderer);
    }

    public void resize(int width, int height){
//        renderer.setBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        int i = 0;
        for (FractalRenderer renderer : renderers)
            renderer.updateSize();
//            renderer.setBounds(Gdx.graphics.getWidth()*0.5f*i++, 0, Gdx.graphics.getWidth()*0.5f, Gdx.graphics.getHeight());
//            renderer.setSize(Gdx.graphics.getWidth()*0.5f, Gdx.graphics.getHeight());
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

    public void openConnectWindow(String text){
        MainStageWindows.openConnectWindow(this, text);
    }

    public void openSettingsWindow(){
        MainStageWindows.openSettingsMenu(this);
    }

    private String getPrintString(ComplexNumber number, int precision){
        return NumberUtil.getRoundedDouble(number.getReal().toDouble(), precision)+", "+NumberUtil.getRoundedDouble(number.getImag().toDouble(), precision);
    }

    @Override
    public void dispose(){
        super.dispose();
    }

//    public FractalRenderer getRenderer() {
//        return renderer;
//    }

    public FractalRenderer getRemoteRendererById(UUID systemId){
        for (FractalRenderer renderer : renderers)
            if (renderer instanceof RemoteRenderer && ((RemoteRenderer)renderer).getSystemId().equals(systemId))
                return renderer;
        return null;
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
                SystemContext systemContext = focusedRenderer.getSystemContext();
                ClientSystem clientSystem = null;
                if (focusedRenderer instanceof RemoteRenderer) {
                    SystemInterfaceGdx systemInterface = ((RemoteRenderer) focusedRenderer).getSystemInterface();
                    clientSystem = systemInterface.getClientSystem();
                }
                ParamContainer container = locations.get(selection.getSelected());
                if (container != null) {
                    Integer viewId = systemContext.getViewId();
                    Integer newViewId = viewId + 1;
                    ParamContainer newContainer = new ParamContainer(container, true);
                    newContainer.addClientParameter(new StaticParamSupplier("view", newViewId));
                    boolean reset = systemContext.setParameters(newContainer);
                    if (reset) {
//                        systemContext.setViewId(newViewId);
                        focusedRenderer.reset();
                    }
                    if (clientSystem != null) {
                        clientSystem.updateConfiguration();
                        clientSystem.resetAnchor();
                    }
                    paramUI.setParameterConfiguration(focusedRenderer, paramUI.serverParamsSideMenu, systemContext.getParamContainer(), systemContext.getParamConfiguration(), paramUI.serverPropertyEntryFactory);//TODO put in updateConfiguration()?
                }
            }
        });

        String paramText = "";
        try {
            ParamContainer paramContainer = paramUI.serverParamsSideMenu.getParamContainer();
            if (paramContainer != null) {
                ParamContainer newContainer = new ParamContainer(paramContainer, true);
                paramUI.serverParamsSideMenu.propertyEntryList.forEach(pe -> pe.applyClientValue(newContainer));
                paramText = newContainer.serializeJson(true).replaceAll("\r\n", "\n");
            }
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
                    focusedRenderer.reset();
                    paramUI.submitServer(focusedRenderer, paramContainer);
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
                ParamContainer data = focusedRenderer.getSystemContext().getParamContainer();
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

    public Table getActiveSettingsTable() {
        return activeSettingsTable;
    }

    public void setActiveSettingsTable(Table activeSettingsTable) {
        this.activeSettingsTable = activeSettingsTable;
    }

    public List<FractalRenderer> getRenderers(){
        return renderers;
    }

    public ParamContainer getClientParameters() {
        return clientParams;
    }

    public FractalRenderer getFocusedRenderer() {
        return focusedRenderer;
    }
}
