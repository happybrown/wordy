#!/usr/bin/env node
// Distribution report over written batches (or merged entries): sense counts, example lengths, topics, pos, warnings hot-spots.
//   node tools/stats.mjs            (all batch outputs found)
import { readFileSync, existsSync } from 'node:fs';
import { loadMaster, loadTopics, CONTENT, LEVELS, wordCount } from './lib/master.mjs';

const master = loadMaster();
const index = JSON.parse(readFileSync(`${CONTENT}batches/index.json`, 'utf8'));
const entries = [];
let written = 0;
for (const b of index.batches) {
  const p = `${CONTENT}batches/${b.kind}/${b.name}.json`;
  if (!existsSync(p)) continue;
  written++;
  entries.push(...JSON.parse(readFileSync(p, 'utf8')));
}
const pct = (n, d) => (d ? `${Math.round((n / d) * 100)}%` : '-');
console.log(`batches written: ${written}/${index.batches.length}; entries: ${entries.length}/${master.length} (${pct(entries.length, master.length)})`);
console.log('\nby level:');
for (const L of LEVELS) { const n = entries.filter((e) => e.level === L).length, t = master.filter((r) => r.level === L).length; console.log(`  ${L.padEnd(5)} ${String(n).padStart(5)}/${t}  ${pct(n, t)}`); }
const core = entries.filter((e) => e.isCore), light = entries.filter((e) => !e.isCore);
const dist = (arr) => { const m = {}; for (const x of arr) m[x] = (m[x] || 0) + 1; return Object.entries(m).sort((a, b) => +a[0] - +b[0]).map(([k, v]) => `${k}:${v}`).join(' '); };
console.log(`\nCore ${core.length}: senses ${dist(core.map((e) => e.senses.length))}`);
console.log(`Light ${light.length}: senses ${dist(light.map((e) => e.senses.length))}`);
const examples = entries.flatMap((e) => e.senses.flatMap((s) => s.examples.map((x) => x.en)));
const lens = examples.map(wordCount);
console.log(`\nexamples: ${examples.length}, words/example avg ${(lens.reduce((a, b) => a + b, 0) / lens.length).toFixed(1)}, dist ${dist(lens)}`);
const starts = {}; for (const ex of examples) { const w = ex.split(' ')[0]; starts[w] = (starts[w] || 0) + 1; }
console.log('most common first words:', Object.entries(starts).sort((a, b) => b[1] - a[1]).slice(0, 10).map(([w, n]) => `${w}(${n})`).join(' '));
const names = ['Mina', 'Jun', 'Emma', 'Tom'];
console.log('character mentions:', names.map((n) => `${n}:${examples.filter((e) => e.includes(n)).length}`).join(' '));
const topics = loadTopics();
console.log('\ntopics:'); for (const t of topics) { const n = entries.filter((e) => e.topic === t.key).length; if (n) console.log(`  ${t.key.padEnd(22)} ${String(n).padStart(4)}  ${t.ko}`); }
const pos = {}; for (const e of entries) for (const s of e.senses) pos[s.pos] = (pos[s.pos] || 0) + 1;
console.log('\npos (senses):', Object.entries(pos).sort((a, b) => b[1] - a[1]).map(([k, v]) => `${k}:${v}`).join(' '));
console.log(`with ipa: ${pct(entries.filter((e) => e.ipa).length, entries.length)}, with nuance (light): ${pct(light.filter((e) => e.senses.some((s) => s.nuance)).length, light.length)}, with confusables: ${pct(entries.filter((e) => e.confusables?.length).length, entries.length)}, with wordFamily: ${pct(entries.filter((e) => e.wordFamily?.length).length, entries.length)}`);
const dupEx = new Map(); for (const ex of examples) { const k = ex.toLowerCase(); dupEx.set(k, (dupEx.get(k) || 0) + 1); }
const dups = [...dupEx.entries()].filter(([, n]) => n > 1);
console.log(`duplicate example sentences across entries: ${dups.length}${dups.length ? ' e.g. ' + dups.slice(0, 3).map(([k]) => `"${k}"`).join(', ') : ''}`);
