<!--
  MyLeaveSummaryView.vue — 연차 현황 (본인 잔여연차 상세, 모바일 앱) 상위 컨테이너
  - 작업: prafta-app-005 슬롯 B (분해: .claude/requests/app_requests/prafta-app-005-plan.md)
  - UI 명세: UI-A005 (.claude/requests/app_requests/prafta-app-005-ui-spec.md)
  - 시안: refs/prafta-app-005/prafta_my_leave_v1.html (4케이스)
  - planner 라운드 스코프: 헤더 + 그룹토글 + 콜아웃 + 메인카드 + 3분할 + 메타 + 푸터 조립 (template/style)
  - developer 라운드 스코프(아래 TODO): API 호출(leave01/my-leave-summary), 라우팅, 로딩/에러, 진입연동
  - 디자인 토큰: MyAttendanceView(.my-attd-view)와 동일 세트를 .my-leave-view 루트에 1회 선언.
    자식 컴포넌트(scoped)는 var(--...) 상속받아 사용. 하드코딩/Tailwind 금지.
-->
<template>
  <div class="my-leave-view">
    <!-- 헤더 -->
    <header class="lv-hd">
      <button type="button" class="lv-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-lv-chev-left" />
        </svg>
      </button>
      <h1 class="lv-hd__title">연차 현황</h1>
      <span class="lv-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 (스크롤 영역) -->
    <main
      class="lv-body"
      ref="scrollRef"
      @touchstart.passive="onPullStart"
      @touchmove="onPullMove"
      @touchend="onPullEnd"
      @touchcancel="onPullEnd"
    >
      <!-- 당겨서 새로고침 인디케이터 — 스크롤 최상단에서 아래로 당기면 노출 -->
      <PullRefreshIndicator v-bind="indicatorProps" />

      <!-- 로딩 -->
      <div v-if="isLoading" class="lv-loading" aria-live="polite">불러오는 중...</div>

      <template v-else>
        <!-- 그룹 토글 (전체 / 법정 / 법정 외) -->
        <LeaveGroupToggle v-model="activeGroup" />

        <!-- 소멸 임박 콜아웃 (전체 토글 + 노출 조건 충족 + 미닫힘)
             2026-08-09 규약: 일 단위 단독 표기 — convMinutes prop 소멸 -->
        <LeaveExpiryCallout v-if="showCallout" :info="expiringSoon" @close="onCalloutClose" />

        <!-- 메인 잔여 카드 (convMinutes 는 WithHourly dayPart 캐리 방어용으로만 잔존) -->
        <LeaveBalanceCard
          :label="balanceLabel"
          :group="currentGroup"
          :conv-minutes="convMinutes"
          :hourly-used-minutes="cellHourlyUsedMinutes"
          :hourly-planned-minutes="cellHourlyPlannedMinutes"
          :half-day-used-days="cellHalfDayUsedDays"
          :half-day-planned-days="cellHalfDayPlannedDays"
        />

        <!-- 3분할 KPI (부여 / 사용 / 사용예정) -->
        <LeaveSplitKpi
          :group="currentGroup"
          :conv-minutes="convMinutes"
          :hourly-used-minutes="cellHourlyUsedMinutes"
          :hourly-planned-minutes="cellHourlyPlannedMinutes"
          :half-day-used-days="cellHalfDayUsedDays"
          :half-day-planned-days="cellHalfDayPlannedDays"
        />

        <!-- LC-11: 시간차 사용분 원본(분) 병기 — 차감 일수 합계와 별개인 서버 합계값 그대로 표시.
             HB-13(F-3): 전체 토글에서는 사용/사용예정 셀이 실분을 직접 표기하므로 중복 노출을 피한다
             (시간차 실분은 그룹 구분이 없어 법정/법정 외 토글에서만 전체 합계로 안내). -->
        <p v-if="showHourlyNote" class="lv-hourly-note">시간차 사용 {{ hourlyUsedText }} 포함</p>

        <!-- 메타 카드 (입사일 / 근속 / 사용률) -->
        <LeaveMetaCard :user="user" :usage-rate="currentUsageRate" />

        <!-- 신청형 휴가 (LEAVE_TYPE='01') — 법정/관리자부여 그룹과 분리된 별도 섹션. 항목 1개 이상일 때만 노출.
             2026-08-09 규약: 일 단위 단독 표기 — convMinutes prop 소멸 -->
        <LeaveAppliedCard v-if="hasAppliedLeave" :types="appliedLeaveTypes" />

        <!-- 가불 사용분 (prafta-com-011-5) — 미상계 가불(borrowedDays>0)일 때만 노출. MVP 표시 전용(액션 없음).
             2026-08-09 규약: 날짜 미정 잔여류 표기 → 일 단위 단독(E4 환산 제거) -->
        <section v-if="hasBorrowed" class="lv-borrow">
          <div class="lv-borrow__row">
            <span class="lv-borrow__lbl">가불 사용</span>
            <span class="lv-borrow__val">{{ formatLeaveDaysOnly(borrowedDays) }}</span>
          </div>
          <p class="lv-borrow__note">미래 연차에서 상계 예정입니다.</p>
        </section>

        <!-- 빈 상태 (로드 완료했으나 데이터 없음) -->
        <p v-if="showEmptyState" class="lv-empty">표시할 연차 정보가 없어요</p>

        <!-- 사용한 연차 리스트 (연 단위 조회) -->
        <section class="lv-used">
          <div class="lv-used__hd">
            <h2 class="lv-used__title">사용한 연차</h2>
            <div class="lv-used__year" role="group" aria-label="조회 연도 선택">
              <button
                type="button"
                class="lv-used__year-btn"
                aria-label="이전 연도"
                :disabled="usedLoading"
                @click="onUsedYearMove(-1)"
              >
                <svg class="icon" width="16" height="16" aria-hidden="true">
                  <use href="#i-lv-chev-left" />
                </svg>
              </button>
              <span class="lv-used__year-lbl">{{ usedYear }}년</span>
              <button
                type="button"
                class="lv-used__year-btn"
                aria-label="다음 연도"
                :disabled="usedLoading"
                @click="onUsedYearMove(1)"
              >
                <svg class="icon lv-used__chev-right" width="16" height="16" aria-hidden="true">
                  <use href="#i-lv-chev-left" />
                </svg>
              </button>
            </div>
          </div>

          <div v-if="usedLoading" class="lv-used__loading" aria-live="polite">불러오는 중...</div>
          <p v-else-if="usedList.length === 0" class="lv-used__empty">
            {{ usedYear }}년에 사용한 연차가 없어요
          </p>
          <ul v-else class="lv-used__list">
            <li v-for="row in usedList" :key="row.leaveId" class="lv-used__row">
              <div class="lv-used__row-main">
                <span class="lv-used__date">{{ usedDateText(row) }}</span>
                <span class="lv-used__name">
                  {{ row.leaveNm || '연차' }}
                  <span class="lv-used__unit">{{ usedUnitLabel(row.useUnitType) }}</span>
                  <span v-if="isFutureUse(row)" class="lv-used__badge">예정</span>
                </span>
              </div>
              <span class="lv-used__amount">{{ usedAmountText(row) }}</span>
            </li>
          </ul>
        </section>
      </template>
    </main>

    <!-- 푸터 액션 -->
    <footer class="lv-footer">
      <button
        type="button"
        class="lv-apply"
        :class="{ 'lv-apply--off': !canApply }"
        :disabled="!canApply"
        @click="onApply"
      >
        <svg class="icon" width="18" height="18" aria-hidden="true">
          <use href="#i-lv-plus" />
        </svg>
        연차 신청하기
      </button>
    </footer>

    <!-- 2026-08-09 규약: 잔여 시간 근사치 안내 시트(HB-13 F-3) 제거 —
         안내 대상이던 E4 시간 환산 표기 자체가 소멸(일 단위 단독 표기 전환). -->

    <!-- 인라인 SVG sprite (본 화면 전용) -->
    <svg width="0" height="0" class="lv-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-lv-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol
          id="i-lv-plus"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { formatLeaveDaysOnly, formatMinutesToHm } from '@/utils/leaveFormat'
