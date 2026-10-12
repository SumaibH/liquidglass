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
    return 1f+(style.selectedScale-1f)*f+
        (style.tapSelectedScale-1f)*appearance.coerceIn(0f,.95f)*(1f-f*f)
}
