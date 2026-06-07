<!--
  AdminTbmRiskPickSheet.vue — 위험성평가 다중선택 바텀시트 (R2-gap)
  - 작업 ID: 001-P5-T-F (분해: 001-phase5-admin-tbm-plan.md §2-2, §3-K risk-options)
  - 트리거: 개설 폼(AdminTbmCreateForm)의 "위험성평가 선택" 버튼이 open=true 로 띄움.
  - 백엔드: GET /appApi/admin/tbm/risk-options?siteCd= (T-K). 식별자/스코프는 서버(토큰).
  - 동작: 사업장(siteCd)의 위험성평가 목록을 체크박스 다중선택 + 검색 + 확인/닫기.
      확인 시 confirm([{ siteCd, processCd, assessmentCd, displayName }]) emit
      → 부모(개설 폼)가 riskRows 에 반영. 위험성평가는 복합키(siteCd|processCd|assessmentCd).
  - 참조 패턴: views/tbm/components/TbmEntryPwdSheet.vue (바텀시트 + 토큰 자급).
  - planner 라운드 스코프: template + style 완성. 선택 토글/검색 필터는 로컬 state(허용).
      ⚠️ 옵션 조회 API 호출은 developer(R3 프론트) — loadOptions 의 TODO 참조.
-->
<template>
  <transition name="pick-sheet-fade">
    <div
      v-if="open"
      class="pick-sheet__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="위험성평가 선택"
      @click.self="onClose"
    >
      <div class="pick-sheet">
        <div class="pick-sheet__handle" aria-hidden="true"></div>

        <header class="pick-sheet__header">
          <h2 class="pick-sheet__title">위험성평가 선택</h2>
          <button type="button" class="pick-sheet__close" aria-label="닫기" @click="onClose">
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <!-- 검색 -->
        <div class="pick-sheet__search">
          <input
            v-model.trim="keyword"
            class="pick-sheet__search-input"
            type="search"
            placeholder="위험성평가명 검색"
          />
        </div>

        <div class="pick-sheet__body">
          <!-- loading -->
          <p v-if="isLoading" class="pick-sheet__state">불러오는 중…</p>

          <!-- error -->
          <div v-else-if="loadError" class="pick-sheet__state">
            <p class="pick-sheet__state-msg">위험성평가를 불러오지 못했어요.</p>
            <button type="button" class="pick-sheet__retry" @click="onRetry">다시 시도</button>
          </div>

          <!-- empty -->
          <p v-else-if="!filteredOptions.length" class="pick-sheet__state">
            선택할 수 있는 위험성평가가 없어요
          </p>

          <!-- 목록(체크박스 다중선택) -->
          <ul v-else class="pick-list">
            <li v-for="opt in filteredOptions" :key="riskKey(opt)" class="pick-list__item">
              <label class="pick-item" :class="{ 'is-checked': isSelected(opt) }">
                <input
                  type="checkbox"
                  class="pick-item__check"
                  :checked="isSelected(opt)"
                  @change="onToggle(opt)"
                />
                <span class="pick-item__body">
                  <span class="pick-item__name">{{ opt.displayName || '(이름 미정)' }}</span>
                  <span v-if="opt.processNm || opt.assessmentStatusNm" class="pick-item__meta">
                    {{ [opt.processNm, opt.assessmentStatusNm].filter(Boolean).join(' · ') }}
                  </span>
                </span>
              </label>
            </li>
          </ul>
        </div>

        <footer class="pick-sheet__footer">
          <span class="pick-sheet__count">선택 {{ selectedKeys.length }}건</span>
          <button
            type="button"
            class="pick-sheet__btn pick-sheet__btn--primary"
            @click="onConfirm"
          >
            선택 완료
          </button>
        </footer>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import api from '@/api/axios'

const props = defineProps({
  // 시트 열림 여부(부모가 제어)
  open: { type: Boolean, default: false },
  // 옵션 조회 기준 사업장
  siteCd: { type: String, default: '' },
  // 이미 선택된 위험성평가([{ siteCd, processCd, assessmentCd }]) — 재진입 시 복원
  selected: { type: Array, default: () => [] },
})

// close: 닫기 / confirm: 선택 완료 → [{ siteCd, processCd, assessmentCd, displayName }]
const emit = defineEmits(['close', 'confirm'])

// ── 로컬 상태 ──────────────────────────────────────────────────────
const isLoading = ref(false)
const loadError = ref(false)
const keyword = ref('')
// 옵션 목록: [{ siteCd, processCd, assessmentCd, displayName, processNm, assessmentStatusNm }]
const options = ref([])
// 선택된 복합키 목록(UI 토글 전용)
const selectedKeys = ref([])

// 복합키(위험성평가 식별)
const riskKey = (r) => [r.siteCd, r.processCd, r.assessmentCd].join('|')

