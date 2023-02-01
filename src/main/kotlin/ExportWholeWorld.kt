import models.StartupOptions
import models.config.ConfigOptions
import models.config.Configuration
import utils.Utils

// Chunk Coordinates size
@Suppress("ConstPropertyName")
private const val ChunkCoordinatesSize = 64

fun main() {
    val debug = true
    // TODO: Optimize! Can merge generating + exporting if we really care about eff.
    val regionIds = generateRegionIds(debug = debug).reversed().sortListForExport()
    if (debug) {
        println("\n")
    }

    if (debug) {
        regionIds.forEach {
            println(it.toString())
        }
    }

    val configOptions = ConfigOptions(Configuration())
    val startupOptions = StartupOptions(configOptions)

    val xIncrementSize = 4
    val yIncrementSize = 4

    var xIndex = 0

    val maxXIndex = regionIds.size
    val maxYIndex = regionIds.first().size

    while (xIndex < maxXIndex) {
        var yIndex = 0
        val xIndexIncremented = (xIndex + xIncrementSize).coerceAtMost(maxXIndex)
        while (yIndex < maxYIndex) {
            val yIndexIncremented = (yIndex + yIncrementSize).coerceAtMost(maxYIndex)

            val newList = regionIds.subSet(
                xInitialIndex = xIndex,
                yInitialIndex = yIndex,
                yFinalIndex = yIndexIncremented,
                xFinalIndex = xIndexIncremented
            )

            if (debug) {
                newList.forEach {
                    println("$it")
                }
                println("\n")
            }

            CliExporter(startupOptions, exportAbsoluteCoordinates = true).exportRegions(newList)

            yIndex = yIndexIncremented
        }
        xIndex = xIndexIncremented
    }
}

private fun generateRegionIds(
    initialX: Int = 1152, // Initial X and Y world coordinates, bottom left of world
    initialY: Int = 2496,
    numXChunks: Int = 42, // Number of X and Y chunks to load
    numYChunks: Int = 25,
    debug: Boolean = true
): List<List<Int>> {
    val regionIds: List<List<Int>> = List(numXChunks) { xIndex ->
        List(numYChunks) { yIndex ->
            val xCoordinate = ChunkCoordinatesSize * xIndex + initialX
            val yCoordinate = ChunkCoordinatesSize * yIndex + initialY
            Utils.worldCoordinatesToRegionId(x = xCoordinate, y = yCoordinate)
        }
    }

    if (debug) {
        regionIds.forEach {
            println(it.toString())
        }
    }

    return regionIds
}

private fun List<List<Int>>.subSet(
    xInitialIndex: Int,
    xFinalIndex: Int,
    yInitialIndex: Int,
    yFinalIndex: Int
): List<List<Int>> {
    val returnList: MutableList<List<Int>> = mutableListOf()

    (xInitialIndex until xFinalIndex).forEach { xIndex ->
        returnList.add(this[xIndex].subList(yInitialIndex, yFinalIndex))
    }

    return returnList
}

private fun List<List<Int>>.sortListForExport(
): List<List<Int>> {
    val initialXSize = size
    val initialYSize = first().size

    return List(initialYSize) { xIndex ->
        List(initialXSize) { yIndex ->
            this[yIndex][xIndex]
        }.reversed()
    }
}