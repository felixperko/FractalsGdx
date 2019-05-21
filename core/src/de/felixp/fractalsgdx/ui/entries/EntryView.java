package de.felixp.fractalsgdx.ui.entries;

import com.badlogic.gdx.scenes.scene2d.ui.Table;

abstract class EntryView {
    public abstract void drawOnTable(Table table);
    public abstract void removeFromTable();
}
