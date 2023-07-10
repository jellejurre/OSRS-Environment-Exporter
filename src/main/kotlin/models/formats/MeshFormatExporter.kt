package models.formats

/** A buffered 3D mesh file format writer. */
interface MeshFormatExporter {
    /** (Create and) retrieve the buffers for the given tile.
     *
     *  @param z The RuneScape z value for the object.
     *  @param materialId The RuneScape material ID, or a negative value for flat colours.
     *  @return The buffers for the given material ID.
     */
    fun getOrCreateBuffersForTile(z: Int, materialId: Int): ObjectBuffers

    /** (Create and) retrieve the buffers for the given Object.
     *
     *  @param z The RuneScape z value for the object.
     *  @param objectType The RuneScape object type ID.
     *  @param objectId The RuneScape object ID.
     *  @param cacheIndex The index of the objcet in the runescape cache.
     *  @param materialId The RuneScape material ID, or a negative value for flat colours.
     *  @param rotateAngle The angle of rotation, clockwise.
     *  @param flipZ Whether or not it's flipped on the z axis before rotating.
     *  @return The buffers for the given material ID.
     */
    fun getOrCreateBuffersForObject(z: Int, objectType: Int, objectId: Int, cacheIndex: Int, materialId: Int, rotateAngle: Int, flipZ: Boolean): ObjectBuffers

    /** Assigns a texture to the given RuneScape material ID.
     *  Subsequent calls with the same material ID will be ignored.
     *
     *  @param rsIndex The RuneScape material ID.
     *  @param imagePath The path to the texture image file.
     */
    fun addTextureMaterial(rsIndex: Int, imagePath: String)

    /** Flush buffers to file.
     *  May be ignored depending on implementation if partial writes are not supported.
     *  The material buffers in memory may be cleared after calling this method.
     *
     *  @param name The name to give the flushed data (if it is given a separate object/mesh/etc)
     */
    fun flush(name: String)

    /** Finish saving buffers to file. This will write any unwritten metadata etc. */
    fun finish()
}
