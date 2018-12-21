package de.felixp.fractalsgdx;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RenderSettings{

    String settingPath;
    Map<String, Serializable> settings = new HashMap<String, Serializable>();

    public RenderSettings(String settingPath){
        this.settingPath = settingPath;
        load();
    }

    public RenderSettings(Map<String, Serializable> settings){
        settingPath = null;
        this.settings = settings;
    }

    public RenderSettings(Map<String, Serializable> settings, String filePath){
        settingPath = filePath;
        this.settings = settings;
    }

    public boolean isSaveable(){
        return settingPath != null;
    }

    private void load(){
        //TODO load file
    }

    public <T> T getSetting(Class<T> cls, String name){
        return (T) settings.get(name);
    }

    public String getSettingS(String name){
        return getSetting(String.class, name);
    }

    public Double getSettingD(String name){
        return getSetting(Double.class, name);
    }

    public Float getSettingF(String name){
        return getSetting(Float.class, name);
    }

    public Integer getSettingI(String name){
        return getSetting(Integer.class, name);
    }

    public Boolean getSettingB(String name){
        return getSetting(Boolean.class, name);
    }
}
