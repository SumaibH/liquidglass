package com.wexpa.liquidglass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.skiaImageFilter
import org.jetbrains.skia.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Uses the production ImageFilter, not a shader with an already complete static input. */
class GlassWideInputBoundsTest {
    @Test fun packedImageToneSurvivesViewportClipping() = verifyClippedTone(deferred = false)

    @Test fun recordedLayerToneSurvivesViewportClipping() = verifyClippedTone(deferred = true)

    @Test fun containerImageToneSurvivesViewportClipping() = verifyClippedTone(deferred = false, container = true)

    @Test fun containerLayerToneSurvivesViewportClipping() = verifyClippedTone(deferred = true, container = true)

    private fun verifyClippedTone(deferred: Boolean, container: Boolean = false) {
        val w = 160
        val h = 96
        val stripH = h / 4
        val info = ImageInfo(w, h + stripH, ColorType.RGBA_8888, ColorAlphaType.PREMUL)
        val pixels = ByteArray(w * (h + stripH) * 4)
        for (y in 0 until h + stripH) for (x in 0 until w) {
            val i = (y * w + x) * 4
            val wide = y >= h && x < w / 4
            pixels[i] = (if (wide) 40 + x * 3 else 180).toByte()
            pixels[i + 1] = (if (wide) 90 else 20).toByte()
            pixels[i + 2] = (if (wide) 100 + (y - h) * 2 else 30).toByte()
            pixels[i + 3] = -1
        }
        Image.makeRaster(info, pixels, w * 4).use { source ->
            val effect = if (container) createGlassContainerRenderEffect(containerUniforms())
                else createGlassRenderEffect(uniforms())
            val filter = checkNotNull(effect).skiaImageFilter
            Paint().use { paint ->
                paint.imageFilter = filter
                // Crop the right half, top/bottom and corners. The quarter-size strip is
                // outside each visible window and must still supply its tone to the panel.
                for ((dx, dy) in listOf(0 to 0, -64 to 0, 20 to 0, 0 to -32, -64 to -32, 20 to 16)) {
                    Surface.makeRasterN32Premul(100, 64).use { target ->
                        target.canvas.clear(0)
                        target.canvas.save()
                        target.canvas.translate(dx.toFloat(), dy.toFloat())
                        if (deferred) {
                            // Compose layers replay a display list into a filtered layer. Unlike
                            // a pre-existing image, its pixels can be culled by the output clip.
                            target.canvas.saveLayer(null, paint)
                            target.canvas.drawImage(source, 0f, 0f)
                            target.canvas.restore()
                        } else {
                            target.canvas.drawImage(source, 0f, 0f, paint)
                        }
                        target.canvas.restore()
                        Bitmap().use { bitmap ->
                            bitmap.allocPixels(ImageInfo(100, 64, ColorType.RGBA_8888, ColorAlphaType.PREMUL))
                            assertTrue(target.readPixels(bitmap, 0, 0))
                            val actual = bitmap.readPixels()!!
                            for (y in 24..71) for (x in 24..135) {
                                val tx = x + dx; val ty = y + dy
                                if (tx !in 0 until 100 || ty !in 0 until 64) continue
                                val i = (ty * 100 + tx) * 4
                                val expectedRed = 40f + ((x + .5f) * .25f - .5f) * 3f
                                assertEquals(expectedRed, (actual[i].toInt() and 255).toFloat(), 1.1f,
                                    "wide gradient at $x,$y in viewport $dx,$dy")
                                assertEquals(90, actual[i + 1].toInt() and 255, "tone clipped at $x,$y ($dx,$dy)")
                                val expectedBlue = 100f + ((y + .5f) * .25f - .5f) * 2f
                                assertEquals(expectedBlue, (actual[i + 2].toInt() and 255).toFloat(), 1.1f)
                                assertEquals(255, actual[i + 3].toInt() and 255)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun containerUniforms(): GlassContainerUniforms {
        val u = uniforms()
        return GlassContainerUniforms(
            width = u.width, height = u.height, pad = u.pad, backdrop = u.backdrop, background = u.background,
            rects = FloatArray(MAX_GLASS_MEMBERS * 4).also { it[2] = u.width; it[3] = u.height },
            radii = FloatArray(MAX_GLASS_MEMBERS), count = 1f, merge = 0f,
            refractBand = u.refractBand, refractDepth = u.refractDepth, aberration = u.aberration,
            ior = u.ior, bevelPower = u.bevelPower, fresnel = u.fresnel, highlightChroma = u.highlightChroma,
            bevel = u.bevel, lightX = u.lightX, lightY = u.lightY, specular = u.specular,
            specularPower = u.specularPower, tint = u.tint, innerShadow = u.innerShadow,
            adaptivity = u.adaptivity, blurRadius = u.blurRadius, backdropBlur = u.backdropBlur,
            counterLight = u.counterLight, edgeLight = u.edgeLight, bevelPeak = u.bevelPeak,
            edgeShadow = u.edgeShadow, rimSoft = u.rimSoft, tintAbsorb = u.tintAbsorb,
            profile = u.profile, wideKernel = u.wideKernel, wideStrip = u.wideStrip,
            wideScale = u.wideScale, fineShare = u.fineShare,
        )
    }

    private fun uniforms() = GlassUniforms(
        width = 128f, height = 64f, pad = 16f, scale = 0f, flip = 0f,
        radii = FloatArray(4), refractBand = 0f, refractDepth = 0f, aberration = 0f,
        profile = 1f, wideKernel = 20f, wideStrip = 96f, wideScale = .25f, fineShare = 0f,
        ior = 1.5f, bevelPower = 2f, cornerPower = 2f, shapeKind = 0f,
        fieldRange = 1f, fieldScale = 1f, field = null,
        mirror = 0f, fresnel = 0f, highlightChroma = 0f, legibility = 0f, bevel = 0f,
        backdrop = floatArrayOf(0f, 0f, 160f, 96f), background = Color.Black,
        touchX = 0f, touchY = 0f, touchAmount = 0f, materialize = 1f, frost = 0f, contrast = 0f,
        lightX = 0f, lightY = -1f, specular = 0f, specularPower = 4f, tint = Color.Transparent,
        innerShadow = 0f, adaptivity = 0f, blurRadius = 0f, backdropBlur = 0f,
        counterLight = 0f, edgeLight = 0f, bevelPeak = 0f, edgeShadow = 0f, rimSoft = 0f, tintAbsorb = 0f,
    )
}
