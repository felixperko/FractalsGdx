package de.felixp.fractalsgdx.rendering;

import java.util.Properties;

public class RendererProperties extends Properties {

    public static int ORIENTATION_FULLSCREEN = 0;
    public static int ORIENTATION_LEFT = 1;
    public static int ORIENTATION_RIGHT = 2;
    public static int ORIENTATION_TOP = 3;
    public static int ORIENTATION_BOTTOM = 4;
    public static int ORIENTATION_TOP_LEFT = 5;
    public static int ORIENTATION_TOP_RIGHT = 6;
    public static int ORIENTATION_BOTTOM_LEFT = 7;
    public static int ORIENTATION_BOTTOM_RIGHT = 8;

    static String KEY_RENDERER_CLASS = "rendererclass";

    static String KEY_X = "x";
    static String KEY_Y = "y";
    static String KEY_W = "w";
    static String KEY_H = "h";
    static String KEY_ORIENTATION = "orientation";

    public RendererProperties(
//            Class<? extends FractalRenderer> rendererClass,
            float x, float y, float w, float h, int orientation){
        setX(x);
        setY(y);
        setW(w);
        setH(h);
        setOrientation(orientation);
    }

    public void setRendererClass(Class<? extends FractalRenderer> rendererClass){
        setProperty(KEY_RENDERER_CLASS, rendererClass.getName());
    }

    public Class<? extends FractalRenderer> getRendererClass() throws ClassNotFoundException {
        return (Class<? extends FractalRenderer>)Class.forName(getProperty(KEY_RENDERER_CLASS));
    }

    public float getX(){
        float x = Float.parseFloat(getProperty(KEY_X));
        int o = (int) getOrientation();
        if (o == ORIENTATION_BOTTOM_RIGHT || o == ORIENTATION_TOP_RIGHT || o == ORIENTATION_RIGHT)
            x = 1f-getW()-x;
        return x;
    }

    public void setX(float val){
        setProperty(KEY_X, val+"");
    }

    public float getY(){
        float y = Float.parseFloat(getProperty(KEY_Y));
        int o = (int) getOrientation();
        if (o == ORIENTATION_TOP_LEFT || o == ORIENTATION_TOP_RIGHT || o == ORIENTATION_TOP)
            y = 1f-getH()-y;
        return y;
    }

    public void setY(float val){
        setProperty(KEY_Y, val+"");
    }

    public float getW(){
        return Float.parseFloat(getProperty(KEY_W));
    }

    public void setW(float val){
        setProperty(KEY_W, val+"");
    }

    public float getH(){
        return Float.parseFloat(getProperty(KEY_H));
    }

    public void setH(float val){
        setProperty(KEY_H, val+"");
    }

    public float getOrientation(){
        return Float.parseFloat(getProperty(KEY_ORIENTATION));
    }

    public void setOrientation(float val){
        setProperty(KEY_ORIENTATION, val+"");
    }
}
