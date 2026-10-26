package com.wexpa.liquidglass

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewParent
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Appearance mode for XML-hosted glass views (`app:liquidGlassDark`).
 */
enum class GlassXmlDarkMode {
    /** Follows the Android `Configuration.UI_MODE_NIGHT_MASK` of the host context. */
    Auto,
    /** Forces the light appearance (`dark = false`). */
    Light,
    /** Forces the dark appearance (`dark = true`). */
    Dark,
}

/**
 * Material preset selectable from Android layout XML (`app:liquidGlassPreset`).
 */
enum class GlassXmlPreset {
    /** [Glass.Regular]: the measured in-app material. */
    Regular,
    /** [Glass.Clear]: permanently more transparent glass. */
    Clear,
    /** [Glass.Toolbar]: toolbar material. */
    Toolbar,
    /** [Glass.TabBar]: measured tab bar material. */
    TabBar,
    /** [GlassStyle.Chrome]: higher-opacity chrome for sparse backdrops. */
    Chrome,
    /** [Glass.Lens]: clear magnifier lens. */
    Lens,
    /** [Glass.Identity]: draws content only, with no glass effect. */
    Identity,
}

/**
 * Surface outline selectable from Android layout XML (`app:liquidGlassShape`).
 */
enum class GlassXmlShape {
    /** [CapsuleShape]: pill with 50% corner radius. */
    Capsule,
    /** [CircleShape]: circular outline. */
    Circle,
    /** [RoundedCornerShape] with `app:liquidGlassCornerRadius`. */
    Rounded,
    /** [GlassSquircleShape] with `app:liquidGlassCornerRadius` and `app:liquidGlassCornerPower`. */
    Squircle,
}

@Volatile
private var isAnyCaptureActive: Boolean = false

private val targetDrawnAtEpoch = WeakHashMap<View, Int>()
private var globalPreDrawEpoch = 0

internal class TouchGatedFrameLayout(context: Context) : FrameLayout(context) {
    var blockTouches: Boolean = false

    init {
        clipChildren = false
        clipToPadding = false
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean =
        !blockTouches && super.dispatchTouchEvent(ev)
}

/**
 * Android `View` host for Compose Liquid Glass overlays (`GlassTabBar`, `GlassEffectContainer`,
 * `Modifier.glassEffect`) inside XML layouts (`ConstraintLayout`, `FrameLayout`, `CoordinatorLayout`).
 *
 * Responsibilities:
 * 1. **Selective touch pass-through**: Only dispatches touch gestures to the internal [composeView]
 *    when `ACTION_DOWN` lands inside a registered interactive glass target (via [glassHitTarget],
 *    padded by the platform `ViewConfiguration.scaledTouchSlop`) or when [interceptAllTouches] is active (for example,
 *    an expanded floating glass menu scrim). All other touches pass straight through to the
 *    underlying Android `View` hierarchy (`RecyclerView`, `NestedScrollView`, canvas views).
 * 2. **Live Android `View` backdrop**: Resolves `app:liquidGlassBackdrop="@id/..."` (or [backdropView])
 *    and mirrors the live underlying `View` hierarchy into [LiquidGlassTheme]'s recording layer via
 *    [LiveViewBackdrop] while skipping any [LiquidGlassHostView] or [LiquidGlassView] instances and
 *    avoiding `RenderNode` recording recursion on ancestor `ViewGroup`s.
 */
open class LiquidGlassHostView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    internal val composeContainer: TouchGatedFrameLayout = TouchGatedFrameLayout(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    val composeView: ComposeView = ComposeView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
    }

    private val hitRects = ConcurrentHashMap<String, Rect>()
    private val backdropRects = ConcurrentHashMap<String, Rect>()

    internal var backdropRectsVersion: Int by mutableIntStateOf(0)
        private set

    // Device scaled touch-slop so edge taps on rounded caps register reliably.
    private val touchSlopPx: Int = ViewConfiguration.get(context).scaledTouchSlop
    private val tempLocation = IntArray(2)

    /** Resource ID of the Android [View] to refract as the backdrop (`app:liquidGlassBackdrop`). */
    var backdropViewId: Int = View.NO_ID
        set(value) {
            field = value
            if (isAttachedToWindow) {
                refreshBackdropTarget()
            }
        }

    private var explicitBackdropView: View? by mutableStateOf<View?>(null)
    private var resolvedBackdropView: View? by mutableStateOf<View?>(null)

    /** Explicit Android [View] to refract as the backdrop. Overrides [backdropViewId] when set. */
    var backdropView: View?
        get() = explicitBackdropView ?: resolvedBackdropView
        set(value) {
            explicitBackdropView = value
            if (isAttachedToWindow) {
                refreshBackdropTarget()
            }
        }

