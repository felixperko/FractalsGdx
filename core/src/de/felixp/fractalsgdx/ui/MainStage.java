package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.rendering.RemoteRenderer;
import de.felixp.fractalsgdx.rendering.RendererContext;
import de.felixp.fractalsgdx.remoteclient.ClientSystem;
import de.felixp.fractalsgdx.remoteclient.SystemInterfaceGdx;
import de.felixp.fractalsgdx.rendering.RendererProperties;
import de.felixp.fractalsgdx.rendering.ShaderRenderer;
import de.felixp.fractalsgdx.rendering.palette.IPalette;
import de.felixp.fractalsgdx.rendering.palette.ImagePalette;
import de.felixp.fractalsgdx.rendering.rendererlink.JuliasetRendererLink;
import de.felixp.fractalsgdx.rendering.rendererlink.RendererLink;
import de.felixp.fractalsgdx.ui.actors.FractalsWindow;
import de.felixp.fractalsgdx.ui.entries.AbstractPropertyEntry;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.numbers.impl.DoubleComplexNumber;
import de.felixperko.fractals.system.numbers.impl.DoubleNumber;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.infra.Selection;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.util.NumberUtil;

public class MainStage extends Stage {

    final static String POSITIONS_PREFS_NAME = "de.felixp.fractalsgdx.ui.MainStage.positions";

    ParamUI paramUI;

    ParamContainer clientParams;

//    FractalRenderer renderer;
    FractalRenderer focusedRenderer = null;

    List<FractalRenderer> renderers = new ArrayList<>();

    Group rendererGroup;

    Table stateBar;

    Preferences positions_prefs;
    Map<String, ParamContainer> locations = new HashMap<>();

    Map<String, IPalette> palettes = new LinkedHashMap<>();

    Table activeSettingsTable = null;

    ParamConfiguration clientParamConfiguration;

    RendererLink juliasetLink;

    public MainStage(Viewport viewport, Batch batch){
        super(viewport, batch);
    }


    public void create(){

        for (FileHandle fileHandle : Gdx.files.internal("palettes").list()){
            String name = fileHandle.nameWithoutExtension();
            if (name.startsWith("palette-"))
                name = name.substring(8);
            palettes.put(name, new ImagePalette(fileHandle));
        }

        FractalRenderer renderer = new ShaderRenderer(new RendererContext(0, 0, 1f, 1, RendererProperties.ORIENTATION_FULLSCREEN));
        setFocusedRenderer(renderer);

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
            }
            locations.put(name, paramContainer);
        }

        VisTable ui = new VisTable();
        ui.align(Align.topLeft);
        ui.setFillParent(true);

        initRightParamContainer();
        ParamConfiguration clientParameterConfiguration = initRightParamConfiguration();

        //initRenderer ParamUI
        paramUI = new ParamUI(this);
        paramUI.init(clientParameterConfiguration, clientParams);

        //Topline
        VisTable topButtons = new VisTable();

        //TODO remove tests
