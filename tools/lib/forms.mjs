// POS-aware cleanup of inflected-form lists (NGSL lemma lists include every inflection regardless of POS, e.g. "peopled").
const IRREGULAR_PLURAL = { man: 'men', woman: 'women', child: 'children', foot: 'feet', tooth: 'teeth', mouse: 'mice', person: 'people', goose: 'geese', ox: 'oxen', policeman: 'policemen', fireman: 'firemen', businessman: 'businessmen', gentleman: 'gentlemen' };
export function lev(a, b) {
  if (a === b) return 0;
  let prev = Array.from({ length: b.length + 1 }, (_, i) => i);
  for (let i = 1; i <= a.length; i++) { const cur = [i]; for (let j = 1; j <= b.length; j++) cur[j] = Math.min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1)); prev = cur; }
  return prev[b.length];
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
  'deader', 'deadest', 'falser', 'falsest', 'truer', 'truest', 'uniquer', 'uniquest',
  'perfecter', 'perfectest', 'mainer', 'mainest', 'onlier', 'onliest', 'wholer', 'wholest',
]);
const SLANG = new Set(['gotta', 'gettin', 'gonna', 'wanna', 'dunno', 'kinda', 'sorta', 'lemme', 'gimme', "ain't", 'em', 'ya', 'yeah', 'cuz', 'til']);
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
  return [...new Set(forms)].filter((f) => {
    if (!f || f === headword) return false;
    if (f === headword + 's' && badS(headword)) return false;
    if (f === headword + 'es' && !/(s|x|z|ch|sh|o)$/.test(headword)) return false;
    if (/[.]/.test(f) && !/[.]/.test(headword)) return false; // "dr." for doctor
    if (SLANG.has(f) || NOT_A_FORM.has(f)) return false;
    if (used(f)) return true;
    const suffix = f.startsWith(headword) ? f.slice(headword.length) : null;
    const variant = lev(f, headword) <= 2 && Math.abs(f.length - headword.length) <= 1 && suffix === null && !headword.startsWith(f);
    if (variant) return true; // spelling variants: colour, advertize
    if (hasV && isVerbish(headword, f)) return true;
    if (hasN && isPlural(headword, f)) return true;
    if (hasAdj && isAdjish(headword, f)) return true;
    if (unknown && (isVerbish(headword, f) || isPlural(headword, f))) return true;
    return false;
  }).sort();
}
