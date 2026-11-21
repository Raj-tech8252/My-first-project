package com.example.engine3d

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Mesh(
    vertices: FloatArray,
    val vertexCount: Int
) {
    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertices)
            position(0)
        }

    private var vboId: Int = 0

    fun uploadIfNeeded() {
        if (vboId != 0) return
        val ids = IntArray(1)
        GLES20.glGenBuffers(1, ids, 0)
        vboId = ids[0]
        if (vboId != 0) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
            vertexBuffer.position(0)
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                verticesByteSize(),
                vertexBuffer,
                GLES20.GL_STATIC_DRAW
            )
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
    }

    private fun verticesByteSize(): Int = vertexCount * STRIDE_BYTES

    fun draw(posHandle: Int, normHandle: Int, uvHandle: Int) {
        uploadIfNeeded()
        if (vboId != 0) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
            GLES20.glEnableVertexAttribArray(posHandle)
            GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0)
            GLES20.glEnableVertexAttribArray(normHandle)
            GLES20.glVertexAttribPointer(normHandle, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, 12)
            GLES20.glEnableVertexAttribArray(uvHandle)
            GLES20.glVertexAttribPointer(uvHandle, 2, GLES20.GL_FLOAT, false, STRIDE_BYTES, 24)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        } else {
            vertexBuffer.position(0)
            GLES20.glEnableVertexAttribArray(posHandle)
            GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, vertexBuffer)
            vertexBuffer.position(3)
            GLES20.glEnableVertexAttribArray(normHandle)
            GLES20.glVertexAttribPointer(normHandle, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, vertexBuffer)
            vertexBuffer.position(6)
            GLES20.glEnableVertexAttribArray(uvHandle)
            GLES20.glVertexAttribPointer(uvHandle, 2, GLES20.GL_FLOAT, false, STRIDE_BYTES, vertexBuffer)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        }
    }

    companion object {
        const val FLOATS_PER_VERTEX = 8
        const val STRIDE_BYTES = FLOATS_PER_VERTEX * 4
    }
}

class MeshBuilder(initialCapacityFloats: Int = 2048) {
    private var data = FloatArray(initialCapacityFloats)
    private var count = 0

    private fun ensureCapacity(extra: Int) {
        if (count + extra > data.size) {
            val newSize = (data.size * 2).coerceAtLeast(count + extra)
            data = data.copyOf(newSize)
        }
    }

    fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float, v: Float) {
        ensureCapacity(8)
        data[count++] = x
        data[count++] = y
        data[count++] = z
        data[count++] = nx
        data[count++] = ny
        data[count++] = nz
        data[count++] = u
        data[count++] = v
    }

    fun addQuad(
        x0: Float, y0: Float, z0: Float, u0: Float, v0: Float,
        x1: Float, y1: Float, z1: Float, u1: Float, v1: Float,
        x2: Float, y2: Float, z2: Float, u2: Float, v2: Float,
        x3: Float, y3: Float, z3: Float, u3: Float, v3: Float,
        nx: Float, ny: Float, nz: Float
    ) {
        addVertex(x0, y0, z0, nx, ny, nz, u0, v0)
        addVertex(x1, y1, z1, nx, ny, nz, u1, v1)
        addVertex(x2, y2, z2, nx, ny, nz, u2, v2)

        addVertex(x0, y0, z0, nx, ny, nz, u0, v0)
        addVertex(x2, y2, z2, nx, ny, nz, u2, v2)
        addVertex(x3, y3, z3, nx, ny, nz, u3, v3)
    }

    fun addGroundQuad(
        cx: Float, y: Float, cz: Float,
        width: Float, length: Float,
        uScale: Float = 1f, vScale: Float = 1f
    ) {
        val hw = width * 0.5f
        val hl = length * 0.5f
        addQuad(
            cx - hw, y, cz - hl, 0f, 0f,
            cx - hw, y, cz + hl, 0f, vScale,
            cx + hw, y, cz + hl, uScale, vScale,
            cx + hw, y, cz - hl, uScale, 0f,
            0f, 1f, 0f
        )
    }

    fun addBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        uScale: Float = 1f, vScale: Float = 1f
    ) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz

        // Front (+Z)
        addQuad(x0, y0, z1, 0f, vScale, x1, y0, z1, uScale, vScale, x1, y1, z1, uScale, 0f, x0, y1, z1, 0f, 0f, 0f, 0f, 1f)
        // Back (-Z)
        addQuad(x1, y0, z0, 0f, vScale, x0, y0, z0, uScale, vScale, x0, y1, z0, uScale, 0f, x1, y1, z0, 0f, 0f, 0f, 0f, -1f)
        // Right (+X)
        addQuad(x1, y0, z1, 0f, vScale, x1, y0, z0, uScale, vScale, x1, y1, z0, uScale, 0f, x1, y1, z1, 0f, 0f, 1f, 0f, 0f)
        // Left (-X)
        addQuad(x0, y0, z0, 0f, vScale, x0, y0, z1, uScale, vScale, x0, y1, z1, uScale, 0f, x0, y1, z0, 0f, 0f, -1f, 0f, 0f)
        // Top (+Y)
        addQuad(x0, y1, z1, 0f, vScale, x1, y1, z1, uScale, vScale, x1, y1, z0, uScale, 0f, x0, y1, z0, 0f, 0f, 0f, 1f, 0f)
        // Bottom (-Y)
        addQuad(x0, y0, z0, 0f, vScale, x1, y0, z0, uScale, vScale, x1, y0, z1, uScale, 0f, x0, y0, z1, 0f, 0f, 0f, -1f, 0f)
    }

    fun addRotatedBoxX(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        pitchRad: Float
    ) {
        val cosP = cos(pitchRad)
        val sinP = sin(pitchRad)
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f

        fun tx(lx: Float, ly: Float, lz: Float): Triple<Float, Float, Float> {
            val ry = ly * cosP - lz * sinP
            val rz = ly * sinP + lz * cosP
            return Triple(cx + lx, cy + ry, cz + rz)
        }

        fun tn(nx: Float, ny: Float, nz: Float): Triple<Float, Float, Float> {
            val ry = ny * cosP - nz * sinP
            val rz = ny * sinP + nz * cosP
            return Triple(nx, ry, rz)
        }

        fun face(
            a: Triple<Float, Float, Float>,
            b: Triple<Float, Float, Float>,
            c: Triple<Float, Float, Float>,
            d: Triple<Float, Float, Float>,
            n: Triple<Float, Float, Float>
        ) {
            addQuad(
                a.first, a.second, a.third, 0f, 1f,
                b.first, b.second, b.third, 1f, 1f,
                c.first, c.second, c.third, 1f, 0f,
                d.first, d.second, d.third, 0f, 0f,
                n.first, n.second, n.third
            )
        }

        val p000 = tx(-hx, -hy, -hz); val p100 = tx(hx, -hy, -hz)
        val p110 = tx(hx, hy, -hz); val p010 = tx(-hx, hy, -hz)
        val p001 = tx(-hx, -hy, hz); val p101 = tx(hx, -hy, hz)
        val p111 = tx(hx, hy, hz); val p011 = tx(-hx, hy, hz)

        face(p001, p101, p111, p011, tn(0f, 0f, 1f))
        face(p100, p000, p010, p110, tn(0f, 0f, -1f))
        face(p101, p100, p110, p111, tn(1f, 0f, 0f))
        face(p000, p001, p011, p010, tn(-1f, 0f, 0f))
        face(p011, p111, p110, p010, tn(0f, 1f, 0f))
        face(p000, p100, p101, p001, tn(0f, -1f, 0f))
    }

    /**
     * Adds a cylinder aligned along the X axis (great for wheels & handlebars)
     */
    fun addWheelX(cx: Float, cy: Float, cz: Float, radius: Float, width: Float, segments: Int = 16) {
        val hw = width * 0.5f
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segments) * (2f * PI.toFloat())
            val y0 = cos(a0) * radius; val z0 = sin(a0) * radius
            val y1 = cos(a1) * radius; val z1 = sin(a1) * radius
            val ny = (cos(a0) + cos(a1)) * 0.5f
            val nz = (sin(a0) + sin(a1)) * 0.5f
            val nLen = sqrt(ny * ny + nz * nz).coerceAtLeast(0.001f)

            // Tread quad
            addQuad(
                cx - hw, cy + y0, cz + z0, 0f, 0f,
                cx + hw, cy + y0, cz + z0, 1f, 0f,
                cx + hw, cy + y1, cz + z1, 1f, 1f,
                cx - hw, cy + y1, cz + z1, 0f, 1f,
                0f, ny / nLen, nz / nLen
            )
            // Left cap (-X)
            addVertex(cx - hw, cy, cz, -1f, 0f, 0f, 0.5f, 0.5f)
            addVertex(cx - hw, cy + y1, cz + z1, -1f, 0f, 0f, 0.5f + 0.5f * cos(a1), 0.5f + 0.5f * sin(a1))
            addVertex(cx - hw, cy + y0, cz + z0, -1f, 0f, 0f, 0.5f + 0.5f * cos(a0), 0.5f + 0.5f * sin(a0))
            // Right cap (+X)
            addVertex(cx + hw, cy, cz, 1f, 0f, 0f, 0.5f, 0.5f)
            addVertex(cx + hw, cy + y0, cz + z0, 1f, 0f, 0f, 0.5f + 0.5f * cos(a0), 0.5f + 0.5f * sin(a0))
            addVertex(cx + hw, cy + y1, cz + z1, 1f, 0f, 0f, 0.5f + 0.5f * cos(a1), 0.5f + 0.5f * sin(a1))
        }
    }

    /**
     * Vertical cylinder along Y axis (poles, tree trunks, exhaust stacks, rider limbs)
     */
    fun addCylinderY(cx: Float, yBottom: Float, cz: Float, radiusBottom: Float, radiusTop: Float, height: Float, segments: Int = 10) {
        val yTop = yBottom + height
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segments) * (2f * PI.toFloat())
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val nx = (c0 + c1) * 0.5f
            val nz = (s0 + s1) * 0.5f
            val len = sqrt(nx * nx + nz * nz).coerceAtLeast(0.001f)

            addQuad(
                cx + c0 * radiusBottom, yBottom, cz + s0 * radiusBottom, 0f, 1f,
                cx + c1 * radiusBottom, yBottom, cz + s1 * radiusBottom, 1f, 1f,
                cx + c1 * radiusTop, yTop, cz + s1 * radiusTop, 1f, 0f,
                cx + c0 * radiusTop, yTop, cz + s0 * radiusTop, 0f, 0f,
                nx / len, 0.15f, nz / len
            )
            if (radiusTop > 0.005f) {
                addVertex(cx, yTop, cz, 0f, 1f, 0f, 0.5f, 0.5f)
                addVertex(cx + c0 * radiusTop, yTop, cz + s0 * radiusTop, 0f, 1f, 0f, 0.5f, 0.5f)
                addVertex(cx + c1 * radiusTop, yTop, cz + s1 * radiusTop, 0f, 1f, 0f, 0.5f, 0.5f)
            }
        }
    }

    /**
     * Horizontal cylinder along Z axis (exhaust pipe, bike frame tubes, headlights)
     */
    fun addCylinderZ(cx: Float, cy: Float, zStart: Float, zEnd: Float, radius: Float, segments: Int = 10) {
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * (2f * PI.toFloat())
            val a1 = ((i + 1).toFloat() / segments) * (2f * PI.toFloat())
            val x0 = cos(a0) * radius; val y0 = sin(a0) * radius
            val x1 = cos(a1) * radius; val y1 = sin(a1) * radius
            val nx = (cos(a0) + cos(a1)) * 0.5f
            val ny = (sin(a0) + sin(a1)) * 0.5f
            val len = sqrt(nx * nx + ny * ny).coerceAtLeast(0.001f)

            addQuad(
                cx + x0, cy + y0, zStart, 0f, 0f,
                cx + x0, cy + y0, zEnd, 1f, 0f,
                cx + x1, cy + y1, zEnd, 1f, 1f,
                cx + x1, cy + y1, zStart, 0f, 1f,
                nx / len, ny / len, 0f
            )
            // Front/back caps
            addVertex(cx, cy, zEnd, 0f, 0f, 1f, 0.5f, 0.5f)
            addVertex(cx + x0, cy + y0, zEnd, 0f, 0f, 1f, 0.5f, 0.5f)
            addVertex(cx + x1, cy + y1, zEnd, 0f, 0f, 1f, 0.5f, 0.5f)

            addVertex(cx, cy, zStart, 0f, 0f, -1f, 0.5f, 0.5f)
            addVertex(cx + x1, cy + y1, zStart, 0f, 0f, -1f, 0.5f, 0.5f)
            addVertex(cx + x0, cy + y0, zStart, 0f, 0f, -1f, 0.5f, 0.5f)
        }
    }

    /**
     * Wedge stunt ramp sloping upward along +Z or -Z (directionZ = +1 or -1)
     */
    fun addRamp(cx: Float, cz: Float, width: Float, length: Float, height: Float, slopeTowardPositiveZ: Boolean) {
        val hw = width * 0.5f
        val hl = length * 0.5f
        val zLow = if (slopeTowardPositiveZ) cz - hl else cz + hl
        val zHigh = if (slopeTowardPositiveZ) cz + hl else cz - hl
        val slopeNy = length / sqrt(length * length + height * height)
        val slopeNz = (if (slopeTowardPositiveZ) -height else height) / sqrt(length * length + height * height)

        // Sloped top surface
        if (slopeTowardPositiveZ) {
            addQuad(
                cx - hw, 0.02f, zLow, 0f, 1f,
                cx + hw, 0.02f, zLow, 1f, 1f,
                cx + hw, height, zHigh, 1f, 0f,
                cx - hw, height, zHigh, 0f, 0f,
                0f, slopeNy, slopeNz
            )
            // Back vertical wall at zHigh
            addQuad(
                cx - hw, 0f, zHigh, 0f, 1f,
                cx + hw, 0f, zHigh, 1f, 1f,
                cx + hw, height, zHigh, 1f, 0f,
                cx - hw, height, zHigh, 0f, 0f,
                0f, 0f, 1f
            )
        } else {
            addQuad(
                cx - hw, height, zHigh, 0f, 0f,
                cx + hw, height, zHigh, 1f, 0f,
                cx + hw, 0.02f, zLow, 1f, 1f,
                cx - hw, 0.02f, zLow, 0f, 1f,
                0f, slopeNy, slopeNz
            )
            addQuad(
                cx + hw, 0f, zHigh, 0f, 1f,
                cx - hw, 0f, zHigh, 1f, 1f,
                cx - hw, height, zHigh, 1f, 0f,
                cx + hw, height, zHigh, 0f, 0f,
                0f, 0f, -1f
            )
        }
        // Left & right side triangles
        addVertex(cx - hw, 0f, zLow, -1f, 0f, 0f, 0f, 1f)
        addVertex(cx - hw, 0f, zHigh, -1f, 0f, 0f, 1f, 1f)
        addVertex(cx - hw, height, zHigh, -1f, 0f, 0f, 1f, 0f)

        addVertex(cx + hw, 0f, zLow, 1f, 0f, 0f, 0f, 1f)
        addVertex(cx + hw, height, zHigh, 1f, 0f, 0f, 1f, 0f)
        addVertex(cx + hw, 0f, zHigh, 1f, 0f, 0f, 1f, 1f)
    }

    fun addPyramidRoof(cx: Float, yBase: Float, cz: Float, width: Float, length: Float, height: Float) {
        val hw = width * 0.5f
        val hl = length * 0.5f
        val topY = yBase + height
        val x0 = cx - hw; val x1 = cx + hw
        val z0 = cz - hl; val z1 = cz + hl

        fun tri(ax: Float, az: Float, bx: Float, bz: Float, nx: Float, nz: Float) {
            addVertex(ax, yBase, az, nx, 0.5f, nz, 0f, 1f)
            addVertex(bx, yBase, bz, nx, 0.5f, nz, 1f, 1f)
            addVertex(cx, topY, cz, nx, 0.5f, nz, 0.5f, 0f)
        }
        tri(x0, z1, x1, z1, 0f, 1f)
        tri(x1, z1, x1, z0, 1f, 0f)
        tri(x1, z0, x0, z0, 0f, -1f)
        tri(x0, z0, x0, z1, -1f, 0f)
    }

    fun build(): Mesh {
        val trimmed = data.copyOf(count)
        return Mesh(trimmed, count / Mesh.FLOATS_PER_VERTEX)
    }
}
