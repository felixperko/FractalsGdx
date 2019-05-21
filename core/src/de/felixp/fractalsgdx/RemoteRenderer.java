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
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
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

import de.felixp.fractalsgdx.client.Client;
import de.felixperko.fractals.data.ArrayChunkFactory;
import de.felixperko.fractals.network.SystemClientData;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;

public class RemoteRenderer extends AbstractRenderer {

    boolean outlineChunkBorders = false;

    SystemClientData systemClientData;

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
                    FractalsGdxMain.client.updateZoom(0.5f);
                    changed = true;
                }else if (button == Input.Buttons.RIGHT){
                    FractalsGdxMain.client.updateZoom(2f);
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
                FractalsGdxMain.client.updatePosition(deltaX, deltaY);
                FractalsGdxMain.forceRefresh = true;
            }
        });
    }

    @Override
    public void reset() {
        xPos = 0;
        yPos = 0;
        FractalsGdxMain.client.jobId++;
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
    public void draw(Batch batch, float parentAlpha) {

        processPixmaps();

        int chunkSize = FractalsGdxMain.client.chunkSize;

        fbo.begin();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //draw textures on framebuffer
        for (Map.Entry<Integer, Map<Integer, Texture>> e : textures.entrySet()) {
            for (Map.Entry<Integer, Texture> e2 : e.getValue().entrySet()) {
                batch.draw(e2.getValue(), (e.getKey() - 0.0f * chunkSize) + (float) xPos + Gdx.graphics.getWidth() / 2, (e2.getKey() - 0.0f * chunkSize) + (float) yPos + Gdx.graphics.getHeight() / 2, chunkSize, chunkSize);
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
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.GRAY);
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        float subY = 0;
        if (height > 1025)
            subY = chunkSize/2f;
        float startX = (float)(xPos+width/2f) % chunkSize;
        float startY = (float)(yPos+height/2f-subY) % chunkSize;
        for (float x = startX ; x < width ; x += chunkSize){
            shapeRenderer.line(x, 0, x, height);
        }
        for (float y = startY ; y < height ; y += chunkSize){
            shapeRenderer.line(0, y, width, y);
        }

        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(Color.RED);
        shapeRenderer.circle(width/2f + (float)xPos, height/2f + (float)yPos, 3);

        shapeRenderer.end();
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

    public SystemClientData getSystemClientData(){
        return systemClientData;
    }

    public void setSystemClientData(SystemClientData systemClientData){
        this.systemClientData = systemClientData;
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