    /**
     * Authored backdrop margin (default `36.dp`) captured around child glass regions inside this [LiquidGlassHostView].
     *
     * When child glass elements are placed within this host container, this margin ensures optical refraction
     * and chromatic dispersion near their outlines have live surrounding backdrop pixels to sample.
     *
     * Note: Standalone [LiquidGlassView] instances size directly to the glass surface itself ([captureFullHostBounds] = true).
     * Their backdrop capture is bounded by the view's layout dimensions; optical refraction sampling outside the view boundary
     * falls back to the configured [liquidGlassBackground] ground color. To capture surrounding pixels beyond the glass surface
     * across sibling XML views, wrap the layout in a [LiquidGlassHostView] container.
     */
    var backdropMarginDp: Dp by mutableStateOf(36.dp)

    /** When true, captures the full bounds of this host view instead of clipping to registered [glassHitTarget] rects. */
    var captureFullHostBounds: Boolean by mutableStateOf(false)

    /** Appearance mode configured via `app:liquidGlassDark` or code. */
    var darkMode: GlassXmlDarkMode by mutableStateOf(GlassXmlDarkMode.Auto)

    /** Optional opaque ground color configured via `app:liquidGlassBackground` or code. */
    var glassBackgroundColor: Color? by mutableStateOf(null)

    /** When true (e.g. a floating glass menu is expanded), all touches are routed to Compose. */
    var interceptAllTouches: Boolean = false

    /** Optional callback invoked when a touch outside all glass hit targets occurs while [interceptAllTouches] is true. */
    var onOutsideTouchWhenIntercepting: (() -> Unit)? = null

    private var activeGestureOwnedByCompose: Boolean = false

    @Volatile
    internal var isCapturingBackdrop: Boolean = false

