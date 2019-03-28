package de.felixp.fractalsgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.felixp.fractalsgdx.client.Client;
import de.felixperko.fractals.network.SystemClientData;

public class RemoteRenderer extends AbstractRenderer {

    SystemClientData systemClientData;

    public static Map<Integer, Map<Integer,Texture>> textures = new HashMap<>();
    List<Texture> textureList = new ArrayList<>();

    public static Map<Integer, Map<Integer,Pixmap>> newPixmaps = new HashMap<>();

    ShaderProgram shader;

    FrameBuffer fbo;

    Matrix3 matrix = new Matrix3(new float[] {1,0,0, 0,1,0, 0,0,1, 0,0,0});

    double xPos = 0;
    double yPos = 0;

    @Override
    public void init() {
        ShaderProgram.pedantic = false;
        shader = compileShader("PassthroughVertexCpu.glsl", "SobelDecodeFragmentCpu.glsl");

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);

        setBounds(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

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
        synchronized (newPixmaps){
            for (Map.Entry<Integer, Map<Integer, Pixmap>> e : newPixmaps.entrySet()){
                Map<Integer, Texture> textureYMap = getTextureYMap(e.getKey());
                for (Map.Entry<Integer,Pixmap> e2 : e.getValue().entrySet()){
                    Texture texture = textureYMap.get(e2.getKey());
                    if (texture == null) {
                        texture = new Texture(e2.getValue());
                        textureYMap.put(e2.getKey(), texture);
                        textureList.add(texture);
                    }
                    else {
                        texture.draw(e2.getValue(), 0, 0);
                    }
                    e2.getValue().dispose();
                }
            }
            newPixmaps.clear();
        }


        fbo.begin();

        Gdx.gl.glClearColor( 0, 0, 0, 1 );
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
//        batch.begin();
        batch.setShader(null);

        for (Map.Entry<Integer, Map<Integer, Texture>> e : textures.entrySet()){
            for (Map.Entry<Integer,Texture> e2 : e.getValue().entrySet())
                batch.draw(e2.getValue(), e.getKey()+(float)xPos, -e2.getKey()-(float)yPos);
        }

        batch.end();
        fbo.end();

        shader.begin();
        batch.begin();

        batch.setShader(shader);

        shader.setUniformMatrix("u_projTrans", matrix);
        shader.setUniformf("colorShift", 0);
        shader.setUniformf("resolution", (float)Gdx.graphics.getWidth(), (float)Gdx.graphics.getHeight());

        Texture texture = fbo.getColorBufferTexture();
        TextureRegion textureRegion = new TextureRegion(texture);
        textureRegion.flip(false, false);
        batch.draw(textureRegion, 0, 0);
        batch.end();

        shader.end();
        batch.begin();
        batch.setShader(null);
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
