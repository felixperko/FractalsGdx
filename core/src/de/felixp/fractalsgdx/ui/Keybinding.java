package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import java.util.ArrayList;
import java.util.List;

import de.felixp.fractalsgdx.FractalsGdxMain;
import sun.tools.jar.Main;

public abstract class Keybinding {

    List<Integer> keys = new ArrayList<>();
    String description;
    boolean requiresRendererFocus = true;

    public Keybinding(String description, int... keys){
        this.description = description;
        for (int key : keys)
            this.keys.add(key);
    }

    public void update(){

        MainStage stage = (MainStage) FractalsGdxMain.stage;

        if (requiresRendererFocus && stage.getKeyboardFocus() != stage.getFocusedRenderer())
            return;

        boolean oneKeyJustPressed = false;
        for (int key : keys) {
            if (!Gdx.input.isKeyPressed(key))
                return;
            if (Gdx.input.isKeyJustPressed(key))
                oneKeyJustPressed = true;
        }

        if (!oneKeyJustPressed)
            return;

        apply();
    }

    public abstract void apply();

    public String getDescription(){
        return description;
    }

    public void setRequiresRendererFocus(boolean requiresRendererFocus) {
        this.requiresRendererFocus = requiresRendererFocus;
    }

    public String getKeysString(){
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int key : keys){
            if (first)
                first = false;
            else
                sb.append(" + ");
            sb.append(Input.Keys.toString(key));
        }
        return sb.toString();
    }
}
