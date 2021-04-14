package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import de.felixp.fractalsgdx.FractalsGdxMain;
import de.felixp.fractalsgdx.remoteclient.Client;
import de.felixp.fractalsgdx.remoteclient.ClientSystem;
import de.felixp.fractalsgdx.remoteclient.SystemInterfaceGdx;
import de.felixp.fractalsgdx.ui.MainStage;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.network.interfaces.ClientSystemInterface;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.suppliers.StaticParamSupplier;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.system.systems.infra.ViewData;
import de.felixperko.fractals.system.systems.stateinfo.TaskState;
import de.felixperko.fractals.util.NumberUtil;

public class RemoteRenderer extends AbstractFractalRenderer {

    private static Logger LOG = LoggerFactory.getLogger(RemoteRenderer.class);

    boolean outlineChunkBorders = false;

    SystemInterfaceGdx systemInterface;

    Map<ViewData, List<RenderChunk>> chunks = new HashMap<>();

    Map<Integer, Map<Integer,Texture>> textures = new HashMap<>();
    //public Map<ComplexNumber, Texture> textures = new HashMap<>();

    List<Texture> textureList = new ArrayList<>();
    Map<Integer, Queue<Texture>> offscreenTextures = new HashMap<>(); //texture dimensions; textures

    public Map<Integer, Map<Integer,Pixmap>> newPixmaps = new HashMap<>();
//    public static Map<ComplexNumber, Pixmap> newPixmaps = new HashMap<>();

    ShaderProgram shader;

    FrameBuffer fbo;

    Matrix3 matrix = new Matrix3(new float[] {1,0,0, 0,1,0, 0,0,1, 0,0,0});

    double xPos = 0;
    double yPos = 0;

    ShapeRenderer shapeRenderer;
    private UUID systemId;

    public RemoteRenderer(RendererContext rendererContext) {
        super(rendererContext);
        rendererContext.setRenderer(this);
        createClientSystem();
    }

    protected void createClientSystem() {
        Client client = FractalsGdxMain.client;
        systemId = UUID.randomUUID();
        ClientSystem clientSystem = new ClientSystem(client.getManagers(), client, systemId);
        client.addClientSystemRequest(systemId, clientSystem);
    }

    @Override
    public void init() {
        ShaderProgram.pedantic = false;
        shader = compileShader("PassthroughVertexCpu.glsl", "SobelDecodeFragmentCpu.glsl");

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, (int)getWidth(), (int)getHeight(), false);

//        setBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        setFillParent(true);

        shapeRenderer = new ShapeRenderer();

        addListener(new ActorGestureListener(){
            @Override
            public void tap(InputEvent event, float x, float y, int count, int button) {

                if (systemInterface != null) {
                    boolean changed = false;
                    NumberFactory nf = getSystemContext().getNumberFactory();
                    if (button == Input.Buttons.LEFT) {
//                        systemInterface.getClientSystem().updatePosition(getWidth() * 0.5 - x, getHeight() * 0.5 - y);
                        systemInterface.getClientSystem().updateZoom(nf.createNumber("0.5"));
                        changed = true;
                    } else if (button == Input.Buttons.RIGHT) {
                        systemInterface.getClientSystem().updateZoom(nf.createNumber("2"));
                        changed = true;
                    }

                    if (changed) {
//                        systemInterface.getClientSystem().updatePosition(0, 0);
                        rendererContext.panned(systemInterface.getParamContainer());
                        reset();
                    }
                }
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
                if (systemInterface != null) {
                    xPos += deltaX;
                    yPos += deltaY;
                    systemInterface.getClientSystem().updatePosition(deltaX, deltaY);
                    rendererContext.panned(systemInterface.getParamContainer());
                }
            }
        });
    }

    //TODO implement coordinate conversion for RemoteRenderer
    @Override
    public float getScreenX(double real) {
        return 0;
    }

    @Override
    public float getScreenY(double imag) {
        return 0;
    }

    @Override
    public float getScreenX(Number real) {
        return 0;
    }

    @Override
    public float getScreenY(Number imag) {
        return 0;
    }

    @Override
    public ComplexNumber getComplexMapping(float screenX, float screenY) {
        return null;
    }

    @Override
    public Number getReal(float screenX) {
        return null;
    }

    @Override
    public Number getImag(float screenY) {
        return null;
    }
