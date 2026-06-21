<!--
  MyPageView.vue — 마이페이지 메인 (모바일 앱)
  - 작업 ID: PRAFTA-APP-010-10 (분해: .claude/requests/app_requests/prafta-app-010-plan.md)
  - UI 명세: UI-A010 (.claude/requests/app_requests/prafta-app-010-ui-spec.md)
  - 진입: MainView 우측 상단 아바타(onAvatarClick → router.push('/MyPage'))
  - planner 라운드 스코프: 프로필카드+메뉴2그룹+로그아웃버튼+탈퇴링크+버전 (template/style)
  - developer 라운드 스코프(아래 TODO): 프로필 조회(010-01, 마스킹), 로그아웃(010-06=기존 /comApi/login/logout 재사용 D3), 탈퇴(010-07), 라우팅
  - 디자인 토큰: MainView(.home-view)와 동일 세트를 .my-page-view 루트에 1회 선언. 하드코딩/Tailwind 금지.

  ── prafta-app-028 (planner 라운드) ──────────────────────────────────────────
  - UI 명세: UI-A028 (마이페이지 연차 요약 섹션 + 당겨서 새로고침)
  - 추가 1) 프로필 카드 아래 "연차 요약" 섹션(3 KPI: 남은/사용예정/사용, 일 단위).
           일용직(DAILY)은 미노출(MainView 잔여연차 카드와 동일 게이트). 섹션 탭 시 연차 현황(/MyLeaveSummaryView) 진입.
  - 추가 2) .mp-body 에 당겨서 새로고침(pull-to-refresh) — MainView .main 패턴 그대로 이식.
  - 데이터 매핑(단일 출처 = prafta-app-028 사용자 확정 사항):
      GET /appApi/leave01/my-leave-summary 응답 groups.TOTAL 기준
        · 남은 연차     = remaining (부여 − 총사용)
        · 사용예정 연차 = planned   (미래 확정 연차)
        · 사용 연차     = used      (총사용 − planned, 실제 소진분)
      → 신규 엔드포인트 없음. 기존 leave01/my-leave-summary 재사용. 그룹은 TOTAL 만(분리 토글 스코프 밖).
  - planner 라운드 스코프: 연차 요약 섹션 마크업 + scoped 스타일 + pull-to-refresh 인디케이터/핸들러 바인딩(template/style).
    script 의 API 호출/리팩터(조회 함수 묶기·pull 핸들러 본문·연차요약 매핑)는 아래 TODO(developer)로 표시만.
