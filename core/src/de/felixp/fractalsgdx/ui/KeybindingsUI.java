package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisWindow;

import de.felixp.fractalsgdx.FractalsGdxMain;

public class KeybindingsUI {

    public static Table getKeysTable(VisWindow settingsWindow) {
        Table keysTable = new VisTable(true);

        MainStage stage = (MainStage) FractalsGdxMain.stage;
        for (Keybinding keybinding : stage.getKeybindings()){
            VisTextField field = new VisTextField(keybinding.getKeysString());
            field.setDisabled(true);

            keysTable.add(keybinding.description).left();
            keysTable.add(field).row();
        }

        return keysTable;
    }
}
