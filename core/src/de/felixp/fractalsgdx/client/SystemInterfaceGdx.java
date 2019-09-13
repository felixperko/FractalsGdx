package de.felixp.fractalsgdx.client;

import com.badlogic.gdx.graphics.Pixmap;

import java.util.Map;
import java.util.UUID;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.MainStage;
import de.felixp.fractalsgdx.RemoteRenderer;
import de.felixperko.fractals.data.AbstractArrayChunk;
import de.felixperko.fractals.data.CompressedChunk;
import de.felixperko.fractals.manager.client.ClientManagers;
import de.felixperko.fractals.network.ClientConfiguration;
import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.network.interfaces.ClientSystemInterface;
import de.felixperko.fractals.system.Numbers.infra.ComplexNumber;
import de.felixperko.fractals.system.Numbers.infra.Number;
import de.felixperko.fractals.system.Numbers.infra.NumberFactory;
import de.felixperko.fractals.system.parameters.ParameterConfiguration;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.util.CategoryLogger;
import de.felixperko.fractals.util.ColorContainer;

public class SystemInterfaceGdx implements ClientSystemInterface {

    ClientManagers managers;
    static CategoryLogger log = new CategoryLogger("systemInterface", new ColorContainer(1f,0.5f,0.5f));

    ClientSystem clientSystem;

//    BufferedImage image = null;
    int imgWidth;
    int imgHeight;

    int chunkCount = 0;

    RemoteRenderer renderer;

    ChunkData chunkData = new ChunkData();


    MessageInterfaceGdx messageInterface;

    UUID systemId;

    ComplexNumber currentMidpoint;

    public SystemInterfaceGdx(UUID systemId, MessageInterfaceGdx messageInterface, ClientManagers managers){
        this.systemId = systemId;
        this.managers = managers;
        this.messageInterface = messageInterface;
        this.renderer = ((MainStage)FractalsGdxMain.stage).getRenderer();
    }