import { usePullToRefresh } from '@/composables/usePullToRefresh'
import PullRefreshIndicator from '@/components/common/PullRefreshIndicator.vue'

import LeaveGroupToggle from './components/LeaveGroupToggle.vue'
import LeaveExpiryCallout from './components/LeaveExpiryCallout.vue'
import LeaveBalanceCard from './components/LeaveBalanceCard.vue'
import LeaveSplitKpi from './components/LeaveSplitKpi.vue'
import LeaveMetaCard from './components/LeaveMetaCard.vue'
import LeaveAppliedCard from './components/LeaveAppliedCard.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백 (앱 전역 $alert 우선, 없으면 window.alert) — MyAttendanceView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ───────────────────────────────────────────────────────────
// 상태 (developer: 초기값/리셋/응답 주입 보완 필요)
// ───────────────────────────────────────────────────────────
const isLoading = ref(true)

// API 응답 (GET /appApi/leave01/my-leave-summary)
//   user: { userNm, hireDate(YYYYMMDD), serviceMonths, serviceCreditMonths }
//   groups: { TOTAL, STATUTORY, NON_STATUTORY } 각 { granted, used, planned, remaining, usageRate }
//   expiringSoon: { exists, daysUntilExpiry, totalRemainingDays, expiryDate }
const user = ref(null)
const groups = ref(null)
const expiringSoon = ref(null)
// 신청형 휴가(LEAVE_TYPE='01') 타입별 항목 — 법정/관리자부여(groups)와 분리. [{leaveCd,leaveNm,maxAplyDays,usedDays,remainDays}]
const appliedLeaveTypes = ref([])
// 미상계 가불 사용 합계(일) — prafta-com-011-5. 0이면 카드 숨김(MVP 표시 전용).
const borrowedDays = ref(0)
// 1일 환산시간(분, 서버 권위 — 오늘 기준 E4 참고 분모). 미제공 시 480 폴백.
//   2026-08-09 규약: 잔여/부여 표기가 일 단위 단독으로 전환되어 시간 환산 분모로는 더 이상 쓰지 않는다
//   — WithHourly 계열의 dayPart 캐리 방어(decompose) 인자로만 잔존(BalanceCard/SplitKpi 전달).
const convMinutes = ref(480)
// LC-11: 시간차(02/03/04) CONFIRMED 사용 분 합계(전 기간) — 원본(분) 병기용. 0이면 미노출.
const hourlyUsedMinutes = ref(0)
// HB-13(F-3): 위 합계를 사용/사용예정으로 분리한 서버 실분(START_DATE <= 오늘 / > 오늘).
//   사용/사용예정 표기를 일수→시간 역환산이 아니라 실분으로 내기 위한 값(구 응답이면 0 폴백).
const hourlyUsedMinutesPast = ref(0)
const hourlyUsedMinutesPlanned = ref(0)
// HB-13 §20-2(B안): 반차 사용/사용예정 "일수"(건수 아님 — 분할차감 대응, 서버 SUM 값).
//   정수부만 쓰는 표기에서 반차 0.5일이 증발하던 문제를 없애기 위한 값(구 응답이면 0 폴백).
const halfDayUsedDaysPast = ref(0)
const halfDayUsedDaysPlanned = ref(0)

