package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Timer;
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisList;
import com.kotcrab.vis.ui.widget.VisRadioButton;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisWindow;

import net.dermetfan.utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.rendering.RendererContext;
import de.felixp.fractalsgdx.rendering.RendererProperties;
import de.felixp.fractalsgdx.rendering.ShaderRenderer;
import de.felixp.fractalsgdx.ui.actors.FractalsWindow;

import static de.felixp.fractalsgdx.FractalsGdxMain.UI_PREFS_NAME;
import static de.felixp.fractalsgdx.FractalsGdxMain.UI_PREFS_SCALE;
import static de.felixp.fractalsgdx.FractalsGdxMain.stage;

public class MainStageWindows {

    public static final String CATEGORY_RENDERERS = "Renderers";
    public static final String CATEGORY_WINDOW = "Window";
    public static final String CATEGORY_PALETTES = "Palettes";

    public static void openConnectWindow(MainStage stage, String text){
        VisWindow window = new VisWindow("Connect or start server");
        window.addCloseButton();

        if (text != null && text.length() > 0){
            VisLabel reasonLabel = new VisLabel(text);

            window.add(reasonLabel).row();
        }

        VisLabel urlLabel = new VisLabel("Adress: ");
        VisTextField urlField = new VisTextField("localhost");
        VisLabel portLabel = new VisLabel("Port: ");
        VisTextField portField = new VisTextField("3141");

        VisTable table1 = new VisTable(true);
        table1.add(urlLabel);
        table1.add(urlField);
        table1.add(portLabel);
        table1.add(portField);
        window.add(table1).row();

        VisTextButton cancelButton = new VisTextButton("Cancel", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                window.remove();
            }
        });
        VisTextButton localButton = new VisTextButton("Start local server", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                FractalsGdxMain.client.connectLocal();
                window.remove();
            }
        });
        VisTextButton remoteButton = new VisTextButton("Connect to server", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                FractalsGdxMain.client.connect(urlField.getText(), Integer.parseInt(portField.getText()));
                window.remove();
            }
        });

        VisTable table2 = new VisTable(true);
        table2.add(cancelButton);
        table2.add(localButton);
        table2.add(remoteButton);
        window.add(table2).pad(5);
        stage.addActor(window);
        window.pack();
        window.centerWindow();
    }

    static VisWindow settingsWindow = null;

    public static void toggleSettingsWindow(MainStage stage){
        if (settingsWindow == null || settingsWindow.getParent() == null)
            openSettingsMenu(stage);
        else{
            settingsWindow.remove();
            stage.resetKeyboardFocus();
        }
    }

    public static void openSettingsMenu(final MainStage stage){

        if(settingsWindow != null && settingsWindow.getParent() != null)
            settingsWindow.remove();

        settingsWindow = new FractalsWindow("Settings");
        settingsWindow.setResizable(true);
        ((VisWindow) settingsWindow).addCloseButton();
//        ((VisWindow) settingsWindow).closeOnEscape();

        Table categoryTable = new VisTable();
        Table contentTable = new VisTable();


        VisList<String> categoryList = new VisList<String>()
//        {
//            @Override
//            public float getPrefHeight() {
//                float height = Gdx.graphics.getHeight();
//                return height*0.7f;
//            }
//        }
        ;
        categoryList.setItems(CATEGORY_WINDOW, CATEGORY_RENDERERS, CATEGORY_PALETTES);
        categoryList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) { //switch to selected tab
                String selected = ((VisList<String>)categoryList).getSelected();
                switchSettingsTable(selected, contentTable, stage, settingsWindow);
            }
        });

        categoryTable.add(categoryList);

        settingsWindow.add(categoryTable).top();
        settingsWindow.add(new Separator()).expandY().fillY().pad(2);
        settingsWindow.add(contentTable).expandX().fillX().expandY().fillY();

        stage.addActor(settingsWindow);
        switchSettingsTable(CATEGORY_RENDERERS, contentTable, stage, settingsWindow);
        switchSettingsTable(CATEGORY_WINDOW, contentTable, stage, settingsWindow);
        settingsWindow.pack();
        ((VisWindow)settingsWindow).centerWindow();
    }

    private static void switchSettingsTable(String newSelected, Table contentTable, MainStage stage, VisWindow window) {
        contentTable.clear();
        if (stage.getActiveSettingsTable() != null)
            stage.getActiveSettingsTable().remove();
        switch (newSelected){
            case CATEGORY_RENDERERS:
                stage.setActiveSettingsTable(getRenderersTable(settingsWindow));
                break;
            case CATEGORY_WINDOW:
                stage.setActiveSettingsTable(getWindowOptions(settingsWindow));
                break;
            case CATEGORY_PALETTES:
                stage.setActiveSettingsTable(PaletteUI.getPaletteTable(settingsWindow));
                break;
        }
        VisScrollPane sp = new VisScrollPane(stage.getActiveSettingsTable());
        sp.setScrollingDisabled(true, false);
        sp.setFadeScrollBars(false);
        contentTable.add(sp).expandX().fillX();
        window.pack();
        window.centerWindow();
    }

    static VisTable renderersTable = null;

    static VisLabel memTextLabel = new VisLabel("Memory usage: ");
    static VisLabel memValueLabel = null;

    private static Table getRenderersTable(Window window){

        if (renderersTable != null) {
            renderersTable.remove();
        }

        renderersTable = new VisTable(true);


        VisLabel textLbl = new VisLabel("Configure the active renderer(s).");
        renderersTable.add(textLbl).row();

        if (memValueLabel == null) {
            memValueLabel = new VisLabel();
            Timer.schedule(getMemoryRunnable(memValueLabel), 0, 0.2f);
        }

        VisTable localResourcesTable = new VisTable();
        localResourcesTable.add(memTextLabel);
        localResourcesTable.add(memValueLabel);

        renderersTable.add(localResourcesTable).left().row();

        for (FractalRenderer renderer : ((MainStage)stage).getRenderers()) {
            addRendererToList(window, renderer);
        }

        VisTextButton addRendererButton = new VisTextButton("Add renderer", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                FractalRenderer renderer = new ShaderRenderer(new RendererContext(0.05f, 0.05f, 0.3f, 0.3f, RendererProperties.ORIENTATION_BOTTOM_LEFT));
                ((MainStage)stage).addFractalRenderer(renderer);
                renderer.initRenderer();
                addRendererToList(window, renderer);
                //button to the bottom
                actor.remove();
                renderersTable.add(actor).row();
            }
        });

        renderersTable.addSeparator();
        renderersTable.add(addRendererButton).row();

        return renderersTable;
    }

    public static void addRendererToList(Window settingsWindow, FractalRenderer renderer) {
        RendererUI rendererUI = new RendererUI(renderer);
        VisTable infoTable = rendererUI.initInfoTable(settingsWindow);
        VisTable resourcesTable = rendererUI.initResourcesTable();

        renderersTable.add(infoTable).left().row();
        if (resourcesTable != null)
            renderersTable.add(resourcesTable).row();
    }

    private static Timer.Task getMemoryRunnable(VisLabel memValueLabel){
        return new Timer.Task() {
            @Override
            public void run() {
                long freeMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                memValueLabel.setText(freeMem/1000_000+" mb");
//                System.out.println("Memory runnable: "+memValueLabel.getParent().getParent());
            }
        };
    }

    private static Table getWindowOptions(Window window){
        Table contentTable = new VisTable();

        VisRadioButton fullscreenBtn = new VisRadioButton("Fullscreen");
        VisRadioButton windowedBtn = new VisRadioButton("Windowed");
        VisRadioButton windowedBtn2 = new VisRadioButton("Windowed");

        VisSelectBox<String> fullscreenSelect = new VisSelectBox();
        fullscreenSelect.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                fullscreenBtn.setChecked(true);
            }
        });
        Map<Graphics.DisplayMode, String> names = new HashMap<>();
        Map<String, Graphics.DisplayMode> fullscreenOptions = new HashMap<>();
        Map<String, List<Graphics.DisplayMode>> modesPerResolution = new HashMap<>();
        Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
        for (Graphics.Monitor monitor : Gdx.graphics.getMonitors()) {
            for (Graphics.DisplayMode mode : Gdx.graphics.getDisplayModes(monitor)) {
                String res = monitor.name + " " + mode.width + "x" + mode.height;
                if (!modesPerResolution.containsKey(res))
                    modesPerResolution.put(res, new ArrayList<>());
                modesPerResolution.get(res).add(mode);
            }
        }
        List<Graphics.DisplayMode> items = new ArrayList<>();
        nextResolution:
        for (Map.Entry<String, List<Graphics.DisplayMode>> e: modesPerResolution.entrySet()){
            List<Graphics.DisplayMode> modes = e.getValue();
//            String name = e.getKey()+"@"+mode.refreshRate+"hz";
            for (Graphics.DisplayMode mode : modes) {
                if (mode.refreshRate != currentMode.refreshRate)
                    continue;
                String name = mode.width + "x" + mode.height + " @" + mode.refreshRate + "hz";
                names.put(mode, name);
                fullscreenOptions.put(name, mode);
                for (int i = 0; i < items.size(); i++) {
                    Graphics.DisplayMode other = items.get(i);
                    if (mode.width > other.width) {
                        items.add(i, mode);
                        continue nextResolution;
                    }
                }
                items.add(mode);
            }
        }
        String[] itemArray = new String[items.size()];
        for (int i = 0 ; i < itemArray.length ; i++)
            itemArray[i] = names.get(items.get(i));
        fullscreenSelect.setItems(itemArray);
        Graphics.DisplayMode current = Gdx.graphics.getDisplayMode();
        fullscreenSelect.setSelected(names.get(modesPerResolution.get(current.width+"x"+current.height)));

        VisSelectBox<String> windowedSelect = new VisSelectBox();
        windowedSelect.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                windowedBtn.setChecked(true);
            }
        });
        Map<String, Pair<Integer, Integer>> windowedOptions = new LinkedHashMap<>();
        int currWidth = Gdx.graphics.getWidth();
        int currHeight = Gdx.graphics.getHeight();
        windowedOptions.put(currWidth+"x"+currHeight, new Pair<>(currWidth, currHeight));
        int testWidth = currWidth*2/3;
        int testHeight = currHeight*2/3;
        if (testWidth*3/2 == currWidth && testHeight*3/2 == currHeight)
            windowedOptions.put(testWidth+"x"+testHeight, new Pair<>(testWidth, testHeight));
        int div = 2;
        while (testWidth >= 300 && testHeight >= 300){
            testWidth = currWidth/div;
            testHeight = currHeight/div;
            if (testWidth*div == currWidth && testHeight*div == currHeight){
                windowedOptions.put(testWidth+"x"+testHeight, new Pair<>(testWidth, testHeight));
            }
            div++;
        }
        String[] itemArray2 = new String[windowedOptions.size()];
        int  i = 0;
        for (String windowedOptionName : windowedOptions.keySet()){
            itemArray2[i++] = windowedOptionName;
        }
        if (itemArray2.length == 0)
            windowedBtn.setDisabled(true);

        windowedSelect.setItems(itemArray2);

        ButtonGroup group = new ButtonGroup();
        group.add(fullscreenBtn);
        group.add(windowedBtn);
        group.add(windowedBtn2);

        fullscreenBtn.setChecked(Gdx.graphics.isFullscreen());
        windowedBtn2.setChecked(!Gdx.graphics.isFullscreen());

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

        ClickListener clickListener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                windowedBtn2.setChecked(true);
            }
        };
        xResultionField.addListener(clickListener);
        yResultionField.addListener(clickListener);

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
                if (setFullscreen){
                    Gdx.graphics.setFullscreenMode(fullscreenOptions.get(fullscreenSelect.getSelected()));
                    return;
                }
                int width = -1;
                int height = -1;
                if (windowedBtn.isChecked()) {
                    Pair<Integer, Integer> dims = windowedOptions.get(windowedSelect.getSelected());
                    width = dims.getKey();
                    height = dims.getValue();
                }
                else if (windowedBtn2.isChecked()){
                    width = Integer.parseInt(xResultionField.getText());
                    height = Integer.parseInt(yResultionField.getText());
                }

                if (Gdx.graphics.isFullscreen() || width != Gdx.graphics.getWidth() ||  height != Gdx.graphics.getHeight()){
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
        fullscreenRow.add(fullscreenBtn).left().pad(2);
        fullscreenRow.add(fullscreenSelect).pad(2);
        contentTable.add(fullscreenRow).left().row();

        VisTable windowedRow = new VisTable();
        windowedRow.add(windowedBtn).left().pad(2);
        windowedRow.add(windowedSelect).pad(2);
        contentTable.add(windowedRow).left().row();

        VisTable windowedRow2 = new VisTable();
        windowedRow2.add(windowedBtn2).left().pad(2);
        windowedRow2.add(xResultionField).pad(2);
        windowedRow2.add(resultionMultLbl).pad(2);
        windowedRow2.add(yResultionField).pad(2);
        contentTable.add(windowedRow2).left().row();

        contentTable.add().row();
        contentTable.add(scaleDescLbl).left().row();
        contentTable.add(scale1Btn).left().row();
        contentTable.add(scale2Btn).left().row();

        contentTable.add().row();
        VisTable btnRow = new VisTable();
        btnRow.add(cancelBtn).pad(2).center();
        btnRow.add(applyBtn).pad(2).center();
        contentTable.add(btnRow);

        return contentTable;
    }

}
