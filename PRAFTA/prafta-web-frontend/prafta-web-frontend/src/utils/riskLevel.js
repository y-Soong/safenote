// 위험도(빈도×강도) → 6단계 등급/라벨/클래스 단일 출처.
// Risk_02.vue 관리기준표(getRiskCellClass)와 동일 권위 기준.
// 매우낮음 1~3 / 낮음 4~7 / 보통 8 / 약간높음 9~12 / 높음 13~15 / 매우높음 16~20
export const RISK_BANDS = [
  { min: 16, max: 20, label: "매우높음", cls: "risk-very-high" },
  { min: 13, max: 15, label: "높음", cls: "risk-high" },
  { min: 9, max: 12, label: "약간높음", cls: "risk-slightly-high" },
  { min: 8, max: 8, label: "보통", cls: "risk-normal" },
  { min: 4, max: 7, label: "낮음", cls: "risk-low" },
  { min: 1, max: 3, label: "매우낮음", cls: "risk-very-low" },
];

// 위험도 점수 → 밴드 객체(없으면 null). 빈/0/비숫자는 null.
export const getRiskBand = (score) => {
  const n = Number(score);
  if (!Number.isFinite(n) || n <= 0) return null;
  return RISK_BANDS.find((b) => n >= b.min && n <= b.max) || null;
};

// 6단계 CSS 클래스명. 빈값이면 ''
export const getRiskLevelClass6 = (score) => getRiskBand(score)?.cls || "";

// 6단계 등급 라벨. 빈값이면 ''
export const getRiskLevelLabel = (score) => getRiskBand(score)?.label || "";

// "값 (등급)" 표기. 예: 15 → "15 (높음)". 빈값이면 '-'
export const formatRiskLevelText = (score) => {
  const band = getRiskBand(score);
  if (band) return `${score} (${band.label})`;
  return score ? String(score) : "-";
};

// 매우낮음(1~3) 여부 — 개선완료 가드용
export const isVeryLow = (score) => {
  const n = Number(score);
  return Number.isFinite(n) && n >= 1 && n <= 3;
};

// 하위 호환 별칭(기존 로컬 getRiskLevelClass 대체 import 편의)
export const getRiskLevelClass = getRiskLevelClass6;
