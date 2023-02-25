package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
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
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.params.ClientParamsEscapeTime;
import de.felixp.fractalsgdx.params.DrawParamsTurtleGraphics;
import de.felixp.fractalsgdx.rendering.AbstractFractalRenderer;
import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.rendering.ReactionDiffusionSystemContext;
import de.felixp.fractalsgdx.rendering.ShaderSystemContext;
import de.felixp.fractalsgdx.rendering.RemoteRenderer;
import de.felixp.fractalsgdx.remoteclient.ClientSystem;
import de.felixp.fractalsgdx.remoteclient.SystemInterfaceGdx;
import de.felixp.fractalsgdx.rendering.ShaderRenderer;
import de.felixp.fractalsgdx.rendering.TurtleGraphicsSystemContext;
import de.felixp.fractalsgdx.rendering.palette.IPalette;
import de.felixp.fractalsgdx.rendering.palette.ImagePalette;
import de.felixp.fractalsgdx.ui.actors.FractalsWindow;
import de.felixp.fractalsgdx.ui.entries.AbstractPropertyEntry;
import de.felixp.fractalsgdx.util.FractalsIOUtil;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.common.CommonFractalParameters;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.util.NumberUtil;
import de.felixperko.fractals.util.io.IIOMetadataUpdater;

public class MainStage extends Stage {

    final static String POSITIONS_PREFS_NAME = "de.felixp.fractalsgdx.ui.MainStage.positions";

    static Map<String, IPalette> palettes = new LinkedHashMap<>();

    ParamUI paramUI;

//    FractalRenderer renderer;
    FractalRenderer focusedRenderer = null;

    RendererConfig activeRendererConfig;
    String activeRendererConfigParamDefUid;
    Map<String, RendererConfig> rendererConfigs = new HashMap<>();
    List<FractalRenderer> renderers = new ArrayList<>();

    Group rendererGroup;

    Table stateBar;

    Preferences positions_prefs;
    Map<String, ParamContainer> locations = new HashMap<>();

    Table activeSettingsTable = null;


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

        positions_prefs = Gdx.app.getPreferences(POSITIONS_PREFS_NAME);
        Map<String, ?> map = positions_prefs.get();
        for (Map.Entry<String, ?> e : map.entrySet()) {
            if (!(e.getValue() instanceof String))
                throw new IllegalStateException("Preferences "+POSITIONS_PREFS_NAME+" contain non-String objects.");
            String name = e.getKey();

//            ParamContainer paramContainer = FractalsIOUtil.deserializeParamContainer(((String) e.getValue()).getBytes());
//            try {
//                paramContainer = ParamContainer.deserializeObjectBase64((String)e.getValue());
//            } catch (IOException | ClassNotFoundException e1) {
//                e1.printStackTrace();
//            }

//            locations.put(name, paramContainer);
        }

        VisTable ui = new VisTable();
        ui.align(Align.topLeft);
        ui.setFillParent(true);

        //initRenderer ParamUI
        paramUI = new ParamUI(this);

        ParamConfiguration clientParameterConfiguration = initRightParamConfiguration();
        ParamContainer drawParamContainer = initRightParamContainer();

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

        rendererGroup = new Group();

        rendererConfigs.put(ShaderSystemContext.UID_PARAMCONFIG, new EscapeTimeRendererConfig());
        rendererConfigs.put(TurtleGraphicsSystemContext.UID_PARAMCONFIG, new TurtleGraphicsRendererConfig());
        rendererConfigs.put(ReactionDiffusionSystemContext.UID_PARAMCONFIG, new ReactionDiffusionRendererConfig());

        activeRendererConfig = rendererConfigs.get(ShaderSystemContext.UID_PARAMCONFIG);
        activeRendererConfigParamDefUid = ShaderSystemContext.UID_PARAMCONFIG;
//        activeRendererConfig = rendererConfigs.get(TurtleGraphicsSystemContext.UID_PARAMCONFIG);
//        activeRendererConfigParamDefUid = TurtleGraphicsSystemContext.UID_PARAMCONFIG;