-->
<template>
  <div class="my-page-view">
    <!-- 헤더 -->
    <header class="mp-hd">
      <button type="button" class="mp-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-mp-chev-left" />
        </svg>
      </button>
      <h1 class="mp-hd__title">마이페이지</h1>
      <!-- Q12 확정: 알림 아이콘 미노출(공지/알림 도메인 미구축). 도메인 구축 후 일괄 도입. -->
      <span class="mp-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 (스크롤 영역) — prafta-app-028: 당겨서 새로고침 제스처 바인딩(MainView .main 동형) -->
    <main
      class="mp-body"
      ref="mpBodyEl"
      @touchstart.passive="onPullStart"
      @touchmove="onPullMove"
      @touchend="onPullEnd"
      @touchcancel="onPullEnd"
    >
      <!-- prafta-app-028: 당겨서 새로고침 인디케이터 — 스크롤 최상단(프로필 카드 위)에서 아래로 당기면 노출 -->
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

      <!-- 로딩 -->
      <div v-if="isLoading" class="mp-loading" aria-live="polite">불러오는 중...</div>

      <template v-else>
        <!-- 프로필 카드 (탭 불가, 정보 확인용) -->
        <section class="mp-profile">
          <div class="mp-profile__avatar" aria-hidden="true">{{ avatarInitial }}</div>
          <div class="mp-profile__info">
            <p class="mp-profile__name">{{ userNm }}</p>
            <p class="mp-profile__meta">{{ siteNm }} · {{ nodeNm }}</p>
          </div>
        </section>

        <!-- prafta-app-028: 연차 요약 섹션 (3 KPI: 남은/사용예정/사용, 일 단위) -->
        <!--   일용직(DAILY)은 연차 해당없음 → 미노출(MainView 잔여연차 카드와 동일 게이트). -->
        <!--   연차요약 로드 실패는 비치명적: leaveSummaryFailed 면 섹션 자체 미노출(전체 화면 에러로 키우지 않음). -->
        <section
          v-if="!isDailyWorker && !leaveSummaryFailed"
          class="mp-leave"
          role="button"
          tabindex="0"
          aria-label="연차 현황 보기"
          @click="onLeaveSummaryClick"
          @keydown.enter="onLeaveSummaryClick"
          @keydown.space.prevent="onLeaveSummaryClick"
        >
          <div class="mp-leave__hd">
            <span class="mp-leave__title">연차 요약</span>
            <svg class="icon mp-leave__chev" width="20" height="20" aria-hidden="true">
              <use href="#i-mp-chev-right" />
            </svg>
          </div>
          <div class="mp-leave__kpis">
            <div class="mp-leave__cell">
              <span class="mp-leave__lbl">남은 연차</span>
              <span
                class="mp-leave__val mp-leave__val--accent"
                :class="{ 'mp-leave__val--muted': leaveRemaining === 0 }"
              >
                {{ leaveRemainingText }}<span class="mp-leave__unit">일</span>
              </span>
            </div>
            <div class="mp-leave__cell">
              <span class="mp-leave__lbl">사용예정</span>
              <span class="mp-leave__val" :class="{ 'mp-leave__val--muted': leavePlanned === 0 }">
                {{ leavePlannedText }}<span class="mp-leave__unit">일</span>
              </span>
            </div>
            <div class="mp-leave__cell">
              <span class="mp-leave__lbl">사용</span>
              <span class="mp-leave__val" :class="{ 'mp-leave__val--muted': leaveUsed === 0 }">
                {{ leaveUsedText }}<span class="mp-leave__unit">일</span>
              </span>
            </div>
          </div>
        </section>

        <!-- 관리자 모드 진입점 (001-Phase1-F4) — 서버 access-context.canEnterAdmin 시에만 노출. -->
        <template v-if="canEnterAdmin">
          <p class="mp-group-label">관리자</p>
          <nav class="mp-menu">
            <button type="button" class="mp-menu__row" @click="onAdminMode">
              <span class="mp-menu__text">관리자 모드</span>
              <svg class="icon mp-menu__chev" width="20" height="20" aria-hidden="true">
                <use href="#i-mp-chev-right" />
              </svg>
            </button>
          </nav>
        </template>

        <!-- 계정 그룹 -->
        <p class="mp-group-label">계정</p>
        <nav class="mp-menu">
          <button type="button" class="mp-menu__row" @click="onProfileEdit">
            <span class="mp-menu__text">개인정보 수정</span>
            <svg class="icon mp-menu__chev" width="20" height="20" aria-hidden="true">
              <use href="#i-mp-chev-right" />
            </svg>
          </button>
          <button type="button" class="mp-menu__row" @click="onPasswordChange">
            <span class="mp-menu__text">비밀번호 변경</span>
            <svg class="icon mp-menu__chev" width="20" height="20" aria-hidden="true">
              <use href="#i-mp-chev-right" />
            </svg>
          </button>
        </nav>

        <!-- 결재 그룹 -->
        <p class="mp-group-label">결재</p>
        <nav class="mp-menu">
          <button type="button" class="mp-menu__row" @click="onPresetManage">
            <span class="mp-menu__text">연차 결재선 관리</span>
            <span class="mp-menu__meta">{{ presetCount }}개</span>
            <svg class="icon mp-menu__chev" width="20" height="20" aria-hidden="true">
              <use href="#i-mp-chev-right" />
            </svg>
          </button>
          <!-- 사용자연차결재-04: 연차 결재 관리(내가 결재자인 연차 대기/처리 내역) -->
          <button type="button" class="mp-menu__row" @click="onLeaveApproval">
            <span class="mp-menu__text">연차 결재 관리</span>
            <span v-if="pendingApprovalCount > 0" class="mp-menu__meta"
              >{{ pendingApprovalCount }}건 대기</span
            >
            <svg class="icon mp-menu__chev" width="20" height="20" aria-hidden="true">
              <use href="#i-mp-chev-right" />
            </svg>
          </button>
        </nav>

        <!-- 로그아웃 (풀폭 secondary 버튼) -->
        <button type="button" class="mp-logout" @click="onLogoutClick">
          <svg class="icon" width="18" height="18" aria-hidden="true">
            <use href="#i-mp-logout" />
          </svg>
          로그아웃
        </button>

        <!-- 회원 탈퇴 (텍스트 링크, tertiary) -->
        <button type="button" class="mp-withdraw" @click="onWithdrawClick">회원 탈퇴</button>

        <!-- 앱 버전 -->
        <p class="mp-version">PRAFTA SAFENOTE v1.0.0</p>
      </template>
    </main>

    <!-- 하단 탭바 (마이 활성) — prafta-app-025 J1-2: 마이 탭의 목적지 화면이므로 공통 탭바 장착. -->
    <AppBottomTabBar :active-tab="'my'" />

    <!-- 로그아웃 확인 모달 -->
    <LogoutConfirmDialog v-model="logoutDialogOpen" @confirm="onLogoutConfirm" />

    <!-- 회원 탈퇴 확인 모달 -->
    <WithdrawalConfirmDialog
      v-model="withdrawDialogOpen"
      :user-nm="userNm"
      @confirm="onWithdrawConfirm"
    />

    <!-- 인라인 SVG sprite (본 화면 전용) -->
    <svg width="0" height="0" class="mp-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-mp-chev-right"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="9 18 15 12 9 6" />
        </symbol>
        <symbol
          id="i-mp-chev-left"
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
          id="i-mp-logout"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
          <polyline points="16 17 21 12 16 7" />
          <line x1="21" y1="12" x2="9" y2="12" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { useUserStore } from '@/stores/userStore'
