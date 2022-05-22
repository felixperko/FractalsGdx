package de.felixp.fractalsgdx.ui;

import de.felixp.fractalsgdx.FractalsGdxMain;

public class PostprocessToggleKeybinding extends Keybinding{

    String paramUID;

    public PostprocessToggleKeybinding(String description, String paramName, int... keys){
        super(description, keys);
        this.paramUID = paramName;
    }

    @Override
    public void apply() {
        MainStage stage = (MainStage) FractalsGdxMain.stage;
        Object val = stage.getClientParam(paramUID).getGeneral();
        stage.getParamUI().clientParamsSideMenu.getPropertyEntryByUID(paramUID).setValue(!(Boolean)val);
    }
}
