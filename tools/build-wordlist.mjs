#!/usr/bin/env node
// Builds content/wordlist/master.tsv from the raw sources with a deterministic, auditable level mapping.
// Usage: node tools/build-wordlist.mjs   (writes master.tsv + report.md)
import { readFileSync, writeFileSync } from 'node:fs';
import { ngslStats, ngslForms, supForms, nawlForms, nawlDefs, bslStats, bslForms, cefrj, moe } from './lib/sources.mjs';

const ROOT = new URL('../', import.meta.url).pathname;
const OUT = ROOT + 'content/wordlist/';
const TARGET = { PRE: 300, ELEM: 800, MID: 1200, HIGH: 1150, ADV: 600 };
const CORE_TARGET = 400;
const LEVELS = ['PRE', 'ELEM', 'MID', 'HIGH', 'ADV'];

const readList = (f) => new Set(readFileSync(OUT + 'seeds/' + f, 'utf8').split(/\r?\n/).map((l) => l.trim().toLowerCase()).filter((l) => l && !l.startsWith('#')));
const preSeed = readList('pre-seed.txt');
const coreSeed = readList('core-seed.txt');
const functionWords = readList('functionwords.txt');

const NG = ngslStats(), NGF = ngslForms(), SUP = supForms(), NAF = nawlForms(), NAD = nawlDefs(), BS = bslStats(), BSF = bslForms(), CJ = cefrj(), MO = moe();

const POS_MAP = { noun: 'n', verb: 'v', adjective: 'adj', adverb: 'adv', preposition: 'prep', pronoun: 'pron', determiner: 'det', conjunction: 'conj', number: 'num', 'modal auxiliary': 'modal', interjection: 'interj', 'be-verb': 'v', 'do-verb': 'v', 'have-verb': 'v', 'infinitive-to': 'prep' };
const NAWL_POS = { n: 'n', v: 'v', adj: 'adj', adv: 'adv', prep: 'prep', conj: 'conj', pron: 'pron' };

// ---- candidate universe -------------------------------------------------------------------
const words = new Map(); // headword -> record
function rec(w) {
  if (!words.has(w)) words.set(w, { headword: w, forms: new Set(), pos: new Set(), sources: {}, topics: new Set() });
  return words.get(w);
}
for (const [w, s] of NG) { const r = rec(w); r.sources.ngsl = s.rank; }
for (const [w, f] of NGF) { const r = rec(w); for (const x of f) r.forms.add(x); }
for (const [w, f] of SUP) { const r = rec(w); r.sources.sup = true; for (const x of f) r.forms.add(x); }
for (const [w, e] of MO) { const r = rec(w); r.sources.moe = e.star === 1 ? 'E' : e.star === 2 ? 'P' : 'S'; for (const v of e.variants) r.forms.add(v); }
for (const [w, e] of CJ) { const r = rec(w); r.sources.cefrj = e.level; r.cefrjLevels = e.levels; for (const p of e.pos) r.pos.add(POS_MAP[p] || p); for (const t of e.topics) r.topics.add(t); for (const v of e.variants) r.forms.add(v); }
for (const [w, f] of NAF) { const r = rec(w); r.sources.nawl = true; for (const x of f) r.forms.add(x); const d = NAD.get(w); if (d && NAWL_POS[d.pos]) r.pos.add(NAWL_POS[d.pos]); }
for (const [w, s] of BS) { const r = rec(w); r.sources.bsl = s.rank; r.bslSfi = s.sfi; }
for (const [w, f] of BSF) { const r = rec(w); for (const x of f) r.forms.add(x); }
for (const w of [...preSeed, ...coreSeed]) rec(w);

// ---- filters ------------------------------------------------------------------------------
const dropped = { functionWord: [], abbreviation: [], properNoun: [], notAWord: [] };
for (const [w, r] of words) {
  const isCoreSeed = coreSeed.has(w);
  if (functionWords.has(w) && !isCoreSeed) { dropped.functionWord.push(w); words.delete(w); continue; }
  if (/\./.test(w) && !r.sources.moe) { dropped.abbreviation.push(w); words.delete(w); continue; }
  if (!/^[a-z][a-z'\-]*( [a-z][a-z'\-]*)?$/.test(w)) { dropped.notAWord.push(w); words.delete(w); continue; }
  if (r.topics.has('Nationalities and countries') && !r.sources.moe && !r.sources.ngsl && !r.sources.nawl && !r.sources.bsl && !isCoreSeed && !preSeed.has(w)) { dropped.properNoun.push(w); words.delete(w); continue; }
}

