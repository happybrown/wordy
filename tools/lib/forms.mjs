// POS-aware cleanup of inflected-form lists (NGSL lemma lists include every inflection regardless of POS, e.g. "peopled").
const IRREGULAR_PLURAL = { man: 'men', woman: 'women', child: 'children', foot: 'feet', tooth: 'teeth', mouse: 'mice', person: 'people', goose: 'geese', ox: 'oxen', policeman: 'policemen', fireman: 'firemen', businessman: 'businessmen', gentleman: 'gentlemen' };
export function lev(a, b) {
  if (a === b) return 0;
  let prev = Array.from({ length: b.length + 1 }, (_, i) => i);
  for (let i = 1; i <= a.length; i++) { const cur = [i]; for (let j = 1; j <= b.length; j++) cur[j] = Math.min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1)); prev = cur; }
  return prev[b.length];
}
/**
 * 같은 원형에 -ed/-ing 이 두 갈래로 실려 있을 때 어느 쪽이 표준인지 판단한다.
 * 원본 목록에는 refer → refered(오류)/referred(정답) 처럼 둘 다 들어 있다.
 * 영어 규칙: 마지막 음절에 강세가 있는 2음절 이상 동사는 자음을 겹친다(refer, prefer, occur).
 * 강세가 앞 음절인 동사는 겹치지 않는다(suffer, offer, answer, visit).
 */
const DOUBLES_CONSONANT = new Set(['refer', 'prefer', 'occur', 'transfer', 'infer', 'confer', 'deter', 'incur', 'recur', 'admit', 'permit', 'commit', 'omit', 'submit', 'begin', 'forget', 'regret', 'prefer', 'control', 'patrol', 'compel', 'expel', 'rebel', 'propel']);
/**
 * 어간이 두 모음자 + l 로 끝나면(reveal, appeal, fail, feel) 어느 표기법에서도 자음을 겹치지 않는다.
 * revealled, appealled 는 영국식이 아니라 그냥 오류다.
 * 반면 travel/cancel(단모음 + l)은 영국식에서 travelled/cancelled 로 겹치는 정식 표기다.
 */
const doubleVowelBeforeL = (h) => /[aeiou][aeiou]l$/.test(h);

const NEVER_DOUBLES = new Set(['suffer', 'offer', 'answer', 'visit', 'open', 'listen', 'happen', 'enter', 'order', 'gather', 'wander', 'cover', 'discover', 'remember', 'consider', 'deliver', 'differ', 'wonder', 'murmur', 'benefit', 'profit', 'limit', 'edit', 'exhibit', 'inhabit', 'prohibit']);

/** 해당 원형에서 f 가 잘못된 자음 중복(또는 누락) 표기인지 */
function isMisspelledDouble(headword, f) {
  if (!f.startsWith(headword)) return false;
  const suffix = f.slice(headword.length);
  const last = headword.at(-1);
  const doubled = new RegExp(`^${last}(ed|ing)$`).test(suffix);
  const single = /^(ed|ing)$/.test(suffix);
  if (doubled && NEVER_DOUBLES.has(headword)) return true;   // sufferred
  if (doubled && doubleVowelBeforeL(headword)) return true;   // revealled, appealled
  if (single && DOUBLES_CONSONANT.has(headword)) return true; // refered
  return false;
}

/**
 * 학습자에게 보여주면 안 되는 형태.
 * - 불가산명사의 가짜 복수 (moneys, advices, homeworks)
 * - 등급이 없는 형용사의 비교급 (deader, falser, uniquer)
 * works/waters/salts 처럼 특수 맥락에서 실재하는 형태는 제외하지 않는다.
 */