// 그룹 토글 (UI 상태 — 허용 범위). 진입 기본값: 전체
const activeGroup = ref('TOTAL')

// 사용한 연차 리스트(연 단위) — GET /appApi/leave01/my-leave-uses?year=YYYY
//   usedYear 초기값은 클라이언트 올해로 표기하되, 첫 응답의 서버 보정 연도(year)로 동기화한다.
const usedYear = ref(new Date().getFullYear())
const usedList = ref([])
const usedLoading = ref(true)

// 콜아웃 세션 한정 닫힘 상태 (재진입 시 재노출 — §3.3)
const calloutDismissed = ref(false)

// ───────────────────────────────────────────────────────────
// 파생 값 (단순 표시 — 비즈니스 로직 아님)
// ───────────────────────────────────────────────────────────
const currentGroup = computed(() => (groups.value ? groups.value[activeGroup.value] : null))

const balanceLabel = computed(() => {
  if (activeGroup.value === 'STATUTORY') return '법정 잔여 일수'
  if (activeGroup.value === 'NON_STATUTORY') return '법정 외 잔여 일수'
  return '잔여 일수'
})

// 사용률 — 결정질문 Q5 확정에 따름(현재 토글 그룹 기준 권장). 응답 usageRate 사용.
const currentUsageRate = computed(() => currentGroup.value?.usageRate ?? 0)

