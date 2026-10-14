/* Apache-2.0. Research-only central shader diagnostic, using the existing JVM
 * renderer helpers. This is NOT a matched Phone scene or a full glyph comparison.
 * Compile against the jvmTest runtime classpath; supply inputCSV, new outputCSV.
 */
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import kotlin.Triple;
import kotlin.jvm.functions.Function2;

public final class PhoneTapOpticsProbe {
    static Object render(String name, int parameters, Map<Integer,Object> overrides) throws Exception {
        Class<?> type=Class.forName("com.wexpa.liquidglass."+name);
        Method method=Arrays.stream(type.getMethods()).filter(m->m.getName().equals("render$default")).findFirst().orElseThrow();
        int masks=(parameters+31)/32;
        if(method.getParameterCount()!=parameters+masks+2)throw new IllegalStateException("Renderer helper ABI changed");
        Object[] args=new Object[method.getParameterCount()];
        args[0]=type.getField("INSTANCE").get(null);
        for(int i=0;i<parameters;i++) {
            Class<?> p=method.getParameterTypes()[i+1];
            if(p==int.class)args[i+1]=0;
            else if(p==float.class)args[i+1]=0f;
            else if(p==boolean.class)args[i+1]=false;
        }
        int[] bits=new int[masks];Arrays.fill(bits,-1);
        for(var entry:overrides.entrySet()) {
            int index=entry.getKey();args[index+1]=entry.getValue();bits[index/32]&=~(1<<(index%32));
        }
        for(int i=0;i<masks;i++)args[parameters+1+i]=bits[i];
        return method.invoke(null,args);
    }
    static Function2<Integer,Integer,Integer> solid(int grey) {
        int color=0xff000000|(grey<<16)|(grey<<8)|grey;
        return (x,y)->color;
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("inputCSV and new outputCSV required");
        try(var reader=Files.newBufferedReader(Path.of(args[0]));
            var out=new PrintWriter(Files.newBufferedWriter(Path.of(args[1]),StandardOpenOption.CREATE_NEW))) {
            if(!reader.readLine().equals("frame,formation,observedBar"))throw new IllegalArgumentException("Unexpected input header");
            out.println("frame,formation,observedBar,renderedSelector,analyticalSelector,centralCircleWidth,centralCircleHeight");
            String line;
            while((line=reader.readLine())!=null) {
                String[] row=line.split(",");float f=Float.parseFloat(row[1]);int bar=Integer.parseInt(row[2]);
                if(!Float.isFinite(f)||f<0||f>1||bar<0||bar>255)throw new IllegalArgumentException("Invalid sample");
                Map<Integer,Object> p=new HashMap<>();
                p.put(0,400);p.put(1,260);p.put(2,24);p.put(21,2f);p.put(23,f);
                p.put(19,.5f*(1-f));p.put(20,new Triple<Float,Float,Float>(0f,0f,0f));
                p.put(30,-10f/255f*(1-f));p.put(46,solid(bar));
                int[] material=(int[])render("GlassRender",47,p);
                int pixel=material[(130+24)*(400+48)+200+24]&255;
                Map<Integer,Object> q=new HashMap<>();
                q.put(0,400);q.put(1,260);q.put(2,24);q.put(4,1f);q.put(5,0f);
                q.put(7,f);q.put(8,.12f*f);q.put(23,solid(0));
                Function2<Integer,Integer,Integer> circle=(x,y)->{
                    double dx=x-224.0,dy=y-154.0;
                    return dx*dx+dy*dy<=34.5*34.5?0xff149cff:0;
                };
                q.put(24,circle);
                Object result=render("GlassEndpointRender",25,q);
                int[] ink=(int[])result.getClass().getMethod("getInk").invoke(result);
                int minX=448,maxX=-1,minY=308,maxY=-1;
                for(int y=0;y<308;y++)for(int x=0;x<448;x++)if((ink[y*448+x]>>>24)>=180) {
                    minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                }
                if(maxX<0)throw new IllegalStateException("Missing rendered circle");
                out.printf(Locale.ROOT,"%s,%.7f,%d,%d,%.5f,%d,%d%n",row[0],f,bar,pixel,
                    bar*(1-.5f*(1-f))-10*(1-f),maxX-minX+1,maxY-minY+1);
            }
        }
    }
}