const NOT_A_FORM = new Set([
  'moneys', 'monies', 'advices', 'homeworks', 'informations', 'newses', 'furnitures',
  'equipments', 'knowledges', 'healths', 'lucks', 'pollutions', 'traffics', 'musics',
  'breads', 'rices', 'weathers', 'funs',
  'safeties', 'insurances', 'violences', 'freedoms', 'patiences', 'progresses', 'accesses',
  'agricultures', 'cautions', 'chaoses', 'leathers', 'organics', 'contraries',
  'prides', 'destructions', 'logics', 'wisdoms', 'braveries', 'honesties',
  'legislations', 'hells', 'childhoods', 'literatures', 'evidences',
  'leisures', 'privacies', 'tobaccos', 'leaderships', 'ownerships',
  'employments', 'reliefs', 'intelligences', 'oppositions',
  'recognitions', 'assistances', 'entertainments', 'retails', 'welfares', 'satisfactions',
  'alcohols', 'independences', 'efficiencies', 'poverties',
  'resistances', 'approvals', 'involvements', 'exposures', 'significances', 'oversea',
  'participations', 'poetries', 'sakes', 'maintenances', 'diversities', 'stabilities',
  'carbons', 'concretes', 'determinations', 'fictions', 'confusions', 'transportations', 'storages',
  'survivals', 'compensations', 'inflations', 'imaginations',
  'excitements', 'attendances', 'immigrations', 'hungers', 'thirsts',
  // 과목명은 복수를 만들지 않는다
  'biologies', 'chemistries', 'physicses', 'geographies', 'historys', 'psychologies', 'economicses',
  'clays', 'muds', 'dusts', 'steels', 'cottons', 'dependences', 'courages', 'despairs',
  'fleshes', 'fora', 'fames', 'minimals', 'preys', 'cattles', 'livestocks',
  'sympathies', 'wheats', 'acceptances', 'toxics', 'ultimates', 
  // 스포츠 이름은 복수를 만들지 않는다
  'golfs', 'soccers', 'tenniss', 'chesses', 'boxings',
  // 편집 거리 1 이라 표기 변형으로 오인되는 철자 오류
  'advertisment', 'advertisments', 'goverment', 'enviroment', 'recieve', 'seperate',
  'definately', 'occured', 'begining', 'writting', 'untill', 'wich', 
  // 과거형이 원형과 같은 동사
  'quitted', 'costed', 'cutted', 'hitted', 'setted', 'letted', 'spreaded', 'shutted',
  'casted', 'bursted', 'hurted', 'beted', 'splited', 'bade', 'ridded',
  // 영국식 철자 규칙 오적용 (fulfil → fulfilled 가 맞다)
  'fulfiled', 'fulfiling', 'instaled', 'instaling', 
  // 단복수 동형 명사 (aircraft, sheep, deer, series, species)
  'aircrafts', 'sheeps', 'deers', 'serieses', 'specieses', 'offsprings',
  'deader', 'deadest', 'falser', 'falsest', 'truer', 'truest', 'uniquer', 'uniquest',
  'perfecter', 'perfectest', 'mainer', 'mainest', 'onlier', 'onliest', 'wholer', 'wholest',
  'baddest', 'badder', 'gooder', 'goodest', 'worser', 'littler', 'littlest', 'farer', 'farest',
  // 불규칙 동사에 실린 가짜 규칙 과거형. bended 는 'on bended knee' 화석 표현에만 남았다.
  // 고어·폐어 철자
  'blest', 'gaol', 'gaoled', 'gaols', 'shew', 'shewed', 'thou', 'thee',
  'bended', 'leaved', 'builded', 'sended', 'spended', 'sleeped', 'keeped', 'feeled',
  'meaned', 'dealed', 'holded', 'selled', 'telled', 'finded', 'standed', 'teached',
  'catched', 'buyed', 'bringed', 'thinked', 'fighted', 'payed',
  'squarer', 'squarest', 'wronger', 'wrongest', 'famouser', 'famousest', 'correcter', 'correctest',
  'unlikelier', 'unlikeliest', 'gayer', 'gayest', 'eagerer', 'eagerest', 'awarer', 'awarest',
  'sparer', 'sparest', 'dailyer', 'dailyest', 'finaler', 'finalest',
  'eviler', 'evilest', 'bitterer', 'bitterest', 'acutest', 'acuter', 'corrupter', 'corruptest',
  // 형용사·부사가 주 품사인데 명사 복수로 실린 것들.
  // generals(장군들), privates(병사들)처럼 실재하는 명사 복수는 넣지 않는다 —
  // 항목이 그 명사 sense 를 쓰면 예문에 필요하다.
  'publics', 'commons', 'sames', 'ables', 'reals', 'eachs',
  // 학술·비즈니스 불가산 명사. 한국어 '준수/보존/소비/담보'가 셀 수 있게 느껴져
  // 복수를 만들기 쉬운 것들. consents(동의서)·completions(완공 건수)처럼
  // 전문 분야에서 실재하는 복수는 넣지 않는다.
  'compliances', 'conservations', 'consumptions', 'collaterals',
]);
/**
 * 불규칙 변화형. 어간이 크게 바뀌어 편집 거리·접미사 규칙으로는 잡히지 않는다.
 * (bad → worse, bear → borne 이 걸러지던 버그를 막는다.)
 */
