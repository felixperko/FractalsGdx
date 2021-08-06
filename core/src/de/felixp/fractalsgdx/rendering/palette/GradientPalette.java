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

    PaletteGenerator paletteGenerator = new PaletteGenerator();

    List<PalettePoint> palettePoints = new ArrayList<>();

    Pixmap pixmap;

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
        paletteGeneratorUpdate();
    }

    public void removeLastPalettePoint(){
        if (!palettePoints.isEmpty())
            removePalettePoint(palettePoints.get(palettePoints.size()-1));
    }

    public void removePalettePoint(PalettePoint palettePoint){
        palettePoints.remove(palettePoint);
        paletteGeneratorUpdate();
    }

    public void setPalettePoints(List<PalettePoint> palettePoints) {
        this.palettePoints = palettePoints;
        paletteGeneratorUpdate();
    }

    @Override
    public String getTypeName() {
        return TYPE_GRADIENT;
    }
}
