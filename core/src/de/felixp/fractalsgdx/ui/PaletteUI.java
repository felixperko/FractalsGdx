package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.kotcrab.vis.ui.util.Validators;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisImage;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.kotcrab.vis.ui.widget.color.ColorPicker;
import com.kotcrab.vis.ui.widget.color.ColorPickerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.rendering.palette.GradientPalette;
import de.felixp.fractalsgdx.rendering.palette.IPalette;
import de.felixp.fractalsgdx.rendering.palette.PalettePoint;
import de.felixp.fractalsgdx.ui.actors.FractalsWindow;
import de.felixp.fractalsgdx.ui.actors.TabTraversableTextField;
import de.felixp.fractalsgdx.ui.actors.WindowAgnosticColorPicker;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

import static de.felixp.fractalsgdx.rendering.palette.GradientPalette.*;

public class PaletteUI {
    static Image image;
    static Texture texture;
//    static Pixmap colorPreviewPixmap = new Pixmap(1,1, Pixmap.Format.RGBA8888);
    static String name;
    static IPalette palette;
    static Table borderTable;
    static boolean populating = false;

    public static Table getPaletteTable(VisWindow settingsWindow) {
        borderTable = new VisTable(true){
            @Override
            protected void sizeChanged() {
                super.sizeChanged();
                if (!populating)
                    repopulatePaletteTable(settingsWindow);
            }
        };

        repopulatePaletteTable(settingsWindow);

        return borderTable;
    }