const IRREGULAR = new Map([
  ['bad', ['worse', 'worst']], ['good', ['better', 'best']], ['well', ['better', 'best']],
  ['little', ['less', 'least']], ['many', ['more', 'most']], ['much', ['more', 'most']],
  ['far', ['further', 'furthest', 'farther', 'farthest']],
  ['be', ['am', 'is', 'are', 'was', 'were', 'been', 'being']],
  ['go', ['went', 'gone', 'goes', 'going']], ['bear', ['bore', 'borne', 'born']],
  ['see', ['saw', 'seen']], ['eat', ['ate', 'eaten']], ['take', ['took', 'taken']],
  ['give', ['gave', 'given']], ['come', ['came']], ['do', ['did', 'done', 'does']],
  ['break', ['broke', 'broken']], ['choose', ['chose', 'chosen']], ['drive', ['drove', 'driven']],
  ['fall', ['fell', 'fallen']], ['forget', ['forgot', 'forgotten']], ['freeze', ['froze', 'frozen']],
  ['get', ['got', 'gotten']], ['grow', ['grew', 'grown']], ['know', ['knew', 'known']],
  ['ride', ['rode', 'ridden']], ['rise', ['rose', 'risen']], ['speak', ['spoke', 'spoken']],
  ['steal', ['stole', 'stolen']], ['throw', ['threw', 'thrown']], ['wear', ['wore', 'worn']],
  ['write', ['wrote', 'written']], ['draw', ['drew', 'drawn']], ['fly', ['flew', 'flown']],
  ['blow', ['blew', 'blown']], ['show', ['showed', 'shown']], ['swim', ['swam', 'swum']],
  ['begin', ['began', 'begun']], ['drink', ['drank', 'drunk']], ['sing', ['sang', 'sung']],
  ['ring', ['rang', 'rung']], ['run', ['ran']], ['sit', ['sat']], ['stand', ['stood']],
  ['teach', ['taught']], ['think', ['thought']], ['buy', ['bought']], ['bring', ['brought']],
  ['catch', ['caught']], ['fight', ['fought']], ['seek', ['sought']], ['leave', ['left']],
  ['lose', ['lost']], ['make', ['made']], ['say', ['said']], ['sell', ['sold']], ['tell', ['told']],
  ['feel', ['felt']], ['keep', ['kept']], ['sleep', ['slept']], ['sweep', ['swept']],
  ['meet', ['met']], ['find', ['found']], ['hold', ['held']], ['hear', ['heard']],
  ['pay', ['paid']], ['lay', ['laid']], ['lead', ['led']], ['feed', ['fed']],
  ['have', ['had', 'has', 'having']], ['person', ['people']], ['child', ['children']],
  ['foot', ['feet']], ['tooth', ['teeth']], ['man', ['men']], ['woman', ['women']],
  ['mouse', ['mice']], ['goose', ['geese']], ['life', ['lives']], ['leaf', ['leaves']],
  ['knife', ['knives']], ['wife', ['wives']], ['shelf', ['shelves']], ['thief', ['thieves']],
]);

/** 약어는 변화형이 아니라 별개 단어다. 학습 카드의 '변화형' 줄에 넣지 않는다. */
const ABBREVIATION = new Set(['math', 'prof', 'lab', 'gym', 'sculpt', 'exam', 'fridge', 'flu', 'app']);

const SLANG = new Set([
  // -ing 의 g 가 탈락한 구어 표기
  'bleedin', 'goin', 'doin', 'comin', 'runnin', 'talkin', 'lookin', 'workin', 'nothin', 'somethin','gotta', 'gettin', 'gonna', 'wanna', 'dunno', 'kinda', 'sorta', 'lemme', 'gimme', "ain't", 'em', 'ya', 'yeah', 'cuz', 'til']);