//    @Override
//    public float getPrefWidth() {
//        return Gdx.graphics.getWidth();
//    }
//
//    @Override
//    public float getPrefHeight() {
//        return Gdx.graphics.getHeight();

//    }

    @Override
    public void reset() {
        xPos = 0;
        yPos = 0;
        prevWidth = getWidth();
        prevHeight = getHeight();
        scaleX = 1;
        scaleY = 1;
        //systemInterface.getClientSystem().incrementJobId();
        synchronized (newPixmaps) {
            newPixmaps.forEach((x2, xMap) -> xMap.forEach((y2, pixmap) -> pixmap.dispose()));
            newPixmaps.clear();
        }
        synchronized (textures) {
            textures.forEach((x2, xMap) -> xMap.forEach((y2, texture) -> texture.dispose()));
            textures.clear();
        }
    }

    @Override
    public void removed() {
        //TODO
    }

    @Override
    public double getXShift() {
        return xPos;
    }

    @Override
    public double getYShift() {
        return yPos;
    }

    Color tintColor = new Color(1f,1f,1f,0.5f);


    float timeCounter = 0;

    @Override
    public void draw(Batch batch, float parentAlpha) {

        batch.setColor(Color.WHITE);
//        getStage().getViewport().apply();

        //react to resizing (partly...)
//        setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (prevWidth == -1){
            prevWidth = getWidth();
            prevHeight = getHeight();
        }

        if (systemInterface == null || systemInterface.getClientSystem() == null)
            return;

        //pixmaps (ram) -> textures (vram) (i guess?)

        int chunkSize = systemInterface.getClientSystem().chunkSize;

        //frame buffer pass 1: draw encoded floats to framebuffer
        fbo.begin();

        Matrix4 matrix = new Matrix4();
        matrix.setToOrtho2D(0, 0, getWidth(), getHeight()); // here is the actual size you want
        batch.setProjectionMatrix(matrix);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawChunks(batch, chunkSize);

        batch.end();
        fbo.end();
        //frame buffer pass 1 end

//        newOffscreenTextures.forEach((texture, textureX) -> {
//            getTextureYMap(textureX).remove(texture);
//            textureList.remove(texture);
////            textureOffscreen(texture);
//            texture.dispose();
//        });

        processPixmaps();

        shader.begin();
        batch.begin();

        batch.setShader(shader);
        shader.setUniformMatrix("u_projTrans", matrix);
        timeCounter += (float)Gdx.graphics.getDeltaTime();

        setColoringParams();

        matrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setProjectionMatrix(matrix);

        //draw flipped framebuffer on screen
        Texture texture = fbo.getColorBufferTexture();
        TextureRegion textureRegion = new TextureRegion(texture);
        textureRegion.flip(false, true);
        batch.draw(textureRegion, getX(), getY(), getWidth(), getHeight());
        batch.end();

        shader.end();
        batch.flush();

        if (isScreenshot(true)) {
            long t1 = System.nanoTime();
            makeScreenshot();
            long t2 = System.nanoTime();
            double dtInMs = NumberUtil.getTimeInS((t2-t1)/1000, 2);
            System.out.println("made screenshot: "+dtInMs+" ms");
        }

