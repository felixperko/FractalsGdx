package de.felixp.fractalsgdx.rendering.palette;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;

public class ImagePalette extends AbstractPalette {

    public static final String TYPE_IMAGE = "Image palette";

    public ImagePalette(FileHandle fileHandle) {
        super(fileHandle.name());
        setTexture(new Texture(fileHandle));
    }

    public ImagePalette(String paletteName, Texture texture) {
        super(paletteName);
        setTexture(texture);
    }

    @Override
    public String getTypeName() {
        return TYPE_IMAGE;
    }
}
