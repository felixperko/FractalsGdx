package de.felixp.fractalsgdx.client;

import com.badlogic.gdx.graphics.Pixmap;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import de.felixperko.fractals.data.AbstractArrayChunk;
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

    MessageInterface messageInterface;

    public SystemInterface(MessageInterface messageInterface, ClientManagers managers){
        this.managers = managers;
        this.messageInterface = messageInterface;

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
    public void chunkUpdated(AbstractArrayChunk chunk) {

        int jobId = chunk.getJobId();
        if (jobId != messageInterface.client.jobId)
            return;
        int chunkSize = chunk.getChunkDimensions();
        int width = parameters.get("width").getGeneral(Integer.class);
        double height = parameters.get("height").getGeneral(Integer.class);
        Number zoom = parameters.get("zoom").getGeneral(Number.class);
        ComplexNumber midpoint = parameters.get("midpoint").getGeneral(ComplexNumber.class);
        NumberFactory nf = parameters.get("numberFactory").getGeneral(NumberFactory.class);

//        ComplexNumber shift = chunk.chunkPos.copy();
//        shift.sub(midpoint);
//        Number zoomY = zoom.copy();
////		zoomY.mult(nf.createNumber(height/width));
//        ComplexNumber internalShift = nf.createComplexNumber(zoom, zoomY);
//        internalShift.multNumber(nf.createNumber(0.5));
//        shift.add(internalShift);
//        shift.divNumber(zoom);
//        double shiftX = (((width/(double)height)-1)*0.5);
//        double relX = (shift.getReal().toDouble() + shiftX) * (height/width);
//        double relY = shift.getImag().toDouble();
//        double testShrinkForHeightGreaterThanWidth = (height/width) > 1 ? (height/width) : 1;
//        int chunkImgX = (int)Math.round(relX*width/testShrinkForHeightGreaterThanWidth - FractalsGdxMain.xPos);
//        int chunkImgY = (int)Math.round(relY*height/testShrinkForHeightGreaterThanWidth - FractalsGdxMain.yPos);

        int chunkX = (int)(long)chunk.getChunkX();
        int chunkY = (int)(long)chunk.getChunkY();

        int chunkImgX = (int)((chunkX-0.5)*chunkSize + width/2.);
        int chunkImgY = (int)((chunkY-1)*chunkSize + (int)height/2.);
//        int chunkImgX = chunk X*chunkSize;
//        int chunkImgY = chunkY*chunkSize;
        Pixmap pixmap = new Pixmap(width, (int)height, Pixmap.Format.RGBA8888);

        System.out.println(chunk.getChunkX()+" / "+chunk.getChunkY()+" -> "+chunkImgX+" / "+chunkImgY);
        boolean inside = false;
        for (int i = 0 ; i < chunk.getArrayLength() ; i++) {
            int x = (int) (i / chunkSize);
            int y = (int) (i % chunkSize);
//            if (x >= image.getWidth() || x < 0 || y >= image.getHeight() || y < 0)
//                continue;
            inside = true;
            float value = (float)chunk.getValue(i);
            if (value > 0) {
//				float hue = (float)Math.log(Math.log(value+1)+1);
                com.badlogic.gdx.graphics.Color color = getColor(value);
                //int color = Color.HSBtoRGB(hue, 1f, 1f);
//                pixmap.setColor();
                pixmap.setColor(color);
                pixmap.drawPixel(x, y);
//				if (y == 501)
//					System.out.println(x+", "+y+": "+value);
            }
        }
        if (inside)
            chunkCount--;

        ((MessageInterface)managers.getNetworkManager().getMessageInterface()).getFractalsGdxMain().drawPixmap(chunkImgX, chunkImgY, pixmap);
//		if (chunkCount == 0) {
//        if (MessageInterface.TEST_FINISH) {
//            try {
//				long endTime = System.nanoTime();
//				System.out.println("calculated in "+NumberUtil.getElapsedTimeInS(startTime, 2)+"s.");
//                ImageIO.write(image, "png", new File("test.png"));
//                System.out.println("done!");
//                managers.getNetworkManager().closeServerConnection();
//                try {
//                    Thread.sleep(1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.exit(0);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    protected com.badlogic.gdx.graphics.Color getColor(float value) {
        float hue = (float)Math.log(value+1)*360;
        com.badlogic.gdx.graphics.Color color = new com.badlogic.gdx.graphics.Color(0,0,0,1);
        color.fromHsv(hue, 1, 1);
        return color;
    }

    public void addChunkCount(int count) {
        chunkCount += count;
    }

    @Override
    public void chunksCleared() {
//        image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
    }

}
