<!--
  JoinApprovalPendingView.vue — 셀프가입(회원가입) 승인 대기/거부 안내 화면 (앱, 비보호 라우트)
  - 작업 ID: 소정-12 (지시서 §셀프가입 승인/거부 화면 신설, plan §2)
  - 참조 패턴: views/login/DailyEntryPendingView.vue (일용직 입장 승인 대기/거부 안내) 그대로 미러.
  - 진입: LoginView 가 로그인 응답에서 셀프가입 승인 상태를 판별하면
      router.replace({ path:'/JoinApprovalPending', state:{ status:'PENDING'|'REJECTED' } })
      (판별 규칙·BE 계약 가정은 utils/joinApproval.js 주석 참조)
  - 상태 원천: ACCOUNT_STATUS[SYS013] '06 가입승인대기' / '07 가입거부'
  - 보안: 거부 사유 상세는 노출하지 않는다(일용직 승인 화면과 동일 — 통합 안내).
      로그인 전 안내 전용이라 본 화면은 API 를 호출하지 않는다(토큰 없음).
-->
<template>
  <div class="join-pending-view">
    <main class="jp-body">
      <!-- 상태 아이콘 -->
      <div
        class="jp-icon"
        :class="isRejected ? 'jp-icon--rejected' : 'jp-icon--pending'"
        aria-hidden="true"
      >
        <!-- 대기: 시계 -->
        <svg
          v-if="!isRejected"
          width="40"
          height="40"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="12" cy="12" r="9" />
          <polyline points="12 7 12 12 15 14" />
        </svg>
        <!-- 거부: 차단 -->
        <svg
          v-else
          width="40"
          height="40"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="12" cy="12" r="9" />
          <line x1="5.5" y1="5.5" x2="18.5" y2="18.5" />
        </svg>
      </div>

      <!-- 대기 상태 -->
      <template v-if="!isRejected">
        <h1 class="jp-title">가입 승인 대기 중</h1>
        <p class="jp-desc">
          가입 신청이 접수되었습니다.<br />
          관리자가 승인하면 로그인할 수 있습니다.<br />
          <strong>승인 후 다시 로그인해 주세요.</strong>
        </p>
        <p class="jp-hint">승인 여부는 소속 부서 관리자에게 문의해 주세요.</p>
      </template>

      <!-- 거부 상태 (사유 상세 미노출 — 통합 메시지) -->
      <template v-else>
        <h1 class="jp-title">가입이 승인되지 않았습니다</h1>
        <p class="jp-desc">관리자에게 문의해 주세요.</p>
      </template>
    </main>

    <footer class="jp-ft">
      <button type="button" class="jp-ft__btn" @click="onGoLogin">로그인 화면으로</button>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import { JOIN_APPROVAL_REJECTED } from '@/utils/joinApproval'

const router = useRouter()

// 상태: 'PENDING'(대기) | 'REJECTED'(거부). history state 유실(새로고침) 시 PENDING 폴백.
const status = ref('PENDING')
const isRejected = computed(() => status.value === JOIN_APPROVAL_REJECTED)

onMounted(() => {
  const state = window.history.state || {}
  if (state.status === JOIN_APPROVAL_REJECTED) {
    status.value = JOIN_APPROVAL_REJECTED
  }
})

const onGoLogin = () => {
  // 로그인 전 안내 화면 → 로그인 화면 복귀(스택 대체)
  router.replace('/')
}
</script>

<style scoped>
.join-pending-view {
  /* 디자인 토큰 자급(앱 공통 세트 미러 — DailyEntryPendingView 동일 세트). 하드코딩 사용 금지 */
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

.jp-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: var(--space-xl) var(--space-lg);
  gap: var(--space-md);
}

.jp-icon {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-full);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-sm);
}
.jp-icon--pending {
  background: var(--color-pending-bg);
  color: var(--color-pending-text);
}
.jp-icon--rejected {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.jp-title {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.jp-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-secondary);
}

.jp-hint {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
  opacity: 0.8;
}

.jp-ft {
  padding: var(--space-sm) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  border-top: 0.5px solid var(--color-border-light);
}
.jp-ft__btn {
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
