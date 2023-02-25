package de.felixp.fractalsgdx.rendering.palette;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.FloatArray;
import com.github.czyzby.kiwi.util.tuple.immutable.Pair;
import com.github.tommyettinger.colorful.oklab.ColorTools;
import com.github.tommyettinger.colorful.oklab.GradientTools;
import com.github.tommyettinger.colorful.oklab.SimplePalette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaletteGenerator {

    int paletteSize = GradientPalette.DEFAULT_PALETTE_SIZE;

    List<PalettePoint> palettePoints = new ArrayList<>();
    String colorSpace;
    String interpolationType;

    public PaletteGenerator() {
    }

    public void setParams(List<PalettePoint> points, String settingColorSpace, String settingInterpolation){
        this.palettePoints = points;
        this.colorSpace = settingColorSpace;
        this.interpolationType = settingInterpolation;
    }

    public Pixmap generatePixmap(){
        Pixmap pixmap = new Pixmap(paletteSize, 1, Pixmap.Format.RGBA8888);

        List<Pair<PalettePoint, Float>> pointPositions = new ArrayList<>();
        if (palettePoints.isEmpty()) {
            pixmap.setColor(Color.BLACK);
            pixmap.fill();
            return pixmap;
        }
        List<PalettePoint> sorted = new ArrayList<>(palettePoints);
        Collections.sort(sorted);
        for (PalettePoint point : sorted){
            pointPositions.add(new Pair<PalettePoint, Float>(point, (float) (point.getRelativePos()*paletteSize)));
        }

        //correct for wrapping
        int firstIndex = 0;
        if (sorted.get(0).getRelativePos() != 0f){
            //add last as first
            firstIndex = 1;
            Pair<PalettePoint, Float> last = pointPositions.get(pointPositions.size()-1);
            float wrappedValue = last.getValue() - paletteSize;
            pointPositions.add(0, new Pair<PalettePoint, Float>(last.getKey(), wrappedValue));
        }
        if (sorted.get(sorted.size()-1).getRelativePos() != 1f){
            //add first as last
            Pair<PalettePoint, Float> first = pointPositions.get(firstIndex);
            float wrappedValue = first.getValue() + paletteSize;
            pointPositions.add( new Pair<PalettePoint, Float>(first.getKey(), wrappedValue));
        }

        int currentLeftIndex = 0;
        int currentRightIndex = 1;

        FloatArray gradientArray = null;
        int gradientUpdateIndex = 0;


        Pair<PalettePoint, Float> leftPair1 = pointPositions.get(currentLeftIndex);
        Pair<PalettePoint, Float> rightPair1 = pointPositions.get(currentRightIndex);
        Color color3 = leftPair1.getKey().getColor().cpy();
        Color color4 = rightPair1.getKey().getColor();
        float oklab3 = ColorTools.fromRGBA(color3.r, color3.g, color3.b, color3.a);
        float oklab4 = ColorTools.fromRGBA(color4.r, color4.g, color4.b, color4.a);
        gradientArray = GradientTools.makeGradient(oklab3, oklab4, (int)Math.ceil(rightPair1.getValue()-leftPair1.getValue()));
        gradientUpdateIndex = Math.round(rightPair1.getValue());

        Interpolation interpolation = Interpolation.linear;
        if (interpolationType.equals(GradientPalette.INTERPOLATIONTYPE_QUADRATIC))
            interpolation = Interpolation.pow2;

        for (int x = 0 ; x < paletteSize ; x++) {
            Pair<PalettePoint, Float> leftPair = pointPositions.get(currentLeftIndex);
            Pair<PalettePoint, Float> rightPair = pointPositions.get(currentRightIndex);
            float leftPos = leftPair.getValue();
            float rightPos = rightPair.getValue();
            int newSteps = 0;

            while ((x > rightPos && currentRightIndex != 0)) {
                currentLeftIndex++;
                currentRightIndex++;
                if (currentLeftIndex > pointPositions.size() - 1)
                    currentLeftIndex = 0;
                else if (currentRightIndex > pointPositions.size() - 1)
                    currentRightIndex = 0;
                leftPair = pointPositions.get(currentLeftIndex);
                rightPair = pointPositions.get(currentRightIndex);
                leftPos = leftPair.getValue();
                rightPos = rightPair.getValue();
                newSteps++;
            }

            Color color1 = leftPair.getKey().getColor().cpy();
            Color color2 = rightPair.getKey().getColor();
            if (colorSpace.equals(GradientPalette.COLORSPACE_OKLAB) && (newSteps > 0 || x == paletteSize-1)) {
                newSteps = (int)Math.ceil(rightPos-gradientUpdateIndex);
                gradientUpdateIndex = Math.round(rightPos);
                float oklab1 = ColorTools.fromRGBA(color1.r, color1.g, color1.b, color1.a);
                float oklab2 = ColorTools.fromRGBA(color2.r, color2.g, color2.b, color2.a);
                System.out.println("Adding steps: "+newSteps+"/"+paletteSize);
                if (gradientArray == null) {
                    gradientArray = GradientTools.makeGradient(oklab1, oklab2, newSteps);
                } else {
                    if (currentRightIndex > 0) {
                        gradientArray = GradientTools.appendPartialGradient(gradientArray, oklab1, oklab2, newSteps, interpolation);
                    } else {
                        gradientArray = GradientTools.appendGradient(gradientArray, oklab1, oklab2, newSteps, interpolation);
                    }
                }
            }

            float prog = Math.abs((x - leftPos) / (rightPos - leftPos));

            if (!colorSpace.equals(GradientPalette.COLORSPACE_OKLAB)) {
                Color color = color1.lerp(color2, prog);
                pixmap.setColor(color);
                pixmap.drawPixel(x, 0);
            }
        }

        if (colorSpace.equals(GradientPalette.COLORSPACE_OKLAB)){
            for (int x = 0 ; x < paletteSize ; x++) {
                Color color = ColorTools.toColor(new Color(), gradientArray.get(x));
                pixmap.setColor(color);
                pixmap.drawPixel(x, 0);
            }
        }

        return pixmap;
    }

    public void setPaletteSize(int paletteSize){
        this.paletteSize = paletteSize;
    }
}
