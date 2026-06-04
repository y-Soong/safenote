<!--
  MainView.vue — PRAFTA 사용자 앱 메인 홈 화면 (정규직)
  - 작업 ID: prafta-app-001 (계약서: .claude/requests/app_requests/prafta-app-001-contract.md)
  - 본 라운드 스코프 (developer):
      (1) 헤더(세션 직결) + 5개 카드를 신규 백엔드 home-summary 응답에 연동
      (2) 기존 mock 4케이스(CASE1~4) + dev picker 제거
      (3) 로딩/에러 상태 처리 추가
  - outside scope (이번 라운드에서 건드리지 않음):
      출퇴근/TBM 쓰기 액션(prafta-app-003), 실시간 GPS,
      공지 도메인(미구축), 알림센터 진입
-->
<template>
  <div class="home-view">
    <!-- SVG sprite (다른 카드의 <use> 참조 전제) -->
    <HomeIcons />

    <!-- 헤더 -->
    <HomeHeader
      :site-name="siteName"
      :notification-count="notificationCount"
      :user-initial="userInitial"
      @click:bell="onBellClick"
      @click:avatar="onAvatarClick"
    />

    <!-- 본문 (스크롤 영역) -->
    <main
      class="main"
      ref="mainEl"
      @touchstart.passive="onPullStart"
      @touchmove="onPullMove"
      @touchend="onPullEnd"
      @touchcancel="onPullEnd"
    >
      <!-- 당겨서 새로고침 인디케이터 — 스크롤 최상단에서 아래로 당기면 노출 -->
      <div
        class="pull-refresh"
        :class="{ 'pull-refresh--animating': !isDragging }"
        :style="{ height: pullIndicatorHeight + 'px' }"
        aria-live="polite"
      >
        <span v-if="isRefreshing" class="pull-refresh__text">새로고침 중...</span>
        <span v-else-if="pullReady" class="pull-refresh__text">놓으면 새로고침</span>
        <span v-else-if="pullDistance > 0" class="pull-refresh__text">당겨서 새로고침</span>
      </div>

      <!-- 인사말 -->
      <div class="greeting">
        <p class="greeting__date">{{ todayDateText }}</p>
        <p class="greeting__msg">오늘도 좋은 하루 되세요</p>
      </div>

      <!-- 로딩 표시 (home-summary 응답 대기) -->
      <div v-if="isLoading" class="home-loading" aria-live="polite">불러오는 중...</div>

      <template v-else>
        <!-- 출퇴근 -->
        <AttendanceCard
          :status="attdStatus"
          :is-offsite="isOffsite"
          :schedule-start-time="scheduleStartTime"
          :schedule-end-time="scheduleEndTime"
          :check-in-time="checkInTime"
          :check-out-time="checkOutTime"
          :schedule-exists="scheduleExists"
          :site-name="siteName"
          :can-check-in="canCheckIn"
          :can-check-out="canCheckOut"
          :is-two-slot="attdIsTwoSlot"
          :slots="attdSlots"
          @click:checkin="onCheckIn"
          @click:checkout="onCheckOut"
        />

        <!-- 근태 조회 -->
        <AttendanceSummaryCard
          :remaining-days="remainingLeaveDays"
          :granted-days="grantedLeaveDays"
          :pending-count="approvalPendingCount"
          @click:detail="onAttdSummaryDetail"
          @click:leave="onLeaveClick"
          @click:approval="onApprovalClick"
        />

        <!-- 안전 활동 (안전점검 시작 + 위험성 발굴 + 아차사고 보고/사건 관리) -->
        <SafetyActivityCard
          :blocked="safetyBlocked"
          :is-safety-manager="isSafetyManager"
          @click:detail="onSafetyDetail"
          @click:safety-check="onSafetyCheck"
          @click:risk-discovery="onRiskDiscovery"
          @click:near-miss-report="onNearMissReport"
          @click:near-miss-manage="onNearMissManage"
        />

        <!-- TBM 참석 -->
        <TbmAttendCard
          :tbm-status="tbmStatus"
          :session-time="tbmSessionTime"
          :session-location="tbmSessionLocation"
          :session-leader="tbmSessionLeader"
          :attended-at="tbmAttendedAt"
          @click:detail="onTbmDetail"
          @click:attend="onTbmAttend"
        />

        <!-- 공지사항 -->
        <NoticeListCard
          :items="notices"
          :unread-count="noticeUnreadCount"
          @click:more="onNoticeMore"
          @click:row="onNoticeRow"
        />
      </template>
    </main>

    <!-- 하단 탭바 -->
    <HomeTabBar :active-tab="'home'" :tbm-badge-count="tbmBadgeCount" @click:tab="onTabClick" />

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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { requestGps } from '@/utils/gpsBridge'
import { loadKakaoMapScript } from '@/utils/kakaoMap'

