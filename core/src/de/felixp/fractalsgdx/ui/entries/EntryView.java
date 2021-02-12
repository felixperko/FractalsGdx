package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Table;

public abstract class EntryView {

    boolean valid = true;

    public abstract void addToTable(Table table);
    public abstract void removeFromTable();

    public void setInvalid(){
        valid = false;
    }

    public boolean isValid(){
        return valid;
    }
}