//        Tooltip tooltip = new Tooltip.Builder("Tooltip").target(topButtons).build();
//        new Tooltip.Builder("Coming soon").target(connect).build();
//        connect.addListener(new TextTooltip("Tooltip test", VisUI.getSkin()));

        VisTextButton screenshotBtn = new VisTextButton("Screenshot");
        MainStage thisStage = this;
        screenshotBtn.addListener(new ClickListener(0){
            @Override
            public void clicked(InputEvent event, float x, float y) {
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
        VisTextButton animationsBtn = new VisTextButton("Animations", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openAnimationsWindow();
            }
        });
        VisTextButton settingsBtn = new VisTextButton("Settings", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openSettingsWindow();
            }
        });

        topButtons.add(screenshotBtn).pad(2);
        topButtons.add(positionsBtn).pad(2);
        topButtons.add(animationsBtn).pad(2);
        topButtons.add(settingsBtn).pad(2);

        ui.add(topButtons).align(Align.top).colspan(8).expandX().row();

        paramUI.addToUiTable(ui);


        stateBar = new Table();
        stateBar.align(Align.left);

        ui.add(stateBar).align(Align.bottomLeft).colspan(5);

        FractalRenderer renderer2 = new ShaderRenderer(
                new RendererContext(0.05f, 0.05f, 0.3f, 0.3f, RendererProperties.ORIENTATION_BOTTOM_RIGHT)
        );

        rendererGroup = new Group();

        addActor(rendererGroup);

        addActor(ui);

        addFractalRenderer(renderer);
        renderer.initRenderer();
        addFractalRenderer(renderer2);
        renderer2.initRenderer();

        //reset ParamUI focus to first renderer
        paramUI.setServerParameterConfiguration(renderer, renderer.getSystemContext().getParamContainer(), renderer.getSystemContext().getParamConfiguration());

        juliasetLink = new JuliasetRendererLink(renderer, renderer2);
        juliasetLink.syncTargetRenderer();
    }

    public void pressedSwitchRenderers(){
        juliasetLink.switchRenderers();
        //force reset side menu
        FractalRenderer renderer = getFocusedRenderer();
        SystemContext systemContext = renderer.getSystemContext();
        paramUI.getServerParamsSideMenu().setParameterConfiguration(null, null, null);
        paramUI.setServerParameterConfiguration(renderer, systemContext.getParamContainer(), systemContext.getParamConfiguration());
    }

    public void setPaletteTexture(String paletteName, Texture texture, boolean createIfNull){
        IPalette palette = palettes.get(paletteName);
        if (palette == null) {
            if (!createIfNull)
                return;
            palettes.put(paletteName, new ImagePalette(paletteName, texture));
            refreshClientSideMenu();
        }
        if (paletteName.equals(getClientParameter(PARAMS_COLOR_USE_PALETTE).getGeneral(String.class))){
            for (FractalRenderer renderer : renderers){
                renderer.setRefresh();
            }
        }
    }

    public void refreshClientSideMenu() {
        clientParamConfiguration = initRightParamConfiguration();
        clientParams.setParamConfiguration(clientParamConfiguration);
        paramUI.updateClientParamConfiguration(clientParamConfiguration);
//        paramUI.refreshClientParameterUI(focusedRenderer);
    }

    public Texture getPaletteTexture() {
        String paletteName = clientParams.getClientParameter("palette").getGeneral(String.class);
        if (paletteName.equalsIgnoreCase("none"))
            return null;
        return palettes.get(paletteName).getTexture();
    }

    public void updateClientParamConfiguration(){
        paramUI.updateClientParamConfiguration(initRightParamConfiguration());
    }

    @Override
    public void act(float delta) {
        handleInput();
        updateStatebar();
        super.act(delta);
    }

    public void submitServer(FractalRenderer renderer, ParamContainer paramContainer){
        ComplexNumber oldMidpoint = renderer.getSystemContext().getMidpoint();
        for (AbstractPropertyEntry entry : paramUI.getServerParamsSideMenu().getPropertyEntries()){
            entry.applyClientValue();
        }
        if (renderer instanceof RemoteRenderer) {
            ClientSystem clientSystem = ((RemoteRenderer) renderer).getSystemInterface().getClientSystem();
            if (paramContainer.needsReset(clientSystem.getOldParams())) {
                clientSystem.incrementJobId();
                paramContainer.getClientParameters().put("view", new StaticParamSupplier("view", clientSystem.getSystemContext().getViewId()));
            }
            clientSystem.setOldParams(paramContainer.getClientParameters());
            clientSystem.getSystemContext().setParameters(paramContainer);
            clientSystem.updateConfiguration();
            clientSystem.resetAnchor();//TODO integrate...
            renderer.reset();
        }
        else if (renderer instanceof ShaderRenderer){
            renderer.getSystemContext().setParameters(paramContainer);
            renderer.reset();
        }
        ComplexNumber newMidpoint = renderer.getSystemContext().getMidpoint();
        if (oldMidpoint != null && !oldMidpoint.equals(newMidpoint))
            renderer.getRendererContext().panned(paramContainer);
        paramUI.refreshServerParameterUI(renderer);
    }

    boolean escapeHandled = false;

    private void handleInput() {
        if (!escapeHandled && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            MainStageWindows.toggleSettingsWindow(this);
        escapeHandled = false;
        boolean controlPressed = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        boolean justNum1Pressed = Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1);
        boolean justNum2Pressed = Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2);

        if (controlPressed && justNum1Pressed){

            CollapsiblePropertyList serverList = paramUI.serverParamsSideMenu;
            serverList.getCollapseButton().toggle();

            if (!serverList.getCollapseButton().isChecked()){
                resetKeyboardFocus();
            } else {
                serverList.focusFirstFocusableControl();
            }

        }
        if (controlPressed && justNum2Pressed){
            CollapsiblePropertyList clientList = paramUI.clientParamsSideMenu;
            clientList.getCollapseButton().toggle();
            if (!clientList.getCollapseButton().isChecked()){
                resetKeyboardFocus();
            } else {
//                clientList.focusFirstFocusableControl();
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB) && getKeyboardFocus() == getFocusedRenderer()) {
            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)){
                //switch renderers
                pressedSwitchRenderers();
            } else {
                //switch focus
                boolean next = false;
                FractalRenderer nextFocusedRenderer = null;
                for (FractalRenderer renderer : getRenderers()){
                    if (next){
                        nextFocusedRenderer = renderer;
                        next = false;
                        break;
                    }
                    if (renderer == getFocusedRenderer()){
                        next = true;
                    }
                }
                if (next) //the next after the last is the first
                    nextFocusedRenderer = getRenderers().get(0);
                if (nextFocusedRenderer != null){
                    setFocusedRenderer(nextFocusedRenderer);
                }
            }
        }
