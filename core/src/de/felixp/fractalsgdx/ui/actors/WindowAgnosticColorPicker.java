package de.felixp.fractalsgdx.ui.actors;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.kotcrab.vis.ui.widget.color.ColorPicker;

/**
 * Doesn't restrict inputs to its own window.
 */
public class WindowAgnosticColorPicker extends ColorPicker {
    public WindowAgnosticColorPicker(String title) {
        super(title);
        getPicker().setShowColorPreviews(true);
    }

    @Override
    protected void setStage(Stage stage) {
        super.setStage(stage);
        setModal(false); //allows to click other windows
    }
}
