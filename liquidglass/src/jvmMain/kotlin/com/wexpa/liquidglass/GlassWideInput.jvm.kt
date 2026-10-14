package com.wexpa.liquidglass

import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Matrix33
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.SamplingMode

/**
 * Keep the portable shader contract intact. The desktop filter needs a separate, transformed
 * input for the packed tone strip: Skiko's RuntimeShader overload assumes same-coordinate
 * sampling, so moving/clipping a nested panel can discard the strip before wideAt reads it.
 * Both production material shaders share this exact sampling block. Fail on contract drift.
 */
internal fun glassShaderWithWideInput(source: String): String {
    val declaration = "uniform shader content;"
    val packedSample = "float2 c = float2(q.x * uWideScale, uWideStrip + q.y * uWideScale);\n" +
        "    half4 s = content.eval(c);"
    for (part in listOf(declaration, packedSample)) {
        check(source.indexOf(part) >= 0 && source.indexOf(part) == source.lastIndexOf(part)) {
            "Glass wide-input shader contract changed"
        }
    }
    return source.replace(declaration, "$declaration\nuniform shader wideContent;")
        .replace(packedSample, "half4 s = wideContent.eval(q);")
}

/** Caller owns the result; [input] remains borrowed. No readback or second blur is introduced. */
internal fun glassWideInputFilter(
    builder: RuntimeShaderBuilder,
    strip: Float,
    scale: Float,
    input: ImageFilter?,
): ImageFilter {
    // Inverse mapping of the old lookup: sample(q * scale + (0, strip)). Declaring
    // both transforms here lets Skia map destination clips back to the storage rectangle.
    return ImageFilter.makeOffset(0f, -strip, input, null).use { shifted ->
        ImageFilter.makeMatrixTransform(Matrix33.makeScale(1f / scale), SamplingMode.LINEAR, shifted).use { wide ->
            ImageFilter.makeRuntimeShader(builder, arrayOf("content", "wideContent"), arrayOf(input, wide))
        }
    }
}