    public void setSystemClientData(SystemClientData systemClientData) {
        int newImgWidth = systemClientData.getClientParameter("width").getGeneral(Integer.class);
        int newImgHeight = systemClientData.getClientParameter("height").getGeneral(Integer.class);

        if (/*image == null || */newImgWidth != imgWidth || newImgHeight != imgHeight) {
            imgWidth = newImgWidth;
            imgHeight = newImgHeight;
//            image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        }
        chunkData.clientData = systemClientData;
        renderer.setSystemInterface(this);
    }

    public SystemClientData getSystemClientData(){
        return chunkData.clientData;
    }

    ParameterConfiguration parameterConfiguration;

    @Override
    public void updateParameterConfiguration(SystemClientData systemClientData, ParameterConfiguration parameterConfiguration) {
        if (parameterConfiguration != null)
            this.parameterConfiguration = parameterConfiguration;
        if (this.parameterConfiguration != null)
            ((MainStage)FractalsGdxMain.stage).setParameterConfiguration(systemClientData, this.parameterConfiguration);
    }

    @Override
    public void chunkUpdated(CompressedChunk compressedChunk) {

        int chunkX = (int)(long)compressedChunk.getChunkX();
        int chunkY = (int)(long)compressedChunk.getChunkY();
        chunkData.addCompressedChunk(compressedChunk, chunkX, chunkY);

        drawPixmap(compressedChunk);
    }

    private void drawPixmap(CompressedChunk compressedChunk) {
        int jobId = compressedChunk.getJobId();
        System.out.println("chunk update "+jobId+" (this:"+clientSystem.jobId+")");
        if (jobId != clientSystem.jobId)
            return;
        int upsample = compressedChunk.getUpsample();
        chunkData.chunkSize = compressedChunk.getDimensionSize();

        AbstractArrayChunk chunk = compressedChunk.decompressPacked();
        Pixmap pixmap = new Pixmap(chunkData.chunkSize/upsample, chunkData.chunkSize/upsample, Pixmap.Format.RGBA8888);

        ComplexNumber chunkCoords = chunkData.getScreenCoords(compressedChunk.getChunkPos(), clientSystem);
        int chunkImgX = (int)Math.round(chunkCoords.getReal().toDouble());
        int chunkImgY = (int)Math.round(chunkCoords.getImag().toDouble());

        System.out.println(chunk.getChunkX()+" / "+chunk.getChunkY()+" -> "+chunkImgX+" / "+chunkImgY);
        boolean inside = false;
        com.badlogic.gdx.graphics.Color color = new com.badlogic.gdx.graphics.Color();

        int scaledChunkSize = chunkData.chunkSize/upsample;

        for (int i = 0 ; i < chunk.getArrayLength()/(upsample*upsample) ; i++) {
            int x = (int) (i / (scaledChunkSize));
            int y = (int) (i % (scaledChunkSize));
            float value = (float)chunk.getValue(i);
            inside = true;
            /*


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

            if (alignment != null){
                if (chunk.getNeighbourBorderData(alignment).isSet(horizontal ? y : x))
                    value = 500;
                else
                    value = 5000;
            }
            */


            if (value < 0 && value != -2)
                value = -value;
//            if (value == -2)
//                value = 2;
            if (value > 0) {
               getColor(color, value);
               pixmap.setColor(color);
               pixmap.drawPixel(x, y);
            }
        }
        if (inside)
            chunkCount--;

        messageInterface.getFractalsGdxMain().drawPixmap(chunkImgX, chunkImgY, pixmap);
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

    @Override
    public ParameterConfiguration getParamConfiguration() {
        return parameterConfiguration;
    }

    public ComplexNumber getWorldCoords(ComplexNumber screenCoords){

        Map<String, ParamSupplier> parameters = chunkData.clientData.getClientParameters();
        NumberFactory nf = parameters.get("numberFactory").getGeneral(NumberFactory.class);
        double width = parameters.get("width").getGeneral(Integer.class);
        double height = parameters.get("height").getGeneral(Integer.class);
        Number zoom = parameters.get("zoom").getGeneral(Number.class);
        ComplexNumber midpoint = parameters.get("midpoint").getGeneral(ComplexNumber.class);

        ComplexNumber worldCoords = screenCoords.copy();
        worldCoords.sub(nf.createComplexNumber(nf.createNumber(width/2), nf.createNumber(height/2)));
        worldCoords.divNumber(nf.createNumber(height));
        worldCoords.multNumber(zoom);
        worldCoords.add(midpoint);
        return worldCoords;
    }

    public ComplexNumber getChunkGridCoords(ComplexNumber worldCoords){

        Map<String, ParamSupplier> parameters = chunkData.clientData.getClientParameters();
        NumberFactory nf = parameters.get("numberFactory").getGeneral(NumberFactory.class);
        int width = parameters.get("width").getGeneral(Integer.class);
        int height = parameters.get("height").getGeneral(Integer.class);
        Number zoom = parameters.get("zoom").getGeneral(Number.class);
        //ComplexNumber anchor = getWorldCoords(nf.createComplexNumber(renderer.getXShift(), renderer.getYShift()));
        ComplexNumber anchor = clientSystem.anchor;

        ComplexNumber gridPos = worldCoords.copy();
        gridPos.sub(anchor);
        gridPos.divNumber(zoom);
//        ComplexNumber shift = nf.createComplexNumber(width/height/2f, 1/2f);
//        gridPos.sub(shift);
        gridPos.multNumber(nf.createNumber(chunkData.chunkSize));
        gridPos.divNumber(nf.createNumber(30.4));
        //gridPos.divNumber(nf.createNumber(64*4));
        //gridPos.add(nf.createComplexNumber(0, 1));
        return gridPos;
    }

    public ComplexNumber getCurrentMidpoint(){
        return currentMidpoint;
    }

    public void setCurrentMidpoint(ComplexNumber currentMidpoint) {
        this.currentMidpoint = currentMidpoint;
    }

    public ComplexNumber toComplex(double x, double y) {
        Map<String, ParamSupplier> parameters = chunkData.clientData.getClientParameters();
        NumberFactory nf = parameters.get("numberFactory").getGeneral(NumberFactory.class);
        return nf.createComplexNumber(x, y);
    }

    public ChunkData getChunkData(){
        return chunkData;
    }

    public ClientSystem getClientSystem(){
        return clientSystem;
    }

    public void setClientSystem(ClientSystem clientSystem){
        this.clientSystem = clientSystem;
    }
}
