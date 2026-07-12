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
      :notification-count="noticeUnreadCount"
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
      <PullRefreshIndicator v-bind="indicatorProps" />

      <!-- 인사말 -->
      <div class="greeting">
        <p class="greeting__date">{{ todayDateText }}</p>
        <p class="greeting__msg">오늘도 좋은 하루 되세요</p>
      </div>

      <!-- 로딩 표시 (home-summary 응답 대기) -->
      <div v-if="isLoading" class="home-loading" aria-live="polite">불러오는 중...</div>

      <template v-else>
        <!-- 연차 변경/삭제 동의 배너(A) — 관리자 발의 미응답 요청 있을 때만 노출. 탭 시 동의 팝업. -->
        <button
          v-if="consentPendingCount > 0"
          type="button"
          class="consent-banner"
          @click="onConsentBannerClick"
        >
          <span class="consent-banner__icon" aria-hidden="true">!</span>
          <span class="consent-banner__text">
            관리자가 요청한 연차 변경/삭제 동의가
            <strong>{{ consentPendingCount }}</strong>건 있어요
          </span>
          <span class="consent-banner__cta" aria-hidden="true">확인 ▸</span>
        </button>

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
          :prev-day-checkout-pending="prevDayCheckoutPending"
          :prev-day-check-in-time="prevDayCheckInTime"
          @click:checkin="onCheckIn"
          @click:checkout="onCheckOut"
        />

        <!-- 근태 조회 — prafta-app-025 J1-4: 일용직(DAILY)은 스케줄/연차/승인요청 해당없음 → 카드 전체 숨김 -->
        <AttendanceSummaryCard
          v-if="!isDailyWorker"
          :remaining-days="remainingLeaveDays"
          :granted-days="grantedLeaveDays"
          :pending-count="approvalPendingCount"
          @click:detail="onAttdSummaryDetail"
          @click:leave="onLeaveClick"
          @click:approval="onApprovalClick"
        />

        <!-- 신청형 휴가 요약 (LEAVE_TYPE='01') — 잔여연차(법정/관리자부여)와 분리된 별도 카드. -->
        <!--   일용직(DAILY)은 연차 해당없음(잔여연차 카드와 동일 게이트), 보유 타입 0이면 미노출. -->
        <AppliedLeaveSummaryCard
          v-if="!isDailyWorker && appliedLeaveTypeCount > 0"
          :type-count="appliedLeaveTypeCount"
          :remaining-days="appliedLeaveRemainingDays"
          @click:detail="onLeaveClick"
        />

        <!-- 안전 활동 (안전점검 시작 + 위험성 발굴 + 아차사고 보고) — prafta-app-025 J1-1: 사건 관리 row 제거 -->
        <SafetyActivityCard
          :blocked="safetyBlocked"
          @click:detail="onSafetyDetail"
          @click:safety-check="onSafetyCheck"
          @click:risk-discovery="onRiskDiscovery"
          @click:near-miss-report="onNearMissReport"
        />

        <!-- TBM 참석 -->
        <TbmAttendCard
          :tbm-status="tbmStatus"
          :session-time="tbmSessionTime"
          :session-location="tbmSessionLocation"
          :session-leader="tbmSessionLeader"
          :attended-at="tbmAttendedAt"
          @click:detail="onTbmDetail"
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

    <!-- 하단 탭바 (prafta-app-025 J1-2: 공통 AppBottomTabBar 로 교체. 라우팅은 컴포넌트가 중앙화 처리) -->
    <AppBottomTabBar :active-tab="'home'" :tbm-badge-count="tbmBadgeCount" />

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

    <!-- 로그인(앱 진입) 공지 팝업 — /popup 결과 있으면 진입 시 오픈 (prafta-app-023-3) -->
    <NoticeLoginPopup
      v-model:open="noticePopupOpen"
      :items="noticePopupItems"
      @confirm="onNoticePopupConfirm"
      @snooze="onNoticePopupSnooze"
      @read="onNoticePopupRead"
      @closed="onNoticePopupClosed"
    />

    <!-- 연차 사용촉진 1차 로그인 안내 팝업 — active(inProgress) + loginNotified=false 시 1회 노출 (prafta-com-008-A-7) -->
    <LeavePromotionLoginPopup
      v-model:open="promoPopupOpen"
      :promotion="promoPopup"
      @register="onPromoRegister"
      @later="onPromoLater"
    />

    <!-- 관리자 발의 연차 변경/삭제 동의 팝업 — 진입 시 미응답 있으면 자동 오픈(B), 배너(A) 탭으로도 오픈 -->
    <LeaveChangeConsentPopup
      v-model:open="consentPopupOpen"
      :items="consentItems"
      :submitting="consentSubmitting"
      @agree="onConsentAgree"
      @reject="onConsentReject"
    />

    <!-- 자발 연차일 출근 확인 팝업 — 종일 연차일(isLeaveDay)에 출근 시도 시 노출 (prafta-com-008-B-6) -->
    <!--   확인 시에만 check-in 호출. 촉진 확정 연차면 서버가 ATTD_400_150 으로 차단(노무수령거부) → 별도 안내. -->
    <LeaveDayCheckInConfirmPopup
      v-model:open="leaveDayConfirmOpen"
      :work-ymd="leaveDayConfirmYmd"
      :submitting="leaveDayConfirmSubmitting"
      @confirm="onLeaveDayConfirm"
      @cancel="onLeaveDayCancel"
    />

    <!-- 소속이동 안내 시트 — 로그인 후 미확인 예약 있으면 노출(PRAFTA-WEB_001-5). 푸시 탭으로도 재오픈. -->
    <!--   z-index 가 공지/촉진 팝업(1000)보다 높아(1001) 안내가 우선 노출된다(소속이동 안내 → 공지 순서). -->
    <TransferNoticeSheet
      v-model:open="transferNoticeOpen"
      :notice="transferNotice"
      :acking="transferAcking"
      @ack="onTransferAck"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { usePullToRefresh } from '@/composables/usePullToRefresh'
