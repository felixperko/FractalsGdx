package de.felixp.fractalsgdx.rendering.palette;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GradientPalette extends AbstractPalette{

    public static final String TYPE_GRADIENT = "Gradient palette";
    public static int DEFAULT_PALETTE_SIZE = 4096;

    public static final String INTERPOLATIONTYPE_LINEAR = "linear";
    public static final String COLORSPACE_RGB = "RGB";

    PaletteGenerator paletteGenerator = new PaletteGenerator();

    List<PalettePoint> palettePoints = new ArrayList<>();

    Pixmap pixmap;

    String settingInterpolationType = INTERPOLATIONTYPE_LINEAR;
    String settingColorSpace = COLORSPACE_RGB;
    boolean settingAutoOffsets = true;
    double settingRandomColorSatMin = 0.7;
    double settingRandomColorSatMax = 1.0;
    double settingRandomColorValMin = 0.7;
    double settingRandomColorValMax = 1.0;

    public GradientPalette(String name){
        this(name, DEFAULT_PALETTE_SIZE);
    }

    public GradientPalette(String name, int width) {
        this(name, width, null);
//                new ArrayList<>(Arrays.asList(
//                        new PalettePoint(Color.BLACK, 0.0),
//                        new PalettePoint(Color.WHITE, 0.5))));
    }

    public GradientPalette(String name, int width, List<PalettePoint> palettePoints) {
        super(name);
        if (palettePoints != null)
            this.palettePoints = palettePoints;
        paletteGeneratorUpdate(width);
    }

    public void paletteGeneratorUpdate(int width) {
        paletteGenerator.setPaletteSize(width);
        paletteGeneratorUpdate();
    }

    public void paletteGeneratorUpdate() {
        paletteGenerator.setPalettePoints(palettePoints);
        pixmap = paletteGenerator.generatePixmap();
        setTexture(new Texture(pixmap));
    }

    public List<PalettePoint> getPalettePoints() {
        return palettePoints;
    }

    public void addPalettePoint(PalettePoint palettePoint){
        palettePoints.add(palettePoint);
        applyAutoOffsets();
        paletteGeneratorUpdate();
    }

    public void removeLastPalettePoint(){
        if (!palettePoints.isEmpty())
            removePalettePoint(palettePoints.get(palettePoints.size()-1));
    }

    public void removePalettePoint(PalettePoint palettePoint){
        palettePoints.remove(palettePoint);
        applyAutoOffsets();
        paletteGeneratorUpdate();
    }

    public void setPalettePoints(List<PalettePoint> palettePoints) {
        this.palettePoints = palettePoints;
        applyAutoOffsets();
        paletteGeneratorUpdate();
    }

    @Override
    public String getTypeName() {
        return TYPE_GRADIENT;
    }

    public String getSettingInterpolationType() {
        return settingInterpolationType;
    }

    public void setSettingInterpolationType(String settingInterpolationType) {
        this.settingInterpolationType = settingInterpolationType;
    }

    public String getSettingColorSpace() {
        return settingColorSpace;
    }

    public void setSettingColorSpace(String settingColorSpace) {
        this.settingColorSpace = settingColorSpace;
    }

    public double getSettingRandomColorSatMin() {
        return settingRandomColorSatMin;
    }

    public boolean getSettingAutoOffsets() {
        return settingAutoOffsets;
    }

    public void setSettingAutoOffsets(boolean settingAutoOffsets) {
        this.settingAutoOffsets = settingAutoOffsets;
        applyAutoOffsets();
    }

    protected void applyAutoOffsets() {
        if (settingAutoOffsets && !palettePoints.isEmpty()){
            double offset = 0;
            double step = 1d/palettePoints.size();
            for (int i = 0 ; i < palettePoints.size() ; i++){
                palettePoints.get(i).setRelativePos(offset);
                offset += step;
            }
        }
    }

    public void setSettingRandomColorSatMin(double settingRandomColorSatMin) {
        this.settingRandomColorSatMin = settingRandomColorSatMin;
    }

    public double getSettingRandomColorSatMax() {
        return settingRandomColorSatMax;
    }

    public void setSettingRandomColorSatMax(double settingRandomColorSatMax) {
        this.settingRandomColorSatMax = settingRandomColorSatMax;
    }

    public double getSettingRandomColorValMin() {
        return settingRandomColorValMin;
    }

    public void setSettingRandomColorValMin(double settingRandomColorValMin) {
        this.settingRandomColorValMin = settingRandomColorValMin;
    }

    public double getSettingRandomColorValMax() {
        return settingRandomColorValMax;
    }

    public void setSettingRandomColorValMax(double settingRandomColorValMax) {
        this.settingRandomColorValMax = settingRandomColorValMax;
    }
}
