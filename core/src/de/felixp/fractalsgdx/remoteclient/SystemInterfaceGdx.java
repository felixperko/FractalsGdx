package de.felixp.fractalsgdx.remoteclient;

import java.util.UUID;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixp.fractalsgdx.rendering.RemoteRenderer;
import de.felixperko.fractals.data.CompressedChunk;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.manager.client.ClientManagers;
import de.felixperko.fractals.network.interfaces.ClientSystemInterface;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.ParamConfiguration;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BFSystemContext;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.system.systems.infra.ViewContainerAdapter;
import de.felixperko.fractals.system.systems.infra.ViewData;
import de.felixperko.fractals.util.CategoryLogger;
import de.felixperko.fractals.util.ColorContainer;

public class SystemInterfaceGdx extends ViewContainerAdapter implements ClientSystemInterface {

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

    EncodePixmapThread pixmapThread;

    public SystemInterfaceGdx(UUID systemId, MessageInterfaceGdx messageInterface, ClientManagers managers){

        this.systemId = systemId;
        this.managers = managers;
        this.messageInterface = messageInterface;
        this.renderer = (RemoteRenderer)((MainStage)FractalsGdxMain.stage).getRemoteRendererById(systemId);

        this.pixmapThread = new EncodePixmapThread(this);
        this.pixmapThread.start();
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

    ParamConfiguration parameterConfiguration;

    @Override
    public void updateParameterConfiguration(ParamContainer systemClientData, ParamConfiguration parameterConfiguration) {
        if (parameterConfiguration != null) {
            this.parameterConfiguration = parameterConfiguration;
            if (clientSystem != null)
                clientSystem.setParamConfiguration(parameterConfiguration);
        }
        if (this.parameterConfiguration != null) {
            ((MainStage) FractalsGdxMain.stage).getParamUI().setServerParameterConfiguration(renderer, systemClientData, this.parameterConfiguration);
        }
    }

    @Override
    public void chunkUpdated(CompressedChunk compressedChunk) {

        int chunkX = (int)(long)compressedChunk.getChunkX();
        int chunkY = (int)(long)compressedChunk.getChunkY();
        //chunkData.addCompressedChunk(compressedChunk, chunkX, chunkY);

        drawPixmap(compressedChunk);
    }

    @Override
    public void updatedCompressedChunk(CompressedChunk compressedChunk, ViewData viewData) {
        drawPixmap(compressedChunk);
    }

    ViewData viewData;

    @Override
    public void activeViewChanged(ViewData activeView) {
        this.viewData = activeView;
    }

    private void drawPixmap(CompressedChunk compressedChunk) {
        pixmapThread.addChunk(compressedChunk);
    }

//    protected void getColor(com.badlogic.gdx.graphics.Color color, float value) {
////        float hue = (float)Math.log(value+1)*360;
//        float hue = value;
//        color.fromHsv(hue, 1, 1);
//    }

    public void addChunkCount(int count) {
        chunkCount += count;
    }

    @Override
    public void chunksCleared() {
//        image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
    }

    @Override
    public ParamConfiguration getParamConfiguration() {
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
        this.clientSystem.setParamConfiguration(parameterConfiguration);
        this.systemContext = (BFSystemContext)clientSystem.getSystemContext();
    }

    public SystemContext getSystemContext() {
        return clientSystem.systemContext;
    }

    public RemoteRenderer getRenderer() {
        return renderer;
    }
}