import { forceLogout } from '@/composables/useAuth'
// prafta-app-028: 일용직(DAILY) 게이트 — 연차 요약 섹션 노출 판정(MainView 잔여연차 카드와 동일 게이트).
import { isDailyWorker as isDailyWorkerFn } from '@/utils/employment'

import LogoutConfirmDialog from './components/LogoutConfirmDialog.vue'
import WithdrawalConfirmDialog from './components/WithdrawalConfirmDialog.vue'
import AppBottomTabBar from '@/components/common/AppBottomTabBar.vue'

const router = useRouter()
const userStore = useUserStore()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백 (앱 전역 $alert 우선) — MainView/MyLeaveSummaryView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ───────────────────────────────────────────────────────────
// 상태 (developer: 초기값/응답 주입 보완 필요)
// ───────────────────────────────────────────────────────────
const isLoading = ref(true)

// GET /appApi/mypage/profile (마스킹 응답 D1) 매핑 (userNm/siteNm/nodeNm/presetCount)
const userNm = ref('')
const siteNm = ref('')
const nodeNm = ref('')
const presetCount = ref(0)

// 사용자연차결재-04: "연차 결재 관리" 대기 건수 배지(경량 조회, 비치명적). 실패 시 0(미노출).
const pendingApprovalCount = ref(0)

// 001-Phase1-F4: 관리자 모드 진입점 노출 판정(서버 access-context.canEnterAdmin).
//   판정 출처를 별도 경량 엔드포인트로 분리하지 않고 진입판정 단일 출처(access-context)를 재사용한다
//   — 판정 일관성 유지 + 엔드포인트 중복 방지. 프로필 조회와 병렬 호출(아래 onMounted)로 추가 지연 최소화.
const canEnterAdmin = ref(false)

