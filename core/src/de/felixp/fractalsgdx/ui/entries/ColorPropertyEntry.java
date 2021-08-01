package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.kotcrab.vis.ui.widget.color.ColorPicker;
import com.kotcrab.vis.ui.widget.color.ColorPickerListener;
import com.kotcrab.vis.ui.widget.color.ExtendedColorPicker;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamDefinition;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;

public class ColorPropertyEntry extends WindowPropertyEntry {

    public ColorPropertyEntry(Tree.Node node, ParamContainer paramContainer, ParamDefinition parameterDefinition, boolean submitValue) {
        super(node, paramContainer, parameterDefinition, submitValue);
        color = paramContainer.getClientParameter(getPropertyName()).getGeneral(Color.class);
    }

    Color color;

    @Override
    public void openWindow(Stage stage) {
        ColorPicker picker = new MyColorPicker("");
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
//                ((MainStage) FractalsGdxMain.stage).
            }

            @Override
            public void reset(Color previousColor, Color newColor) {
                color = newColor;
            }

            @Override
            public void finished(Color newColor) {
                color = newColor;
            }
        });
    }

    @Override
    public ParamSupplier getSupplier() {
        return new StaticParamSupplier(getPropertyName(), color);
    }
}

class MyColorPicker extends ColorPicker{
    public MyColorPicker(String title) {
        super(title);
//        setModal(false);
//        setResizable(true);
    }

    @Override
    protected void setStage(Stage stage) {
        super.setStage(stage);
        setModal(false);
    }
}
