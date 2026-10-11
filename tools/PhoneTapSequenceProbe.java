/* Apache-2.0. Research only: exports the unchanged production controller's
 * leftward adjacent tap at the original source's exact scanline offsets.
 * Input durations and resting alternatives are authored, not recovered input.
 */
import com.wexpa.liquidglass.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public final class PhoneTapSequenceProbe {
    static final float[] ROWS = {120, 124, 128, 132, 134, 136, 146, 148, 150};
    static final int POINTS = 1024;

    static float[] cross(float[] polygon, float at, boolean horizontal) {
        float lo = Float.POSITIVE_INFINITY, hi = Float.NEGATIVE_INFINITY;
        int axis = horizontal ? 1 : 0, other = 1 - axis, count = 0;
        for (int i = 0; i < POINTS; i++) {
            int j = (i + 1) % POINTS;
            float a = polygon[2*i+axis], b = polygon[2*j+axis];
            if ((a <= at && b > at) || (b <= at && a > at)) {
                float value = polygon[2*i+other] + (at-a)/(b-a) *
                    (polygon[2*j+other]-polygon[2*i+other]);
                lo = Math.min(lo, value); hi = Math.max(hi, value); count++;
            }
        }
        if (count != 2 || !Float.isFinite(lo) || !Float.isFinite(hi))
            throw new IllegalStateException("Expected two contour intersections; got " + count);
        return new float[] {lo, hi};
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Supply new output CSV path");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Path.of(args[0]),
                StandardOpenOption.CREATE_NEW))) {
            out.print("restWidth,restHeight,pressSeconds,seconds,cx,height,formation");
            for (float y : ROWS) out.printf(Locale.ROOT, ",left%d,right%d", (int)y, (int)y);
            out.println();
            float[] polygon = new float[POINTS*2];
            for (float[] rest : new float[][] {{285,163}, {278,167.5f}, {271,172}}) {
                for (float duration : new float[] {.04f,.08f,.116f,.16f,.24f}) {
                    GlassSelectorBar bar = new GlassSelectorBar(834,186,21.75f,3,93,
                        rest[0]/2,rest[1]/2,90,150,150,3,800);
                    GlassPoseController c = new GlassPoseController(GlassPoseSpec.Companion.getCalm());
                    c.attach(bar,null,null,0); c.snapToRest(2);
                    c.pointerDown(bar.centreOf(1),bar.getCentreY(),0,true); c.retarget(1);
                    boolean released=false, acquired=false;
                    float sourceOffsetX=479-bar.centreOf(1);
                    for (int i=0; i<=480; i++) {
                        float t=i/480f;
                        if (duration>.12f && !acquired && t>=.12f) {
                            c.advanceTo(.12f,.5f);
                            c.beginDrag(bar.centreOf(1),bar.getCentreY()); acquired=true;
                        }
                        if (!released && t>=duration) {
                            c.advanceTo(duration,.5f); c.pointerUp(1); released=true;
                        }
                        c.advanceTo(t,.5f);
                        if(c.getSolverFailed() || (c.isHeld() && duration<=.12f))
                            throw new IllegalStateException("Invalid controller state");
                        c.getFrame().contour(POINTS,polygon);
                        float[] heights=new float[5];
                        for(int k=0;k<5;k++) {
                            float[] cut=cross(polygon,c.getCentreX()+(k-2)*8,false);
                            heights[k]=cut[1]-cut[0];
                        }
                        Arrays.sort(heights);
                        out.printf(Locale.ROOT,"%.3f,%.3f,%.3f,%.7f,%.5f,%.5f,%.7f",
                            rest[0],rest[1],duration,t,c.getCentreX()+sourceOffsetX,
                            heights[2],c.getFormation());
                        for(float y:ROWS) {
                            float[] cut=cross(polygon,bar.getCentreY()+y-125.5f,true);
                            out.printf(Locale.ROOT,",%.5f,%.5f",cut[0]+sourceOffsetX,cut[1]+sourceOffsetX);
                        }
                        out.println();
                    }
                }
            }
        }
    }
}
