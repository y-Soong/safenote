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

/**
 * BE 근태/스케줄 요약 문자열을 "HH:MM ~ HH:MM" 표시형으로 변환.
 *   - 1구간: "0716~1811" → "07:16 ~ 18:11"
 *   - 2구간: "0700~1300 / 1700~2100" → "07:00 ~ 13:00 / 17:00 ~ 21:00"
 *   - 진행중(종료 결측): "0716~" → "07:16 ~" (근무중)
 * BE 는 raw HHMM 으로 내려주고 콜론 삽입은 FE 가 한다(attendanceSummary/scheduleSummary 규약).
 * 입력이 비거나 형식 불충분한 토큰은 원본을 그대로 두어 손실 없이 폴백한다.
 */
export function formatTimeSummary(summary) {
  if (!summary || typeof summary !== 'string') return summary || ''
  return summary
    .split('/')
    .map((seg) => {
      const parts = seg.split('~')
      if (parts.length !== 2) return seg.trim()
      const left = formatHHMM(parts[0].trim()) || parts[0].trim()
      const right = formatHHMM(parts[1].trim()) || parts[1].trim()
      return `${left} ~ ${right}`.trimEnd()
    })
    .join(' / ')
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
 * 스케줄 본문 한글 표기용.
 */
export function minutesToKorean(min) {
  const total = Number.isFinite(min) ? Math.max(0, Math.round(min)) : 0
  const h = Math.floor(total / 60)
  const m = total % 60
  if (h > 0 && m > 0) return `${h}시간 ${m}분`
  if (h > 0) return `${h}시간`
  return `${m}분`
}

// ====================================================================
// prafta-app-018-E: 부분연차(시간차/반차) 마커 표시 헬퍼 (표시 전용, 순수함수)
//   단위 라벨은 BE 가 아니라 FE 에서 매핑한다(앱 관례 + 한글 인코딩 함정 회피).
//   02·03·04(2시간/1시간/30분)는 모두 "시간차" 그룹라벨로 묶고, 정밀 단위는 시각 range 로 대체한다.
// ====================================================================

/** SYS025 사용단위 코드 → 표시 라벨(그룹). 02~04 는 모두 "시간차". */
const LEAVE_UNIT_LABELS = {
  '00': '종일',
  '01': '반차',
  '02': '시간차',
  '03': '시간차',
  '04': '시간차',
}

/** SYS025 코드 → 단위 라벨. 미상이면 ''. */
export function leaveUnitLabel(useUnitType) {
  return LEAVE_UNIT_LABELS[useUnitType] || ''
}

/** 시간 단위(시간차) 코드 여부 — 02/03/04. */
export function isLeaveTimeUnit(useUnitType) {
  return ['02', '03', '04'].includes(useUnitType)
}

/** BE 가 준 "HHMM~HHMM" → "HH:MM~HH:MM". 입력 null/형식불충분이면 null. */
export function formatLeaveTimeRange(rangeStr) {
  if (!rangeStr || typeof rangeStr !== 'string') return null
  const parts = rangeStr.split('~')
  if (parts.length !== 2) return null
  const s = formatHHMM(parts[0].trim())
  const e = formatHHMM(parts[1].trim())
  if (!s || !e) return null
  return `${s}~${e}`
}

/**
 * 차감일수 → 소수 2자리 반올림 + trailing zero 정리 문자열.
 *   예 0.1875→"0.19", 0.5→"0.5", 1→"1". 비유효 입력이면 null.
 *   ⚠️ 앱 E 의 표시 자릿수 단일 출처. 웹 D(무반올림 trim)와 혼용 금지.
 */
export function formatLeaveDays(days) {
  if (days === null || days === undefined || days === '') return null
  const n = parseFloat(days)
  if (!Number.isFinite(n)) return null
  return String(Number(n.toFixed(2)))
}

// ====================================================================
// 승인된 초과근무(applied overtime) 표시 헬퍼 (표시 전용, 순수함수)
//   BE 응답: { startDate(YYYYMMDD), startTime(HHMM), endDate(YYYYMMDD), endTime(HHMM), workMinutes(int|null) }
//   분→한글은 minutesToKorean, HHMM→HH:MM 은 formatHHMM 재사용(단일 출처).
// ====================================================================

/**
 * OT 1건 → "HH:MM~HH:MM (N시간 M분)" 한 줄 문자열.
 *   - 오버나이트(시작일≠종료일)면 종료 시각 앞에 날짜 구분을 붙인다:
 *       종료일이 시작일 +1 → "익일 HH:MM", 그 외 → "MM.DD HH:MM"(날짜는 점 컨벤션).
 *   - workMinutes 가 null/0 이하이면 분 괄호를 생략하고 시각 range 만 표시.
 *   - 시각 형식 불충분이면 해당 토큰을 "-" 로 폴백(손실 없이).
 */
export function formatOvertimeLine(ot) {
  const o = ot || {}
  const start = formatHHMM(o.startTime) || '-'
  const endHm = formatHHMM(o.endTime) || '-'

  // 오버나이트 종료 표기: 시작일/종료일 비교(둘 다 8자리일 때만 판정).
  const sd = String(o.startDate || '')
  const ed = String(o.endDate || '')
  let endText = endHm
  if (sd.length === 8 && ed.length === 8 && sd !== ed && endHm !== '-') {
    const sDate = ymdToDate(sd)
    const eDate = ymdToDate(ed)
    if (sDate && eDate) {
      const diffDays = Math.round((eDate - sDate) / 86400000)
      if (diffDays === 1) endText = `익일 ${endHm}`
      else endText = `${ed.slice(4, 6)}.${ed.slice(6, 8)} ${endHm}`
    }
  }

  const range = `${start}~${endText}`
  const min = Number(o.workMinutes)
  if (Number.isFinite(min) && min > 0) {
    return `${range} (${minutesToKorean(min)})`
  }
  return range
}

/** OT 분 합계(int) → "N시간 M분" 보조 표기. 0/null 이면 null(미표기). */
export function formatOvertimeMinutes(min) {
  const n = Number(min)
  if (!Number.isFinite(n) || n <= 0) return null
  return minutesToKorean(n)
}

/**
 * 연차 사용 마커 1줄 문자열 생성(부분/종일 공통). 단위코드로 토큰 구성 분기.
 *   00 종일  → "월차 · 종일"
 *   01 반차  → "월차 · 반차 · 0.5일"
 *   02~04 시간차 → "월차 · 시간차 · 03:00~04:30 · 0.19일"
 *   (코드 null/미상) → "월차"
 *   시간차인데 시각 결측이면 시각 토큰만 생략(나머지 유지).
 */
export function formatLeaveMarker(detail) {
  const d = detail || {}
  const typeName = d.leaveTypeName || ''
  const code = d.leaveUnitType
  const tokens = [typeName, leaveUnitLabel(code)]

  if (isLeaveTimeUnit(code)) {
    const range = formatLeaveTimeRange(d.leaveTimeRange)
    if (range) tokens.push(range)
  }

  // 종일(00)은 차감일수 토큰 생략(현행 유지). 반차/시간차만 일수 표기.
  if (code !== '00') {
    const days = formatLeaveDays(d.leaveDays)
    if (days) tokens.push(`${days}일`)
  }

  return tokens.filter(Boolean).join(' · ')
}

/**
 * 같은 날 부분연차(시간차/반차) 다건 마커 객체 배열 생성.
 *   PRAFTA_COM_002-B-1: 각 항목을 { text, pending } 으로 반환(요청중 배지 분기용).
 *     - text: 기존 formatLeaveMarker 1줄 문자열(표시/색 불변).
 *     - pending: 승인 대기(요청중) 여부 — BE leaves[].pendingApproval(다건) 또는 단건 isLeavePending(폴백).
 *   - BE 가 leaves[](각 항목 {leaveTypeName, leaveUnitType, leaveTimeRange, leaveDays, pendingApproval})를 주면 각 항목을 1줄로.
 *   - 구버전 응답(leaves 미제공)이면 단건 스칼라(isLeaveUsed 시)로 1줄 폴백(하위호환, 필드 없으면 pending=false).
 *   - 연차 미사용일이면 빈 배열 → 미렌더.
 */
export function formatLeaveMarkers(obj) {
  const o = obj || {}
  if (Array.isArray(o.leaves) && o.leaves.length) {
    return o.leaves
      .map((lv) => ({ text: formatLeaveMarker(lv), pending: !!lv.pendingApproval }))
      .filter((m) => m.text)
  }
  if (o.isLeaveUsed) {
    const s = formatLeaveMarker(o)
    return s ? [{ text: s, pending: !!o.isLeavePending }] : []
  }
  return []
}
