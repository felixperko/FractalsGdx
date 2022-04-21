package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import java.util.ArrayList;
import java.util.List;

import de.felixperko.fractals.data.ParamContainer;

public class ParamKeybinding extends Keybinding{

    ParamContainer paramContainer;

    public ParamKeybinding(String description, int... keys){
        super(description, keys);
    }

    @Override
    public void apply() {
        //TODO
        changed();
    }

    public void changed() {
    }

    public ParamContainer getParamContainer() {
        return paramContainer;
    }

    public void setParamContainer(ParamContainer paramContainer) {
        this.paramContainer = paramContainer;
    }
}