// 검색 필터(단순 부분일치 — UI 필터)
const filteredOptions = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return options.value
  return options.value.filter((o) => (o.displayName || '').toLowerCase().includes(kw))
})

const isSelected = (opt) => selectedKeys.value.includes(riskKey(opt))

// 체크 토글(다중선택)
const onToggle = (opt) => {
  const key = riskKey(opt)
  const idx = selectedKeys.value.indexOf(key)
  if (idx >= 0) selectedKeys.value.splice(idx, 1)
  else selectedKeys.value.push(key)
}

// 옵션 조회 — GET /appApi/admin/tbm/risk-options?siteCd=
// 응답 riskList([{ siteCd, processCd, assessmentCd, displayName, processNm, assessmentStatusNm, ... }])를 options 에 채운다.
const loadOptions = async () => {
  isLoading.value = true
  loadError.value = false
  try {
    const { data } = await api.get('/appApi/admin/tbm/risk-options', {
      params: { siteCd: props.siteCd },
    })
    options.value = Array.isArray(data?.riskList) ? data.riskList : []
  } catch (e) {
    console.error('[AdminTbmRiskPickSheet] 위험성평가 옵션 조회 실패:', e?.message)
    loadError.value = true
  } finally {
    isLoading.value = false
  }
}

const onRetry = () => loadOptions()

const onClose = () => emit('close')

// 선택 완료: 선택된 복합키를 옵션에서 매핑해 emit
const onConfirm = () => {
  const picked = options.value
    .filter((o) => selectedKeys.value.includes(riskKey(o)))
    .map((o) => ({
      siteCd: o.siteCd,
      processCd: o.processCd,
      assessmentCd: o.assessmentCd,
      displayName: o.displayName,
    }))
  emit('confirm', picked)
}

// 열릴 때: 선택 상태 복원 + 옵션 재조회
watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen) return
    keyword.value = ''
    selectedKeys.value = (props.selected || []).map((s) =>
      [s.siteCd, s.processCd, s.assessmentCd].join('|'),
    )
    loadOptions()
  },
)
</script>

<style scoped>
.pick-sheet__dimmer {
  /* 토큰 자급(self-contained): 개설 폼 위에 떠서 부모 토큰을 못 받을 수 있어 직접 선언 */
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-surface: #ffffff;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-bg: #f9fafb;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-overlay: rgba(0, 0, 0, 0.45);
  --radius-md: 10px;
  --radius-xl: 20px;
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  position: fixed;
  inset: 0;
  background: var(--color-overlay);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 120;
}
.pick-sheet {
  width: 100%;
  max-width: 414px;
  background: var(--color-surface);
  border-top-left-radius: var(--radius-xl);
  border-top-right-radius: var(--radius-xl);
  padding: var(--space-sm) 0 calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  max-height: 82vh;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}
.pick-sheet__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: var(--space-xs) auto var(--space-sm);
}
.pick-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-xs) var(--space-lg) var(--space-sm);
}
.pick-sheet__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.pick-sheet__close {
  width: 32px;
  height: 32px;
  background: transparent;
  border: 0;
  color: var(--color-text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.pick-sheet__search {
  padding: 0 var(--space-lg) var(--space-sm);
}
.pick-sheet__search-input {
  width: 100%;
  box-sizing: border-box;
  height: 40px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
}
.pick-sheet__search-input:focus {
  outline: none;
  border-color: var(--color-primary);
}
.pick-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: 0 var(--space-lg) var(--space-md);
}
.pick-sheet__state {
  margin: var(--space-lg) 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
}
.pick-sheet__state-msg {
  margin: 0 0 var(--space-sm);
}
.pick-sheet__retry {
  height: 36px;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1.5px solid var(--color-primary);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.pick-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.pick-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  cursor: pointer;
}
.pick-item.is-checked {
  border-color: var(--color-primary-tint-border);
  background: var(--color-primary-tint);
}
.pick-item__check {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  accent-color: var(--color-primary);
}
.pick-item__body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.pick-item__name {
  font-size: 14px;
  color: var(--color-text-primary);
  word-break: break-all;
}
.pick-item__meta {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.pick-sheet__footer {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-sm) var(--space-lg) 0;
  border-top: 0.5px solid var(--color-border-light);
}
.pick-sheet__count {
  font-size: 13px;
  color: var(--color-text-secondary);
}
.pick-sheet__btn {
  flex: 1;
  height: 48px;
  border: 0;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.pick-sheet__btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
}
.pick-sheet-fade-enter-active,
.pick-sheet-fade-leave-active {
  transition: opacity 0.18s ease;
}
.pick-sheet-fade-enter-from,
.pick-sheet-fade-leave-to {
  opacity: 0;
}
</style>