import HomeIcons from './components/HomeIcons.vue'
import HomeHeader from './components/HomeHeader.vue'
import AttendanceCard from './components/AttendanceCard.vue'
import AttendanceSummaryCard from './components/AttendanceSummaryCard.vue'
import SafetyActivityCard from './components/SafetyActivityCard.vue'
import TbmAttendCard from './components/TbmAttendCard.vue'
import NoticeListCard from './components/NoticeListCard.vue'
import HomeTabBar from './components/HomeTabBar.vue'
import OffsiteReasonSheet from '@/views/attd/components/OffsiteReasonSheet.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// ───────────────────────────────────────────────────────────
// 로딩 상태 — home-summary 응답 대기
// ───────────────────────────────────────────────────────────
const isLoading = ref(true)

// ───────────────────────────────────────────────────────────
// 헤더 / 사용자 — 세션(sessionStorage gv_*) 직결, API 불요 (계약서 §3)
// ───────────────────────────────────────────────────────────
const siteName = ref('') // gv_siteNm
const userInitial = ref('?') // gv_userNm 앞 2자

// prafta-app-001: 공지 도메인 미구축으로 보류 → 알림벨 카운트는 0 고정
const notificationCount = ref(0)

// 세션값 주입: 사업장명 + 아바타 이니셜(이름 앞 2자, 빈값이면 '?')
const applySessionHeader = () => {
  siteName.value = sessionStorage.getItem('gv_siteNm') || ''
  const userNm = sessionStorage.getItem('gv_userNm') || ''
  userInitial.value = userNm ? userNm.slice(0, 2) : '?'
}

