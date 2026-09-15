// Shared helpers: master.tsv loading, topics, level order, text utilities.
import { readFileSync, existsSync } from 'node:fs';
export const ROOT = new URL('../../', import.meta.url).pathname;
export const CONTENT = ROOT + 'content/';
export const LEVELS = ['PRE', 'ELEM', 'MID', 'HIGH', 'ADV'];
export const LEVEL_FILE = { PRE: 'pre', ELEM: 'elem', MID: 'mid', HIGH: 'high', ADV: 'adv' };

export function loadMaster() {
  const [head, ...lines] = readFileSync(CONTENT + 'wordlist/master.tsv', 'utf8').trim().split('\n');
  const cols = head.split('\t');
  const rows = lines.map((l) => Object.fromEntries(l.split('\t').map((v, i) => [cols[i], v])));
  for (const r of rows) {
    r.isCore = r.isCore === '1';
    r.forms = r.forms ? r.forms.split('|') : [];
    r.pos_hint = r.pos_hint ? r.pos_hint.split('|') : [];
    r.freqRank = r.freqRank === '' ? null : +r.freqRank;
    r.sources = JSON.parse(r.sources);
  }
  return rows;
}
export const loadTopics = () => JSON.parse(readFileSync(CONTENT + 'topics.json', 'utf8')).topics;
export const readJson = (p) => JSON.parse(readFileSync(p, 'utf8'));
export const exists = existsSync;

/** true if `text` contains any of `forms` as a whole word (case-insensitive; allows possessive 's) */
export function containsForm(text, forms) {
  return forms.some((f) => f && new RegExp(`(^|[^A-Za-z])${escapeRe(f)}(?:'s)?(?=$|[^A-Za-z])`, 'i').test(text));
}
export const escapeRe = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
export const hasHangul = (s) => /[가-힣]/.test(s || '');
export const wordCount = (s) => s.trim().split(/\s+/).filter(Boolean).length;