    init {
        clipChildren = false
        clipToPadding = false
        if (id != View.NO_ID) {
            composeView.id = id xor 0x5F474C53
        } else {
            composeView.id = View.generateViewId()
        }
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.LiquidGlassHostView, defStyleAttr, 0)
            try {
                backdropViewId = a.getResourceId(R.styleable.LiquidGlassHostView_liquidGlassBackdrop, View.NO_ID)
                if (a.hasValue(R.styleable.LiquidGlassHostView_liquidGlassBackdropMargin)) {
                    val px = a.getDimension(R.styleable.LiquidGlassHostView_liquidGlassBackdropMargin, 0f)
                    backdropMarginDp = (px / resources.displayMetrics.density).dp
                }
                captureFullHostBounds = a.getBoolean(
                    R.styleable.LiquidGlassHostView_liquidGlassCaptureFullBounds,
                    false,
                )
                darkMode = when (a.getInt(R.styleable.LiquidGlassHostView_liquidGlassDark, 0)) {
                    1 -> GlassXmlDarkMode.Light
                    2 -> GlassXmlDarkMode.Dark
                    else -> GlassXmlDarkMode.Auto
                }
                if (a.hasValue(R.styleable.LiquidGlassHostView_liquidGlassBackground)) {
                    glassBackgroundColor = Color(a.getColor(R.styleable.LiquidGlassHostView_liquidGlassBackground, 0))
                }
            } finally {
                a.recycle()
            }
        }
        clipChildren = false
        clipToPadding = false
        composeContainer.addView(composeView)
        super.addView(composeContainer, 0, composeContainer.layoutParams)
    }

    override fun setId(id: Int) {
        super.setId(id)
        if (id != View.NO_ID) {
            composeView.id = id xor 0x5F474C53
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        refreshBackdropTarget()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        resolvedBackdropView = null
    }

    protected fun refreshBackdropTarget() {
        if (explicitBackdropView != null) {
            resolvedBackdropView = null
            return
        }
        resolvedBackdropView = if (backdropViewId != View.NO_ID) {
            findBackdropById(backdropViewId)
        } else {
            parent as? View
        }
    }

    protected fun findBackdropById(id: Int): View? {
        if (id == View.NO_ID) return null
        var current: View? = parent as? View
        while (current != null) {
            current.findViewById<View>(id)?.let { return it }
            current = current.parent as? View
        }
        return rootView?.findViewById(id)
    }

    protected fun resolveIsDark(): Boolean = when (darkMode) {
        GlassXmlDarkMode.Dark -> true
        GlassXmlDarkMode.Light -> false
        GlassXmlDarkMode.Auto -> {
            val nightBits = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            nightBits == Configuration.UI_MODE_NIGHT_YES
        }
    }

    protected fun resolveBackgroundColor(dark: Boolean): Color =
        glassBackgroundColor ?: if (dark) Color.Black else Color.White

    /**
     * Sets raw Compose content on this host. Callers that use [setGlassContent] directly can
     * configure their own [LiquidGlassTheme] and [LiveViewBackdrop], or call [setThemedGlassContent]
     * to have both wired automatically from XML attributes.
     */
    fun setGlassContent(content: @Composable (LiquidGlassHostView) -> Unit) {
        composeView.setContent {
            content(this)
        }
    }

    /**
     * Sets Compose content wrapped in [LiquidGlassTheme] and backed by a recording-only [LiveViewBackdrop]
     * using the backdrop view, appearance, and ground color configured via XML attributes or properties.
     */
    fun setThemedGlassContent(
        glass: Glass = Glass.Regular,
        content: @Composable (LiquidGlassHostView) -> Unit,
    ) {
        composeView.setContent {
            val dark = resolveIsDark()
            val bg = resolveBackgroundColor(dark)
            ThemedGlassScaffold(
                host = this@LiquidGlassHostView,
                glass = glass,
                dark = dark,
                backgroundColor = bg,
                targetView = backdropView,
                marginDp = backdropMarginDp,
                captureFullHostBounds = captureFullHostBounds,
                content = content,
            )
        }
    }

    internal fun updateHitRect(id: String, rect: Rect?) {
        if (rect == null || rect.isEmpty) {
            hitRects.remove(id)
        } else {
            hitRects[id] = rect
        }
    }

    internal fun updateBackdropRect(id: String, rect: Rect?) {
        val changed = if (rect == null || rect.isEmpty) {
            backdropRects.remove(id) != null
        } else {
            val old = backdropRects.put(id, rect)
            old != rect
        }
        if (changed) {
            backdropRectsVersion++
        }
    }

    internal fun currentBackdropRects(): Collection<Rect> = backdropRects.values

    /** Returns true if screen-space coordinates `(rawX, rawY)` fall inside any registered glass hit target. */
    fun isPointInsideGlass(rawX: Float, rawY: Float): Boolean {
        getLocationOnScreen(tempLocation)
        val localX = (rawX - tempLocation[0]).roundToInt()
        val localY = (rawY - tempLocation[1]).roundToInt()
        return isLocalPointInsideGlass(localX, localY)
    }

    private fun isLocalPointInsideGlass(x: Int, y: Int): Boolean {
        for (rect in hitRects.values) {
            if (
                x >= rect.left - touchSlopPx &&
                x <= rect.right + touchSlopPx &&
                y >= rect.top - touchSlopPx &&
                y <= rect.bottom + touchSlopPx
            ) {
                return true
            }
        }
        return false
    }

    protected open fun shouldFilterTouches(): Boolean = true

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // 0.01f: authored minimum alpha threshold below which the host ignores touch events.
        if (visibility != VISIBLE || alpha <= 0.01f) return false
        if (!onFilterTouchEventForSecurity(ev)) return false
        if (!shouldFilterTouches()) {
            return super.dispatchTouchEvent(ev)
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = ev.x.roundToInt()
                val y = ev.y.roundToInt()
                val hitGlass = isLocalPointInsideGlass(x, y)
                if (hitGlass) {
                    activeGestureOwnedByCompose = true
                    return super.dispatchTouchEvent(ev)
                }
                if (interceptAllTouches) {
                    activeGestureOwnedByCompose = true
                    onOutsideTouchWhenIntercepting?.invoke()
                    super.dispatchTouchEvent(ev)
                    return true
                }
                activeGestureOwnedByCompose = false
                return false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val owned = activeGestureOwnedByCompose
                activeGestureOwnedByCompose = false
                return if (owned) super.dispatchTouchEvent(ev) else false
            }
            else -> {
                return if (activeGestureOwnedByCompose) super.dispatchTouchEvent(ev) else false
            }
        }
    }

    override fun invalidate() {
        if (isCapturingBackdrop || isAnyCaptureActive) return
        super.invalidate()
    }

    override fun draw(canvas: Canvas) {
        if (isCapturingBackdrop || isAnyCaptureActive) return
        super.draw(canvas)
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (isCapturingBackdrop || isAnyCaptureActive) return
        super.dispatchDraw(canvas)
    }
}

@Composable
internal fun ThemedGlassScaffold(
    host: LiquidGlassHostView,
    glass: Glass,
    dark: Boolean,
    backgroundColor: Color,
    targetView: View?,
    marginDp: Dp,
    captureFullHostBounds: Boolean,
    content: @Composable (LiquidGlassHostView) -> Unit,
) {
    val state = rememberLiquidGlassState(backgroundColor, 0f, 0f, 0f, 1f)
    val region = remember(state) { GlassRegion(state) }
    // 20.dp: authored default container merge distance matching LiquidGlassTheme and LocalGlassDefaults.
    val defaults = GlassDefaults(dark = dark, glass = glass, containerSpacing = 20.dp)

    CompositionLocalProvider(
        LocalGlassRegion provides region,
        LocalGlassDefaults provides defaults,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { region.root = it },
            propagateMinConstraints = true,
        ) {
            val backdropLayer = rememberGraphicsLayer()
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .onGloballyPositioned { coords ->
                        val size = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
                        state.sourceCoordinates = coords
                        state.sourceSize = size
                        for (m in region.views.values) {
                            m.sourceCoordinates = coords
                            m.sourceSize = size
                        }
                    }
                    .drawWithContent {
                        backdropLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        state.layer = backdropLayer
                        for (m in region.views.values) {
                            m.layer = backdropLayer
                        }
                    },
            ) {
                LiveViewBackdrop(
                    targetView = targetView,
                    host = host,
                    marginDp = marginDp,
                    captureFullHostBounds = captureFullHostBounds,
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                content(host)
            }

            GlassOverlay(region, Modifier.matchParentSize())
        }
    }
}

