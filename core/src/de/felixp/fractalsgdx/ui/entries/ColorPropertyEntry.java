package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImage;
import com.kotcrab.vis.ui.widget.VisImageButton;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.color.ColorPicker;
import com.kotcrab.vis.ui.widget.color.ColorPickerListener;

import de.felixp.fractalsgdx.ui.actors.WindowAgnosticColorPicker;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class ColorPropertyEntry extends WindowPropertyEntry {

    private static final Drawable white = VisUI.getSkin().getDrawable("white") ;

    static ColorPicker picker;

    Pixmap previewPixmap;

    Color color;

    Image previewImage;

//    ImageButton windowButton;

    public ColorPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, submitValue);
        color = paramContainer.getClientParameter(getPropertyName()).getGeneral(Color.class);


        previewImage = new VisImage(white);
        previewPixmap = new Pixmap(1,1, Pixmap.Format.RGBA8888);
//        windowButton = new ImageButton(previewImage.getDrawable());
//        previewImage.setColor(color);

//        palette = new Palette()
    }

    @Override
    public void readWindowFields() {
    }

    @Override
    public Button getWindowButton() {
        return new VisTextButton("change");
    }

    @Override
    public void openWindow(Stage stage) {
        if (picker == null)
            picker = new WindowAgnosticColorPicker("");
        picker.getPicker().setShowColorPreviews(false);
        stage.addActor(picker.fadeIn());
        picker.setColor(color);
        picker.setListener(new ColorPickerListener() {
            @Override
            public void canceled(Color oldColor) {

            }

            @Override
            public void changed(Color newColor) {
                color = newColor;
                applyClientValue();
                updatePreviewImage();
            }

            @Override
            public void reset(Color previousColor, Color newColor) {
                color = newColor;
            }

            @Override
            public void finished(Color newColor) {
                color = newColor;
                updatePreviewImage();
            }
        });
        updatePreviewImage();
    }

    private void updatePreviewImage() {
        previewPixmap.setColor(color);
        previewPixmap.drawPixel(0,0);
        previewImage.setDrawable(new TextureRegionDrawable(new TextureRegion(new Texture(previewPixmap))));
    }

    @Override
    protected void fillControlsTable(VisTable controlsTable, Button windowButton) {
        super.fillControlsTable(controlsTable, windowButton);
        controlsTable.add(previewImage).size(16).pad(3);
    }

    @Override
    public ParamSupplier getSupplier() {
        return new StaticParamSupplier(getPropertyName(), color);
    }
}

