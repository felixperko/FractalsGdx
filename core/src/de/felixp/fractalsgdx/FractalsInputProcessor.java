package de.felixp.fractalsgdx;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

public class FractalsInputProcessor implements InputProcessor {

    public static float iterationsStep = 0;
    public static long lastIterationStepTime = 0;
    public static float iterationsChangeSpeed = 0.05f;

    @Override
    public boolean keyDown(int keycode) {
//        System.out.println(keycode);
//        if (keycode == Input.Keys.PLUS) {
//            iterationsStep += iterationsChangeSpeed;
//            lastIterationStepTime = System.currentTimeMillis();
//        } else if (keycode == Input.Keys.MINUS) {
//            iterationsStep -= iterationsChangeSpeed;
//            lastIterationStepTime = System.currentTimeMillis();
//        } else if (keycode == Input.Keys.J) {
//            FractalsGdxMain.juliaset = !FractalsGdxMain.juliaset;
//            System.out.println(FractalsGdxMain.juliaset);
//        }  else if (keycode == Input.Keys.B) {
//            FractalsGdxMain.burningship = !FractalsGdxMain.burningship;
//        } else
//            return false;
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
//        if (keycode == Input.Keys.PLUS)
//            iterationsStep -= iterationsChangeSpeed;
//        else if (keycode == Input.Keys.MINUS)
//            iterationsStep += iterationsChangeSpeed;
//        else
//            return false;
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        // TODO Auto-generated method stub
        return false;
    }

    public static double speedMultiplier_zoom = 2;
    public static double speedMultiplier_pan = 2;

    public static double yMultiplier = 1;

    int touchDownScreenX;
    int touchDownScreenY;

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
//		if (button == 0) {
//			FractalsGdxMain.scale /= 2;
//		} else if (button == 1) {
//			FractalsGdxMain.scale *= 2;
//		}
//        if (button == 0) {
//            FractalsGdxMain.scalingFactor = -speedMultiplier_zoom;
//            return true;
//        } else if (button == 1) {
//            FractalsGdxMain.scalingFactor = +speedMultiplier_zoom;
//            return true;
//        }

        this.touchDownScreenX = screenX;
        this.touchDownScreenY = screenY;

        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
//        if (button == 0) {
//            FractalsGdxMain.scalingFactor = 0;
//            return true;
//        } else if (button == 1) {
//            FractalsGdxMain.scalingFactor = 0;
//            return true;
//        }

        System.out.println("button: "+button);
        if (touchDownScreenX != screenX || touchDownScreenY != screenY)
            return false;

        boolean changed = false;
        if (button == Input.Buttons.LEFT) {
            FractalsGdxMain.controls.updateZoom(0.5f);
            changed = true;
        }else if (button == Input.Buttons.RIGHT){
            FractalsGdxMain.controls.updateZoom(2f);
            changed = true;
        }

        if (changed){
            FractalsGdxMain.xPos = 0;
            FractalsGdxMain.yPos = 0;
            FractalsGdxMain.controls.incrementJobId();
            synchronized (FractalsGdxMain.newPixmaps) {
                FractalsGdxMain.newPixmaps.forEach((x, xMap) -> xMap.forEach((y, pixmap) -> pixmap.dispose()));
                FractalsGdxMain.newPixmaps.clear();
            }
            synchronized (FractalsGdxMain.textures) {
                FractalsGdxMain.textures.forEach((x, xMap) -> xMap.forEach((y, texture) -> texture.dispose()));
                FractalsGdxMain.textures.clear();
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        // TODO Auto-generated method stub
        return false;
    }

}