/**
 * Pure Android XML glass surface container (`<com.wexpa.liquidglass.LiquidGlassView>`).
 *
 * Wraps ordinary Android XML child views (`LinearLayout`, `ImageView`, `TextView`, etc.) and renders
 * a refracting Liquid Glass surface behind them using XML attributes (`app:liquidGlassBackdrop`,
 * `app:liquidGlassPreset`, `app:liquidGlassShape`, `app:liquidGlassCornerRadius`, `app:liquidGlassCornerPower`,
 * `app:liquidGlassTint`, `app:liquidGlassInteractive`, `app:liquidGlassPullable`, `app:liquidGlassDark`,
 * `app:liquidGlassBackground`) without requiring any Compose code in the Activity or Fragment.
 *
 * ### Backdrop Boundary Contract
 * Because [LiquidGlassView] sizes directly to the glass surface itself ([captureFullHostBounds] = true),
 * its backdrop capture is bounded by the view's layout dimensions. Within these bounds, optical refraction,
 * chromatic dispersion, and blur render against the underlying [backdropView]. Beyond the view's outer
 * boundary, optical sampling falls back to the configured [liquidGlassBackground] ground color.
 * For full surrounding margin sampling across external sibling views, wrap the layout in a [LiquidGlassHostView]
 * container.
 */
class LiquidGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LiquidGlassHostView(context, attrs, defStyleAttr) {

    /** Material preset configured via `app:liquidGlassPreset` or code. */
    var preset: GlassXmlPreset by mutableStateOf(GlassXmlPreset.Regular)

    /** Surface shape configured via `app:liquidGlassShape` or code. */
    var xmlShape: GlassXmlShape by mutableStateOf(GlassXmlShape.Capsule)

    /**
     * Corner radius used when [xmlShape] is [GlassXmlShape.Rounded] or [GlassXmlShape.Squircle].
     * Authored default is `24.dp`.
     */
    var cornerRadiusDp: Dp by mutableStateOf(24.dp)

    /**
     * Superellipse exponent used when [xmlShape] is [GlassXmlShape.Squircle].
     * Inherited default is `4f` ([GlassSquircleShape]'s default `power`).
     */
    var cornerPower: Float by mutableFloatStateOf(4f)

    /** Optional surface tint color configured via `app:liquidGlassTint` or code. */
    var glassTint: Color? by mutableStateOf(null)

    /** Whether touch interaction (press glow and lift) is enabled (`app:liquidGlassInteractive`). */
    var isGlassInteractive: Boolean by mutableStateOf(false)

    /** Whether interactive touch response uses [GlassInteraction.Pullable] (`app:liquidGlassPullable`). */
    var isGlassPullable: Boolean by mutableStateOf(false)

    /**
     * Magnification factor when [preset] is [GlassXmlPreset.Lens] (`app:liquidGlassLensMagnification`).
     * Inherited default: `1.4f` (matches [Glass.Lens] default magnification).
     */
    var lensMagnification: Float by mutableFloatStateOf(1.4f)

    /**
     * Optional custom [Glass] override; when non-null, overrides [preset], [glassTint], and the
     * press response built from [isGlassPullable]. Touch forwarding is still controlled by
     * [isGlassInteractive], so set it to `true` when [customGlass] is interactive.
     */
    var customGlass: Glass? by mutableStateOf(null)

    /** Optional custom Compose [Shape] override; when non-null, overrides [xmlShape] and [cornerRadiusDp]. */
    var customShape: Shape? by mutableStateOf(null)

