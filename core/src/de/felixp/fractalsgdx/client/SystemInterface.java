package de.felixp.fractalsgdx.client;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import de.felixperko.fractals.data.Chunk;
import de.felixperko.fractals.manager.client.ClientManagers;
import de.felixperko.fractals.network.ClientSystemInterface;
import de.felixperko.fractals.system.Numbers.infra.ComplexNumber;
import de.felixperko.fractals.system.Numbers.infra.Number;
import de.felixperko.fractals.system.Numbers.infra.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamSupplier;

public class SystemInterface implements ClientSystemInterface {

    BufferedImage image = null;
    int imgWidth;
    int imgHeight;

    int chunkCount = 0;

    Map<String, ParamSupplier> parameters = new HashMap<>();

    ClientManagers managers;

    public SystemInterface(ClientManagers managers){
        this.managers = managers;
    }

    public void setParameters(Map<String, ParamSupplier> parameters) {
        int newImgWidth = parameters.get("width").getGeneral(Integer.class);
        int newImgHeight = parameters.get("height").getGeneral(Integer.class);

        if (image == null || newImgWidth != imgWidth || newImgHeight != imgHeight) {
            imgWidth = newImgWidth;
            imgHeight = newImgHeight;
            image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        }
        this.parameters = parameters;
    }

    @Override
    public void chunkUpdated(Chunk chunk) {
        int chunkSize = chunk.getChunkSize();
        int width = parameters.get("width").getGeneral(Integer.class);
        double height = parameters.get("height").getGeneral(Integer.class);
        Number zoom = parameters.get("zoom").getGeneral(Number.class);
        ComplexNumber midpoint = parameters.get("midpoint").getGeneral(ComplexNumber.class);
        NumberFactory nf = parameters.get("numberFactory").getGeneral(NumberFactory.class);

        ComplexNumber shift = chunk.chunkPos.copy();
        shift.sub(midpoint);
        Number zoomY = zoom.copy();
//		zoomY.mult(nf.createNumber(height/width));
        ComplexNumber internalShift = nf.createComplexNumber(zoom, zoomY);
        internalShift.multNumber(nf.createNumber(0.5));
        shift.add(internalShift);
        shift.divNumber(zoom);
        double shiftX = (((width/(double)height)-1)*0.5);
        double relX = (shift.getReal().toDouble() + shiftX) * (height/width);
        double relY = shift.getImag().toDouble();
        double testShrinkForHeightGreaterThanWidth = (height/width) > 1 ? (height/width) : 1;
        int chunkImgX = (int)Math.round(relX*width/testShrinkForHeightGreaterThanWidth);
        int chunkImgY = (int)Math.round(relY*height/testShrinkForHeightGreaterThanWidth);

        System.out.println(chunk.getChunkX()+" / "+chunk.getChunkY()+" -> "+chunkImgX+" / "+chunkImgY);
        boolean inside = false;
        for (int i = 0 ; i < chunk.getArrayLength() ; i++) {
            int x = (int) (i / chunkSize + chunkImgX);
            int y = (int) (i % chunkSize + chunkImgY);
            if (x >= image.getWidth() || x < 0 || y >= image.getHeight() || y < 0)
                continue;
            inside = true;
            double value = chunk.getValue(i);
            if (value > 0) {
//				float hue = (float)Math.log(Math.log(value+1)+1);
                float hue = (float)Math.log(value+1);
                int color = Color.HSBtoRGB(hue, 1f, 1f);
                image.setRGB(x, y, color);
//				if (y == 501)
//					System.out.println(x+", "+y+": "+value);
            }
        }
        if (inside)
            chunkCount--;
//		if (chunkCount == 0) {
        if (MessageInterface.TEST_FINISH) {
            try {
//				long endTime = System.nanoTime();
//				System.out.println("calculated in "+NumberUtil.getElapsedTimeInS(startTime, 2)+"s.");
                ImageIO.write(image, "png", new File("test.png"));
                System.out.println("done!");
                managers.getNetworkManager().closeServerConnection();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addChunkCount(int count) {
        chunkCount += count;
    }

    @Override
    public void chunksCleared() {
        image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
    }

}
