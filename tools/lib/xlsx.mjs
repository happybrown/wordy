// Minimal dependency-free .xlsx reader (shared strings + inline strings + numbers).
// Usage as CLI: node tools/lib/xlsx.mjs <file.xlsx> [SheetName]  -> TSV on stdout
import { readFileSync } from 'node:fs';
import { inflateRawSync } from 'node:zlib';

function readZip(buf) {
  // Locate End Of Central Directory record
  let eocd = -1;
  for (let i = buf.length - 22; i >= Math.max(0, buf.length - 66000); i--) {
    if (buf.readUInt32LE(i) === 0x06054b50) { eocd = i; break; }
  }
  if (eocd < 0) throw new Error('not a zip: EOCD not found');
  const count = buf.readUInt16LE(eocd + 10);
  let off = buf.readUInt32LE(eocd + 16);
  const entries = new Map();
  for (let n = 0; n < count; n++) {
    if (buf.readUInt32LE(off) !== 0x02014b50) throw new Error('bad central dir');
    const method = buf.readUInt16LE(off + 10);
    const csize = buf.readUInt32LE(off + 20);
    const nameLen = buf.readUInt16LE(off + 28);
    const extraLen = buf.readUInt16LE(off + 30);
    const commentLen = buf.readUInt16LE(off + 32);
    const localOff = buf.readUInt32LE(off + 42);
    const name = buf.toString('utf8', off + 46, off + 46 + nameLen);
    entries.set(name, { method, csize, localOff });
    off += 46 + nameLen + extraLen + commentLen;
  }
  return {
    read(name) {
      const e = entries.get(name);
      if (!e) return null;
      const lo = e.localOff;
      if (buf.readUInt32LE(lo) !== 0x04034b50) throw new Error('bad local header');
      const nameLen = buf.readUInt16LE(lo + 26);
      const extraLen = buf.readUInt16LE(lo + 28);
      const start = lo + 30 + nameLen + extraLen;
      const data = buf.subarray(start, start + e.csize);
      return e.method === 8 ? inflateRawSync(data) : Buffer.from(data);
    },
    names: [...entries.keys()],
  };
}

const decode = (s) => s
  .replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"')
  .replace(/&apos;/g, "'").replace(/&#(\d+);/g, (_, d) => String.fromCodePoint(+d))
  .replace(/&#x([0-9a-f]+);/gi, (_, h) => String.fromCodePoint(parseInt(h, 16)))
  .replace(/&amp;/g, '&');

function textOf(xml) {
  // concatenate all <t>…</t> (rich-text runs included)
  let out = '';
  for (const m of xml.matchAll(/<t(?:\s[^>]*)?>([^<]*)<\/t>/g)) out += m[1];
  return decode(out);
}

function colIndex(ref) {
  let n = 0;
  for (const ch of ref.replace(/\d+/g, '')) n = n * 26 + (ch.charCodeAt(0) - 64);
  return n - 1;
}

export function readXlsx(path) {
  const zip = readZip(readFileSync(path));
  const wb = zip.read('xl/workbook.xml').toString('utf8');
  const rels = zip.read('xl/_rels/workbook.xml.rels').toString('utf8');
  const relMap = new Map();
  for (const m of rels.matchAll(/<Relationship\s[^>]*?Id="([^"]+)"[^>]*?Target="([^"]+)"/g)) relMap.set(m[1], m[2]);
  for (const m of rels.matchAll(/<Relationship\s[^>]*?Target="([^"]+)"[^>]*?Id="([^"]+)"/g)) relMap.set(m[2], m[1]);
  const sst = [];
  const ss = zip.read('xl/sharedStrings.xml');
  if (ss) for (const m of ss.toString('utf8').matchAll(/<si>([\s\S]*?)<\/si>/g)) sst.push(textOf(m[1]));
  const sheets = {};
  for (const m of wb.matchAll(/<sheet\s[^>]*?name="([^"]+)"[^>]*?r:id="([^"]+)"/g)) {
    const name = decode(m[1]);
    let target = relMap.get(m[2]);
    if (!target) continue;
    target = target.replace(/^\//, '');
    if (!target.startsWith('xl/')) target = 'xl/' + target;
    sheets[name] = () => parseSheet(zip.read(target).toString('utf8'), sst);
  }
  return sheets; // name -> lazy loader returning rows[][]
}

function parseSheet(xml, sst) {
  const rows = [];
  for (const rm of xml.matchAll(/<row\b[^>]*>([\s\S]*?)<\/row>/g)) {
    const row = [];
    for (const cm of rm[1].matchAll(/<c\s([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)) {
      const attrs = cm[1];
      const ref = /r="([A-Z]+)\d+"/.exec(attrs)?.[1];
      const t = /\bt="([^"]+)"/.exec(attrs)?.[1];
      const inner = cm[2] ?? '';
      let val = '';
      if (t === 's') { const v = /<v>([^<]*)<\/v>/.exec(inner)?.[1]; val = v !== undefined ? sst[+v] : ''; }
      else if (t === 'inlineStr') val = textOf(inner);
      else { const v = /<v>([^<]*)<\/v>/.exec(inner)?.[1]; val = v !== undefined ? decode(v) : ''; }
      if (ref) row[colIndex(ref)] = val; else row.push(val);
    }
    for (let i = 0; i < row.length; i++) if (row[i] === undefined) row[i] = '';
    rows.push(row);
  }
  return rows;
}

if (process.argv[1] && import.meta.url.endsWith(process.argv[1].split('/').pop())) {
  const [file, sheet] = process.argv.slice(2);
  if (!file) { console.error('usage: xlsx.mjs <file.xlsx> [Sheet]'); process.exit(1); }
  const sheets = readXlsx(file);
  if (!sheet) { console.log(Object.keys(sheets).join('\n')); process.exit(0); }
  for (const r of sheets[sheet]()) console.log(r.map((c) => String(c).replace(/[\t\n\r]+/g, ' ')).join('\t'));
}
