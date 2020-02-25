package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class CollapsiblePropertyListButton extends VisTextButton {

    String category;

    public CollapsiblePropertyListButton(String text, String category, ChangeListener listener) {
        super(text, listener);
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