//        System.out.println("MainStage focus: " + (getKeyboardFocus() == null ? null : getKeyboardFocus()));
    }

    private boolean groupHasChildInHierarchy(Group group, Actor checkActor){
        for (Actor actor : group.getChildren()){
            if (checkActor == actor)
                return true;
            if (actor instanceof Group && groupHasChildInHierarchy((Group)actor, checkActor))
                return true;
        }
        return false;
    }

    public final static String PARAMS_NUMBERFACTORY = "numberFactory";

    public final static String PARAMS_COLOR_ADD = "color offset";
    public final static String PARAMS_COLOR_MULT = "color period";
    //    public final static String PARAMS_SOBEL_FACTOR = "glow sensitivity";
    public final static String PARAMS_SOBEL_GLOW_LIMIT = "edge glow";
    public final static String PARAMS_SOBEL_DIM_PERIOD = "glow fade";
    public final static String PARAMS_AMBIENT_GLOW = "ambient light";

    public final static String PARAMS_COLOR_SATURATION = "saturation";
    public final static String PARAMS_COLOR_USE_PALETTE = "palette";
    public final static String PARAMS_EXTRACT_CHANNEL = "monochrome source";
    public final static String PARAMS_MAPPING_COLOR = "monochrome color";

    public final static String PARAMS_DRAW_AXIS = "draw axis";
    public final static String PARAMS_DRAW_ORBIT = "draw orbit";
    public final static String PARAMS_DRAW_PATH = "draw animation path";
    public final static String PARAMS_DRAW_MIDPOINT = "draw midpoint";
    public final static String PARAMS_DRAW_ZERO = "draw origin";

    public final static String PARAMS_ORBIT_ITERATIONS = "orbit iterations";
    public final static String PARAMS_ORBIT_TARGET = "orbit target";

    public final static String PARAMS_TRACES_VALUE = "trace position";
    public final static String PARAMS_TRACES_LINE_WIDTH = "line width";
    public final static String PARAMS_TRACES_POINT_SIZE = "point size";
    public final static String PARAMS_TRACES_LINE_TRANSPARENCY = "line transparency";
    public final static String PARAMS_TRACES_POINT_TRANSPARENCY = "point transparency";
    public final static String PARAMS_TRACES_START_COLOR = "";
    public final static String PARAMS_TRACES_END_COLOR = "";

    public final static NumberFactory nf = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

    protected void initRightParamContainer() {
        clientParams = new ParamContainer();
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_NUMBERFACTORY, nf));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_COLOR_USE_PALETTE, "none"));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_COLOR_MULT, nf.createNumber(2.0)));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_COLOR_ADD, nf.createNumber(0.0)));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_COLOR_SATURATION, nf.createNumber(0.5)));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_AMBIENT_GLOW, nf.createNumber(0.2)));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_SOBEL_GLOW_LIMIT, nf.createNumber(1.5)));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_SOBEL_DIM_PERIOD, nf.createNumber(3.0)));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_EXTRACT_CHANNEL, 0));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_MAPPING_COLOR, Color.WHITE));

        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_DRAW_PATH, true));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_DRAW_AXIS, false));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_DRAW_MIDPOINT, false));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_DRAW_ZERO, false));

        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_DRAW_ORBIT, false));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_ORBIT_ITERATIONS, 1000));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_ORBIT_TARGET, "mouse"));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_TRACES_VALUE, nf.createComplexNumber(0,0)));

        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_TRACES_LINE_WIDTH, 1.0));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_TRACES_POINT_SIZE, 3.0));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_TRACES_LINE_TRANSPARENCY, 0.5));
        clientParams.addClientParameter(new StaticParamSupplier(PARAMS_TRACES_POINT_TRANSPARENCY, 0.75));
    }

    protected ParamConfiguration initRightParamConfiguration() {
        ParamConfiguration config = new ParamConfiguration();

        ParamValueType integerType = new ParamValueType("integer");
        config.addValueType(integerType);
        ParamValueType doubleType = new ParamValueType("double");
        config.addValueType(doubleType);
        ParamValueType booleanType = new ParamValueType("boolean");
        config.addValueType(booleanType);
        ParamValueType selectionType = new ParamValueType("selection");
        config.addValueType(selectionType);
        ParamValueType stringType = new ParamValueType("string");
        config.addValueType(stringType);
        ParamValueType complexNumberType = new ParamValueType("complexnumber");
        config.addValueType(complexNumberType);
        ParamValueType numberType = new ParamValueType("number");
        config.addValueType(numberType);
        ParamValueType colorType = new ParamValueType("color");
        config.addValueType(colorType);


        config.addParameterDefinition(new ParamDefinition(PARAMS_COLOR_MULT, "coloring", StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0.02 max=10"), clientParams.getClientParameter(PARAMS_COLOR_MULT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_COLOR_ADD, "coloring", StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0 max=1"), clientParams.getClientParameter(PARAMS_COLOR_ADD));
//        config.addParameterDefinition(new ParameterDefinition(PARAMS_SOBEL_FACTOR, "coloring", StaticParamSupplier.class, doubleType));
        config.addParameterDefinition(new ParamDefinition(PARAMS_AMBIENT_GLOW, "coloring", StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=-1 max=1"), clientParams.getClientParameter(PARAMS_AMBIENT_GLOW));
        config.addParameterDefinition(new ParamDefinition(PARAMS_SOBEL_GLOW_LIMIT, "coloring", StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=-1 max=5"), clientParams.getClientParameter(PARAMS_SOBEL_GLOW_LIMIT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_SOBEL_DIM_PERIOD, "coloring", StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0.01 max=5"), clientParams.getClientParameter(PARAMS_SOBEL_DIM_PERIOD));
        config.addParameterDefinition(new ParamDefinition(PARAMS_COLOR_SATURATION, "coloring", StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0 max=1"), clientParams.getClientParameter(PARAMS_COLOR_SATURATION));

        config.addParameterDefinition(new ParamDefinition(PARAMS_COLOR_USE_PALETTE, "coloring II", StaticParamSupplier.class, selectionType),
                clientParams.getClientParameter(PARAMS_COLOR_USE_PALETTE));
        Selection<String> paletteSelection = new Selection<>(PARAMS_COLOR_USE_PALETTE);
        paletteSelection.addOption("none", "none", "No predefined palette");
        for (String paletteName : palettes.keySet())
            paletteSelection.addOption(paletteName, paletteName, "Palette '"+paletteName+"'");
        config.addSelection(paletteSelection);

        config.addParameterDefinition(new ParamDefinition(PARAMS_EXTRACT_CHANNEL, "coloring II", StaticParamSupplier.class, selectionType),
                clientParams.getClientParameter(PARAMS_EXTRACT_CHANNEL));
        Selection<Integer> extractChannelSelection = new Selection<Integer>(PARAMS_EXTRACT_CHANNEL);
        extractChannelSelection.addOption("none", 0, "No channel remapping");
        extractChannelSelection.addOption("red", 1, "Remap red channel to all (greyscale)");
        extractChannelSelection.addOption("green", 2, "Remap green channel to all (greyscale)");
        extractChannelSelection.addOption("blue", 3, "Remap blue channel to all (greyscale)");
        config.addSelection(extractChannelSelection);
        config.addParameterDefinition(new ParamDefinition(PARAMS_MAPPING_COLOR, "coloring II", StaticParamSupplier.class, colorType),
                clientParams.getClientParameter(PARAMS_MAPPING_COLOR));

        config.addParameterDefinition(new ParamDefinition(PARAMS_DRAW_AXIS, "shape rendering", StaticParamSupplier.class, booleanType),
                clientParams.getClientParameter(PARAMS_DRAW_AXIS));

        config.addParameterDefinition(new ParamDefinition(PARAMS_DRAW_ORBIT, "shape rendering", StaticParamSupplier.class, booleanType)
                , clientParams.getClientParameter(PARAMS_DRAW_ORBIT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_DRAW_PATH, "shape rendering", StaticParamSupplier.class, booleanType),
                clientParams.getClientParameter(PARAMS_DRAW_PATH));
        config.addParameterDefinition(new ParamDefinition(PARAMS_DRAW_MIDPOINT, "shape rendering", StaticParamSupplier.class, booleanType),
                clientParams.getClientParameter(PARAMS_DRAW_MIDPOINT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_DRAW_ZERO, "shape rendering", StaticParamSupplier.class, booleanType),
                clientParams.getClientParameter(PARAMS_DRAW_ZERO));
        config.addParameterDefinition(new ParamDefinition(PARAMS_ORBIT_TARGET, "shape rendering", StaticParamSupplier.class, selectionType)
                , clientParams.getClientParameter(PARAMS_ORBIT_TARGET));
        Selection<String> traceTargetSelection = new Selection<String>(PARAMS_ORBIT_TARGET);
        traceTargetSelection.addOption("mouse", "mouse", "The trace target is set to the current mouse position");
        traceTargetSelection.addOption("path", "path", "The trace target is set to the animation named 'path'");
        config.addSelection(traceTargetSelection);

        config.addParameterDefinition(new ParamDefinition(PARAMS_ORBIT_ITERATIONS, "shape rendering", StaticParamSupplier.class, integerType)
                , clientParams.getClientParameter(PARAMS_ORBIT_ITERATIONS));
        config.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_VALUE, "shape rendering", StaticParamSupplier.class, complexNumberType)
                        .withVisible(false), clientParams.getClientParameter(PARAMS_TRACES_VALUE));

        config.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_LINE_WIDTH, "shape settings", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0 max=3"), clientParams.getClientParameter(PARAMS_TRACES_LINE_WIDTH));
        config.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_POINT_SIZE, "shape settings", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0 max=10"), clientParams.getClientParameter(PARAMS_TRACES_POINT_SIZE));
        config.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_LINE_TRANSPARENCY, "shape settings", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0 max=1"), clientParams.getClientParameter(PARAMS_TRACES_LINE_TRANSPARENCY));
        config.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_POINT_TRANSPARENCY, "shape settings", StaticParamSupplier.class, doubleType)
                .withHints("ui-element[default]:slider min=0 max=1"), clientParams.getClientParameter(PARAMS_TRACES_POINT_TRANSPARENCY));

        //no client param resets the renderer
        for (ParamDefinition paramDef : config.getParameters())
            paramDef.setResetRendererOnChange(false);

        this.clientParamConfiguration = config;
        return config;
    }

    protected FractalRenderer getRenderer(int position){
        return position < 0 ? null : position >= renderers.size() ? null : renderers.get(position);
    }

    public void addFractalRenderer(FractalRenderer renderer){
        if (!(renderer instanceof Actor))
            throw new IllegalArgumentException("Renderer has to extend "+Actor.class);
        this.renderers.add(renderer);
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
        for (FractalRenderer renderer : renderers)
            renderer.updateSize();

        for (Actor actor : getActors()){
            if (actor instanceof FractalsWindow){
                if (((FractalsWindow)actor).isAutoReposition())
                    ((FractalsWindow)actor).reposition();
            }
            else if (actor instanceof VisWindow){
                ((VisWindow) actor).pack();
                ((VisWindow) actor).centerWindow();
            }
        }
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return true;
    }

    public ParamSupplier getClientParameter(String name){
        return clientParams.getClientParameter(name);
    }

    VisLabel fpsLabel = null;
    double updateInterval = 0.1;
    double longestFrametime = -1;
    long lastStatebarUpdate = -1;
    long lastLongestFrametime = -1;
    long longestFrametimeTimeout = 1*NumberUtil.S_TO_NS;

    public void updateStatebar(){

        boolean longestFrameExpired = (System.nanoTime()-lastLongestFrametime) > longestFrametimeTimeout;
        if (Gdx.graphics.getDeltaTime() > longestFrametime || longestFrameExpired){
            lastLongestFrametime = System.nanoTime();
            longestFrametime = Gdx.graphics.getDeltaTime();
        }

        if ((System.nanoTime()-lastStatebarUpdate)*NumberUtil.NS_TO_S < updateInterval)
            return;

        if (fpsLabel == null){
            fpsLabel = new VisLabel("FPS: ");

            stateBar.add(fpsLabel);
        }

        lastStatebarUpdate = System.nanoTime();

        double frametime = Gdx.graphics.getDeltaTime();
        double fps = 1.0/frametime;
        fpsLabel.setText("FPS: "+((int)(fps*10.0))/10.0+" ("+ formatFrametime(frametime) +" ms, max: "+formatFrametime(longestFrametime)+" ms)");


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
    }

    public double formatFrametime(double frametime) {
        return ((int)(frametime*1000.0*10.0))/10.0;
    }

    public void openConnectWindow(String text){
        MainStageWindows.openConnectWindow(this, text);
    }

    public void openSettingsWindow(){
        MainStageWindows.openSettingsMenu(this);
    }

    private void openAnimationsWindow() {
        AnimationsUI.openAnimationsWindow(this);
    }

    private String getPrintString(ComplexNumber number, int precision){
        return NumberUtil.getRoundedDouble(number.getReal().toDouble(), precision)+", "+NumberUtil.getRoundedDouble(number.getImag().toDouble(), precision);
    }

    @Override
    public void dispose(){
        super.dispose();
    }

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
                    submitServer(focusedRenderer, paramContainer);
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

    public void setFocusedRenderer(FractalRenderer newFocusedRenderer) {

        if (this.focusedRenderer != null)
            this.focusedRenderer.setFocused(false);

        this.focusedRenderer = newFocusedRenderer;
        newFocusedRenderer.setFocused(true);
        if (newFocusedRenderer instanceof Actor) {
            setKeyboardFocus((Actor) newFocusedRenderer);
            setScrollFocus((Actor) newFocusedRenderer);
        }
    }

    public ParamConfiguration getClientParamConfiguration() {
        return clientParamConfiguration;
    }

    public void escapeHandled() {
        escapeHandled = true;
    }

    public void resetKeyboardFocus() {
        if (focusedRenderer != null && focusedRenderer instanceof Actor){
            setKeyboardFocus((Actor)focusedRenderer);
            focusedRenderer.setFocused(true);
        }
    }

    public Map<String, IPalette> getPalettes() {
        return palettes;
    }

    public void addPalette(IPalette palette) {
        boolean isNew = this.palettes.containsKey(palette.getName());
        this.palettes.put(palette.getName(), palette);
        if (isNew){
            refreshClientSideMenu();
        }
    }
}
