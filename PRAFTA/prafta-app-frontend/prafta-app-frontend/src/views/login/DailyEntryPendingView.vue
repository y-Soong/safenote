<!--
  DailyEntryPendingView.vue — 일용직 입장 승인 대기/거부 안내 화면 (앱, 비보호 라우트)
  - 분해: .claude/requests/common/작업지시서_일용직-계약서-서명-승인제.plan.md §4 UI-DC-02 / §2 T4
  - 요청서 근거: §4-1(대기/거부 안내), R4(푸시 불가 → 대기 안내 + 재로그인 시 자동 판정)
  - 진입: LoginView.fnSubmitDailyLogin catch 에서
      DAILYLOGIN_400_006 → router.replace({ path:'/DailyEntryPending', state:{ status:'PENDING' } })
      DAILYLOGIN_400_007 → state:{ status:'REJECTED' }
  - 보안: 거부 사유 상세 미노출(통합 메시지 — 요청서 §4-1). 본 화면은 로그인 전 안내 전용(API 호출 없음).
  - planner 라운드 스코프: template + style 완성. 상태 판별은 history state(새로고침 유실 시 PENDING 폴백).
  - developer 라운드 스코프(TODO): /DailyEntryPending 비보호 라우트 등록.
-->
<template>
  <div class="entry-pending-view">
    <main class="ep-body">
      <!-- 상태 아이콘 -->
      <div class="ep-icon" :class="isRejected ? 'ep-icon--rejected' : 'ep-icon--pending'" aria-hidden="true">
        <!-- 대기: 시계 -->
        <svg v-if="!isRejected" width="40" height="40" viewBox="0 0 24 24" fill="none"
          stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="9" />
          <polyline points="12 7 12 12 15 14" />
        </svg>
        <!-- 거부: 차단 -->
        <svg v-else width="40" height="40" viewBox="0 0 24 24" fill="none"
          stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="9" />
          <line x1="5.5" y1="5.5" x2="18.5" y2="18.5" />
        </svg>
      </div>

      <!-- 대기 상태 -->
      <template v-if="!isRejected">
        <h1 class="ep-title">관리자 승인 대기 중</h1>
        <p class="ep-desc">
          입장 승인 요청이 접수되었습니다.<br />
          관리자가 승인하면 로그인할 수 있습니다.<br />
          <strong>승인 후 다시 로그인해 주세요.</strong>
        </p>
        <p class="ep-hint">승인 요청은 당일 자정까지 유효합니다.</p>
      </template>

      <!-- 거부 상태 (사유 상세 미노출 — 통합 메시지) -->
      <template v-else>
        <h1 class="ep-title">입장이 승인되지 않았습니다</h1>
        <p class="ep-desc">
          관리자에게 문의해 주세요.
        </p>
      </template>
    </main>

    <footer class="ep-ft">
      <button type="button" class="ep-ft__btn" @click="onGoLogin">로그인 화면으로</button>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 상태: 'PENDING'(대기) | 'REJECTED'(거부). history state 유실(새로고침) 시 PENDING 폴백.
const status = ref('PENDING')
const isRejected = computed(() => status.value === 'REJECTED')

onMounted(() => {
  const state = window.history.state || {}
  if (state.status === 'REJECTED') {
    status.value = 'REJECTED'
  }
})

const onGoLogin = () => {
  // 로그인 전 안내 화면 → 로그인 화면 복귀(스택 대체)
  router.replace('/')
}
</script>

<style scoped>
.entry-pending-view {
  /* 디자인 토큰 자급(앱 공통 세트 미러) — 하드코딩 사용 금지 */
  --color-primary: #16a34a;
  --color-danger: #ef4444;
  --color-danger-bg: #fef2f2;
  --color-surface: #ffffff;
  --color-border-light: #f3f4f6;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-pending-bg: #fef3c7;
  --color-pending-text: #b45309;
  --radius-md: 10px;
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;
  --space-xl: 24px;

  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--color-surface);
}

.ep-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: var(--space-xl) var(--space-lg);
  gap: var(--space-md);
}

.ep-icon {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-full);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-sm);
}
.ep-icon--pending {
  background: var(--color-pending-bg);
  color: var(--color-pending-text);
}
.ep-icon--rejected {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.ep-title {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.ep-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-secondary);
}

.ep-hint {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
  opacity: 0.8;
}

.ep-ft {
  padding: var(--space-sm) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  border-top: 0.5px solid var(--color-border-light);
}
.ep-ft__btn {
  width: 100%;
  height: 48px;
  border: 0;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-surface);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
</style>
