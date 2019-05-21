package de.felixp.fractalsgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;

public abstract class AbstractRenderer extends WidgetGroup implements Renderer {

    boolean screenshot;
//    boolean refresh = true;

    public abstract void reset();

    @Override
    public void setScreenshot(boolean screenshot) {
        this.screenshot = screenshot;
    }

    @Override
    public boolean isScreenshot(boolean reset) {
        boolean curr = screenshot;
        if (reset)
            screenshot = false;
        return curr;
    }

    public void setRefresh(){
//        this.refresh = true;
        if (!Gdx.graphics.isContinuousRendering())
            Gdx.graphics.requestRendering();
    }

//    public boolean isRefresh(boolean reset){
//        boolean val = refresh;
//        if (reset)
//            refresh = false;
//        return val;
//    }
}