// 소멸 임박 콜아웃: 전체 토글 + exists + 미닫힘 (§3.4)
const showCallout = computed(
  () => activeGroup.value === 'TOTAL' && !!expiringSoon.value?.exists && !calloutDismissed.value,
)

// 푸터 활성: 현재 토글 그룹 잔여 > 0 (§3.5)
//   ⚠️ Q7: 진입/신청 라우팅 미확정. 활성 판정은 잔여 기준만.
const canApply = computed(() => (currentGroup.value?.remaining ?? 0) > 0)

// 신청형 휴가 섹션 노출: 항목 1개 이상(서버 권위값 그대로 표시, 재계산 없음).
const hasAppliedLeave = computed(
  () => Array.isArray(appliedLeaveTypes.value) && appliedLeaveTypes.value.length > 0,
)

// 빈 상태: 로드 완료 + 부여 데이터 전무
const showEmptyState = computed(() => !isLoading.value && !groups.value)

// 가불 사용분 카드 노출: 미상계 가불 > 0 (prafta-com-011-5).
const hasBorrowed = computed(() => Number(borrowedDays.value) > 0)

// LC-11: 시간차 사용분 원본 표기("1시간 30분") — 서버 합계값 그대로, 재계산 없음.
const hourlyUsedText = computed(() => formatMinutesToHm(hourlyUsedMinutes.value))

// HB-13(F-3): 시간차 실분·반차 일수는 법정/법정 외 구분이 없는 전체 합계다. 그룹 토글이 전체가 아닐 때
//   그 셀에 붙이면 그룹 수치와 어긋나므로, 전체(TOTAL) 토글에서만 셀 병기에 사용한다(§20-2 도 동일 스코프).
const isTotalGroup = computed(() => activeGroup.value === 'TOTAL')
const cellHourlyUsedMinutes = computed(() => (isTotalGroup.value ? hourlyUsedMinutesPast.value : 0))
const cellHourlyPlannedMinutes = computed(() =>
  isTotalGroup.value ? hourlyUsedMinutesPlanned.value : 0,
)
const cellHalfDayUsedDays = computed(() => (isTotalGroup.value ? halfDayUsedDaysPast.value : 0))
const cellHalfDayPlannedDays = computed(() =>
  isTotalGroup.value ? halfDayUsedDaysPlanned.value : 0,
)
// 하단 병기 노트: 전체 토글에서는 셀이 실분을 직접 표기하므로 중복 노출하지 않는다.
const showHourlyNote = computed(() => hourlyUsedMinutes.value > 0 && !isTotalGroup.value)

// ───────────────────────────────────────────────────────────
// 이벤트 핸들러
// ───────────────────────────────────────────────────────────
const onBack = () => {
  // 메인 홈 복귀 (MyAttendanceView onBack 패턴 동일)
  router.push('/MainView')
}

const onCalloutClose = () => {
  // 세션 한정(메모리) 닫기 — 서버 호출 없음. 화면 재진입(재마운트) 시 ref 초기화로 재노출(§3.3 D-Q8).
  calloutDismissed.value = true
}

const onApply = () => {
  // prafta-app-018-C: 연차 신청 폼. 특정 일자 컨텍스트 없음 → 폼에서 날짜 직접 선택.
  //   (잔여 0이면 버튼 disabled 로 진입 차단 — canApply 가드 현행 유지.)
  router.push('/LeaveApply')
}

