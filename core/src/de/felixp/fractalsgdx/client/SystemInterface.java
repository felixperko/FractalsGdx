package de.felixp.fractalsgdx.client;

import com.badlogic.gdx.graphics.Pixmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.MainStage;
import de.felixperko.fractals.data.AbstractArrayChunk;
import de.felixperko.fractals.data.BorderAlignment;
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

    int chunkSize;

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
    public void chunkUpdated(CompressedChunk compressedChunk) {

        int chunkX = (int)(long)compressedChunk.getChunkX();
        int chunkY = (int)(long)compressedChunk.getChunkY();
        addCompressedChunk(compressedChunk, chunkX, chunkY);

        drawPixmap(compressedChunk);
    }

    private void drawPixmap(CompressedChunk compressedChunk) {
        int jobId = compressedChunk.getJobId();
        System.out.println("chunk update "+jobId+" (this:"+messageInterface.client.jobId+")");
        if (jobId != messageInterface.client.jobId)
            return;
        int upsample = compressedChunk.getUpsample();
        this.chunkSize = compressedChunk.getDimensionSize();

        AbstractArrayChunk chunk = compressedChunk.decompressPacked();
        Pixmap pixmap = new Pixmap(chunkSize/upsample, chunkSize/upsample, Pixmap.Format.RGBA8888);

        ComplexNumber chunkCoords = getScreenCoords(compressedChunk.getChunkPos());
        int chunkImgX = (int)Math.round(chunkCoords.getReal().toDouble());
        int chunkImgY = (int)Math.round(chunkCoords.getImag().toDouble());

        System.out.println(chunk.getChunkX()+" / "+chunk.getChunkY()+" -> "+chunkImgX+" / "+chunkImgY);
        boolean inside = false;
        com.badlogic.gdx.graphics.Color color = new com.badlogic.gdx.graphics.Color();

        int scaledChunkSize = chunkSize/upsample;

        for (int i = 0 ; i < chunk.getArrayLength()/chunk.getDownsampleIncrement() ; i++) {
            int x = (int) (i / (scaledChunkSize));
            int y = (int) (i % (scaledChunkSize));
            inside = true;
            float value = (float)chunk.getValue(i);


            BorderAlignment alignment = null;
            boolean horizontal = false;
            if (x < 0){
                alignment = BorderAlignment.LEFT;
                horizontal = true;
            } else if (x == scaledChunkSize-1){
                alignment = BorderAlignment.RIGHT;
                horizontal = true;
            }
            if (y < 0){
                alignment = BorderAlignment.UP;
            } else if (y == scaledChunkSize-1){
                alignment = BorderAlignment.DOWN;
            }

            /*
            if (alignment != null){
                if (chunk.getNeighbourBorderData(alignment).isSet(horizontal ? y : x))
                    value = 500;
                else
                    value = 5000;
            }
            */


            if (value < 0 && value != -2)
                value = -value;
            //if (value == -2)
            //    value = 2;
            if (value > 0) {
               getColor(color, value);
               pixmap.setColor(color);
               pixmap.drawPixel(x, y);
            }
        }
        if (inside)
            chunkCount--;

        ((MessageInterface)managers.getNetworkManager().getMessageInterface()).getFractalsGdxMain().drawPixmap(chunkImgX, chunkImgY, pixmap);
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

    public ComplexNumber getScreenCoords(ComplexNumber worldCoords){

        double height = parameters.get("height").getGeneral(Integer.class);
        Number zoom = parameters.get("zoom").getGeneral(Number.class);
        NumberFactory nf = parameters.get("numberFactory").getGeneral(NumberFactory.class);

        //((x-anchorX)*height/zoom, -((y-anchorY)*height/zoom + chunkSize)

        ComplexNumber pos = worldCoords.copy();
        pos.sub(FractalsGdxMain.client.getAnchor());
        pos.divNumber(zoom);
        pos.multNumber(nf.createNumber(height)); //TODO buffer wrapped values?

        Number screenY = pos.getImag();
        screenY.add(nf.createNumber(chunkSize));
        screenY.mult(nf.createNumber(-1));
        return nf.createComplexNumber(pos.getReal(), screenY);
    }

    public ComplexNumber getWorldCoords(ComplexNumber screenCoords){

        double width = parameters.get("width").getGeneral(Integer.class);
        double height = parameters.get("height").getGeneral(Integer.class);
        Number zoom = parameters.get("zoom").getGeneral(Number.class);
        NumberFactory nf = parameters.get("numberFactory").getGeneral(NumberFactory.class);

        Number screenY = screenCoords.getImag();
        screenY.mult(nf.createNumber(-1));
        //screenY.add(nf.createNumber(chunkSize));

        ComplexNumber worldCoords = nf.createComplexNumber(screenCoords.getReal(), screenY);
        worldCoords.divNumber(nf.createNumber(height));
        worldCoords.add(FractalsGdxMain.client.getAnchor());
        //worldCoords.multNumber(zoom);
        worldCoords.multNumber(nf.createNumber(chunkSize));
        return worldCoords;
    }

    public ComplexNumber getChunkGridCoords(ComplexNumber worldCoords){

        NumberFactory nf = parameters.get("numberFactory").getGeneral(NumberFactory.class);

        ComplexNumber gridPos = worldCoords.copy();
        gridPos.divNumber(nf.createNumber(chunkSize));
        gridPos.add(nf.createComplexNumber(0, 1));
        return gridPos;
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

    public ComplexNumber toComplex(double x, double y) {
        NumberFactory nf = parameters.get("numberFactory").getGeneral(NumberFactory.class);
        return getWorldCoords(nf.createComplexNumber(x, y));
    }
}
