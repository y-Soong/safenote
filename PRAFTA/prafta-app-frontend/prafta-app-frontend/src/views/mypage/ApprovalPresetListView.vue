<!--
  ApprovalPresetListView.vue — 연차 결재선 관리 (프리셋 목록, 모바일 앱)
  - 작업 ID: PRAFTA-APP-010-13 (분해: .claude/requests/app_requests/prafta-app-010-plan.md)
  - UI 명세: UI-A013
  - planner 라운드 스코프: 안내노트 + 프리셋 카드 리스트 + 추가 카드 (template/style)
  - developer 라운드 스코프(아래 TODO): 목록 조회(010-05, 앱 전용 /appApi/mypage/approval-presets), 카드 탭/추가 라우팅
  - ⚠️ D2 확정: 프리셋은 web user04 재사용 없이 앱 전용 신규 모듈(com.prafta.app.mypage.mypage01)이 제공.
    앱 프리셋 응답(steps)에 결재자 활성여부 필드가 없어 "확인 필요" 배지는 1차 미노출(questions ③ 보류).
  - 디자인 토큰: MyLeaveSummaryView 세트를 .preset-list-view 루트에 1회 선언.
-->
<template>
  <div class="preset-list-view">
    <!-- 헤더 -->
    <header class="pl-hd">
      <button type="button" class="pl-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-pl-chev-left" />
        </svg>
      </button>
      <h1 class="pl-hd__title">연차 결재선 관리</h1>
      <span class="pl-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 -->
    <main class="pl-body">
      <!-- 안내 노트 -->
      <div class="pl-notice">
        연차 신청 시 기본 프리셋이 자동 적용됩니다.<br />
        신청 폼에서 다른 프리셋으로 전환할 수 있어요.
      </div>

      <!-- 로딩 -->
      <div v-if="isLoading" class="pl-loading" aria-live="polite">불러오는 중...</div>

      <template v-else>
        <!-- 빈 상태 -->
        <div v-if="presets.length === 0" class="pl-empty">
          <p class="pl-empty__title">아직 결재선이 없어요</p>
          <p class="pl-empty__desc">
            연차 신청 시 결재자를 직접 지정하거나, 미리 자주 쓰는 결재선을 등록해 두세요
          </p>
        </div>

        <!-- 프리셋 카드 리스트 -->
        <button
          v-for="preset in sortedPresets"
          :key="preset.presetId"
          type="button"
          class="pl-card"
          @click="onCardClick(preset)"
        >
          <div class="pl-card__top">
            <span v-if="preset.defaultYn === 'Y'" class="pl-card__badge">기본</span>
            <span class="pl-card__name">{{ preset.presetNm }}</span>
            <svg class="icon pl-card__edit" width="18" height="18" aria-hidden="true">
              <use href="#i-pl-edit" />
            </svg>
          </div>
          <p class="pl-card__summary">{{ summaryOf(preset) }}</p>
        </button>

        <!-- 새 프리셋 추가 (dashed) -->
        <button type="button" class="pl-add" @click="onAddClick">
          <svg class="icon" width="18" height="18" aria-hidden="true">
            <use href="#i-pl-plus" />
          </svg>
          새 프리셋 추가
        </button>
      </template>
    </main>

    <!-- 인라인 SVG sprite -->
    <svg width="0" height="0" class="pl-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-pl-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol id="i-pl-edit" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
        </symbol>
        <symbol id="i-pl-plus" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ───────────────────────────────────────────────────────────
// 상태
// ───────────────────────────────────────────────────────────
const isLoading = ref(true)

// GET /appApi/mypage/approval-presets (앱 전용 D2) 응답: [{ presetId, presetNm, defaultYn, steps:[{ userNm, ... }] }]
const presets = ref([])

// ───────────────────────────────────────────────────────────
// 파생 표시값 (단순 정렬/합성 — 비즈니스 로직 아님)
// ───────────────────────────────────────────────────────────
// 기본(DEFAULT_YN='Y') 우선 정렬
const sortedPresets = computed(() => {
  return [...presets.value].sort((a, b) => {
    if (a.defaultYn === 'Y' && b.defaultYn !== 'Y') return -1
    if (a.defaultYn !== 'Y' && b.defaultYn === 'Y') return 1
    return 0
  })
})

// 결재자 이름을 "→"로 합성 (steps.userNm)
const summaryOf = (preset) => {
  const steps = preset?.steps || []
  return steps.map((s) => s.userNm).filter(Boolean).join(' → ')
}

// ───────────────────────────────────────────────────────────
// 라우팅 (UI 이동 — 허용)
// ───────────────────────────────────────────────────────────
const onCardClick = (preset) => {
  // 프리셋 편집 화면 진입(수정 모드, presetId 전달)
  router.push({ path: '/ApprovalPresetEdit', query: { presetId: preset.presetId } })
}
const onAddClick = () => {
  // 프리셋 편집 화면 진입(신규 모드, presetId 없음)
  router.push('/ApprovalPresetEdit')
}
const onBack = () => {
  router.push('/MyPage')
}

// ───────────────────────────────────────────────────────────
// 진입 시 1회 조회 (010-05)
// ───────────────────────────────────────────────────────────
onMounted(async () => {
  // GET /appApi/mypage/approval-presets (앱 전용 D2)
  try {
    const { data } = await api.get('/appApi/mypage/approval-presets')
    presets.value = data?.presets || []
  } catch (e) {
    console.warn('[PresetList] 프리셋 목록 조회 실패:', e?.message)
    showAlert('결재선을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
.preset-list-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-info-strong: #1d4ed8;
  --color-info-tint: #eff6ff;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
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
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}

/* 헤더 */
.pl-hd {
  height: 56px;
  flex-shrink: 0;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  border-bottom: 1px solid var(--color-border-light);
}
.pl-hd__back {
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
.pl-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
}
.pl-hd__spacer {
  width: 44px;
}

/* 본문 */
.pl-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-lg) var(--space-lg) 40px;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.pl-notice {
  padding: var(--space-md);
  background: var(--color-info-tint);
  color: var(--color-info-strong);
  border-radius: var(--radius-md);
  font-size: 13px;
  line-height: 1.5;
}
.pl-loading {
  padding: 48px var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* 빈 상태 */
.pl-empty {
  padding: 40px var(--space-lg);
  text-align: center;
}
.pl-empty__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.pl-empty__desc {
  margin: var(--space-sm) 0 0;
  font-size: 13px;
  color: var(--color-text-tertiary);
  line-height: 1.5;
}

/* 프리셋 카드 */
.pl-card {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: var(--space-md) var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.pl-card__top {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.pl-card__badge {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 6px;
  background: var(--color-primary-tint);
  color: var(--color-primary);
  font-size: 11px;
  font-weight: 600;
  border-radius: 4px;
}
.pl-card__name {
  flex: 1;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.pl-card__edit {
  color: var(--color-text-tertiary);
}
.pl-card__summary {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 새 프리셋 추가 (dashed) */
.pl-add {
  width: 100%;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: transparent;
  color: var(--color-primary);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

.pl-sprite {
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
