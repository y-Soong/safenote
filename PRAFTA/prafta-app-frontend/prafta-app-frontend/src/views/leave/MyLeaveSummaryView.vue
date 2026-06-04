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
    <main class="lv-body">
      <!-- 로딩 -->
      <div v-if="isLoading" class="lv-loading" aria-live="polite">불러오는 중...</div>

      <template v-else>
        <!-- 그룹 토글 (전체 / 법정 / 법정 외) -->
        <LeaveGroupToggle v-model="activeGroup" />

        <!-- 소멸 임박 콜아웃 (전체 토글 + 노출 조건 충족 + 미닫힘) -->
        <LeaveExpiryCallout v-if="showCallout" :info="expiringSoon" @close="onCalloutClose" />

        <!-- 메인 잔여 카드 -->
        <LeaveBalanceCard :label="balanceLabel" :group="currentGroup" />

        <!-- 3분할 KPI (부여 / 사용 / 사용예정) -->
        <LeaveSplitKpi :group="currentGroup" />

        <!-- 메타 카드 (입사일 / 근속 / 사용률) -->
        <LeaveMetaCard :user="user" :usage-rate="currentUsageRate" />

        <!-- 빈 상태 (로드 완료했으나 데이터 없음) -->
        <p v-if="showEmptyState" class="lv-empty">표시할 연차 정보가 없어요</p>
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

import LeaveGroupToggle from './components/LeaveGroupToggle.vue'
import LeaveExpiryCallout from './components/LeaveExpiryCallout.vue'
import LeaveBalanceCard from './components/LeaveBalanceCard.vue'
import LeaveSplitKpi from './components/LeaveSplitKpi.vue'
import LeaveMetaCard from './components/LeaveMetaCard.vue'

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

// 그룹 토글 (UI 상태 — 허용 범위). 진입 기본값: 전체
const activeGroup = ref('TOTAL')

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

// 빈 상태: 로드 완료 + 부여 데이터 전무
const showEmptyState = computed(() => !isLoading.value && !groups.value)

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
// 진입 시 1회 조회 (캐시 없음 — §3.6). 401/403/500 은 axios 인터셉터가 처리, 그 외만 안내.
// ───────────────────────────────────────────────────────────
onMounted(async () => {
  try {
    const res = await api.get('/appApi/leave01/my-leave-summary')
    user.value = res?.data?.user ?? null
    groups.value = res?.data?.groups ?? null
    expiringSoon.value = res?.data?.expiringSoon ?? null
  } catch (e) {
    console.error('[MyLeaveSummary] 연차 현황 조회 실패:', e?.message)
    showAlert('연차 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isLoading.value = false
  }
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
  min-height: 100vh;
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
