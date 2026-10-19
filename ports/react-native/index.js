/** Apache-2.0. Liquid Glass for React Native, drawn by React Native Skia with the production shaders. */
import React from 'react';
import {PixelRatio} from 'react-native';
import {Skia, TileMode, FilterMode, MipmapMode, BlendMode, ClipOp, Picture, Group} from '@shopify/react-native-skia';
import {bindLiquidGlass} from './core.js';

const glass = bindLiquidGlass({Skia, TileMode, FilterMode, MipmapMode, BlendMode, ClipOp, Picture, Group, React,
  pixelRatio: () => PixelRatio.get()});

/** A rounded piece of glass inside a React Native Skia `<Canvas>`. */
export const GlassSurface = glass.GlassSurface;
/** A clear magnifier inside a React Native Skia `<Canvas>`. */
export const GlassLens = glass.GlassLens;
/** The production painter over React Native Skia, for custom drawing (device pixels). */
export const createGlassPainter = glass.createPainter;
/** Record painter calls into an `SkPicture` (device pixels). */
export const recordGlass = glass.recordGlass;
export {bindLiquidGlass} from './core.js';
