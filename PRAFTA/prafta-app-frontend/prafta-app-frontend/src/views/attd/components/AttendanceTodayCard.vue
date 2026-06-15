<!--
  AttendanceTodayCard.vue — 오늘 근태 카드 (5 변형 통합)
  - 작업 ID: APP002-06 (UI 명세: UI-A002) / 보완: prafta-app-014-C (UI-A014-1)
  - 시안 화면 1~5: 근무중 / 퇴근완료 / 사업장다름 / 2구간 / 퇴근미등록
  - 정책: attd §7.1~7.3, §11.1 / 시안 §3.1·§3.5·§3.6·§4.2
  - 2행(스케줄/근태) 구조는 구간(slot)마다 반복. slotCount>=2 면 구분선으로 분리.
  - 활성/비활성은 detail.actions(서버 산출) 표시만. 비즈니스 판정 금지.
  - 참조 패턴: views/main/components/AttendanceCard.vue (badge/btn/HHMM 포맷)
  - prafta-app-013: "수정 요청"은 항상 활성(4액션 시트를 연다). 개별 게이팅은 시트가 담당.
  - prafta-app-014: 슬롯/버튼 판정을 isTwoSlot → slotCount(서버) 로 이관.
      스케줄 미대응 슬롯(slot.schedule==null)은 "스케줄 없음(추가근무)" 표기(D2).
      primary "출근" 버튼은 actions.canCheckIn(서버 effective 산출) 단독 기준(D4).
