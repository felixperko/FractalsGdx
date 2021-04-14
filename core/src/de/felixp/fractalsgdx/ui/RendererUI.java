package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

import net.dermetfan.utils.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.rendering.RemoteRenderer;
import de.felixp.fractalsgdx.rendering.RendererContext;
import de.felixp.fractalsgdx.rendering.RendererProperties;
import de.felixp.fractalsgdx.rendering.ShaderRenderer;
import de.felixp.fractalsgdx.remoteclient.ChangedResourcesListener;
import de.felixp.fractalsgdx.remoteclient.MessageInterfaceGdx;
import de.felixperko.fractals.network.messages.ResourceRequestMessage;

import static de.felixp.fractalsgdx.FractalsGdxMain.client;
import static de.felixp.fractalsgdx.rendering.RendererProperties.*;

public class RendererUI {

    static String RENDERER_REMOTE = "Remote";
    static String RENDERER_SHADER = "Shader";

    static Map<String, Class<? extends FractalRenderer>> availableRenderers =
            new LinkedHashMap<String, Class<? extends FractalRenderer>>(){
        {
            put(RENDERER_SHADER, ShaderRenderer.class);
            put(RENDERER_REMOTE, RemoteRenderer.class);
        }
    };

    VisTable infoTable;
    VisLabel rendererLabel;
    VisSelectBox<String> rendererSelection;
    VisTextButton removeButton;

    VisLabel xLabel, yLabel, widthLabel, heightLabel;
    VisTextField xField, yField, widthField, heightField;
    VisSelectBox orientationSelect;

    VisTable resourcesTable = null;
    VisSlider cpuCoresSlider = null;
    boolean cpuCoresBound = false;
    ChangedResourcesListener changedResourcesListener = null;

    FractalRenderer renderer;

    public RendererUI(FractalRenderer renderer){
        this.renderer = renderer;
    }

    public VisTable initInfoTable(Window settingsWindow){
        if (infoTable != null) {
            infoTable.remove();
        }
        infoTable = new VisTable(true);

        rendererLabel = new VisLabel("Renderer "+renderer.getId());
        rendererSelection = new VisSelectBox<>();
        removeButton = new VisTextButton("Remove", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ((MainStage) FractalsGdxMain.stage).removeFractalRenderer((FractalRenderer) renderer);
                infoTable.remove();
                if (resourcesTable != null)
                    resourcesTable.remove();
            }
        });

        VisTextButton connectToServerButton = new VisTextButton("Connect to server", new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ((MainStage) FractalsGdxMain.stage).openConnectWindow(null);
                settingsWindow.remove();
            }
        });

                Set < String > rendererNameSet = availableRenderers.keySet();
        rendererSelection.setItems(rendererNameSet.toArray(new String[rendererNameSet.size()]));

        //set current renderer selection
        for (Map.Entry<String, Class<? extends FractalRenderer>> e : availableRenderers.entrySet()) {
            if (renderer.getClass().isAssignableFrom(e.getValue())) {
                rendererSelection.setSelected(e.getKey());
            }
        }
        //renderer change listener
        rendererSelection.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                RendererContext properties = renderer.getRendererContext();

                FractalRenderer oldRenderer = renderer;
                FractalRenderer newRenderer = null;
                //create new renderer
                try {
                    Class<? extends FractalRenderer> cls = availableRenderers.get(rendererSelection.getSelected());
                    newRenderer = cls.getDeclaredConstructor(RendererContext.class).newInstance(properties);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                        | InstantiationException e){
                    e.printStackTrace();
                    return;
                }

                //set as main renderer if old renderer was
                MainStage stage = (MainStage) FractalsGdxMain.stage;
                if (stage.focusedRenderer == oldRenderer)
                    stage.focusedRenderer = newRenderer;

                //rebuild renderer draw list
                List<FractalRenderer> renderers = new ArrayList<>(stage.getRenderers());
                for (FractalRenderer renderer : renderers)
                    stage.removeFractalRenderer(renderer);
                for (FractalRenderer renderer : renderers) {
                    if (renderer == oldRenderer) {
                        stage.addFractalRenderer(newRenderer);
                        newRenderer.init();
                    } else
                        stage.addFractalRenderer(renderer);
                }

                settingsWindow.remove();
//                MainStageWindows.openSettingsMenu(stage);

                if (newRenderer instanceof RemoteRenderer)
                    ((MainStage)FractalsGdxMain.stage).openConnectWindow(null);
