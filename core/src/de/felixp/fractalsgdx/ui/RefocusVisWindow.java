package de.felixp.fractalsgdx.ui;

import com.kotcrab.vis.ui.widget.VisWindow;

public class RefocusVisWindow extends VisWindow {

    boolean autoRefocus = true;
    boolean autoReposition = true;

    public RefocusVisWindow(String title) {
        super(title);
    }

    public void setAutoRefocus(boolean autoRefocus){
        this.autoRefocus = autoRefocus;
    }

    @Override
    public boolean remove() {
        if (autoRefocus && getStage() instanceof MainStage)
            ((MainStage)getStage()).resetKeyboardFocus();
        return super.remove();
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
}
