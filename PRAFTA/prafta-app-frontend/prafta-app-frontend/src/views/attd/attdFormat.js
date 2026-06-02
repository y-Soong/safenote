// attdFormat.js — 내 근태 조회 화면 전용 포맷 헬퍼
// - 도메인(근태) 종속 표시 헬퍼이므로 공통 utils 가 아닌 attd 화면 폴더에 둔다.
// - 시간=HHMM(4자리), 일자=YYYYMMDD(8자리), 분=int 를 입력으로 받는다.
// - 시안(prafta_my_attendance_v8.html) 의 정확한 워딩/포맷을 그대로 따른다.

const WEEKDAY_KO = ['일', '월', '화', '수', '목', '금', '토']
const DOW_KEY = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']

/** HHMM(예: "0930") → "09:30". 형식 불충분 시 null. */
export function formatHHMM(s) {
  if (!s || String(s).length < 4) return null
  const v = String(s)
  return `${v.slice(0, 2)}:${v.slice(2, 4)}`
}

/** 시작/종료 HHMM → "09:30 ~ 18:00". 한쪽이 없으면 "-". */
export function formatRange(start, end) {
  const s = formatHHMM(start) || '-'
  const e = formatHHMM(end) || '-'
  return `${s} ~ ${e}`
}

/** YYYYMMDD → Date 객체. 형식 불충분 시 null. */
export function ymdToDate(ymd) {
  const v = String(ymd || '')
  if (v.length < 8) return null
  const y = Number(v.slice(0, 4))
  const m = Number(v.slice(4, 6))
  const d = Number(v.slice(6, 8))
  if (!y || !m || !d) return null
  return new Date(y, m - 1, d)
}

/** Date → YYYYMMDD 문자열. */
export function dateToYmd(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}${m}${d}`
}

/** YYYYMMDD → "2026년 5월 20일". */
export function formatYmdLong(ymd) {
  const date = ymdToDate(ymd)
  if (!date) return ''
  return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`
}

/** YYYYMMDD → "5월 20일 (수)". */
export function formatYmdShortWithDow(ymd) {
  const date = ymdToDate(ymd)
  if (!date) return ''
  return `${date.getMonth() + 1}월 ${date.getDate()}일 (${WEEKDAY_KO[date.getDay()]})`
}

/** YYYYMMDD → "수요일". */
export function formatDowLong(ymd) {
  const date = ymdToDate(ymd)
  if (!date) return ''
  return `${WEEKDAY_KO[date.getDay()]}요일`
}

/** dayOfWeek 키(MON~SUN) → "월". 키가 없으면 ymd 로 보조 산출. */
export function dowShort(dow, ymd) {
  const map = { MON: '월', TUE: '화', WED: '수', THU: '목', FRI: '금', SAT: '토', SUN: '일' }
  if (dow && map[dow]) return map[dow]
  const date = ymdToDate(ymd)
  return date ? WEEKDAY_KO[date.getDay()] : ''
}

/** dayOfWeek 키 또는 ymd 로 SAT/SUN 판정 → 'SAT'|'SUN'|''. */
export function dowKey(dow, ymd) {
  if (dow === 'SAT' || dow === 'SUN') return dow
  const date = ymdToDate(ymd)
  if (!date) return ''
  const k = DOW_KEY[date.getDay()]
  return k === 'SAT' || k === 'SUN' ? k : ''
}

/** YYYYMMDD → DD(앞 0 제거, 예: "08" → "8"). */
export function dayNumber(ymd) {
  const v = String(ymd || '')
  if (v.length < 8) return ''
  return String(Number(v.slice(6, 8)))
}

/** YYYYMM → "2026년 5월". */
export function formatYearMonth(ym) {
  const v = String(ym || '')
  if (v.length < 6) return ''
  return `${v.slice(0, 4)}년 ${Number(v.slice(4, 6))}월`
}

/**
 * 분(int) → "Nh Nm" (예: 843 → "14h 03m"). 합계 카드용.
 * null/undefined 는 "0h 00m".
 */
export function minutesToHhMm(min) {
  const total = Number.isFinite(min) ? Math.max(0, Math.round(min)) : 0
  const h = Math.floor(total / 60)
  const m = total % 60
  return `${h}h ${String(m).padStart(2, '0')}m`
}

/**
 * 분(int) → "N시간" 또는 "N시간 N분" (예: 420 → "7시간", 450 → "7시간 30분").
 * 스케줄/표준화 본문 한글 표기용.
 */
export function minutesToKorean(min) {
  const total = Number.isFinite(min) ? Math.max(0, Math.round(min)) : 0
  const h = Math.floor(total / 60)
  const m = total % 60
  if (h > 0 && m > 0) return `${h}시간 ${m}분`
  if (h > 0) return `${h}시간`
  return `${m}분`
}