// ---- helpers ------------------------------------------------------------------------------
const rank = (r) => r.sources.ngsl ?? (r.sources.bsl ? 3000 + r.sources.bsl : 99999);
const byRank = (a, b) => rank(words.get(a)) - rank(words.get(b)) || a.localeCompare(b);
const all = () => [...words.keys()];
const placed = new Map(); // headword -> level
const count = (lvl) => [...placed.values()].filter((l) => l === lvl).length;
function fill(level, tiers) {
  // tiers: array of arrays (cut at TARGET) or {force: true, words} objects (always placed, may overflow TARGET)
  for (const t of tiers) {
    const force = !Array.isArray(t) && t.force;
    const list = Array.isArray(t) ? t : t.words;
    for (const w of t.ordered ? list : [...list].sort(byRank)) {
      if (!force && count(level) >= TARGET[level]) break;
      if (!words.has(w) || placed.has(w)) continue;
      placed.set(w, level);
    }
  }
}
const F = (words) => ({ force: true, words });
const CEFR_RANK = { A1: 1, A2: 2, B1: 3, B2: 4 };
const has = (w, k, v) => { const s = words.get(w)?.sources; return s && (v === undefined ? s[k] !== undefined : s[k] === v); };
const ngslLe = (w, n) => has(w, 'ngsl') && words.get(w).sources.ngsl <= n;

// ---- level assignment (tiers are priority order; each tier sorted by NGSL rank; F() = forced) ------
// Guarantees: every non-function NGSL word and every MOE E/S word is placed; MOE P goes to HIGH/ADV.
fill('PRE', [F([...preSeed].filter((w) => words.has(w))), all().filter((w) => has(w, 'moe', 'E') && has(w, 'cefrj', 'A1'))]);
fill('ELEM', [
  F(all().filter((w) => has(w, 'moe', 'E'))),
  all().filter((w) => has(w, 'cefrj', 'A1')),
  all().filter((w) => ngslLe(w, 800)),
]);
fill('MID', [
  F(all().filter((w) => ngslLe(w, 1500))),
  all().filter((w) => has(w, 'moe', 'S') && ['A1', 'A2', 'B1'].includes(words.get(w).sources.cefrj)),
  all().filter((w) => has(w, 'cefrj', 'A2')),
]);
fill('HIGH', [
  F(all().filter((w) => has(w, 'ngsl'))),
  F(all().filter((w) => has(w, 'moe', 'S'))),
  all().filter((w) => has(w, 'moe', 'P') && has(w, 'cefrj')),
  all().filter((w) => has(w, 'cefrj', 'B1') && (has(w, 'nawl') || has(w, 'bsl'))),
]);
// ADV: remaining MOE P, then academic (NAWL) and business (BSL) interleaved 1:1, then CEFR-J B2 leftovers
const nawlLeft = all().filter((w) => has(w, 'nawl') && !placed.has(w)).sort((a, b) => (CEFR_RANK[words.get(a).sources.cefrj] ?? 9) - (CEFR_RANK[words.get(b).sources.cefrj] ?? 9) || a.localeCompare(b));
const bslLeft = all().filter((w) => has(w, 'bsl') && !placed.has(w) && !has(w, 'nawl')).sort(byRank);
const interleaved = []; for (let i = 0; i < Math.max(nawlLeft.length, bslLeft.length); i++) { if (nawlLeft[i]) interleaved.push(nawlLeft[i]); if (bslLeft[i]) interleaved.push(bslLeft[i]); }
fill('ADV', [F(all().filter((w) => has(w, 'moe', 'P'))), { force: false, ordered: true, words: interleaved }, all().filter((w) => has(w, 'cefrj', 'B2'))]);
// Core seed words that did not make any cut get their natural level (small overflow is acceptable).
function naturalLevel(w) {
  const s = words.get(w).sources;
  if (preSeed.has(w)) return 'PRE';
  if (s.moe === 'E') return 'ELEM'; if (s.moe === 'S') return 'MID'; if (s.moe === 'P') return 'HIGH';
  if (s.cefrj === 'A1') return 'ELEM'; if (s.cefrj === 'A2') return 'MID'; if (s.cefrj === 'B1' || s.cefrj === 'B2') return 'HIGH';
  if (s.ngsl <= 800) return 'ELEM'; if (s.ngsl <= 1800) return 'MID'; if (s.ngsl) return 'HIGH';
  return 'ADV';
}
for (const w of coreSeed) if (words.has(w) && !placed.has(w)) placed.set(w, naturalLevel(w));

// ---- core track -----------------------------------------------------------------------------
const core = new Set([...coreSeed].filter((w) => words.has(w)));
const POLY = ['n', 'v', 'adj', 'adv'];
const polyCandidates = [...placed.keys()].filter((w) => !core.has(w) && POLY.filter((p) => words.get(w).pos.has(p)).length >= 2 && has(w, 'ngsl')).sort(byRank);
for (const w of polyCandidates) { if (core.size >= CORE_TARGET) break; core.add(w); }

