<!--
  MyAttendanceView.vue — 내 근태 조회 (모바일 앱) 상위 컨테이너
  - 작업 ID: APP002-05 (분해: .claude/requests/app_requests/prafta-app-002-plan.md)
  - UI 명세: UI-A001 (.claude/requests/app_requests/prafta-app-002-ui-spec.md)
  - 시안: refs/prafta-app-002/prafta_my_attendance_v8.html
  - planner 라운드 스코프: 헤더 + 3등분 세그먼트 + 본문 분기 + 탭바 조립 (template/style 완성)
  - developer 라운드 스코프(아래 TODO): API 호출(01~04), 라우팅, 캐시, 케이스 분기 실데이터
  - 디자인 토큰: MainView(.home-view)와 동일 세트를 .my-attd-view 루트에 1회 선언.
    자식 컴포넌트(scoped)는 var(--...) 상속받아 사용.
-->
<template>
  <div class="my-attd-view">
    <!-- 헤더 -->
    <header class="attd-hd">
      <button type="button" class="attd-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-attd-chev-left" />
        </svg>
      </button>
      <h1 class="attd-hd__title">내 근태</h1>
      <button type="button" class="attd-hd__bell" aria-label="알림" @click="onBell">
        <svg class="icon" width="20" height="20" aria-hidden="true">
          <use href="#i-attd-bell" />
        </svg>
        <span v-if="notificationCount > 0" class="attd-hd__badge">{{ notificationCount }}</span>
      </button>
    </header>

    <!-- 세그먼트 (오늘/이번주/이번달) -->
    <div class="attd-seg" role="tablist" aria-label="근태 조회 기간">
      <button
        v-for="seg in segments"
        :key="seg.key"
        type="button"
        role="tab"
        class="attd-seg__item"
        :class="{ 'attd-seg__item--on': activeTab === seg.key }"
        :aria-selected="activeTab === seg.key"
        @click="onSelectTab(seg.key)"
      >
        {{ seg.label }}
      </button>
    </div>

    <!-- 본문 (스크롤 영역) -->
    <main
      class="attd-body"
      ref="scrollRef"
      @touchstart.passive="onPullStart"
      @touchmove="onPullMove"
      @touchend="onPullEnd"
      @touchcancel="onPullEnd"
    >
      <!-- 당겨서 새로고침 인디케이터 — 스크롤 최상단에서 아래로 당기면 노출 -->
      <PullRefreshIndicator v-bind="indicatorProps" />

      <!-- 오늘 탭 -->
      <AttendanceTodayCard
        v-if="activeTab === 'today'"
        :detail="todayDetail"
        @action="onTodayAction"
      />

      <!-- 이번주 탭 -->
      <AttendanceWeekList
        v-else-if="activeTab === 'week'"
        :week="weekData"
        @prev-week="onPrevWeek"
        @next-week="onNextWeek"
        @select-day="onSelectWeekDay"
      />

      <!-- 이번달 탭 -->
      <template v-else>
        <AttendanceMonthCalendar
          :month="monthData"
          :selected-ymd="selectedYmd"
          @prev-month="onPrevMonth"
          @next-month="onNextMonth"
          @select-month="onSelectMonth"
          @select-date="onSelectDate"
        />
        <AttendanceDayDetailCard :detail="dayDetail" @action="onDayDetailAction" />
      </template>

      <!-- 표준 빈 상태 (로드 완료했으나 표시할 근태가 없을 때) -->
      <p v-if="showEmptyState" class="attd-empty">표시할 근태가 없어요</p>
    </main>

    <!-- 하단 탭바 (근태 활성) — prafta-app-025 J1-2: 자체 4탭 nav 를 공통 AppBottomTabBar(5탭)로 교체.
         '마이' 탭 누락 + 안전/TBM '준비중' 문제가 동시 해소됨(라우팅은 컴포넌트가 중앙화 처리). -->
    <AppBottomTabBar :active-tab="'attd'" />

    <!-- 이번주 카드 탭 시 바텀시트 -->
    <AttendanceActionSheet
      v-model="actionSheetOpen"
      :day="actionSheetDay"
      @action="onSheetAction"
    />

    <!-- 외근(지오펜스 밖) 사유 시트 — 서버 ATTD_400_086 수신 시 오픈 (prafta-app-008) -->
    <OffsiteReasonSheet
      v-model="offsiteSheetOpen"
      :mode="offsiteMode"
      :lat="offsiteCtx.lat"
      :lon="offsiteCtx.lon"
      :accuracy="offsiteCtx.accuracy"
      @submit="onOffsiteSubmit"
      @cancel="onOffsiteCancel"
    />

    <!-- 인라인 SVG sprite (본 화면 전용) -->
    <svg width="0" height="0" class="attd-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-attd-chev-left"
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
          id="i-attd-bell"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path
            d="M10 5a2 2 0 0 1 4 0a7 7 0 0 1 4 6v3a4 4 0 0 0 2 3h-16a4 4 0 0 0 2 -3v-3a7 7 0 0 1 4 -6"
          />
          <path d="M9 17v1a3 3 0 0 0 6 0v-1" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { usePullToRefresh } from '@/composables/usePullToRefresh'