//                for (FractalRenderer renderer : renderers)
//                    stage.addFractalRenderer((AbstractFractalRenderer)renderer);
            }
        });



        xLabel = new VisLabel("x: ");
        yLabel = new VisLabel("y: ");
        widthLabel = new VisLabel("width: ");
        heightLabel = new VisLabel("height: ");

        int orientation = renderer.getOrientation();
        boolean invertX = orientation == ORIENTATION_BOTTOM_RIGHT || orientation == ORIENTATION_TOP_RIGHT;
        boolean invertY = orientation == ORIENTATION_TOP_LEFT || orientation == ORIENTATION_TOP_RIGHT;

        float x = renderer.getRelativeX();
        float y = renderer.getRelativeY();
        if (invertX)
            x = 1-renderer.getRelativeWidth()-x;
        if (invertY)
            y = 1-renderer.getRelativeHeight()-y;
        xField = new VisTextField(getPercentage(x));
        yField = new VisTextField(getPercentage(y));
        widthField = new VisTextField(getPercentage(renderer.getRelativeWidth()));
        heightField = new VisTextField(getPercentage(renderer.getRelativeHeight()));

        orientationSelect = new VisSelectBox();
        orientationSelect.setItems("fullscreen", "top left", "top right", "bottom left", "bottom right", "left", "right", "top", "bottom");
        orientationSelect.setSelectedIndex(orientation);

        xField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateRendererX();
            }
        });
        yField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateRendererY();
            }
        });
        widthField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateRendererWidth();
            }
        });
        heightField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateRendererHeight();
            }
        });
        orientationSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int newO = orientationSelect.getSelectedIndex();
                renderer.setOrientation(newO);
                if (newO == ORIENTATION_LEFT){
                    xField.setText("0");
                    yField.setText("0");
                    widthField.setText("50");
                    heightField.setText("100");
                    updateRendererDims();
                }
                else if (newO == ORIENTATION_RIGHT){
                    xField.setText("0");
                    yField.setText("0");
                    widthField.setText("50");
                    heightField.setText("100");
                    updateRendererDims();
                }
                else if (newO == ORIENTATION_TOP){
                    xField.setText("0");
                    yField.setText("0");
                    widthField.setText("100");
                    heightField.setText("50");
                    updateRendererDims();
                }
                else if (newO == ORIENTATION_BOTTOM){
                    xField.setText("0");
                    yField.setText("0");
                    widthField.setText("100");
                    heightField.setText("50");
                    updateRendererDims();
                }
                else if (newO == ORIENTATION_FULLSCREEN){
                    xField.setText("0");
                    yField.setText("0");
                    widthField.setText("100");
                    heightField.setText("100");
                    updateRendererDims();
                }
                renderer.updateSize();
            }
        });

        VisTable headerTable = new VisTable(true);
        headerTable.add(rendererLabel);
        headerTable.add(rendererSelection);
        headerTable.add(removeButton);
        if (renderer instanceof RemoteRenderer) {
            headerTable.row();
            headerTable.add(connectToServerButton).colspan(3).left();
        }

        VisTable dimTable = new VisTable(true);
        dimTable.add(xLabel);
        dimTable.add(xField);
        dimTable.add(yLabel);
        dimTable.add(yField).row();
        dimTable.add(widthLabel);
        dimTable.add(widthField);
        dimTable.add(heightLabel);
        dimTable.add(heightField);

        infoTable.addSeparator();
        infoTable.add(headerTable).left().row();
        infoTable.add(dimTable).row();
        infoTable.add(orientationSelect);

        return infoTable;
    }

    protected void updateRendererDims() {
        updateRendererX();
        updateRendererY();
        updateRendererWidth();
        updateRendererHeight();
    }

    protected void updateRendererHeight() {
        float y = renderer.getRelativeY();
        int o = renderer.getOrientation();
        boolean invertY = o == ORIENTATION_TOP_LEFT || o == ORIENTATION_TOP_RIGHT || o == ORIENTATION_TOP;
        if (invertY)
            y = 1-renderer.getRelativeHeight()-y;
        Integer value  = parseIntPercentage(heightField.getText(), 1, (int)(100*(1f - y)));
        yField.setInputValid(value != null);
        heightField.setInputValid(value != null);
        if  (value != null){
            renderer.setRelativeHeight(value / 100f);
        }
    }

    protected void updateRendererWidth() {
        float x = renderer.getRelativeX();
        int o = renderer.getOrientation();
        boolean invertX = o == ORIENTATION_BOTTOM_RIGHT || o == ORIENTATION_TOP_RIGHT || o == ORIENTATION_RIGHT;
        if (invertX)
            x = 1-renderer.getRelativeWidth()-x;
        Integer value  = parseIntPercentage(widthField.getText(), 1, (int)(100*(1f - x)));
        xField.setInputValid(value != null);
        widthField.setInputValid(value != null);
        if  (value != null){
            renderer.setRelativeWidth(value / 100f);
        }
    }

    protected void updateRendererY() {
        Integer value  = parseIntPercentage(yField.getText(), 0, (int)(100*(1f-renderer.getRelativeHeight())));
        yField.setInputValid(value != null);
        heightField.setInputValid(value != null);
        if  (value != null){
            renderer.setRelativeY(value / 100f);
        }
    }

    protected void updateRendererX() {
        Integer value  = parseIntPercentage(xField.getText(), 0, (int)(100*(1f-renderer.getRelativeWidth())));
        xField.setInputValid(value != null);
        widthField.setInputValid(value != null);
        if  (value != null){
            renderer.setRelativeX(value / 100f);
        }
    }

    /**
     * @param inputText
     * @param min
     * @param max
     * @return parsed int value in the given range, null if not parsable or out of the given range
     */
    public static Integer parseIntPercentage(String inputText, int min, int max){

        try {
            int value = Integer.parseInt(inputText);
            boolean valid = value >= min && value <= max;
            return valid ? value : null;
        } catch (NumberFormatException e){
            return null;
        }
    }

    private String getPercentage(float relativeValue) {
        return Math.round(relativeValue*100)+"";
    }

    public VisTable initResourcesTable() {

        if (renderer instanceof ShaderRenderer)
            return null;

        if (resourcesTable != null)
            resourcesTable.remove();

        resourcesTable = new VisTable(true);

        MessageInterfaceGdx messageInterface = client.getMessageInterface();
        Map<String, Float> gpus = messageInterface == null ? new HashMap<>() : messageInterface.getResourceGpus();


        VisLabel cpuCoresLabel = new VisLabel("CPU Threads");
        if (cpuCoresSlider == null) {
            cpuCoresSlider = new VisSlider(0, 1, 1, false);
            cpuCoresSlider.setDisabled(true);
        }
        if (!cpuCoresBound && messageInterface != null) {
            cpuCoresBound = true;
            cpuCoresSlider.setDisabled(false);
            cpuCoresSlider.setRange(0, messageInterface.getResourceMaxCpuCores());
            cpuCoresSlider.setValue(messageInterface.getResourceCpuCores());
            cpuCoresSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (client.getServerConnection() != null)
                        client.getServerConnection().writeMessage(new ResourceRequestMessage(
                                (int) cpuCoresSlider.getValue(), null, gpus));
                }
            });
        }

