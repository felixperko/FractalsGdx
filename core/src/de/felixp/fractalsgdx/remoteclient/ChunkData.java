package de.felixp.fractalsgdx.remoteclient;

import java.util.HashMap;
import java.util.Map;

import de.felixperko.fractals.data.CompressedChunk;
import de.felixperko.fractals.data.ParamContainer;
import de.felixperko.fractals.system.numbers.ComplexNumber;
import de.felixperko.fractals.system.numbers.Number;
import de.felixperko.fractals.system.numbers.NumberFactory;
import de.felixperko.fractals.system.parameters.suppliers.ParamSupplier;

@Deprecated
public class ChunkData {

    Map<Integer, Map<Integer, ChunkContainer>> chunks_compressed = new HashMap<>();

    Map<Integer, ChunkContainer> chunksByTaskId = new HashMap<>();

    ParamContainer clientData;

    int chunkSize;

    public ChunkContainer getChunkContainer(int taskId){
        return chunksByTaskId.get(taskId);
    }

    public ChunkContainer getChunkContainer(int chunkX, int chunkY){
        return getCompressedChunkYMap(chunkX).get(chunkY);
    }

    public CompressedChunk getCompressedChunk(int x, int y){
        ChunkContainer chunkContainer = getChunkContainer(x, y);
        if (chunkContainer == null)
            return null;
        return chunkContainer.getCompressedChunk();
    }

    public void addCompressedChunk(CompressedChunk chunk, int x, int y){
        Map<Integer, ChunkContainer> yMap = getCompressedChunkYMap(x);
        ChunkContainer chunkContainer = new ChunkContainer(chunk);
        yMap.put(y, chunkContainer);
        chunksByTaskId.put(chunk.getTaskId(), chunkContainer);
    }

    public void removeCompressedChunk(int x, int y){
        Map<Integer, ChunkContainer> yMap = getCompressedChunkYMap(x);
        ChunkContainer chunkContainer = yMap.remove(y);
        if (chunkContainer != null)
            chunksByTaskId.remove(chunkContainer.getCompressedChunk().getTaskId());
    }

    private Map<Integer, ChunkContainer> getCompressedChunkYMap(int x){
        Map<Integer, ChunkContainer> map = chunks_compressed.get(x);
        if (map == null){
            map = new HashMap<>();
            chunks_compressed.put(x, map);
        }
        return map;
    }

    public ComplexNumber getScreenCoords(ComplexNumber worldCoords, ClientSystem clientSystem){

        Map<String, ParamSupplier> parameters = clientData.getParamMap();
        double height = parameters.get("height").getGeneral(Integer.class);
        Number zoom = parameters.get("zoom").getGeneral(Number.class);
        NumberFactory nf = parameters.get("numberFactory").getGeneral(NumberFactory.class);

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
