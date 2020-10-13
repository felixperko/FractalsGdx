package de.felixp.fractalsgdx.rendering;

import java.util.Properties;

class RendererProperties extends Properties {

    static String KEY_RENDERER_CLASS = "rendererclass";

    static String KEY_X = "x";
    static String KEY_Y = "y";
    static String KEY_W = "w";
    static String KEY_H = "h";

    public RendererProperties(
//            Class<? extends FractalRenderer> rendererClass,
            float x, float y, float w, float h){
        setX(x);
        setY(y);
        setW(w);
        setH(h);
    }

    public void setRendererClass(Class<? extends FractalRenderer> rendererClass){
        setProperty(KEY_RENDERER_CLASS, rendererClass.getName());
    }

    public Class<? extends FractalRenderer> getRendererClass() throws ClassNotFoundException {
        return (Class<? extends FractalRenderer>)Class.forName(getProperty(KEY_RENDERER_CLASS));
    }

    public float getX(){
        return Float.parseFloat(getProperty(KEY_X));
    }

    public void setX(float val){
        setProperty(KEY_X, val+"");
    }

    public float getY(){
        return Float.parseFloat(getProperty(KEY_Y));
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
}
