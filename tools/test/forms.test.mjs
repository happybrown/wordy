import { test } from 'node:test';
import assert from 'node:assert/strict';
import { cleanForms } from '../lib/forms.mjs';

test('불규칙 변화형은 어간이 크게 바뀌어도 유지된다', () => {
  assert.deepEqual(cleanForms('bad', ['worse', 'worst'], ['adj']), ['worse', 'worst']);
  assert.deepEqual(cleanForms('good', ['better', 'best'], ['adj']), ['best', 'better']);
  assert.ok(cleanForms('bear', ['bore', 'borne', 'bears'], ['v']).includes('borne'));
  assert.ok(cleanForms('person', ['people'], ['n']).includes('people'));
});

test('동사의 3인칭 단수는 불규칙 과거가 있어도 유지된다', () => {
  assert.ok(cleanForms('run', ['ran', 'runs', 'running'], ['v']).includes('runs'));
  assert.ok(cleanForms('get', ['got', 'gets', 'getting'], ['v']).includes('gets'));
  assert.ok(cleanForms('go', ['went', 'goes', 'going'], ['v']).includes('goes'));
});

test('불규칙 복수가 있는 명사는 규칙 복수를 만들지 않는다', () => {
  assert.deepEqual(cleanForms('leaf', ['leaves', 'leafs'], ['n']), ['leaves']);
  assert.deepEqual(cleanForms('child', ['children', 'childs'], ['n']), ['children']);
});

test('불가산명사의 가짜 복수와 등급 없는 형용사의 비교급은 제거한다', () => {
  assert.deepEqual(cleanForms('money', ['moneys'], ['n']), []);
  assert.deepEqual(cleanForms('advice', ['advices'], ['n']), []);
  assert.deepEqual(cleanForms('dead', ['deader', 'deadest'], ['adj']), []);
});

test('품사에 맞지 않는 형태는 제거한다', () => {
  // people 을 명사로만 쓰면 동사형 peopled/peopling 은 필요 없다
  assert.deepEqual(cleanForms('people', ['peopled', 'peopling', 'peoples'], ['n']), ['peoples']);
  // arm 을 명사로만 쓴 항목
  assert.deepEqual(cleanForms('arm', ['armed', 'arming', 'arms'], ['n']), ['arms']);
});

test('예문에 실제로 쓰인 형태는 품사와 무관하게 유지한다', () => {
  const kept = cleanForms('top', ['topped', 'tops'], ['n'], 'He topped the cake with cream.');
  assert.ok(kept.includes('topped'));
});

test('철자 변형은 유지한다', () => {
  assert.ok(cleanForms('color', ['colour', 'colors'], ['n']).includes('colour'));
});

test('동명사의 복수는 학습 변화형이 아니다', () => {
  // score/file 의 동사 sense 를 써도 scorings/filings 는 넣지 않는다
  assert.deepEqual(cleanForms('score', ['scored', 'scores', 'scoring', 'scorings'], ['n', 'v']), ['scored', 'scores', 'scoring']);
  assert.ok(!cleanForms('file', ['filed', 'filing', 'filings'], ['n', 'v']).includes('filings'));
  // 다만 headword 자체가 -ing 으로 끝나는 명사의 복수는 정상이다
  assert.deepEqual(cleanForms('meeting', ['meetings'], ['n']), ['meetings']);
  assert.deepEqual(cleanForms('building', ['buildings'], ['n']), ['buildings']);
});
