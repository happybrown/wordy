#!/usr/bin/env node
// Validates content entries against the JSON Schema and the semantic rules in content/GUIDE.md.
//   node tools/validate.mjs --batch d001 [--repair]   validate one batch output (+ .fix.json overrides)
//   node tools/validate.mjs --file some.json          validate an arbitrary entries array
//   node tools/validate.mjs --all                     validate merged content/entries/*.json + global rules
// Exit code 1 when any error exists. --json prints machine-readable output.
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { validator } from '@exodus/schemasafe';
import { loadMaster, loadTopics, CONTENT, LEVELS, LEVEL_FILE, containsForm, hasHangul, wordCount, escapeRe } from './lib/master.mjs';
import { cleanForms } from './lib/forms.mjs';

const args = process.argv.slice(2);
const opt = (k) => { const i = args.indexOf(k); return i >= 0 ? args[i + 1] : null; };
const flag = (k) => args.includes(k);

const schema = JSON.parse(readFileSync(CONTENT + 'schema/entry.schema.json', 'utf8'));
const validateSchema = validator(schema, { includeErrors: true, allErrors: true, mode: 'default', allowUnusedKeywords: true, requireValidation: false, requireStringValidation: false });
const master = loadMaster();
const byId = new Map(master.map((r) => [r.id, r]));
const topicKeys = new Set(loadTopics().map((t) => t.key));
const POS = new Set(schema.$defs.pos.enum);
const BANNED_STARTS = [/^this is an? /i, /^i like /i, /^there (is|are) /i];
const PLACEHOLDER = /(\.\.\.|…|TODO|lorem|예문|xxx)/i;

const problems = []; // {id, level:'error'|'warn', rule, msg}
const err = (id, rule, msg) => problems.push({ id, level: 'error', rule, msg });
const warn = (id, rule, msg) => problems.push({ id, level: 'warn', rule, msg });