//        cpuCoresSlider.setValue(0f);
        VisLabel cpuCoresValueLabel = new VisLabel(messageInterface == null ? 0+"" : messageInterface.getResourceCpuCores()+"") {
            @Override
            public float getPrefWidth() {
                return 50;
            }
        };

        resourcesTable.add(cpuCoresLabel).left();
        resourcesTable.add(cpuCoresSlider);
        resourcesTable.add(cpuCoresValueLabel).center().row();

        //TODO test values
//        gpus.put("GPU 0 (NVIDIA Corporation)", (float)Math.random());
//        gpus.put("GPU 1 (NVIDIA Corporation)", 0f);

        List<Pair<VisLabel, VisLabel>> gpuLabels = new ArrayList<>();

        for (Map.Entry<String, Float> e : gpus.entrySet()) {
            VisLabel gpuLabel = new VisLabel(e.getKey());
            VisSlider gpuSlider = new VisSlider(0, 1, 0.01f, false);
            float value = 0f;
            if (e.getValue() != null)
                value = e.getValue();
            gpuSlider.setValue(value);
            VisLabel gpuUtilizationLabel = new VisLabel(getGpuUsageString(gpuSlider.getValue()));
            gpuLabels.add(new Pair<>(gpuLabel, gpuUtilizationLabel));
            gpuSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    gpus.put(gpuLabel.getText().toString(), gpuSlider.getValue());
                    client.getServerConnection().writeMessage(new ResourceRequestMessage(
                            (int)cpuCoresSlider.getValue(), null, gpus));
                }
            });

            resourcesTable.add(gpuLabel).left();
            resourcesTable.add(gpuSlider);
            resourcesTable.add(gpuUtilizationLabel).row();
        }

        if (changedResourcesListener != null)
            client.removeChangedResourcesListener(changedResourcesListener);
        changedResourcesListener = new ChangedResourcesListener() {
            @Override
            public void changedResources(int cpuCores, int maxCpuCores, Map<String, Float> gpus) {
                cpuCoresValueLabel.setText(cpuCores+"");
                cpuCoresSlider.setRange(0, maxCpuCores);
                for (Pair<VisLabel, VisLabel> gpuLbls : gpuLabels){
                    VisLabel nameLabel = gpuLbls.getKey();
                    VisLabel valueLabel = gpuLbls.getValue();
                    String name = nameLabel.getText().toString();
                    valueLabel.setText(getGpuUsageString(gpus.get(name)));
                }
            }
        };
        client.addChangedResourcesListener(changedResourcesListener);
        return resourcesTable;
    }

    private String getGpuUsageString(float value) {
        return Math.round(value * 100) + "%";
    }
}
