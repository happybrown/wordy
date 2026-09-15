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

test('자음 중복 철자 오류를 걸러낸다', () => {
  // 원본 목록에는 refered(오류)와 referred(정답)가 함께 실려 있다
  assert.deepEqual(cleanForms('refer', ['refered', 'refering', 'referred', 'referring', 'refers'], ['v']),
    ['referred', 'referring', 'refers']);
  // suffer 는 강세가 앞이라 자음을 겹치지 않는다
  assert.deepEqual(cleanForms('suffer', ['suffered', 'sufferred', 'suffers'], ['v']), ['suffered', 'suffers']);
  assert.ok(cleanForms('prefer', ['prefered', 'preferred'], ['v']).includes('preferred'));
  assert.ok(!cleanForms('prefer', ['prefered', 'preferred'], ['v']).includes('prefered'));
  // 정상 동사는 그대로
  assert.deepEqual(cleanForms('visit', ['visited', 'visiting', 'visits'], ['v']), ['visited', 'visiting', 'visits']);
});

test('형용사가 주 품사인 단어의 명사 복수는 넣지 않는다', () => {
  assert.deepEqual(cleanForms('public', ['publics'], ['n', 'adj']), []);
});

test('두 모음자 + l 로 끝나는 어간은 자음을 겹치지 않는다', () => {
  // revealled 는 영국식이 아니라 오류
  assert.deepEqual(cleanForms('reveal', ['revealed', 'revealing', 'revealled', 'revealling'], ['v']),
    ['revealed', 'revealing']);
  // 단모음 + l 은 영국식에서 겹치는 정식 표기
  assert.ok(cleanForms('travel', ['traveled', 'travelled'], ['v']).includes('travelled'));
  assert.ok(cleanForms('cancel', ['canceled', 'cancelled'], ['v']).includes('cancelled'));
  // 마지막 음절 강세는 미국식도 겹친다
  assert.ok(cleanForms('control', ['controlled', 'controlling'], ['v']).includes('controlled'));
});

test('실재하는 명사 복수는 형용사가 주 품사여도 유지한다', () => {
  // generals(장군들), privates(병사들)는 실제 명사 복수다
  assert.deepEqual(cleanForms('general', ['generals'], ['n', 'adj']), ['generals']);
  // publics 는 쓰지 않는 형태
  assert.deepEqual(cleanForms('public', ['publics'], ['n', 'adj']), []);
});

test('불가산명사의 가짜 복수는 명사 항목에서만 거른다', () => {
  assert.deepEqual(cleanForms('safety', ['safeties'], ['n']), []);
  assert.deepEqual(cleanForms('insurance', ['insurances'], ['n']), []);
  // 동사 sense 가 있으면 -s/-es 는 3인칭 단수이므로 살린다
  assert.ok(cleanForms('access', ['accesses', 'accessed'], ['n', 'v']).includes('accesses'));
  assert.ok(cleanForms('progress', ['progresses', 'progressed'], ['n', 'v']).includes('progresses'));
  // 실제 용법이 있는 복수는 유지
  assert.deepEqual(cleanForms('wine', ['wines'], ['n']), ['wines']);
  assert.deepEqual(cleanForms('content', ['contents'], ['n']), ['contents']);
});

test('실제로 쓰지 않는 비교급은 거른다', () => {
  assert.deepEqual(cleanForms('square', ['squarer', 'squarest', 'squares'], ['n', 'adj']), ['squares']);
  assert.ok(!cleanForms('wrong', ['wronger', 'wrongest'], ['adj']).includes('wronger'));
});

test('약어는 변화형이 아니다', () => {
  assert.deepEqual(cleanForms('professor', ['prof', 'professors'], ['n']), ['professors']);
  assert.deepEqual(cleanForms('mathematics', ['math', 'maths'], ['n']), []);
  assert.deepEqual(cleanForms('laboratory', ['lab', 'laboratories'], ['n']), ['laboratories']);
  // math 가 원형일 때 maths 는 영국식 표기 변형이므로 유지
  assert.deepEqual(cleanForms('math', ['maths'], ['n']), ['maths']);
});

test('불규칙 동사에 실린 가짜 규칙 과거형을 거른다', () => {
  // bended 는 'on bended knee' 화석 표현에만 남은 고어형
  assert.deepEqual(cleanForms('bend', ['bended', 'bends', 'bending', 'bent'], ['v']),
    ['bending', 'bends', 'bent']);
  assert.ok(!cleanForms('leave', ['leaved', 'left', 'leaves'], ['v']).includes('leaved'));
  // heard 는 hear 의 정상 과거형이므로 유지
  assert.ok(cleanForms('hear', ['heard', 'hears'], ['v']).includes('heard'));
});

test('고어·폐어 철자는 학습 변화형이 아니다', () => {
  // blest 는 bless 의 시적 과거형, gaol 은 jail 의 영국 폐어 철자
  assert.deepEqual(cleanForms('bless', ['blessed', 'blest'], ['v']), ['blessed']);
  assert.ok(!cleanForms('jail', ['gaol', 'jails'], ['n', 'v']).includes('gaol'));
});

test('추상 불가산명사의 복수를 거른다', () => {
  assert.deepEqual(cleanForms('pride', ['prides'], ['n']), []);
  assert.deepEqual(cleanForms('destruction', ['destructions'], ['n']), []);
  assert.deepEqual(cleanForms('logic', ['logics'], ['n']), []);
  assert.deepEqual(cleanForms('agriculture', ['agricultures'], ['n']), []);
});

test('단복수 동형 명사에 -s 를 붙이지 않는다', () => {
  assert.deepEqual(cleanForms('aircraft', ['aircrafts'], ['n']), []);
  assert.deepEqual(cleanForms('sheep', ['sheeps'], ['n']), []);
  assert.deepEqual(cleanForms('series', ['serieses'], ['n']), []);
});

test('과거형이 원형과 같은 동사에 -ed 를 붙이지 않는다', () => {
  // quit/cost/spread 는 과거형도 같은 형태다
  assert.deepEqual(cleanForms('quit', ['quitted', 'quits', 'quitting'], ['v']), ['quits', 'quitting']);
  assert.ok(!cleanForms('cost', ['costed', 'costs'], ['v']).includes('costed'));
  assert.ok(!cleanForms('spread', ['spreaded', 'spreads'], ['v']).includes('spreaded'));
});
