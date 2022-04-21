package de.felixp.fractalsgdx.ui;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;

public class ImageRenderer extends WidgetGroup {

    Texture texture;
    int width, height;

    int offsetX = 0;
    int offsetY = 0;

    public ImageRenderer(){
        super();

        addListener(new ActorGestureListener(){
            @Override
            public void tap(InputEvent event, float x, float y, int count, int button) {
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
                offsetX -= deltaX;
                offsetY += deltaY;

                offsetX = Math.max(0, Math.min(offsetX, Math.max(0, texture.getWidth()-width)));
                offsetY = Math.max(0, Math.min(offsetY, Math.max(0, texture.getHeight()-height)));
            }
        });
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (texture != null){
            if (width > 0 && height > 0)
                batch.draw(texture, getX(), getY(), offsetX, offsetY, width, height);
//            else
//                batch.draw(texture, 0, 0);
        }
    }

    public void setPixmap(Pixmap pixmap, int width, int height){
        setDimensions(width, height);
        setPixmap(pixmap);
    }

    public void setPixmap(Pixmap pixmap){
        if (this.texture != null)
            this.texture.dispose();
        this.texture = new Texture(pixmap, true);
    }

    public void setDimensions(int width, int height){
        this.width = width;
        this.height = height;
    }

    @Override
    public float getPrefWidth() {
        return width;
    }

    @Override
    public float getPrefHeight() {
        return height;
    }
}