import PullRefreshIndicator from '@/components/common/PullRefreshIndicator.vue'
import { requestGps } from '@/utils/gpsBridge'
import { loadKakaoMapScript } from '@/utils/kakaoMap'
import { isDailyWorker as isDailyWorkerFn } from '@/utils/employment'
import { formatMdDot } from '@/utils/approvalFormat'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { TRANSFER_NOTICE_OPEN_EVENT } from '@/utils/pushRouteBridge'

import HomeIcons from './components/HomeIcons.vue'
import HomeHeader from './components/HomeHeader.vue'
import AttendanceCard from './components/AttendanceCard.vue'
import AttendanceSummaryCard from './components/AttendanceSummaryCard.vue'
import AppliedLeaveSummaryCard from './components/AppliedLeaveSummaryCard.vue'
import SafetyActivityCard from './components/SafetyActivityCard.vue'
import TbmAttendCard from './components/TbmAttendCard.vue'
import NoticeListCard from './components/NoticeListCard.vue'
import NoticeLoginPopup from './components/NoticeLoginPopup.vue'
import LeavePromotionLoginPopup from './components/LeavePromotionLoginPopup.vue'
import LeaveChangeConsentPopup from './components/LeaveChangeConsentPopup.vue'
import TransferNoticeSheet from './components/TransferNoticeSheet.vue'
import AppBottomTabBar from '@/components/common/AppBottomTabBar.vue'
import OffsiteReasonSheet from '@/views/attd/components/OffsiteReasonSheet.vue'
import LeaveDayCheckInConfirmPopup from '@/views/attd/components/LeaveDayCheckInConfirmPopup.vue'

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

// prafta-app-025 J1-4: 일용직(EMPLOYMENT_TYPE='DAILY') 여부 — 근태조회 카드 숨김 판정.
//   세션값(gv_employmentType, 로그인 시 저장)을 그대로 사용(라운드트립 없음).
const isDailyWorker = computed(() => isDailyWorkerFn())

// prafta-app-023: 헤더 벨 배지는 미열람 공지 수(noticeUnreadCount)를 그대로 사용한다.
//   (별도 stub notificationCount 제거 — 공지 도메인 구축 완료로 실데이터 연결.)

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
// prafta-app-021: 전날 미퇴근 마감 대기 신호(home-summary attendance 확장).
const prevDayCheckoutPending = ref(false)
const prevDayCheckInTime = ref('') // HHMM
// prafta-com-008-B-6: 기준일 종일 연차일 여부(home-summary attendance.isLeaveDay).
//   true 면 출근 시도 시 자발 연차일 출근 확인 팝업을 먼저 노출한다(확인 시에만 check-in 호출).
const attdIsLeaveDay = ref(false)

// ───────────────────────────────────────────────────────────
// 근태 조회 카드 — home-summary leave / approval 매핑
// ───────────────────────────────────────────────────────────
const remainingLeaveDays = ref(0)
const grantedLeaveDays = ref(0)
const approvalPendingCount = ref(0)
// 연차 개편(표시): 신청형 휴가(LEAVE_TYPE='01') 요약 — 잔여연차(GRANT 그룹)와 분리. 서버 권위값 그대로.
const appliedLeaveTypeCount = ref(0)
const appliedLeaveRemainingDays = ref(0)

// ───────────────────────────────────────────────────────────
// 안전 활동 카드
// blocked = (attdStatus !== 'WORKING') — 근무중(WORKING)에만 허용 — PRAFTA-022
//   (출근 전 BEFORE_WORK·퇴근 후 OFF_WORK 모두 차단. 산업안전/근무시간 게이트 정책)
// ───────────────────────────────────────────────────────────
const safetyBlocked = computed(() => attdStatus.value !== 'WORKING')

