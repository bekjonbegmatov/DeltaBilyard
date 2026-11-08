import {describe,it,expect} from 'vitest';
import {readFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import sources from './public/audio/sources.json';

describe('distributed sound assets',()=>{
 it('ships audible mono PCM files matching the verified source manifest',()=>{
  for(const source of sources){
   const file=readFileSync(new URL('./public/audio/'+source.file,import.meta.url));
   expect(createHash('sha256').update(file).digest('hex')).toBe(source.sha256);
   expect(file.toString('ascii',0,4)).toBe('RIFF');expect(file.toString('ascii',8,12)).toBe('WAVE');
   expect(file.readUInt16LE(22)).toBe(1);expect(file.readUInt32LE(24)).toBe(32000);expect(file.readUInt16LE(34)).toBe(16);
   let peak=0;for(let i=44;i<file.length;i+=2)peak=Math.max(peak,Math.abs(file.readInt16LE(i)));
   expect(peak).toBeGreaterThan(2000);expect(peak).toBeLessThan(32767);expect(source.license).toBe('CC0-1.0');
  }
 });
});
