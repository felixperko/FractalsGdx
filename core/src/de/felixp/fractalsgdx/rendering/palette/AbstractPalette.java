package de.felixp.fractalsgdx.rendering.palette;

import com.badlogic.gdx.graphics.Texture;

public abstract class AbstractPalette implements IPalette {

    String name;
    Texture texture;

    public AbstractPalette(String name, Texture texture) {
        this.name = name;
        this.texture = texture;
    }

    public AbstractPalette(String name) {
        this.name = name;
    }

    public abstract String getTypeName();

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture){
        setTexture(texture, true);
    }

    public void setTexture(Texture texture, boolean disposeOld) {
        if (disposeOld && this.texture != null)
            this.texture.dispose();
        this.texture = texture;
    }



}
