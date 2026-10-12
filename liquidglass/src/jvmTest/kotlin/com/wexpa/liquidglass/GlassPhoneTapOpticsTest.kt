package com.wexpa.liquidglass

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.math.abs

/** Original native material patches, not an inferred hidden page or finger timestamp. */
class GlassPhoneTapOpticsTest {
    private fun bar() = GlassSelectorBar(834f,186f,21.75f,3,93f,139f,83.75f,90f,150f,150f,3f,800f)

    @Test fun movingTapClearsItsInsetWithoutBecomingHeld() {
        val b=bar();val c=GlassPoseController(GlassPoseSpec.Calm)
        c.attach(b,null,null);c.snapToRest(2)
        c.pointerDown(b.centreOf(1),b.centreY,0.0,true);c.retarget(1)
        val style=GlassTabBarStyle.Calm()
        val times=floatArrayOf(.0125f,.029167f,.045834f,.0625f,.079167f,.095834f,.1125f,.145834f,.1625f)
        val bars=floatArrayOf(37f,43f,47f,49f,50f,50.5f,51f,51f,51f)
        val observed=floatArrayOf(14f,15f,23f,29f,35f,40f,44.5f,50f,52f)
        var released=false
        for(i in times.indices) {
            if(!released && times[i]>=.04f){c.advanceTo(.04f);c.pointerUp(1);released=true}
            c.advanceTo(times[i])
            assertFalse(c.isHeld);assertFalse(c.solverFailed)
            val material=glassTransitStyle(lerpGlassStyle(style.pill,style.lens,c.formation),
                c.transitAppearance,style.tapTintFade)
            // Interior transfer is also exercised by the real production shader below.
            val expected=bars[i]*(1-material.tint.alpha)+255f*material.tintLift
            println("TAP_OPTICS i=$i f=${c.formation} appearance=${c.transitAppearance} rgb=$expected")
            assertEquals(observed[i],expected,6f,"native frame${i+6} interior on its observed bar")
            assertTrue(c.transitAppearance in 0f..0.95f)
        }
        val material=glassTransitStyle(lerpGlassStyle(style.pill,style.lens,c.formation),
            c.transitAppearance,style.tapTintFade)
        val pixels=GlassRender.render(width=400,height=260,pad=24,profile=2f,
            heldLens=material.heldLens,tintAlpha=material.tint.alpha,
            tint=Triple(material.tint.red,material.tint.green,material.tint.blue),lift=material.tintLift,
            backdrop={_,_->0xff333333.toInt()})
        assertEquals(52.0,GlassRender.luma(pixels[154*448+224]),6.0)
        val scale=glassTransitInkScale(style,c.formation,c.transitAppearance)/
            (1f-material.heldMagnification*material.heldLens)
        assertEquals(79f/69f,scale,.025f,"visible icon scale; not a full font/edge fit")
        val sourceRadius=34.5f*glassTransitInkScale(style,c.formation,c.transitAppearance)
        val ink=GlassEndpointRender.render(width=400,height=260,pad=24,refractBand=1f,refractDepth=0f,
            heldLens=material.heldLens,heldMagnification=material.heldMagnification,
            backdrop={_,_->0xff333333.toInt()},ink1={x,y->
                val dx=x-224f;val dy=y-154f
                if(dx*dx+dy*dy<=sourceRadius*sourceRadius)0xff149cff.toInt() else 0
            }).ink
        val visible=ink.indices.filter { (ink[it] ushr 24)>=180 }
        assertTrue(visible.isNotEmpty())
        val inkWidth=visible.maxOf {it%448}-visible.minOf {it%448}+1
        val inkHeight=visible.maxOf {it/448}-visible.minOf {it/448}+1
        assertEquals(79f,inkWidth.toFloat(),2f,"source complete icon79..80px")
        assertEquals(79f,inkHeight.toFloat(),2f,"source complete icon79..80px")
        println("TAP_OPTICS renderedInk=$inkWidth x $inkHeight")
        // Remaining held-specific band parameters keep their old formation response.
        assertEquals(c.formation,material.heldLens,1e-6f)
        assertTrue(material.heldLens<.25f)
    }

