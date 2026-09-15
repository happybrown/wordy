#!/usr/bin/env node
// Merges batch outputs (+ .fix.json overrides) into content/entries/{pre,elem,mid,high,adv}.json.
//   node tools/merge.mjs [--partial]   (--partial: allow missing batches, e.g. during seed stage)
import { readFileSync, writeFileSync, existsSync, mkdirSync } from 'node:fs';
import { loadMaster, CONTENT, LEVELS, LEVEL_FILE } from './lib/master.mjs';

const partial = process.argv.includes('--partial');
const master = loadMaster();
const byId = new Map(master.map((r) => [r.id, r]));
const index = JSON.parse(readFileSync(`${CONTENT}batches/index.json`, 'utf8'));
const merged = new Map();
let batchesRead = 0, missingBatches = [];
for (const b of index.batches) {
  const dir = `${CONTENT}batches/${b.kind}/`;
  const out = `${dir}${b.name}.json`;
  if (!existsSync(out)) { missingBatches.push(b.name); continue; }
  batchesRead++;
  const entries = JSON.parse(readFileSync(out, 'utf8'));
  const fixPath = `${dir}${b.name}.fix.json`;
  const fixes = existsSync(fixPath) ? new Map(JSON.parse(readFileSync(fixPath, 'utf8')).map((e) => [e.id, e])) : new Map();
  for (const e of entries) {
    const final = fixes.get(e.id) ?? e;
    if (merged.has(final.id)) throw new Error(`duplicate entry across batches: ${final.id} (${b.name})`);
    if (!byId.has(final.id)) throw new Error(`entry not in master: ${final.id} (${b.name})`);
    merged.set(final.id, final);
  }
  for (const [id, e] of fixes) if (!merged.has(id)) merged.set(id, e);
}
const missing = master.filter((r) => !merged.has(r.id));
if (missing.length && !partial) { console.error(`missing ${missing.length} entries (use --partial to write anyway); missing batches: ${missingBatches.join(', ')}`); process.exit(1); }
mkdirSync(`${CONTENT}entries`, { recursive: true });
const rank = (id) => byId.get(id).freqRank ?? 99999;
for (const L of LEVELS) {
  const list = [...merged.values()].filter((e) => e.level === L).sort((a, b) => (b.isCore - a.isCore) || rank(a.id) - rank(b.id) || a.id.localeCompare(b.id));
  writeFileSync(`${CONTENT}entries/${LEVEL_FILE[L]}.json`, JSON.stringify(list) + '\n');
  console.log(`${L}: ${list.length}/${master.filter((r) => r.level === L).length} -> entries/${LEVEL_FILE[L]}.json`);
}
console.log(`batches read: ${batchesRead}/${index.batches.length}; entries: ${merged.size}/${master.length}${missing.length ? ` (missing ${missing.length})` : ''}`);