function checkExample(id, ex, forms, where) {
  if (!containsForm(ex.en, forms)) err(id, 'example.headword', `${where}: 예문에 headword/forms 없음: "${ex.en}"`);
  if (!ex.en.includes(ex.focus)) err(id, 'example.focus', `${where}: focus가 예문의 부분문자열이 아님: "${ex.focus}" ⊄ "${ex.en}"`);
  else if (!containsForm(ex.focus, forms)) err(id, 'example.focus', `${where}: focus에 headword 형태 없음: "${ex.focus}"`);
  if (ex.focus.length >= ex.en.length) err(id, 'example.focus', `${where}: focus가 예문 전체와 같음`);
  const wc = wordCount(ex.en);
  if (wc < 5 || wc > 18) err(id, 'example.length', `${where}: 예문 ${wc}단어 (허용 5~18, 목표 8~14): "${ex.en}"`);
  else if (wc < 7 || wc > 15) warn(id, 'example.length', `${where}: 예문 ${wc}단어 (목표 8~14)`);
  if (!/^["'“‘(]?[A-Z]/.test(ex.en)) err(id, 'example.format', `${where}: 대문자로 시작하지 않음: "${ex.en}"`);
  if (!/[.!?]["'”’)]?$/.test(ex.en)) err(id, 'example.format', `${where}: 문장부호로 끝나지 않음: "${ex.en}"`);
  if (/\s{2,}/.test(ex.en)) err(id, 'example.format', `${where}: 이중 공백`);
  if (hasHangul(ex.en) || hasHangul(ex.focus)) err(id, 'lang', `${where}: 영어 필드에 한글`);
  if (!hasHangul(ex.ko)) err(id, 'lang', `${where}: 한국어 번역에 한글 없음: "${ex.ko}"`);
  if (PLACEHOLDER.test(ex.en) || PLACEHOLDER.test(ex.ko)) err(id, 'placeholder', `${where}: 자리표시/말줄임 텍스트`);
}

export function validateEntries(entries, { manifest = null, globalScope = false } = {}) {
  const seenIds = new Set();
  const globalExamples = new Map(); // en -> id
  for (const e of entries) {
    const id = e?.id ?? '(no id)';
    if (!validateSchema(e)) { for (const x of validateSchema.errors || []) err(id, 'schema', `${x.instanceLocation} ${x.keywordLocation}`); }
    if (typeof e !== 'object' || !e.id) continue;
    if (seenIds.has(id)) err(id, 'id.unique', '같은 배치/파일 안에 id 중복'); seenIds.add(id);
    const row = byId.get(id);
    if (!row) { err(id, 'id.master', 'master.tsv에 없는 id'); continue; }
    if (e.headword !== row.headword) err(id, 'master.headword', `headword 불일치: "${e.headword}" ≠ "${row.headword}"`);
    if (e.level !== row.level) err(id, 'master.level', `level 불일치: ${e.level} ≠ ${row.level}`);
    if (e.isCore !== row.isCore) err(id, 'master.isCore', `isCore 불일치: ${e.isCore} ≠ ${row.isCore}`);
    if (!topicKeys.has(e.topic)) err(id, 'topic', `알 수 없는 topic: ${e.topic}`);
    const forms = [e.headword, ...(e.forms || [])];
    const missingForms = cleanForms(row.headword, row.forms, row.pos_hint).filter((f) => !(e.forms || []).includes(f));
    if (missingForms.length) warn(id, 'forms.master', `manifest forms 누락: ${missingForms.join(', ')}`);
    if ((e.forms || []).includes(e.headword)) warn(id, 'forms.self', 'forms에 headword 자체가 포함됨');
    if (!Array.isArray(e.senses)) continue;

    // --- senses
    const glosses = new Set(); const exampleSet = new Set(); let collocTotal = 0; let hasVerb = false; let bannedStarts = 0;
    e.senses.forEach((s, i) => {
      const where = `sense ${i + 1}`;
      if (s.id !== `${id}-${i + 1}`) err(id, 'sense.id', `${where}: id는 "${id}-${i + 1}" 이어야 함 (현재 "${s.id}")`);
      if (!POS.has(s.pos)) err(id, 'sense.pos', `${where}: pos "${s.pos}"`);
      if (s.pos === 'v') hasVerb = true;
      if (!hasHangul(s.ko)) err(id, 'lang', `${where}: ko에 한글 없음`);
      if (glosses.has(s.ko)) err(id, 'sense.ko.unique', `${where}: 뜻 중복 "${s.ko}"`); glosses.add(s.ko);
      if (hasHangul(s.en)) err(id, 'lang', `${where}: en 정의에 한글`);
      if (containsForm(s.en, forms)) err(id, 'sense.en.circular', `${where}: 영어 정의에 headword 사용: "${s.en}"`);
      if (wordCount(s.en) > 18) warn(id, 'sense.en.length', `${where}: 영어 정의 ${wordCount(s.en)}단어 (목표 ≤15)`);
      if (s.nuance !== undefined && !hasHangul(s.nuance)) err(id, 'lang', `${where}: nuance에 한글 없음`);
      if (e.isCore && !s.nuance) err(id, 'core.nuance', `${where}: Core는 nuance 필수`);
      if (e.isCore && (s.examples?.length ?? 0) < 2) err(id, 'core.examples', `${where}: Core는 sense당 예문 2~3개 (현재 ${s.examples?.length ?? 0})`);
      if (!e.isCore && (s.examples?.length ?? 0) > 2) err(id, 'light.examples', `${where}: 일반 항목은 sense당 예문 최대 2개`);
      for (const [j, ex] of (s.examples || []).entries()) {
        checkExample(id, ex, forms, `${where} 예문 ${j + 1}`);
        const key = ex.en.toLowerCase().replace(/[^a-z ]/g, '');
        if (exampleSet.has(key)) err(id, 'example.dup', `${where}: 단어 내 예문 중복 "${ex.en}"`); exampleSet.add(key);
        if (globalScope) { if (globalExamples.has(key) && globalExamples.get(key) !== id) warn(id, 'example.dup.global', `예문이 ${globalExamples.get(key)}와 동일`); else globalExamples.set(key, id); }
        if (BANNED_STARTS.some((re) => re.test(ex.en))) bannedStarts++;
      }
      for (const c of s.collocations || []) {
        collocTotal++;
        if (!containsForm(c, forms)) err(id, 'collocation.headword', `${where}: 연어에 headword 없음: "${c}"`);
        if (/[.!?]$/.test(c) || wordCount(c) > 6) warn(id, 'collocation.format', `${where}: 연어는 짧은 구여야 함: "${c}"`);
        if (hasHangul(c)) err(id, 'lang', `${where}: 연어에 한글`);
      }
      for (const [j, p] of (s.phrasals || []).entries()) {
        if (!new RegExp(`^(${forms.map(escapeRe).join('|')})\\b`, 'i').test(p.phrase)) err(id, 'phrasal.headword', `${where}: 구동사가 headword로 시작하지 않음: "${p.phrase}"`);
        if (!hasHangul(p.ko)) err(id, 'lang', `${where}: 구동사 ko에 한글 없음`);
        checkExample(id, p.example, forms, `${where} 구동사 ${j + 1}`);
        if (!containsForm(p.example.en, [p.phrase.split(' ')[0]])) err(id, 'phrasal.example', `${where}: 구동사 예문에 동사 없음`);
      }
    });
    if (e.senses.some((s) => s.phrasals?.length) && !hasVerb) err(id, 'phrasal.pos', '구동사는 동사 sense가 있을 때만');
    if (e.isCore && (collocTotal < 3 || collocTotal > 8)) err(id, 'core.collocations', `Core 연어 총 ${collocTotal}개 (3~8 필요)`);
    if (!e.isCore && collocTotal > 3) warn(id, 'light.collocations', `일반 항목 연어 ${collocTotal}개 (목표 ≤3)`);
    if (bannedStarts > 1) warn(id, 'example.banned', `밋밋한 시작(This is a…/I like…/There is…) 예문 ${bannedStarts}개`);
    // --- relations
    for (const c of e.confusables || []) {
      if (!hasHangul(c.note)) err(id, 'lang', `confusable note에 한글 없음: ${c.word}`);
      if (c.word.toLowerCase() === e.headword) err(id, 'confusable.self', '자기 자신을 confusable로 지정');
      if (![...byId.values()].some((r) => r.headword === c.word.toLowerCase())) warn(id, 'confusable.unknown', `confusable "${c.word}"가 단어 목록에 없음`);
    }
    for (const f of e.wordFamily || []) {
      if (!hasHangul(f.ko)) err(id, 'lang', `wordFamily ko에 한글 없음: ${f.form}`);
      if (f.form.toLowerCase() === e.headword) err(id, 'family.self', 'wordFamily에 headword 자체');
    }
    if (e.memoryHook && !hasHangul(e.memoryHook)) warn(id, 'lang', 'memoryHook에 한글 없음');
  }
  // --- coverage against manifest
  if (manifest) {
    const want = new Set(manifest.entries.map((m) => m.id));
    for (const id of want) if (!seenIds.has(id)) err(id, 'coverage.missing', `배치 ${manifest.batch}에 있어야 하는 항목이 없음`);
    for (const id of seenIds) if (!want.has(id)) err(id, 'coverage.extra', `배치 ${manifest.batch}에 없는 항목`);
  }
  return problems;
}

function loadBatch(name) {
  const kind = name.startsWith('d') ? 'deep' : 'light';
  const dir = `${CONTENT}batches/${kind}/`;
  const manifest = JSON.parse(readFileSync(`${dir}${name}.manifest.json`, 'utf8'));
  const outPath = `${dir}${name}.json`;
  if (!existsSync(outPath)) { console.error(`no output yet: ${outPath}`); process.exit(2); }
  let entries = JSON.parse(readFileSync(outPath, 'utf8'));
  if (!Array.isArray(entries)) { console.error('batch output must be a JSON array'); process.exit(2); }
  const fixPath = `${dir}${name}.fix.json`;
  if (existsSync(fixPath)) {
    const fixes = new Map(JSON.parse(readFileSync(fixPath, 'utf8')).map((e) => [e.id, e]));
    entries = entries.map((e) => fixes.get(e.id) ?? e);
    for (const [id, e] of fixes) if (!entries.some((x) => x.id === id)) entries.push(e);
  }
  return { manifest, entries, dir };
}

function report(label) {
  const errors = problems.filter((p) => p.level === 'error'), warns = problems.filter((p) => p.level === 'warn');
  if (flag('--json')) { console.log(JSON.stringify({ label, errors, warns }, null, 2)); }
  else {
    for (const p of problems) console.log(`${p.level === 'error' ? 'ERROR' : 'warn '} ${p.id} [${p.rule}] ${p.msg}`);
    console.log(`\n${label}: ${errors.length} error(s), ${warns.length} warning(s), ${new Set(errors.map((e) => e.id)).size} failing entr(y/ies)`);
  }
  return errors.length ? 1 : 0;
}

const batch = opt('--batch');
if (batch) {
  const { manifest, entries, dir } = loadBatch(batch);
  validateEntries(entries, { manifest });
  if (flag('--repair')) {
    const failing = new Set(problems.filter((p) => p.level === 'error').map((p) => p.id));
    const repair = { batch, kind: manifest.kind, purpose: 'regenerate ONLY these entries; output to ' + batch + '.fix.json', entries: manifest.entries.filter((m) => failing.has(m.id)).map((m) => ({ ...m, problems: problems.filter((p) => p.id === m.id && p.level === 'error').map((p) => p.msg) })) };
    writeFileSync(`${dir}${batch}.repair.json`, JSON.stringify(repair, null, 2) + '\n');
    console.log(`repair manifest: ${dir}${batch}.repair.json (${repair.entries.length} entries)`);
  }
  process.exit(report(`batch ${batch} (${entries.length} entries)`));
} else if (opt('--file')) {
  const entries = JSON.parse(readFileSync(opt('--file'), 'utf8'));
  validateEntries(Array.isArray(entries) ? entries : [entries]);
  process.exit(report(opt('--file')));
} else if (flag('--all')) {
  const all = [];
  for (const L of LEVELS) { const p = `${CONTENT}entries/${LEVEL_FILE[L]}.json`; if (existsSync(p)) all.push(...JSON.parse(readFileSync(p, 'utf8'))); }
  validateEntries(all, { globalScope: true });
  const ids = new Set(all.map((e) => e.id));
  const missing = master.filter((r) => !ids.has(r.id));
  if (missing.length) { if (flag('--partial')) warn('(global)', 'coverage', `master 대비 미작성 ${missing.length}개`); else err('(global)', 'coverage', `master 대비 미작성 ${missing.length}개: ${missing.slice(0, 10).map((r) => r.id).join(', ')}…`); }
  for (const L of LEVELS) { const n = all.filter((e) => e.level === L).length, t = master.filter((r) => r.level === L).length; if (!flag('--partial') && n !== t) err('(global)', 'coverage.level', `${L}: ${n}/${t}`); }
  const core = all.filter((e) => e.isCore).length; if (!flag('--partial') && core !== master.filter((r) => r.isCore).length) err('(global)', 'coverage.core', `Core ${core}`);
  process.exit(report(`all entries (${all.length}/${master.length})`));
} else {
  console.log('usage: validate.mjs --batch dNNN [--repair] | --file x.json | --all [--partial] [--json]');
  process.exit(2);
}
