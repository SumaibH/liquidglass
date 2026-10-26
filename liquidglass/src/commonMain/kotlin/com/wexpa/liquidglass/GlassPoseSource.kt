package com.wexpa.liquidglass

/**
 * The pose body's geometry, as one shared piece of SkSL.
 *
 * The panel pass, the content pass and the aperture pass all include this text, so there is
 * exactly one distance field, one normal and one curvature for the deforming body rather than
 * three transcriptions that can drift apart — the discrepancy the brief's section 6.3 tells this
 * pass not to repeat. `GlassPoseShaderAgreementTest` reads coordinates back out of a real render
 * and asserts the GPU agrees with [GlassPoseFrame] on the CPU.
 *
 * The uniforms carry the pose already reduced on the CPU, because `exp`, `cosh` and `sinh` are
 * per-frame quantities and have no business running per pixel:
 *
 * ```
 * uPoseA    (A00, A01, A10, A11)      the 2x2 pose matrix
 * uPoseAInv (i00, i01, i10, i11)      its inverse
 * uPoseC    (cx, cy, bx, by)          reference centre, and b = k / R
 * uPoseD    (spine, radius, scaleX, band)
 * uPoseE    (squircle, exponent, 0, 0)  the superellipse blend (#93); 0 is the capsule
 * ```
 *
 * `scaleX` is candidate A's horizontal accommodation and is exactly 1 for candidate B, whose
 * accommodation is already inside `spine`. Both branches evaluate the **same** capsule; which
 * space they evaluate it in is the only difference, and both agree on the contour.
 *
 * Everything here is constant work per pixel: one 2x2 multiply, one square root for the stable
 * quadratic inverse, one capsule distance, one 2x2 inverse-transpose for the normal. No
 * iteration, no angle tracking, no distance field.
 */
