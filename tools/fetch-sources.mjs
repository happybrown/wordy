#!/usr/bin/env node
// Re-downloads the raw word-list sources into content/sources (see content/sources/README.md for licenses).
// Requires: network, `pdftotext` (poppler) on PATH for the MOE curriculum PDF.
import { writeFileSync, existsSync, mkdirSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { inflateRawSync } from 'node:zlib';
import { SRC } from './lib/sources.mjs';

const FILES = {
  'NGSL_12_stats.csv': 'https://www.newgeneralservicelist.com/s/NGSL_12_stats.csv',
  'NGSL_12_lemmatized_for_teaching.csv': 'https://www.newgeneralservicelist.com/s/NGSL_12_lemmatized_for_teaching.csv',
  'SUP_lemmatized.csv': 'https://www.newgeneralservicelist.com/s/SUP_lemmatized.csv',
  'NAWL_12_lemmatized_for_teaching.csv': 'https://www.newgeneralservicelist.com/s/NAWL_12_lemmatized_for_teaching.csv',
  'NAWL_12_with_en_definitions.csv': 'https://www.newgeneralservicelist.com/s/NAWL_12_with_en_definitions.csv',
  'BSL_120_stats.csv': 'https://www.newgeneralservicelist.com/s/BSL_120_stats.csv',
  'BSL_120_lemmatized_for_teaching.csv': 'https://www.newgeneralservicelist.com/s/BSL_120_lemmatized_for_teaching.csv',
  'CEFRJ_wordlist_ver1.6.zip': 'https://www.cefr-j.org/data/CEFRJ_wordlist_ver1.6.zip',
  // 교육부 고시 제2015-74호 [별책 14] 영어과 교육과정 — publisher-hosted copy of the government notice (public domain under 저작권법 §7)
  'MOE_2015-74_appendix14_english.pdf': 'https://textbook.tsherpa.co.kr/data/%EA%B5%90%EC%9C%A1%EB%B6%80%20%EA%B3%A0%EC%8B%9C%20%EC%A0%9C2015-74%ED%98%B8%20%5B%EB%B3%84%EC%B1%8514%5D%20%5B%EA%B5%90%EC%9C%A1%EB%B6%80%5D%20%EC%98%81%EC%96%B4%EA%B3%BC_%EA%B5%90%EC%9C%A1%EA%B3%BC%EC%A0%95.pdf',
};
mkdirSync(SRC, { recursive: true });
const force = process.argv.includes('--force');
for (const [name, url] of Object.entries(FILES)) {
  const dest = SRC + name;
  if (existsSync(dest) && !force) { console.log(`skip  ${name} (exists)`); continue; }
  const res = await fetch(url, { headers: { 'user-agent': 'Mozilla/5.0 (wordy content pipeline)' } });
  if (!res.ok) throw new Error(`${name}: HTTP ${res.status}`);
  writeFileSync(dest, Buffer.from(await res.arrayBuffer()));
  console.log(`fetch ${name}`);
}
// unzip CEFR-J (single xlsx inside) without external tools
const xlsx = SRC + 'CEFRJ_wordlist_ver1.6.xlsx';
if (!existsSync(xlsx) || force) {
  const { readFileSync } = await import('node:fs');
  const buf = readFileSync(SRC + 'CEFRJ_wordlist_ver1.6.zip');
  let eocd = -1; for (let i = buf.length - 22; i >= 0; i--) if (buf.readUInt32LE(i) === 0x06054b50) { eocd = i; break; }
  let off = buf.readUInt32LE(eocd + 16);
  const method = buf.readUInt16LE(off + 10), csize = buf.readUInt32LE(off + 20), nameLen = buf.readUInt16LE(off + 28), localOff = buf.readUInt32LE(off + 42);
  const lnl = buf.readUInt16LE(localOff + 26), lel = buf.readUInt16LE(localOff + 28);
  const data = buf.subarray(localOff + 30 + lnl + lel, localOff + 30 + lnl + lel + csize);
  writeFileSync(xlsx, method === 8 ? inflateRawSync(data) : data);
  console.log('unzip CEFRJ_wordlist_ver1.6.xlsx');
}
// pdftotext -layout for the MOE list (multi-column layout is parsed by lib/sources.mjs)
const txt = SRC + 'MOE_2015-74_appendix14_english.txt';
if (!existsSync(txt) || force) {
  execFileSync('pdftotext', ['-layout', SRC + 'MOE_2015-74_appendix14_english.pdf', txt]);
  console.log('pdftotext MOE_2015-74_appendix14_english.txt');
}
console.log('done');