import PullRefreshIndicator from '@/components/common/PullRefreshIndicator.vue'
import { requestGps } from '@/utils/gpsBridge'
import { loadKakaoMapScript } from '@/utils/kakaoMap'
import { isDailyWorker } from '@/utils/employment'
import { dateToYmd, ymdToDate } from './attdFormat'

import AttendanceTodayCard from './components/AttendanceTodayCard.vue'
import AttendanceWeekList from './components/AttendanceWeekList.vue'
import AttendanceMonthCalendar from './components/AttendanceMonthCalendar.vue'
import AttendanceDayDetailCard from './components/AttendanceDayDetailCard.vue'
import AttendanceActionSheet from './components/AttendanceActionSheet.vue'
import OffsiteReasonSheet from './components/OffsiteReasonSheet.vue'
import AppBottomTabBar from '@/components/common/AppBottomTabBar.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백 (앱 전역 $alert 우선, 없으면 window.alert) — MainView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 공통: confirm 폴백 (앱 전역 $confirm 우선, 없으면 window.confirm) — MainView 패턴 동일
const askConfirm = async (message) => {
  if (proxy?.$confirm) return await proxy.$confirm(message)
  return window.confirm(message)
}

// ───────────────────────────────────────────────────────────
// 세그먼트 / 탭 정의 (UI 토글 — 허용 범위)
// ───────────────────────────────────────────────────────────
const segments = [
  { key: 'today', label: '오늘' },
  { key: 'week', label: '이번주' },
  { key: 'month', label: '이번달' },
]

const activeTab = ref('today')

// ───────────────────────────────────────────────────────────
// 헤더 상태
// 알림 카운트는 공통 알림 소스가 아직 없어 0 고정(후속 연동).
// ───────────────────────────────────────────────────────────
const notificationCount = ref(0)

// ───────────────────────────────────────────────────────────
// 탭별 데이터 (API 응답 주입)
// 엔드포인트(baseURL=/prafta):
//   GET /appApi/attd/my/today
//   GET /appApi/attd/my/week?weekStartYmd=YYYYMMDD
//   GET /appApi/attd/my/month?yearMonth=YYYYMM
//   GET /appApi/attd/my/day-detail?workYmd=YYYYMMDD
// USER_CD/CMPNY_CD 등 세션 값은 axios 인터셉터가 자동 주입한다.
// ───────────────────────────────────────────────────────────
const todayDetail = ref(null)
const weekData = ref(null)
const monthData = ref(null)
const dayDetail = ref(null)
const selectedYmd = ref('')

// 현재 보고 있는 주/월 기준값
const currentWeekStartYmd = ref('')
const currentYearMonth = ref('')

// 탭별 최초 로드 여부 (재진입 시 불필요한 재호출 방지)
const todayLoaded = ref(false)

// 주/월 캐시 (key=weekStartYmd / yearMonth). 재조회 최소화(시안 §6.2)
const weekCache = new Map()
const monthCache = new Map()
const dayDetailCache = new Map()

// 바텀시트
const actionSheetOpen = ref(false)
const actionSheetDay = ref(null)

// 표준 빈 상태 — 활성 탭의 데이터 로드가 끝났는데 비어 있을 때만 노출
const showEmptyState = computed(() => {
  if (activeTab.value === 'today') return todayLoaded.value && !todayDetail.value
  if (activeTab.value === 'week') return !!currentWeekStartYmd.value && !weekData.value
  if (activeTab.value === 'month') return !!currentYearMonth.value && !monthData.value
  return false
})

