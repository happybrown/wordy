// Parsers for the raw word-list sources in content/sources. Pure Node, no deps.
import { readFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';
import { readXlsx } from './xlsx.mjs';

export const SRC = new URL('../../content/sources/', import.meta.url).pathname;

export function parseCsv(text) {
  const rows = []; let row = []; let cell = ''; let q = false;
  text = text.replace(/^﻿/, '');
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (q) { if (c === '"') { if (text[i + 1] === '"') { cell += '"'; i++; } else q = false; } else cell += c; continue; }
    if (c === '"') q = true;
    else if (c === ',') { row.push(cell); cell = ''; }
    else if (c === '\n' || c === '\r') { if (c === '\r' && text[i + 1] === '\n') i++; row.push(cell); rows.push(row); row = []; cell = ''; }
    else cell += c;
  }
  if (cell !== '' || row.length) { row.push(cell); rows.push(row); }
  return rows;
}
const read = (f) => readFileSync(join(SRC, f), 'utf8');
const norm = (w) => w.trim().toLowerCase();

/** NGSL 1.2: Map<lemma, {rank, sfi, u}> */
export function ngslStats() {
  const m = new Map();
  for (const r of parseCsv(read('NGSL_12_stats.csv')).slice(1)) if (r[0]) m.set(norm(r[0]), { rank: +r[1], sfi: +r[2], u: +r[3] });
  return m;
}
/** headword -> Set(forms) from a "lemmatized_for_teaching" csv (## comment lines skipped) */
function lemmaForms(file) {
  const m = new Map();
  for (const line of read(file).split(/\r?\n/)) {
    if (!line || line.startsWith('##')) continue;
    const cells = line.replace(/^﻿/, '').split(',').map(norm).filter(Boolean);
    if (!cells.length) continue;
    const [head, ...forms] = cells;
    if (!m.has(head)) m.set(head, new Set());
    for (const f of forms) if (f !== head) m.get(head).add(f);
  }
  return m;
}
export const ngslForms = () => lemmaForms('NGSL_12_lemmatized_for_teaching.csv');
export const supForms = () => lemmaForms('SUP_lemmatized.csv');
export const nawlForms = () => lemmaForms('NAWL_12_lemmatized_for_teaching.csv');
export const bslForms = () => lemmaForms('BSL_120_lemmatized_for_teaching.csv');

/** NAWL definitions: Map<word, {def, pos}> (pos: adv/prep/n/v/adj…) */
export function nawlDefs() {
  const m = new Map();
  for (const r of parseCsv(read('NAWL_12_with_en_definitions.csv')).slice(1)) if (r[0]) m.set(norm(r[0]), { def: r[1], pos: (r[2] || '').trim() });
  return m;
}
/** BSL 1.20 stats: Map<word, {rank, band, sfi}> */
export function bslStats() {
  const m = new Map();
  for (const r of parseCsv(read('BSL_120_stats.csv')).slice(1)) if (r[0] && r[1]) m.set(norm(r[0]), { rank: +r[1], band: +r[2], sfi: +r[3] });
  return m;
}

const CEFR_ORDER = { A1: 1, A2: 2, B1: 3, B2: 4 };
/** CEFR-J 1.6 ALL sheet: Map<headword, {level(min), levels:{pos:level}, pos:Set, topics:Set, variants:[]}> */
export function cefrj() {
  const sheets = readXlsx(join(SRC, 'CEFRJ_wordlist_ver1.6.xlsx'));
  const rows = sheets['ALL']().slice(1);
  const m = new Map();
  for (const [hw, pos, lvl, t1, t2, t3] of rows) {
    if (!hw || !lvl) continue;
    const variants = hw.split('/').map(norm).filter(Boolean);
    const head = variants[0];
    if (!m.has(head)) m.set(head, { level: lvl, levels: {}, pos: new Set(), topics: new Set(), variants: variants.slice(1) });
    const e = m.get(head);
    if (CEFR_ORDER[lvl] < CEFR_ORDER[e.level]) e.level = lvl;
    e.levels[pos] = e.levels[pos] && CEFR_ORDER[e.levels[pos]] <= CEFR_ORDER[lvl] ? e.levels[pos] : lvl;
    e.pos.add(pos);
    for (const t of [t1, t2, t3]) if (t) e.topics.add(t);
  }
  return m;
}

