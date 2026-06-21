/**
 * 승인 관리(관리자 모드 Phase 2) 표시/입력 변환 유틸.
 *
 * 백엔드 승인 계약(001-phase2-admin-approval-plan §3)의 날짜/시각 원본 필드를
 *  - 카드/상세 표시용 문자열(formatYmdDisplay / formatDateTimeDisplay)
 *  - 조정 시트의 date/time input 초기값(splitDateTime)
 * 으로 변환한다.
 *
 * 서버가 이미 표시용 문자열을 내려주면 호출부에서 그 값을 우선 사용하고,
 * 본 유틸은 원본(YYYYMMDD / 타임스탬프)일 때의 폴백 포매팅을 담당한다.
 *
 * ⚠️ 도메인 비종속 순수 변환만 둔다(비즈니스 판정 없음).
 */

// 숫자/문자 혼재 입력에서 숫자만 추출
function digitsOnly(value) {
  return String(value).replace(/\D/g, '')
}

/**
 * 대상일자(YYYYMMDD 또는 YYYY-MM-DD 등) → "YYYY.MM.DD".
 * 파싱 불가하면 원본 문자열을 그대로 반환(표시 깨짐 방지).
 */
export function formatYmdDisplay(value) {
  if (value == null || value === '') return ''
  const d = digitsOnly(value)
  if (d.length >= 8) {
    return `${d.slice(0, 4)}.${d.slice(4, 6)}.${d.slice(6, 8)}`
  }
  return String(value)
}

/**
 * 요청일시(YYYYMMDDHHmmss / YYYY-MM-DD HH:mm:ss / ISO 등) → "YYYY.MM.DD HH:mm".
 * 날짜만 있으면 날짜만, 파싱 불가하면 원본을 그대로 반환.
 */
export function formatDateTimeDisplay(value) {
  if (value == null || value === '') return ''
  const d = digitsOnly(value)
  if (d.length >= 12) {
    return `${d.slice(0, 4)}.${d.slice(4, 6)}.${d.slice(6, 8)} ${d.slice(8, 10)}:${d.slice(10, 12)}`
  }
  if (d.length >= 8) {
    return formatYmdDisplay(d)
  }
  return String(value)
}

/**
 * 근태 시각 표시(근무일자 기준 오버나이트 인지).
 *   value(타임스탬프/조인문자열)의 날짜가 targetYmd(근무일자)와 같으면 "HH:mm"만 표시하고,
 *   다르면(오버나이트 등 근무일자≠실제 출근/퇴근일자) "MM.DD HH:mm"으로 날짜를 덧붙여
 *   관리자가 다른 날임을 알 수 있게 한다. 날짜+시각(12자리)이 아니면 formatDateTimeDisplay 폴백.
 */
export function formatTimeWithDateIfDiff(value, targetYmd) {
  if (value == null || value === '') return ''
  const d = digitsOnly(value)
  if (d.length < 12) return formatDateTimeDisplay(value)
  const ymd = d.slice(0, 8)
  const hm = `${d.slice(8, 10)}:${d.slice(10, 12)}`
  const tgt = digitsOnly(targetYmd || '')
  if (tgt.length >= 8 && ymd === tgt.slice(0, 8)) return hm
  return `${ymd.slice(4, 6)}.${ymd.slice(6, 8)} ${hm}`
}

/**
 * 월-일(YYYYMMDD 또는 YYYY-MM-DD 등) → "MM.DD".
 * 파싱 불가하면 원본 문자열을 그대로 반환(표시 깨짐 방지).
 */
export function formatMdDot(value) {
  if (value == null || value === '') return ''
  const d = digitsOnly(value)
  if (d.length >= 8) {
    return `${d.slice(4, 6)}.${d.slice(6, 8)}`
  }
  // YYYYMM(6자리) 또는 MMDD(4자리)도 월-일 추출 시도
  if (d.length === 4) {
    return `${d.slice(0, 2)}.${d.slice(2, 4)}`
  }
  return String(value)
}

/**
 * 연-월(YYYYMM 또는 YYYY-MM 등) → "YYYY.MM".
 * 파싱 불가하면 원본 문자열을 그대로 반환(표시 깨짐 방지).
 */
export function formatYmDot(value) {
  if (value == null || value === '') return ''
  const d = digitsOnly(value)
  if (d.length >= 6) {
    return `${d.slice(0, 4)}.${d.slice(4, 6)}`
  }
  return String(value)
}

/**
 * 초가 실재하는 타임스탬프 → "YYYY.MM.DD HH:mm:ss".
 * 초가 없으면 분까지, 날짜만 있으면 날짜만, 파싱 불가하면 원본을 그대로 반환.
 */
export function formatHms(value) {
  if (value == null || value === '') return ''
  const d = digitsOnly(value)
  if (d.length >= 14) {
    return `${d.slice(0, 4)}.${d.slice(4, 6)}.${d.slice(6, 8)} ${d.slice(8, 10)}:${d.slice(10, 12)}:${d.slice(12, 14)}`
  }
  return formatDateTimeDisplay(value)
}

/**
 * 타임스탬프/시각 값을 date/time input 초기값으로 분해.
 *   반환: { date:'YYYY-MM-DD'|'' , time:'HH:mm'|'' }
 * - "YYYYMMDDHHmm[ss]" / "YYYY-MM-DD HH:mm[:ss]" / ISO("...T...") / "HH:mm"(시각만) 지원.
 */
export function splitDateTime(value) {
  if (value == null || value === '') return { date: '', time: '' }

  const str = String(value).trim()

  // 시각만("HH:mm" 또는 "HH:mm:ss")
  if (/^\d{1,2}:\d{2}/.test(str)) {
    const [hh, mm] = str.split(':')
    return { date: '', time: `${hh.padStart(2, '0')}:${mm.slice(0, 2)}` }
  }

  const d = digitsOnly(str)
  let date = ''
  let time = ''
  if (d.length >= 8) {
    date = `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6, 8)}`
  }
  if (d.length >= 12) {
    time = `${d.slice(8, 10)}:${d.slice(10, 12)}`
  }
  return { date, time }
}
