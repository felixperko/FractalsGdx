package de.felixp.fractalsgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector2;

import static de.felixp.fractalsgdx.FractalsGdxMain.*;

public class FractalsGestureListener implements GestureDetector.GestureListener {

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
//        xPos -= scale*deltaX / Gdx.graphics.getHeight();
//        yPos += FractalsInputProcessor.yMultiplier * scale*deltaY / Gdx.graphics.getHeight();
        xPos += deltaX;
        yPos += deltaY;
        FractalsGdxMain.client.updatePosition(deltaX, deltaY);
        FractalsGdxMain.forceRefresh = true;
        //System.out.println(deltaX+", "+deltaY);
        return true;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        return false;
    }

    double lastDistance = 0;
    double lastInitialDistance = 0;

    long lastZoom = 0;

    @Override
    public boolean zoom(float initialDistance, float distance) {
//        if (initialDistance != lastInitialDistance){
//            lastInitialDistance = initialDistance;
//            lastDistance = initialDistance;
//        }
//        scale *= 1+((lastDistance/distance)-1);
//        lastDistance = distance;
//        lastZoom = System.currentTimeMillis();
//        FractalsGdxMain.forceRefresh = true;
        return true;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }

    @Override
    public void pinchStop() {

    }

    @Override
    public boolean fling(float vX, float vY, int button) {
        if (System.currentTimeMillis() - lastZoom < 100)
            return false;
        velocityX = -vX / Gdx.graphics.getHeight();
        velocityY = vY / Gdx.graphics.getHeight();
        return true;
    }

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
        if (count >= 2) {
            if (button == 0) {
                FractalsGdxMain.scale /= 2;
                FractalsGdxMain.forceRefresh = true;
            } else if (button == 1) {
                FractalsGdxMain.scale *= 2;
                FractalsGdxMain.forceRefresh = true;
            }
            return true;
        } else if (button == 1) {
            FractalsGdxMain.scale *= 2;
            FractalsGdxMain.forceRefresh = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean longPress(float x, float y) {
        return false;
    }
}
