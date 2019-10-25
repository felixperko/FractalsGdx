package de.felixp.fractalsgdx.client;

import com.badlogic.gdx.graphics.Pixmap;

import java.util.Map;
import java.util.UUID;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.MainStage;
import de.felixp.fractalsgdx.RemoteRenderer;
import de.felixperko.fractals.data.AbstractArrayChunk;
import de.felixperko.fractals.data.BorderAlignment;
import de.felixperko.fractals.data.CompressedChunk;
import de.felixperko.fractals.manager.client.ClientManagers;
import de.felixperko.fractals.network.ClientConfiguration;
import de.felixperko.fractals.network.ParamContainer;
import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.network.interfaces.ClientSystemInterface;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ParameterConfiguration;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BFSystemContext;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.util.CategoryLogger;
import de.felixperko.fractals.util.ColorContainer;

public class SystemInterfaceGdx implements ClientSystemInterface {

    ClientManagers managers;
    static CategoryLogger log = new CategoryLogger("systemInterface", new ColorContainer(1f,0.5f,0.5f));

    ClientSystem clientSystem;
    BFSystemContext systemContext;

//    BufferedImage image = null;
    int imgWidth;
    int imgHeight;

    int chunkCount = 0;

    RemoteRenderer renderer;

    //ChunkData chunkData = new ChunkData();


    MessageInterfaceGdx messageInterface;

    UUID systemId;

    ComplexNumber currentMidpoint;

    public SystemInterfaceGdx(UUID systemId, MessageInterfaceGdx messageInterface, ClientManagers managers){
        this.systemId = systemId;
        this.managers = managers;
        this.messageInterface = messageInterface;
        this.renderer = ((MainStage)FractalsGdxMain.stage).getRenderer();
    }

    @Override
    public UUID getSystemId(){
        return systemId;
    }

