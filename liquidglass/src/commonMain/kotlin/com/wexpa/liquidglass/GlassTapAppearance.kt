package com.wexpa.liquidglass

/** Fade only the inset pigment during coupled travel; held band/rim geometry is independent. */
internal fun glassTransitStyle(style: GlassStyle, appearance: Float, fade: Float): GlassStyle {
    val amount=(appearance*fade).coerceIn(0f,.95f)
    if(amount==0f)return style
    return style.copy(tint=style.tint.copy(alpha=style.tint.alpha*(1f-amount)),
        tintLift=style.tintLift*(1f-amount))
}

/** Ink emphasis is around each item's own anchor and never changes its layout or hit target. */
internal fun glassTransitInkScale(style: GlassTabBarStyle, formation: Float, appearance: Float): Float {
    val f=formation.coerceIn(0f,1f)
    val scale=1f+(style.selectedScale-1f)*f+
        (style.tapSelectedScale-1f)*appearance.coerceIn(0f,.95f)*(1f-f*f)
    if (style.tapOptics<=0f) return scale
    // Transit optics magnify the ink themselves; keep the measured visible tap scale (video1
    // 79/69 px) instead of compounding it. Equal optical and held formation is unchanged.
    fun lensGain(x: Float): Float {
        val m=lerpGlassStyle(style.pill,style.lens,x)
        return 1f-m.heldMagnification*m.heldLens
    }
    val optical=maxOf(f,appearance*style.tapOptics).coerceIn(0f,1f)
    // A folding transit band has no interior zoom (the fold family ignores heldMagnification).
    val gain=if (glassSelectorFold(style,f,appearance)>0f) 1f else lensGain(optical)
    return if (optical<=f) scale else scale*gain/lensGain(f)
}