// ───────────────────────────────────────────────────────────
// prafta-app-028: 연차 요약(3 KPI) 상태
//   데이터 출처 = GET /appApi/leave01/my-leave-summary 응답 groups.TOTAL (단일 출처).
//     · 남은 연차     = remaining
//     · 사용예정 연차 = planned
//     · 사용 연차     = used
//   developer: loadLeaveSummary() 에서 groups.TOTAL 을 아래 ref 들에 주입한다(매핑 그대로).
//   leaveSummaryFailed: 연차요약 로드 실패 플래그(비치명적). true 면 섹션 자체 미노출(전체 화면 에러 금지).
// ───────────────────────────────────────────────────────────
const leaveRemaining = ref(0)
const leavePlanned = ref(0)
const leaveUsed = ref(0)
const leaveSummaryFailed = ref(false)

// 일용직(DAILY) 여부 — 연차 요약 섹션 노출 게이트(라운드트립 없이 세션값으로 판정).
const isDailyWorker = computed(() => isDailyWorkerFn())

// 0.5 단위 표기 (정수면 정수, 소수면 1자리) — LeaveSplitKpi.trimDays 와 동일 규칙.
const trimDays = (v) => {
  if (v == null) return '0'
  const n = Number(v)
  return Number.isInteger(n) ? String(n) : n.toFixed(1)
}
const leaveRemainingText = computed(() => trimDays(leaveRemaining.value))
const leavePlannedText = computed(() => trimDays(leavePlanned.value))
const leaveUsedText = computed(() => trimDays(leaveUsed.value))

// 모달 토글 (UI 상태 — 허용 범위)
const logoutDialogOpen = ref(false)
const withdrawDialogOpen = ref(false)

// 탈퇴 진행 중 (중복 제출 방지)
const isWithdrawing = ref(false)

// 아바타 이니셜 (이름 앞 2자, 빈값이면 '?')
const avatarInitial = computed(() => (userNm.value ? userNm.value.slice(0, 2) : '?'))

// ───────────────────────────────────────────────────────────
// 메뉴 라우팅 (UI 토글/이동 — 허용)
// ───────────────────────────────────────────────────────────
// 뒤로가기: 메인 화면(MainView)으로 복귀
const onBack = () => {
  router.push('/MainView')
}
// prafta-app-028: 연차 요약 섹션 탭 → 연차 현황(MyLeaveSummaryView) 진입.
const onLeaveSummaryClick = () => {
  router.push('/MyLeaveSummaryView')
}
const onProfileEdit = () => {
  router.push('/ProfileEdit')
}
const onPasswordChange = () => {
  router.push('/PasswordChange')
}
const onPresetManage = () => {
  router.push('/ApprovalPresetList')
}
// 사용자연차결재-04: 연차 결재 관리 진입(결재자 본인 스코프).
const onLeaveApproval = () => {
  router.push('/LeaveApproval')
}
// 001-Phase1-F4: 관리자 모드 진입(보호 라우트). 서버가 최종 진입 판정.
const onAdminMode = () => {
  router.push('/AdminHome')
}

// ───────────────────────────────────────────────────────────
// 로그아웃 / 탈퇴 모달 열기 (UI 토글 — 허용)
// ───────────────────────────────────────────────────────────
const onLogoutClick = () => {
  logoutDialogOpen.value = true
}
const onWithdrawClick = () => {
  withdrawDialogOpen.value = true
}