// ---- output ---------------------------------------------------------------------------------
const slug = (w) => w.replace(/'/g, '').replace(/[^a-z0-9-]+/g, '-').replace(/^-|-$/g, '');
const rows = [...placed.entries()].map(([w, level]) => {
  const r = words.get(w);
  const forms = [...r.forms].filter((f) => f !== w).sort();
  return { id: slug(w), headword: w, level, isCore: core.has(w) ? 1 : 0, pos_hint: [...r.pos].sort().join('|'), forms: forms.join('|'), freqRank: r.sources.ngsl ?? '', cefr: r.sources.cefrj ?? '', moe: r.sources.moe ?? '', sources: JSON.stringify(r.sources) };
});
const levelOrder = Object.fromEntries(LEVELS.map((l, i) => [l, i]));
rows.sort((a, b) => levelOrder[a.level] - levelOrder[b.level] || (b.isCore - a.isCore) || (a.freqRank || 99999) - (b.freqRank || 99999) || a.headword.localeCompare(b.headword));
const ids = new Set(); for (const r of rows) { if (ids.has(r.id)) throw new Error('duplicate id ' + r.id); ids.add(r.id); }
const header = ['id', 'headword', 'level', 'isCore', 'pos_hint', 'forms', 'freqRank', 'cefr', 'moe', 'sources'];
writeFileSync(OUT + 'master.tsv', header.join('\t') + '\n' + rows.map((r) => header.map((h) => r[h]).join('\t')).join('\n') + '\n');

// ---- report ---------------------------------------------------------------------------------
const lines = [];
lines.push('# master.tsv 생성 리포트', '', `총 ${rows.length}개 (목표 ${Object.values(TARGET).reduce((a, b) => a + b)}), Core ${core.size}개 (목표 ${CORE_TARGET})`, '');
lines.push('| 레벨 | 개수 | 목표 | Core | MOE E/S/P | CEFR A1/A2/B1/B2/- | NGSL 있음 | pos 없음 |', '|---|---|---|---|---|---|---|---|');
for (const L of LEVELS) {
  const rs = rows.filter((r) => r.level === L);
  const m = (k) => rs.filter((r) => r.moe === k).length;
  const c = (k) => rs.filter((r) => r.cefr === k).length;
  lines.push(`| ${L} | ${rs.length} | ${TARGET[L]} | ${rs.filter((r) => r.isCore).length} | ${m('E')}/${m('S')}/${m('P')} | ${c('A1')}/${c('A2')}/${c('B1')}/${c('B2')}/${rs.filter((r) => !r.cefr).length} | ${rs.filter((r) => r.freqRank !== '').length} | ${rs.filter((r) => !r.pos_hint).length} |`);
}
lines.push('', '## 레벨별 경계 표본 (빈도순 처음 15 / 마지막 15)');
for (const L of LEVELS) {
  const rs = rows.filter((r) => r.level === L && !r.isCore);
  lines.push(`- **${L} 앞**: ${rs.slice(0, 15).map((r) => r.headword).join(', ')}`);
  lines.push(`- **${L} 뒤**: ${rs.slice(-15).map((r) => r.headword).join(', ')}`);
}
lines.push('', '## Core 표본', `- 시드 외 자동 선정(빈도순 앞 40): ${[...core].filter((w) => !coreSeed.has(w)).sort(byRank).slice(0, 40).join(', ')}`, `- 자동 선정 마지막 20: ${[...core].filter((w) => !coreSeed.has(w)).sort(byRank).slice(-20).join(', ')}`);
lines.push('', '## 제외/미배치');
for (const [k, v] of Object.entries(dropped)) lines.push(`- ${k}: ${v.length} (${v.slice(0, 25).join(', ')}${v.length > 25 ? ', …' : ''})`);
const unplaced = all().filter((w) => !placed.has(w));
const up = (pred) => unplaced.filter(pred);
lines.push(`- 미배치 후보 총 ${unplaced.length}: MOE E ${up((w) => has(w, 'moe', 'E')).length}, MOE S ${up((w) => has(w, 'moe', 'S')).length}, MOE P ${up((w) => has(w, 'moe', 'P')).length}, NGSL ${up((w) => has(w, 'ngsl')).length}, NAWL ${up((w) => has(w, 'nawl')).length}, BSL ${up((w) => has(w, 'bsl')).length}, CEFR-J only ${up((w) => has(w, 'cefrj') && !has(w, 'moe') && !has(w, 'ngsl') && !has(w, 'nawl') && !has(w, 'bsl')).length}`);
lines.push(`- 미배치 MOE S/P 표본: ${up((w) => has(w, 'moe', 'S') || has(w, 'moe', 'P')).sort(byRank).slice(0, 40).join(', ')}`);
lines.push(`- 미배치 NGSL 표본: ${up((w) => has(w, 'ngsl')).sort(byRank).slice(0, 40).join(', ')}`);
writeFileSync(OUT + 'report.md', lines.join('\n') + '\n');
console.log(lines.slice(0, 10).join('\n'));
console.log(`\nwrote ${OUT}master.tsv (${rows.length} rows) and report.md`);