    init {
        captureFullHostBounds = true
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.LiquidGlassView, defStyleAttr, 0)
            try {
                backdropViewId = a.getResourceId(R.styleable.LiquidGlassView_liquidGlassBackdrop, backdropViewId)
                darkMode = when (a.getInt(R.styleable.LiquidGlassView_liquidGlassDark, 0)) {
                    1 -> GlassXmlDarkMode.Light
                    2 -> GlassXmlDarkMode.Dark
                    else -> GlassXmlDarkMode.Auto
                }
                if (a.hasValue(R.styleable.LiquidGlassView_liquidGlassBackground)) {
                    glassBackgroundColor = Color(a.getColor(R.styleable.LiquidGlassView_liquidGlassBackground, 0))
                }
                preset = when (a.getInt(R.styleable.LiquidGlassView_liquidGlassPreset, 0)) {
                    1 -> GlassXmlPreset.Clear
                    2 -> GlassXmlPreset.Toolbar
                    3 -> GlassXmlPreset.TabBar
                    4 -> GlassXmlPreset.Chrome
                    5 -> GlassXmlPreset.Lens
                    6 -> GlassXmlPreset.Identity
                    else -> GlassXmlPreset.Regular
                }
                xmlShape = when (a.getInt(R.styleable.LiquidGlassView_liquidGlassShape, 0)) {
                    1 -> GlassXmlShape.Circle
                    2 -> GlassXmlShape.Rounded
                    3 -> GlassXmlShape.Squircle
                    else -> GlassXmlShape.Capsule
                }
                if (a.hasValue(R.styleable.LiquidGlassView_liquidGlassCornerRadius)) {
                    val px = a.getDimension(R.styleable.LiquidGlassView_liquidGlassCornerRadius, 0f)
                    cornerRadiusDp = (px / resources.displayMetrics.density).dp
                }
                cornerPower = a.getFloat(R.styleable.LiquidGlassView_liquidGlassCornerPower, 4f)
                if (a.hasValue(R.styleable.LiquidGlassView_liquidGlassTint)) {
                    glassTint = Color(a.getColor(R.styleable.LiquidGlassView_liquidGlassTint, 0))
                }
                isGlassInteractive = a.getBoolean(R.styleable.LiquidGlassView_liquidGlassInteractive, false)
                isGlassPullable = a.getBoolean(R.styleable.LiquidGlassView_liquidGlassPullable, false)
                lensMagnification = a.getFloat(R.styleable.LiquidGlassView_liquidGlassLensMagnification, 1.4f)
            } finally {
                a.recycle()
            }
        }

        setGlassContent { host ->
            val dark = resolveIsDark()
            val bg = resolveBackgroundColor(dark)
            val resolvedGlass = customGlass ?: buildXmlGlass()
            val resolvedShape = customShape ?: buildXmlShape()

            ThemedGlassScaffold(
                host = host,
                glass = resolvedGlass,
                dark = dark,
                backgroundColor = bg,
                targetView = backdropView,
                marginDp = backdropMarginDp,
                captureFullHostBounds = true,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .glassHitTarget(
                            id = "xml_liquid_glass_surface",
                            host = host,
                            interactive = isGlassInteractive,
                            captureBackdrop = true,
                        )
                        .glassEffect(
                            glass = resolvedGlass,
                            shape = resolvedShape,
                        ),
                )
            }
        }
    }

    private fun buildXmlGlass(): Glass {
        val base = when (preset) {
            GlassXmlPreset.Regular -> Glass.Regular
            GlassXmlPreset.Clear -> Glass.Clear
            GlassXmlPreset.Toolbar -> Glass.Toolbar
            GlassXmlPreset.TabBar -> Glass.TabBar
            GlassXmlPreset.Chrome -> Glass(GlassStyle.Chrome)
            GlassXmlPreset.Lens -> Glass.Lens(lensMagnification)
            GlassXmlPreset.Identity -> Glass.Identity
        }
        val tinted = if (glassTint != null) base.tint(glassTint) else base
        return if (isGlassInteractive) {
            val response = if (isGlassPullable) {
                GlassInteraction.Pullable.copy(pullShape = GlassPullShape.Free)
            } else {
                GlassInteraction.Calm
            }
            tinted.interactive(enabled = true, response = response)
        } else {
            tinted
        }
    }

    private fun buildXmlShape(): Shape = when (xmlShape) {
        GlassXmlShape.Capsule -> CapsuleShape
        GlassXmlShape.Circle -> CircleShape
        GlassXmlShape.Rounded -> RoundedCornerShape(cornerRadiusDp)
        GlassXmlShape.Squircle -> GlassSquircleShape(cornerRadiusDp, power = cornerPower)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Measure XML child views first without letting the internal ComposeView expand wrap_content
        // dimensions to the parent's full bounds.
        composeContainer.layoutParams?.let { lp ->
            lp.width = 0
            lp.height = 0
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        composeContainer.measure(
            MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY),
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // Always span the full outer bounds of LiquidGlassView regardless of container padding.
        composeContainer.layout(0, 0, right - left, bottom - top)
    }

    override fun shouldFilterTouches(): Boolean = false

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // 0.01f: authored minimum alpha threshold below which the host ignores touch events.
        if (visibility != VISIBLE || alpha <= 0.01f) return false
        if (!onFilterTouchEventForSecurity(ev)) return false

        composeContainer.blockTouches = true
        val handled = try {
            super.dispatchTouchEvent(ev)
        } finally {
            composeContainer.blockTouches = false
        }

        if (isGlassInteractive) {
            composeView.dispatchTouchEvent(ev)
        }

        return handled || isGlassInteractive
    }
}

