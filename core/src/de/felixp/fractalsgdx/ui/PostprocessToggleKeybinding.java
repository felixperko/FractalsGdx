package de.felixp.fractalsgdx.ui;

import de.felixp.fractalsgdx.FractalsGdxMain;

public class PostprocessToggleKeybinding extends Keybinding{

    String paramName;

    public PostprocessToggleKeybinding(String description, String paramName, int... keys){
        super(description, keys);
        this.paramName = paramName;
    }

    @Override
    public void apply() {
        MainStage stage = (MainStage) FractalsGdxMain.stage;
        Object val = stage.getClientParameter(paramName).getGeneral();
        stage.getParamUI().clientParamsSideMenu.getPropertyEntry(paramName).setValue(!(Boolean)val);
    }
}
