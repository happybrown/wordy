#!/usr/bin/env node
// master.tsv -> content/batches/{deep,light}/XNNN.manifest.json (+ index.json). Deterministic; safe to re-run.
import { writeFileSync, mkdirSync, existsSync, readFileSync } from 'node:fs';
import { loadMaster, CONTENT, LEVELS } from './lib/master.mjs';
import { cleanForms } from './lib/forms.mjs';

const DEEP_SIZE = +(process.env.DEEP_SIZE || 20);
const LIGHT_SIZE = +(process.env.LIGHT_SIZE || 60);
const rows = loadMaster();
const order = Object.fromEntries(LEVELS.map((l, i) => [l, i]));
const sortKey = (a, b) => order[a.level] - order[b.level] || (a.freqRank ?? 99999) - (b.freqRank ?? 99999) || a.headword.localeCompare(b.headword);

// --- confusable candidates: edit distance <= 2 (len >= 4) or shared 5-letter stem (len >= 6), excluding own forms
const byLen = new Map();
for (const r of rows) { const L = r.headword.length; if (!byLen.has(L)) byLen.set(L, []); byLen.get(L).push(r.headword); }
function lev(a, b, max) {
  if (Math.abs(a.length - b.length) > max) return max + 1;
  let prev = Array.from({ length: b.length + 1 }, (_, i) => i);
  for (let i = 1; i <= a.length; i++) {
    const cur = [i]; let rowMin = i;
    for (let j = 1; j <= b.length; j++) { cur[j] = Math.min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1)); rowMin = Math.min(rowMin, cur[j]); }
    if (rowMin > max) return max + 1;
    prev = cur;
  }
  return prev[b.length];
}
function confusables(r) {
  const w = r.headword, out = new Set();
  if (w.length >= 4) for (let L = w.length - 1; L <= w.length + 1; L++) for (const c of byLen.get(L) || []) { if (c !== w && !r.forms.includes(c) && lev(w, c, 2) <= 2) out.add(c); }
  if (w.length >= 6) for (const c of rows) if (c.headword !== w && c.headword.length >= 6 && c.headword.slice(0, 5) === w.slice(0, 5) && !r.forms.includes(c.headword)) out.add(c.headword);
  return [...out].sort().slice(0, 6);
}
const manifestEntry = (r) => ({ id: r.id, headword: r.headword, level: r.level, isCore: r.isCore, pos_hint: r.pos_hint, forms: cleanForms(r.headword, r.forms, r.pos_hint), cefr: r.cefr || null, freqRank: r.freqRank, confusable_candidates: confusables(r) });

// 이미 작성된 배치가 있으면 재생성이 배치 구성을 흔들어 작업 중인 내용과 어긋날 수 있다.
// master.tsv 를 바꾼 뒤 재생성해야 할 때는 --force 를 준다.
const written = existsSync(`${CONTENT}batches/index.json`)
  ? JSON.parse(readFileSync(`${CONTENT}batches/index.json`, 'utf8')).batches
      .filter((b) => existsSync(`${CONTENT}batches/${b.kind}/${b.name}.json`))
  : [];
if (written.length && !process.argv.includes('--force')) {
  console.error(`이미 작성된 배치가 ${written.length}개 있습니다. 재생성하면 배치 구성이 바뀌어`);
  console.error('작업 중인 에이전트의 manifest 와 어긋날 수 있습니다.');
  console.error('master.tsv 를 바꾼 뒤 정말 재생성하려면 --force 를 주고, 재생성 후에는');
  console.error('node tools/validate.mjs --batch <이름> 으로 기존 배치를 다시 검증하세요.');
  process.exit(1);
}

const deep = rows.filter((r) => r.isCore).sort(sortKey);
const light = rows.filter((r) => !r.isCore).sort(sortKey);
const chunk = (arr, n) => Array.from({ length: Math.ceil(arr.length / n) }, (_, i) => arr.slice(i * n, i * n + n));
const index = { generatedFrom: 'content/wordlist/master.tsv', deepSize: DEEP_SIZE, lightSize: LIGHT_SIZE, batches: [] };
for (const [kind, list, size, prefix] of [['deep', deep, DEEP_SIZE, 'd'], ['light', light, LIGHT_SIZE, 'l']]) {
  mkdirSync(`${CONTENT}batches/${kind}`, { recursive: true });
  chunk(list, size).forEach((c, i) => {
    const name = `${prefix}${String(i + 1).padStart(3, '0')}`;
    const levels = [...new Set(c.map((r) => r.level))];
    const manifest = { batch: name, kind, levels, count: c.length, entries: c.map(manifestEntry) };
    writeFileSync(`${CONTENT}batches/${kind}/${name}.manifest.json`, JSON.stringify(manifest, null, 2) + '\n');
    index.batches.push({ name, kind, levels, count: c.length, ids: c.map((r) => r.id) });
  });
}
writeFileSync(`${CONTENT}batches/index.json`, JSON.stringify(index, null, 2) + '\n');
console.log(`deep: ${deep.length} words -> ${Math.ceil(deep.length / DEEP_SIZE)} batches (${DEEP_SIZE}/batch)`);
console.log(`light: ${light.length} words -> ${Math.ceil(light.length / LIGHT_SIZE)} batches (${LIGHT_SIZE}/batch)`);
for (const L of LEVELS) console.log(`  ${L}: deep ${deep.filter((r) => r.level === L).length}, light ${light.filter((r) => r.level === L).length}`);
