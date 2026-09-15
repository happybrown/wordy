#!/usr/bin/env node
// 배치 작성 진행 상황과 다음에 돌릴 배치를 보여준다.
//   node tools/batch-status.mjs            요약
//   node tools/batch-status.mjs --next 3   다음에 돌릴 배치 3개의 이름
import { readFileSync, existsSync } from 'node:fs';
import { CONTENT } from './lib/master.mjs';

const idx = JSON.parse(readFileSync(`${CONTENT}batches/index.json`, 'utf8'));
const done = [], todo = [];
for (const b of idx.batches) (existsSync(`${CONTENT}batches/${b.kind}/${b.name}.json`) ? done : todo).push(b);

const nextArg = process.argv.indexOf('--next');
if (nextArg >= 0) {
  const n = Number(process.argv[nextArg + 1] || 3);
  // Core(deep)를 먼저, 그 다음 레벨 순서대로
  const order = [...todo].sort((a, b) => (a.kind === b.kind ? a.name.localeCompare(b.name) : a.kind === 'deep' ? -1 : 1));
  for (const b of order.slice(0, n)) console.log(`${b.name}\t${b.kind}\t${b.levels.join('/')}\t${b.count}개`);
  process.exit(0);
}

const words = (list) => list.reduce((n, b) => n + b.count, 0);
console.log(`배치 ${done.length}/${idx.batches.length} 완료, 단어 ${words(done)}/${words(done) + words(todo)}개`);
for (const kind of ['deep', 'light']) {
  const d = done.filter((b) => b.kind === kind), t = todo.filter((b) => b.kind === kind);
  console.log(`  ${kind === 'deep' ? 'Core(deep)' : 'light     '} ${d.length}/${d.length + t.length} 배치, ${words(d)}/${words(d) + words(t)}개`);
}
if (todo.length) console.log(`\n다음 배치: ${todo.slice(0, 8).map((b) => b.name).join(' ')}${todo.length > 8 ? ' …' : ''}`);
