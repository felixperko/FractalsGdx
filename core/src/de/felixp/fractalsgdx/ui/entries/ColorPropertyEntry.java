package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisImage;
import com.kotcrab.vis.ui.widget.VisImageButton;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.color.ColorPicker;
import com.kotcrab.vis.ui.widget.color.ColorPickerListener;
import com.kotcrab.vis.ui.widget.color.internal.Palette;
import com.kotcrab.vis.ui.widget.color.internal.PickerCommons;

import de.felixp.fractalsgdx.ui.actors.WindowAgnosticColorPicker;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class ColorPropertyEntry extends WindowPropertyEntry {

    private static final Drawable white = VisUI.getSkin().getDrawable("white") ;

    public ColorPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, submitValue);
        color = paramContainer.getClientParameter(getPropertyName()).getGeneral(Color.class);


        image = new VisImage(white);
        image.setColor(color);

//        palette = new Palette()
    }

    Color color;

//    Palette palette;

    Image image;

    @Override
    public void openWindow(Stage stage) {
        ColorPicker picker = new WindowAgnosticColorPicker("");
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
                image.setColor(color);
//                ((MainStage) FractalsGdxMain.stage).
            }

            @Override
            public void reset(Color previousColor, Color newColor) {
                color = newColor;
            }

            @Override
            public void finished(Color newColor) {
                color = newColor;
                image.setColor(color);
            }
        });
    }

    @Override
    protected void fillControlsTable(VisTable controlsTable, VisTextButton windowButton) {
        super.fillControlsTable(controlsTable, windowButton);
        controlsTable.add(image).size(32).pad(3);
    }

    @Override
    public ParamSupplier getSupplier() {
        return new StaticParamSupplier(getPropertyName(), color);
    }
}