internal val GLASS_POSE_SKSL = """
// The stable inverse of section 6.2. Returns z; `valid` is 0 when the discriminant is negative,
// which cannot happen on or inside the body and means "outside coverage", not NaN.
float3 poseInverse(float2 x, float4 uPoseAInv, float4 uPoseC) {
    float2 d = x - uPoseC.xy;
    float2 y = float2(uPoseAInv.x * d.x + uPoseAInv.y * d.y,
                      uPoseAInv.z * d.x + uPoseAInv.w * d.y);
    float2 b = uPoseC.zw;
    float h = dot(b, y);
    float disc = 1.0 + 4.0 * h;
    if (disc < 0.0) return float3(y, 0.0);
    // 2 / (1 + sqrt(1 + 4h)): the root that stays continuous at h = 0 instead of cancelling.
    float lambda = 2.0 / (1.0 + sqrt(disc));
    return float3(lambda * y, 1.0);
}

/**
 * `(coverageDistance, normalX, normalY, curvature)` for the pose body, matching the four-vector
 * the old two-disk `bodyQuery` returned so the optical passes downstream are unchanged.
 *
 * coverageDistance is the first-order normal-corrected distance `d/|grad|`. It is NOT an exact
 * deep Euclidean SDF and is deliberately named for what it is used for.
 */
float4 poseQuery(float2 x, float4 uPoseA, float4 uPoseAInv, float4 uPoseC, float4 uPoseD) {
    float spine  = uPoseD.x;
    float radius = uPoseD.y;
    float scaleX = uPoseD.z;

    float3 zi = poseInverse(x, uPoseAInv, uPoseC);
    float2 z = zi.xy;
    if (zi.z < 0.5) {
        float len = max(length(z), 1e-4);
        return float4(len, z.x / len, z.y / len, 0.0);
    }

    // The canonical capsule. Candidate B evaluates it directly in z, which keeps the piecewise
    // material stretch out of boundary lighting; candidate A evaluates it in q and carries C in
    // the Jacobian below. Both describe the same contour.
    float2 e;
    float2 w;
    float halfSpine;
    if (scaleX == 1.0) {
        halfSpine = spine;
        w = z;
        e = float2(z.x - clamp(z.x, -halfSpine, halfSpine), z.y);
    } else {
        float2 q = float2(z.x / max(scaleX, 1e-6), z.y);
        halfSpine = spine / max(scaleX, 1e-6);
        w = q;
        e = float2(q.x - clamp(q.x, -halfSpine, halfSpine), q.y);
    }
    float len = length(e);
    float d = len - radius;
    float2 n0 = (len > 1e-5) ? (e / len) : float2(0.0, 1.0);
    // The squircle (#93): the held lens's outline is a superellipse |x/A|^n + |y/B|^n = 1 over the
    // capsule's own extent (A = halfSpine + radius, B = radius), blended in by uPoseE.x; iOS's held lens
    // fits n = 3 (IMG_6756 35.5 s). The first-order distance (f - 1) / |grad f| is exact on both axes.
    float sq = clamp(uPoseE.x, 0.0, 1.0);
    if (sq > 0.0 && radius > 1e-5) {
        float pn = max(uPoseE.y, 2.0);
        float A = halfSpine + radius;
        float B = radius;
        float ax = abs(w.x) / A;
        float ay = abs(w.y) / B;
        float f = pow(pow(ax, pn) + pow(ay, pn), 1.0 / pn);
        float dS = -B;
        float2 nS = float2(0.0, 1.0);
        if (f > 1e-3) {
            float k = pow(f, 1.0 - pn);
            float2 gS = float2(sign(w.x) * pow(ax, pn - 1.0) / A, sign(w.y) * pow(ay, pn - 1.0) / B) * k;
            float gn = max(length(gS), 1e-6);
            dS = (f - 1.0) / gn;
            nS = gS / gn;
        }
        d = mix(d, dS, sq);
        float2 nm = mix(n0, nS, sq);
        float nl = length(nm);
        n0 = (nl > 1e-5) ? (nm / nl) : nS;
    }

    // J = A * DF_k(z) * (C for candidate A);  DF_k(z) = s I + z b^T
    float2 b = uPoseC.zw;
    float s = 1.0 + dot(b, z);
    float j0 = s + z.x * b.x;
    float j1 = z.x * b.y;
    float j2 = z.y * b.x;
    float j3 = s + z.y * b.y;
    if (scaleX != 1.0) { j0 *= scaleX; j2 *= scaleX; }
    float m0 = uPoseA.x * j0 + uPoseA.y * j2;
    float m1 = uPoseA.x * j1 + uPoseA.y * j3;
    float m2 = uPoseA.z * j0 + uPoseA.w * j2;
    float m3 = uPoseA.z * j1 + uPoseA.w * j3;
    float det = m0 * m3 - m1 * m2;
    float invDet = (abs(det) > 1e-9) ? (1.0 / det) : 0.0;
    // grad = J^-T n0, written out rather than left to the reader's transpose.
    float2 g = float2(invDet * (m3 * n0.x - m2 * n0.y),
                      invDet * (-m1 * n0.x + m0 * n0.y));
    float gl = length(g);
    if (gl <= 1e-7) return float4(d, n0.x, n0.y, 0.0);

    // Curvature, transported to first order by the tangential stretch: exact for an affine map,
    // an approximation once the taper is on, which is why it is named transported.
    float kappa = 0.0;
    if (len > 1e-5 && radius > 1e-5) {
        float2 t = float2(-n0.y, n0.x);
        float2 st = float2(m0 * t.x + m1 * t.y, m2 * t.x + m3 * t.y);
        kappa = (1.0 / radius) / max(length(st), 1e-5);
    }
    return float4(d / gl, g.x / gl, g.y / gl, kappa);
}

/** The optical band, precomputed on the CPU as the canonical band under the smallest stretch. */
float poseBand(float4 uPoseD) { return uPoseD.w; }

/**
 * How far the nearest feature is an end cap rather than the straight run, 1 to 0, blended over a
 * short geometric distance so the transport profile does not step where the nearest feature
 * changes identity. Evaluated in z, where the joins are at +-spine whatever the pose is.
 */
float poseCorner(float2 x, float4 uPoseAInv, float4 uPoseC, float4 uPoseD) {
    float3 zi = poseInverse(x, uPoseAInv, uPoseC);
    float spine = uPoseD.x;
    float scaleX = uPoseD.z;
    float zx = (scaleX == 1.0) ? zi.x : (zi.x / max(scaleX, 1e-6));
    float halfSpine = (scaleX == 1.0) ? spine : (spine / max(scaleX, 1e-6));
    float soft = max(0.25 * uPoseD.y, 1.0);
    // Full from where the cap begins, ramping in along the run, as cornerWeight and bodyCorner do;
    // straddling the cap start left the cap's shoulders and a circular body's middle at half.
    return clamp(smoothstep(halfSpine - soft, halfSpine, abs(zx)), 0.0, 1.0);
}
"""