//        if (outlineChunkBorders || Gdx.input.isKeyPressed(Input.Keys.O)) {
//            drawOutline(chunkSize);
//        }

        batch.begin();
        batch.setShader(null);
    }

    protected void drawChunks(Batch batch, int chunkSize) {
        //        Map<Texture, Integer> newOffscreenTextures = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Texture>> e : textures.entrySet()) {
            for (Map.Entry<Integer, Texture> e2 : e.getValue().entrySet()) {
                Integer textureX = e.getKey();
                Integer textureY = e2.getKey();
                float x = ((textureX - 0.0f * chunkSize) + (float) xPos) + getWidth() / 2;
                float y = ((textureY - 0.0f * chunkSize) + (float) yPos) + getHeight() / 2;

                int margin = 100;
                boolean onScreen = x > 0-chunkSize-margin*2 && x < getWidth()+margin
                                && y > 0-chunkSize-margin*2 && y < getHeight()+margin;

                Texture texture = e2.getValue();
                if (onScreen) {
                    //draw encoded float texture on framebuffer
                    batch.draw(texture, x, y, chunkSize, chunkSize);
                } else {
//                    newOffscreenTextures.put(texture, textureX);
                }
            }
        }
    }

    protected void setColoringParams(){
        ComplexNumber anchor = systemInterface.getClientSystem().getAnchor().copy();
        super.setColoringParams(shader, getWidth(), getHeight(), (MainStage)getStage(), getSystemContext(), getRendererContext());
    }

    private void processPixmaps() {
        synchronized (newPixmaps){

//            int countLeft = 2;
//            List<Pair<Integer, Integer>> removePairs = new ArrayList<>();
//            boolean limitReached = false;

            loop:
            for (Map.Entry<Integer, Map<Integer, Pixmap>> e : newPixmaps.entrySet()){
                int x = e.getKey();
                Map<Integer, Texture> textureYMap = getTextureYMap(e.getKey());
                for (Map.Entry<Integer,Pixmap> e2 : e.getValue().entrySet()){

                    int y = e2.getKey();
                    Pixmap pixmap = e2.getValue();
                    Texture texture = textureYMap.get(y);


//                    if (texture != null)
//                        texture.dispose();

////                    if (texture == null) {
//                    texture = new Texture(pixmap);
////                        texture.draw(pixmap, 0, 0);
//                    textureYMap.put(y, texture);
//                    textureList.add(texture);
////                    }
////                    else {
////                        texture.draw(pixmap, 0, 0);
////                    }

//                    pixmap.dispose();


                    //load a cached offscreen texture to overwrite
                    if (texture == null){
                        Queue<Texture> offscreenTextureList = offscreenTextures.get(pixmap.getWidth());
                        if (offscreenTextureList != null && !offscreenTextureList.isEmpty())
                            texture = offscreenTextureList.poll();
                    }
//                    if (texture != null && texture.getWidth() != pixmap.getWidth()){
//                        textureOffscreen(texture);
//                    }
                    //not cached -> create
                    if (texture != null){
                        texture.dispose();
//                        PixmapTextureData pixmapTextureData = new PixmapTextureData(pixmap, null, false, false);
//                        texture.load(pixmapTextureData);
                    }
//                    if (texture == null) {
                        texture = new Texture(pixmap);
                        textureYMap.put(y, texture);
                        textureList.add(texture);
//                    }



//                    removePairs.add(new Pair<Integer, Integer>(e.getKey(), e2.getKey()));
//                    if (--countLeft <= 0) {
//                        limitReached = true;
//                        break loop;
//                    }
                }
            }
            if (!newPixmaps.isEmpty())
                setRefresh();
            newPixmaps.clear();
//            if (!limitReached)
//            else {
//                for (Pair<Integer, Integer> remove : removePairs){
//                    Integer x = remove.getKey();
//                    Integer y = remove.getValue();
//                    newPixmaps.get(x).remove(y);
//                }
//            }
        }
    }
    //TODO outline