// ───────────────────────────────────────────────────────────
// 인사말 — 날짜 (클라이언트 로컬, 계약서 §3 변경 없음)
// ───────────────────────────────────────────────────────────
const todayDateText = computed(() => {
  const d = new Date()
  const days = ['일', '월', '화', '수', '목', '금', '토']
  return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일 (${days[d.getDay()]})`
})

// ───────────────────────────────────────────────────────────
// 출퇴근 카드 — home-summary attendance 매핑
// ───────────────────────────────────────────────────────────
const attdStatus = ref('BEFORE_WORK') // 'BEFORE_WORK' | 'WORKING' | 'OFF_WORK'
const isOffsite = ref(false)
const scheduleStartTime = ref('') // HHMM
const scheduleEndTime = ref('') // HHMM
const checkInTime = ref('') // HHMM
const checkOutTime = ref('') // HHMM (OFF_WORK 시 퇴근시각)
const scheduleExists = ref(true) // 기준일 근무 스케줄 존재 여부
const canCheckIn = ref(false)
const canCheckOut = ref(false)
// prafta-app-015: 2구간 스케줄 구간 선택 게이팅(home-summary attendance 확장).
const attdIsTwoSlot = ref(false)
const attdSlots = ref([]) // [{ workSeq, canCheckInThisSlot, alreadyCheckedIn }]

// ───────────────────────────────────────────────────────────
// 근태 조회 카드 — home-summary leave / approval 매핑
// ───────────────────────────────────────────────────────────
const remainingLeaveDays = ref(0)
const grantedLeaveDays = ref(0)
const approvalPendingCount = ref(0)

// ───────────────────────────────────────────────────────────
// 안전 활동 카드
// blocked = (attdStatus === 'BEFORE_WORK') — 산업안전 정책서 §3
// ───────────────────────────────────────────────────────────
const safetyBlocked = computed(() => attdStatus.value === 'BEFORE_WORK')

// ───────────────────────────────────────────────────────────
// 아차사고 (prafta-app-012)
//   - "사건 관리" 진입점은 안전직군(gv_authCd) 에게만 노출. 사업장 권한 최종 판정은 서버(assertSiteAccess).
//   - 안전직군 AUTH_CD 집합은 서버 푸시 대상(AppNearMiss01ServiceImpl.SAFETY_AUTH_CDS)과 정합 — '99999'(접근차단 권한) 제외.
//   - "아차사고 보고"(근로자) 는 출근 게이트 미적용(즉시성) — 항상 노출.
// ───────────────────────────────────────────────────────────
const SAFETY_MANAGER_AUTH_CODES = ['00001', '00004', '00006', '00008', 'master', 'safe', 'system']
const isSafetyManager = computed(() => {
  const authCd = sessionStorage.getItem('gv_authCd') || ''
  return SAFETY_MANAGER_AUTH_CODES.includes(authCd)
})

// ───────────────────────────────────────────────────────────
// TBM 카드 — home-summary tbm 매핑
// 카드 상태 enum('BEFORE_CHECK_IN' | 'AVAILABLE' | 'ATTENDED' | 'NONE')은
// 계약서 응답(hasToday / sessionStatus / myAttendanceStatus)을 가공해 산출한다.
// ───────────────────────────────────────────────────────────
const tbmStatus = ref('NONE')
const tbmSessionTime = ref('') // HHMM (openedTime)
const tbmSessionLocation = ref('') // 장소 소스 없음 → title 로 대체 (계약서 §3)
const tbmSessionLeader = ref('') // presenterName
const tbmAttendedAt = ref('') // HHMM (myEntryTime)
// prafta-app-004-C5: 입실 화면(/TbmEntry) 진입에 필요한 세션 식별자.
// ⚠️ 현재 home-summary 응답(HomeSummaryResponse.Tbm)에는 sessionCd 가 없다 → 빈 값.
//    백엔드가 tbm.sessionCd 를 노출하면 자동으로 라우팅이 동작한다(아래 onTbmAttend).
const tbmSessionCd = ref('')

// ───────────────────────────────────────────────────────────
// 공지사항 카드
// prafta-app-001: 공지 도메인 미구축으로 보류 → 빈 배열 / 미열람 0
// ───────────────────────────────────────────────────────────
const notices = ref([])
const noticeUnreadCount = ref(0)

// ───────────────────────────────────────────────────────────
// 하단 탭바 — TBM 미참석 카운트 (참석 가능 상태면 1)
// ───────────────────────────────────────────────────────────
const tbmBadgeCount = computed(() => (tbmStatus.value === 'AVAILABLE' ? 1 : 0))

// ───────────────────────────────────────────────────────────
// 공통: alert 폴백 (앱 전역 $alert 우선, 없으면 window.alert)
// ───────────────────────────────────────────────────────────
const showAlert = (message) => {
  if (proxy?.$alert) {
    return proxy.$alert(message)
  }
  window.alert(message)
  return Promise.resolve()
}

// 공통: confirm 폴백 (앱 전역 $confirm 우선, 없으면 window.confirm)
const askConfirm = async (message) => {
  if (proxy?.$confirm) {
    return await proxy.$confirm(message)
  }
  return window.confirm(message)
}

// ───────────────────────────────────────────────────────────
// home-summary 응답 → 카드 state 매핑
// ───────────────────────────────────────────────────────────
const applyAttendance = (attd) => {
  if (!attd) return
  // prafta-app-013-4: BEFORE_WORK / WORKING / OFF_WORK 3상태를 그대로 사용(통합 제거).
  attdStatus.value = attd.status || 'BEFORE_WORK'
  isOffsite.value = !!attd.isOffsite
  scheduleStartTime.value = attd.scheduleStart || ''
  scheduleEndTime.value = attd.scheduleEnd || ''
  checkInTime.value = attd.checkInTime || ''
  checkOutTime.value = attd.checkOutTime || ''
  scheduleExists.value = !!attd.scheduleExists
  canCheckIn.value = !!attd.canCheckIn
  canCheckOut.value = !!attd.canCheckOut
  // prafta-app-015: 2구간 스케줄 구간 선택 게이팅 플래그.
  attdIsTwoSlot.value = !!attd.isTwoSlot
  attdSlots.value = Array.isArray(attd.slots) ? attd.slots : []
}

const applyLeave = (leave) => {
  remainingLeaveDays.value = leave?.remainingDays ?? 0
  grantedLeaveDays.value = leave?.grantedDays ?? 0
}

const applyApproval = (approval) => {
  approvalPendingCount.value = approval?.pendingCount ?? 0
}

const applyTbm = (tbm) => {
  if (!tbm || !tbm.hasToday) {
    tbmStatus.value = 'NONE'
    tbmSessionTime.value = ''
    tbmSessionLocation.value = ''
    tbmSessionLeader.value = ''
    tbmAttendedAt.value = ''
    tbmSessionCd.value = ''
    return
  }

  // sessionCd 는 현재 계약에 없으므로 있으면 사용, 없으면 빈 값(라우팅 가드에서 처리).
  tbmSessionCd.value = tbm.sessionCd || ''
  tbmSessionTime.value = tbm.openedTime || ''
  // 장소(location) 백엔드 소스 없음 → title 을 메타 표시에 사용 (계약서 §3, 카드 수정 최소화)
  tbmSessionLocation.value = tbm.title || ''
  tbmSessionLeader.value = tbm.presenterName || ''
  tbmAttendedAt.value = tbm.myEntryTime || ''

  // 카드 상태 산출
  // - 내가 참석 완료 → ATTENDED
  // - 미참석 + 출근 전 → BEFORE_CHECK_IN (출근 후 참석 가능)
  // - 미참석 + 근무중 → AVAILABLE
  if (tbm.myAttendanceStatus === 'COMPLETED') {
    tbmStatus.value = 'ATTENDED'
  } else if (attdStatus.value === 'WORKING') {
    tbmStatus.value = 'AVAILABLE'
  } else {
    tbmStatus.value = 'BEFORE_CHECK_IN'
  }
}

// ───────────────────────────────────────────────────────────
// home-summary 호출 (GET /appApi/home01/home-summary — 컨트롤러 @RequestMapping("/home01")과 일치)
// 인증/세션값은 axios 인터셉터가 처리(Authorization 헤더 + gv_* 파라미터).
// ───────────────────────────────────────────────────────────
// showLoading=true: 최초 진입 등 전체 로딩 표시(카드 숨김). false: 당겨서 새로고침 — 카드 유지하고 자체 인디케이터 사용.
const loadHomeSummary = async ({ showLoading = true } = {}) => {
  if (showLoading) isLoading.value = true
  try {
    const { data } = await api.get('/appApi/home01/home-summary')
    // attendance 를 먼저 적용해야 TBM 상태 산출 시 근무중 여부를 참조할 수 있다.
    applyAttendance(data?.attendance)
    applyLeave(data?.leave)
    applyApproval(data?.approval)
    applyTbm(data?.tbm)
  } catch (e) {
    // 401/403 등 토큰 에러는 axios 인터셉터가 강제 로그아웃 처리.
    // 그 외 실패는 사용자 알림 (기존 앱 알림 패턴).
    console.warn('[MainView] home-summary 조회 실패:', e?.message)
    showAlert('홈 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.')
  } finally {
    if (showLoading) isLoading.value = false
  }
}

// ───────────────────────────────────────────────────────────
// 당겨서 새로고침 — 스크롤 최상단에서 아래로 더 당기면(overscroll) home-summary 재조회.
//   1) touchstart 시점에 스크롤이 최상단이면 추적 시작
//   2) touchmove 에서 아래로 당긴 거리(저항감 0.5배)를 인디케이터 높이로 환산
//   3) touchend 시 임계값 이상이면 새로고침 실행
// ───────────────────────────────────────────────────────────
const mainEl = ref(null)
const pullDistance = ref(0) // 현재 당김 거리(px, 인디케이터 높이)
const isRefreshing = ref(false) // 새로고침 진행 중
const isDragging = ref(false) // 손가락으로 당기는 중(애니메이션 토글용)
const PULL_THRESHOLD = 70 // 이 거리 이상 당기고 놓으면 새로고침
const MAX_PULL = 120 // 인디케이터 최대 높이

const pullReady = computed(() => pullDistance.value >= PULL_THRESHOLD)
const pullIndicatorHeight = computed(() => (isRefreshing.value ? 48 : pullDistance.value))

let touchStartY = 0
let tracking = false

// 스크롤이 최상단에 닿았는지 판정(1px 오차 허용)
const isScrolledToTop = () => {
  const el = mainEl.value
  if (!el) return false
  return el.scrollTop <= 0
}

const onPullStart = (e) => {
  if (isRefreshing.value) return
  // 최상단에 있을 때만 당김 추적 시작
  tracking = isScrolledToTop()
  if (tracking) touchStartY = e.touches[0].clientY
}

const onPullMove = (e) => {
  if (!tracking || isRefreshing.value) return
  const delta = e.touches[0].clientY - touchStartY // 아래로 당기면 양수
  if (delta > 0 && isScrolledToTop()) {
    isDragging.value = true
    pullDistance.value = Math.min(MAX_PULL, delta * 0.5) // 저항감
    // iOS 고무줄/추가 스크롤 억제(가능한 경우)
    if (e.cancelable) e.preventDefault()
  } else {
    isDragging.value = false
    pullDistance.value = 0
  }
}

const onPullEnd = async () => {
  isDragging.value = false
  if (!tracking) return
  tracking = false
  const shouldRefresh = pullDistance.value >= PULL_THRESHOLD
  pullDistance.value = 0
  if (!shouldRefresh || isRefreshing.value) return
  isRefreshing.value = true
  try {
    applySessionHeader()
    await loadHomeSummary({ showLoading: false })
  } finally {
    isRefreshing.value = false
  }
}

onMounted(() => {
  applySessionHeader()
  loadHomeSummary()
  // prafta-app-008: 외근 사유 시트(OffsiteReasonSheet)의 카카오 지도 SDK 를 미리 1회 로드해 둔다.
  // 시트를 열 때 네트워크로 SDK 를 받느라 시트 표시가 지연되는 문제 방지(프리로드).
  // 로드 함수는 중복 가드가 있어 idempotent 하며, 실패해도 시트의 좌표 텍스트 폴백이 동작하므로 조용히 무시.
  loadKakaoMapScript().catch((e) => {
    console.warn('[MainView] 카카오 지도 SDK 프리로드 실패(무시):', e?.message)
  })
})

// prafta-app-010: 로그아웃은 마이페이지(/MyPage) 의 로그아웃 버튼으로 이전되었다.
// 기존 MainView.logout() 은 마이페이지 메인(MyPageView)으로 책임 이동.

// ───────────────────────────────────────────────────────────
// 이벤트 핸들러
// ───────────────────────────────────────────────────────────

// 헤더
const onBellClick = () => {
  // prafta-app-001: 공지/알림 도메인 미구축으로 보류
  showAlert('준비 중입니다')
}

const onAvatarClick = () => {
  // prafta-app-010: 마이페이지로 진입. 로그아웃은 마이페이지 메인의 로그아웃 버튼으로 이전.
  router.push('/MyPage')
}

// ───────────────────────────────────────────────────────────
// prafta-app-008/015: 외근 사유 시트 + 2-pass 출퇴근 흐름 (MyAttendanceView 와 동형)
//   프론트는 사전에 외근 여부를 모르므로 일단 사유 없이 호출하고, 서버 errorCode 로 분기한다.
//   - ATTD_400_086: 외근 사유 필요 → OffsiteReasonSheet 오픈 → 사유 동봉 재호출
//   prafta-app-015: 2구간 출근 구간은 사용자 명시 선택(targetWorkSeq)이므로 자동추정/084/085/
//     confirmSkipPrevSlot 흐름 폐기. 087(구간 미선택)·088(구간 중복)은 정상 흐름에선 발생하지
//     않으나 방어적으로 서버 message 를 노출한다(아래 기본 분기).
//   (홈 화면은 today 미보유 → workYmd 생략, 서버가 최신 열린 근태/오늘로 폴백.)
// ───────────────────────────────────────────────────────────

// 외근 사유 시트 상태 + 진행 중 출퇴근 컨텍스트
const offsiteSheetOpen = ref(false)
const offsiteMode = ref('checkIn') // 'checkIn' | 'checkOut'
const offsiteCtx = ref({ lat: null, lon: null, accuracy: null })

// 재시도 컨텍스트(좌표·플래그·사유 유지). 시트 제출 시 이 ctx 로 재호출한다.
let pendingCtx = null

// 출퇴근 API 단일 호출기 — 서버 errorCode 로 2-pass 분기(086 외근 사유만).
//   mode: 'checkIn'|'checkOut', ctx: { lat, lon, accuracy, isMocked, offsiteReason, targetWorkSeq }
const callCheckInOut = async (mode, ctx) => {
  const url = mode === 'checkOut' ? '/appApi/attd/check-out' : '/appApi/attd/check-in'
  const body = {
    lat: ctx.lat,
    lon: ctx.lon,
    accuracy: ctx.accuracy,
    isMocked: ctx.isMocked,
    offsiteReason: ctx.offsiteReason || undefined,
  }
  // prafta-app-015: 2구간 스케줄 출근 구간 명시 선택(check-in 전용). 미선택이면 미전송(1구간/스케줄없음).
  if (mode === 'checkIn' && (ctx.targetWorkSeq === 1 || ctx.targetWorkSeq === 2)) {
    body.targetWorkSeq = ctx.targetWorkSeq
  }

  try {
    const res = await api.post(url, body)
    // 홈 요약 재조회로 출퇴근 카드 상태 갱신.
    await loadHomeSummary()
    if (res?.data?.isOffsite) {
      showAlert('근무지 밖이라 외근으로 처리되었어요.')
    } else {
      showAlert(mode === 'checkOut' ? '퇴근이 등록되었어요.' : '출근이 등록되었어요.')
    }
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

    // 그 외 거부/실패(087 구간 미선택·088 구간 중복 포함) — 서버 message 우선 노출.
    console.error(`[MainView] ${mode} 실패:`, e?.message)
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
  await callCheckInOut(offsiteMode.value, ctx)
}

// 외근 사유 시트 취소 — 출퇴근 미등록(중단).
const onOffsiteCancel = () => {
  pendingCtx = null
  offsiteSheetOpen.value = false
}

// 출퇴근 진입 — 확인 → GPS 브리지(위치 권한은 앱 기동 시 하드게이트로 보장) → 2-pass 호출.
//   prafta-app-015: targetWorkSeq(1|2|null) — 2구간 스케줄 출근 구간 선택. 그 외(퇴근/단일출근)는 null.
const startCheckInOut = async (mode, targetWorkSeq = null) => {
  let confirmMsg
  if (mode === 'checkOut') confirmMsg = '퇴근하시겠어요?'
  else if (targetWorkSeq === 1) confirmMsg = '1구간 출근하시겠어요?'
  else if (targetWorkSeq === 2) confirmMsg = '2구간 출근하시겠어요?'
  else confirmMsg = '출근하시겠어요?'
  const ok = await askConfirm(confirmMsg)
  if (!ok) return
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

// 출근 — 2-pass 진입.
//   prafta-app-015: 2구간 스케줄 구간 선택 시 payload.targetWorkSeq(1|2) 전달. 단일 출근은 null.
const onCheckIn = async (payload) => {
  const targetWorkSeq =
    payload?.targetWorkSeq === 1 || payload?.targetWorkSeq === 2 ? payload.targetWorkSeq : null
  await startCheckInOut('checkIn', targetWorkSeq)
}

// 퇴근 — 2-pass 진입.
const onCheckOut = async () => {
  await startCheckInOut('checkOut')
}

// 근태 조회
const onAttdSummaryDetail = () => {
  // 근태 탭 "오늘" 화면 진입 (prafta-app-002 내 근태 조회)
  router.push('/MyAttendance')
}

const onLeaveClick = () => {
  // 연차 현황(본인 잔여연차 상세) 화면 진입 — prafta-app-005
  router.push('/MyLeaveSummaryView')
}

const onApprovalClick = () => {
  // PRAFTA-APP-006: 내 승인 요청 목록 화면 진입.
  // 메인 홈 KPI(approval.pendingCount)와 본 화면의 대기 건수는
  // home-summary 의 onMounted 재호출로 자연 동기화된다 (plan §1 P10).
  router.push('/MyRequests')
}

// 안전 활동
const onSafetyDetail = () => {
  // TODO(developer): 본인 안전점검/위험성 발굴 통합 이력 화면 진입 (별도 작업)
  showAlert('준비 중입니다')
}

// "안전점검 시작" → 기존 fnDayChkLst() → router.push('/QrScanner')
const onSafetyCheck = () => {
  router.push('/QrScanner')
}

// "위험성 발굴" → 기존 fnRisk_01() → router.push('/Risk_01')
const onRiskDiscovery = () => {
  router.push('/Risk_01')
}

// 아차사고 보고(근로자) → prafta-app-012 보고 화면. 출근 게이트 미적용.
const onNearMissReport = () => {
  router.push('/NearMissReport')
}

// 사건 관리(관리자/안전직군) → prafta-app-012 목록 화면. 사업장 권한은 서버가 최종 판정.
const onNearMissManage = () => {
  router.push('/NearMissManageList')
}

// TBM
const onTbmDetail = () => {
  // TODO(developer): 본인이 받은 TBM 리스트 진입 (별도 라우트)
  showAlert('준비 중입니다')
}

const onTbmAttend = () => {
  // prafta-app-004-C5: TBM 입실 화면 진입. sessionCd 가 있어야 entry-context 조회 가능.
  // ⚠️ home-summary 응답에 sessionCd 가 아직 없어 빈 값이면 진입 불가 → 안내.
  // TODO(developer): 백엔드 home-summary(tbm) 응답에 sessionCd 추가 필요(planner 재분해).
  if (!tbmSessionCd.value) {
    showAlert('세션 정보를 불러오는 중입니다. 잠시 후 다시 시도해 주세요.')
    return
  }
  router.push({ path: '/TbmEntry', query: { sessionCd: tbmSessionCd.value } })
}

// 공지 — prafta-app-001: 공지 도메인 미구축으로 보류 (빈 리스트 렌더)
const onNoticeMore = () => {
  showAlert('준비 중입니다')
}

const onNoticeRow = (noticeId) => {
  // prafta-app-001: 공지 도메인 미구축으로 보류
  console.log('[MainView] notice row click — noticeId=', noticeId)
  showAlert('준비 중입니다')
}

// 하단 탭
const onTabClick = (tabKey) => {
  // TODO(developer): 신규 라우트 신설 후 매핑
  if (tabKey === 'home') return
  if (tabKey === 'attd') {
    router.push('/MyAttendance')
    return
  }
  showAlert('준비 중입니다')
}
</script>

<style scoped>
/*
 * 디자인 토큰 — 시안 :root 토큰을 본 화면의 .home-view 에 한 번만 선언.
 * 자식 카드 컴포넌트는 scoped 상태에서도 var(--color-...) 를 상속받아 사용 가능.
 */
.home-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-danger: #ef4444;
  --color-warning: #f59e0b;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-warning-text-strong: #92400e;
  --color-warning-text-deep: #9a3412;
  --color-info: #3b82f6;
  --color-info-strong: #1d4ed8;
  --color-info-tint: #eff6ff;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --color-disabled-bg: #e5e7eb;
  --color-disabled-text: #9ca3af;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-full: 9999px;
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);

  min-height: 100vh;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 본문 — 헤더(56) 와 탭바(72) 사이 영역, 탭바에 가려지지 않도록 하단 패딩 */
.main {
  flex: 1;
  padding: 8px 16px 88px;
  overflow-y: auto;
}

.greeting {
  padding: 4px 4px 12px;
}
.greeting__date {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}
.greeting__msg {
  margin: 2px 0 0;
  font-size: 18px;
  font-weight: 500;
  color: var(--color-text-primary);
}

/* 로딩 표시 — home-summary 응답 대기 중 간단 안내 */
.home-loading {
  padding: 32px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}

/* 당겨서 새로고침 인디케이터 — 당김 거리에 따라 높이가 늘어났다 줄어든다 */
.pull-refresh {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  height: 0;
  color: var(--color-text-secondary);
  font-size: 13px;
}
/* 손가락을 뗀 뒤(또는 새로고침 중)에는 부드럽게 높이 전환, 당기는 중에는 즉시 반응 */
.pull-refresh--animating {
  transition: height 0.2s ease;
}
.pull-refresh__text {
  padding: 8px 0;
}
</style>