// ───────────────────────────────────────────────────────────
// 조회 (캐시 없음 — §3.6). 401/403/500 은 axios 인터셉터가 처리, 그 외만 안내.
//   showLoading=true: 최초 진입(전체 로딩 표시). false: 당겨서 새로고침(본문 유지).
// ───────────────────────────────────────────────────────────
const loadSummary = async ({ showLoading = true } = {}) => {
  if (showLoading) isLoading.value = true
  try {
    const res = await api.get('/appApi/leave01/my-leave-summary')
    user.value = res?.data?.user ?? null
    groups.value = res?.data?.groups ?? null
    expiringSoon.value = res?.data?.expiringSoon ?? null
    appliedLeaveTypes.value = Array.isArray(res?.data?.appliedLeaveTypes)
      ? res.data.appliedLeaveTypes
      : []
    borrowedDays.value = Number(res?.data?.borrowedDays) || 0
    // LC-11: 표기 분모/시간차 사용분 — 서버 미제공(구버전 응답) 시 480/0 폴백.
    convMinutes.value = Number(res?.data?.convMinutes) > 0 ? Number(res.data.convMinutes) : 480
    hourlyUsedMinutes.value = Number(res?.data?.hourlyUsedMinutes) || 0
    // HB-13(F-3): 사용/사용예정 실분(서버 additive 필드). 구 응답이면 0 → 기존 일수 표기로 폴백.
    hourlyUsedMinutesPast.value = Number(res?.data?.hourlyUsedMinutesPast) || 0
    hourlyUsedMinutesPlanned.value = Number(res?.data?.hourlyUsedMinutesPlanned) || 0
    // HB-13 §20-2: 반차 사용/사용예정 일수(서버 additive 필드). 구 응답이면 0 → 반차 항목 미표기.
    halfDayUsedDaysPast.value = Number(res?.data?.halfDayUsedDaysPast) || 0
    halfDayUsedDaysPlanned.value = Number(res?.data?.halfDayUsedDaysPlanned) || 0
  } catch (e) {
    console.error('[MyLeaveSummary] 연차 현황 조회 실패:', e?.message)
    showAlert('연차 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    if (showLoading) isLoading.value = false
  }
}

// ───────────────────────────────────────────────────────────
// 사용한 연차 리스트(연 단위) — 표시 전용, 서버 권위값 그대로.
// ───────────────────────────────────────────────────────────
const loadUses = async (year) => {
  usedLoading.value = true
  try {
    const res = await api.get('/appApi/leave01/my-leave-uses', {
      params: year ? { year } : {},
    })
    usedList.value = Array.isArray(res?.data?.list) ? res.data.list : []
    // 서버 보정 연도(미지정 요청 시 올해)로 표기를 동기화한다.
    const serverYear = Number(res?.data?.year)
    if (serverYear > 0) usedYear.value = serverYear
  } catch (e) {
    console.error('[MyLeaveSummary] 연차 사용 내역 조회 실패:', e?.message)
    usedList.value = []
  } finally {
    usedLoading.value = false
  }
}

const onUsedYearMove = (delta) => {
  const next = usedYear.value + delta
  usedYear.value = next
  loadUses(String(next))
}

// SYS025 사용 단위 라벨(표시 전용).
const USED_UNIT_LABELS = {
  '00': '종일',
  '01': '반차',
  '02': '시간차',
  '03': '시간차',
  '04': '시간차',
  '05': '반반차',
}
const usedUnitLabel = (unitType) => USED_UNIT_LABELS[unitType] ?? ''

// 시간차(02/03/04) 여부 — 분 표기 우선 판정.
const isHourlyUse = (row) => ['02', '03', '04'].includes(row?.useUnitType)

// YYYYMMDD → M.D 표기. 기간이면 "M.D ~ M.D", 시간차면 "M.D HH:MM~HH:MM".
const fmtMd = (ymd) => {
  const s = String(ymd ?? '')
  if (s.length !== 8) return '-'
  return `${Number(s.slice(4, 6))}.${Number(s.slice(6, 8))}`
}
const fmtHm = (hhmm) => {
  const s = String(hhmm ?? '')
  return s.length === 4 ? `${s.slice(0, 2)}:${s.slice(2, 4)}` : ''
}
const usedDateText = (row) => {
  const start = fmtMd(row.startDate)
  if (isHourlyUse(row) && row.startTime && row.endTime) {
    return `${start} ${fmtHm(row.startTime)}~${fmtHm(row.endTime)}`
  }
  if (row.endDate && row.endDate !== row.startDate) {
    return `${start} ~ ${fmtMd(row.endDate)}`
  }
  return start
}

// 차감량 표기 — 시간차는 원본(분, 실사용 정확값 — 유지), 그 외는 일 단위 단독(2026-08-09 규약).
//   구 표기는 E4 참고 분모(convMinutes)로 일→시간 환산해 반차가 "3시간 30분" 등으로 보였음 — 제거.
const usedAmountText = (row) => {
  if (isHourlyUse(row) && Number(row.leaveMinutes) > 0) {
    return formatMinutesToHm(Number(row.leaveMinutes))
  }
  return formatLeaveDaysOnly(Number(row.leaveDays) || 0)
}

// 미래 시작일(사용예정 확정분) 뱃지 판정 — 표시 전용이라 클라이언트 오늘 기준으로 충분.
const clientTodayYmd = () => {
  const d = new Date()
  return `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}`
}
const isFutureUse = (row) => String(row?.startDate ?? '') > clientTodayYmd()

// 당겨서 새로고침 — 본문 유지하고 연차 현황 + 사용 내역(현재 연도)을 재조회(부작용 없는 조회).
const scrollRef = ref(null)
const { onPullStart, onPullMove, onPullEnd, indicatorProps } = usePullToRefresh(scrollRef, async () => {
  await Promise.all([loadSummary({ showLoading: false }), loadUses(String(usedYear.value))])
})

onMounted(() => {
  loadSummary()
  // 최초 진입: year 미지정 → 서버가 올해로 보정(응답 year 로 표기 동기화).
  loadUses()
})
</script>

<style scoped>
/*
 * 디자인 토큰 — MyAttendanceView(.my-attd-view)와 동일 세트를 본 화면 루트에 선언.
 * 자식 컴포넌트(scoped)는 var(--...) 를 상속받아 사용한다. 하드코딩/Tailwind 금지.
 */
.my-leave-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-primary-text-deep: #15803d;
  --color-danger: #ef4444;
  --color-warning: #f59e0b;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-warning-border: #fcd9a4;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-full: 9999px;
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.06);
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  position: relative;
  height: 100vh;
  height: 100dvh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.lv-hd {
  height: 56px;
  flex-shrink: 0;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  border-bottom: 1px solid var(--color-border-light);
}
.lv-hd__back {
  width: 44px;
  height: 44px;
  margin-left: -10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.lv-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.lv-hd__spacer {
  width: 44px;
}

/* 본문 */
.lv-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: var(--space-lg) var(--space-lg) 88px;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.lv-loading {
  padding: 48px var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.lv-empty {
  margin: 0;
  padding: 48px var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* LC-11: 시간차 사용분 원본(분) 병기 — KPI 아래 보조 안내(표시 전용). */
.lv-hourly-note {
  margin: calc(-1 * var(--space-sm)) 0 0;
  padding: 0 var(--space-xs);
  font-size: 12px;
  color: var(--color-text-tertiary);
  font-variant-numeric: tabular-nums;
}

/* 가불 사용분 카드 (prafta-com-011-5) — 표시 전용. 메타카드/경고 톤 재사용(CSS 변수만). */
.lv-borrow {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  padding: var(--space-md);
  background: var(--color-warning-tint);
  border: 1px solid var(--color-warning-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}
.lv-borrow__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.lv-borrow__lbl {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-warning-text);
}
.lv-borrow__val {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-warning-text);
  font-variant-numeric: tabular-nums;
}
.lv-borrow__note {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* 사용한 연차 리스트 (연 단위 조회) — 카드 톤은 메타카드와 동일(CSS 변수만). */
.lv-used {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  padding: var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}
.lv-used__hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.lv-used__title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.lv-used__year {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
}
.lv-used__year-btn {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.lv-used__year-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.lv-used__chev-right {
  transform: rotate(180deg);
}
.lv-used__year-lbl {
  min-width: 56px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.lv-used__loading,
.lv-used__empty {
  margin: 0;
  padding: var(--space-lg) 0;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.lv-used__list {
  margin: 0;
  padding: 0;
  list-style: none;
}
.lv-used__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  padding: 10px 0;
  border-bottom: 1px solid var(--color-border-light);
}
.lv-used__row:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}
.lv-used__row-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.lv-used__date {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}
.lv-used__name {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: 12px;
  color: var(--color-text-secondary);
}
.lv-used__unit {
  color: var(--color-text-tertiary);
}
.lv-used__badge {
  padding: 1px 6px;
  background: var(--color-primary-tint);
  border: 1px solid var(--color-primary-tint-border);
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
  color: var(--color-primary-text-deep);
}
.lv-used__amount {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
}

/* 푸터 */
.lv-footer {
  flex-shrink: 0;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border-light);
  padding: var(--space-md) var(--space-lg);
}
.lv-apply {
  width: 100%;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.lv-apply--off {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}

.lv-sprite {
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
