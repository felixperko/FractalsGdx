package de.felixp.fractalsgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.client.ChunkContainer;
import de.felixp.fractalsgdx.client.SystemInterfaceGdx;
import de.felixperko.fractals.network.ParamContainer;
import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.Numbers.infra.ComplexNumber;
import de.felixperko.fractals.system.Numbers.infra.Number;
import de.felixperko.fractals.system.Numbers.infra.NumberFactory;
import de.felixperko.fractals.system.systems.BreadthFirstSystem.BFSystemContext;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.system.systems.stateinfo.TaskState;
import de.felixperko.fractals.system.systems.stateinfo.TaskStateInfo;

public class RemoteRenderer extends AbstractRenderer {

    boolean outlineChunkBorders = false;

    SystemInterfaceGdx systemInterface;

    public static Map<Integer, Map<Integer,Texture>> textures = new HashMap<>();
    List<Texture> textureList = new ArrayList<>();

    public static Map<Integer, Map<Integer,Pixmap>> newPixmaps = new HashMap<>();

    ShaderProgram shader;

    FrameBuffer fbo;

    Matrix3 matrix = new Matrix3(new float[] {1,0,0, 0,1,0, 0,0,1, 0,0,0});

    double xPos = 0;
    double yPos = 0;

    ShapeRenderer shapeRenderer;

    @Override
    public void init() {
        ShaderProgram.pedantic = false;
        shader = compileShader("PassthroughVertexCpu.glsl", "SobelDecodeFragmentCpu.glsl");

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

        setBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        shapeRenderer = new ShapeRenderer();

        addListener(new ActorGestureListener(){
            @Override
            public void tap(InputEvent event, float x, float y, int count, int button) {

                boolean changed = false;
                if (button == Input.Buttons.LEFT) {
                    systemInterface.getClientSystem().updateZoom(0.5f);
                    changed = true;
                }else if (button == Input.Buttons.RIGHT){
                    systemInterface.getClientSystem().updateZoom(2f);
                    changed = true;
                }

                if (changed){
                    reset();
                }
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY) {
                xPos += deltaX;
                yPos += deltaY;
                systemInterface.getClientSystem().updatePosition(deltaX, deltaY);
                FractalsGdxMain.forceRefresh = true;
            }
        });
    }

