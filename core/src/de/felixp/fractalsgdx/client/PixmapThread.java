package de.felixp.fractalsgdx.client;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

import net.dermetfan.utils.Pair;

import java.util.Comparator;
import java.util.PriorityQueue;

import de.felixperko.fractals.data.AbstractArrayChunk;
import de.felixperko.fractals.data.BorderAlignment;
import de.felixperko.fractals.data.CompressedChunk;
import de.felixperko.fractals.system.ZoomableSystemContext;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.systems.infra.SystemContext;
import de.felixperko.fractals.util.Nestable;
import de.felixperko.fractals.util.NestedMap;

public abstract class PixmapThread extends Thread {

    NestedMap<Integer, CompressedChunk> chunkMap = new NestedMap<>();

    PriorityQueue<Pair<Integer, Integer>> coordQueue = new PriorityQueue<Pair<Integer, Integer>>(new Comparator<Pair<Integer, Integer>>() {
        @Override
        public int compare(Pair<Integer, Integer> p1, Pair<Integer, Integer> p2) {
            CompressedChunk c1 = chunkMap.getChild(p1.getKey()).getChild(p1.getValue()).getValue();
            CompressedChunk c2 = chunkMap.getChild(p2.getKey()).getChild(p2.getValue()).getValue();
            int upsampleComp = -Double.compare(c1.getUpsample(), c2.getUpsample());
            if (upsampleComp != 0)
                return upsampleComp;
            return Double.compare(c1.getPriority(), c2.getPriority());

//            int upsampleComp = -Double.compare(c1.getUpsample(), c2.getUpsample());
//            if (upsampleComp != 0)
//                return upsampleComp;
//            return Double.compare(c1.getPriority(), c2.getPriority());

        }
    });

    SystemInterfaceGdx systemInterface;

    public PixmapThread(SystemInterfaceGdx systemInterface){
        this.systemInterface = systemInterface;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()){
//            drawNext();
            if (!drawNext()){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean drawNext() {
        CompressedChunk next = null;
        synchronized (this){
            Pair<Integer, Integer> nextCoords = null;
            nextCoords = coordQueue.poll();
            if (nextCoords == null)
                return false;
            next = chunkMap.getChild(nextCoords.getKey()).getChild(nextCoords.getValue()).getValue();
            chunkMap.getChild(nextCoords.getKey()).removeChild(nextCoords.getValue());
        }
        draw(next);


//        CompressedChunk next = null;
//        synchronized (this) {
//            next = coordQueue.poll();
//        }
//        if (next == null)
//            return false;
//        draw(next);
        return true;
    }

    private void draw(CompressedChunk compressedChunk) {

        ClientSystem clientSystem = systemInterface.getClientSystem();

        int jobId = compressedChunk.getJobId();
        System.out.println("chunk update "+jobId+" (this:"+clientSystem.systemContext.getViewId()+")");
        if (jobId != clientSystem.systemContext.getViewId())
            return;

        SystemContext systemContext = systemInterface.getSystemContext();
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
            float value = (float)chunk.getValue(i, true);
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
//        if (inside)
//            chunkCount--;

//        Gdx.app.postRunnable(new Runnable() {
//            @Override
//            public void run() {
        if (jobId == clientSystem.systemContext.getViewId())
            systemInterface.messageInterface.getFractalsGdxMain().drawPixmap(chunkImgX, chunkImgY, pixmap);
//            }
//        });
    }

    protected abstract void getColor(Color color, float value);

    public synchronized void addChunk(CompressedChunk chunk){
//        for (CompressedChunk chunk2 : coordQueue){
//            if (chunk2.getChunkX() == chunk.getChunkX() && chunk2.getChunkY() == chunk.getChunkY() && chunk2.getUpsample() == chunk.getUpsample()) {
//                coordQueue.remove(chunk2);
//                break;
//            }
//        }

//        CompressedChunk existingChunk = chunkMap.getChild(chunk.getChunkX()).getChild(chunk.getChunkY()).getValue();
        Nestable<Integer, CompressedChunk> currentNode = chunkMap.getOrMakeChild(chunk.getChunkX()).getOrMakeChild(chunk.getChunkY());
        Pair<Integer, Integer> coords = new Pair<>(chunk.getChunkX(), chunk.getChunkY());
        if (currentNode.getValue() == null){
            chunkMap.getOrMakeChild(chunk.getChunkX()).getOrMakeChild(chunk.getChunkY()).setValue(chunk);
            coordQueue.add(coords);
        } else {
            currentNode.setValue(chunk);
//            coordQueue.add(coords);
        }

//        coordQueue.add(chunk);
    }

//    private void addNewChunks(){
//        List<CompressedChunk> add = new ArrayList<>();
//        synchronized (addChunks){
//            for (CompressedChunk chunk : addChunks){
//
//            }
//        }
//    }

    public synchronized void clear(){
        coordQueue.clear();
    }

    public ComplexNumber getScreenCoords(ComplexNumber worldCoords, ClientSystem clientSystem){

        ZoomableSystemContext systemContext = (ZoomableSystemContext)systemInterface.getSystemContext();

        NumberFactory nf = systemContext.getNumberFactory();
        int height = (int)systemContext.getParamValue("height", Integer.class);
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
}
