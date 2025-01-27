/* Derived from RuneLite source code, which is licensed as follows:
 *
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package cache.definitions

class ObjectDefinition {
    var id = 0
    var retextureToFind: ShortArray? = null
    var decorDisplacement = 16
    var isHollow = false
    var name = "null"
    var modelIds: IntArray? = null
    var modelTypes: IntArray? = null
    var recolorToFind: ShortArray? = null
    var mapAreaId = -1
    var textureToReplace: ShortArray? = null
    var sizeX = 1
    var sizeY = 1
    var anInt2083 = 0
    var anIntArray2084: IntArray? = null
    var offsetX = 0
    var mergeNormals = false
    var wallOrDoor = -1
    var animationID = -1
    var transformVarbit = -1
    var ambient = 0
    var contrast = 0
    var actions = arrayOfNulls<String>(5)
    var interactType = 2
    var mapSceneID = -1
    var blockingMask = 0
    lateinit var recolorToReplace: ShortArray
    var shadow = true
    var modelSizeX = 128
    var modelSizeHeight = 128
    var modelSizeY = 128
    var objectID = 0
    var offsetHeight = 0
    var offsetY = 0
    var obstructsGround = false
    var contouredGround = -1
    var supportsItems = -1
    lateinit var transforms: IntArray
    var isRotated = false
    var transformVarp = -1
    var ambientSoundId = -1
    var aBool2111 = false
    var anInt2112 = 0
    var anInt2113 = 0
    var blocksProjectile = true
    var params: Map<Int, Any>? = null

    override fun toString(): String {
        return "ObjectDefinition($javaClass, name: $name, id: $id, contouredGround: $contouredGround)"
    }
}