    protected static void repopulatePaletteTable(VisWindow settingsWindow) {
        populating = true;
        borderTable.clear();
        Table table = new VisTable(true);

        MainStage stage = ((MainStage) FractalsGdxMain.stage);
        Map<String, IPalette> palettes = stage.getPalettes();

        Table selectTable = new VisTable(true);
        Table controlTable = new VisTable(true);

        image = new VisImage();

        SelectBox<String> paletteSelect = new VisSelectBox<>();
        List<String> items = new ArrayList<>((palettes.keySet()));
        paletteSelect.setItems(items.toArray(new String[items.size()]));
        name = stage.getClientParameter(MainStage.PARAMS_PALETTE).getGeneral(String.class);
        paletteSelect.setSelected(name);
        palette = palettes.get(name);
        paletteSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                name = paletteSelect.getSelected();
                stage.getClientParameters().addClientParameter(new StaticParamSupplier(MainStage.PARAMS_PALETTE, name));
                palette = palettes.get(name);
                displayPalette(palette);
                fillControlTable(settingsWindow, controlTable);
            }
        });

        IPalette palette = palettes.get(paletteSelect.getSelected());
        if (palette != null)
            image.setDrawable(new TextureRegionDrawable(palette.getTexture()));
        texture = new Texture(palette.getTexture().getTextureData());

        VisTextButton createButton = new VisTextButton("create new");
        createButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openCreateNewPaletteWindow(settingsWindow, paletteSelect, controlTable);
            }
        });
        VisTextButton importButton = new VisTextButton("import");
        VisTextButton exportButton = new VisTextButton("export");

        Table buttonTable = new VisTable(true);
        buttonTable.add(createButton);
        buttonTable.add(importButton);
        buttonTable.add(exportButton);

        VisTextButton renameButton = new VisTextButton("rename");
        renameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

            }
        });
        VisTextButton removeButton = new VisTextButton("remove");
        removeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                MainStage stage = (MainStage) FractalsGdxMain.stage;
                stage.getClientParameters().addClientParameter(new StaticParamSupplier(MainStage.PARAMS_PALETTE, MainStage.PARAMS_PALETTE_VALUE_DISABLED));
                stage.getPalettes().remove(name);
                name = paletteSelect.getSelected();
                repopulatePaletteTable(settingsWindow);
            }
        });

        selectTable.add("Palette:");
        selectTable.add(paletteSelect);
        selectTable.add(renameButton);
        selectTable.add(removeButton).row();

        fillControlTable(settingsWindow, controlTable);

        table.add(buttonTable).left().row();
        table.add(selectTable).row();

        borderTable.add(table).expandX().fillX().pad(20);
        table.add(image).width(table.getWidth()).height(50).expandX().fillX().pad(3).row();
        table.add(controlTable).row();

        populating = false;
    }

    private static void openPaletteSettingsWindow(VisWindow settingsWindow, Table controlTable, GradientPalette palette) {

        MainStage stage = (MainStage) FractalsGdxMain.stage;

        VisWindow paletteSettingsWindow = new FractalsWindow(palette.getName()+" settings");
        ((FractalsWindow) paletteSettingsWindow).setAutoRefocus(false);
        VisTable contentTable = new VisTable(true);

        VisSelectBox<String> interpolationTypeSelect = new VisSelectBox<>();
        interpolationTypeSelect.setItems(INTERPOLATIONTYPE_LINEAR);
        interpolationTypeSelect.setSelected(palette.getSettingInterpolationType());
        interpolationTypeSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                palette.setSettingInterpolationType(interpolationTypeSelect.getSelected());
                displayPalette(palette);
            }
        });
        VisSelectBox<String> colorSpaceSelect = new VisSelectBox();
        colorSpaceSelect.setItems(COLORSPACE_RGB);
        colorSpaceSelect.setSelected(palette.getSettingColorSpace());
        colorSpaceSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                palette.setSettingColorSpace(colorSpaceSelect.getSelected());
                displayPalette(palette);
            }
        });
        VisCheckBox autoOffsetsCheckbox = new VisCheckBox(null);
        autoOffsetsCheckbox.setChecked(palette.getSettingAutoOffsets());
        autoOffsetsCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                palette.setSettingAutoOffsets(autoOffsetsCheckbox.isChecked());
                repopulatePaletteTable(settingsWindow);
                settingsWindow.pack();
                displayPalette(palette);
            }
        });

        contentTable.add("Interpolation:").left();
        contentTable.add(interpolationTypeSelect).row();
        contentTable.add("Color space:").left();
        contentTable.add(colorSpaceSelect).row();
        contentTable.add("Auto offsets:").left();
        contentTable.add(autoOffsetsCheckbox).row();

        VisTable randomSettingsTable = new VisTable(true);

        TabTraversableTextField randomColorSatMin = new TabTraversableTextField(Validators.FLOATS);
        randomColorSatMin.setText(palette.getSettingRandomColorSatMin()+"");
        TabTraversableTextField randomColorSatMax = new TabTraversableTextField(Validators.FLOATS);
        randomColorSatMax.setText(palette.getSettingRandomColorSatMax()+"");
        TabTraversableTextField randomColorValMin = new TabTraversableTextField(Validators.FLOATS);
        randomColorValMin.setText(palette.getSettingRandomColorValMin()+"");
        TabTraversableTextField randomColorValMax = new TabTraversableTextField(Validators.FLOATS);
        randomColorValMax.setText(palette.getSettingRandomColorValMax()+"");

        randomColorSatMin.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (randomColorSatMin.isInputValid()){
                    palette.setSettingRandomColorSatMin(Double.parseDouble(randomColorSatMin.getText()));
                }
            }
        });
        randomColorSatMax.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (randomColorSatMax.isInputValid()){
                    palette.setSettingRandomColorSatMax(Double.parseDouble(randomColorSatMax.getText()));
                }
            }
        });
        randomColorValMin.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (randomColorValMin.isInputValid()){
                    palette.setSettingRandomColorValMin(Double.parseDouble(randomColorValMin.getText()));
                }
            }
        });
        randomColorValMax.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (randomColorValMax.isInputValid()){
                    palette.setSettingRandomColorValMax(Double.parseDouble(randomColorValMax.getText()));
                }
            }
        });

        randomSettingsTable.add("Random color saturation range:").left();
        randomSettingsTable.add(randomColorSatMin);
        randomSettingsTable.add(randomColorSatMax).row();
        randomSettingsTable.add("Random color value range:").left();
        randomSettingsTable.add(randomColorValMin);
        randomSettingsTable.add(randomColorValMax);

        contentTable.add(randomSettingsTable).colspan(2);

        paletteSettingsWindow.add(contentTable).row();

        paletteSettingsWindow.addCloseButton();
        paletteSettingsWindow.pack();
        paletteSettingsWindow.centerWindow();
        stage.addActor(paletteSettingsWindow);
    }

    private static void openCreateNewPaletteWindow(VisWindow settingsWindow, SelectBox<String> paletteSelect, Table controlTable) {

        MainStage stage = (MainStage) FractalsGdxMain.stage;
        VisWindow createWindow = new FractalsWindow("Create gradient palette");
        ((FractalsWindow) createWindow).setAutoRefocus(false);

        VisTable contentTable = new VisTable(true);

        VisTable nameTable = new VisTable(true);
        VisTable buttonTable = new VisTable(true);

        VisTextField paletteNameField = new VisTextField();
        int number = stage.getPalettes().size()+1;
        String paletteName;
        do {
            paletteName = "Palette "+number;
            number++;
        } while (stage.getPalettes().containsKey(paletteName));
        paletteNameField.setText(paletteName);

        VisTextButton cancelButton = new VisTextButton("cancel");
        cancelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                createWindow.remove();
            }
        });
        VisTextButton createButton = new VisTextButton("create");
        createButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String paletteName = paletteNameField.getText();
                GradientPalette palette = new GradientPalette(paletteName);
                stage.addPalette(palette);
                List<PalettePoint> palettePoints = new ArrayList<>();
                int randomPointCount = MathUtils.random(2, 6);
                float step = 100f/randomPointCount;
                float startLower = 0;
                float startUpper = step;
                for (int i = 0; i < randomPointCount; i++){
                    double relativePos = i == 0 ? 0.0 : ((int)MathUtils.random(startLower, startUpper))/100.0;
                    startLower += step;
                    startUpper += step;
                    Color randomHsvColor = getRandomHsvColor(0.5, 1, 0.5, 1);
                    palettePoints.add(new PalettePoint(randomHsvColor, relativePos));
                }
                palette.setPalettePoints(palettePoints);
                stage.getParamUI().getClientParamsSideMenu().getParamContainer().addClientParameter(
                        new StaticParamSupplier(MainStage.PARAMS_PALETTE, paletteName)
                );
                stage.refreshClientSideMenu();
                List<String> paletteNames = new ArrayList<>(stage.getPalettes().keySet());
                paletteSelect.setItems(paletteNames.toArray(new String[paletteNames.size()]));
                paletteSelect.setSelected(paletteName);
                PaletteUI.palette = stage.getPalettes().get(paletteName);
