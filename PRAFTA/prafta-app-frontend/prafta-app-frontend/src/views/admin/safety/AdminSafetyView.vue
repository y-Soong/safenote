<!--
  AdminSafetyView.vue — 관리자 모드 안전 관리 허브 (진입 카드 3개)
  - 작업 ID: prafta-app-025 J1-6 (.claude/requests/app_requests/job_1/J1-6-admin-safety.md)
  - 진입: AdminLauncherView 본문 SAFETY 섹션 / 하단 탭바 '안전' 탭 → router.push('/AdminSafety') (보호 라우트)
  - 권한 단일 출처: 001_관리자모드-권한매트릭스.md §3.2(SAFETY = master ∥ safe ∥ nodeAdmin-in-site)
      ⚠️ C1: 진입 게이팅은 access-context.moduleActiveMap.SAFETY(서버 산출)로 상위(AdminLauncher)에서 처리.
              본 화면은 클라이언트 역할(AUTH_CD) 분기 없음. 하위 화면 EP가 사업장 스코프를 서버에서 재강제.
  - 카드 3개: 순회점검 결과(조회) / 위험성평가(조회+상태전환) / 아차사고 관리(기존 화면 재사용).
      · 순회점검  → /AdminSafetyInspection (신규)
      · 위험성평가 → /AdminSafetyRisk        (신규)
      · 아차사고  → /NearMissManageList      (기존, prafta-app-012)
  - planner 라운드 스코프: template + style 완성. script 는 선언 + 단순 라우팅(정적)만.
  - 디자인 토큰: SafetyHubView/AdminLauncherView 세트를 .admin-safety-view 루트에 1회 선언. 하드코딩 금지.
-->
<template>
  <div class="admin-safety-view">
    <!-- 아이콘 스프라이트(본 화면 전역 use 참조 전제) -->
    <svg width="0" height="0" class="as-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-as-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol id="i-as-chev-right" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="9 18 15 12 9 6" />
        </symbol>
        <symbol id="i-as-clipboard" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="8" y="2" width="8" height="4" rx="1" /><path d="M9 4H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-3" />
        </symbol>
        <symbol id="i-as-warning" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" /><line x1="12" y1="9" x2="12" y2="13" /><line x1="12" y1="17" x2="12.01" y2="17" />
        </symbol>
        <symbol id="i-as-alert" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
        </symbol>
      </defs>
    </svg>

    <!-- 헤더: 관리자 모드(런처) 복귀 + 타이틀 -->
    <header class="as-hd">
      <button type="button" class="as-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true"><use href="#i-as-chev-left" /></svg>
      </button>
      <h1 class="as-hd__title">안전 관리</h1>
      <span class="as-hd__spacer" aria-hidden="true" />
    </header>

    <!-- 본문: 진입 카드 3개 -->
    <main class="as-body">
      <div class="action-list">
        <!-- 1) 순회점검 결과 (조회 전용) -->
        <button type="button" class="action-row" @click="onInspection">
          <svg class="icon row-icon" width="22" height="22" aria-hidden="true"><use href="#i-as-clipboard" /></svg>
          <span class="row-text">
            <span class="row-label">순회점검 결과</span>
            <span class="row-desc">일자별 점검 결과 · 불량 사진/비고 확인</span>
          </span>
          <svg class="icon row-chev" width="18" height="18" aria-hidden="true"><use href="#i-as-chev-right" /></svg>
        </button>

        <!-- 2) 위험성평가 (조회 + 상태전환) -->
        <button type="button" class="action-row" @click="onRisk">
          <svg class="icon row-icon" width="22" height="22" aria-hidden="true"><use href="#i-as-warning" /></svg>
          <span class="row-text">
            <span class="row-label">위험성평가</span>
            <span class="row-desc">올라온 위험성평가 검토 · 상태 전환</span>
          </span>
          <svg class="icon row-chev" width="18" height="18" aria-hidden="true"><use href="#i-as-chev-right" /></svg>
        </button>

        <!-- 3) 아차사고 관리 (기존 화면 재사용) -->
        <button type="button" class="action-row" @click="onNearMiss">
          <svg class="icon row-icon" width="22" height="22" aria-hidden="true"><use href="#i-as-alert" /></svg>
          <span class="row-text">
            <span class="row-label">아차사고 관리</span>
            <span class="row-desc">현장 아차사고 접수 · 검토</span>
          </span>
          <svg class="icon row-chev" width="18" height="18" aria-hidden="true"><use href="#i-as-chev-right" /></svg>
        </button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

// 관리자 모드(런처) 복귀 — 히스토리 누적 방지 위해 replace.
const onBack = () => {
  router.replace('/AdminHome')
}

// 순회점검 결과 화면(신규).
const onInspection = () => {
  router.push('/AdminSafetyInspection')
}

// 위험성평가 화면(신규).
const onRisk = () => {
  router.push('/AdminSafetyRisk')
}

// 아차사고 관리(기존 화면 재사용 — prafta-app-012 nearmiss01).
//   사업장 권한(assertSiteAccess)은 서버가 최종 판정.
const onNearMiss = () => {
  router.push('/NearMissManageList')
}
</script>

<style scoped>
/* 디자인 토큰 1회 선언(SafetyHubView/AdminLauncherView 세트) — 자식 scoped 상속, 하드코딩 금지 */
.admin-safety-view {
  --color-primary: #16a34a;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-lg: 14px;

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

/* 헤더 */
.as-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 8px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
}
.as-hd__back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  min-height: 44px;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
}
.as-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
}
.as-hd__spacer {
  min-width: 44px;
}

/* 본문 */
.as-body {
  flex: 1;
  min-height: 0;
  padding: 16px;
  overflow-y: auto;
}

.action-list {
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 4px 16px;
}
.action-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 16px 2px;
  background: transparent;
  border: 0;
  border-top: 0.5px solid var(--color-border-light);
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.action-row:first-child {
  border-top: 0;
}
.row-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.row-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.row-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.row-icon {
  color: var(--color-primary);
}
.row-chev {
  color: var(--color-text-tertiary);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
.as-sprite {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
</style>