-->
<template>
  <div v-if="detail" class="cd today-card">
    <!-- 날짜 헤더 -->
    <div class="dr">
      <p class="dm">{{ dateMainText }}</p>
      <p class="dx">{{ dateSubText }}</p>
    </div>

    <!-- 상태 배지 + 근무타입명 -->
    <div class="sl">
      <span class="bd" :class="statusBadgeClass"> <span class="bd__dot" />{{ statusText }} </span>
      <strong>{{ detail.workPlanName }}</strong>
    </div>

    <!-- prafta-app-018-E: 연차 사용 마커 1줄(부분연차 상세). 근무 행은 그대로 유지. -->
    <p v-if="isLeaveUsed" class="lv-marker">{{ leaveMarkerText }}</p>

    <!-- 구간별 3행 정보 -->
    <template v-for="(slot, idx) in slots" :key="slot.workSeq">
      <!--
        prafta-app-014: 구분선은 slotCount>=2 일 때. 라벨은 스케줄 대응 여부에 따라:
          스케줄 대응 슬롯 → "1구간"/"2구간", 스케줄 미대응(추가 출근) → "추가 근무".
      -->
      <div v-if="hasMultiSlot" class="dv">{{ slotDividerLabel(slot, idx) }}</div>
      <div class="tr">
        <!-- 스케줄 (미대응 슬롯이면 "스케줄 없음(추가근무)") -->
        <div class="tw" :class="{ x: !slotHasSchedule(slot) }">
          <div class="tl">
            <svg class="icon" width="13" height="13" aria-hidden="true">
              <use href="#i-attd-cal-ev" />
            </svg>
            스케줄
          </div>
          <div class="tb" :class="{ x: !slotHasSchedule(slot) }">
            <template v-if="slotHasSchedule(slot)">
              <div class="tt2">
                {{ formatRange(slot.schedule.startTime, slot.schedule.endTime) }}
              </div>
              <div class="tm">{{ scheduleMetaText(slot.schedule) }}</div>
            </template>
            <template v-else>
              <div class="tt2">스케줄 없음</div>
              <div class="tm">추가근무 (사후 초과근무 상신 대상)</div>
            </template>
          </div>
        </div>

        <!-- 근태 -->
        <div class="tw a" :class="attendanceRowClass(slot)">
          <div class="tl">
            <svg class="icon" width="13" height="13" aria-hidden="true">
              <use
                :href="
                  slot.attendance && slot.attendance.isMissingCheckOut
                    ? '#i-attd-alert-c'
                    : '#i-attd-finger'
                "
              />
            </svg>
            근태
          </div>
          <div class="tb">
            <div class="tt2" v-html="attendanceTimeHtml(slot)"></div>
            <div v-if="attendanceMetaHtml(slot)" class="tm" v-html="attendanceMetaHtml(slot)"></div>
          </div>
        </div>
      </div>
    </template>

    <!-- 인라인 알림 -->
    <div v-if="alertText" class="al" :class="alertToneClass">
      <svg class="icon" width="14" height="14" aria-hidden="true"><use :href="alertIconId" /></svg>
      <span>{{ alertText }}</span>
    </div>

    <!-- 푸터 액션 -->
    <div class="ft">
      <!--
        prafta-app-013: "수정 요청"은 항상 눌러서 4액션 시트를 연다(결정 §3).
        개별 게이팅은 시트 내부 4행(sheetActions)이 담당하므로 버튼 자체는 항상 활성.
        종전 canRequestModify(서버) 의존을 끊었다.
      -->
      <button type="button" class="bt bt-s" @click="onModify">
        <svg class="icon" width="16" height="16" aria-hidden="true">
          <use href="#i-attd-edit" />
        </svg>
        수정 요청
      </button>

      <!--
        prafta-app-015: 2구간 스케줄이고 출근 가능 구간이 있으면 "1구간 출근"/"2구간 출근" 2버튼.
          각 버튼 enabled = 서버 구간 플래그(slot.canCheckInThisSlot). 이미 등록 구간 disabled(bt-x).
          그 외(1구간/스케줄없음/모든 구간 등록 완료)는 기존 단일 primary 버튼 유지.
      -->
      <template v-if="showSlotCheckInButtons">
        <button
          v-for="slot in checkInSlots"
          :key="`ci-${slot.workSeq}`"
          type="button"
          class="bt"
          :class="slot.canCheckInThisSlot ? 'bt-p' : 'bt-x'"
          :disabled="!slot.canCheckInThisSlot"
          @click="onSlotCheckIn(slot.workSeq)"
        >
          <svg class="icon" width="16" height="16" aria-hidden="true">
            <use href="#i-attd-login" />
          </svg>
          {{ slot.workSeq === 1 ? '1구간 출근' : '2구간 출근' }}
        </button>
      </template>
      <button
        v-else
        type="button"
        class="bt"
        :class="primaryActionEnabled ? 'bt-p' : 'bt-x'"
        :disabled="!primaryActionEnabled"
        @click="onPrimaryAction"
      >
        <svg class="icon" width="16" height="16" aria-hidden="true">
          <use :href="primaryActionIcon" />
        </svg>
        {{ primaryActionLabel }}
      </button>
    </div>

    <!--
      prafta-app-026: 보조 2회차 출근 버튼(별도 줄). 퇴근 재등록(주 버튼)과 동시에 추가 출근도
      가능할 때만 노출. 오탭이 허깨비 출근을 만들지 않도록 주 액션과 분리한다.
    -->
    <div v-if="showSecondaryCheckIn" class="ft ft-sub">
      <button type="button" class="bt bt-s" @click="onSecondaryCheckIn">
        <svg class="icon" width="16" height="16" aria-hidden="true">
          <use href="#i-attd-login" />
        </svg>
        출근하기 (2회차)
      </button>
    </div>

    <!-- 본 카드 전용 sprite -->
    <svg width="0" height="0" class="card-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-attd-cal-ev"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <rect x="4" y="5" width="16" height="16" rx="2" />
          <line x1="16" y1="3" x2="16" y2="7" />
          <line x1="8" y1="3" x2="8" y2="7" />
          <line x1="4" y1="11" x2="20" y2="11" />
          <line x1="8" y1="15" x2="8" y2="15" />
        </symbol>
        <symbol
          id="i-attd-finger"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M18.9 7a8 8 0 0 1 1.1 5v1a6 6 0 0 0 .8 3" />
          <path d="M8 11a4 4 0 0 1 8 0v1a10 10 0 0 0 2 6" />
          <path d="M12 11v2a14 14 0 0 0 2.5 8" />
          <path d="M8 15a18 18 0 0 0 1.8 6" />
          <path d="M4.9 19a22 22 0 0 1-.9-7a8 8 0 0 1 12-7" />
        </symbol>
        <symbol
          id="i-attd-alert-c"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="12" cy="12" r="9" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12" y2="16" />
        </symbol>
        <symbol
          id="i-attd-info"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="12" cy="12" r="9" />
          <line x1="12" y1="8" x2="12" y2="8" />
          <line x1="11" y1="12" x2="12" y2="12" />
          <line x1="12" y1="12" x2="12" y2="16" />
        </symbol>
        <symbol
          id="i-attd-alert-t"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M12 9v4" />
          <path
            d="M10.4 4.6l-8 14a1.7 1.7 0 0 0 1.5 2.4h16.2a1.7 1.7 0 0 0 1.5-2.4l-8-14a1.7 1.7 0 0 0-3 0z"
          />
          <line x1="12" y1="17" x2="12" y2="17" />
        </symbol>
        <symbol
          id="i-attd-edit"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3z" />
          <line x1="13.5" y1="6.5" x2="17.5" y2="10.5" />
        </symbol>
        <symbol
          id="i-attd-logout"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M14 8V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h7a2 2 0 0 0 2-2v-2" />
          <line x1="9" y1="12" x2="21" y2="12" />
          <polyline points="18 9 21 12 18 15" />
        </symbol>
        <symbol
          id="i-attd-login"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M10 8V6a2 2 0 0 1 2-2h7a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-7a2 2 0 0 1-2-2v-2" />
          <line x1="3" y1="12" x2="15" y2="12" />
          <polyline points="6 9 3 12 6 15" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  formatHHMM,
  formatRange,
  formatYmdLong,
  formatDowLong,
  minutesToKorean,
  formatLeaveMarker,
} from '../attdFormat'

