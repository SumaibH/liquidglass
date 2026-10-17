package com.wexpa.liquidglass

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.geometry.Size
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Reference geometry of the bar's fusion with a linked free button ([GlassTabBarAccessory.link]) for the
 * CanvasKit and .NET ports: the rect the bar's outline reaches for, from the button's drawn press and
 * pull ([GlassFuse.toPx] through [accessoryExtents] and [glassInscribedStadium]) and undone through the
 * bar's own press ([GlassFuse.toMaterialPx]), and the neck's yield to an arriving lens
 * ([glassAccessoryYield]). Regenerate with -Pliquidglass.exportShaders=true; otherwise the committed
 * fixture must match.
 */
class GlassAccessoryFusePortFixtureTest {
    private val density = Density(3f)
    // The Phone scene at 3x: a 278 x 62 pt bar, the 62 pt search button 8 pt to its right, fusion 6 pt.
    private val bar = Size(834f, 186f)
    private val button = floatArrayOf(858f, 0f, 1044f, 186f)
    private val smoothing = 18f

    private fun f(x: Float) = "%.6f".format(java.util.Locale.ROOT, x)

    @Test fun accessoryFuseForPorts() {
        val cases = mutableListOf<String>()
        val drawings = listOf(
            GlassMaterialDrawing(),
            GlassMaterialDrawing(pressX = 1.26f, pressY = 1.26f),
            GlassMaterialDrawing(pull = GlassPullDeformation(1.2f, .85f, 0f, -30f, 0f), pressX = 1.2f, pressY = 1.2f),
            GlassMaterialDrawing(pull = GlassPullDeformation(1.25f, .8f, -90f, 0f, -40f), pressX = 1.18f, pressY = 1.18f),
            GlassMaterialDrawing(pull = GlassPullDeformation(1.15f, .9f, 35f, 22f, 15f), pressX = 1.1f, pressY = 1.1f),
            GlassMaterialDrawing(pull = GlassPullDeformation(1.3f, .78f, 180f, -60f, 0f), pressX = 1.26f, pressY = 1.26f),
        )
        val barPresses = listOf(1f to 1f, 1.05f to 1.05f, 1.08f to 1.03f)
        for (d in drawings) for ((bx, by) in barPresses) {
            val link = GlassAccessoryLink().apply { drawing.drawing = d }
            val fuse = with(density) {
                GlassFuse(DpRect(button[0].toDp(), button[1].toDp(), button[2].toDp(), button[3].toDp()),
                    cornerRadius = ((button[2] - button[0]) / 2f).toDp(), smoothing = smoothing.toDp(), anchored = true)
            }.apply { follow = link }
            val out = fuse.toMaterialPx(bar, density, GlassPullDeformation.None, bx, by)
            cases += "{\"pressScale\":${f(d.pressX)},\"along\":${f(d.pull.along)},\"across\":${f(d.pull.across)}," +
                "\"angle\":${f(d.pull.angleDegrees)},\"tx\":${f(d.pull.translationX)},\"ty\":${f(d.pull.translationY)}," +
                "\"barPressX\":${f(bx)},\"barPressY\":${f(by)},\"fuse\":[${out.joinToString(",") { f(it) }}]}"
        }
        val yields = mutableListOf<String>()
        for (formation in listOf(0f, .5f, 1f)) for (end in listOf(780f, 815f, 825f, 834f, 845f, 860f))
            yields += "{\"formation\":${f(formation)},\"endEdge\":${f(end)},\"yield\":${f(glassAccessoryYield(formation, end, bar.width, bar.height))}}"
        val text = "{\"provenance\":\"GlassFuse.toMaterialPx with a linked accessory and glassAccessoryYield (GlassAccessoryFusePortFixtureTest)\",\n" +
            "\"bar\":[${f(bar.width)},${f(bar.height)}],\"button\":[${button.joinToString(",") { f(it) }}],\"smoothing\":${f(smoothing)},\n" +
            "\"cases\":[\n${cases.joinToString(",\n")}],\n\"yields\":[\n${yields.joinToString(",\n")}]}\n"
        val file = File("../ports/skia/fixtures/accessory-fuse.json")
        if (System.getProperty("liquidglass.exportShaders") == "true") { file.parentFile.mkdirs(); file.writeText(text) }
        assertEquals(text, file.readText().replace("\r\n", "\n"),
            "Regenerate the accessory fuse fixture with -Pliquidglass.exportShaders=true")
    }
}
