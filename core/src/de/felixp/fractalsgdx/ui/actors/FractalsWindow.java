package de.felixp.fractalsgdx.ui.actors;

import com.kotcrab.vis.ui.widget.VisWindow;

import de.felixp.fractalsgdx.ui.MainStage;

/**
 * VisWindow with additional functionality:
 * - recenter if stage is resized
 * - set focus to active renderer if the window is removed
 */
public class FractalsWindow extends VisWindow {

    boolean autoRefocus = true;
    boolean autoReposition = true;

    public FractalsWindow(String title) {
        super(title);
    }

    public void setAutoRefocus(boolean autoRefocus){
        this.autoRefocus = autoRefocus;
    }

    public boolean isAutoReposition() {
        return autoReposition;
    }

    public void setAutoReposition(boolean autoReposition){
        this.autoReposition = autoReposition;
    }

    public void reposition() {
        pack();
        centerWindow();
    }

    @Override
    public boolean remove() {
        if (autoRefocus && getStage() instanceof MainStage)
            ((MainStage)getStage()).resetKeyboardFocus();
        return super.remove();
    }
}