// ───────────────────────────────────────────────────────────
// 아차사고 (prafta-app-012)
//   - "아차사고 보고"(근로자) 는 출근 게이트 미적용(즉시성) — 항상 노출.
//   - prafta-app-025 J1-1: "사건 관리"(관리자 목록) 진입점은 앱에서 제거(웹 전용). 관리자 모드는 J1-6.
//     이에 따라 isSafetyManager/SAFETY_MANAGER_AUTH_CODES 게이팅도 함께 제거.
// ───────────────────────────────────────────────────────────

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

// ───────────────────────────────────────────────────────────
// 공지사항 카드 — prafta-app-023-2: /appApi/notice01/my-notices 연동
//   notices = 응답 noticeList 를 카드 계약({noticeId,isImportant,title,displayTime,isRead})으로
//             변환 후 slice(0,3). noticeUnreadCount = 응답 unreadCount(전체 모수, slice 무관).
// ───────────────────────────────────────────────────────────
const notices = ref([])
const noticeUnreadCount = ref(0)

// 로그인(앱 진입) 공지 팝업 상태 — /appApi/notice01/popup 결과를 NoticeLoginPopup 으로 전달
const noticePopupOpen = ref(false)
const noticePopupItems = ref([])

// 연차 사용촉진 1차 로그인 안내 팝업 상태 (prafta-com-008-A-7)
//   GET /appApi/leavepromo01/active 결과(inProgress + loginNotifiedYn='N')일 때만 1회 노출.
//   promoPopup 은 LoginPopup 계약({ remainingDays, availTo }) 에 맞춘 가공값.
const promoPopupOpen = ref(false)
const promoPopup = ref(null)

// 관리자 발의 연차 변경/삭제 동의 — 앱 진입 시 미응답(REQUESTED) 있으면 자동 팝업(B) + 배너(A).
//   GET /appApi/leavechange/pending-consents 결과를 LeaveChangeConsentPopup 으로 전달.
const consentPopupOpen = ref(false)
const consentItems = ref([])
const consentSubmitting = ref(false)
const consentPendingCount = computed(() => consentItems.value.length)

// ───────────────────────────────────────────────────────────
// 소속이동 안내 시트 — 로그인 후 미확인 예약 자동 노출(PRAFTA-WEB_001-5).
//   GET /appApi/user01/my-transfer-notice → { hasNotice, reservation:{...} }.
//   확인 시 POST /appApi/user01/transfer-notice/ack { reservationId }.
//   advisory: ack 실패해도 시트는 닫는다(기능 차단 아님).
// ───────────────────────────────────────────────────────────
const transferNoticeOpen = ref(false)
const transferNotice = ref(null)
const transferAcking = ref(false)

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
  // prafta-app-021: 전날 미퇴근 마감 대기 신호(021-2 백엔드 필드명 그대로 사용).
  prevDayCheckoutPending.value = !!attd.prevDayCheckoutPending
  prevDayCheckInTime.value = attd.prevDayCheckInTime || ''
  // prafta-com-008-B-6: 종일 연차일 여부(출근 시 자발 연차일 확인 팝업 분기 근거).
  attdIsLeaveDay.value = !!attd.isLeaveDay
}

