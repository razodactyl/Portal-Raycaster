
data class PaintContext(val wallHeight: Int,
                        val start: Int,
                        val end: Int,
                        val clipped: Boolean)

class RenderUtil(private val buffer: IntArray,
                 val width: Int,
                 val height: Int) {

    fun calcDrawStartEnd(ray: Ray): PaintContext {
        val wallHeight = (height / ray.distance).toInt()
        var drawStart = -(wallHeight / 2) + (height / 2)
        if (drawStart < 0) drawStart = 0

        var drawEnd = (wallHeight / 2) + (height / 2) + 1
        var clipped = false
        if (drawEnd >= height) {
            clipped = true
            drawEnd = height - 1
        }
        return PaintContext(wallHeight, drawStart, drawEnd, clipped)
    }

    fun paint(rays: List<Ray>, x: Int) {

        // DEBUG
        //paintLine(x, 0, height - 1, 0)

        val lastRay = rays.last()

        paintWall(lastRay, x)
        paintFloors(rays, x)

    }

    /**
     * Paints a single wall column with shading from the context of a ray
     */
    fun paintWall(ray: Ray, x: Int) {
        var intensity = if (ray.side == 0) 255 else 150

        intensity -= (ray.distance * 2).toInt()
        if (intensity < 0) {
            intensity = 0
        }

        val shade = if (ray.wallHitDirection == Direction.NORTH) {
            Color.color(0, 0, intensity)
        } else if (ray.wallHitDirection == Direction.SOUTH) {
            Color.color(0, intensity, 0)
        } else if (ray.wallHitDirection == Direction.EAST) {
            Color.color(intensity, 0, 0)
        } else {
            Color.color(0, intensity, intensity)
        }

        var (height, drawStart, drawEnd, clipped) = calcDrawStartEnd(ray)

        // Paint fake SSAO effect to top and bottom of wall
//        val fractionOfWallHeight = 25
//        val pxOffset = ((height / fractionOfWallHeight) - 1) + 1
//
//        var darkenAmount = 40
//        var decreseRatio = (darkenAmount / pxOffset)
//        for (y in drawStart until drawStart + pxOffset) {
//            darkenAmount = if (clipped) 0 else darkenAmount - decreseRatio
//            buffer[y * width + x] = Color.darken(shade, darkenAmount)
//        }
        for (y in drawStart until drawEnd) {
            buffer[y * width + x] = shade
        }
    }


    /**
     * Paints floors and floors through portals
     */
    fun paintFloors(rays: List<Ray>, x: Int) {
        val lastRay = rays.last()

        // Reverse the list so start with the furthest away ray
        val reversedRays = rays.reversed()
        for (i in reversedRays.indices) {

            val first = reversedRays[i]
            val second = reversedRays.getOrElse(i + 1) {
                lastRay
            }

            val (_, _, firstDrawEnd, _) = calcDrawStartEnd(first)
            var (_, _, secondDrawEnd, _) = calcDrawStartEnd(second)

            if (second == lastRay) {
                secondDrawEnd = height - 1
            }

            paintFloor(first, x, firstDrawEnd, secondDrawEnd)
        }
    }

    /**
     * Paints a single floor column given the context of 1 ray. (portals excluded)
     */
    private fun  paintFloor(ray: Ray, x: Int, wallDrawEnd: Int, stopHeight: Int) {
        var floorXWall: Double
        var floorYWall: Double

        if(ray.side == 0 && ray.direction.x > 0) {
            floorXWall = ray.mapX.toDouble()
            floorYWall = ray.mapY + ray.wallX
        } else if(ray.side == 0 && ray.direction.x < 0) {
            floorXWall = ray.mapX + 1.0
            floorYWall = ray.mapY + ray.wallX
        } else if(ray.side == 1 && ray.direction.y > 0) {
            floorXWall = ray.mapX + ray.wallX
            floorYWall = ray.mapY.toDouble()
        } else {
            floorXWall = ray.mapX + ray.wallX
            floorYWall = ray.mapY + 1.0
        }

        val distWall = ray.distance
        var currentDist: Double

        var drawEnd = wallDrawEnd
        if (drawEnd < 0) drawEnd = stopHeight

        for (y in drawEnd until stopHeight) {
            currentDist = height / (2.0 * y - height)

            val weight = currentDist / distWall

            val currentFloorX = weight * floorXWall + (1.0 - weight) * ray.origin.x
            val currentFloorY = weight * floorYWall + (1.0 - weight) * ray.origin.y

            val checkerBoardPattern = ((currentFloorX).toInt() + (currentFloorY).toInt()) % 2

            val floorColor = if(checkerBoardPattern == 0) Color.color(119, 119, 119)
            else                         Color.color(51, 51, 51)

            val ceilingColor = if(checkerBoardPattern == 0) Color.color(119, 119, 119)
            else                         Color.color(226, 226, 226)

            buffer[(y * width + x)] = floorColor
            buffer[(height - y) * width + x] = ceilingColor
        }
    }

}