/**
 * Registers this composable's bounds with [host] so touches inside it are routed to Compose
 * and its region is included in the live Android `View` backdrop capture.
 */
@Composable
fun Modifier.glassHitTarget(
    id: String,
    host: LiquidGlassHostView,
    interactive: Boolean = true,
    captureBackdrop: Boolean = true,
): Modifier {
    val lastRectRef = remember { arrayOfNulls<Rect>(1) }
    DisposableEffect(id, host, interactive, captureBackdrop) {
        val current = lastRectRef[0]
        if (current != null) {
            if (interactive) {
                host.updateHitRect(id, Rect(current))
            }
            if (captureBackdrop) {
                host.updateBackdropRect(id, Rect(current))
            }
        }
        onDispose {
            host.updateHitRect(id, null)
            host.updateBackdropRect(id, null)
        }
    }
    val tempLoc = remember { IntArray(2) }
    return this.onGloballyPositioned { coords ->
        if (!coords.isAttached) return@onGloballyPositioned
        val bounds = coords.boundsInWindow()
        host.getLocationInWindow(tempLoc)
        val left = (bounds.left - tempLoc[0]).roundToInt()
        val top = (bounds.top - tempLoc[1]).roundToInt()
        val right = (bounds.right - tempLoc[0]).roundToInt()
        val bottom = (bounds.bottom - tempLoc[1]).roundToInt()
        val rect = Rect(left, top, right, bottom)
        lastRectRef[0] = rect
        if (interactive) {
            host.updateHitRect(id, rect)
        } else {
            host.updateHitRect(id, null)
        }
        if (captureBackdrop) {
            host.updateBackdropRect(id, rect)
        } else {
            host.updateBackdropRect(id, null)
        }
    }
}

/**
 * Mirrors the live pixel output of an underlying Android [targetView] into the active
 * [LiquidGlassTheme] backdrop layer, clipped to the union of active glass regions plus an
 * authored [marginDp] (`36.dp` by default) so optical refraction, chromatic dispersion, and the
 * wide blur kernel have surrounding live backdrop pixels to sample without re-rendering the rest
 * of the screen.
 */
