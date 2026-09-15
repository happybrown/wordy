#!/usr/bin/env node
// Builds the prepackaged content database from content/entries/*.json using the Room-exported schema,
// so the file passes Room's schema/identity checks without any runtime import.
//   node tools/build-db.mjs [--include-golden]
// Output: app/src/main/assets/database/content.db + content.version
import { readFileSync, writeFileSync, existsSync, mkdirSync, rmSync, readdirSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { DatabaseSync } from 'node:sqlite';
import { loadMaster, loadTopics, CONTENT, ROOT, LEVELS, LEVEL_FILE } from './lib/master.mjs';
import { cleanForms } from './lib/forms.mjs';

const includeGolden = process.argv.includes('--include-golden');
const SCHEMA_DIR = ROOT + 'app/schemas/dev.brown.wordy.data.db.content.ContentDatabase/';
const ASSET_DIR = ROOT + 'app/src/main/assets/database/';
const UNIT_MAX = 25;
const CAPITALIZED = new Set(['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday', 'january', 'february', 'march', 'april', 'may', 'june', 'july', 'august', 'september', 'october', 'november', 'december']);
const display = (h) => (CAPITALIZED.has(h) ? h[0].toUpperCase() + h.slice(1) : h);

// --- schema (latest version file)
const schemaFile = readdirSync(SCHEMA_DIR).filter((f) => f.endsWith('.json')).sort((a, b) => parseInt(a) - parseInt(b)).at(-1);
const schema = JSON.parse(readFileSync(SCHEMA_DIR + schemaFile, 'utf8')).database;
console.log(`schema: ${schemaFile} (version ${schema.version}, identity ${schema.identityHash})`);

// --- entries
const master = loadMaster();
const byId = new Map(master.map((r) => [r.id, r]));
const entries = new Map();
for (const L of LEVELS) {
  const p = `${CONTENT}entries/${LEVEL_FILE[L]}.json`;
  if (existsSync(p)) for (const e of JSON.parse(readFileSync(p, 'utf8'))) entries.set(e.id, e);
}
if (includeGolden) for (const f of ['deep.json', 'light.json']) for (const e of JSON.parse(readFileSync(`${CONTENT}golden/${f}`, 'utf8'))) if (!entries.has(e.id)) entries.set(e.id, e);
if (!entries.size) { console.error('no entries found (run merge first, or pass --include-golden)'); process.exit(1); }
for (const e of entries.values()) if (!byId.has(e.id)) throw new Error(`entry not in master: ${e.id}`);
console.log(`entries: ${entries.size} (master ${master.length})`);

// --- units: per level, group by topic (topics.json order), chunk to UNIT_MAX, ordered by freqRank inside
const topics = loadTopics();
const topicOrder = Object.fromEntries(topics.map((t, i) => [t.key, i]));
const topicKo = Object.fromEntries(topics.map((t) => [t.key, t.ko]));
const rank = (e) => byId.get(e.id).freqRank ?? 99999;
const units = []; const unitOf = new Map(); // id -> {unitId, unitOrd}
for (const L of LEVELS) {
  const list = [...entries.values()].filter((e) => e.level === L);
  const groups = new Map();
  for (const e of list) { if (!groups.has(e.topic)) groups.set(e.topic, []); groups.get(e.topic).push(e); }
  let ord = 0;
  for (const topic of [...groups.keys()].sort((a, b) => topicOrder[a] - topicOrder[b])) {
    const words = groups.get(topic).sort((a, b) => rank(a) - rank(b) || a.id.localeCompare(b.id));
    const chunks = Math.ceil(words.length / UNIT_MAX);
    const size = Math.ceil(words.length / chunks);
    for (let c = 0; c < chunks; c++) {
      const slice = words.slice(c * size, (c + 1) * size);
      const id = `${L.toLowerCase()}-${topic}-${c + 1}`;
      units.push({ id, level: L, ord, title: chunks > 1 ? `${topicKo[topic]} ${c + 1}` : topicKo[topic], topic, wordCount: slice.length });
      slice.forEach((e, i) => unitOf.set(e.id, { unitId: id, unitOrd: ord * 1000 + i }));
      ord++;
    }
  }
}

// --- build sqlite
mkdirSync(ASSET_DIR, { recursive: true });
const dbPath = ASSET_DIR + 'content.db';
for (const s of ['', '-journal', '-wal', '-shm']) rmSync(dbPath + s, { force: true });
const db = new DatabaseSync(dbPath);
db.exec('PRAGMA journal_mode = DELETE;');
for (const t of schema.entities) {
  db.exec(t.createSql.replace('${TABLE_NAME}', t.tableName));
  for (const idx of t.indices || []) db.exec(idx.createSql.replace('${TABLE_NAME}', t.tableName));
}
db.exec('CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)');
db.prepare('INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)').run(schema.identityHash);
db.exec(`PRAGMA user_version = ${schema.version};`);

const insWord = db.prepare(`INSERT INTO word (id, headword, ipa, level, cefr, freqRank, isCore, topic, unitId, unitOrd, formsJson, familyJson, confusablesJson, memoryHook, searchText)
  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`);
const insSense = db.prepare(`INSERT INTO sense (id, wordId, ord, pos, ko, en, nuance, pattern, examplesJson, collocationsJson, phrasalsJson) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`);
const insUnit = db.prepare('INSERT INTO unit (id, level, ord, title, topic, wordCount) VALUES (?, ?, ?, ?, ?, ?)');
const insMeta = db.prepare('INSERT INTO content_meta (key, value) VALUES (?, ?)');
db.exec('BEGIN');
for (const u of units) insUnit.run(u.id, u.level, u.ord, u.title, u.topic, u.wordCount);
let senseCount = 0;
for (const e of [...entries.values()].sort((a, b) => a.id.localeCompare(b.id))) {
  const row = byId.get(e.id); const u = unitOf.get(e.id);
  const corpus = e.senses.flatMap((s) => [...s.examples.map((x) => x.en), ...(s.collocations || []), ...(s.phrasals || []).flatMap((p) => [p.phrase, p.example.en])]).join(' ');
  const forms = cleanForms(e.headword, [...(e.forms || []), ...row.forms], [...new Set(e.senses.map((s) => s.pos))], corpus);
  const searchText = [e.headword, ...forms, ...e.senses.map((s) => s.ko)].join(' ').toLowerCase();
  insWord.run(e.id, display(e.headword), e.ipa ?? null, e.level, row.cefr || null, row.freqRank, e.isCore ? 1 : 0, e.topic, u.unitId, u.unitOrd,
    JSON.stringify(forms), JSON.stringify(e.wordFamily || []), JSON.stringify(e.confusables || []), e.memoryHook ?? null, searchText);
  e.senses.forEach((s, i) => { senseCount++; insSense.run(s.id, e.id, i, s.pos, s.ko, s.en, s.nuance ?? null, s.pattern ?? null, JSON.stringify(s.examples), JSON.stringify(s.collocations || []), JSON.stringify(s.phrasals || [])); });
}
const hash = createHash('sha1').update(JSON.stringify([...entries.values()].sort((a, b) => a.id.localeCompare(b.id)))).digest('hex').slice(0, 10);
const version = `${schema.version}.${entries.size}.${hash}`;
const counts = Object.fromEntries(LEVELS.map((L) => [L, [...entries.values()].filter((e) => e.level === L).length]));
for (const [k, v] of Object.entries({ version, generatedAt: new Date().toISOString(), wordCount: String(entries.size), coreCount: String([...entries.values()].filter((e) => e.isCore).length), senseCount: String(senseCount), unitCount: String(units.length), levelCounts: JSON.stringify(counts), masterCount: String(master.length), includeGolden: String(includeGolden) })) insMeta.run(k, v);
db.exec('COMMIT');
db.exec('VACUUM');
db.close();
writeFileSync(ASSET_DIR + 'content.version', version + '\n');
const size = (readFileSync(dbPath).length / 1024).toFixed(0);
console.log(`wrote ${dbPath} (${size} KB): ${entries.size} words, ${senseCount} senses, ${units.length} units; version ${version}`);