    @Test fun appearanceDoesNotChangeThePoseOrHeldFormation() {
        for(target in listOf(1,2)) {
            val b=bar()
            val current=GlassPoseController(GlassPoseSpec.Calm)
            val previous=GlassPoseController(GlassPoseSpec.Calm.copy(tapAppearanceArea=0f))
            for(c in listOf(current,previous)){c.attach(b,null,null);c.snapToRest(0);c.retarget(target)}
            for(i in 0..960) {
                val t=i/480f
                for(c in listOf(current,previous))c.advanceTo(t)
                val a=GlassPoseExtents().also(current::extents)
                val p=GlassPoseExtents().also(previous::extents)
                assertEquals(previous.centreX,current.centreX,0f)
                assertEquals(previous.centreVelocityX,current.centreVelocityX,0f)
                assertEquals(previous.formation,current.formation,0f)
                assertEquals(p.width,a.width,0f);assertEquals(p.height,a.height,0f)
            }
            assertTrue(current.isIdle);assertEquals(0f,current.transitAppearance)
        }
    }

    @Test fun shortAppearanceIsScaleCadenceAndDirectionIndependent() {
        fun samples(hz:Int,scale:Float,reverse:Boolean):List<Float> {
            val b=GlassSelectorBar(834f*scale,186f*scale,21.75f*scale,3,93f*scale,
                139f*scale,83.75f*scale,90f*scale,150f*scale,150f*scale,3f*scale,800f*scale)
            val c=GlassPoseController(GlassPoseSpec.Calm)
            c.attach(b,null,null);c.snapToRest(if(reverse)2 else 0);c.retarget(1)
            val values=mutableListOf<Float>()
            for(i in 0..hz) {
                c.advanceFrameTo(i.toFloat()/hz)
                assertTrue(c.transitAppearance in 0f..0.95f)
                if(i%(hz/30)==0)values+=c.transitAppearance
            }
            return values
        }
        val reference=samples(120,1f,false)
        for(hz in listOf(30,60,90,120))for(scale in listOf(.5f,1f,2f))for(reverse in listOf(false,true)) {
            val actual=samples(hz,scale,reverse)
            for(i in reference.indices)assertEquals(reference[i],actual[i],.01f,"$hz/$scale/$reverse sample$i")
        }
    }

    @Test fun regrabReleaseReducedMotionAndRelayoutClearOrCarryAppearance() {
        val b=bar()
        for(time in listOf(.07f,.12f,.18f)) {
            val c=GlassPoseController(GlassPoseSpec.Calm)
            c.attach(b,null,null);c.snapToRest(0);c.retarget(1);c.advanceTo(time)
            val before=c.transitAppearance
            c.pointerDown(c.centreX,b.centreY,time.toDouble(),true)
            c.beginDrag(c.centreX,b.centreY)
            assertEquals(before,c.transitAppearance,0f)
            c.advanceTo(time+1f/960f)
            assertTrue(abs(c.transitAppearance-before)<.03f)
            val held=c.transitAppearance;c.pointerUp(1)
            assertEquals(held,c.transitAppearance,0f)
            c.advanceTo(time+.4f)
            c.motionEnabled=false;c.advanceTo(time+.41f)
            assertEquals(0f,c.transitAppearance)
            assertFalse(c.solverFailed)
        }
        val c=GlassPoseController(GlassPoseSpec.Calm)
        c.attach(b,null,null);c.snapToRest(0);c.retarget(1);c.advanceTo(.15f)
        assertTrue(c.transitAppearance>.8f)
        c.attach(GlassSelectorBar(500f,100f,12f,3,50f,80f,45f,90f,150f,150f,1f,800f),null,null)
        assertEquals(0f,c.transitAppearance)
    }

    @Test fun stationaryHoldAndLightStyleRetainExistingOptics() {
        val b=bar();val c=GlassPoseController(GlassPoseSpec.Calm)
        c.attach(b,null,null);c.snapToRest(0);c.pointerDown(b.firstCentre,b.centreY,0.0,true)
        for(i in 0..240){c.advanceTo(i/240f);assertEquals(0f,c.transitAppearance)}
        c.pointerUp(0)
        for(i in 1..240){c.advanceTo(1f+i/240f);assertEquals(0f,c.transitAppearance)}
        val light=GlassTabBarStyle.Calm(dark=false)
        val style=lerpGlassStyle(light.pill,light.lens,.15f)
        assertEquals(style,glassTransitStyle(style,.95f,light.tapTintFade))
        assertEquals(1f,glassTransitInkScale(light,.15f,.95f))
        assertEquals(GlassTabBarStyle.V3().tapSelectedScale,1f)
        assertEquals(GlassTabBarStyle.V3().tapTintFade,0f)
    }
}