@Composable
internal fun LiveViewBackdrop(
    targetView: View?,
    host: LiquidGlassHostView,
    marginDp: Dp = 36.dp,
    captureFullHostBounds: Boolean = false,
) {
    if (targetView == null) return
    var frameTick by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val marginPx = with(density) { marginDp.toPx() }.roundToInt()
    val targetLoc = remember { IntArray(2) }
    val composeLoc = remember { IntArray(2) }
    val hostLoc = remember { IntArray(2) }
    var justDrewHost by remember { mutableStateOf(false) }

    DisposableEffect(targetView, host) {
        val vto = targetView.viewTreeObserver
        var pendingInvalidate = false
        var targetBackgroundDirty = false
        var installedBg: Drawable? = null
        var installedWrapper: TargetBackgroundCallbackWrapper? = null

        val wrapCallback: () -> Unit = {
            val bg = targetView.background
            if (bg != null && bg !== installedBg) {
                val wrapper = TargetBackgroundCallbackWrapper(bg.callback) {
                    targetBackgroundDirty = true
                }
                bg.callback = wrapper
                installedBg = bg
                installedWrapper = wrapper // Strong ref; Drawable holds callbacks weakly
            }
        }
        wrapCallback()

        val frameRunnable = Runnable {
            pendingInvalidate = false
            if (!host.isCapturingBackdrop && host.isShown) {
                frameTick++
            }
        }
        val preDrawListener = ViewTreeObserver.OnPreDrawListener {
            globalPreDrawEpoch++
            val lastDrawnEpoch = targetDrawnAtEpoch[targetView] ?: -1

            // Detect if background was replaced on targetView (even with same size/padding)
            val currentBg = targetView.background
            if (currentBg !== installedBg) {
                if (currentBg == null) {
                    // View.setBackground(null) already cleared the old drawable's callback.
                    installedBg = null
                    installedWrapper = null
                } else {
                    wrapCallback()
                }
                targetBackgroundDirty = true
            }

            val bgDirty = targetBackgroundDirty
            targetBackgroundDirty = false

            val targetIsDirty = isTargetDirty(targetView, host)
            val realChange = targetIsDirty || targetView.isLayoutRequested || bgDirty
            val suppressCapture = (lastDrawnEpoch == globalPreDrawEpoch || justDrewHost) && !realChange
            justDrewHost = false

            if (!host.isCapturingBackdrop && !pendingInvalidate && host.isShown) {
                if (realChange && !suppressCapture) {
                    pendingInvalidate = true
                    host.postOnAnimation(frameRunnable)
                }
            }
            true
        }
        val scrollListener = ViewTreeObserver.OnScrollChangedListener {
            if (!host.isCapturingBackdrop && !pendingInvalidate && host.isShown) {
                pendingInvalidate = true
                host.postOnAnimation(frameRunnable)
            }
        }
        val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            wrapCallback()
            if (!host.isCapturingBackdrop && !pendingInvalidate && host.isShown) {
                pendingInvalidate = true
                host.postOnAnimation(frameRunnable)
            }
        }
        if (vto.isAlive) {
            vto.addOnPreDrawListener(preDrawListener)
            vto.addOnScrollChangedListener(scrollListener)
        }
        targetView.addOnLayoutChangeListener(layoutListener)
        onDispose {
            host.removeCallbacks(frameRunnable)
            targetView.removeOnLayoutChangeListener(layoutListener)
            val bg = installedBg
            val wrapper = installedWrapper
            if (bg != null && wrapper != null) {
                bg.callback = unlinkBackgroundWrapper(bg.callback, wrapper)
            }
            installedBg = null
            installedWrapper = null
            val currentVto = targetView.viewTreeObserver
            if (currentVto.isAlive) {
                currentVto.removeOnPreDrawListener(preDrawListener)
                currentVto.removeOnScrollChangedListener(scrollListener)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Read state subscriptions first so early returns keep the draw scope subscribed.
                val tick = frameTick
                val rectsVersion = host.backdropRectsVersion
                if (tick < 0 || rectsVersion < 0) return@drawBehind
                if (host.isCapturingBackdrop || !targetView.isShown) return@drawBehind
                val rects = host.currentBackdropRects()
                if (!captureFullHostBounds && rects.isEmpty()) return@drawBehind

                targetView.getLocationInWindow(targetLoc)
                host.composeView.getLocationInWindow(composeLoc)
                host.getLocationInWindow(hostLoc)

                val hostWidth = size.width.roundToInt()
                val hostHeight = size.height.roundToInt()
                if (hostWidth <= 0 || hostHeight <= 0) return@drawBehind

                val composeOffsetX = composeLoc[0] - hostLoc[0]
                val composeOffsetY = composeLoc[1] - hostLoc[1]

                val clipLeft: Int
                val clipTop: Int
                val clipRight: Int
                val clipBottom: Int

                if (captureFullHostBounds) {
                    clipLeft = 0
                    clipTop = 0
                    clipRight = hostWidth
                    clipBottom = hostHeight
                } else {
                    var minL = Int.MAX_VALUE
                    var minT = Int.MAX_VALUE
                    var maxR = Int.MIN_VALUE
                    var maxB = Int.MIN_VALUE
                    for (r in rects) {
                        minL = min(minL, r.left)
                        minT = min(minT, r.top)
                        maxR = max(maxR, r.right)
                        maxB = max(maxB, r.bottom)
                    }
                    if (minL >= maxR || minT >= maxB) return@drawBehind
                    clipLeft = max(0, minL - composeOffsetX - marginPx)
                    clipTop = max(0, minT - composeOffsetY - marginPx)
                    clipRight = min(hostWidth, maxR - composeOffsetX + marginPx)
                    clipBottom = min(hostHeight, maxB - composeOffsetY + marginPx)
                }

                val dx = (targetLoc[0] - composeLoc[0]).toFloat()
                val dy = (targetLoc[1] - composeLoc[1]).toFloat()

                val nativeCanvas = drawContext.canvas.nativeCanvas
                val saveCount = nativeCanvas.save()
                host.isCapturingBackdrop = true
                try {
                    nativeCanvas.clipRect(clipLeft, clipTop, clipRight, clipBottom)
                    nativeCanvas.translate(dx, dy)
                    drawBackdropTree(targetView, host, nativeCanvas)
                } finally {
                    host.isCapturingBackdrop = false
                    nativeCanvas.restoreToCount(saveCount)
                    justDrewHost = true
                    targetDrawnAtEpoch[targetView] = globalPreDrawEpoch + 1
                }
            },
    )
}

private fun buildAncestorPath(host: View, target: View): Set<View> {
    val path = HashSet<View>()
    var curr: ViewParent? = host.parent
    while (curr != null) {
        if (curr is View) {
            path.add(curr)
            if (curr === target) break
        }
        curr = curr.parent
    }
    return path
}

/**
 * Traverses and draws [view] into [canvas] for [host]'s backdrop without mutating view visibility
 * and without re-entering already-recording RenderNodes of intermediate ancestors.
 */