/**
 * 교육부 2015 기본 어휘 목록 (pdftotext -layout). Multi-column layout: split each line on 2+ spaces.
 * Returns Map<headword, {star: 0|1|2, variants:[]}>; star 1 = 초등 800, star 2 = 진로선택/전문교과 400.
 */
/**
 * 교육부 고시 원문의 표기 오류를 바로잡는다.
 * 공식 문서라도 비표준 철자가 있으면 학습자에게는 표준형을 가르쳐야 한다.
 */
const MOE_SPELLING_FIX = new Map([
  ['livingroom', 'living room'],
]);

export function moe() {
  const lines = read('MOE_2015-74_appendix14_english.txt').split(/\r?\n/);
  const start = lines.findIndex((l, i) => i > 6000 && /^\s*기본 어휘 목록\s*$/.test(l));
  const zoo = lines.findIndex((l, i) => i > start && /\bzoo\b/.test(l));
  // the list may spill one or more lines past 'zoo' (e.g. 'withdraw **'): consume until the first blank line
  let end = zoo; while (end + 1 < lines.length && lines[end + 1].trim()) end++;
  if (start < 0 || end < 0) throw new Error('MOE list boundaries not found');
  const m = new Map();
  const wordRe = /^[A-Za-z][A-Za-z'\-.]*(?: [A-Za-z][A-Za-z'\-.]*)?$/; // allow one internal space (e.g. "ice cream")
  for (let i = start + 1; i <= end; i++) {
    const line = lines[i];
    if (!line.trim()) continue;
    for (const raw of line.trim().split(/\s{2,}/)) {
      const cell = raw.trim();
      if (!cell || /^[A-Z]$/.test(cell) || /^\d+$/.test(cell) || /[가-힣【】]/.test(cell)) continue;
      // stars may follow any variant: "airplane * / plane / aeroplane"
      let star = 0;
      const stripped = cell.replace(/\*+/g, (s) => { star = Math.max(star, s.length); return ' '; });
      const variants = [...new Set(stripped.split('/').map(norm).filter(Boolean))];
      if (!variants.length || !variants.every((v) => wordRe.test(v))) { m.__rejects = (m.__rejects || []).concat(cell); continue; }
      const head = variants[0];
      if (m.has(head)) { m.__dups = (m.__dups || []).concat(head); continue; }
      const fixed = MOE_SPELLING_FIX.get(head) ?? head;
      m.set(fixed, { star, variants: variants.slice(1) });
    }
  }
  return m;
}

if (process.argv[1] && /(^|\/)sources\.mjs$/.test(process.argv[1])) {
  const ng = ngslStats(), nf = ngslForms(), sup = supForms(), na = nawlForms(), nd = nawlDefs(), bs = bslStats(), bf = bslForms(), cj = cefrj(), mo = moe();
  console.log('NGSL stats', ng.size, '| forms', nf.size, '| SUP', sup.size);
  console.log('NAWL forms', na.size, '| defs', nd.size, '| BSL stats', bs.size, '| BSL forms', bf.size);
  console.log('CEFR-J heads', cj.size, 'levels:', Object.entries([...cj.values()].reduce((a, e) => (a[e.level] = (a[e.level] || 0) + 1, a), {})).join(' '));
  const stars = [...mo.values()].reduce((a, e) => (a[e.star] = (a[e.star] || 0) + 1, a), {});
  console.log('MOE total', mo.size, 'stars:', JSON.stringify(stars), '| rejects', (mo.__rejects || []).length, '| dups', (mo.__dups || []).length);
  if (mo.__rejects) console.log('  rejects sample:', mo.__rejects.slice(0, 15));
  if (mo.__dups) console.log('  dups sample:', mo.__dups.slice(0, 15));
  const withVariants = [...mo.entries()].filter(([, e]) => e.variants.length);
  console.log('MOE variants sample:', withVariants.slice(0, 8).map(([h, e]) => h + '=' + e.variants.join('|')).join(', '));
  const notInNgsl = [...mo.keys()].filter((w) => !ng.has(w));
  console.log('MOE words not in NGSL:', notInNgsl.length, notInNgsl.slice(0, 20).join(' '));
  const notInCefrj = [...mo.keys()].filter((w) => !cj.has(w));
  console.log('MOE words not in CEFR-J:', notInCefrj.length, notInCefrj.slice(0, 20).join(' '));
}
