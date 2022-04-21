package de.felixp.fractalsgdx.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;

import net.dermetfan.utils.Pair;

import de.felixp.fractalsgdx.remoteclient.ClientSystem;
import de.felixperko.fractals.data.AbstractArrayChunk;
import de.felixperko.fractals.system.ZoomableSystemContext;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.systems.stateinfo.TaskState;

class RenderChunk {

    Pixmap pixmap;
    boolean pixmapUpdated;
    Texture texture;

    Pair<Integer, Integer> chunkPos;
    ComplexNumber pos;
    ZoomableSystemContext context;
    ClientSystem clientSystem;

    TaskState state;
    double progress;

    long lastSeen;

    public RenderChunk(AbstractArrayChunk chunk, ZoomableSystemContext context, ClientSystem clientSystem, Pixmap pixmap){
        this.chunkPos = new Pair<>(chunk.getChunkX(), chunk.getChunkY());
        this.pixmap = pixmap;
        this.pixmapUpdated = true;
        this.context = context;
        this.clientSystem = clientSystem;
        this.lastSeen = System.nanoTime();
    }

    public void updateLastSeen(){
        this.lastSeen = System.nanoTime();
    }

    public void setPixmap(Pixmap pixmap){
        this.pixmap = pixmap;
        pixmapUpdated = true;
    }

    public void generateTextureIfPixmapUpdated(){
        if (pixmapUpdated){
            generateTexture();
            pixmapUpdated = false;
        }
    }

    public void generateTexture(){
        if (texture != null)
            texture.dispose();
        texture = new Texture(pixmap);
    }

    /**
     * Draws the texture if visible and returns if object should be disposed.
     * @param batch
     * @return true if object should be disposed
     */
    public boolean draw(Batch batch, long visbilityTimeout){
        ComplexNumber screenPos = getScreenCoords(pos, clientSystem);
        int lowerBorderX = 0;
        int lowerBorderY = 0;
        int higherBorderX = Gdx.graphics.getWidth();
        int higherBorderY = Gdx.graphics.getHeight();
        float drawX = (float) screenPos.getReal().toDouble();
        float drawY = (float) screenPos.getImag().toDouble();

        if (drawX >= lowerBorderX && drawX <= higherBorderX &&
                drawY >= lowerBorderY && drawY <= higherBorderY) {
            updateLastSeen();
            int chunkSize = context.getChunkSize();
            generateTextureIfPixmapUpdated();
            batch.draw(texture, drawX, drawY, chunkSize, chunkSize);
            return false;
        } else {
            return reachedVisibilityTimeout(visbilityTimeout);
        }
    }

    public boolean reachedVisibilityTimeout(long visiblityTimeout){
        return System.nanoTime()-lastSeen > visiblityTimeout;
    }

    public ComplexNumber getScreenCoords(ComplexNumber worldCoords, ClientSystem clientSystem){

        NumberFactory nf = context.getNumberFactory();
        int height = (int)context.getParamValue("height", Integer.class);
        Number zoom = context.getZoom();
        int chunkSize = context.getChunkSize();

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
}