// ───────────────────────────────────────────────────────────
// 날짜 계산 유틸 (로컬)
// ───────────────────────────────────────────────────────────
// 해당 일자가 속한 주의 시작(월요일) YYYYMMDD
const weekStartOf = (date) => {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const dow = d.getDay() // 0=일 ~ 6=토
  const diff = dow === 0 ? -6 : 1 - dow // 월요일로 보정
  d.setDate(d.getDate() + diff)
  return dateToYmd(d)
}
const addDaysYmd = (ymd, days) => {
  const d = ymdToDate(ymd)
  if (!d) return ymd
  d.setDate(d.getDate() + days)
  return dateToYmd(d)
}
const yearMonthOf = (date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  return `${y}${m}`
}
const shiftYearMonth = (ym, delta) => {
  const y = Number(ym.slice(0, 4))
  const m = Number(ym.slice(4, 6))
  const d = new Date(y, m - 1 + delta, 1)
  return yearMonthOf(d)
}

// ───────────────────────────────────────────────────────────
// API 호출 (401/403/500 은 axios 인터셉터가 처리. 네트워크 실패만 안내)
// ───────────────────────────────────────────────────────────
const loadToday = async () => {
  if (todayLoaded.value) return
  try {
    const res = await api.get('/appApi/attd/my/today')
    todayDetail.value = res?.data ?? null
    todayLoaded.value = true
  } catch (e) {
    console.error('[MyAttendance] today 조회 실패:', e?.message)
    showAlert('근태 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  }
}

// 쓰기(퇴근) 성공 후 강제 재조회 — todayLoaded 가드를 우회한다.
const reloadToday = async () => {
  todayLoaded.value = false
  await loadToday()
}

const loadWeek = async (weekStartYmd) => {
  currentWeekStartYmd.value = weekStartYmd
  if (weekCache.has(weekStartYmd)) {
    weekData.value = weekCache.get(weekStartYmd)
    return
  }
  try {
    const res = await api.get('/appApi/attd/my/week', { params: { weekStartYmd } })
    const data = res?.data ?? null
    if (data) weekCache.set(weekStartYmd, data)
    weekData.value = data
  } catch (e) {
    console.error('[MyAttendance] week 조회 실패:', e?.message)
    weekData.value = null
    showAlert('주간 근태를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  }
}

const loadMonth = async (yearMonth) => {
  currentYearMonth.value = yearMonth
  if (monthCache.has(yearMonth)) {
    monthData.value = monthCache.get(yearMonth)
    return
  }
  try {
    const res = await api.get('/appApi/attd/my/month', { params: { yearMonth } })
    const data = res?.data ?? null
    if (data) monthCache.set(yearMonth, data)
    monthData.value = data
  } catch (e) {
    console.error('[MyAttendance] month 조회 실패:', e?.message)
    monthData.value = null
    showAlert('월간 근태를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  }
}

// 일자상세를 "조회만" 한다(캐시 우선). loadDayDetail 과 달리 dayDetail.value(월 탭 상세 패널 상태)를
//   건드리지 않는다 — 요청 컨텍스트를 보강하려고 화면 상태까지 바꾸면 안 되기 때문.
const fetchDayDetail = async (workYmd) => {
  if (dayDetailCache.has(workYmd)) return dayDetailCache.get(workYmd)
  try {
    const res = await api.get('/appApi/attd/my/day-detail', { params: { workYmd } })
    const data = res?.data ?? null
    if (data) dayDetailCache.set(workYmd, data)
    return data
  } catch (e) {
    console.error('[MyAttendance] day-detail 조회 실패(컨텍스트 보강):', e?.message)
    return null
  }
}

const loadDayDetail = async (workYmd) => {
  if (dayDetailCache.has(workYmd)) {
    dayDetail.value = dayDetailCache.get(workYmd)
    return
  }
  try {
    const res = await api.get('/appApi/attd/my/day-detail', { params: { workYmd } })
    const data = res?.data ?? null
    if (data) dayDetailCache.set(workYmd, data)
    dayDetail.value = data
  } catch (e) {
    console.error('[MyAttendance] day-detail 조회 실패:', e?.message)
    dayDetail.value = null
    showAlert('일자 상세를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  }
}

// ───────────────────────────────────────────────────────────
// 탭 전환 — 최초 진입 시 해당 기간 데이터 로드(캐시 확인 후)
// ───────────────────────────────────────────────────────────
const onSelectTab = (key) => {
  activeTab.value = key
  if (key === 'today') {
    loadToday()
  } else if (key === 'week') {
    if (!currentWeekStartYmd.value) {
      loadWeek(weekStartOf(new Date()))
    }
  } else if (key === 'month') {
    if (!currentYearMonth.value) {
      const ym = yearMonthOf(new Date())
      // 월 진입 시 오늘 일자를 기본 선택 + 상세 조회
      const todayYmd = dateToYmd(new Date())
      selectedYmd.value = todayYmd
      loadMonth(ym).then(() => loadDayDetail(todayYmd))
    }
  }
}

// ───────────────────────────────────────────────────────────
// 헤더 / 하단 탭바
// ───────────────────────────────────────────────────────────
const onBack = () => {
  router.push('/MainView')
}
const onBell = () => {
  // TODO(developer): 알림 센터 진입(별도 라우트). 대상 화면 미구현.
  showAlert('준비 중입니다')
}
// prafta-app-025 J1-2: 하단 탭 라우팅은 공통 AppBottomTabBar 가 중앙화 처리한다(자체 onBottomTab 제거).

// ───────────────────────────────────────────────────────────
// 이번주 네비
// ───────────────────────────────────────────────────────────
const onPrevWeek = () => {
  loadWeek(addDaysYmd(currentWeekStartYmd.value, -7))
}
const onNextWeek = () => {
  loadWeek(addDaysYmd(currentWeekStartYmd.value, 7))
}
const onSelectWeekDay = (day) => {
  // 주 카드 탭 → 바텀시트 오픈 (UI 토글 — 허용 범위)
  actionSheetDay.value = day
  actionSheetOpen.value = true
}

// ───────────────────────────────────────────────────────────
// 이번달 네비 / 셀 선택
// ───────────────────────────────────────────────────────────
const onPrevMonth = () => {
  selectedYmd.value = ''
  dayDetail.value = null
  loadMonth(shiftYearMonth(currentYearMonth.value, -1))
}
const onNextMonth = () => {
  selectedYmd.value = ''
  dayDetail.value = null
  loadMonth(shiftYearMonth(currentYearMonth.value, 1))
}
// 연월 시트에서 임의 월 선택
const onSelectMonth = (yearMonth) => {
  if (!yearMonth || yearMonth === currentYearMonth.value) return
  selectedYmd.value = ''
  dayDetail.value = null
  loadMonth(yearMonth)
}
const onSelectDate = (ymd) => {
  selectedYmd.value = ymd
  loadDayDetail(ymd)
}

// ───────────────────────────────────────────────────────────
// prafta-app-008/015: 외근 사유 시트 + 2-pass 출퇴근 흐름
//   프론트는 사전에 외근 여부를 모르므로 일단 사유 없이 호출하고, 서버 errorCode 로 분기한다.
//   - ATTD_400_086: 외근 사유 필요 → OffsiteReasonSheet 오픈 → 사유 동봉 재호출
//   prafta-app-015: 2구간 출근 구간은 사용자가 명시 선택(targetWorkSeq)하므로 자동추정/084/085/
//     confirmSkipPrevSlot 2-pass 흐름은 폐기. 087(구간 미선택)·088(구간 중복)은 정상 흐름에서는
//     발생하지 않으나 방어적으로 서버 message 를 노출한다(아래 기본 분기에서 처리).
//   좌표·offsiteReason·targetWorkSeq 를 ctx 에 유지하며 재시도한다.
// ───────────────────────────────────────────────────────────

// 외근 사유 시트 상태 + 진행 중 출퇴근 컨텍스트
const offsiteSheetOpen = ref(false)
const offsiteMode = ref('checkIn') // 'checkIn' | 'checkOut'
const offsiteCtx = ref({ lat: null, lon: null, accuracy: null })

// 재시도 컨텍스트(좌표·플래그·사유 유지). 시트 제출 시 이 ctx 로 재호출한다.
let pendingCtx = null

// 출퇴근 API 단일 호출기 — 서버 errorCode 로 2-pass 분기(086 외근 사유만).
//   mode: 'checkIn'|'checkOut', ctx: { lat, lon, accuracy, isMocked, workYmd, offsiteReason, targetWorkSeq }
const callCheckInOut = async (mode, ctx) => {
  const url = mode === 'checkOut' ? '/appApi/attd/check-out' : '/appApi/attd/check-in'
  const body = {
    lat: ctx.lat,
    lon: ctx.lon,
    accuracy: ctx.accuracy,
    isMocked: ctx.isMocked,
    workYmd: ctx.workYmd,
    offsiteReason: ctx.offsiteReason || undefined,
  }
  // prafta-app-015: 2구간 스케줄 출근 구간 명시 선택(check-in 전용). 미선택이면 미전송(1구간/스케줄없음).
  if (mode === 'checkIn' && (ctx.targetWorkSeq === 1 || ctx.targetWorkSeq === 2)) {
    body.targetWorkSeq = ctx.targetWorkSeq
  }

  try {
    const res = await api.post(url, body)
    await reloadToday()
    if (res?.data?.isOffsite) {
      showAlert('근무지 밖이라 외근으로 처리되었어요.')
    } else {
      showAlert(mode === 'checkOut' ? '퇴근이 등록되었어요.' : '출근이 등록되었어요.')
    }
    // 성공 시 진행 중 컨텍스트/시트 정리.
    pendingCtx = null
    offsiteSheetOpen.value = false
  } catch (e) {
    const errorCode = e?.response?.data?.errorCode
    const message = e?.response?.data?.message

    // 086: 외근 사유 필요 → 사유 시트 오픈(좌표·기존 ctx 유지하여 제출 시 재호출).
    if (errorCode === 'ATTD_400_086') {
      pendingCtx = { ...ctx }
      offsiteMode.value = mode
      offsiteCtx.value = { lat: ctx.lat, lon: ctx.lon, accuracy: ctx.accuracy }
      offsiteSheetOpen.value = true
      return
    }

    // 근태 E2E(F2): 150(노무수령거부) — 촉진 확정 연차일 출근/퇴근 차단. 재시도 불가, 전용 차단 안내만 노출.
    if (errorCode === 'ATTD_400_150') {
      pendingCtx = null
      showAlert(
        message ||
          '오늘은 연차사용촉진으로 확정된 연차 사용일입니다. 회사는 금일 노무 제공을 수령하지 않으며, 출퇴근·근태 등록이 제한됩니다. 연차 변경이 필요하면 관리자에게 문의해 주세요.',
      )
      return
    }

    // 그 외 거부/실패(087 구간 미선택·088 구간 중복 포함) — 서버 message 우선 노출.
    //   087/088 은 정상 흐름(버튼 게이팅)에서는 발생하지 않으나 방어적으로 처리.
    console.error(`[MyAttendance] ${mode} 실패:`, e?.message)
    pendingCtx = null
    showAlert(
      message ||
        (mode === 'checkOut'
          ? '퇴근을 등록하지 못했어요. 잠시 후 다시 시도해 주세요.'
          : '출근을 등록하지 못했어요. 잠시 후 다시 시도해 주세요.'),
    )
  }
}

// 외근 사유 시트 제출 — 사유를 동봉하여 동일 좌표/플래그로 재호출.
const onOffsiteSubmit = async ({ reason }) => {
  if (!pendingCtx) {
    offsiteSheetOpen.value = false
    return
  }
  const ctx = { ...pendingCtx, offsiteReason: reason }
  // 시트는 재호출 성공/실패에 따라 callCheckInOut 내부에서 정리된다.
  await callCheckInOut(offsiteMode.value, ctx)
}

// 외근 사유 시트 취소 — 출퇴근 미등록(중단).
const onOffsiteCancel = () => {
  pendingCtx = null
  offsiteSheetOpen.value = false
}

// 출퇴근 진입 — 확인 → GPS 브리지 → status 분기 → 2-pass 호출 진입.
//   prafta-app-015: targetWorkSeq(1|2|null) — 2구간 스케줄 출근 구간 선택. 그 외(퇴근/단일출근)는 null.
const startCheckInOut = async (mode, targetWorkSeq = null) => {
  let confirmMsg
  if (mode === 'checkOut') confirmMsg = '퇴근하시겠어요?'
  else if (targetWorkSeq === 1) confirmMsg = '1구간 출근하시겠어요?'
  else if (targetWorkSeq === 2) confirmMsg = '2구간 출근하시겠어요?'
  else confirmMsg = '출근하시겠어요?'
  const ok = await askConfirm(confirmMsg)
  if (!ok) return
  // Flutter 위치 브리지로 현재 좌표 획득(권한은 앱 기동 시 하드게이트로 보장됨).
  const gps = await requestGps()
  if (gps.status === 'OK') {
    // Mock 위치는 서버가 거부하나, 사용자 경험상 먼저 안내 후 중단.
    if (gps.isMocked) {
      showAlert(
        mode === 'checkOut'
          ? '위치 위변조가 감지되어 퇴근할 수 없어요.'
          : '위치 위변조가 감지되어 출근할 수 없어요.',
      )
      return
    }
    await callCheckInOut(mode, {
      lat: gps.lat,
      lon: gps.lon,
      accuracy: gps.accuracy,
      isMocked: gps.isMocked ? 'Y' : 'N',
      // 출퇴근 대상 근무일. 오늘 카드의 workDate 기준.
      workYmd: todayDetail.value?.workDate,
      offsiteReason: null,
      targetWorkSeq,
    })
    return
  }
  if (gps.status === 'PERMISSION_DENIED' || gps.status === 'SERVICE_DISABLED') {
    showAlert('위치 권한 또는 위치 서비스가 꺼져 있어요. 설정에서 위치를 허용해 주세요.')
    return
  }
  // TIMEOUT / BRIDGE_UNAVAILABLE 등 측위 실패.
  showAlert('현재 위치를 확인하지 못했어요. 잠시 후 다시 시도해 주세요.')
}

// ───────────────────────────────────────────────────────────
// prafta-app-013: 오늘/일상세 카드 응답(detail)을 4액션 바텀시트용 day 객체로 변환.
//   - 시트(AttendanceActionSheet)는 day.workYmd(날짜)·day.actions(4플래그)·메타로 구동된다.
//   - detail 의 날짜 필드는 workDate 이므로 workYmd 로 매핑하고, 4액션은 서버 산출 sheetActions 를 쓴다.
//   - 메타/컨텍스트(workPlanName/scheduleSummary/attendanceSummary/leaveTypeName/nodeCd/siteName/
//     hasIssue/slots)는 시트 메타 표시 + onSheetAction→navigateToAttdRequest 컨텍스트에 사용된다.
//   - 게이팅은 전부 서버(sheetActions) 표시만. 프론트 비즈니스 판정 없음.
// ───────────────────────────────────────────────────────────
const toSheetDay = (detail) => {
  if (!detail) return null
  return {
    workYmd: detail.workDate,
    actions: detail.sheetActions || {},
    workPlanName: detail.workPlanName,
    leaveTypeName: detail.leaveTypeName,
    scheduleSummary: detail.scheduleSummary,
    attendanceSummary: detail.attendanceSummary,
    nodeCd: detail.nodeCd,
    siteName: detail.siteName,
    hasIssue: detail.hasIssue,
    slots: detail.slots,
  }
}

// ───────────────────────────────────────────────────────────
// 액션 핸들러 — 실 신청/보정/연차 플로우는 후속 작업(대상 화면 미구현)
// ───────────────────────────────────────────────────────────
// payload.type: requestModify | checkOut | checkIn
const onTodayAction = async (payload) => {
  const type = payload?.type
  if (type === 'checkOut') {
    await startCheckInOut('checkOut')
    return
  }
  if (type === 'checkIn') {
    // prafta-app-015: 2구간 스케줄은 카드에서 선택한 구간(payload.targetWorkSeq=1|2)을 전달.
    //   1구간/스케줄없음 단일 출근은 targetWorkSeq 미동봉(null).
    const targetWorkSeq =
      payload?.targetWorkSeq === 1 || payload?.targetWorkSeq === 2 ? payload.targetWorkSeq : null
    await startCheckInOut('checkIn', targetWorkSeq)
    return
  }
  if (type === 'requestModify') {
    // prafta-app-013: 오늘 탭 "수정 요청" → 이번주와 동일한 4액션 시트 오픈.
    //   todayDetail.sheetActions 로 시트 4행을 개별 게이팅한다(결정 §3).
    const day = toSheetDay(todayDetail.value)
    if (!day) {
      showAlert('근태 정보를 확인할 수 없습니다.')
      return
    }
    actionSheetDay.value = day
    actionSheetOpen.value = true
    return
  }
  // 그 외 액션은 대상 플로우 미구현.
  showAlert('준비 중입니다')
}
// ───────────────────────────────────────────────────────────
// PRAFTA-APP-007: 근태 요청 폼 (스케줄 수정 / 근태 보정 / 초과근무) 라우팅
// 컨텍스트는 sessionStorage 에 1회 저장하고 라우트 진입 시 폼이 읽고 즉시 제거한다.
// ───────────────────────────────────────────────────────────
const ATTD_REQ_CONTEXT_KEY = 'attd_req_ctx_v1'
// prafta-app-018-C: 연차 신청 폼 전용 컨텍스트 키(근태요청 키와 분리). LeaveApplyView 가 읽고 제거.
const LEAVE_APPLY_CONTEXT_KEY = 'leave_apply_ctx_v1'

const buildContextFromDay = (day) => {
  if (!day) return null
  return {
    workYmd: day.workYmd,
    nodeCd: day.nodeCd,
    siteName: day.siteName,
    scheduleSummary: day.scheduleSummary,
    workPlanName: day.workPlanName,
    attendanceSummary: day.attendanceSummary,
    hasIssue: day.hasIssue,
    // 출퇴근 시각 프리필용 (보정 폼이 사용). 백엔드 day-detail 응답의 slots 배열.
    slots: day.slots,
  }
}

const navigateToAttdRequest = async (formType, day) => {
  if (!day || !day.workYmd) {
    showAlert('대상 일자를 확인할 수 없습니다.')
    return
  }
  // 주/월 목록 응답(WeekDayResponse)에는 slots 가 없다(오늘/일자상세 응답에만 존재).
  //   그 경로로 열린 시트의 day 를 그대로 넘기면 폼이 스케줄·실근태를 알 수 없어
  //   출퇴근 시각 프리필과 초과근무 "등록 가능 시간"이 통째로 비어버린다.
  //   → slots 가 없으면 일자상세를 먼저 조회해 컨텍스트를 채운다(캐시 있으면 즉시).
  let ctxDay = day
  if (!Array.isArray(day.slots)) {
    const detail = await fetchDayDetail(day.workYmd)
    if (detail) {
      // 시트가 준 메타(actions 등)는 유지하고 slots 등 상세 필드만 보강한다.
      ctxDay = { ...day, ...toSheetDay(detail), actions: day.actions }
    }
  }
  try {
    sessionStorage.setItem(ATTD_REQ_CONTEXT_KEY, JSON.stringify(buildContextFromDay(ctxDay)))
  } catch (e) {
    console.error('[MyAttendance] 컨텍스트 저장 실패:', e?.message)
    showAlert('컨텍스트 저장에 실패했습니다.')
    return
  }
  router.push({
    path: '/AttdRequest',
    query: { type: formType, workYmd: day.workYmd, nodeCd: day.nodeCd || '' },
  })
}

// payload.type: requestModify
//   prafta-app-013: 이번달 일자상세 하단 2버튼(근태 보정/초과근무)을 제거하고 시트로 통일.
//   본체 "수정 요청" → requestModify 수신 → dayDetail 기준 4액션 시트 오픈(onSheetAction 재사용).
const onDayDetailAction = (payload) => {
  const type = payload?.type
  if (type === 'requestModify') {
    const day = toSheetDay(payload?.detail || dayDetail.value)
    if (!day) {
      showAlert('일자 상세를 확인할 수 없습니다.')
      return
    }
    actionSheetDay.value = day
    actionSheetOpen.value = true
    return
  }
  // 그 외 액션은 대상 플로우 미구현
  showAlert('준비 중입니다')
}
// payload.type: scheduleModify | attendanceCorrection | overtime | leave
const onSheetAction = (payload) => {
  actionSheetOpen.value = false
  const type = payload?.type
  const day = payload?.day || actionSheetDay.value
  if (type === 'scheduleModify') {
    return navigateToAttdRequest('schedModify', day)
  }
  if (type === 'attendanceCorrection') {
    return navigateToAttdRequest('attdCorrection', day)
  }
  if (type === 'overtime') {
    return navigateToAttdRequest('overtime', day)
  }
  if (type === 'leave') {
    // prafta-app-018-C: 연차 신청 폼 라우팅. day 컨텍스트(workYmd/nodeCd/siteName/schedule)를
    //   연차 전용 sessionStorage 키(attd_req_ctx_v1 와 분리)로 저장 → 폼 컨테이너가 1회 읽고 제거.
    if (!day || !day.workYmd) {
      showAlert('대상 일자를 확인할 수 없습니다.')
      return
    }
    try {
      sessionStorage.setItem(LEAVE_APPLY_CONTEXT_KEY, JSON.stringify(buildContextFromDay(day)))
    } catch (e) {
      console.error('[MyAttendance] 연차 컨텍스트 저장 실패:', e?.message)
      showAlert('컨텍스트 저장에 실패했습니다.')
      return
    }
    return router.push({
      path: '/LeaveApply',
      query: { workYmd: day.workYmd, nodeCd: day.nodeCd || '' },
    })
  }
  // 그 외 액션은 대상 플로우 미구현.
  showAlert('준비 중입니다')
}

// ───────────────────────────────────────────────────────────
// 당겨서 새로고침 — 활성 탭의 현재 보고 있는 기간 데이터를 강제 재조회(캐시 가드 우회).
//   부작용 없는 조회만. 탭별로 today/week/month(+선택 일자 상세)를 갱신한다.
// ───────────────────────────────────────────────────────────
const scrollRef = ref(null)
const { onPullStart, onPullMove, onPullEnd, indicatorProps } = usePullToRefresh(
  scrollRef,
  async () => {
    if (activeTab.value === 'today') {
      await reloadToday()
    } else if (activeTab.value === 'week') {
      const ymd = currentWeekStartYmd.value || weekStartOf(new Date())
      weekCache.delete(ymd)
      await loadWeek(ymd)
    } else if (activeTab.value === 'month') {
      const ym = currentYearMonth.value || yearMonthOf(new Date())
      monthCache.delete(ym)
      await loadMonth(ym)
      // 선택된 일자 상세도 함께 갱신(있을 때만).
      if (selectedYmd.value) {
        dayDetailCache.delete(selectedYmd.value)
        await loadDayDetail(selectedYmd.value)
      }
    }
  },
)

// ───────────────────────────────────────────────────────────
// 진입 시 기본 탭('오늘') 데이터 로드
// ───────────────────────────────────────────────────────────
onMounted(() => {
  // prafta-app-025 J1-4: 일용직(DAILY)은 근태 조회 대상이 아님 → 직접 URL/딥링크 진입 방어.
  //   탭/카드 숨김은 UX 차단일 뿐이라 직접 진입을 막기 위해 안내 후 메인으로 복귀.
  if (isDailyWorker()) {
    showAlert('일용직 사용자는 근태 조회 대상이 아닙니다.')
    router.replace('/MainView')
    return
  }
  loadToday()
  // prafta-app-008: 외근 사유 시트(OffsiteReasonSheet)의 카카오 지도 SDK 프리로드.
  // 시트 오픈 시 SDK 네트워크 로드로 표시가 지연되는 문제 방지. 중복 가드로 idempotent, 실패는 폴백 동작.
  loadKakaoMapScript().catch((e) => {
    console.warn('[MyAttendanceView] 카카오 지도 SDK 프리로드 실패(무시):', e?.message)
  })
})
</script>

<style scoped>
/*
 * 디자인 토큰 — MainView(.home-view)와 동일 세트를 본 화면 루트에 선언.
 * 자식 컴포넌트(scoped)는 var(--...) 를 상속받아 사용한다. 하드코딩 금지.
 */
.my-attd-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-primary-text-deep: #15803d;
  --color-primary-text-darkest: #14532d;
  --color-danger: #ef4444;
  --color-on-danger: #ffffff;
  --color-danger-tint: #fef2f2;
  --color-danger-border: #fecaca;
  --color-danger-text: #b91c1c;
  --color-warning: #f59e0b;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-warning-border: #fed7aa;
  --color-warning-border-light: #fef3c7;
  --color-info: #3b82f6;
  --color-info-strong: #1e40af;
  --color-info-tint: #eff6ff;
  --color-info-border: #bfdbfe;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --color-attd-cell-off: #fafafa;
  --color-overlay: rgba(0, 0, 0, 0.45);
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-xl: 20px;
  --radius-full: 9999px;
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  position: relative;
  /* 1뎁스 화면 공통 패턴(MainView/SafetyHub 등)과 동일하게 뷰포트 높이 고정 —
     콘텐츠가 짧아도 하단 탭바(AppBottomTabBar)가 뷰포트 바닥에 고정되도록. dvh 미지원은 위 vh 폴백. */
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
.attd-hd {
  height: 56px;
  flex-shrink: 0;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  border-bottom: 1px solid var(--color-border-light);
}
.attd-hd__back,
.attd-hd__bell {
  position: relative;
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.attd-hd__back {
  margin-left: -10px;
}
.attd-hd__bell {
  margin-right: -10px;
}
.attd-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.attd-hd__badge {
  position: absolute;
  top: 8px;
  right: 8px;
  min-width: 16px;
  height: 16px;
  padding: 0 var(--space-xs);
  background: var(--color-danger);
  color: var(--color-surface);
  font-size: 10px;
  font-weight: 700;
  line-height: 16px;
  text-align: center;
  border-radius: var(--radius-full);
  border: 1.5px solid var(--color-surface);
}

/* 세그먼트 */
.attd-seg {
  margin: var(--space-md) var(--space-lg) 0;
  padding: var(--space-xs);
  background: var(--color-border-light);
  border-radius: var(--radius-md);
  display: grid;
  grid-template-columns: repeat(3, 1fr);
}
.attd-seg__item {
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.attd-seg__item--on {
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-weight: 700;
  box-shadow: var(--shadow-sm);
}

/* 본문 */
.attd-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: var(--space-md) var(--space-lg) 88px;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* 표준 빈 상태 */
.attd-empty {
  margin: 0;
  padding: 48px var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.attd-sprite {
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
