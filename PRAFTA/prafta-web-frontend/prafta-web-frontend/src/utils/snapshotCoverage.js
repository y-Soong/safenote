/**
 * 하도급 공유 스냅샷 마감 커버리지 표시 유틸 (하도급 근태공유 마감게이트 개선 PS-08a/b).
 *
 * 목적
 *  - snapshot-lists 응답의 coverageMeta(JSON 문자열)를 안전 파싱하고,
 *    상세 헤더 가이드 문구용 제외 내역 요약을 조립한다.
 *  - 승인 팝업(coverageMonths)과 조회 화면(coverageMeta.months)이 월 표기를 공유한다
 *    (승인 사전정보는 'YYYY-MM', 스냅샷 메타는 'YYYYMM' — 양쪽 모두 수용).
 *
 * ⚠️ 표시 전용 모듈이다. 포함/제외 판정 자체는 백엔드(COVERAGE_META 기록)가 단일 출처이며,
 *    프론트는 내려온 메타를 그대로 표기만 한다. 파싱 실패 시 null 반환 → 화면은 문구 생략 fallback.
 */

/**
 * coverageMeta JSON 문자열 → 객체 안전 파싱.
 * 비정형(빈 문자열/파싱 실패/객체 아님)이어도 예외를 던지지 않는다 — 화면 크래시 방지.
 *
 * @param {string|null|undefined} metaStr COVERAGE_META JSON 문자열 (null=메타 없는 이전 자료)
 * @returns {object|null} { closedOnly, months[], relayPartialIncludedYn } 또는 null(파싱 실패/메타 없음)
 */
export function parseCoverageMeta(metaStr) {
  if (typeof metaStr !== "string" || metaStr.trim() === "") return null;
  try {
    const meta = JSON.parse(metaStr);
    if (!meta || typeof meta !== "object" || Array.isArray(meta)) return null;
    return meta;
  } catch (e) {
    // 비정형 메타 — 문구 생략 fallback (PS-08b AC 4)
    return null;
  }
}

/**
 * 커버리지 월 표기 정규화 — 'YYYYMM' → 'YYYY-MM'. 이미 'YYYY-MM' 이면 그대로.
 *
 * @param {string|null|undefined} ym 월 키
 * @returns {string} 'YYYY-MM' 표기 (비정형은 원문 그대로, null/비문자열은 빈 문자열)
 */
export function formatCoverageYm(ym) {
  if (typeof ym !== "string") return "";
  if (ym.length === 6 && !ym.includes("-")) {
    return `${ym.slice(0, 4)}-${ym.slice(4, 6)}`;
  }
  return ym;
}

/**
 * 상세 헤더 가이드용 제외 내역 요약 조립 (Subcon_04 · PS-08b).
 * months 중 status !== 'FULL' 만 나열:
 *  - PARTIAL → "2026-07(부서1·부서2 미마감)" (+ 무부서 제외 시 " · 무부서 근태 제외" 병기)
 *  - NONE    → "2026-07(포함 없음)"
 *
 * @param {object|null} meta parseCoverageMeta 결과
 * @returns {string} 예: "2026-06(부서1·부서2 미마감), 2026-07(포함 없음)". 대상 없음/비정형 → 빈 문자열
 */
export function coverageExcludedSummary(meta) {
  const months = Array.isArray(meta?.months) ? meta.months : [];
  const parts = [];

  months.forEach((m) => {
    if (!m || typeof m !== "object") return;
    if (m.status !== "PARTIAL" && m.status !== "NONE") return; // FULL/미상은 표기 대상 아님

    const ym = formatCoverageYm(m.ym);
    if (m.status === "NONE") {
      parts.push(`${ym}(포함 없음)`);
      return;
    }

    // PARTIAL — 제외 부서명 + 무부서(고아) 제외 병기
    const depts = Array.isArray(m.excludedDeptNms)
      ? m.excludedDeptNms.filter((d) => typeof d === "string" && d.trim() !== "")
      : [];
    const inner = [];
    if (depts.length) inner.push(`${depts.join("·")} 미마감`);
    if (m.orphanUnclosedYn === "Y") inner.push("무부서 근태 제외");
    parts.push(inner.length ? `${ym}(${inner.join(" · ")})` : `${ym}(부분 포함)`);
  });

  return parts.join(", ");
}
