/** Apache-2.0. Framework-independent surfaces and lenses using production SkSL. */
import {compileGlassEffects, clearMaterialUniforms} from './renderer.mjs';
import {inAppMaterialUniforms} from './material.mjs';
import {createBackdropSampler} from './backdrop.mjs';
import {glassShaderSources} from './shaders.mjs';
import {calmSelectorMaterial, calmBarMaterial, calmMaterialUniforms} from './selector-material.mjs';

/**
 * Compile once, then draw surfaces and lenses over one caller-owned Skia Image.
 * Coordinates and image dimensions are device pixels with a top-left origin.
 * Production shaders are included; optional sources support advanced/custom shader hosts.
 * This object owns its effects and paint; it borrows, and never deletes, the source image.
 */
export function createGlassPainter(kit, sources = glassShaderSources) {
  const effects = compileGlassEffects(kit, sources);
  let paint;
  try { paint = new kit.Paint(); }
  catch (error) { effects.dispose(); throw error; }
  let source = null, disposed = false, sampler=null;
  function checkAlive() { if (disposed) throw new Error('Glass painter disposed'); }
  // The material pass's share of a Calm material (ink-only terms belong to the content pass).
  const assignMaterial=(uniforms,values)=>{for(const[k,v]of Object.entries(values))if(k in uniforms)uniforms[k]=v;return uniforms;};
  // Compose's elementSizeFactor: 0 for chrome up to 56 dp, 1 from 320 dp.
  const sizeFactor=(width,height,density)=>Math.max(0,Math.min(1,(Math.min(width,height)/density-56)/264));
  // The backdrop with the glass the lens looks through over it, plus rawShare of the raw page.
  function composeThrough(canvas,through,rawShare) {
    const surface=canvas.makeSurface({width:source.width(),height:source.height(),colorType:kit.ColorType.RGBA_8888,
      alphaType:kit.AlphaType.Premul,colorSpace:kit.ColorSpace.SRGB});
    if(!surface)throw new Error('Could not allocate the selector backdrop');
    const add=new kit.Paint();
    try {
      const target=surface.getCanvas();target.clear(kit.BLACK);target.drawImage(through,0,0);
      if(rawShare>.001){add.setBlendMode(kit.BlendMode.Plus);add.setAlphaf(rawShare);target.drawImage(source,0,0,add);}
      surface.flush();return surface.makeImageSnapshot();
    } finally {add.delete();surface.delete();}
  }
  // Consumes the temporary backdrop shader; both public drawing paths preserve identical
  // coverage, page coordinates, host canvas state and exception-safe shader lifetime.
  function draw(canvas,uniforms,backdrop,originX,originY) {
    let material;
    try {
      uniforms.uBackdrop=[-originX,-originY,source.width()-originX,source.height()-originY];
      material=effects.shader('material',uniforms,{content:backdrop,field:backdrop});
      paint.setShader(material);canvas.save();
      try {
        canvas.translate(originX,originY);
        canvas.drawRect(kit.XYWHRect(0,0,uniforms.uSize[0]+2*uniforms.uPad,
          uniforms.uSize[1]+2*uniforms.uPad),paint);
      } finally {canvas.restore();}
    } finally {paint.setShader(null);material?.delete();backdrop.delete();}
  }

  return {
    /** Replace after backdrop changes. Keep the image alive until replacement or disposal. */
    setSource(image) {
      checkAlive();
      if (!image || typeof image.makeShaderOptions !== 'function' ||
          typeof image.width !== 'function' || typeof image.height !== 'function' ||
          !(image.width() > 0) || !(image.height() > 0))
        throw new TypeError('Source must be a live, nonempty CanvasKit Image');
      sampler?.reset();source = image;
    },

    /** Draw a rounded in-app material, including its wide tone kernel. Labels stay with the host. */
    drawSurface(canvas,{x,y,width,height,radius=20},options={}) {
      checkAlive();
      if(!source)throw new Error('Call setSource before drawing glass');
      if(![x,y,width,height].every(Number.isFinite)||width<=0||height<=0)
        throw new RangeError('Surface requires finite position and positive dimensions');
      const pad=2,originX=x-pad,originY=y-pad;
      const uniforms=inAppMaterialUniforms(effects,{...options,width,height,radius,pad});
      sampler??=createBackdropSampler(kit);
      const backdrop=sampler.shader(canvas,source,originX,originY,uniforms.uWideKernel);
      uniforms.uWideStrip=backdrop.strip;
      draw(canvas,uniforms,backdrop.shader,originX,originY);
    },

    /** Draw a clear magnifier, preserving the host canvas transform and clip. */
    drawLens(canvas, {x, y, width, height = width, magnification = 1.25}) {
      checkAlive();
      if (!source) throw new Error('Call setSource before drawing glass');
      if (![x,y,width,height,magnification].every(Number.isFinite) || width <= 0 || height <= 0 ||
          magnification < 1 || magnification > 2.5)
        throw new RangeError('Lens requires finite geometry, positive dimensions and 1–2.5 magnification');
      // The material's local padded coordinates must sample the same global page coordinates
      // that the host drew. A local shader matrix is inverted by Skia during sampling.
      const pad = 2;
      const originX = x - pad, originY = y - pad;
      const uniforms = clearMaterialUniforms(effects, {width, height, pad, magnification});
      const backdrop = source.makeShaderOptions(
        kit.TileMode.Clamp, kit.TileMode.Clamp, kit.FilterMode.Linear, kit.MipmapMode.None,
        kit.Matrix.translated(-originX, -originY),
      );
      draw(canvas,uniforms,backdrop,originX,originY);
    },

    /**
     * Draw a Calm navigation bar's glass with the Compose Calm bar material (GlassTabBarStyle.Calm,
     * its held lift following the selector's `formation`). Device px; `density` is px per dp.
     */
    drawCalmBar(canvas,{x,y,width,height},{dark=true,density=1,formation=0,materialize=1,
      reducedTransparency=0,increasedContrast=0}={}) {
      checkAlive();
      if(!source)throw new Error('Call setSource before drawing glass');
      if(![x,y,width,height,density,formation].every(Number.isFinite)||width<=0||height<=0||density<=0)
        throw new RangeError('Calm bar requires finite geometry, positive dimensions and density');
      const pad=2,originX=x-pad,originY=y-pad;
      const uniforms=clearMaterialUniforms(effects,{width,height,pad,magnification:1});
      assignMaterial(uniforms,calmMaterialUniforms(calmBarMaterial({dark,formation}),density));
      Object.assign(uniforms,{
        uRadii:Array(4).fill(height/2),uRefractBand:.6*height/2,uFormation:0,
        uScale:sizeFactor(width,height,density),uMaterialize:materialize,uFrost:reducedTransparency,uContrast:increasedContrast,
      });
      sampler??=createBackdropSampler(kit);
      const backdrop=sampler.shader(canvas,source,originX,originY,uniforms.uWideKernel);
      // Compose records the wide strip only for a kernel of at least a pixel (LiquidGlass.kt useWide).
      uniforms.uWideStrip=uniforms.uWideKernel>=1?backdrop.strip:0;
      draw(canvas,uniforms,backdrop.shader,originX,originY);
    },

    /**
     * Draw a navigation selector's deformed body (a CalmSelectorController frame) over the bar
     * at device rect `bar`. `pose` is `controller.render()` in the same device px as `bar`;
     * `formation`, `transit` and `heldTravel` are the controller's. The material is the Compose
     * Calm selector's own (calmSelectorMaterial: the production pill and lens styles, fold and
     * rim mirror). Like Compose, the lens looks through the bar: pass `through`, an image the
     * size of the source holding the backdrop with the bar's glass drawn over it, and a share of
     * the raw backdrop is added on top; without it the lens sees the backdrop alone.
     * Labels and selected ink stay with the host.
     */
    drawSelector(canvas,{x,y,width,height},{pose,formation=0,transit=0,heldTravel=0},options={}) {
      checkAlive();
      if(!source)throw new Error('Call setSource before drawing glass');
      if(![x,y,width,height,formation,transit,heldTravel].every(Number.isFinite)||width<=0||height<=0)
        throw new RangeError('Selector requires a finite bar rect, formation, transit and held travel');
      if(!pose||![pose.a,pose.aInv,pose.c,pose.d].every(v=>Array.isArray(v)&&v.length===4&&v.every(Number.isFinite)))
        throw new TypeError('Selector requires a pose from CalmSelectorController.render()');
      const {dark=true,density=1,materialize=1,reducedTransparency=0,increasedContrast=0,through=null}=options;
      if(!Number.isFinite(density)||density<=0)throw new RangeError('Selector density must be positive');
      // The body may stand proud of the bar while held and protrude past its ends in transit.
      const mx=height*.6,my=height*.4,nodeW=width+2*mx,nodeH=height+2*my,pad=2;
      const originX=x-mx-pad,originY=y-my-pad;
      const {values,uFormation}=calmSelectorMaterial({dark,formation,transit,heldTravel});
      const uniforms=clearMaterialUniforms(effects,{width:nodeW,height:nodeH,pad,magnification:1});
      // Pose c is bar-local; the node is centred on the bar, as the shader expects.
      const [cx,cy,bx,by]=pose.c;
      assignMaterial(uniforms,calmMaterialUniforms(values,density));
      Object.assign(uniforms,{
        uFormation,uBodyKind:2,uPoseA:pose.a,uPoseAInv:pose.aInv,uPoseD:pose.d,
        uPoseC:[cx-width/2,cy-height/2,bx,by],uRefractBand:pose.d[3],
        uRadii:Array(4).fill(Math.min(nodeW,nodeH)/2),uScale:sizeFactor(nodeW,nodeH,density),
        uMaterialize:materialize,uFrost:reducedTransparency,uContrast:increasedContrast,
      });
      sampler??=createBackdropSampler(kit);
      let composed=null;
      try {
        let view=source;
        if(through) {
          if(typeof through.width!=='function'||through.width()!==source.width()||through.height()!==source.height())
            throw new TypeError('through must be a live image the size of the source');
          composed=composeThrough(canvas,through,values.rawShare);view=composed;
        }
        const backdrop=sampler.shader(canvas,view,originX,originY,Math.max(uniforms.uWideKernel,1));
        uniforms.uWideStrip=uniforms.uWideKernel>=1?backdrop.strip:0;
        draw(canvas,uniforms,backdrop.shader,originX,originY);
      } finally {
        // The tone buffer was built from the composed image; drop it with the image.
        if(composed){sampler.reset();composed.delete();}
      }
    },

    /**
     * Draw the selected ink inside the selector, as Compose's content pass does: `ink` is an image
     * of the bar's items drawn in their selected appearance on a transparent ground, in device px
     * with its origin at the bar's top-left (the same layout as the ordinary row the host draws on
     * the bar). Only what lies under the selector shows, refracted, colour-split and folded by the
     * same body, map and band as `drawSelector`; draw it right after `drawSelector`. Every item may
     * be in the image: the lens shows each in its selected colour as it passes (as Calm does).
     */
    drawSelectorInk(canvas,{x,y,width,height},{pose,formation=0,transit=0,heldTravel=0},ink,options={}) {
      checkAlive();
      if(![x,y,width,height,formation,transit,heldTravel].every(Number.isFinite)||width<=0||height<=0)
        throw new RangeError('Selector ink requires a finite bar rect, formation, transit and held travel');
      if(!pose||![pose.a,pose.aInv,pose.c,pose.d].every(v=>Array.isArray(v)&&v.length===4&&v.every(Number.isFinite)))
        throw new TypeError('Selector ink requires a pose from CalmSelectorController.render()');
      if(!ink||typeof ink.makeShaderOptions!=='function')throw new TypeError('Selector ink requires a live CanvasKit Image');
      const {dark=true,density=1,materialize=1}=options;
      if(!Number.isFinite(density)||density<=0)throw new RangeError('Selector density must be positive');
      const mx=height*.6,my=height*.4,nodeW=width+2*mx,nodeH=height+2*my,pad=2;
      const originX=x-mx-pad,originY=y-my-pad;
      const {values,uFormation}=calmSelectorMaterial({dark,formation,transit,heldTravel});
      const px=calmMaterialUniforms(values,density);
      const material=clearMaterialUniforms(effects,{width:nodeW,height:nodeH,pad,magnification:1});
      const [cx,cy,bx,by]=pose.c;
      Object.assign(material,px,{
        uFormation,uBodyKind:2,uPoseA:pose.a,uPoseAInv:pose.aInv,uPoseD:pose.d,
        uPoseC:[cx-width/2,cy-height/2,bx,by],uRefractBand:pose.d[3],
        uRadii:Array(4).fill(Math.min(nodeW,nodeH)/2),uScale:sizeFactor(nodeW,nodeH,density),uMaterialize:materialize,
      });
      // The content pass shares the material's geometry and optics; it emits the ink premultiplied
      // and clipped to the body (uEndpointAlpha 0), to be drawn over the material.
      const uniforms={};
      for(const u of effects.layout('content'))
        uniforms[u.name]=u.name in material?material[u.name]:(u.columns*u.rows===1?0:Array(u.columns*u.rows).fill(0));
      uniforms.uEndpointAlpha=0;uniforms.uInkSplit=px.uInkSplit;uniforms.uHeldInk=px.uHeldInk;
      let shader,content;
      try {
        // The ink's origin is the bar's: in the padded node frame that is (mx + pad, my + pad).
        content=ink.makeShaderOptions(kit.TileMode.Decal,kit.TileMode.Decal,kit.FilterMode.Linear,kit.MipmapMode.None,
          kit.Matrix.translated(mx+pad,my+pad));
        shader=effects.shader('content',uniforms,{content,field:content});
        paint.setShader(shader);canvas.save();
        try {
          canvas.translate(originX,originY);
          canvas.drawRect(kit.XYWHRect(0,0,nodeW+2*pad,nodeH+2*pad),paint);
        } finally {canvas.restore();}
      } finally {paint.setShader(null);shader?.delete();content?.delete();}
    },

    dispose() {
      if (!disposed) {
        disposed = true;
        source = null;
        paint.delete();
        sampler?.dispose();sampler=null;
        effects.dispose();
      }
    },
  };
}
