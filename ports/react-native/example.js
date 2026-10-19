/** Apache-2.0. Renders build/react-native-glass.png with React Native Skia's headless build: the same
 * components an app puts in a <Canvas>, over a page drawn in Skia. */
import {mkdir, writeFile} from 'node:fs/promises';
import React from 'react';
import {Skia, headless, glass, pageImage} from './test/headless.js';

const W = 480, H = 320, ratio = 2;
// The backdrop at device resolution (pixelRatio 2), as makeImageFromView returns it.
const big = Skia.Surface.Make(W * ratio, H * ratio), small = pageImage();
big.getCanvas().drawImageRect(small, Skia.XYWHRect(0, 0, W, H), Skia.XYWHRect(0, 0, W * ratio, H * ratio), Skia.Paint());
const source = big.makeImageSnapshot();
const {Group, Image, Text, matchFont} = headless;
const element = React.createElement(Group, {transform: [{scale: ratio}]},
  React.createElement(Image, {image: small, x: 0, y: 0, width: W, height: H}),
  React.createElement(glass.GlassSurface, {source, x: 24, y: 200, width: 250, height: 92, radius: 30, dark: true, pixelRatio: ratio}),
  React.createElement(glass.GlassSurface, {source, x: 300, y: 28, width: 156, height: 110, radius: 24, light: [0, -1], pixelRatio: ratio}),
  React.createElement(glass.GlassLens, {source, x: 60, y: 30, width: 150, magnification: 1.5, pixelRatio: ratio}));
const surface = headless.makeOffscreenSurface(W * ratio, H * ratio);
const image = await headless.drawOffscreen(surface, element);
await mkdir('build', {recursive: true});
await writeFile('build/react-native-glass.png', image.encodeToBytes());
console.log('Rendered build/react-native-glass.png (960 x 640, pixel ratio 2)');