private fun drawBackdropNode(
    view: View,
    ancestorPath: Set<View>,
    canvas: Canvas,
) {
    // 0.005f: authored minimum alpha threshold below which backdrop views are skipped.
    if (view is LiquidGlassHostView || view.visibility != View.VISIBLE || view.alpha <= 0.005f) {
        return
    }

    val sx = view.scrollX.toFloat()
    val sy = view.scrollY.toFloat()
    val save = canvas.save()
    canvas.translate(-sx, -sy)
    if (view !in ancestorPath) {
        // Safe to draw directly: this branch does not contain the currently recording host or its ancestors,
        // and any nested LiquidGlassHostView instances will self-suppress via isAnyCaptureActive.
        view.draw(canvas)
        canvas.restoreToCount(save)
        return
    }

    // View is an ancestor currently recording on the outer frame.
    // We MUST NOT call view.draw(canvas) or dispatchDraw(canvas), which would re-enter
    // the already-recording RenderNode of intermediate containers.
    // Instead, draw its background, then safely traverse its children.
    // Background stays fixed relative to the view bounds, as in View.drawBackground.
    view.background?.let { bg ->
        canvas.translate(sx, sy)
        bg.draw(canvas)
        canvas.translate(-sx, -sy)
    }

    if (view is ViewGroup) {
        val count = view.childCount
        for (i in 0 until count) {
            val child = view.getChildAt(i) ?: continue
            if (child is LiquidGlassHostView || child.visibility != View.VISIBLE || child.alpha <= 0.005f) {
                continue
            }
            val saveChild = canvas.save()
            canvas.translate(child.left.toFloat(), child.top.toFloat())
            if (!child.matrix.isIdentity) {
                canvas.concat(child.matrix)
            }
            if (view.clipChildren) {
                canvas.clipRect(0, 0, child.width, child.height)
            }
            if (child.alpha < 1f) {
                canvas.saveLayerAlpha(
                    0f,
                    0f,
                    child.width.toFloat(),
                    child.height.toFloat(),
                    (child.alpha * 255).roundToInt(),
                )
            }
            drawBackdropNode(child, ancestorPath, canvas)
            canvas.restoreToCount(saveChild)
        }
    }

    view.foreground?.let { fg ->
        canvas.translate(sx, sy)
        fg.draw(canvas)
        canvas.translate(-sx, -sy)
    }

    canvas.restoreToCount(save)
}

/**
 * Draws [targetView] into [canvas] for [host]'s backdrop while skipping any [LiquidGlassHostView]
 * instances and avoiding re-entry into recording ancestor RenderNodes.
 */
private fun drawBackdropTree(
    targetView: View,
    host: LiquidGlassHostView,
    canvas: Canvas,
) {
    val save = canvas.save()
    val wasCapturing = isAnyCaptureActive
    isAnyCaptureActive = true
    val ancestorPath = buildAncestorPath(host, targetView)
    try {
        canvas.clipRect(0, 0, targetView.width, targetView.height)
        drawBackdropNode(targetView, ancestorPath, canvas)
    } finally {
        isAnyCaptureActive = wasCapturing
        canvas.restoreToCount(save)
    }
}

private fun hasRealTargetChange(target: View, host: View): Boolean {
    return target.isLayoutRequested || isTargetDirty(target, host)
}

private fun isTargetDirty(target: View, host: View): Boolean {
    if (target === host || target is LiquidGlassHostView) return false
    if (!target.isDirty) return false
    if (target !is ViewGroup) return true

    var hasDirtyNonGlassChild = false
    var hasAnyDirtyChild = false

    for (i in 0 until target.childCount) {
        val child = target.getChildAt(i)
        if (child.isDirty) {
            hasAnyDirtyChild = true
            if (child !== host && child !is LiquidGlassHostView) {
                if (isTargetDirty(child, host)) {
                    hasDirtyNonGlassChild = true
                    break
                }
            }
        }
    }

    if (hasDirtyNonGlassChild) return true
    // If target itself is dirty and NO children are dirty, the ViewGroup itself was invalidated directly (background, onDraw)
    if (!hasAnyDirtyChild) return true
    // If only glass hosts were dirty, the invalidation came from our own glass overlays
    return false
}

private class TargetBackgroundCallbackWrapper(
    var delegate: Drawable.Callback?,
    private val onInvalidate: () -> Unit,
) : Drawable.Callback {
    override fun invalidateDrawable(who: Drawable) {
        onInvalidate()
        delegate?.invalidateDrawable(who)
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        delegate?.scheduleDrawable(who, what, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        delegate?.unscheduleDrawable(who, what)
    }
}

private fun unlinkBackgroundWrapper(
    head: Drawable.Callback?,
    target: TargetBackgroundCallbackWrapper,
): Drawable.Callback? {
    if (head === target) return target.delegate
    var current = head
    while (current is TargetBackgroundCallbackWrapper) {
        if (current.delegate === target) {
            current.delegate = target.delegate
            break
        }
        current = current.delegate
    }
    return head
}
