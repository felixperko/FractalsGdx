package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
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

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.rendering.ShaderSystemContext;
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
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.numbers.impl.DoubleComplexNumber;
import de.felixperko.fractals.system.numbers.impl.DoubleNumber;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.ParamValueType;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;
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

    List<Keybinding> keybindings = new ArrayList<>();

    public MainStage(Viewport viewport, Batch batch){
        super(viewport, batch);
    }


    public void create(){

        createKeybindings();

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

        ParamConfiguration clientParameterConfiguration = initRightParamConfiguration();
        initRightParamContainer();

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
        VisTextButton positionsBtn = new VisTextButton("Presets", new ChangeListener(){
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
//        paramUI.setServerParameterConfiguration(renderer, systemContext.getParamContainer(), systemContext.getParamConfiguration());
    }

    public void setPaletteTexture(String paletteName, Texture texture, boolean createIfNull){
        IPalette palette = palettes.get(paletteName);
        if (palette == null) {
            if (!createIfNull)
                return;
            palettes.put(paletteName, new ImagePalette(paletteName, texture));
            refreshClientSideMenu();
        }
        if (paletteName.equals(getClientParam(PARAMS_PALETTE).getGeneral(String.class))){
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

    public void refreshRenderers(){
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                for (FractalRenderer renderer : renderers){
                    renderer.setRefresh();
                }
            }
        });
    }

    Texture blackTexture = new Texture(128,1, Pixmap.Format.RGB888);
    Texture blackTexture2 = new Texture(128,1, Pixmap.Format.RGB888);

    public Texture getPaletteTexture(String postprocessParameter, int fallbackIndex) {
        String paletteName = clientParams.getParam(postprocessParameter).getGeneral(String.class);
        Texture fallbackTexture = fallbackIndex == 0 ? blackTexture : blackTexture2;
        if (paletteName.equalsIgnoreCase(PARAMVALUE_PALETTE_DISABLED))
            return fallbackTexture;
        IPalette palette = palettes.get(paletteName);
        if (palette == null)
            return fallbackTexture;
        return palette.getTexture();
    }

    public void updateClientParamConfiguration(){
        paramUI.updateClientParamConfiguration(initRightParamConfiguration());
    }

    @Override
    public void act(float delta) {
        updateTimeBudgets();
        handleInput();
        updateStatebar();
        super.act(delta);
    }

    private void updateTimeBudgets() {
        Object fpsObj = getRenderers().get(0).getSystemContext().getParamValue(ShaderSystemContext.PARAM_TARGET_FRAMERATE);
        if (fpsObj == null || !(fpsObj instanceof Integer || fpsObj instanceof Double || fpsObj instanceof Float))
            return;
        if ((int)fpsObj == 0) {
            for (FractalRenderer renderer : getRenderers())
                renderer.setTimeBudget(Integer.MAX_VALUE);
        }
        else {
            double totalTimeBudget = 1.0 / (int) fpsObj;
            double lastDelta = Gdx.graphics.getDeltaTime();

            Map<FractalRenderer, Double> rendererPrios = new HashMap<>();
            double totalPrio = 0.0;
            for (FractalRenderer renderer : getRenderers()) {
                Object prioObj = renderer.getSystemContext().getParamValue(ShaderSystemContext.PARAM_PRIORITY);
                Double prio = prioObj == null || !(prioObj instanceof Number) ? 1.0 : ((Number) prioObj).toDouble();
                prio *= renderer.getPixelCount();
                rendererPrios.put(renderer, prio);
                totalPrio += prio;
            }
            for (Map.Entry<FractalRenderer, Double> e : rendererPrios.entrySet()) {
                Double rendererPrio = e.getValue();
                double newTimeBudget = totalTimeBudget * rendererPrio / totalPrio;
                e.getKey().setTimeBudget(newTimeBudget);
            }
        }
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
                paramContainer.getParamMap().put("view", new StaticParamSupplier("view", clientSystem.getSystemContext().getViewId()));
            }
            clientSystem.setOldParams(paramContainer.getParamMap());
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

    private void createKeybindings() {
        MainStage thisStage = this;
        keybindings.add(new CalcMultKeybinding("increase iterations", CommonFractalParameters.PARAM_ITERATIONS, 2.0, Input.Keys.NUMPAD_ADD));
        keybindings.add(new CalcMultKeybinding("decrease iterations", CommonFractalParameters.PARAM_ITERATIONS, 0.5, Input.Keys.NUMPAD_SUBTRACT));
        keybindings.add(new Keybinding("toggle fullscreen/windowed", Input.Keys.F11) {
            @Override
            public void apply() {
                FractalsGdxMain.windowed = !FractalsGdxMain.windowed;
                FractalsGdxMain.uiScaleWorkaround = false;
                FractalsGdxMain.setScreenMode(FractalsGdxMain.windowed, 1280, 720);
            }
        });
        keybindings.add(new Keybinding("switch focused renderer", Input.Keys.TAB) {
            @Override
            public void apply() {
                if (getKeyboardFocus() == getFocusedRenderer()) {
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
        });
        keybindings.add(new Keybinding("switch renderer positions", Input.Keys.SHIFT_LEFT, Input.Keys.TAB) {
            @Override
            public void apply() {
                pressedSwitchRenderers();
            }
        });
        Keybinding openCalculationParamMenuBinding = new Keybinding("open calculation param menu", Input.Keys.CONTROL_LEFT, Input.Keys.NUM_1) {
            @Override
            public void apply() {
                CollapsiblePropertyList serverList = paramUI.serverParamsSideMenu;
                serverList.getCollapseButton().toggle();
                if (!serverList.getCollapseButton().isChecked()) {
                    resetKeyboardFocus();
                } else {
                    serverList.focusFirstFocusableControl();
                }
            }
        };
        keybindings.add(openCalculationParamMenuBinding);
        Keybinding openPostprocessingParamMenuBinding = new Keybinding("open postprocess param menu", Input.Keys.CONTROL_LEFT, Input.Keys.NUM_2) {
            @Override
            public void apply() {
                CollapsiblePropertyList clientList = paramUI.clientParamsSideMenu;
                clientList.getCollapseButton().toggle();
                if (!clientList.getCollapseButton().isChecked()) {
                    resetKeyboardFocus();
                } else {
                    clientList.focusFirstFocusableControl();
                }
            }
        };
        keybindings.add(openPostprocessingParamMenuBinding);
        Keybinding resetKeyboardFocusBinding = new Keybinding("reset focus/close window", Input.Keys.ESCAPE) {
            @Override
            public void apply() {
                if (!escapeHandled) {
                    MainStageWindows.toggleSettingsWindow(thisStage);
                    escapeHandled = true;
                }
            }
        };
        keybindings.add(resetKeyboardFocusBinding);
        keybindings.add(new PostprocessToggleKeybinding("toggle orbit traces", PARAMS_DRAW_ORBIT, Input.Keys.O));
        keybindings.add(new PostprocessToggleKeybinding("toggle trace instructions", PARAMS_ORBIT_TRACE_PER_INSTRUCTION, Input.Keys.I));
        keybindings.add(new PostprocessToggleKeybinding("toggle overlay midpoint", PARAMS_DRAW_MIDPOINT, Input.Keys.M));
        keybindings.add(new PostprocessToggleKeybinding("toggle overlay axis", PARAMS_DRAW_AXIS, Input.Keys.C));;
        keybindings.add(new PostprocessToggleKeybinding("toggle overlay zero", PARAMS_DRAW_ZERO, Input.Keys.Z));

        openCalculationParamMenuBinding.setRequiresRendererFocus(false);
        openPostprocessingParamMenuBinding.setRequiresRendererFocus(false);
        resetKeyboardFocusBinding.setRequiresRendererFocus(false);
    }

    private void handleInput() {

    for (Keybinding keybinding : keybindings)
        keybinding.update();

        escapeHandled = false;
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

    public final static String PARAMNAME_NUMBERFACTORY = "numberFactory";

    public final static String PARAMNAME_COLOR_ADD = "color offset";
    public final static String PARAMNAME_COLOR_MULT = "color period";
    public final static String PARAMNAME_COLOR_SATURATION = "saturation";
    public final static String PARAMNAME_SOBEL_GLOW_LIMIT = "edge brightness";
    public final static String PARAMNAME_SOBEL_GLOW_FACTOR = "glow period";
    public final static String PARAMNAME_AMBIENT_LIGHT = "ambient light";

    public final static String PARAMNAME_FALLBACK_COLOR_ADD = "color offset 2";
    public final static String PARAMNAME_FALLBACK_COLOR_MULT = "color period 2";
    public final static String PARAMNAME_FALLBACK_COLOR_SATURATION = "saturation 2";
    public final static String PARAMNAME_FALLBACK_SOBEL_GLOW_LIMIT = "edge brightness 2";
    public final static String PARAMNAME_FALLBACK_SOBEL_GLOW_FACTOR = "glow sensitivity 2";
    public final static String PARAMNAME_FALLBACK_AMBIENT_LIGHT = "ambient light 2";

    public final static String PARAMNAME_PALETTE = "palette (condition)";
    public final static String PARAMNAME_PALETTE2 = "palette (fallback)";
    public final static String PARAMNAME_EXTRACT_CHANNEL = "monochrome source";
    public final static String PARAMNAME_MAPPING_COLOR = "monochrome color";

    public final static String PARAMNAME_DRAW_AXIS = "draw axis";
    public final static String PARAMNAME_DRAW_ORBIT = "draw orbit";
    public final static String PARAMNAME_DRAW_PATH = "draw current path";
    public final static String PARAMNAME_DRAW_MIDPOINT = "draw midpoint";
    public final static String PARAMNAME_DRAW_ZERO = "draw (0+0*i)";

    public final static String PARAMNAME_ORBIT_TRACES = "orbit traces";
    public final static String PARAMNAME_ORBIT_TRACE_PER_INSTRUCTION = "trace instructions";
    public final static String PARAMNAME_ORBIT_TARGET = "orbit target";

    public final static String PARAMNAME_TRACES_VALUE = "trace position";
    public final static String PARAMNAME_TRACES_LINE_WIDTH = "line width";
    public final static String PARAMNAME_TRACES_POINT_SIZE = "point size";
    public final static String PARAMNAME_TRACES_LINE_TRANSPARENCY = "line transparency";
    public final static String PARAMNAME_TRACES_POINT_TRANSPARENCY = "point transparency";
    public final static String PARAMNAME_TRACES_START_COLOR = "";
    public final static String PARAMNAME_TRACES_END_COLOR = "";

    public final static String PARAMS_NUMBERFACTORY = "0oizcO";

    public final static String PARAMS_COLOR_ADD = "UgHgR5";
    public final static String PARAMS_COLOR_MULT = "OFTMFw";
    public final static String PARAMS_COLOR_SATURATION = "EgDJY4";
    //    public final static String PARAMS_SOBEL_FACTOR = "glow sensitivity";
    public final static String PARAMS_SOBEL_GLOW_LIMIT = "bW0VpF";
    public final static String PARAMS_SOBEL_GLOW_FACTOR = "iOOW84";
    public final static String PARAMS_AMBIENT_LIGHT = "0IH3rK";

    public final static String PARAMS_FALLBACK_COLOR_ADD = "12f1jD";
    public final static String PARAMS_FALLBACK_COLOR_MULT = "56al2K";
    public final static String PARAMS_FALLBACK_COLOR_SATURATION = "pDD1eC";
    //    public final static String PARAMS_FALLBACK_SOBEL_FACTOR = "glow sensitivity";
    public final static String PARAMS_FALLBACK_SOBEL_GLOW_LIMIT = "VujYT7";
    public final static String PARAMS_FALLBACK_SOBEL_GLOW_FACTOR = "2NgNhI";
    public final static String PARAMS_FALLBACK_AMBIENT_LIGHT = "PWfG3I";

    public final static String PARAMS_PALETTE = "pLrKY-";
    public final static String PARAMS_PALETTE2 = "3YtNML";
    public static final String PARAMVALUE_PALETTE_DISABLED = "hue";
    public final static String PARAMS_EXTRACT_CHANNEL = "xbMt0u";
    public final static String PARAMS_MAPPING_COLOR = "gS2lYj";

    public final static String PARAMS_DRAW_AXIS = "CGrDXZ";
    public final static String PARAMS_DRAW_ORBIT = "m9TYl2";
    public final static String PARAMS_DRAW_PATH = "ENQY4_";
    public final static String PARAMS_DRAW_MIDPOINT = "LHvVgb";
    public final static String PARAMS_DRAW_ZERO = "Vwr62J";

    public final static String PARAMS_ORBIT_TRACES = "aW_XS3";
    public final static String PARAMS_ORBIT_TRACE_PER_INSTRUCTION = "cqTeeI";
    public final static String PARAMS_ORBIT_TARGET = "mzwBNO";

    public final static String PARAMS_TRACES_VALUE = "mCZoSI";
    public final static String PARAMS_TRACES_LINE_WIDTH = "Rr79wb";
    public final static String PARAMS_TRACES_POINT_SIZE = "ZEMbVL";
    public final static String PARAMS_TRACES_LINE_TRANSPARENCY = "jcENk2";
    public final static String PARAMS_TRACES_POINT_TRANSPARENCY = "2fXEPs";
    public final static String PARAMS_TRACES_START_COLOR = "";
    public final static String PARAMS_TRACES_END_COLOR = "";

    public final static NumberFactory nf = new NumberFactory(DoubleNumber.class, DoubleComplexNumber.class);

    protected void initRightParamContainer() {
        
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
        ParamValueType numberFactoryType = new ParamValueType("numberfactory");
        config.addValueType(numberFactoryType);


        List<ParamSupplier> params = new ArrayList<>();

        params.add(new StaticParamSupplier(PARAMS_NUMBERFACTORY, nf));
        params.add(new StaticParamSupplier(PARAMS_COLOR_MULT, nf.createNumber(2.5)));
        params.add(new StaticParamSupplier(PARAMS_COLOR_ADD, nf.createNumber(0.0)));
        params.add(new StaticParamSupplier(PARAMS_COLOR_SATURATION, nf.createNumber(0.5)));
        params.add(new StaticParamSupplier(PARAMS_AMBIENT_LIGHT, nf.createNumber(0.2)));
        params.add(new StaticParamSupplier(PARAMS_SOBEL_GLOW_LIMIT, nf.createNumber(0.8)));
        params.add(new StaticParamSupplier(PARAMS_SOBEL_GLOW_FACTOR, nf.createNumber(4.0)));

        params.add(new StaticParamSupplier(PARAMS_FALLBACK_COLOR_MULT, nf.createNumber(1.0)));
        params.add(new StaticParamSupplier(PARAMS_FALLBACK_COLOR_ADD, nf.createNumber(0.0)));
        params.add(new StaticParamSupplier(PARAMS_FALLBACK_COLOR_SATURATION, nf.createNumber(0.5)));
        params.add(new StaticParamSupplier(PARAMS_FALLBACK_AMBIENT_LIGHT, nf.createNumber(0.0)));
        params.add(new StaticParamSupplier(PARAMS_FALLBACK_SOBEL_GLOW_LIMIT, nf.createNumber(1.0)));
        params.add(new StaticParamSupplier(PARAMS_FALLBACK_SOBEL_GLOW_FACTOR, nf.createNumber(1.0)));

        params.add(new StaticParamSupplier(PARAMS_PALETTE, PARAMVALUE_PALETTE_DISABLED));
        params.add(new StaticParamSupplier(PARAMS_PALETTE2, PARAMVALUE_PALETTE_DISABLED));
        params.add(new StaticParamSupplier(PARAMS_EXTRACT_CHANNEL, 0));
        params.add(new StaticParamSupplier(PARAMS_MAPPING_COLOR, Color.WHITE));

        params.add(new StaticParamSupplier(PARAMS_DRAW_PATH, true));
        params.add(new StaticParamSupplier(PARAMS_DRAW_AXIS, false));
        params.add(new StaticParamSupplier(PARAMS_DRAW_MIDPOINT, false));
        params.add(new StaticParamSupplier(PARAMS_DRAW_ZERO, false));

        params.add(new StaticParamSupplier(PARAMS_DRAW_ORBIT, false));
        params.add(new StaticParamSupplier(PARAMS_ORBIT_TRACES, 1000));
        params.add(new StaticParamSupplier(PARAMS_ORBIT_TRACE_PER_INSTRUCTION, true));
        params.add(new StaticParamSupplier(PARAMS_ORBIT_TARGET, "mouse"));
        params.add(new StaticParamSupplier(PARAMS_TRACES_VALUE, nf.createComplexNumber(0,0)));

        params.add(new StaticParamSupplier(PARAMS_TRACES_LINE_WIDTH, nf.createNumber(1.0)));
        params.add(new StaticParamSupplier(PARAMS_TRACES_POINT_SIZE, nf.createNumber(3.0)));
        params.add(new StaticParamSupplier(PARAMS_TRACES_LINE_TRANSPARENCY, nf.createNumber(0.5)));
        params.add(new StaticParamSupplier(PARAMS_TRACES_POINT_TRANSPARENCY, nf.createNumber(0.75)));
        
        Map<String, ParamSupplier> paramMap = new HashMap<>();
        params.stream().map(e -> paramMap.put(e.getUID(), e));
        
        config.addParameterDefinition(new ParamDefinition(PARAMS_NUMBERFACTORY, PARAMNAME_NUMBERFACTORY, "Calculator", StaticParamSupplier.class, numberFactoryType),
                paramMap.get(PARAMS_NUMBERFACTORY));

        String cat_coloring_reached = "Coloring (condition reached)";
        config.addParameterDefinition(new ParamDefinition(PARAMS_COLOR_MULT, PARAMNAME_COLOR_MULT, cat_coloring_reached, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0.02 max=10"), paramMap.get(PARAMS_COLOR_MULT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_COLOR_ADD, PARAMNAME_COLOR_ADD, cat_coloring_reached, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0 max=1"), paramMap.get(PARAMS_COLOR_ADD));
        config.addParameterDefinition(new ParamDefinition(PARAMS_COLOR_SATURATION, PARAMNAME_COLOR_SATURATION, cat_coloring_reached, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0 max=1"), paramMap.get(PARAMS_COLOR_SATURATION));
//        config.addParameterDefinition(new ParameterDefinition(PARAMS_SOBEL_FACTOR, "coloring (reached)", StaticParamSupplier.class, doubleType));
        config.addParameterDefinition(new ParamDefinition(PARAMS_AMBIENT_LIGHT, PARAMNAME_AMBIENT_LIGHT, cat_coloring_reached, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=-1 max=1"), paramMap.get(PARAMS_AMBIENT_LIGHT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_SOBEL_GLOW_LIMIT, PARAMNAME_SOBEL_GLOW_LIMIT, cat_coloring_reached, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=-1 max=5"), paramMap.get(PARAMS_SOBEL_GLOW_LIMIT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_SOBEL_GLOW_FACTOR, PARAMNAME_SOBEL_GLOW_FACTOR, cat_coloring_reached, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0 max=10"), paramMap.get(PARAMS_SOBEL_GLOW_FACTOR));

        String cat_coloring_fallback = "Coloring (fallback)";
        config.addParameterDefinition(new ParamDefinition(PARAMS_FALLBACK_COLOR_MULT, PARAMNAME_FALLBACK_COLOR_MULT, cat_coloring_fallback, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0.01 max=2"), paramMap.get(PARAMS_COLOR_MULT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_FALLBACK_COLOR_ADD, PARAMNAME_FALLBACK_COLOR_ADD, cat_coloring_fallback, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0 max=1"), paramMap.get(PARAMS_COLOR_ADD));
        config.addParameterDefinition(new ParamDefinition(PARAMS_FALLBACK_COLOR_SATURATION, PARAMNAME_FALLBACK_COLOR_SATURATION, cat_coloring_fallback, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0 max=1"), paramMap.get(PARAMS_COLOR_SATURATION));
//        config.addParameterDefinition(new ParameterDefinition(PARAMS_SOBEL_FACTOR, "coloring", StaticParamSupplier.class, doubleType));
        config.addParameterDefinition(new ParamDefinition(PARAMS_FALLBACK_AMBIENT_LIGHT, PARAMNAME_FALLBACK_AMBIENT_LIGHT, cat_coloring_fallback, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=-1 max=1"), paramMap.get(PARAMS_AMBIENT_LIGHT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_FALLBACK_SOBEL_GLOW_LIMIT, PARAMNAME_FALLBACK_SOBEL_GLOW_LIMIT, cat_coloring_fallback, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=-10 max=10"), paramMap.get(PARAMS_SOBEL_GLOW_LIMIT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_FALLBACK_SOBEL_GLOW_FACTOR, PARAMNAME_FALLBACK_SOBEL_GLOW_FACTOR, cat_coloring_fallback, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0 max=2"), paramMap.get(PARAMS_SOBEL_GLOW_FACTOR));

        String cat_coloring_palettes = "Palettes";
        config.addParameterDefinition(new ParamDefinition(PARAMS_PALETTE, PARAMNAME_PALETTE, cat_coloring_palettes, StaticParamSupplier.class, selectionType),
                paramMap.get(PARAMS_PALETTE));
        config.addParameterDefinition(new ParamDefinition(PARAMS_PALETTE2, PARAMNAME_PALETTE2, cat_coloring_palettes, StaticParamSupplier.class, selectionType),
                paramMap.get(PARAMS_PALETTE2));

        Selection<String> paletteSelection = new Selection<>(PARAMS_PALETTE);
        Selection<String> paletteSelection2 = new Selection<>(PARAMS_PALETTE2);
        paletteSelection.addOption(PARAMVALUE_PALETTE_DISABLED, PARAMVALUE_PALETTE_DISABLED, "No predefined palette");
        paletteSelection2.addOption(PARAMVALUE_PALETTE_DISABLED, PARAMVALUE_PALETTE_DISABLED, "No predefined palette");
        for (String paletteName : palettes.keySet()) {
            paletteSelection.addOption(paletteName, paletteName, "Palette '" + paletteName + "'");
            paletteSelection2.addOption(paletteName, paletteName, "Palette '" + paletteName + "'");
        }
        config.addSelection(paletteSelection);
        config.addSelection(paletteSelection2);

        config.addParameterDefinition(new ParamDefinition(PARAMS_EXTRACT_CHANNEL, PARAMNAME_EXTRACT_CHANNEL, cat_coloring_palettes, StaticParamSupplier.class, selectionType),
                paramMap.get(PARAMS_EXTRACT_CHANNEL));
        Selection<Integer> extractChannelSelection = new Selection<Integer>(PARAMS_EXTRACT_CHANNEL);
        extractChannelSelection.addOption("disabled", 0, "No channel remapping");
        extractChannelSelection.addOption("red", 1, "Remap red channel to all (greyscale)");
        extractChannelSelection.addOption("green", 2, "Remap green channel to all (greyscale)");
        extractChannelSelection.addOption("blue", 3, "Remap blue channel to all (greyscale)");
        config.addSelection(extractChannelSelection);
        config.addParameterDefinition(new ParamDefinition(PARAMS_MAPPING_COLOR, PARAMNAME_MAPPING_COLOR, cat_coloring_palettes, StaticParamSupplier.class, colorType),
                paramMap.get(PARAMS_MAPPING_COLOR));

        String cat_shape_drawing = "Shape drawing";
        config.addParameterDefinition(new ParamDefinition(PARAMS_DRAW_AXIS, PARAMNAME_DRAW_AXIS, cat_shape_drawing, StaticParamSupplier.class, booleanType),
                paramMap.get(PARAMS_DRAW_AXIS));

        config.addParameterDefinition(new ParamDefinition(PARAMS_DRAW_ORBIT, PARAMNAME_DRAW_ORBIT, cat_shape_drawing, StaticParamSupplier.class, booleanType)
                , paramMap.get(PARAMS_DRAW_ORBIT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_DRAW_PATH, PARAMNAME_DRAW_PATH, cat_shape_drawing, StaticParamSupplier.class, booleanType),
                paramMap.get(PARAMS_DRAW_PATH));
        config.addParameterDefinition(new ParamDefinition(PARAMS_DRAW_MIDPOINT, PARAMNAME_DRAW_MIDPOINT, cat_shape_drawing, StaticParamSupplier.class, booleanType),
                paramMap.get(PARAMS_DRAW_MIDPOINT));
        config.addParameterDefinition(new ParamDefinition(PARAMS_DRAW_ZERO, PARAMNAME_DRAW_ZERO, cat_shape_drawing, StaticParamSupplier.class, booleanType),
                paramMap.get(PARAMS_DRAW_ZERO));
        config.addParameterDefinition(new ParamDefinition(PARAMS_ORBIT_TARGET, PARAMNAME_ORBIT_TARGET, cat_shape_drawing, StaticParamSupplier.class, selectionType)
                , paramMap.get(PARAMS_ORBIT_TARGET));
        Selection<String> traceTargetSelection = new Selection<String>(PARAMS_ORBIT_TARGET);
        traceTargetSelection.addOption("mouse", "mouse", "The trace target is set to the current mouse position");
        traceTargetSelection.addOption("path", "path", "The trace target is set to the animation named 'path'");
        config.addSelection(traceTargetSelection);
        config.addParameterDefinition(new ParamDefinition(PARAMS_ORBIT_TRACES, PARAMNAME_ORBIT_TRACES, cat_shape_drawing, StaticParamSupplier.class, integerType)
                , paramMap.get(PARAMS_ORBIT_TRACES));
        config.addParameterDefinition(new ParamDefinition(PARAMS_ORBIT_TRACE_PER_INSTRUCTION, PARAMNAME_ORBIT_TRACE_PER_INSTRUCTION, cat_shape_drawing, StaticParamSupplier.class, booleanType)
                , paramMap.get(PARAMS_ORBIT_TRACE_PER_INSTRUCTION));
        config.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_VALUE, PARAMNAME_TRACES_VALUE, cat_shape_drawing, StaticParamSupplier.class, complexNumberType)
                        .withVisible(false), paramMap.get(PARAMS_TRACES_VALUE));

        String cat_shape_settings = "Shape settings";
        config.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_LINE_WIDTH, PARAMNAME_TRACES_LINE_WIDTH, cat_shape_settings, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0 max=3"), paramMap.get(PARAMS_TRACES_LINE_WIDTH));
        config.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_POINT_SIZE, PARAMNAME_TRACES_POINT_SIZE, cat_shape_settings, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0 max=10"), paramMap.get(PARAMS_TRACES_POINT_SIZE));
        config.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_LINE_TRANSPARENCY, PARAMNAME_TRACES_LINE_TRANSPARENCY, cat_shape_settings, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0 max=1"), paramMap.get(PARAMS_TRACES_LINE_TRANSPARENCY));
        config.addParameterDefinition(new ParamDefinition(PARAMS_TRACES_POINT_TRANSPARENCY, PARAMNAME_TRACES_POINT_TRANSPARENCY, cat_shape_settings, StaticParamSupplier.class, numberType)
                .withHints("ui-element[default]:slider min=0 max=1"), paramMap.get(PARAMS_TRACES_POINT_TRANSPARENCY));

        //no client param resets the renderer
        for (ParamDefinition paramDef : config.getParameters())
            paramDef.setResetRendererOnChange(false);

        this.clientParamConfiguration = config;
        this.clientParams = new ParamContainer(config);
        params.forEach(supp -> clientParams.addParam(supp));
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

        paramUI.serverParamsSideMenu.resized();
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return true;
    }

    public ParamSupplier getClientParam(String uid){
        return clientParams.getParam(uid);
    }

    VisLabel samplesLeftLabel = null;
    VisLabel fpsLabel = null;
    double fpsUpdateInterval = 0.1;
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

        if (fpsLabel == null){
            samplesLeftLabel = new VisLabel("");
            fpsLabel = new VisLabel("FPS: ");

            stateBar.add(samplesLeftLabel).left().row();
            stateBar.add(fpsLabel).left();
        }

        int samplesLeft = getFocusedRenderer() instanceof ShaderRenderer ? ((ShaderRenderer)getFocusedRenderer()).getSamplesLeft() : -1;
        if (samplesLeft > 0){
            samplesLeftLabel.setText(""+samplesLeft);
        } else {
            samplesLeftLabel.setText("");
        }

        if ((System.nanoTime()-lastStatebarUpdate)*NumberUtil.NS_TO_S < fpsUpdateInterval)
            return;

        lastStatebarUpdate = System.nanoTime();

        double frametime = Gdx.graphics.getDeltaTime();
        double fps = 1.0/frametime;
        String fpsText = "FPS: " + ((int) (fps * 10.0)) / 10.0 + " (" + formatFrametime(frametime) + " ms, max: " + formatFrametime(longestFrametime) + " ms)";
        fpsLabel.setText(fpsText);
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
        Window window = new VisWindow("Presets");
        ((VisWindow) window).addCloseButton();

        VisTable repoTab = new VisTable();


        VisSelectBox repoSel = new VisSelectBox();
        repoSel.setItems("default (local)");
        VisTextButton repoOptionBtn = new VisTextButton("...");
        repoTab.add("Repository:");
        repoTab.add(repoSel).expandX().fillX().padLeft(2).padRight(2).padTop(2);
        repoTab.add(repoOptionBtn).row();

        window.add(repoTab).colspan(3).fillX().row();

        VisTable presetSelectionTable = new VisTable();

        VisSelectBox selection = new VisSelectBox();
        updateParamSelectBox(selection);
        VisTextButton selectPrev = new VisTextButton("<");
        VisTextButton selectNext = new VisTextButton(">");
        presetSelectionTable.add("Preset:");
        presetSelectionTable.add(selectPrev);
        presetSelectionTable.add(selection).expandX().fillX().pad(2);
        presetSelectionTable.add(selectNext);

        window.add(presetSelectionTable).colspan(3).fillX().row();

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
                    newContainer.addParam(new StaticParamSupplier("view", newViewId));
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
                container.getParamMap().remove("view");
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

    public ParamContainer getClientParams() {
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
            Gdx.input.setOnscreenKeyboardVisible(false);
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

    public List<Keybinding> getKeybindings() {
        return keybindings;
    }
}
