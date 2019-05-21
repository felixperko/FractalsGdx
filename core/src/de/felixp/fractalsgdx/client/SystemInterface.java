package de.felixp.fractalsgdx.client;

import com.badlogic.gdx.graphics.Pixmap;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.MainStage;
import de.felixperko.fractals.data.AbstractArrayChunk;
import de.felixperko.fractals.data.CompressedChunk;
import de.felixperko.fractals.manager.client.ClientManagers;
import de.felixperko.fractals.network.ClientConfiguration;
import de.felixperko.fractals.network.ClientSystemInterface;
import de.felixperko.fractals.system.Numbers.infra.ComplexNumber;
import de.felixperko.fractals.system.Numbers.infra.Number;
import de.felixperko.fractals.system.Numbers.infra.NumberFactory;
import de.felixperko.fractals.system.parameters.ParameterConfiguration;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;

public class SystemInterface implements ClientSystemInterface {

//    BufferedImage image = null;
    int imgWidth;
    int imgHeight;

    int chunkCount = 0;

    Map<String, ParamSupplier> parameters = new HashMap<>();

    Map<Integer, Map<Integer, CompressedChunk>> chunks_compressed = new HashMap<>();

    ClientManagers managers;

    MessageInterface messageInterface;

    UUID systemId;

    public SystemInterface(UUID systemId, MessageInterface messageInterface, ClientManagers managers){
        this.systemId = systemId;
        this.managers = managers;
        this.messageInterface = messageInterface;

    }

    public void setParameters(Map<String, ParamSupplier> parameters) {
        int newImgWidth = parameters.get("width").getGeneral(Integer.class);
        int newImgHeight = parameters.get("height").getGeneral(Integer.class);

        if (/*image == null || */newImgWidth != imgWidth || newImgHeight != imgHeight) {
            imgWidth = newImgWidth;
            imgHeight = newImgHeight;
//            image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        }
        this.parameters = parameters;
    }

    ParameterConfiguration parameterConfiguration;

    @Override
    public void updateParameterConfiguration(ClientConfiguration clientConfiguration, ParameterConfiguration parameterConfiguration) {
        if (parameterConfiguration != null)
            this.parameterConfiguration = parameterConfiguration;
        if (this.parameterConfiguration != null)
            ((MainStage)FractalsGdxMain.stage).setParameterConfiguration(clientConfiguration.getSystemClientData(systemId), this.parameterConfiguration);
    }

    @Override
    public void chunkUpdated(AbstractArrayChunk chunk) {

        int jobId = chunk.getJobId();
        System.out.println("chunk update "+jobId+" (this:"+messageInterface.client.jobId+")");
        if (jobId != messageInterface.client.jobId)
            return;

        int upsample = chunk.getDownsample();
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

        float subY = 0.0f;
        if (height > 1024)
            subY += 0.5f;
        float subX = 0.5f;
        if (width > 1023)
            subX += 0.5f;

        int chunkImgX = (int)((chunkX-subX)*chunkSize);
        int chunkImgY = (int)((-chunkY-subY)*chunkSize);
//        int chunkImgX = chunk X*chunkSize;
//        int chunkImgY = chunkY*chunkSize;
        Pixmap pixmap = new Pixmap(chunkSize/upsample, chunkSize/upsample, Pixmap.Format.RGBA8888);

        System.out.println(chunk.getChunkX()+" / "+chunk.getChunkY()+" -> "+chunkImgX+" / "+chunkImgY);
        boolean inside = false;
        com.badlogic.gdx.graphics.Color color = new com.badlogic.gdx.graphics.Color();
        for (int i = 0 ; i < chunk.getArrayLength()/chunk.getDownsampleIncrement() ; i++) {
            int x = (int) (i / (chunkSize/upsample));
            int y = (int) (i % (chunkSize/upsample));
//            if (x >= image.getWidth() || x < 0 || y >= image.getHeight() || y < 0)
//                continue;
            inside = true;
            float value = (float)chunk.getValue(i);
//            if (x == 0 || y == 0)
//                value = 1;
            if (value == -2)
                value = 2;
            if (value > 0) {
//				float hue = (float)Math.log(Math.log(value+1)+1);
               getColor(color, value);
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

    protected void getColor(com.badlogic.gdx.graphics.Color color, float value) {
//        float hue = (float)Math.log(value+1)*360;
        float hue = value;
        color.fromHsv(hue, 1, 1);
    }

    public void addChunkCount(int count) {
        chunkCount += count;
    }

    @Override
    public void chunksCleared() {
//        image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
    }

    public CompressedChunk getCompressedChunk(int x, int y){
        Map<Integer, CompressedChunk> yMap = getCompressedChunkYMap(x);
        return yMap.get(y);
    }

    public void addCompressedChunk(CompressedChunk chunk, int x, int y){
        Map<Integer, CompressedChunk> yMap = getCompressedChunkYMap(x);
        yMap.put(y, chunk);
    }

    public void removeCompressedChunk(int x, int y){
        Map<Integer, CompressedChunk> yMap = getCompressedChunkYMap(x);
        yMap.remove(y);
    }

    private Map<Integer, CompressedChunk> getCompressedChunkYMap(int x){
        Map<Integer, CompressedChunk> map = chunks_compressed.get(x);
        if (map == null){
            map = new HashMap<>();
            chunks_compressed.put(x, map);
        }
        return map;
    }

}
