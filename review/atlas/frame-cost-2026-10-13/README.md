# Atlas rendering-cost investigation — 13 October 2026

Atlas selects the NVIDIA RTX 3050 Laptop GPU on the owner's Windows machine. Forced redraws
can still take hundreds of milliseconds. The profiles below separate those redraw calls from
normal Compose picture recording. **No performance fix or presented-frame result is established.**
Production optics and motion remain the verified `cea1913` revision; the new code is sample-only
diagnostic instrumentation.

## Results and limitations

| Replay | Backend / actual adapter | Result | Relevant observation |
| --- | --- | --- | --- |
| Forced redraw, no screenshot readback | Direct3D / NVIDIA | All 33 phases pass | Median18.35ms; held-travel redraw638.83ms |
| Forced redraw with JFR | Direct3D / NVIDIA | All 33 phases pass | Median10.01ms; first-tap redraw555.26ms |
| Forced redraw, OpenGL override | OpenGL / Intel UHD | All 33 phases pass | Median10.76ms; maximum25.84ms; **different GPU** |
| Natural recording, first run | Direct3D / NVIDIA | **Fails bottom-right selection** | 5,823 callbacks: median1.10ms, p953.32ms, maximum16.36ms |
| Natural recording with own-input/focus observation | Direct3D / NVIDIA | All 33 phases pass | 4,550 callbacks: median2.53ms, p955.02ms, maximum34.27ms |
| Natural recording without JFR | Direct3D / NVIDIA | All 33 phases pass | 8,903 callbacks: median0.90ms, p951.60ms, maximum15.02ms |

The failed natural run selected Sky instead of Lines after the bottom-right pull. Its result
is retained. The next run passed with536 recorded replay events, no other mouse events and
focus retained throughout. The first run lacked that input/focus trace, so the repeat does
not establish why it failed. Do not report this as a repaired defect.

In the profiled555.26ms forced redraw, the UI thread spent491.01ms waiting for Skiko's
Direct3D draw lock while background rendering samples were in native `flush`. The installed
Skiko0.150.1 code confirms that `renderImmediately` takes this lock. This identifies a backend
wait but cannot distinguish GPU compilation, execution, driver work or other native costs.
It also cannot establish the delay experienced by ordinary input without forced redraw.

The later replay without JFR also passed with536replay events, no other mouse/focus events,
and maximum input dispatch32.1551ms. Variation between runs is retained; it is not an A/B
performance result or proof that profiling alone caused the earlier outlier.

The second natural run's slowest input dispatch was87.0834ms, on its first press. The saved
JFR interval contains no usable UI execution samples that explain it. The next slowest dispatch
was13.6254ms. Do not infer a root cause from missing samples. Profiling and CSV collection add
overhead; no speedup is claimed between these uncontrolled runs.

## What these measurements mean

- `events.csv`: dispatch duration surrounds delivery to this application's AWT component.
  It is not hardware-input-to-display latency. The wall-clock anchor supports external profiles.
- With `atlas.motionObserveFrames=true`, `frames.csv` times the existing Skiko render delegate's
  Compose picture-recording callback. It does not time GPU execution or display presentation.
  Phase capture rows are markers; no forced redraw or screenshot readback occurs during gestures.
  Initial layout stabilization still redraws before measurement starts.
- `input-observed.csv` records only this application's mouse/focus events, distinguishing replay
  dispatch from other events. It never hooks global input or reads another application's window.
- Missing callbacks after saturated or unchanged input are not automatically missed frames.
  A callback-to-input association is not a response-latency measurement unless required visual
  invalidation is established. Idle gaps must not be included in an FPS claim.
- Skiko's built-in `FPSCounter.tick` occurs before drawing and counts intervals between ticks.
  It is not an independent measurement of display presentation, so enabling it would not close
  the remaining evidence gap.

## Secondary construction cost

Whole-record JFR samples showed `Image.makeFromBitmap` in all three field-shader constructors.
The navigation footprint is drawn once but remains a mutable bitmap. A local CPU probe called
the existing production helper for a352×82 bitmap,300 warmups and1,000 samples per round:

| Input | First median / p95 | Repeated median / p95 |
| --- | --- | --- |
| Mutable bitmap | 0.0388 /0.0551ms | 0.0377 /0.0501ms |
| Immutable bitmap | 0.0005 /0.0006ms | 0.0004 /0.0004ms |

These measure shader construction alone, including reflective call overhead, with shaders
closed after each sample. They do not measure GPU uploads or rendered output. About0.11ms for
three mutable snapshots is insufficient evidence that this explains the observed large delays.
No cache, immutability change, quality reduction or backend switch was adopted.

## Presentation trace limitation

The official standalone PresentMon2.6.0 binary was checked against its release SHA-256 and a
valid Intel Authenticode signature. A unique trace session targeted only the Atlas process,
disabled input tracking and had a150second/process-exit bound. Windows refused the ETW session
with access denied (exit6): this requires administrator or Performance Log Users privileges.
No presentation CSV was generated and no permission/group changes were made. The independent
Atlas replay completed normally. See [PresentMon's console documentation](https://github.com/GameTechDev/PresentMon/blob/main/README-ConsoleApplication.md)
for the process filter and measurement definitions. Presentation verification remains open.

## Reproduce the natural observation

```powershell
$motionOutput = Join-Path (Get-Location) '.local/atlas-natural-new'
./tools/workspace.ps1 library :desktop:run "-Patlas.motionCapture=$motionOutput" `
  '-Patlas.motionReadback=false' '-Patlas.motionObserveFrames=true'
```

Use a fresh directory. Read `capture.json` and the process exit result before interpreting any
trace; failed output must not be included as a successful replay. Avoid interacting with Atlas
during the replay; check the recorded own-input/focus events for interference.

The raw traces, JFR profiles and probe source are preserved in the owner's local handoff.
Raw JFR contains process metadata and is deliberately excluded from this public packet.
[verification.json](verification.json) retains the numeric results, source hashes and remaining
questions without that metadata. No new library tests were required: production source did not
change. Exact-head hosted run38320819316 passes344 library cases,27 optional skips and3 Atlas
cases, with Android/desktop build success. This is regression evidence, not full iOS parity.

Next: establish ordinary input/presentation timing and explain the intermittent selection
failure. The complete motion, optical, integration and visual-acceptance scope remains in
[TODO.md](../../../TODO.md).