    public void setParamContainer(ParamContainer systemClientData) {
        int newImgWidth = systemClientData.getClientParameter("width").getGeneral(Integer.class);
        int newImgHeight = systemClientData.getClientParameter("height").getGeneral(Integer.class);

        if (/*image == null || */newImgWidth != imgWidth || newImgHeight != imgHeight) {
            imgWidth = newImgWidth;
            imgHeight = newImgHeight;
//            image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        }
        //chunkData.clientData = systemClientData;
        renderer.setSystemInterface(this);
    }

    public ParamContainer getParamContainer(){
        return systemContext.getParamContainer();
    }

    ParameterConfiguration parameterConfiguration;

    @Override
    public void updateParameterConfiguration(ParamContainer systemClientData, ParameterConfiguration parameterConfiguration) {
        if (parameterConfiguration != null)
            this.parameterConfiguration = parameterConfiguration;
        if (this.parameterConfiguration != null)
            ((MainStage)FractalsGdxMain.stage).setServerParameterConfiguration(systemClientData, this.parameterConfiguration);
    }

    @Override
    public void chunkUpdated(CompressedChunk compressedChunk) {

        int chunkX = (int)(long)compressedChunk.getChunkX();
        int chunkY = (int)(long)compressedChunk.getChunkY();
        //chunkData.addCompressedChunk(compressedChunk, chunkX, chunkY);

        drawPixmap(compressedChunk);
    }

    private void drawPixmap(CompressedChunk compressedChunk) {
        int jobId = compressedChunk.getJobId();
        System.out.println("chunk update "+jobId+" (this:"+clientSystem.systemContext.getViewId()+")");
        if (jobId != clientSystem.systemContext.getViewId())
            return;
        int upsample = compressedChunk.getUpsample();
        int chunkSize = systemContext.getChunkSize();
        //chunkData.chunkSize = compressedChunk.getDimensionSize();

        AbstractArrayChunk chunk = compressedChunk.decompressPacked();
        Pixmap pixmap = new Pixmap(chunkSize/upsample, chunkSize/upsample, Pixmap.Format.RGBA8888);

        ComplexNumber chunkCoords = getScreenCoords(compressedChunk.getChunkPos(), clientSystem);
        int chunkImgX = (int)Math.round(chunkCoords.getReal().toDouble());
        int chunkImgY = (int)Math.round(chunkCoords.getImag().toDouble());

        System.out.println(chunk.getChunkX()+" / "+chunk.getChunkY()+" -> "+chunkImgX+" / "+chunkImgY);
        boolean inside = false;
        com.badlogic.gdx.graphics.Color color = new com.badlogic.gdx.graphics.Color();

        int scaledChunkSize = chunkSize/upsample;

        for (int i = 0 ; i < chunk.getArrayLength()/(upsample*upsample) ; i++) {
            int x = (int) (i / (scaledChunkSize));
            int y = (int) (i % (scaledChunkSize));
            float value = (float)chunk.getValue(i);
            inside = true;



//            BorderAlignment alignment = null;
//            boolean horizontal = false;
//            if (x == 0){
//                alignment = BorderAlignment.LEFT;
//                horizontal = true;
//            }
//            else
//            if (x == scaledChunkSize-1){
//                alignment = BorderAlignment.RIGHT;
//                horizontal = true;
//            }
//            if (y == 0){
//                alignment = BorderAlignment.UP;
//            }
//            else
//            if (y == scaledChunkSize-1){
//                alignment = BorderAlignment.DOWN;
//            }
//
//            if (alignment != null){
//                if (chunk.getNeighbourBorderData() != null && chunk.getNeighbourBorderData().get(alignment).isSet(horizontal ? y*upsample : x*upsample))
//                    value = 100;
//                else
//                    value = 10000;
//            }



//            if (value < 0 && value != -2)
//                value = -value;

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

    @Deprecated
    public ComplexNumber getWorldCoords(ComplexNumber screenCoords){

        NumberFactory nf = systemContext.getNumberFactory();
        int width = systemContext.getParamValue("width", Integer.class);
        int height = systemContext.getParamValue("height", Integer.class);
        Number zoom = systemContext.getZoom();
        ComplexNumber midpoint = systemContext.getMidpoint();

        ComplexNumber worldCoords = screenCoords.copy();
        worldCoords.sub(nf.createComplexNumber(nf.createNumber(width/2), nf.createNumber(height/2)));
        worldCoords.divNumber(nf.createNumber(height));
        worldCoords.multNumber(zoom);
        worldCoords.add(midpoint);
        return worldCoords;
    }

    @Deprecated
    public ComplexNumber getChunkGridCoords(ComplexNumber worldCoords){

        NumberFactory nf = systemContext.getNumberFactory();
        int width = systemContext.getParamValue("width", Integer.class);
        int height = systemContext.getParamValue("height", Integer.class);
        Number zoom = systemContext.getZoom();
        //ComplexNumber anchor = getWorldCoords(nf.createComplexNumber(renderer.getXShift(), renderer.getYShift()));
        ComplexNumber anchor = clientSystem.anchor;

        ComplexNumber gridPos = worldCoords.copy();
        gridPos.sub(anchor);
        gridPos.divNumber(zoom);
//        ComplexNumber shift = nf.createComplexNumber(width/height/2f, 1/2f);
//        gridPos.sub(shift);
        gridPos.multNumber(nf.createNumber(systemContext.getChunkSize()));
        gridPos.divNumber(nf.createNumber(30.4));
        //gridPos.divNumber(nf.createNumber(64*4));
        //gridPos.add(nf.createComplexNumber(0, 1));
        return gridPos;
    }

    public ComplexNumber getScreenCoords(ComplexNumber worldCoords, ClientSystem clientSystem){

        NumberFactory nf = systemContext.getNumberFactory();
        int height = systemContext.getParamValue("height", Integer.class);
        Number zoom = systemContext.getZoom();
        int chunkSize = systemContext.getChunkSize();

        //((x-anchorX)*height/zoom, -((y-anchorY)*height/zoom + chunkSize)

        ComplexNumber pos = worldCoords.copy();
        pos.sub(clientSystem.getAnchor());
        pos.divNumber(zoom);
        pos.multNumber(nf.createNumber(height)); //TODO buffer wrapped values?

        Number screenY = pos.getImag();
        screenY.add(nf.createNumber(chunkSize));
        screenY.mult(nf.createNumber(-1));
        return nf.createComplexNumber(pos.getReal(), screenY);
    }

    public ComplexNumber getCurrentMidpoint(){
        return currentMidpoint;
    }

    public void setCurrentMidpoint(ComplexNumber currentMidpoint) {
        this.currentMidpoint = currentMidpoint;
    }

    public ComplexNumber toComplex(double x, double y) {
        return systemContext.getNumberFactory().createComplexNumber(x, y);
    }

    public ClientSystem getClientSystem(){
        return clientSystem;
    }

    public void setClientSystem(ClientSystem clientSystem){
        this.clientSystem = clientSystem;
        this.systemContext = (BFSystemContext)clientSystem.getSystemContext();
    }

    public SystemContext getSystemContext() {
        return clientSystem.systemContext;
    }
}
