<!--
  MyPageView.vue — 마이페이지 메인 (모바일 앱)
  - 작업 ID: PRAFTA-APP-010-10 (분해: .claude/requests/app_requests/prafta-app-010-plan.md)
  - UI 명세: UI-A010 (.claude/requests/app_requests/prafta-app-010-ui-spec.md)
  - 진입: MainView 우측 상단 아바타(onAvatarClick → router.push('/MyPage'))
  - planner 라운드 스코프: 프로필카드+메뉴2그룹+로그아웃버튼+탈퇴링크+버전 (template/style)
  - developer 라운드 스코프(아래 TODO): 프로필 조회(010-01, 마스킹), 로그아웃(010-06=기존 /comApi/login/logout 재사용 D3), 탈퇴(010-07), 라우팅
  - 디자인 토큰: MainView(.home-view)와 동일 세트를 .my-page-view 루트에 1회 선언. 하드코딩/Tailwind 금지.
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

    <!-- 본문 (스크롤 영역) -->
    <main class="mp-body">
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
        <p class="mp-version">PRAFTA SAFETY NOTE v1.0.0</p>
      </template>
    </main>

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
        <symbol id="i-mp-chev-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="9 18 15 12 9 6" />
        </symbol>
        <symbol id="i-mp-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol id="i-mp-logout" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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

import LogoutConfirmDialog from './components/LogoutConfirmDialog.vue'
import WithdrawalConfirmDialog from './components/WithdrawalConfirmDialog.vue'

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
const onProfileEdit = () => {
  router.push('/ProfileEdit')
}
const onPasswordChange = () => {
  router.push('/PasswordChange')
}
const onPresetManage = () => {
  router.push('/ApprovalPresetList')
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
    await showAlert('그동안 PRAFTA SAFETY NOTE를 이용해 주셔서 감사합니다.')
    await forceLogout()
    delete api.defaults.headers.common.Authorization
    try {
      userStore.logout()
    } catch (e) {
      console.warn('[MyPage] userStore logout skip:', e?.message)
    }
    router.replace('/')
  } catch (e) {
    const msg = e?.response?.data?.message || '탈퇴 처리 중 문제가 발생했어요. 잠시 후 다시 시도해 주세요.'
    showAlert(msg)
  } finally {
    isWithdrawing.value = false
  }
}

// ───────────────────────────────────────────────────────────
// 진입 시 1회 조회 (프로필 + presetCount). 401/403은 axios 인터셉터 처리.
// ───────────────────────────────────────────────────────────
onMounted(async () => {
  // GET /appApi/mypage/profile (마스킹 응답 D1). 메인 화면은 마스킹 PII를 사용하지 않고
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
    isLoading.value = false
  }
})
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
  min-height: 100vh;
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

/* 본문 */
.mp-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-sm) var(--space-lg) 40px;
}
.mp-loading {
  padding: 48px var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
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
