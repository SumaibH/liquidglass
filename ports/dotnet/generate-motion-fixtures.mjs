// Apache-2.0. Independent portable implementation supplies the native controller oracle.
import {readFile, writeFile, mkdir} from 'node:fs/promises';
import {createHash} from 'node:crypto';
import {createCalmInteraction} from '../web/interaction.mjs';

const cases=[];
for (const [width,height,radius] of [[56,56,28],[278,62,31],[320,180,20]]) {
  for (const pullShape of ['adaptive','area-preserving']) {
    const options={width,height,radius,x:20,y:20,viewportWidth:width+40,viewportHeight:height+40,
      pullShape,...(pullShape==='area-preserving'?{pressScale:1.05,pressGrowth:0}:{})};
    const actions=[['press',0,0,0],['sample',.04],['sample',.12],['press',.13,50,-30],
      ['sample',.2],['press',.23,-1e6,1e6],['sample',.5],['release',.5],['sample',.6],
      ['press',.6,30,80],['sample',.8],['sample',.7],['release',.9],['sample',1.1],['sample',4],
      ['press',4,1e308,-1e308],['sample',4.4],['setReducedMotion',4.5,true],['sample',4.6],
      ['setReducedMotion',4.7,false],['press',4.7,20,30],['sample',4.8],
      ['resize',4.9,{width:width*.9,height:height*.9}],['sample',5]];
    const motion=createCalmInteraction(options), samples=[];
    for(const [operation,time,...args] of actions) {
      const expected=operation==='resize'?motion.resize(args[0],time):
        operation==='setReducedMotion'?motion.setReducedMotion(args[0],time):motion[operation](time,...args);
      samples.push({operation,time,args,expected});
    }
    cases.push({options,samples});
  }
}
const sources={};
for(const name of ['core','interaction']) sources[`ports/web/${name}.mjs`]=createHash('sha256')
  .update((await readFile(new URL(`../web/${name}.mjs`,import.meta.url),'utf8')).replace(/\r\n/g,'\n')).digest('hex');
const text=JSON.stringify({provenance:'Portable authored controller; cross-language equivalence, not an Apple motion oracle',sources,cases},null,2)+'\n';
const target=new URL('./Checks/fixtures/calm-motion.json',import.meta.url);
if(process.argv.includes('--check')) {
  if((await readFile(target,'utf8')).replace(/\r\n/g,'\n')!==text) throw new Error('Native motion fixtures stale; run node ports/dotnet/generate-motion-fixtures.mjs');
} else {await mkdir(new URL('./Checks/fixtures/',import.meta.url),{recursive:true});await writeFile(target,text);}
console.log(`${cases.length} native motion scenarios match portable source (${cases.reduce((n,c)=>n+c.samples.length,0)} samples)`);