    @Override
    public void reset() {
        xPos = 0;
        yPos = 0;
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
    public double getXShift() {
        return xPos;
    }

    @Override
    public double getYShift() {
        return yPos;
    }

    Color tintColor = new Color(1f,1f,1f,0.5f);

    @Override
    public void draw(Batch batch, float parentAlpha) {

        if (systemInterface == null || systemInterface.getClientSystem() == null)
            return;

        processPixmaps();

        int chunkSize = systemInterface.getClientSystem().chunkSize;

        fbo.begin();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        for (Map.Entry<Integer, Map<Integer, Texture>> e : textures.entrySet()) {
            for (Map.Entry<Integer, Texture> e2 : e.getValue().entrySet()) {
                float x = (e.getKey() - 0.0f * chunkSize) + (float) xPos + Gdx.graphics.getWidth() / 2;
                float y = (e2.getKey() - 0.0f * chunkSize) + (float) yPos + Gdx.graphics.getHeight() / 2;
                batch.draw(e2.getValue(), x, y, chunkSize, chunkSize);
            }
        }

        batch.end();
        fbo.end();

        shader.begin();
        batch.begin();

        batch.setShader(shader);
        shader.setUniformMatrix("u_projTrans", matrix);
        shader.setUniformf("colorShift", 0);
        shader.setUniformf("resolution", (float) Gdx.graphics.getWidth(), (float) Gdx.graphics.getHeight());

        //draw flipped framebuffer on screen
        Texture texture = fbo.getColorBufferTexture();
        TextureRegion textureRegion = new TextureRegion(texture);
        textureRegion.flip(false, true);
        batch.draw(textureRegion, 0, 0);
        batch.end();

        shader.end();

        if (isScreenshot(true)) {
            makeScreenshot();
        }

        if (outlineChunkBorders || Gdx.input.isKeyPressed(Input.Keys.O)) {
            drawOutline(chunkSize);
        }

        batch.begin();
        batch.setShader(null);
    }

    private void processPixmaps() {
        synchronized (newPixmaps){
            for (Map.Entry<Integer, Map<Integer, Pixmap>> e : newPixmaps.entrySet()){
                int x = e.getKey();
                Map<Integer, Texture> textureYMap = getTextureYMap(e.getKey());
                for (Map.Entry<Integer,Pixmap> e2 : e.getValue().entrySet()){
                    int y = e2.getKey();
                    Pixmap pixmap = e2.getValue();
                    Texture texture = textureYMap.get(y);
                    if (texture != null)
                        texture.dispose();
//                    if (texture == null) {
                        texture = new Texture(pixmap);
//                        texture.draw(pixmap, 0, 0);
                        textureYMap.put(y, texture);
                        textureList.add(texture);
//                    }
//                    else {
//                        texture.draw(pixmap, 0, 0);
//                    }
                    pixmap.dispose();
                    setRefresh();
                }
            }
            newPixmaps.clear();
        }
    }

    private void drawOutline(int chunkSize) {
        if (systemInterface == null)
            return;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        float subX = 1;
        float subY = 1;
//        subX = chunkSize/2f;
//        subY = -chunkSize*9/32f;
        float startX = (float)(xPos+width/2f-subX) % chunkSize;
        float startY = (float)(yPos-height/2f+subY) % chunkSize;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        //ComplexNumber startWorldPos = systemInterface.getWorldCoords(systemInterface.toComplex(startX, Gdx.graphics.getHeight()-startY));
        ComplexNumber startWorldPos = systemInterface.getWorldCoords(systemInterface.toComplex(width*0.5, height*0.5));
        //ComplexNumber startChunkPos = systemInterface.getChunkGridCoords(startWorldPos);
        BFSystemContext systemContext = (BFSystemContext) systemInterface.getSystemContext();
        ComplexNumber startChunkPos = systemContext.getNumberFactory().createComplexNumber(systemContext.getChunkX(startWorldPos), systemContext.getChunkY(startWorldPos));
        startChunkPos.add(systemInterface.toComplex(-width*0.5/chunkSize, height*0.5/chunkSize));
//        ComplexNumber endWorldPos = systemInterface.getWorldCoords(systemInterface.toComplex(Gdx.graphics.getWidth(), 0));
//        ComplexNumber endChunkPos = systemInterface.getChunkGridCoords(endWorldPos);
//        System.out.println("startWorldPos: "+startWorldPos.toString()+" startChunkPos: "+startChunkPos.toString());

        long startChunkX = (long)Math.floor(startChunkPos.getReal().toDouble());
        long startChunkY = (long)Math.ceil(startChunkPos.getImag().toDouble());
        long chunkX = startChunkX;
        for (float x = startX-chunkSize ; x < width ; x += chunkSize){
            long chunkY = startChunkY;
            for (float y = startY-chunkSize ; y < height ; y += chunkSize){
                ChunkContainer chunkContainer = getChunkContainer(chunkX, chunkY);
                shapeRenderer.setColor(getChunkStateColor(chunkContainer == null ? null : chunkContainer.getTaskState()));
                if (chunkContainer != null && chunkContainer.getTaskState() == TaskState.STARTED) {
                    float progress = (float)chunkContainer.getProgress();
                    float height1 = progress*chunkSize;
                    float height2 = chunkSize-height1;
                    shapeRenderer.rect(x, y+height2, chunkSize, height1);
                    shapeRenderer.setColor(getChunkStateColor(TaskState.PLANNED));
                    shapeRenderer.rect(x, y, chunkSize, height2);
                } else {
                    shapeRenderer.rect(x, y, chunkSize, chunkSize);
                }
                chunkY--;
            }
            chunkX++;
        }


//        shapeRenderer.flush();
//        shapeRenderer.end();
//        Gdx.gl.glDisable(GL20.GL_BLEND);
//        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(Color.RED);
        shapeRenderer.circle(width/2f + (float)xPos, height/2f + (float)yPos, 3);

        ComplexNumber currentMidpoint = systemInterface.getCurrentMidpoint();
        if (currentMidpoint != null) {
            ParamContainer systemClientData = systemInterface.getSystemClientData();
            int w = systemClientData.getClientParameter("width").getGeneral(Integer.class);
            int h = systemClientData.getClientParameter("height").getGeneral(Integer.class);
            NumberFactory nf = systemClientData.getClientParameter("numberFactory").getGeneral(NumberFactory.class);

            ComplexNumber screenMid = nf.createComplexNumber(w/2., h/2.);
            ComplexNumber screenPos = systemInterface.getWorldCoords(screenMid);
            screenPos.sub(currentMidpoint);
            screenPos.divNumber(systemClientData.getClientParameter("zoom").getGeneral(Number.class));
            //screenPos.add(screenMid);
//            screenPos.divNumber(nf.createNumber(30.4));
//            screenPos.sub(screenMid);
//            ComplexNumber screenPos = systemInterface.getChunkData().getScreenCoords(currentMidpoint);
//            screenPos.multNumber(nf.createNumber(-1.));

            System.out.println(currentMidpoint.toString()+" -> "+screenPos.toString());
            shapeRenderer.setColor(Color.GREEN);
            shapeRenderer.circle((float) screenPos.getReal().toDouble(), (float) screenPos.getImag().toDouble(), 3);
        }

        shapeRenderer.setColor(Color.GRAY);
        for (float x = startX ; x < width ; x += chunkSize){
            shapeRenderer.line(x, 0, x, height);
        }
        for (float y = startY; y < height ; y += chunkSize){
            shapeRenderer.line(0, y, width, y);
        }

        shapeRenderer.end();
    }

    private ChunkContainer getChunkContainer(long chunkX, long chunkY){
        return systemInterface.getChunkData().getChunkContainer((int)chunkX, (int)chunkY);
    }

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

    private void makeScreenshot() {
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

        // this loop makes sure the whole screenshot is opaque and looks exactly like what the user is seeing
        for(int i = 4; i < pixels.length; i += 4) {
            pixels[i - 1] = (byte) 255;
        }

        //get date string
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Calendar cal = Calendar.getInstance();
        Date d = new Date();
        String date = dateFormat.format(d);
        String filename = "fractals_"+date+".png";


        Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
        BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
        FileHandle fileHandle = Gdx.files.external(filename);
        PixmapIO.writePNG(fileHandle, pixmap);
        System.out.println("saved screenshot to "+fileHandle.file().getAbsolutePath());
        pixmap.dispose();
    }

    @Override
    protected void sizeChanged() {
        setRefresh();
        super.sizeChanged();
    }

    public SystemInterfaceGdx getSystemInterface() {
        return systemInterface;
    }

    public void setSystemInterface(SystemInterfaceGdx systemInterface) {
        this.systemInterface = systemInterface;
    }

    public ShaderProgram compileShader(String vertexPath, String fragmentPath){
        ShaderProgram shader = new ShaderProgram(Gdx.files.internal(vertexPath),
                Gdx.files.internal(fragmentPath));
        if (!shader.isCompiled()) {
            throw new IllegalStateException("Error compiling shaders ("+vertexPath+", "+fragmentPath+"): "+shader.getLog());
        }
        return shader;
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
}