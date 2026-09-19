// 좌우 피드는 원 커뮤니티 제목을 그대로 보여주는데, 크롤러의 수집 단계
// 필터(crawler/siso_crawler/profanity.py, 백엔드 ProfanityFilter와 같은
// 목록)는 완성형 단어만 걸러서 "ㅈㄹ", "ㅅㅂ" 같은 초성 표기가 첫 화면에
// 그대로 노출되고 있었음(2026-09-19 종합 검토, 이틀간 40건). 수집량을
// 줄이지 않도록 걸러내는 대신 표시 단계에서 가린다. 특정 진영을 겨냥한
// 정치 은어는 넣지 않음 — 넣으려면 좌우 대칭으로 따로 설계할 것.
const PROFANITY = [
  "씨발", "씨팔", "시발", "개새끼", "새끼", "병신", "지랄",
  "좆", "존나", "닥쳐", "미친놈", "미친년", "걸레", "창녀",
  "죽여버려", "꺼져", "찐따", "한남", "김치녀", "맘충",
  "ㅅㅂ", "ㅆㅂ", "ㅂㅅ", "ㅈㄹ", "ㅈㄴ", "ㅄ", "ㅗ",
];

const PATTERN = new RegExp(
  PROFANITY.map((word) => word.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")).join("|"),
  "g",
);

export function maskProfanity(text: string): string {
  return text.replace(PATTERN, (match) => "○".repeat(match.length));
}
