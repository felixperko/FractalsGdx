package de.felixp.fractalsgdx.rendering.palette;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.github.czyzby.kiwi.util.tuple.immutable.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaletteGenerator {

    int paletteSize = GradientPalette.DEFAULT_PALETTE_SIZE;

    List<PalettePoint> palettePoints = new ArrayList<>();

    public PaletteGenerator() {
    }

    public void setPalettePoints(List<PalettePoint> points){
        this.palettePoints = points;
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

        int lastIndex = pointPositions.size()-1;
        int currentLeftIndex = 0;
        int currentRightIndex = 1;
        if (pointPositions.get(0).getValue() != 0f){
            currentLeftIndex = lastIndex;
            currentRightIndex = 0;
        }
        for (int x = 0 ; x < paletteSize ; x++){
            Pair<PalettePoint, Float> leftPair = pointPositions.get(currentLeftIndex);
            Pair<PalettePoint, Float> rightPair = pointPositions.get(currentRightIndex);
            float leftPos = leftPair.getValue();
            float rightPos = rightPair.getValue();

            while ((x > rightPos && currentRightIndex != 0)){
                currentLeftIndex++;
                currentRightIndex++;
                if (currentLeftIndex > palettePoints.size()-1)
                    currentLeftIndex = 0;
                else if (currentRightIndex > palettePoints.size()-1)
                    currentRightIndex = 0;
                leftPair = pointPositions.get(currentLeftIndex);
                rightPair = pointPositions.get(currentRightIndex);
                leftPos = leftPair.getValue();
                rightPos = rightPair.getValue();
            }

            if (leftPos > rightPos){
                rightPos += paletteSize;
            }
            else if (rightPos-leftPos > paletteSize/2)
                rightPos -= paletteSize;


//            if (currentLeftIndex == lastIndex && currentRightIndex == 0){
//                rightPos -= paletteSize;
//            }

            float prog = Math.abs((x - leftPos) / (rightPos-leftPos));
            Color color = leftPair.getKey().getColor().cpy().lerp(rightPair.getKey().getColor(), prog);
            pixmap.setColor(color);
            pixmap.drawPixel(x, 0);
        }
        return pixmap;
    }

    public void setPaletteSize(int paletteSize){
        this.paletteSize = paletteSize;
    }

    public List<PalettePoint> getPalettePoints() {
        return palettePoints;
    }
}