// ───────────────────────────────────────────────────────────
// 로그아웃 확정 — 모달 confirm 후 처리
// ───────────────────────────────────────────────────────────
const onLogoutConfirm = async () => {
  // D3: 기존 로그아웃 인프라 재사용. forceLogout()이 서버 로그아웃(/comApi/login/logout) +
  //   clearSession + removeRefreshToken 을 일괄 처리한다(@/composables/useAuth).
  await forceLogout()
  // axios 기본 Authorization 헤더 제거 + userStore 초기화
  delete api.defaults.headers.common.Authorization
  try {
    userStore.logout()
  } catch (e) {
    console.warn('[MyPage] userStore logout skip:', e?.message)
  }
  router.replace('/')
}

// ───────────────────────────────────────────────────────────
// 회원 탈퇴 확정 — 체크박스 게이트 통과 후 처리
// ───────────────────────────────────────────────────────────
const onWithdrawConfirm = async () => {
  // D5: 탈퇴는 연차 자동취소/결재자 알림을 하지 않는다(서버 트랜잭션도 동일).
  //   서버가 ACCOUNT_STATUS='03' + PII 마스킹 + 토큰 폐기까지 처리한다.
  if (isWithdrawing.value) return
  isWithdrawing.value = true
  try {
    await api.post('/appApi/auth/withdraw', { confirmed: true })
    // 인사말 후 로컬 세션/토큰 초기화 + 로그인 화면 이동.
    await showAlert('그동안 PRAFTA SAFENOTE를 이용해 주셔서 감사합니다.')
    await forceLogout()
    delete api.defaults.headers.common.Authorization
    try {
      userStore.logout()
    } catch (e) {
      console.warn('[MyPage] userStore logout skip:', e?.message)
    }
    router.replace('/')
  } catch (e) {
    const msg =
      e?.response?.data?.message || '탈퇴 처리 중 문제가 발생했어요. 잠시 후 다시 시도해 주세요.'
    showAlert(msg)
  } finally {
    isWithdrawing.value = false
  }
}

// ───────────────────────────────────────────────────────────
// 진입 시 1회 조회 (프로필 + presetCount). 401/403은 axios 인터셉터 처리.
// ───────────────────────────────────────────────────────────
// 001-Phase1-F4: 관리자 진입점 노출 판정(경량). 실패는 비치명적 → 미노출 폴백.
const loadAdminEntryFlag = async () => {
  try {
    const { data } = await api.get('/appApi/admin/access-context')
    canEnterAdmin.value = data?.canEnterAdmin === true
  } catch (e) {
    canEnterAdmin.value = false
    console.warn('[MyPage] 관리자 진입판정 실패:', e?.message)
  }
}

// 사용자연차결재-04: 연차 결재 대기 건수 배지(경량 조회). 비치명적 → 실패는 0 폴백(미노출).
const loadPendingApprovalCount = async () => {
  try {
    const { data } = await api.get('/appApi/leaveflow/approval/pending')
    pendingApprovalCount.value = data?.totalCount ?? 0
  } catch (e) {
    pendingApprovalCount.value = 0
    console.warn('[MyPage] 연차 결재 대기 건수 조회 실패:', e?.message)
  }
}

// ───────────────────────────────────────────────────────────
// prafta-app-028: 연차 요약 로드 (GET /appApi/leave01/my-leave-summary, 기존 EP 재사용)
//   비치명적: 실패하면 leaveSummaryFailed=true → 섹션 미노출(전체 화면 에러로 키우지 않음).
//   일용직(DAILY)은 호출 자체를 생략해도 무방(섹션이 isDailyWorker 게이트로 미노출).
//   ── developer 라운드 스코프 ──
// ───────────────────────────────────────────────────────────
const loadLeaveSummary = async () => {
  // 일용직(DAILY)은 연차 해당없음 → 조회 자체를 생략(섹션도 isDailyWorker 게이트로 미노출).
  if (isDailyWorker.value) return
  try {
    const { data } = await api.get('/appApi/leave01/my-leave-summary')
    const total = data?.groups?.TOTAL
    // 데이터 없음도 비치명적 미노출(섹션 자체를 숨김, 전체 화면 에러로 키우지 않음).
    if (!total) {
      leaveSummaryFailed.value = true
      return
    }
    leaveRemaining.value = total.remaining ?? 0 // 남은 연차(부여 − 총사용)
    leavePlanned.value = total.planned ?? 0 // 사용예정 연차(미래 확정 연차)
    leaveUsed.value = total.used ?? 0 // 사용 연차(실제 소진분)
    // 새로고침 재호출 대비: 성공 경로에서 실패 플래그를 명시적으로 리셋(이전 실패 상태 박제 방지).
    leaveSummaryFailed.value = false
  } catch (e) {
    // 비치명적: 섹션만 미노출. showAlert 금지(전체 화면 에러로 키우지 않음).
    leaveSummaryFailed.value = true
    console.warn('[MyPage] 연차 요약 조회 실패:', e?.message)
  }
}