//                fillControlTable(settingsWindow, controlTable);
                repopulatePaletteTable(settingsWindow);
                settingsWindow.pack();
                settingsWindow.centerWindow();
                createWindow.remove();
            }
        });

        nameTable.add("Name:");
        nameTable.add(paletteNameField);

        buttonTable.add(cancelButton);
        buttonTable.add(createButton);

        contentTable.add(nameTable).row();
        contentTable.add(buttonTable);
        createWindow.add(contentTable);

        createWindow.addCloseButton();
        createWindow.pack();
        createWindow.centerWindow();
        stage.addActor(createWindow);
    }

    static ColorPicker picker;

    private static void fillControlTable(VisWindow settingsWindow, Table controlTable) {
        controlTable.clear();

        if (!(palette instanceof GradientPalette))
            return;

        GradientPalette gpal = (GradientPalette)palette;
        List<PalettePoint> palettePoints = gpal.getPalettePoints();
        if (palettePoints.isEmpty())
            return;
        Collections.sort(palettePoints);

        VisTextButton settingsButton = new VisTextButton("Gradient palette settings");
        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openPaletteSettingsWindow(settingsWindow, controlTable, (GradientPalette) palette);
            }
        });
        VisTextButton duplicateButton = new VisTextButton("duplicate");
        duplicateButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (palette instanceof GradientPalette){
                    MainStage stage = ((MainStage)FractalsGdxMain.stage);
                    GradientPalette currPal = (GradientPalette)palette;
                    GradientPalette newPal = currPal.copy();
                    stage.addPalette(newPal);
                    stage.getClientParameters().addClientParameter(new StaticParamSupplier(MainStage.PARAMS_PALETTE, newPal.getName()));
                    repopulatePaletteTable(settingsWindow);
                }
            }
        });

        boolean autoOffsets = ((GradientPalette) palette).getSettingAutoOffsets();
        int cols = autoOffsets ? 5 : 6;

        VisTable innerSettingsTable = new VisTable(true);
        innerSettingsTable.add(settingsButton).left();
        innerSettingsTable.add(duplicateButton);
        controlTable.add(innerSettingsTable).colspan(cols).row();

        controlTable.add("Offset:").colspan(autoOffsets ? 1 : 2);
        controlTable.add("Color:").colspan(3).row();

        List<Image> images = new ArrayList<>();


        for (PalettePoint point : palettePoints){
            double offset = point.getRelativePos();
            TabTraversableTextField offsetField = new TabTraversableTextField((float)(offset*100d)+"");
            final PalettePoint finalPoint = point;

            VisSlider offsetSlider = null;
            if (!autoOffsets) {
                offsetSlider = new VisSlider(0, 1, 0.005f, false);
                offsetSlider.setValue((float) offset);
                offsetSlider.setDisabled(autoOffsets);
                final VisSlider finalOffsetSlider = offsetSlider;
                offsetSlider.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        float value = finalOffsetSlider.getValue();
                        offsetField.setText(value * 100d + "");
                        finalPoint.setRelativePos(value);
                        updatePalette();
                        displayPalette(palette);
                    }
                });
            }

            offsetField.addValidator(Validators.FLOATS);
            offsetField.setDisabled(autoOffsets);
            final VisSlider finalOffsetSlider = offsetSlider;
            offsetField.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (!offsetField.isInputValid())
                        return;
                    double actualVal = Double.parseDouble(offsetField.getText()) / 100d;
                    if (finalOffsetSlider != null)
                        finalOffsetSlider.setValue((float)actualVal);
                    finalPoint.setRelativePos(actualVal);
                    updatePalette();
                    displayPalette(palette);
                }
            });
            Image colorImage = new Image();
            images.add(colorImage);
            VisTextButton setColorButton = new VisTextButton("set");
            setColorButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (picker == null){
                        picker = new WindowAgnosticColorPicker("");
                        picker.getPicker().setShowColorPreviews(false);
                    }
                    controlTable.getStage().addActor(picker.fadeIn());
                    picker.setListener(new ColorPickerAdapter(){
                        @Override
                        public void changed(Color newColor) {
                            finalPoint.setColor(newColor.cpy());
                            ((GradientPalette) palette).setPalettePoints(palettePoints);
                            displayPalette(palette);
                            updateColorImage(colorImage, newColor.cpy());
//                            updatePalette();
                        }
                    });
                    picker.setColor(finalPoint.getColor().cpy());
                }
            });
            VisTextButton setRandomColorButton = new VisTextButton("random");
            setRandomColorButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    double lowerS = ((GradientPalette) palette).getSettingRandomColorSatMin();
                    double higherS = ((GradientPalette) palette).getSettingRandomColorSatMax();
                    double lowerV = ((GradientPalette) palette).getSettingRandomColorValMin();
                    double higherV = ((GradientPalette) palette).getSettingRandomColorValMax();
                    finalPoint.setColor(getRandomHsvColor(lowerS, higherS, lowerV, higherV));
                    ((GradientPalette) palette).paletteGeneratorUpdate();
                    displayPalette(palette);
                    updateColorImage(colorImage, finalPoint.getColor());
                }
            });
            VisTextButton removeButton = new VisTextButton("remove");
            removeButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    ((GradientPalette) palette).removePalettePoint(finalPoint);
                    displayPalette(palette);
                    updateColorImage(colorImage, finalPoint.getColor());
                }
            });

            updateColorImage(colorImage, point.getColor());

            if (offsetSlider != null)
                controlTable.add(offsetSlider);
            controlTable.add(offsetField).width(75);
            controlTable.add(colorImage).size(16);
            controlTable.add(setColorButton);
            controlTable.add(setRandomColorButton);
            controlTable.add(removeButton).row();
        }

        VisTextButton addPalettePointButton = new VisTextButton("add palette point");
        addPalettePointButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (palette instanceof GradientPalette) {
                    double lowerS = ((GradientPalette) palette).getSettingRandomColorSatMin();
                    double higherS = ((GradientPalette) palette).getSettingRandomColorSatMax();
                    double lowerV = ((GradientPalette) palette).getSettingRandomColorValMin();
                    double higherV = ((GradientPalette) palette).getSettingRandomColorValMax();
                    Color randColor = getRandomHsvColor(lowerS, higherS, lowerV, higherV);
                    double pos = (getRandomIntPercentage())/100.0;
                    ((GradientPalette) palette).addPalettePoint(new PalettePoint(randColor, pos));
                    repopulatePaletteTable(settingsWindow);
                    settingsWindow.pack();
                    settingsWindow.centerWindow();
                    displayPalette(palette);
                    if (MainStageWindows.scrollPane != null)
                        MainStageWindows.scrollPane.setScrollPercentY(100);
                }
            }
        });

        VisTextButton randomizeAllOffsetsButton = null;
        if (!autoOffsets) {
            randomizeAllOffsetsButton = new VisTextButton("random offsets");
            randomizeAllOffsetsButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    List<PalettePoint> palettePoints = ((GradientPalette) palette).getPalettePoints();
                    int set0Index = MathUtils.random(0, palettePoints.size() - 1);
                    int index = 0;
                    for (PalettePoint point : palettePoints) {
                        point.setRelativePos(index == set0Index ? 0.0 : Math.random());
                        index++;
                    }
                    ((GradientPalette) palette).paletteGeneratorUpdate();
                    displayPalette(palette);
                    fillControlTable(settingsWindow, controlTable);
                }
            });
        }

        VisTextButton randomizeAllColorsButton = new VisTextButton("random colors");
        randomizeAllColorsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                List<PalettePoint> points = ((GradientPalette) palette).getPalettePoints();
                for (int i = 0; i < points.size() ; i++) {
                    double lowerS = ((GradientPalette) palette).getSettingRandomColorSatMin();
                    double higherS = ((GradientPalette) palette).getSettingRandomColorSatMax();
                    double lowerV = ((GradientPalette) palette).getSettingRandomColorValMin();
                    double higherV = ((GradientPalette) palette).getSettingRandomColorValMax();
                    Color randColor = getRandomHsvColor(lowerS, higherS, lowerV, higherV);
                    points.get(i).setColor(randColor);
                    updateColorImage(images.get(i), randColor);
                }
                ((GradientPalette) palette).paletteGeneratorUpdate();
                displayPalette(palette);
            }
        });

        Table buttonTable = new VisTable(true);
        if (!autoOffsets)
            buttonTable.add(randomizeAllOffsetsButton);
        buttonTable.add(addPalettePointButton);
        buttonTable.add(randomizeAllColorsButton);

        controlTable.add(buttonTable).colspan(cols);
    }

    public static int getRandomIntPercentage() {
        return (int)(Math.random()*100.0);
    }

    public static Color getRandomHsvColor(double lowerS, double higherS, double lowerV, double higherV) {
        float s = (float)(lowerS + (Math.random()*(higherS-lowerS)));
        float v = (float)(lowerV + (Math.random()*(higherV-lowerV)));
        return new Color(1, 1, 1, 1).fromHsv((float)(Math.random()*360), s, v);
    }

    private static void updateColorImage(Image colorImage, Color newColor) {
        Drawable drawable = colorImage.getDrawable();
        if (drawable instanceof TextureRegionDrawable) {
            ((TextureRegionDrawable) drawable).getRegion().getTexture().dispose();
        }
        Pixmap newPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        newPixmap.setColor(newColor);
        newPixmap.drawPixel(0,0);
        colorImage.setDrawable(new TextureRegionDrawable(new TextureRegion(new Texture(newPixmap))));
    }

    private static void updatePalette(){
        if (palette instanceof GradientPalette)
            ((GradientPalette)palette).paletteGeneratorUpdate();
    }

    private static void displayPalette(IPalette palette){
        if (texture != null) {
            texture.dispose();
        }
        texture = new Texture(palette.getTexture().getTextureData());
        if (texture != null)
            image.setDrawable(new TextureRegionDrawable(texture));
    }
}
