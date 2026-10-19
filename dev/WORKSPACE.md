# One development workspace

Current work and recovery: read [PROGRESS.md](PROGRESS.md) after an interruption. It identifies the
verified commit, requirement checklist, evidence and next steps; the local checkpoint names live jobs.
For the exact unfinished experiment and commands, start with [NEXT-SESSION.md](NEXT-SESSION.md).
[TODO.md](TODO.md) retains the full delivery checklist.

Use the canonical `liquidglass` checkout, branching from `main` for changes. Atlas Studio is the
desktop sample **inside liquidglass**, not another copy of Vitals. The former
Astra, Fable and Antigravity workspaces are retired. Their names in old research
records describe provenance, not instructions to resume those workspaces.

## Build on Windows

Use JDK 21 (`JAVA_HOME`) and an Android SDK (`ANDROID_HOME` or each repository's
`local.properties`). From the liquidglass root:

```powershell
./tools/workspace.ps1 library :desktop:run
./tools/workspace.ps1 library :desktop:compileKotlinDesktop :sample:assembleDebug
./tools/workspace.ps1 library :liquidglass:jvmTest --rerun
./tools/workspace.ps1 library publishToMavenLocal
./tools/workspace.ps1 app :androidApp:assembleDebug :shared:jvmTest
./tools/workspace.ps1 app :desktopApp:run
```

The wrapper gives both repositories the same private Gradle cache and Maven
repository under `liquidglass/.local/`. It also isolates temporary files. It does
not publish remotely, switch branches, or read signing credentials. Optional
`LIQUIDGLASS_WORK_HOME` relocates that local data; use the same value for both
publishing and consuming. Keep Vitals' version catalog aligned with the library's
`VERSION_NAME`. On other systems use the Gradle wrappers with the same
`GRADLE_USER_HOME` and `-Dmaven.repo.local` for both repositories.

## Atlas Studio

`tools/launch_desktop.bat` starts `:desktop:run` through the shared wrapper. Its JVM
window calls `LoupeScreen`, shared with Android under
`apps/sample/src/main/kotlin/shared/`. At 1000dp and wider it adds a composition catalog.
The canvas demonstrates sky, type and line-grid backdrops with a movable continuous lens,
anchored note card and calm navigation. Narrower windows reflow the same controls.
The sky is illustrative; this is a material demo, not a live astronomy instrument.

Atlas prefers a discrete GPU when one is available. Override with `-Patlas.gpu=auto` or
`-Patlas.gpu=integrated` on `:desktop:run`; the default is `discrete`. This sets Skiko's
per-process adapter preference, without changing Windows-wide graphics settings or the library's
rendering policy. Startup prints `ATLAS_RENDERER` with the actual selected video card; capture
metadata records it too. Check that adapter name rather than assuming the requested preference
was honored. On the owner's hybrid machine, automatic selection used Intel UHD while discrete
selected the NVIDIA RTX3050Laptop6GB. See [the GPU diagnostic](../review/atlas/gpu-2026-10-11/README.md)
for the paired static-scene results and their limits.

The library preserves earlier Astra/Claude/Fable work in historical presets and history.
The current development preview adds opt-in calm interactions, continuous magnifier optics
and the scene API. See `review/ATLAS-STABILIZATION.md` for native desktop evidence, timing
and limits. The existing r14 APK is a historical artifact, not a build of this update.
Vitals is outside the current work scope; its commands above remain for maintainers.

`ports/web` provides the small GLSL lens preview. `ports/skia` exposes the actual production
material/ink/aperture shaders and a CanvasKit adapter. Both have terminal tests and CI;
neither silently captures content from another UI stack.

`ports/dotnet` adds a native SkiaSharp painter with executable fixture checks and a Windows Forms
host. See its README for the .NET 8 renderer / .NET 10 desktop prerequisites. On this Windows
checkout `tools/dotnet-port.ps1` uses an existing private SDK and isolated NuGet cache; it does not
install an SDK. Supply one on PATH on other machines. Host pixels/input remain explicit.

### Native motion diagnostics

Atlas can replay navigation input through its own AWT component and save app-owned phase snapshots:

```powershell
$motionOutput = Join-Path (Get-Location) '.local/atlas-motion'
./tools/workspace.ps1 library :desktop:run "-Patlas.motionCapture=$motionOutput"
python tools/plot_native_motion.py $motionOutput --output .local/atlas-motion.png
```

Use a wide desktop window and the default Sky scene. This opt-in diagnostic verifies tap/held-drag
selection, full-corner pulls and fixed layout bounds. It never moves the OS cursor or reads other
windows. It also replays early and formed endpoint flings in both directions, then captures recovery.
The complete sequence produces 33 phase snapshots, including actual drawable-window edges and all
four corners, sustained held reversals, and a soft intermediate-tab landing. The older far-outside
diagonal stress cases remain separate. These coordinates cover the app window, not OS-owned areas
or a physical phone screen. Each phase replays its gesture independently because native screenshot readback can be
slow. CSV timestamps describe actual input and render-start times; PNG encoding follows input.
Use `-Patlas.motionReadback=false` with a **different output directory** to replay the same
assertions without screenshots. This records CSV/metadata only; do not run the image plotter on it.
Before input, diagnostics redraw and wait for stable layout/layer dimensions so asynchronous
window maximization cannot invalidate the initial coordinates. Gesture-time layout checks remain strict.
Diagnostics run on the plain Swing dispatcher because forced redraw can re-enter Compose's
coroutine dispatcher. Capture and pointer-sequence guards fail the run instead of accepting a
corrupted trace. Check `checksPassed` in `capture.json`, and use actual CSV timestamps rather
than the nominal phase names. Timing can vary substantially even without screenshot readback.
The contact sheet preserves source pixels. These are phase snapshots, not a continuous recording,
presented-frame benchmark or measured iOS timing. Pillow is needed only for the optional sheet.

For the adjacent-tap path specifically, use `-Patlas.motionShortTaps=true` with a new
`atlas.motionCapture` directory. This produces ten snapshots: rest, four phases from each end
to the middle tab, and recovery. The default33-phase replay remains unchanged. Both scenarios
use the same app-owned input, stable-layout and selection assertions; their separate CSVs retain
actual timestamps and readback overhead.

To observe normal picture recording, add `-Patlas.motionObserveFrames=true` together with
`-Patlas.motionReadback=false`, using a fresh output directory. This disables forced redraws
during gestures and adds `frames.csv` plus an own-component input/focus trace in
`input-observed.csv`. Initial layout stabilization still redraws before measurements. Callbacks
measure Compose picture recording, not GPU/display presentation; unchanged input may need no
new callback. Phase capture rows are markers in this mode. Read `checksPassed` and keep any
failed trace. See the [frame-cost investigation](../review/atlas/frame-cost-2026-10-13/README.md).

## Recovery and local evidence

[Retired source history](archive/retired-workflows/2026-10-03/README.md) is committed
on GitHub, including the previously uncommitted alternative implementations.
The manifest names every retired branch and its exact commit. Restore an
alternative in a separate clone only when needed; there is one active approach.

On the owner's PC, `.local/consolidation-2026-10-03/` holds exact dirty-file
backups and the cleanup manifest. Original reference material stays in
`research/`; unique retired evidence is preserved locally. Private transcripts,
recordings, caches, SDK configuration and signing files are not GitHub content.

Physical testing still uses the shared atomic lease at sibling
`liquidglass-coordination/DEVICE-LEASE.json`. Read its `COORDINATION.md` before
device operations. Never kill unrelated Java processes or steal a device lease.