// ───────────────────────────────────────────────────────────
// prafta-app-028: 진입/새로고침 공통 조회 묶음.
//   onMounted 와 pull-to-refresh(onPullEnd) 가 모두 호출한다.
//   { showLoading } — true: 최초 진입(전체 로딩 표시). false: 당겨서 새로고침(카드 유지 + 자체 인디케이터).
//   ── developer 라운드 스코프 ──
// ───────────────────────────────────────────────────────────
const loadAll = async ({ showLoading = true } = {}) => {
  // 최초 진입(showLoading=true)에서만 전체 로딩 표시. 당겨서 새로고침은 카드 유지 + 자체 인디케이터.
  if (showLoading) isLoading.value = true

  // 비치명적 3종은 먼저 병렬 시작(각자 내부에서 예외 흡수 → reject 없음).
  const adminP = loadAdminEntryFlag()
  const pendingP = loadPendingApprovalCount()
  const leaveP = loadLeaveSummary()

  // 프로필(주 데이터)은 try/catch 로 직접 처리.
  //   GET /appApi/mypage/profile (마스킹 응답 D1). 메인 화면은 마스킹 PII를 사용하지 않고
  //   이름/사업장/부서/프리셋개수만 표시한다.
  try {
    const { data } = await api.get('/appApi/mypage/profile')
    userNm.value = data?.userNm || ''
    siteNm.value = data?.siteNm || ''
    nodeNm.value = data?.nodeNm || ''
    presetCount.value = data?.presetCount ?? 0
  } catch (e) {
    // 401/403 등 토큰 에러는 axios 인터셉터가 처리. 그 외 실패는 세션값으로 최소 폴백 표시.
    console.warn('[MyPage] 프로필 조회 실패:', e?.message)
    userNm.value = sessionStorage.getItem('gv_userNm') || ''
    siteNm.value = sessionStorage.getItem('gv_siteNm') || ''
    nodeNm.value = sessionStorage.getItem('gv_nodeNm') || ''
    showAlert('정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    // 프로필 기준으로 로딩 표시를 먼저 푼다(아래 Promise.all 보다 앞).
    if (showLoading) isLoading.value = false
  }

  // 비치명적 3종 완료 대기(내부에서 예외 흡수되어 reject 없음).
  await Promise.all([adminP, pendingP, leaveP])
}

onMounted(() => {
  // prafta-app-028: 진입 시 프로필/관리자판정/대기건수/연차요약을 loadAll 로 일괄 조회(중복 호출 제거).
  loadAll()
})

// ───────────────────────────────────────────────────────────
// prafta-app-028: 당겨서 새로고침 — MainView .main 패턴 그대로 이식.
//   .mp-body 가 실제 스크롤 컨테이너(overflow-y:auto, flex:1, min-height:0)이며
//   스크롤 최상단(프로필 카드 위)에서 아래로 더 당기면 loadAll({showLoading:false}) 재조회.
//   상수/제스처 로직은 MainView 와 동일(PULL_THRESHOLD/MAX_PULL/PULL_ENGAGE_SLOP 등).
// ───────────────────────────────────────────────────────────
const mpBodyEl = ref(null)
const pullDistance = ref(0) // 현재 당김 거리(px, 인디케이터 높이)
const isRefreshing = ref(false) // 새로고침 진행 중
const isDragging = ref(false) // 손가락으로 당기는 중(애니메이션 토글용)
const PULL_THRESHOLD = 70 // 이 거리 이상 당기고 놓으면 새로고침
const MAX_PULL = 120 // 인디케이터 최대 높이

const pullReady = computed(() => pullDistance.value >= PULL_THRESHOLD)
const pullIndicatorHeight = computed(() => (isRefreshing.value ? 48 : pullDistance.value))

let touchStartY = 0
let tracking = false // 이 제스처를 추적 중인가(스크롤 컨테이너 최상단에서 시작했을 때만)
let pullArmed = false // 당겨서 새로고침 모드로 확정됐는가(확정 후에만 preventDefault)
// 방향 확정 데드존(px). 손가락을 댈 때 흔히 생기는 미세한 초기 떨림으로
// preventDefault 가 걸려 네이티브 스크롤 제스처 전체가 취소되는 버그를 막는다.
const PULL_ENGAGE_SLOP = 6

// 스크롤이 최상단에 닿았는지 판정(1px 오차 허용)
const isScrolledToTop = () => {
  const el = mpBodyEl.value
  if (!el) return false
  return el.scrollTop <= 0
}

const onPullStart = (e) => {
  if (isRefreshing.value) return
  // 매 제스처 상태 초기화. 추적은 스크롤 컨테이너 최상단에서만 시작.
  pullArmed = false
  tracking = isScrolledToTop()
  if (tracking) touchStartY = e.touches[0].clientY
}

const onPullMove = (e) => {
  if (!tracking || isRefreshing.value) return
  const delta = e.touches[0].clientY - touchStartY // 아래로 당기면 양수

  // 아직 당김 모드로 확정되지 않았다면: 데드존을 넘는 '첫 의미있는 이동'에서 방향을 확정한다.
  //   - 최상단에서 아래로 당긴 경우에만 새로고침 모드(pullArmed)로 진입.
  //   - 그 외(위로 스크롤 등)는 추적을 끊어 이후 preventDefault 가 절대 호출되지 않게 한다
  //     → 네이티브 스크롤 제스처가 보존된다(상단 붙음/스크롤 먹힘 버그 방지).
  if (!pullArmed) {
    if (Math.abs(delta) < PULL_ENGAGE_SLOP) return // 판단 보류(네이티브 스크롤 그대로 둠)
    if (delta > 0 && isScrolledToTop()) {
      pullArmed = true
    } else {
      tracking = false
      return
    }
  }

  isDragging.value = true
  pullDistance.value = Math.min(MAX_PULL, delta * 0.5) // 저항감
  // iOS 고무줄/추가 스크롤 억제(당김 모드로 확정된 경우에만)
  if (e.cancelable) e.preventDefault()
}

const onPullEnd = async () => {
  isDragging.value = false
  const wasArmed = pullArmed
  pullArmed = false
  if (!tracking) return
  tracking = false
  const shouldRefresh = wasArmed && pullDistance.value >= PULL_THRESHOLD
  pullDistance.value = 0
  if (!shouldRefresh || isRefreshing.value) return
  isRefreshing.value = true
  try {
    // prafta-app-028: 새로고침 시 프로필/관리자판정/대기건수/연차요약을 함께 갱신(비치명적 항목은 자체 폴백).
    await loadAll({ showLoading: false })
  } finally {
    isRefreshing.value = false
  }
}
</script>

<style scoped>
/*
 * 디자인 토큰 — MainView(.home-view)와 동일 세트를 본 화면 루트에 1회 선언.
 * 자식 컴포넌트(scoped)는 var(--...) 를 상속받아 사용. 하드코딩/Tailwind 금지.
 */
.my-page-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-danger: #ef4444;
  --color-on-danger: #ffffff;
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
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  position: relative;
  /* prafta-app-025 J1-2: 하단 공통 탭바를 뷰포트 바닥에 고정하기 위해 화면 높이를 뷰포트로 고정. */
  height: 100vh;
  height: 100dvh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.mp-hd {
  height: 56px;
  flex-shrink: 0;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
}
.mp-hd__back {
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
.mp-hd__title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.mp-hd__spacer {
  width: 44px;
}

/* 본문 — 하단 공통 탭바(72px)에 가려지지 않도록 하단 패딩(88px).
   prafta-app-028: min-height:0 으로 .mp-body 를 실제 스크롤 컨테이너로 고정(당겨서 새로고침 스크롤 위치 오판 방지). */
.mp-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: var(--space-sm) var(--space-lg) 88px;
}
.mp-loading {
  padding: 48px var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* prafta-app-028: 당겨서 새로고침 인디케이터 — MainView .pull-refresh 와 동형. 당김 거리에 따라 높이 증감. */
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
  padding: var(--space-sm) 0;
}

/* 프로필 카드 */
.mp-profile {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-lg);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: var(--space-lg);
}
.mp-profile__avatar {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: var(--radius-full);
  background: var(--color-primary-tint);
  color: var(--color-primary);
  border: 0.5px solid var(--color-primary-tint-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
}
.mp-profile__info {
  min-width: 0;
}
.mp-profile__name {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.mp-profile__meta {
  margin: 2px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* prafta-app-028: 연차 요약 섹션 (탭 가능 카드) */
.mp-leave {
  margin-bottom: var(--space-lg);
  padding: var(--space-lg);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  font-variant-numeric: tabular-nums;
}
.mp-leave__hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-md);
}
.mp-leave__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.mp-leave__chev {
  color: var(--color-text-tertiary);
}
.mp-leave__kpis {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
}
.mp-leave__cell {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  padding: 0 var(--space-xs);
  position: relative;
}
.mp-leave__cell + .mp-leave__cell::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 1px;
  background: var(--color-border-light);
}
.mp-leave__lbl {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.mp-leave__val {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
}
/* 남은 연차는 강조(primary). */
.mp-leave__val--accent {
  color: var(--color-primary);
}
/* 0값은 muted 로 노이즈 감소(LeaveSplitKpi 패턴). accent 보다 우선 적용. */
.mp-leave__val--muted {
  color: var(--color-text-tertiary);
}
.mp-leave__unit {
  margin-left: 2px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-tertiary);
}