const props = defineProps({
  // GET /appApi/attd/my/today (또는 day-detail) 응답 1건. null=로딩/미주입
  detail: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['action'])

// ───────────────────────────────────────────────────────────
// 파생 값 (단순 포맷/표시 — 비즈니스 로직 아님)
// ───────────────────────────────────────────────────────────
const slots = computed(() => (props.detail && props.detail.slots) || [])

// prafta-app-018-E: 부분연차(시간차/반차) 마커 — 근무 행은 그대로 두고 1줄만 부가 표시(근무일 유지).
//   종일연차일도 동일 마커. 연차 미사용일은 isLeaveUsed=false → 미렌더.
const isLeaveUsed = computed(() => !!(props.detail && props.detail.isLeaveUsed))
const leaveMarkerText = computed(() => formatLeaveMarker(props.detail || {}))

// prafta-app-014: 슬롯 개수 판정 단일 출처를 서버 slotCount 로 이관.
//   서버 미제공(구버전 응답) 폴백: slots.length, 그래도 없으면 isTwoSlot.
const slotCount = computed(() => {
  if (props.detail && typeof props.detail.slotCount === 'number') return props.detail.slotCount
  if (slots.value.length) return slots.value.length
  return props.detail && props.detail.isTwoSlot ? 2 : 1
})
const hasMultiSlot = computed(() => slotCount.value >= 2)

// 슬롯이 스케줄 대응(지각·조퇴 적용 대상)인지: schedule 객체+시작시각 존재.
//   서버는 미대응 슬롯의 schedule 을 null 로 내려준다(D2).
const slotHasSchedule = (slot) => !!(slot && slot.schedule && slot.schedule.startTime)

// 구분선 라벨: 스케줄 대응이면 "1구간"/"2구간", 미대응이면 "추가 근무".
const slotDividerLabel = (slot, idx) => {
  if (!slotHasSchedule(slot)) return '추가 근무'
  return idx === 0 ? '1구간' : '2구간'
}

// 날짜 헤더 — "2026년 5월 20일" / "수요일 · 중곡사업장"
const dateMainText = computed(() => formatYmdLong(props.detail && props.detail.workDate))
const dateSubText = computed(() => {
  if (!props.detail) return ''
  const dow = formatDowLong(props.detail.workDate)
  const site = props.detail.siteName || ''
  return [dow, site].filter(Boolean).join(' · ')
})

// 상태 배지
const statusBadgeClass = computed(() => {
  switch (props.detail && props.detail.workStatus) {
    case 'WORKING':
      return 'bd-p'
    case 'TWO_SLOT_WORKING':
      return 'bd-i'
    case 'CHECK_OUT_MISSING':
      return 'bd-w'
    case 'CHECKED_OUT':
    default:
      return 'bd-n'
  }
})
const statusText = computed(() => {
  switch (props.detail && props.detail.workStatus) {
    case 'WORKING':
      return '근무중'
    case 'TWO_SLOT_WORKING':
      // prafta-app-014: slotCount>=2 의 진행 상태(야간2구간 또는 추가 출근) 공통 라벨.
      return '근무중'
    case 'CHECK_OUT_MISSING':
      return '퇴근 미등록'
    case 'CHECKED_OUT':
      return '퇴근'
    case 'BEFORE_WORK':
      return '출근 전'
    default:
      return ''
  }
})

// 스케줄 메타 — "휴게 90분 · 7시간"
const scheduleMetaText = (schedule) => {
  if (!schedule) return ''
  const parts = []
  if (schedule.breakMinutes != null) parts.push(`휴게 ${schedule.breakMinutes}분`)
  if (schedule.workMinutes != null) parts.push(minutesToKorean(schedule.workMinutes))
  return parts.join(' · ')
}

// 근태 행 톤 클래스 (.a 위에 추가): 사업장다름=.wr / 미등록=.m
const attendanceRowClass = (slot) => {
  const a = slot.attendance
  if (!a) return ''
  if (a.isMissingCheckOut) return 'm'
  if (a.isDifferentSite) return 'wr'
  return ''
}

// 근태 시각 HTML — 미등록 시 빨간 "미등록"(.dg) 표시
const attendanceTimeHtml = (slot) => {
  const a = slot.attendance
  // 근태 미생성(미래/2구간 미시작) → '- ~ -'
  if (!a || !a.checkInTime) return '- ~ -'
  const inText = formatHHMM(a.checkInTime) || '-'
  // 퇴근 미등록(보정 대상) → 빨간 "미등록"
  if (a.isMissingCheckOut) return `${inText} ~ <span class="dg">미등록</span>`
  // 근무중(퇴근 시각 없음) → "09:28 ~ -" (시안 화면1)
  const outText = formatHHMM(a.checkOutTime) || '-'
  return `${inText} ~ ${outText}`
}

// 근태 메타 HTML — "출근 {사업장} · 퇴근 {사업장}".
//   prafta-app-003 B-2: GPS 행 존재(checkInOffsite/checkOutOffsite=true) = 근무지 밖(외근).
//   외근이면 "근무지 외(외근)"를 warning 톤(.pw)으로 노출(시안 §3.5 "근무지 외" 태그).
const checkInLabel = (a) => {
  if (a.checkInOffsite === true) return '<span class="pw">출근 근무지 외(외근)</span>'
  return a.checkInSiteName ? `출근 ${a.checkInSiteName}` : ''
}
const attendanceMetaHtml = (slot) => {
  const a = slot.attendance
  if (!a || !a.checkInTime) return ''
  const parts = []
  const inLabel = checkInLabel(a)
  if (inLabel) parts.push(inLabel)
  // 퇴근 미등록이면 퇴근 메타 없음(시안 화면5: "출근 중곡사업장"만)
  if (!a.isMissingCheckOut && a.checkOutTime) {
    let outLabel
    if (a.checkOutOffsite === true) outLabel = '<span class="pw">퇴근 근무지 외(외근)</span>'
    else if (a.checkOutSiteName) {
      // 사업장다름(스키마 한계로 현재 항상 false)이면 퇴근지를 warning 톤(.pw)으로 강조 (시안 §3.5)
      outLabel = a.isDifferentSite
        ? `<span class="pw">퇴근 ${a.checkOutSiteName}</span>`
        : `퇴근 ${a.checkOutSiteName}`
    } else {
      outLabel = ''
    }
    if (outLabel) parts.push(outLabel)
  }
  return parts.join(' · ')
}

// ───────────────────────────────────────────────────────────
// 인라인 알림 (상태별 워딩 — 시안 §3.1 고정 문구)
// ───────────────────────────────────────────────────────────
const alertText = computed(() => {
  switch (props.detail && props.detail.workStatus) {
    case 'WORKING':
      return '근무 중에는 근태 수정을 요청할 수 없어요.'
    case 'TWO_SLOT_WORKING':
      // prafta-app-014: slotCount>=2 진행 중 공통 문구(야간2구간/추가 출근 모두 포함).
      return '오늘 근무가 모두 끝난 뒤에 수정 요청을 등록할 수 있어요.'
    case 'CHECK_OUT_MISSING':
      return '퇴근은 다음 날까지 등록할 수 있어요.'
    case 'CHECKED_OUT':
      // 사업장다름(.wr)이면 안내 문구 없음(시안 §3.1)
      return hasDifferentSite.value ? '' : '출퇴근 기록과 근태가 다르면 수정 요청해 주세요.'
    default:
      return ''
  }
})
const hasDifferentSite = computed(() =>
  slots.value.some((s) => s.attendance && s.attendance.isDifferentSite),
)
const alertToneClass = computed(() =>
  (props.detail && props.detail.workStatus) === 'CHECK_OUT_MISSING' ? 'dg' : 'in',
)
const alertIconId = computed(() =>
  (props.detail && props.detail.workStatus) === 'CHECK_OUT_MISSING'
    ? '#i-attd-alert-t'
    : '#i-attd-info',
)

// ───────────────────────────────────────────────────────────
// 푸터 액션 (서버 산출 actions 표시만)
//   prafta-app-013: "수정 요청"은 항상 활성(시트 오픈). canRequestModify 의존 제거.
//   prafta-app-014: primary 버튼은 actions.canCheckIn/canCheckOut(서버 effective 산출) 단독 기준.
//     상태 문자열(TWO_SLOT_WORKING) 의존을 끊어 1구간 스케줄/스케줄없음의 2번째 출근도 포괄.
// ───────────────────────────────────────────────────────────
const actions = computed(() => (props.detail && props.detail.actions) || {})

// prafta-app-015: 2구간 스케줄이면 구간 선택 버튼(서버 slot.canCheckInThisSlot 게이팅).
//   - isTwoSlot && 퇴근 우선 상태가 아니고(canCheckOut=false) && 출근 가능 구간이 1개 이상이면 2버튼 노출.
//   - 퇴근 우선(진행 중 구간 존재)이거나 1구간/스케줄없음/모든 구간 완료면 단일 primary 버튼.
const isTwoSlotSchedule = computed(() => !!(props.detail && props.detail.isTwoSlot))
const checkInSlots = computed(() =>
  // 표시 일관: 1구간/2구간 순으로 노출(slots 는 workSeq 오름차순으로 가정하나 명시 정렬).
  slots.value
    .filter((s) => s && (s.workSeq === 1 || s.workSeq === 2))
    .slice()
    .sort((a, b) => a.workSeq - b.workSeq),
)
const showSlotCheckInButtons = computed(
  () =>
    isTwoSlotSchedule.value &&
    !actions.value.canCheckOut &&
    checkInSlots.value.some((s) => s.canCheckInThisSlot === true),
)

// prafta-app-026: 주 버튼 우선순위 역전 — canCheckOut 우선 > canCheckIn.
//   - 진행 중 슬롯(WORKING/TWO_SLOT_WORKING/CHECK_OUT_MISSING) → "퇴근하기".
//   - 퇴근완료(CHECKED_OUT) && canCheckOut → "퇴근 시간 재등록"(오탭 보정·last-write-wins).
//   - canCheckOut=false && canCheckIn → "출근하기"(최초 출근).
//   재량 2회차 출근(canCheckOut && canCheckIn)은 보조 버튼으로 분리(아래 secondaryCheckIn*).
const canCheckOut = computed(() => !!actions.value.canCheckOut)
const canCheckIn = computed(() => !!actions.value.canCheckIn)
// 주 액션이 퇴근인지(출근인지) 판정: 퇴근 가능하면 퇴근 우선, 아니면 출근.
const isPrimaryCheckOut = computed(() => canCheckOut.value)
const primaryActionEnabled = computed(() =>
  isPrimaryCheckOut.value ? canCheckOut.value : canCheckIn.value,
)
const primaryActionLabel = computed(() => {
  if (!isPrimaryCheckOut.value) return '출근하기'
  // 퇴근완료 상태에서의 퇴근 가능 = 재등록(오탭 보정), 그 외(진행 중)는 일반 퇴근.
  return (props.detail && props.detail.workStatus) === 'CHECKED_OUT' ? '퇴근 시간 재등록' : '퇴근하기'
})
const primaryActionIcon = computed(() =>
  isPrimaryCheckOut.value ? '#i-attd-logout' : '#i-attd-login',
)

// prafta-app-026: 보조 2회차 출근 버튼 노출 조건 — 퇴근 재등록(주 버튼)과 동시에 추가 출근도 가능할 때.
//   showSlotCheckInButtons(진짜 2구간 명시 출근)와 배타: 그쪽이 노출되면 보조 버튼은 숨긴다.
const showSecondaryCheckIn = computed(
  () => canCheckOut.value && canCheckIn.value && !showSlotCheckInButtons.value,
)

const onModify = () => {
  // prafta-app-013: 4액션 시트를 열도록 컨테이너에 위임.
  emit('action', { type: 'requestModify', detail: props.detail })
}
const onPrimaryAction = () => {
  // prafta-app-026: 주 버튼 경로 — 퇴근 우선. targetWorkSeq 미동봉(서버 채번/재퇴근 tail 슬롯 선정).
  emit('action', {
    type: isPrimaryCheckOut.value ? 'checkOut' : 'checkIn',
    detail: props.detail,
  })
}
// prafta-app-026: 보조 2회차 출근 — 단일 출근(구간 미지정). targetWorkSeq 미동봉(서버 existing+1 채번).
const onSecondaryCheckIn = () => {
  emit('action', {
    type: 'checkIn',
    detail: props.detail,
  })
}
// prafta-app-015: 2구간 구간 선택 출근 — 선택 구간(workSeq)을 targetWorkSeq 로 동봉하여 emit.
const onSlotCheckIn = (workSeq) => {
  emit('action', {
    type: 'checkIn',
    detail: props.detail,
    targetWorkSeq: workSeq,
  })
}
</script>

<style scoped>
.cd {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 14px;
}

.dr {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin: 0 0 var(--space-sm);
  flex-wrap: wrap;
}
.dm {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.dx {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.sl {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.sl strong {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-primary);
}

/* prafta-app-018-E: 부분연차 마커 라인(warning 톤 — 주간 연차 배지/캘린더 lv 셀과 톤 일관). */
.lv-marker {
  margin: 2px 0 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-warning-text);
}

/* 상태 배지 */
.bd {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  height: 22px;
  padding: 0 var(--space-sm);
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}
.bd__dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
}
.bd-p {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.bd-p .bd__dot {
  background: var(--color-primary);
}
.bd-n {
  background: var(--color-border-light);
  color: var(--color-text-secondary);
}
.bd-n .bd__dot {
  background: var(--color-text-secondary);
}
.bd-w {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.bd-w .bd__dot {
  background: var(--color-warning);
}
.bd-i {
  background: var(--color-info-tint);
  color: var(--color-info);
}
.bd-i .bd__dot {
  background: var(--color-info);
}

/* 구분선 (slotCount>=2) */
.dv {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: var(--space-sm) 0 var(--space-xs);
  font-size: 10px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.dv::before,
.dv::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--color-border-light);
}

/* 3행 정보 */
.tr {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.tw {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: var(--space-sm);
  align-items: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
}
/* 스케줄 미대응 슬롯의 스케줄 행 (흐림) */
.tw.x {
  border-color: var(--color-border);
  background: var(--color-border-light);
}
.tw.x .tl {
  color: var(--color-text-tertiary);
}
.tw.a {
  border-color: var(--color-primary-tint-border);
  background: var(--color-primary-tint);
}
.tw.a.m {
  border-color: var(--color-danger-border);
  background: var(--color-danger-tint);
}
.tw.a.wr {
  border-color: var(--color-warning-border);
  background: var(--color-warning-tint);
}

.tl {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.tw.a .tl {
  color: var(--color-primary-text-deep);
}
.tw.a.m .tl {
  color: var(--color-danger);
}
.tw.a.wr .tl {
  color: var(--color-warning-text);
}

.tb {
  min-width: 0;
}
.tt2 {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.3;
  font-variant-numeric: tabular-nums;
}
.tb.x .tt2 {
  color: var(--color-text-tertiary);
  font-weight: 500;
}
.tm {
  margin-top: 1px;
  font-size: 11px;
  color: var(--color-text-secondary);
}

/* deep selector — v-html 내부 span 색상 (서버/포맷 문자열의 .dg/.pw/.mu) */
.tt2 :deep(.dg) {
  color: var(--color-danger);
}
.tt2 :deep(.mu) {
  color: var(--color-text-tertiary);
  font-weight: 500;
}
.tm :deep(.pw) {
  color: var(--color-warning-text);
  font-weight: 600;
}

/* 인라인 알림 */
.al {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: var(--space-sm);
  padding: var(--space-sm) 10px;
  border-radius: var(--radius-sm);
  font-size: 11px;
  line-height: 1.4;
  background: var(--color-warning-tint);
  border: 1px solid var(--color-warning-border-light);
  color: var(--color-warning-text);
}
.al .icon {
  margin-top: 1px;
}
.al.dg {
  background: var(--color-danger-tint);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}
.al.in {
  background: var(--color-info-tint);
  border-color: var(--color-info-border);
  color: var(--color-info-strong);
}

/* 푸터 */
.ft {
  display: flex;
  gap: var(--space-sm);
  margin-top: var(--space-md);
}
/* prafta-app-026: 보조 2회차 출근 줄 — 주 버튼 바로 아래 좁은 간격으로 분리. */
.ft-sub {
  margin-top: var(--space-sm);
}
.bt {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  white-space: nowrap;
  font-family: inherit;
}
.bt-p {
  background: var(--color-primary);
  color: var(--color-surface);
}
.bt-s {
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1.5px solid var(--color-primary);
}
.bt-x {
  background: var(--color-border-light);
  color: var(--color-text-tertiary);
  border: 1px solid var(--color-border);
  cursor: not-allowed;
}

.card-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
