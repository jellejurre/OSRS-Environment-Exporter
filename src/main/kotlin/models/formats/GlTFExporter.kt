package models.formats

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import models.formats.glTF.Accessor
import models.formats.glTF.AccessorType
import models.formats.glTF.Attributes
import models.formats.glTF.Buffer
import models.formats.glTF.BufferView
import models.formats.glTF.Image
import models.formats.glTF.Material
import models.formats.glTF.Mesh
import models.formats.glTF.Node
import models.formats.glTF.Primitive
import models.formats.glTF.Scene
import models.formats.glTF.Texture
import models.formats.glTF.glTF
import utils.ByteChunkBuffer
import utils.ChunkWriteListener
import java.io.File

class GlTFExporter(private val directory: String, private val chunkWriteListeners: List<ChunkWriteListener>) : MeshFormatExporter {

    data class TileData(val z: Int, val materialId: Int)
    data class ObjectData(val z: Int, val objectTypeId: Int, val objectId: Int, val cacheIndex: Int, val materialId: Int)

    private val tileMap = HashMap<TileData, ObjectBuffers>()
    private val objectMap = HashMap<ObjectData, ObjectBuffers>()
    private val rsIndexToMaterialIndex = HashMap<Int, Int>()
    private val sceneNodes = ArrayList<Int>()
    private val chunkBuffer = ByteChunkBuffer()
    /** How many bytes the data file contains already, offsetting memory buffers by that many bytes. */
    private var bufferFileOffset = 0L

    private val dataFile = File("$directory/$dataFilename").outputStream().channel
    private var flushingRegionNum = 0
    private val gltfModel = glTF()

    private val nullMaterial = createNullTextureMaterial(gltfModel)

    private fun addObject(objectData: ObjectData, buffer: ByteChunkBuffer): Int {
        val materialBuffer = objectMap[objectData]!!

        val positionsAccessor = addAccessorForFloats(materialBuffer.positions, buffer)
        var texCoordsAccessor: Int? = null
        var colorsAccessor: Int? = null

        if (objectData.materialId != -1) {
            texCoordsAccessor = addAccessorForFloats(materialBuffer.texcoords!!, buffer)
        } else {
            colorsAccessor = addAccessorForFloats(materialBuffer.colors!!, buffer)
        }

        // primitive attributes
        val attributes = Attributes(positionsAccessor, texCoordsAccessor, colorsAccessor)

        // primitive
        val primitives = ArrayList<Primitive>()
        primitives.add(Primitive(attributes, rsIndexToMaterialIndex[objectData.materialId] ?: nullMaterial))

        // mesh
        val mesh = Mesh(primitives)
        gltfModel.meshes.add(mesh)

        // node
        return gltfModel.meshes.size - 1
    }

    private fun addTile(tileData: TileData, buffer: ByteChunkBuffer): Int {
        val materialBuffer = tileMap[tileData]!!

        val positionsAccessor = addAccessorForFloats(materialBuffer.positions, buffer)
        var texCoordsAccessor: Int? = null
        var colorsAccessor: Int? = null

        if (tileData.materialId != -1) {
            texCoordsAccessor = addAccessorForFloats(materialBuffer.texcoords!!, buffer)
        } else {
            colorsAccessor = addAccessorForFloats(materialBuffer.colors!!, buffer)
        }

        // primitive attributes
        val attributes = Attributes(positionsAccessor, texCoordsAccessor, colorsAccessor)

        // primitive
        val primitives = ArrayList<Primitive>()
        primitives.add(Primitive(attributes, rsIndexToMaterialIndex[tileData.materialId] ?: nullMaterial))

        // mesh
        val mesh = Mesh(primitives)
        gltfModel.meshes.add(mesh)

        // node
        return gltfModel.meshes.size - 1
    }

    private fun addAccessorForFloats(
        floatBuffer: FloatVectorBuffer,
        buffer: ByteChunkBuffer
    ): Int {
        val floatsByteChunkBuffer = floatBuffer.getByteChunks()

        // buffer view
        val bufferView = BufferView(0, bufferFileOffset + buffer.byteLength, floatsByteChunkBuffer.byteLength)
        gltfModel.bufferViews.add(bufferView)

        buffer.addBytes(floatsByteChunkBuffer)

        // accessor
        val accessorType = when (floatBuffer.dims) {
            4 -> AccessorType.VEC4
            3 -> AccessorType.VEC3
            2 -> AccessorType.VEC2
            else -> throw UnsupportedOperationException()
        }

        val accessor = Accessor(
            gltfModel.bufferViews.size - 1,
            accessorType,
            floatBuffer.size,
            floatBuffer.min,
            floatBuffer.max
        )
        gltfModel.accessors.add(accessor)

        return gltfModel.accessors.size - 1
    }

    override fun getOrCreateBuffersForTile(z: Int, materialId: Int): ObjectBuffers = tileMap.getOrPut(TileData(z, materialId)) {
        ObjectBuffers(materialId >= 0)
    }

    override fun getOrCreateBuffersForObject(z: Int, objectType: Int, objectId: Int, cacheIndex:Int, materialId: Int): ObjectBuffers = objectMap.getOrPut(
        ObjectData(z, objectType, objectId, cacheIndex, materialId)
    ) {
        ObjectBuffers(materialId >= 0)
    }

