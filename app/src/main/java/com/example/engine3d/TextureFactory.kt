package com.example.engine3d

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.GLES20
import android.opengl.GLUtils
import kotlin.random.Random

enum class TexId {
    WHITE,
    HIGHWAY_ROAD,
    CITY_ROAD,
    VILLAGE_DIRT,
    GRASS_FIELD,
    BUILDING_CITY,
    SHOP_AWNING,
    TEMPLE_STONE,
    PETROL_CANOPY,
    RAILWAY_TRACK,
    TRUCK_DECOR,
    RAMP_CHEVRON
}

class TextureFactory {
    private val textures = mutableMapOf<TexId, Int>()

    fun initAll() {
        textures.clear()
        TexId.entries.forEach { id ->
            val bmp = generateBitmap(id)
            textures[id] = uploadTexture(bmp)
            bmp.recycle()
        }
    }

    fun get(id: TexId): Int = textures[id] ?: 0

    private fun uploadTexture(bitmap: Bitmap): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        val texId = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        return texId
    }

    private fun generateBitmap(id: TexId): Bitmap {
        val size = if (id == TexId.WHITE) 4 else 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rng = Random(id.ordinal * 997 + 42)

        when (id) {
            TexId.WHITE -> {
                c.drawColor(Color.WHITE)
            }
            TexId.HIGHWAY_ROAD -> {
                c.drawColor(Color.rgb(42, 46, 52))
                // Asphalt grain & subtle patches
                for (i in 0..900) {
                    val v = rng.nextInt(32, 62)
                    p.color = Color.rgb(v, v + 2, v + 5)
                    c.drawRect(rng.nextFloat() * 256f, rng.nextFloat() * 256f, rng.nextFloat() * 256f + 3f, rng.nextFloat() * 256f + 3f, p)
                }
                // Yellow side shoulders
                p.color = Color.rgb(235, 178, 25)
                c.drawRect(6f, 0f, 13f, 256f, p)
                c.drawRect(243f, 0f, 250f, 256f, p)
                // Double yellow highway divider
                c.drawRect(122f, 0f, 126f, 256f, p)
                c.drawRect(130f, 0f, 134f, 256f, p)
                // White dashed lane markings
                p.color = Color.rgb(238, 240, 244)
                for (y in 8 until 256 step 64) {
                    c.drawRect(64f, y.toFloat(), 70f, (y + 34).toFloat(), p)
                    c.drawRect(186f, y.toFloat(), 192f, (y + 34).toFloat(), p)
                }
            }
            TexId.CITY_ROAD -> {
                c.drawColor(Color.rgb(50, 53, 59))
                for (i in 0..700) {
                    val v = rng.nextInt(40, 68)
                    p.color = Color.rgb(v, v + 1, v + 3)
                    c.drawCircle(rng.nextFloat() * 256f, rng.nextFloat() * 256f, 1.5f, p)
                }
                // Concrete curbs
                p.color = Color.rgb(140, 138, 132)
                c.drawRect(0f, 0f, 12f, 256f, p)
                c.drawRect(244f, 0f, 256f, 256f, p)
                // White dashed center line
                p.color = Color.rgb(235, 235, 235)
                for (y in 10 until 256 step 58) {
                    c.drawRect(125f, y.toFloat(), 131f, (y + 30).toFloat(), p)
                }
            }
            TexId.VILLAGE_DIRT -> {
                c.drawColor(Color.rgb(156, 114, 72))
                for (i in 0..1000) {
                    val r = rng.nextInt(125, 178)
                    val g = (r * 0.72f).toInt()
                    val b = (r * 0.45f).toInt()
                    p.color = Color.rgb(r, g, b)
                    c.drawCircle(rng.nextFloat() * 256f, rng.nextFloat() * 256f, rng.nextFloat() * 4f + 1f, p)
                }
                // Wheel rut tracks
                p.color = Color.rgb(120, 86, 52)
                c.drawRect(66f, 0f, 88f, 256f, p)
                c.drawRect(168f, 0f, 190f, 256f, p)
            }
            TexId.GRASS_FIELD -> {
                c.drawColor(Color.rgb(74, 116, 48))
                for (i in 0..1200) {
                    val g = rng.nextInt(88, 142)
                    val r = rng.nextInt(54, 98)
                    val b = rng.nextInt(32, 62)
                    p.color = Color.rgb(r, g, b)
                    c.drawRect(rng.nextFloat() * 256f, rng.nextFloat() * 256f, rng.nextFloat() * 256f + 6f, rng.nextFloat() * 256f + 6f, p)
                }
            }
            TexId.BUILDING_CITY -> {
                c.drawColor(Color.rgb(212, 204, 192))
                // Ground floor shop shutters & colorful band
                p.color = Color.rgb(55, 68, 86)
                c.drawRect(0f, 196f, 256f, 256f, p)
                p.color = Color.rgb(225, 108, 28)
                c.drawRect(0f, 182f, 256f, 196f, p)
                // Upper floor windows
                for (row in 0..2) {
                    val y = 18f + row * 52f
                    for (col in 0..3) {
                        val x = 18f + col * 60f
                        p.color = Color.rgb(42, 54, 72)
                        c.drawRect(x, y, x + 36f, y + 32f, p)
                        p.color = if ((row + col) % 3 == 0) Color.rgb(255, 214, 128) else Color.rgb(108, 148, 184)
                        c.drawRect(x + 4f, y + 4f, x + 32f, y + 28f, p)
                    }
                }
            }
            TexId.SHOP_AWNING -> {
                c.drawColor(Color.rgb(232, 222, 202))
                for (i in 0 until 8) {
                    p.color = if (i % 2 == 0) Color.rgb(230, 81, 0) else Color.rgb(245, 245, 240)
                    c.drawRect(i * 32f, 0f, (i + 1) * 32f, 256f, p)
                }
            }
            TexId.TEMPLE_STONE -> {
                c.drawColor(Color.rgb(218, 166, 112))
                p.color = Color.rgb(184, 128, 76)
                p.strokeWidth = 3f
                for (y in 0 until 256 step 32) {
                    c.drawLine(0f, y.toFloat(), 256f, y.toFloat(), p)
                    val offset = if ((y / 32) % 2 == 0) 0 else 32
                    for (x in offset until 256 step 64) {
                        c.drawLine(x.toFloat(), y.toFloat(), x.toFloat(), (y + 32).toFloat(), p)
                    }
                }
                p.color = Color.rgb(235, 100, 15)
                c.drawRect(0f, 116f, 256f, 134f, p)
            }
            TexId.PETROL_CANOPY -> {
                c.drawColor(Color.rgb(236, 240, 245))
                p.color = Color.rgb(13, 71, 161)
                c.drawRect(0f, 0f, 256f, 96f, p)
                p.color = Color.rgb(255, 109, 0)
                c.drawRect(0f, 96f, 256f, 144f, p)
                p.color = Color.rgb(213, 0, 0)
                c.drawRect(0f, 144f, 256f, 168f, p)
            }
            TexId.RAILWAY_TRACK -> {
                c.drawColor(Color.rgb(92, 86, 80))
                for (i in 0..800) {
                    val v = rng.nextInt(70, 125)
                    p.color = Color.rgb(v, v - 4, v - 8)
                    c.drawCircle(rng.nextFloat() * 256f, rng.nextFloat() * 256f, 2f, p)
                }
                // Wooden sleepers across X
                p.color = Color.rgb(96, 62, 34)
                for (y in 12 until 256 step 32) {
                    c.drawRect(24f, y.toFloat(), 232f, (y + 14).toFloat(), p)
                }
                // Steel rails along Y
                p.color = Color.rgb(185, 194, 204)
                c.drawRect(56f, 0f, 70f, 256f, p)
                c.drawRect(186f, 0f, 200f, 256f, p)
            }
            TexId.TRUCK_DECOR -> {
                c.drawColor(Color.rgb(255, 193, 7))
                p.color = Color.rgb(211, 47, 47)
                c.drawRect(0f, 0f, 256f, 44f, p)
                p.color = Color.rgb(0, 137, 123)
                c.drawRect(0f, 44f, 256f, 196f, p)
                p.color = Color.rgb(255, 111, 0)
                c.drawCircle(128f, 120f, 46f, p)
                p.color = Color.rgb(255, 235, 59)
                c.drawCircle(128f, 120f, 24f, p)
                p.color = Color.rgb(183, 28, 28)
                c.drawRect(0f, 208f, 256f, 256f, p)
            }
            TexId.RAMP_CHEVRON -> {
                c.drawColor(Color.rgb(45, 50, 58))
                p.color = Color.rgb(255, 128, 0)
                p.strokeWidth = 18f
                for (y in 32 until 256 step 64) {
                    c.drawLine(32f, y + 28f, 128f, y.toFloat(), p)
                    c.drawLine(128f, y.toFloat(), 224f, y + 28f, p)
                }
            }
        }
        return bmp
    }
}
