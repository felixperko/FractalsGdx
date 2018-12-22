package de.felixp.fractalsgdx;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

public class FractalsInputProcessor implements InputProcessor {

    public static float iterationsStep = 0;
    public static long lastIterationStepTime = 0;
    public static float iterationsChangeSpeed = 0.05f;

    @Override
    public boolean keyDown(int keycode) {
        System.out.println(keycode);
        if (keycode == Input.Keys.PLUS) {
            iterationsStep += iterationsChangeSpeed;
            lastIterationStepTime = System.currentTimeMillis();
        } else if (keycode == Input.Keys.MINUS) {
            iterationsStep -= iterationsChangeSpeed;
            lastIterationStepTime = System.currentTimeMillis();
        } else if (keycode == Input.Keys.J) {
            FractalsGdxMain.juliaset = !FractalsGdxMain.juliaset;
            System.out.println(FractalsGdxMain.juliaset);
        }  else if (keycode == Input.Keys.B) {
            FractalsGdxMain.burningship = !FractalsGdxMain.burningship;
        } else
            return false;
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.PLUS)
            iterationsStep -= iterationsChangeSpeed;
        else if (keycode == Input.Keys.MINUS)
            iterationsStep += iterationsChangeSpeed;
        else
            return false;
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
        return false;
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