    override fun addTextureMaterial(rsIndex: Int, imagePath: String) {
        if (rsIndexToMaterialIndex.containsKey(rsIndex)) return
        rsIndexToMaterialIndex[rsIndex] = gltfModel.materials.size

        val material = Material(gltfModel.textures.size, name = "rs_texture_$rsIndex")
        gltfModel.materials.add(material)

        val texture = Texture(gltfModel.images.size)
        gltfModel.textures.add(texture)

        val image = Image(imagePath)
        gltfModel.images.add(image)
    }

    private fun createNullTextureMaterial(gltfModel: glTF): Int {
        val material = Material(null, name = "rs_untextured")
        gltfModel.materials.add(material)
        return gltfModel.materials.size - 1
    }

    override fun flush(name: String) {
        if (objectMap.isNotEmpty() || tileMap.isNotEmpty()) {
            val objectNodes = ArrayList<Pair<ObjectData, Int>>()
            for (objectData in objectMap.keys) {
                objectNodes.add(Pair(objectData, addObject(objectData, chunkBuffer)))
            }

            val tileNodes = ArrayList<Pair<TileData, Int>>()
            for (tileData in tileMap.keys) {
                tileNodes.add(Pair(tileData, addTile(tileData, chunkBuffer)))
            }

            // Now that the meshes have been added, clear old buffers out
            objectMap.clear()
            tileMap.clear()

            val objectIndices = objectNodes.indices.map { Pair(objectNodes.get(it).first, objectNodes.get(it).second) }
            val tileIndices = tileNodes.indices.map { Pair(tileNodes.get(it).first, tileNodes.get(it).second) }

            val heightObjectTypeNodeMap = HashMap<Int, HashMap<Int, ArrayList<Triple<Int, Int, Int>>>>()

            for (objectData in objectIndices) {
                val heightMap = heightObjectTypeNodeMap.getOrPut(objectData.first.z) { HashMap<Int, ArrayList<Triple<Int, Int, Int>>>() }
                val typeList = heightMap.getOrPut(objectData.first.objectTypeId) { ArrayList<Triple<Int, Int, Int>>() }
                typeList.add(Triple(objectData.first.objectId, objectData.second, objectData.first.cacheIndex))
            }

            for (tileData in tileIndices) {
                val heightMap = heightObjectTypeNodeMap.getOrPut(tileData.first.z) { HashMap<Int, ArrayList<Triple<Int, Int, Int>>>() }
                val typeList = heightMap.getOrPut(-1) { ArrayList<Triple<Int, Int, Int>>() }
                typeList.add(Triple(-1, tileData.second, -1))
            }

            val heights = ArrayList<Node>()
            for (height in heightObjectTypeNodeMap.keys) {
                val objectTypeNodeMap = heightObjectTypeNodeMap[height]
                val objectTypeNodes = ArrayList<Node>()
                for (objectType in objectTypeNodeMap!!.keys) {
                    if (objectType == -1) {
                        // Tiles
                        for (i in 0 until objectTypeNodeMap[objectType]!!.count()){
                            val tileNode = Node(mesh = objectTypeNodeMap[objectType]!![i].second, name = "obj_type_tiles_" + i)
                            objectTypeNodes.add(tileNode)
                        }
                        continue
                    }
                    val objectTypeList = objectTypeNodeMap[objectType]
                    val objectNodes = ArrayList<Node>()
                    for (obj in objectTypeList!!) {
                        objectNodes.add(Node(mesh = obj.second, name = "obj_" + obj.first + "_index_" + obj.third))
                    }
                    val objectIndices = objectNodes.indices.map { it + gltfModel.nodes.size }
                    gltfModel.nodes.addAll(objectNodes)
                    objectTypeNodes.add(Node(children = objectIndices, name = "obj_type_" + objectType))
                }
                val objectTypeIndices = objectTypeNodes.indices.map { it + gltfModel.nodes.size }
                gltfModel.nodes.addAll(objectTypeNodes)
                heights.add(Node(children = objectTypeIndices, name = "height_" + height))
            }

            val heightIndices = heights.indices.map { it + gltfModel.nodes.size }
            gltfModel.nodes.addAll(heights)

            val sceneNode = Node(
                name = name,
                children = heightIndices
            )

            // Add parent node into scene
            gltfModel.nodes.add(sceneNode)
            sceneNodes.add(gltfModel.nodes.size - 1)

            // Flush buffers to data file
            chunkWriteListeners.forEach { it.onStartRegion(flushingRegionNum) }
            chunkBuffer.getBuffers().forEach { buf ->
                val dataLength = buf.remaining().toLong()
                dataFile.write(buf)
                bufferFileOffset += dataLength
            }
            chunkBuffer.clear()
            chunkWriteListeners.forEach { it.onEndRegion() }
        }

        flushingRegionNum++
    }

    override fun finish() {
        flush("")
        dataFile.close()

        // setup single scene
        gltfModel.scenes.add(Scene(sceneNodes))

        // create buffer
        val buffer = Buffer(dataFilename, byteLength = bufferFileOffset)
        gltfModel.buffers.add(buffer)

        // indicate start of gltf file
        chunkWriteListeners.forEach { it.onStartRegion(-1) }

        // convert to JSON
        val mapper = ObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
        val json = mapper.writeValueAsString(gltfModel)

        // write glTf file
        File("$directory/scene.gltf").printWriter().use {
            it.write(json)
        }
        chunkWriteListeners.forEach { it.onFinishWriting() }
    }

    companion object {
        private const val dataFilename = "data.bin"
    }
}
