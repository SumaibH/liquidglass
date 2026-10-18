# Material 3 in Liquid Glass — 18 October 2026

`liquidglass-material3` (Phase 3): glass versions of the Material 3 components with the same names and
parameters. These renders come from `GlassMaterialComponentsTest` (desktop JVM, Skia raster, a
390 × 844 dp window at density 2, branch `claude/material3`): one screen built only from the glass
components, over a grid of gradient album covers.

![Dark scheme](showcase-dark.png) ![Light scheme](showcase-light.png)

*Our renders only. Material's `darkColorScheme()` and `lightColorScheme()`, unchanged. From the top:
`TopAppBar` with two `IconButton`s, a three-segment `SingleChoiceSegmentedButtonRow`, `Button`,
`OutlinedButton`, `IconButton`, `FilterChip`, `AssistChip`, a `Card` holding a `Switch` and a `Slider`,
and `NavigationBar` with three `NavigationBarItem`s and a search accessory. Icons from
material-icons-core. The covers are test content.*

| Dialog | Sheet | Menu |
| :-: | :-: | :-: |
| ![AlertDialog](dialog.png) | ![ModalBottomSheet](sheet.png) | ![DropdownMenu](menu.png) |

Checked by the test: a tap on the third tab of the glass `NavigationBar` selects it; a glass `Button`
clicks; `AlertDialog` opens and its button dismisses it; `ModalBottomSheet` shows its content;
`DropdownMenu` opens under its anchor and an item closes it.

Fixed while making these renders: the navigation bar's search accessory drew its glass but not its
icon (the overlay drew it under the bar's portal; linked accessories now draw after portals), and the
slider now uses iOS's thin continuous track instead of Material's split track.

Not checked here: Android devices, the web build, motion of the switch and slider thumbs (static
frames only), and visual parity with iOS 26 for components other than the tab bar (authored, not
measured).