//    private void drawOutline(int chunkSize) {
//        if (systemInterface == null)
//            return;
//        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
//        int width = Gdx.graphics.getWidth();
//        int height = Gdx.graphics.getHeight();
//        float subX = 1;
//        float subY = 1;
////        subX = chunkSize/2f;
////        subY = -chunkSize*9/32f;
//        float startX = (float)(xPos+width/2f-subX) % chunkSize;
//        float startY = (float)(yPos-height/2f+subY) % chunkSize;
//
//        Gdx.gl.glEnable(GL20.GL_BLEND);
//        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
//
//        //ComplexNumber startWorldPos = systemInterface.getWorldCoords(systemInterface.toComplex(startX, Gdx.graphics.getHeight()-startY));
//        ComplexNumber startWorldPos = systemInterface.getWorldCoords(systemInterface.toComplex(width*0.5, height*0.5));
//        //ComplexNumber startChunkPos = systemInterface.getChunkGridCoords(startWorldPos);
//        BFSystemContext systemContext = (BFSystemContext) systemInterface.getSystemContext();
//        ComplexNumber startChunkPos = systemContext.getNumberFactory().createComplexNumber(systemContext.getChunkX(startWorldPos), systemContext.getChunkY(startWorldPos));
//        startChunkPos.add(systemInterface.toComplex(-width*0.5/chunkSize, height*0.5/chunkSize));
////        ComplexNumber endWorldPos = systemInterface.getWorldCoords(systemInterface.toComplex(Gdx.graphics.getWidth(), 0));
////        ComplexNumber endChunkPos = systemInterface.getChunkGridCoords(endWorldPos);
////        System.out.println("startWorldPos: "+startWorldPos.toString()+" startChunkPos: "+startChunkPos.toString());
//
//        long startChunkX = (long)Math.floor(startChunkPos.getReal().toDouble());
//        long startChunkY = (long)Math.ceil(startChunkPos.getImag().toDouble());
//        long chunkX = startChunkX;
//        for (float x = startX-chunkSize ; x < width ; x += chunkSize){
//            long chunkY = startChunkY;
//            for (float y = startY-chunkSize ; y < height ; y += chunkSize){
//                ChunkContainer chunkContainer = getChunkContainer(chunkX, chunkY);
//                shapeRenderer.setColor(getChunkStateColor(chunkContainer == null ? null : chunkContainer.getTaskState()));
//                if (chunkContainer != null && chunkContainer.getTaskState() == TaskState.STARTED) {
//                    float progress = (float)chunkContainer.getTimeProgress();
//                    float height1 = progress*chunkSize;
//                    float height2 = chunkSize-height1;
//                    shapeRenderer.rect(x, y+height2, chunkSize, height1);
//                    shapeRenderer.setColor(getChunkStateColor(TaskState.PLANNED));
//                    shapeRenderer.rect(x, y, chunkSize, height2);
//                } else {
//                    shapeRenderer.rect(x, y, chunkSize, chunkSize);
//                }
//                chunkY--;
//            }
//            chunkX++;
//        }
//
//
////        shapeRenderer.flush();
////        shapeRenderer.end();
////        Gdx.gl.glDisable(GL20.GL_BLEND);
////        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
//
//        shapeRenderer.setColor(Color.RED);
//        shapeRenderer.circle(width/2f + (float)xPos, height/2f + (float)yPos, 3);
//
//        ComplexNumber currentMidpoint = systemInterface.getCurrentMidpoint();
//        if (currentMidpoint != null) {
//            ParamContainer systemClientData = systemInterface.getParamContainer();
//            int w = systemClientData.getClientParameter("width").getGeneral(Integer.class);
//            int h = systemClientData.getClientParameter("height").getGeneral(Integer.class);
//            NumberFactory nf = systemClientData.getClientParameter("numberFactory").getGeneral(NumberFactory.class);
//
//            ComplexNumber screenMid = nf.createComplexNumber(w/2., h/2.);
//            ComplexNumber screenPos = systemInterface.getWorldCoords(screenMid);
//            screenPos.sub(currentMidpoint);
//            screenPos.divNumber(systemClientData.getClientParameter("zoom").getGeneral(Number.class));
//            //screenPos.add(screenMid);
////            screenPos.divNumber(nf.createNumber(30.4));
////            screenPos.sub(screenMid);
////            ComplexNumber screenPos = systemInterface.getChunkData().getScreenCoords(currentMidpoint);
////            screenPos.multNumber(nf.createNumber(-1.));
//
//            System.out.println(currentMidpoint.toString()+" -> "+screenPos.toString());
//            shapeRenderer.setColor(Color.GREEN);
//            shapeRenderer.circle((float) screenPos.getReal().toDouble(), (float) screenPos.getImag().toDouble(), 3);
//        }
//
//        shapeRenderer.setColor(Color.GRAY);
//        for (float x = startX ; x < width ; x += chunkSize){
//            shapeRenderer.line(x, 0, x, height);
//        }
//        for (float y = startY; y < height ; y += chunkSize){
//            shapeRenderer.line(0, y, width, y);
//        }
//
//        shapeRenderer.end();