        activeRendererConfig.createRenderers();
        activeRendererConfig.addRenderers(this);
        FractalRenderer firstRenderer = activeRendererConfig.getRenderers().get(0);
        setFocusedRenderer(firstRenderer);

        paramUI.init(clientParameterConfiguration, drawParamContainer);
        paramUI.addToUiTable(ui);

        stateBar = new Table();
        stateBar.align(Align.left);

        ui.add(stateBar).align(Align.bottomLeft).colspan(5);

        addActor(rendererGroup);

        addActor(ui);

        activeRendererConfig.initRenderers();

        //reset ParamUI focus to first renderer
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                paramUI.setServerParameterConfiguration(firstRenderer);
            }
        });

    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        FractalRenderer renderer = getFocusedRenderer();
        if (renderer != null && getKeyboardFocus() == renderer) {
            boolean handled = renderer.scrolled(amountX, amountY);
            if (handled)
                return true;
        }
        return super.scrolled(amountX, amountY);
    }

    public boolean switchRendererConfigs(String computeParamDefUid){

        if (computeParamDefUid.equals(activeRendererConfigParamDefUid))
            return false;

        activeRendererConfig.removeRenderers(this);

        activeRendererConfig = rendererConfigs.get(computeParamDefUid);
        activeRendererConfigParamDefUid = computeParamDefUid;

        activeRendererConfig.createRenderers();
        activeRendererConfig.addRenderers(this);
        FractalRenderer firstRenderer = activeRendererConfig.getRenderers().get(0);
        setFocusedRenderer(firstRenderer);
        activeRendererConfig.initRenderers();

//        paramUI.setServerParameterConfiguration(firstRenderer, null, firstRenderer.getSystemContext().getParamConfiguration());

        ParamContainer drawParamContainer = activeRendererConfig.getDrawParamContainer();
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                paramUI.setServerParameterConfiguration(firstRenderer);
                paramUI.setClientParameterConfiguration(firstRenderer, drawParamContainer, drawParamContainer.getParamConfiguration());
            }
        });
        return true;
    }

    public void pressedSwitchRenderers(){
        activeRendererConfig.switchRenderers();
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
        if (paletteName.equals(getClientParam(ClientParamsEscapeTime.PARAMS_PALETTE).getGeneral(String.class))){
            for (FractalRenderer renderer : renderers){
                renderer.setRefresh();
            }
        }
    }

    public void refreshClientSideMenu() {
        ParamConfiguration clientParamConfiguration = initRightParamConfiguration();
        getClientParams().setParamConfiguration(clientParamConfiguration);
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
        String paletteName = getClientParams().getParam(postprocessParameter).getGeneral(String.class);
        Texture fallbackTexture = fallbackIndex == 0 ? blackTexture : blackTexture2;
        if (paletteName.equalsIgnoreCase(ClientParamsEscapeTime.OPTIONVALUE_PALETTE_DISABLED))
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
        if (getRenderers().isEmpty())
            return;
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
        else if (renderer instanceof AbstractFractalRenderer){
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
        keybindings.add(new PostprocessToggleKeybinding("toggle orbit traces", ClientParamsEscapeTime.PARAMS_DRAW_ORBIT, Input.Keys.O));
        keybindings.add(new PostprocessToggleKeybinding("toggle trace instructions", ClientParamsEscapeTime.PARAMS_ORBIT_TRACE_PER_INSTRUCTION, Input.Keys.I));
        keybindings.add(new PostprocessToggleKeybinding("toggle overlay midpoint", ClientParamsEscapeTime.PARAMS_DRAW_MIDPOINT, Input.Keys.M));
        keybindings.add(new PostprocessToggleKeybinding("toggle overlay axis", ClientParamsEscapeTime.PARAMS_DRAW_AXIS, Input.Keys.C));;
        keybindings.add(new PostprocessToggleKeybinding("toggle overlay zero", ClientParamsEscapeTime.PARAMS_DRAW_ZERO, Input.Keys.Z));

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

    protected ParamContainer initRightParamContainer() {
        return ClientParamsEscapeTime.getParamContainer(palettes);
    }

    protected ParamConfiguration initRightParamConfiguration() {
        ParamContainer paramContainer = ClientParamsEscapeTime.getParamContainer(palettes);
        return paramContainer.getParamConfiguration();
    }

    protected FractalRenderer getRenderer(int position){
        return position < 0 ? null : position >= renderers.size() ? null : renderers.get(position);
    }

    public void addFractalRenderer(FractalRenderer renderer){
        if (!(renderer instanceof Actor))
            throw new IllegalArgumentException("Renderer has to extend "+Actor.class);
        this.renderers.add(renderer);
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                rendererGroup.addActor((Actor)renderer);
            }
        });
        renderer.updateSize();
    }

    public void removeFractalRenderer(FractalRenderer renderer){
        renderer.removed();
        renderer.disposeRenderer();
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
        return getClientParams().getParam(uid);
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
        VisTextButton jumptoBtn = new VisTextButton("jump to preset");
        VisTextButton importFileBtn = new VisTextButton("read file...");

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
//        try {
            ParamContainer paramContainer = paramUI.serverParamsSideMenu.getParamContainer();
            if (paramContainer != null) {
                ParamContainer newContainer = new ParamContainer(paramContainer, true);
                paramUI.serverParamsSideMenu.propertyEntryList.forEach(pe -> pe.applyClientValue(newContainer));
//                paramText = newContainer.serializeJson(true).replaceAll("\r\n", "\n");
                try {
                    paramText = FractalsIOUtil.serializeParamContainers(newContainer, focusedRenderer.getSystemContext().getParamConfiguration(),
                            getClientParams(), getClientParamConfiguration());
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
//        }
//        catch (IOException e){
//            e.printStackTrace();
//        }

        ScrollableTextArea parameterTextArea = new ScrollableTextArea(paramText){
            @Override
            public float getPrefWidth() {
                return Gdx.graphics.getWidth()/2;
            }
        };
        ScrollPane scrollPane = parameterTextArea.createCompatibleScrollPane();

        importFileBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String defaultPath = System.getProperty("user.home");
                if (fileChooserDirPath == null)
                    fileChooserDirPath = defaultPath;

                FileChooser fileChooser = initImportFileChooser(fileChooserDirPath);

                fileChooser.setListener(new FileChooserAdapter() {
                    @Override
                    public void selected (Array<FileHandle> files) {
                        fileChooserFilePath = files.first().file().getAbsolutePath();
                        FileHandle file = Gdx.files.external(fileChooserFilePath);
                        try {
                            System.out.println("reading metadata for file: "+file.path());
                            String text = IIOMetadataUpdater.readMetadata(new File(file.path()), ScreenshotUI.METADATA_KEY);
                            if (text == null || text.isEmpty()) //JPG Tags work differently, key is "comment" for some reason
                                text = IIOMetadataUpdater.readMetadata(new File(file.path()), "comment");
                            parameterTextArea.setText(text);
//                            importWindow.remove();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

//                        fileTextField.focusLost();
                FractalsGdxMain.mainStage.addActor(fileChooser.fadeIn());
                fileChooser.setSize(Gdx.graphics.getWidth()*0.7f, Gdx.graphics.getHeight()*0.7f);
                fileChooser.centerWindow();



//                openImportFileWindow(parameterTextArea);
            }
        });

        VisTextButton applyBtn = new VisTextButton("Apply", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
//                try {
//                    ParamContainer paramContainer = ParamContainer.deserializeJson(parameterTextArea.getText());
                    ParamContainer[] newParamContainers = FractalsIOUtil.deserializeParamContainers(
                            parameterTextArea.getText().getBytes(StandardCharsets.UTF_8), focusedRenderer.getSystemContext().getParamConfiguration(), paramUI.serverParamsSideMenu.getParamContainer(),
                            getClientParamConfiguration(), getClientParams());

                    if (newParamContainers == null){
                        System.err.println("MainStage: created ParamContainer was null!");
                    }
                    else {
                        submitServer(focusedRenderer, newParamContainers[0]);
                        paramUI.setClientParameterConfiguration(focusedRenderer, newParamContainers[1], getClientParamConfiguration());
                    }
                    focusedRenderer.reset();
//                }
//                catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        });

        VisTable topButtonTable = new VisTable(true);
        topButtonTable.add(saveBtn);
        topButtonTable.add(jumptoBtn);
        window.add(topButtonTable).pad(2).row();

        window.add(scrollPane).width(Gdx.graphics.getWidth()*.5f).height(Gdx.graphics.getHeight()*.5f).row();

        VisTable buttonTable = new VisTable(true);
        buttonTable.add(cancelBtn);
        buttonTable.add(importFileBtn);
        buttonTable.add(applyBtn);
        window.add(buttonTable).pad(2);

        addActor(window);
        window.pack();
        ((VisWindow) window).centerWindow();

    }

    static FileChooser fileChooser;
    static String fileChooserFilePath = null;
    static String fileChooserDirPath = null;


    private FileChooser initImportFileChooser(String path){
        FileChooser.setDefaultPrefsName("de.felixp.fractalsgdx.ui.filechooser");
        if (fileChooser != null)
            fileChooser.remove();
        else
            fileChooser = new FileChooser("Import file", FileChooser.Mode.OPEN);
//        fileChooser.setDirectory(path);
        Class<FileChooser> fileChooserClass = FileChooser.class;
        for (Field field : fileChooserClass.getDeclaredFields()){
            if (field.getName().equals("confirmButton")){
                field.setAccessible(true);
                try {
                    VisTextButton confirmButton = (VisTextButton) field.get(fileChooser);
                    confirmButton.setText("Import file");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                field.setAccessible(false);
                break;
            }
        }
        fileChooser.setSelectionMode(FileChooser.SelectionMode.FILES);
        return fileChooser;
    }

    @Deprecated
    private void openImportFileWindow(ScrollableTextArea parameterTextArea){

        MainStage stage = FractalsGdxMain.mainStage;
        FractalsWindow importWindow = new FractalsWindow("Import file");

        VisTextField fileTextField = new VisTextField("...");

        initImportFileChooser(fileChooserFilePath);

        fileChooser.setListener(new FileChooserAdapter() {
            @Override
            public void selected (Array<FileHandle> files) {
                fileChooserFilePath = files.first().file().getAbsolutePath();
                fileChooserDirPath = files.first().file().getParentFile().getAbsolutePath();
                fileTextField.setText(fileChooserFilePath);
            }
        });

        fileTextField.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                fileTextField.focusLost();
                stage.addActor(fileChooser.fadeIn());
                fileChooser.setSize(Gdx.graphics.getWidth()*0.7f, Gdx.graphics.getHeight()*0.7f);
                fileChooser.centerWindow();
            }
        });

        VisTextButton importButton = new VisTextButton("import");
        importButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                FileHandle file = Gdx.files.external(fileTextField.getText());
                try {
                    System.out.println("reading metadata for file: "+file.path());
                    String text = IIOMetadataUpdater.readMetadata(new File(file.path()), ScreenshotUI.METADATA_KEY);
                    parameterTextArea.setText(text);
                    importWindow.remove();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        importWindow.add(fileTextField).row();
        importWindow.add(importButton);

//        VisTextField
        importWindow.pack();
        stage.addActor(importWindow);
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

//                try {
//                    positions_prefs.putString(nameFld.getText(), container.serializeObjectBase64());
                    positions_prefs.putString(nameFld.getText(),
                            FractalsIOUtil.serializeParamContainers(container, focusedRenderer.getSystemContext().getParamConfiguration(),
                                    getClientParams(), getClientParamConfiguration()));
                    positions_prefs.flush();
//                } catch (IOException e) {
//                    throw new IllegalStateException("couldn't serialize locations");
//                }
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
        return paramUI.getClientParamsSideMenu().getParamContainer();
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
        return paramUI.clientParamsSideMenu.getParamContainer().getParamConfiguration();
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