const applyLeave = (leave) => {
  remainingLeaveDays.value = leave?.remainingDays ?? 0
  grantedLeaveDays.value = leave?.grantedDays ?? 0
  // 연차 개편(표시): 신청형 휴가 요약(타입 수 + 총잔여) — GRANT 합산과 별개 필드.
  appliedLeaveTypeCount.value = leave?.appliedTypeCount ?? 0
  appliedLeaveRemainingDays.value = leave?.appliedRemainingDays ?? 0
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
    return
  }

  tbmSessionTime.value = tbm.openedTime || ''
  // 장소(location) 백엔드 소스 없음 → title 을 메타 표시에 사용 (계약서 §3, 카드 수정 최소화)
  tbmSessionLocation.value = tbm.title || ''
  tbmSessionLeader.value = tbm.presenterName || ''
  tbmAttendedAt.value = tbm.myEntryTime || ''

  // 카드 상태 산출 — PRAFTA-022: 근무중(WORKING)에만 참석 가능
  // - 내가 참석 완료 → ATTENDED
  // - 미참석 + 근무중 → AVAILABLE
  // - 미참석 + 근무중 아님(출근 전·퇴근 후) → BEFORE_CHECK_IN (의미="근무중 아님", 근무 중에만 참석 가능)
  // 종료(COMPLETED) 세션 가드: 교육이 끝난 세션은 더 이상 참석할 수 없으므로 미참석이라도 AVAILABLE 로 두지 않는다.
  //   (관리자 입실취소(물리삭제)로 출결행이 사라진 사용자는 미참석(NOT_ENTERED)으로 잡혀, 종료 세션에서도
  //    AVAILABLE 로 새어나가 바텀탭 배지가 잘못 뜨는 결함 방지 — home-summary 는 COMPLETED 세션까지 포함한다.)
  //   참석한 사람은 ATTENDED(참석 완료) 유지, 미참석자는 할 수 있는 액션이 없으므로 NONE(일정 없음 톤)으로 둔다.
  const sessionEnded = tbm.sessionStatus === 'COMPLETED'
  if (tbm.myAttendanceStatus === 'COMPLETED') {
    tbmStatus.value = 'ATTENDED'
  } else if (sessionEnded) {
    tbmStatus.value = 'NONE'
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
// prafta-app-023-2: 공지 — 메인 카드 목록 + 미열람 배지
//   GET /appApi/notice01/my-notices → { noticeList:[{noticeId,title,pinYn,insertDate,fileCnt,isUnread,isImportant}], unreadCount }
//   카드 계약(NoticeListCard)으로 변환: { noticeId, isImportant, title, displayTime, isRead }
//   - displayTime: insertDate('YYYY-MM-DD HH:mm')에서 'MM.DD' 만 추출(카드 메타 컬럼은 좁음, D1 점).
//   - isRead: !isUnread (카드/목록은 읽음 여부로 강조 토글).
//   - isImportant: 응답 isImportant(=pinYn==='Y') 그대로 신뢰.
// ───────────────────────────────────────────────────────────
const toCardDisplayTime = (insertDate) => {
  // insertDate 형식 'YYYY-MM-DD HH:mm' (서버 가공). 카드 메타는 'MM.DD' 만 표시(D1 점 통일).
  if (!insertDate || typeof insertDate !== 'string') return ''
  const datePart = insertDate.split(' ')[0] // 'YYYY-MM-DD'
  return formatMdDot(datePart)
}

const toCardRow = (row) => ({
  noticeId: row.noticeId,
  isImportant: !!row.isImportant,
  title: row.title,
  displayTime: toCardDisplayTime(row.insertDate),
  isRead: !row.isUnread,
})

// 메인 카드/배지 로드. home-summary 와 독립 try/catch — 한쪽 실패가 다른 쪽을 막지 않는다.
const loadMyNotices = async () => {
  try {
    const { data } = await api.get('/appApi/notice01/my-notices')
    const list = Array.isArray(data?.noticeList) ? data.noticeList : []
    // 정렬은 백엔드 신뢰(고정 우선→최신). 카드는 최대 3행만(slice).
    notices.value = list.slice(0, 3).map(toCardRow)
    noticeUnreadCount.value = data?.unreadCount ?? 0
  } catch (e) {
    // 공지 로드 실패는 전체 화면 에러로 키우지 않고 카드 빈 상태로 폴백.
    console.warn('[MainView] my-notices 조회 실패(무시):', e?.message)
    notices.value = []
    noticeUnreadCount.value = 0
  }
}

// 로그인(앱 진입) 공지 팝업 로드 — POST /appApi/notice01/popup (바디 없음).
//   결과 있으면 NoticeLoginPopup 오픈(백엔드 정렬 순서 그대로 스택 표시).
const loadNoticePopup = async () => {
  try {
    const { data } = await api.post('/appApi/notice01/popup')
    const list = Array.isArray(data?.popupList) ? data.popupList : []
    noticePopupItems.value = list
    noticePopupOpen.value = list.length > 0
  } catch (e) {
    // 팝업 판정 실패는 조용히 무시(진입 차단 금지).
    console.warn('[MainView] notice popup 조회 실패(무시):', e?.message)
    noticePopupItems.value = []
    noticePopupOpen.value = false
  }
}

// ───────────────────────────────────────────────────────────
// prafta-app-023-3: 로그인 팝업 액션 — 각 ACK EP 호출(child 가 인덱스 진행 담당).
//   child(NoticeLoginPopup)는 emit 직후 자체적으로 다음 항목으로 advance() 한다.
//   여기서는 서버 ACK 만 비동기로 수행(실패해도 진행을 막지 않음 — UX 우선).
// ───────────────────────────────────────────────────────────
const onNoticePopupConfirm = async (noticeId) => {
  try {
    await api.post('/appApi/notice01/ack-confirm', { noticeId })
  } catch (e) {
    console.warn('[MainView] ack-confirm 실패(무시):', e?.message)
  }
}

const onNoticePopupSnooze = async (noticeId) => {
  try {
    await api.post('/appApi/notice01/ack-snooze', { noticeId })
  } catch (e) {
    // NOTICE_400_004(일용직/비고정) 등은 버튼 게이팅으로 정상 흐름에선 발생하지 않으나 방어.
    const message = e?.response?.data?.message
    if (message) showAlert(message)
    else console.warn('[MainView] ack-snooze 실패(무시):', e?.message)
  }
}

const onNoticePopupRead = async (noticeId) => {
  try {
    await api.post('/appApi/notice01/read', { noticeId })
  } catch (e) {
    console.warn('[MainView] notice read 실패(무시):', e?.message)
  }
}

// 팝업 전체 종료 — 닫힌 뒤 카드/배지를 최신(읽음 반영)으로 재동기화.
const onNoticePopupClosed = () => {
  loadMyNotices()
}

// ───────────────────────────────────────────────────────────
// prafta-com-008-A-7: 연차 사용촉진 1차 로그인 안내
//   GET /appApi/leavepromo01/active → 진행 중(inProgress) + 미안내(loginNotifiedYn='N')면
//   팝업 1회 노출 후 POST /appApi/leavepromo01/notified 로 플래그 갱신(다음 로그인부터 미노출).
// ───────────────────────────────────────────────────────────
const loadPromotionActive = async () => {
  try {
    const { data } = await api.get('/appApi/leavepromo01/active')
    // 진행 중 1차 촉진 없으면 미노출.
    if (!data || data.inProgress !== true) {
      promoPopup.value = null
      promoPopupOpen.value = false
      return
    }
    // 이미 안내(Y)했으면 다시 띄우지 않음(확정-3).
    if (data.loginNotifiedYn === 'Y') {
      promoPopup.value = null
      promoPopupOpen.value = false
      return
    }
    // LoginPopup 계약에 맞춰 가공(remainingDays + availTo=baseAvailToDate).
    promoPopup.value = {
      remainingDays: data.remainingDays,
      availTo: data.baseAvailToDate || '',
    }
    promoPopupOpen.value = true
    // 1회 노출 즉시 플래그 갱신(노출 자체로 안내 완료 처리 — 실패해도 진입 막지 않음).
    try {
      await api.post('/appApi/leavepromo01/notified')
    } catch (e) {
      console.warn('[MainView] leavepromo notified 갱신 실패(무시):', e?.message)
    }
  } catch (e) {
    // 촉진 조회 실패는 조용히 무시(진입 차단 금지).
    console.warn('[MainView] leavepromo active 조회 실패(무시):', e?.message)
    promoPopup.value = null
    promoPopupOpen.value = false
  }
}

// "계획 등록" → 계획서 화면으로 이동(팝업은 자체 close).
const onPromoRegister = () => {
  router.push('/LeavePromotionPlan')
}

// "나중에"/닫기 → 계획 미제출 이탈 1회 경고(확정-3). 취소면 팝업 재노출.
const onPromoLater = async () => {
  const ok = await askConfirm(
    '1차 촉진 기간에 계획서를 제출하지 않으면 2차 촉진 때 남은 모든 연차 날짜를 회사가 직접 지정합니다. 그대로 종료하시겠습니까?',
  )
  if (!ok) {
    // 머무름 — 팝업 다시 노출.
    promoPopupOpen.value = true
  }
}

// ───────────────────────────────────────────────────────────
// 관리자 발의 연차 변경/삭제 동의 — 진입 시 미응답 요청 자동 조회.
//   GET /appApi/leavechange/pending-consents → 미응답(REQUESTED) 목록.
//   결과 있으면 배너(A) 노출 + 진입 1회 자동 팝업(B). 연차/월차 구분 없이 모두 대상.
// ───────────────────────────────────────────────────────────
const loadPendingConsents = async ({ autoOpen = false } = {}) => {
  try {
    const { data } = await api.get('/appApi/leavechange/pending-consents')
    const list = Array.isArray(data?.list) ? data.list : []
    consentItems.value = list
    // 진입(onMounted) 시에만 자동 오픈. 응답 후 재조회 시엔 남은 건이 없으면 닫고, 있으면 유지.
    if (autoOpen) {
      consentPopupOpen.value = list.length > 0
    } else if (list.length === 0) {
      consentPopupOpen.value = false
    }
  } catch (e) {
    // 조회 실패는 조용히 무시(진입 차단 금지).
    console.warn('[MainView] 연차 변경 동의 목록 조회 실패(무시):', e?.message)
    consentItems.value = []
    consentPopupOpen.value = false
  }
}

// 배너(A) 탭 → 동의 팝업 오픈.
const onConsentBannerClick = () => {
  if (consentPendingCount.value > 0) consentPopupOpen.value = true
}

// 동의(AGREE) → POST respond. 성공 시 목록 재조회(남은 건 없으면 팝업 자동 닫힘).
const onConsentAgree = async (changeReqId) => {
  if (consentSubmitting.value) return
  consentSubmitting.value = true
  try {
    await api.post(`/appApi/leavechange/${changeReqId}/respond`, {
      WORKER_RESPONSE: 'AGREE',
    })
    await showAlert('동의했어요. 관리자 확인 후 반영됩니다.')
    await loadPendingConsents()
  } catch (e) {
    await showAlert(resolveApiErrorMessage(e, '처리에 실패했어요.'))
  } finally {
    consentSubmitting.value = false
  }
}

// 거부(REJECT, 사유필수) → POST respond. 성공 시 목록 재조회.
const onConsentReject = async ({ changeReqId, reason }) => {
  if (consentSubmitting.value) return
  consentSubmitting.value = true
  try {
    await api.post(`/appApi/leavechange/${changeReqId}/respond`, {
      WORKER_RESPONSE: 'REJECT',
      RESPONSE_REASON: reason,
    })
    await showAlert('거부했어요.')
    await loadPendingConsents()
  } catch (e) {
    await showAlert(resolveApiErrorMessage(e, '처리에 실패했어요.'))
  } finally {
    consentSubmitting.value = false
  }
}

// ───────────────────────────────────────────────────────────
// 소속이동 안내 시트 — 미확인 예약 조회/확인(PRAFTA-WEB_001-5).
//   본인(USER_CD=JWT) 미확인 예약만 서버가 도출(IDOR 방지). 실패는 조용히 무시(진입 차단 금지).
// ───────────────────────────────────────────────────────────
const loadTransferNotice = async () => {
  try {
    const { data } = await api.get('/appApi/user01/my-transfer-notice')
    if (data?.hasNotice && data?.reservation) {
      transferNotice.value = data.reservation
      transferNoticeOpen.value = true
    } else {
      transferNotice.value = null
      transferNoticeOpen.value = false
    }
  } catch (e) {
    // 안내 조회 실패는 진입을 막지 않는다(advisory).
    console.warn('[MainView] 소속이동 안내 조회 실패(무시):', e?.message)
    transferNotice.value = null
    transferNoticeOpen.value = false
  }
}

// 확인 → ack POST 후 시트 닫기. ack 실패해도 닫는다(advisory — 기능 차단 아님).
const onTransferAck = async (reservationId) => {
  if (transferAcking.value) return
  transferAcking.value = true
  try {
    if (reservationId) {
      await api.post('/appApi/user01/transfer-notice/ack', { reservationId })
    }
  } catch (e) {
    // ack 실패는 사용자 흐름을 막지 않는다(다음 로그인 시 재안내).
    console.warn('[MainView] 소속이동 안내 확인(ack) 실패(무시):', e?.message)
  } finally {
    transferAcking.value = false
    transferNoticeOpen.value = false
    transferNotice.value = null
  }
}

// 푸시 탭(open) 라우팅 신호 수신 → 안내 시트 재조회/재오픈(pushRouteBridge → window 이벤트).
const onTransferNoticeOpenEvent = () => {
  loadTransferNotice()
}

// ───────────────────────────────────────────────────────────
// 당겨서 새로고침 — 스크롤 최상단에서 아래로 더 당기면(overscroll) home-summary 등 재조회.
//   공통 컴포저블(usePullToRefresh)로 추출. 제스처/인디케이터 동작은 동일.
// ───────────────────────────────────────────────────────────
const mainEl = ref(null)
const { onPullStart, onPullMove, onPullEnd, indicatorProps } = usePullToRefresh(mainEl, async () => {
  applySessionHeader()
  // 홈 요약 + 공지 카드 + 연차 변경 동의 배너를 함께 갱신(각 실패는 자체 폴백/격리).
  //   새로고침에서는 동의 팝업 자동 오픈 안 함(배너 수만 갱신) — autoOpen 생략.
  await Promise.all([
    loadHomeSummary({ showLoading: false }),
    loadMyNotices(),
    loadPendingConsents(),
  ])
})

onMounted(() => {
  applySessionHeader()
  loadHomeSummary()
  // prafta-app-023-2: 공지 카드/배지 + 로그인 팝업을 home-summary 와 병행 로드(독립 실패 격리).
  loadMyNotices()
  loadNoticePopup()
  // prafta-com-008-A-7: 연차 사용촉진 1차 안내 팝업도 병행 로드(독립 실패 격리).
  loadPromotionActive()
  // 관리자 발의 연차 변경/삭제 동의 — 미응답 있으면 진입 시 자동 팝업(B)(독립 실패 격리).
  loadPendingConsents({ autoOpen: true })
  // PRAFTA-WEB_001-5: 소속이동 안내 — 미확인 예약 있으면 진입 시 자동 시트(독립 실패 격리).
  loadTransferNotice()
  // 푸시 탭(open) 라우팅 신호(window 이벤트) 수신 → 안내 시트 재오픈.
  window.addEventListener(TRANSFER_NOTICE_OPEN_EVENT, onTransferNoticeOpenEvent)
  // prafta-app-008: 외근 사유 시트(OffsiteReasonSheet)의 카카오 지도 SDK 를 미리 1회 로드해 둔다.
  // 시트를 열 때 네트워크로 SDK 를 받느라 시트 표시가 지연되는 문제 방지(프리로드).
  // 로드 함수는 중복 가드가 있어 idempotent 하며, 실패해도 시트의 좌표 텍스트 폴백이 동작하므로 조용히 무시.
  loadKakaoMapScript().catch((e) => {
    console.warn('[MainView] 카카오 지도 SDK 프리로드 실패(무시):', e?.message)
  })
})

onUnmounted(() => {
  // PRAFTA-WEB_001-5: 푸시 탭 라우팅 신호 리스너 해제(누수 방지).
  window.removeEventListener(TRANSFER_NOTICE_OPEN_EVENT, onTransferNoticeOpenEvent)
})

// prafta-app-010: 로그아웃은 마이페이지(/MyPage) 의 로그아웃 버튼으로 이전되었다.
// 기존 MainView.logout() 은 마이페이지 메인(MyPageView)으로 책임 이동.

// ───────────────────────────────────────────────────────────
// 이벤트 핸들러
// ───────────────────────────────────────────────────────────

// 헤더
const onBellClick = () => {
  // prafta-app-023: 헤더 벨은 미열람 공지 배지. 클릭 시 공지 전체목록으로 이동.
  router.push('/NoticeList')
}

const onAvatarClick = () => {
  // prafta-app-021: 우상단 아바타 → 푸시 알림 설정 화면으로 진입(라우팅 스왑).
  //   마이페이지(/MyPage) 진입은 하단 탭 '마이'(onTabClick)로 이전.
  router.push('/PushSetting')
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

// ───────────────────────────────────────────────────────────
// prafta-com-008-B-6: 자발 연차일 출근 확인 팝업 상태
//   종일 연차일(attdIsLeaveDay)에 출근 시도 시 본 팝업으로 1차 확인한 뒤에만 check-in 을 진행한다.
//   - 확인 시: startCheckInOut('checkIn', targetWorkSeq) 호출 → 서버가 자발은 허용, 촉진 확정 연차면
//     ATTD_400_150(노무수령거부)로 차단(callCheckInOut 의 catch 에서 §7 안내).
//   - 취소 시: 출근 중단(팝업 닫기).
// ───────────────────────────────────────────────────────────
const leaveDayConfirmOpen = ref(false)
const leaveDayConfirmYmd = ref('') // 팝업 표시용 대상일(YYYYMMDD). 없으면 오늘로 폴백.
const leaveDayConfirmSubmitting = ref(false)
// 확인 시 재개할 출근 컨텍스트(구간 선택값 보존).
let leaveDayPendingWorkSeq = null

// 오늘(YYYYMMDD) — 홈 화면은 기준일 미보유라 팝업 표시는 클라이언트 오늘로 폴백.
const todayYmd = () => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}${m}${day}`
}

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
    // prafta-com-008-B-6: 자발 연차일 확인 팝업이 열려 있었다면(출근 성공) 닫는다.
    leaveDayConfirmSubmitting.value = false
    leaveDayConfirmOpen.value = false
    leaveDayPendingWorkSeq = null
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

    // prafta-com-008-B-6: 노무수령거부 차단(촉진 확정 연차일 출근/퇴근). 재시도 불가 — §7 안내만.
    //   서버 message 에 §7 차단 문구(대상일 포함)가 담겨 있으므로 우선 노출, 없으면 폴백 문구.
    if (errorCode === 'ATTD_400_150') {
      console.warn(`[MainView] ${mode} 노무수령거부 차단(ATTD_400_150)`)
      pendingCtx = null
      // 자발 연차일 확인 팝업이 열려 있었다면 닫는다(차단이므로 재시도 없음).
      leaveDayConfirmSubmitting.value = false
      leaveDayConfirmOpen.value = false
      leaveDayPendingWorkSeq = null
      showAlert(
        message ||
          '오늘은 연차사용촉진으로 확정된 연차 사용일입니다. 회사는 금일 노무 제공을 수령하지 않으며, 출근(근무) 등록이 차단됩니다. 연차 변경이 필요하면 관리자에게 문의해 주세요.',
      )
      return
    }

    // 그 외 거부/실패(087 구간 미선택·088 구간 중복 포함) — 서버 message 우선 노출.
    console.error(`[MainView] ${mode} 실패:`, e?.message)
    pendingCtx = null
    // 확인 팝업이 열려 있었다면 진행 표시만 해제(팝업은 유지 — 재시도 가능).
    leaveDayConfirmSubmitting.value = false
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
//   prafta-com-008-B-6: skipConfirm=true 면 기본 출근 확인을 생략(자발 연차일 확인 팝업이 이미 확인을 받음).
const startCheckInOut = async (mode, targetWorkSeq = null, { skipConfirm = false } = {}) => {
  if (!skipConfirm) {
    let confirmMsg
    if (mode === 'checkOut') confirmMsg = '퇴근하시겠어요?'
    else if (targetWorkSeq === 1) confirmMsg = '1구간 출근하시겠어요?'
    else if (targetWorkSeq === 2) confirmMsg = '2구간 출근하시겠어요?'
    else confirmMsg = '출근하시겠어요?'
    const ok = await askConfirm(confirmMsg)
    if (!ok) return
  }
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
//   prafta-com-008-B-6: 종일 연차일(attdIsLeaveDay)이면 즉시 출근하지 않고 자발 연차일 확인 팝업을
//     먼저 띄운다(확인 시에만 check-in). 촉진 확정 연차면 서버가 ATTD_400_150 으로 차단한다.
const onCheckIn = async (payload) => {
  const targetWorkSeq =
    payload?.targetWorkSeq === 1 || payload?.targetWorkSeq === 2 ? payload.targetWorkSeq : null
  if (attdIsLeaveDay.value) {
    // 자발 연차일 출근 확인 팝업 오픈(확인 콜백에서 실제 출근 진행).
    leaveDayPendingWorkSeq = targetWorkSeq
    leaveDayConfirmYmd.value = todayYmd()
    leaveDayConfirmSubmitting.value = false
    leaveDayConfirmOpen.value = true
    return
  }
  await startCheckInOut('checkIn', targetWorkSeq)
}

// prafta-com-008-B-6: 자발 연차일 확인 → "출근하기" 클릭.
//   팝업을 낙관적으로 닫지 않고 진행 표시(submitting)만 켠 뒤 check-in 호출.
//   - 성공/ATTD_400_150(차단): callCheckInOut 가 팝업을 닫는다.
//   - 그 외 실패: submitting 만 해제(팝업 유지 → 재시도 가능).
const onLeaveDayConfirm = async () => {
  if (leaveDayConfirmSubmitting.value) return
  leaveDayConfirmSubmitting.value = true
  // skipConfirm: 팝업이 이미 사용자 확인을 받았으므로 기본 출근 확인은 생략.
  await startCheckInOut('checkIn', leaveDayPendingWorkSeq, { skipConfirm: true })
  // GPS 거부/측위 실패 등 callCheckInOut 미도달 경로에서도 버튼이 잠기지 않도록 진행 표시 해제.
  //   (성공/ATTD_400_150 경로는 callCheckInOut 가 팝업까지 닫으므로 이 해제는 무해한 멱등 처리.)
  leaveDayConfirmSubmitting.value = false
}

// prafta-com-008-B-6: 자발 연차일 확인 취소 → 출근 중단(팝업 자체 close 는 컴포넌트가 update:open 으로 처리).
const onLeaveDayCancel = () => {
  leaveDayConfirmSubmitting.value = false
  leaveDayPendingWorkSeq = null
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

// 안전 활동 — prafta-app-025 J1-10 B-6: 본인 안전활동 이력 화면(/MySafetyHistory)으로 진입.
//   진입점 역할 분리: 하단 '안전' 탭(AppBottomTabBar) = /SafetyHub(허브, 불변),
//   본 카드 헤더 ">"(onSafetyDetail) = 본인 이력 조회(순회점검 + 위험성평가, 시간순).
const onSafetyDetail = () => {
  router.push('/MySafetyHistory')
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

// TBM — 카드 `>` 진입: 사용자 TBM 허브(3탭) 화면으로 이동.
//   PRAFTA-022: 근무 중에만 TBM 참석 허용 — 출근 전·퇴근 후 진입 가드.
const onTbmDetail = async () => {
  if (attdStatus.value !== 'WORKING') {
    await showAlert('근무 중에만 TBM에 참석할 수 있어요. 출근 후 다시 시도해 주세요.')
    return
  }
  router.push('/TbmHub')
}

// 공지 — prafta-app-023-2: 카드 행/전체보기 라우팅.
const onNoticeMore = () => {
  // "전체보기" → 공지 전체목록 화면(prafta-app-023-5).
  router.push('/NoticeList')
}

const onNoticeRow = (noticeId) => {
  // 행 클릭 → 공지 상세 화면(prafta-app-023-4).
  if (!noticeId) return
  router.push({ path: '/NoticeDetail', query: { noticeId } })
}

// prafta-app-025 J1-2: 하단 탭 라우팅은 공통 AppBottomTabBar 가 중앙화 처리한다.
//   (home/attd/safety/tbm/my 목적지 매핑 + 현재 탭 재클릭 무동작). MainView 의 onTabClick 분기는 제거.
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
  --color-on-danger: #ffffff;
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

  /* 앱 셸은 뷰포트 높이로 '고정'한다(min-height 가 아님). 그래야 내부 .main 이
     실제 스크롤 컨테이너가 되고, 내용이 길어도 문서(body)로 스크롤이 새지 않는다.
     dvh 미지원 환경은 위의 vh 값으로 폴백. */
  height: 100vh;
  height: 100dvh;
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
  /* flex 자식의 기본 min-height:auto 는 내용이 길면 축소를 막아 overflow-y 스크롤이
     컨테이너 대신 문서로 새는 원인이 된다(스크롤 위치 오판 → 당겨서 새로고침 오작동).
     min-height:0 으로 .main 을 실제 스크롤 컨테이너로 고정한다. */
  min-height: 0;
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

/* 연차 변경/삭제 동의 배너(A) — 관리자 발의 미응답 요청 알림(경고 톤). 탭하면 동의 팝업. */
.consent-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 14px;
  margin-bottom: 12px;
  background: var(--color-warning-tint);
  border: 0.5px solid var(--color-warning);
  border-radius: var(--radius-md);
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}
.consent-banner__icon {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--color-warning);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
  text-align: center;
}
.consent-banner__text {
  flex: 1 1 auto;
  min-width: 0;
  font-size: 13px;
  line-height: 1.45;
  color: var(--color-warning-text-strong);
}
.consent-banner__text strong {
  font-weight: 700;
}
.consent-banner__cta {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-warning-text);
}

</style>