//    }

    private Color getChunkStateColor(TaskState taskState){
        float alpha = 0.5f;
        if (taskState == null)
            return new Color(0.5f, 0.5f, 0.5f, alpha);
        switch (taskState){
            case PLANNED:
                return new Color(0.7f, 0.7f, 0.7f, alpha);
            case STARTED:
                return new Color(1f, 1f, 0f, alpha);
            case OPEN:
                return new Color(0.2f, 1f, 0.2f, alpha);
            case FINISHED:
                return new Color(1f, 0.7f, 0f, alpha);
            case BORDER:
                return new Color(1f, 0.2f, 0.2f, alpha);
            case DONE:
                return new Color(0f, 0f, 1f, alpha);
            case ASSIGNED:
                return new Color(1f, 0f, 1f, alpha);
            case REMOVED:
                return new Color(1f, 0f, 1f, alpha);
        }
        return null;
    }
    float prevWidth = -1, prevHeight = -1;

    float scaleX = 1, scaleY = 1;

    @Override
    public void setSize(float width, float height) {

        super.setSize(width, height);

        if (prevWidth != -1){
            scaleX /= getWidth() / prevWidth;
            scaleY /= getHeight() / prevHeight;
        }
    }

    @Override
    protected void sizeChanged() {

        if (getWidth() > 0 && getHeight() > 0)
            fbo = new FrameBuffer(Pixmap.Format.RGBA8888, (int)getWidth(), (int)getHeight(), false);

        if (systemInterface == null)
            return;

        SystemContext context = systemInterface.getSystemContext();
        context.getParamContainer().addClientParameter(new StaticParamSupplier("width", (int)getWidth()));
        context.getParamContainer().addClientParameter(new StaticParamSupplier("height", (int)getHeight()));
        context.setParameters(context.getParamContainer());

        setRefresh();
        super.sizeChanged();
    }

    public SystemInterfaceGdx getSystemInterface() {
        return systemInterface;
    }

    public void setSystemInterface(SystemInterfaceGdx systemInterface) {
        this.systemInterface = systemInterface;
    }

    @Override
    public SystemContext getSystemContext(){
        return systemInterface.getSystemContext();
    }

    public void drawPixmap(Integer startX, Integer startY, Pixmap pixmap){
        synchronized (newPixmaps){
            Map<Integer, Pixmap> pixmapsYMap = getPixmapsYMap(startX);
            pixmapsYMap.put(startY, pixmap);
        }
        setRefresh();
//			texture = new Texture(pixmap);
//			textureYMap.put(startY, texture)
    }

    private void textureOffscreen(Texture texture) {
        Queue offscreenTextureQueue = offscreenTextures.get(texture.getWidth());
        if (offscreenTextureQueue == null){
            offscreenTextureQueue = new LinkedList();
            offscreenTextures.put(texture.getWidth(), offscreenTextureQueue);
        }
        offscreenTextureQueue.add(texture);
    }

    private Map<Integer, Texture> getTextureYMap(Integer x){
        Map<Integer, Texture> map = textures.get(x);
        if (map == null) {
            map = new HashMap<>();
            textures.put(x, map);
        }
        return map;
    }

    private Map<Integer, Pixmap> getPixmapsYMap(Integer x){
        Map<Integer, Pixmap> map = newPixmaps.get(x);
        if (map == null) {
            map = new HashMap<>();
            newPixmaps.put(x, map);
        }
        return map;
    }

    public UUID getSystemId() {
        return systemId;
    }
}