/* 그룹 라벨 */
.mp-group-label {
  margin: var(--space-lg) 0 var(--space-sm) var(--space-xs);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

/* 메뉴 리스트 */
.mp-menu {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}
.mp-menu__row {
  width: 100%;
  min-height: 52px;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 0 var(--space-lg);
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--color-border-light);
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.mp-menu__row:last-child {
  border-bottom: 0;
}
.mp-menu__text {
  flex: 1;
  font-size: 15px;
  color: var(--color-text-primary);
}
.mp-menu__meta {
  font-size: 13px;
  color: var(--color-text-secondary);
}
.mp-menu__chev {
  color: var(--color-text-tertiary);
}

/* 로그아웃 버튼 (풀폭 secondary) */
.mp-logout {
  width: 100%;
  height: 48px;
  margin-top: var(--space-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: var(--color-surface);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

/* 회원 탈퇴 텍스트 링크 */
.mp-withdraw {
  display: block;
  margin: var(--space-lg) auto 0;
  padding: var(--space-sm);
  background: transparent;
  border: 0;
  color: var(--color-text-tertiary);
  font-size: 13px;
  font-weight: 500;
  text-decoration: underline;
  cursor: pointer;
  font-family: inherit;
}

/* 앱 버전 */
.mp-version {
  margin: var(--space-md) 0 0;
  text-align: center;
  font-size: 11px;
  color: var(--color-text-tertiary);
}

.mp-sprite {
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
