package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Table;

public abstract class EntryView {

    boolean valid = true;

    public abstract void addToTable(Table table);
    public abstract void removeFromTable();
    /**
     * read actual field input into internal value fields
     */
    public abstract void readFields();

    public void applyValue(Object value) {};

    public void setInvalid(){
        valid = false;
    }

    public boolean isValid(){
        return valid;
    }

}
