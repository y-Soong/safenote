<!--
  SafetyInspectSavedView.vue — 점검 저장 완료 (prafta-app-011 화면 C, 신규)
  - 성공 아이콘 + 타이틀 + 요약({체크포인트명} · 양호N · 불량M) + [다른 개소 점검]/[메인으로].
  - 요약값은 ChkLst 저장 성공 시 query(chkptName/okCount/badCount)로 전달. 폴백: 빈 값/0.
-->
<template>
  <div class="saved-view">
    <!-- 헤더 -->
    <header class="sv-hd">
      <button type="button" class="sv-hd__back" aria-label="뒤로" @click="goHome">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-sv-chev-left" />
        </svg>
      </button>
      <h1 class="sv-hd__title">안전점검</h1>
      <span class="sv-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 -->
    <main class="sv-body">
      <div class="sv-ico" aria-hidden="true">
        <svg
          width="32"
          height="32"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
          <polyline points="22 4 12 14.01 9 11.01" />
        </svg>
      </div>
      <h2 class="sv-title">점검을 저장했어요</h2>
      <p class="sv-summary">
        {{ summaryLine }}<br />
        발견한 불량은 관리자에게 자동 전달돼요
      </p>
    </main>

    <!-- 푸터 -->
    <footer class="sv-footer">
      <button type="button" class="sv-btn sv-btn--secondary" @click="goScanAnother">
        다른 개소 점검
      </button>
      <button type="button" class="sv-btn sv-btn--primary" @click="goHome">메인으로</button>
    </footer>

    <!-- 인라인 SVG 스프라이트 -->
    <svg width="0" height="0" class="sv-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-sv-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const chkptName = computed(() => route.query.chkptName || '')
const okCount = computed(() => Number(route.query.okCount) || 0)
const badCount = computed(() => Number(route.query.badCount) || 0)

// 요약 라인 — 체크포인트명이 있을 때만 앞에 표기
const summaryLine = computed(() => {
  const counts = `양호 ${okCount.value}건 · 불량 ${badCount.value}건`
  return chkptName.value ? `${chkptName.value} · ${counts}` : counts
})

// 다른 개소 점검 → 화면 A(QR 스캐너)
const goScanAnother = () => {
  router.replace('/QrScanner')
}

// 메인으로
const goHome = () => {
  router.replace('/MainView')
}
</script>

<style scoped>
.saved-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;

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
.sv-hd {
  height: 56px;
  flex-shrink: 0;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  padding-top: env(safe-area-inset-top);
  border-bottom: 1px solid var(--color-border-light);
}
.sv-hd__back {
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
.sv-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
}
.sv-hd__spacer {
  width: 44px;
}

/* 본문 */
.sv-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  text-align: center;
  gap: 8px;
}
.sv-ico {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--color-primary-tint);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}
.sv-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.sv-summary {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-secondary);
  max-width: 280px;
}

/* 푸터 */
.sv-footer {
  flex-shrink: 0;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  display: flex;
  gap: 8px;
}
.sv-btn {
  flex: 1;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  font-family: inherit;
}
.sv-btn--primary {
  background: var(--color-primary);
  color: #ffffff;
}
.sv-btn--secondary {
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1.5px solid var(--color-primary);
}

.sv-sprite {
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
