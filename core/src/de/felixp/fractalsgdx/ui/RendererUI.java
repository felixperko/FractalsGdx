package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.felixp.fractalsgdx.rendering.FractalRenderer;
import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.rendering.RemoteRenderer;
import de.felixp.fractalsgdx.rendering.RendererContext;
import de.felixp.fractalsgdx.rendering.ShaderRenderer;
import de.felixp.fractalsgdx.remoteclient.ChangedResourcesListener;
import de.felixp.fractalsgdx.remoteclient.MessageInterfaceGdx;
import de.felixperko.fractals.network.messages.ResourceRequestMessage;

import static de.felixp.fractalsgdx.FractalsGdxMain.client;

public class RendererUI {

    static String RENDERER_REMOTE = "Remote";
    static String RENDERER_SHADER = "Shader";

    static Map<String, Class<? extends FractalRenderer>> availableRenderers =
            new HashMap<String, Class<? extends FractalRenderer>>(){
        {
            put(RENDERER_REMOTE, RemoteRenderer.class);
            put(RENDERER_SHADER, ShaderRenderer.class);
        }
    };

    VisTable infoTable;
    VisLabel rendererLabel;
    VisSelectBox<String> rendererSelection;
    VisTextButton removeButton;

    VisLabel xLabel, yLabel, widthLabel, heightLabel;
    VisTextField xField, yField, widthField, heightField;

    VisTable resourcesTable = null;
    VisSlider cpuCoresSlider = null;
    boolean cpuCoresBound = false;
    ChangedResourcesListener changedResourcesListener = null;

    FractalRenderer renderer;

    public RendererUI(FractalRenderer renderer){
        this.renderer = renderer;
    }

    public VisTable initInfoTable(){
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

                //set as main renderer if old renderer were
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

                MainStageWindows.openSettingsMenu(stage);
//                for (FractalRenderer renderer : renderers)
//                    stage.addFractalRenderer((AbstractFractalRenderer)renderer);
            }
        });



            xLabel = new VisLabel("x: ");
            yLabel = new VisLabel("y: ");
            widthLabel = new VisLabel("width: ");
            heightLabel = new VisLabel("height: ");

            xField = new VisTextField(getPercentage(renderer.getRelativeX()));
            yField = new VisTextField(getPercentage(renderer.getRelativeY()));
            widthField = new VisTextField(getPercentage(renderer.getRelativeWidth()));
            heightField = new VisTextField(getPercentage(renderer.getRelativeHeight()));

            xField.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    try {
                        renderer.setRelativeX(Integer.parseInt(xField.getText()) / 100f);
                        xField.setInputValid(true);
                    } catch (NumberFormatException e){xField.setInputValid(false);}
                }
            });
            yField.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    try {
                        renderer.setRelativeY(Integer.parseInt(yField.getText()) / 100f);
                        yField.setInputValid(true);
                    } catch (NumberFormatException e){yField.setInputValid(false);}
                }
            });
            widthField.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    try {
                        renderer.setRelativeWidth(Integer.parseInt(widthField.getText()) / 100f);
                        widthField.setInputValid(true);
                    } catch (NumberFormatException e){widthField.setInputValid(false);}
                }
            });
            heightField.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    try {
                        renderer.setRelativeHeight(Integer.parseInt(heightField.getText()) / 100f);
                        heightField.setInputValid(true);
                    } catch (NumberFormatException e){heightField.setInputValid(false);}
                }
            });

            VisTable headerTable = new VisTable(true);
            headerTable.add(rendererLabel);
            headerTable.add(rendererSelection);
            headerTable.add(removeButton);

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
            infoTable.add(dimTable);
//        }
        return infoTable;
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