const badS = (h) => /(s|x|z|ch|sh)$/.test(h) || /[^aeiou]y$/.test(h); // marchs, storys are not words
const isPlural = (h, f) => (IRREGULAR_PLURAL[h] ? IRREGULAR_PLURAL[h] === f : (f === h + 's' && !badS(h)) || f === h + 'es' || (h.endsWith('y') && f === h.slice(0, -1) + 'ies') || (h.endsWith('f') && f === h.slice(0, -1) + 'ves') || (h.endsWith('fe') && f === h.slice(0, -2) + 'ves'));
const isVerbish = (h, f) => /(s|es|ed|d|ing|en|t|n)$/.test(f) || lev(h, f) <= 3;
const isAdjish = (h, f) => /(er|est|ier|iest|ly|r|st)$/.test(f);

/**
 * @param headword lowercase headword
 * @param forms candidate forms
 * @param pos array of pos codes (n, v, adj, adv, …); empty = unknown (keep verb-like + plural)
 * @param usedIn optional text corpus (examples etc.): forms that actually occur are always kept
 */
export function cleanForms(headword, forms, pos, usedIn = '') {
  const hasV = pos.includes('v'), hasN = pos.includes('n'), hasAdj = pos.includes('adj') || pos.includes('adv');
  const unknown = pos.length === 0;
  const low = usedIn.toLowerCase();
  const used = (f) => low && new RegExp(`(^|[^a-z])${f.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}(?=$|[^a-z])`).test(low);
  // 원본 목록에 불규칙형이 빠져 있으면 채워 넣는다 (master.tsv 의 person 에는 people 이 없다)
  const withIrregular = [...new Set([...forms, ...(IRREGULAR.get(headword) ?? [])])];
  return withIrregular.filter((f) => {
    if (!f || f === headword) return false;
    if (f === headword + 's' && badS(headword)) return false;
    if (f === headword + 'es' && !/(s|x|z|ch|sh|o)$/.test(headword)) return false;
    if (/[.]/.test(f) && !/[.]/.test(headword)) return false; // "dr." for doctor
    if (SLANG.has(f)) return false;
    if (ABBREVIATION.has(f) && f !== headword) return false;
    // 불가산 가짜 복수·비표준 비교급. 동사 sense 가 있으면 -s 는 3인칭 단수일 수 있어 살린다.
    if (NOT_A_FORM.has(f) && !(pos.includes('v') && (f === headword + 's' || f === headword + 'es'))) return false;
    if (isMisspelledDouble(headword, f)) return false;
    // 동명사의 복수(scorings, filings, meetings 아님 — meeting 은 headword 자체)는
    // 동사 변화형도 명사 복수도 아니라 학습 카드에 넣을 이유가 없다.
    if (/ings$/.test(f) && f !== headword && !headword.endsWith('ing')) return false;
    if (used(f)) return true;
    const irr = IRREGULAR.get(headword);
    if (irr?.includes(f)) return true;
    // 불규칙 '복수'가 정해진 명사는 규칙 복수를 만들어 붙이지 않는다 (leaf → leafs, child → childs).
    // 동사의 3인칭 단수(-s)는 불규칙 과거와 무관하게 필요하므로 명사 전용일 때만 막는다.
    const nounOnly = pos.length > 0 && !pos.includes('v');
    if (irr && nounOnly && (f === headword + 's' || f === headword + 'es')) return false;
    const suffix = f.startsWith(headword) ? f.slice(headword.length) : null;
    // 영국식 -ue 표기(dialog → dialogue, catalog → catalogue)는 원형보다 2자 길다
    const ueVariant = f === headword + 'ue' || f === headword + 'ues';
    const variant = ueVariant ||
      (lev(f, headword) <= 2 && Math.abs(f.length - headword.length) <= 1 && suffix === null && !headword.startsWith(f));
    if (variant) return true; // spelling variants: colour, advertize
    if (hasV && isVerbish(headword, f)) return true;
    if (hasN && isPlural(headword, f)) return true;
    if (hasAdj && isAdjish(headword, f)) return true;
    if (unknown && (isVerbish(headword, f) || isPlural(headword, f))) return true;
    return false;
  }).sort();
}
