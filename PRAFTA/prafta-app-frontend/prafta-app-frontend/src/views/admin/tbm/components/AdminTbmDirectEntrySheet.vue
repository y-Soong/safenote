<!--
  AdminTbmDirectEntrySheet.vue — 정규직 대리입실 검색 시트 (D-4)
  - 작업 ID: 051-04 (분해: 051-admin-tbm-statemachine-plan.md §3-2, §4 UI-051-02)
  - 트리거: 교육준비 화면(AdminTbmPrepView) "정규직 대리입실" → open=true.
  - 백엔드:
      GET  /appApi/admin/tbm/sessions/{sessionCd}/eligible-regulars?keyword=  (검색, E9)
      POST .../{sessionCd}/attendees/manager-direct { userCd }                (대리입실, E10 — 부모가 호출)
  - 동작: 이름/사번 검색 → 결과 리스트(이미 입실자 배지/비활성) → 행 "대리입실" → confirm(userCd) emit.
      실제 manager-direct POST 는 부모(PrepView)가 수행. 본 시트는 검색(E9)만 직접 호출.
  - 참조 패턴: AdminTbmContentPickSheet(검색+바텀시트 토큰 자급) + AdminTbmForceExitSheet(시트 골격).
  - developer R-A: props/emits + 토글 골격만 확보(import 깨짐 방지). 검색(onSearch, E9) 은 R-B → TODO 유지.
-->
<template>
  <transition name="de-sheet-fade">
    <div
      v-if="open"
      class="de-sheet__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="정규직 대리입실"
      @click.self="onClose"
    >
      <div class="de-sheet">
        <div class="de-sheet__handle" aria-hidden="true"></div>

        <header class="de-sheet__header">
          <h2 class="de-sheet__title">정규직 대리입실</h2>
          <button type="button" class="de-sheet__close" aria-label="닫기" @click="onClose">
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

        <div class="de-sheet__search">
          <input
            v-model="keyword"
            type="search"
            class="de-sheet__input"
            placeholder="이름 또는 사번으로 검색"
            @keyup.enter="onSearch"
          />
          <button type="button" class="de-sheet__search-btn" :disabled="searching" @click="onSearch">
            검색
          </button>
        </div>

        <div class="de-sheet__body">
          <p v-if="searching" class="de-sheet__state">검색 중…</p>
          <p v-else-if="searched && !results.length" class="de-sheet__state">
            검색 결과가 없어요
          </p>
          <p v-else-if="!searched" class="de-sheet__state">
            휴대전화 사용이 어려운 정규직을 검색해 대리입실하세요.
          </p>
          <ul v-else class="de-sheet__list">
            <li v-for="u in results" :key="u.userCd" class="de-sheet__row">
              <div class="de-sheet__row-main">
                <span class="de-sheet__row-name">{{ u.userNm || '-' }}</span>
                <span v-if="u.deptNm" class="de-sheet__row-dept">{{ u.deptNm }}</span>
              </div>
              <span v-if="u.alreadyEntered" class="de-sheet__row-badge">입실됨</span>
              <button
                v-else
                type="button"
                class="de-sheet__row-btn"
                :disabled="submitting"
                @click="onPick(u)"
              >
                대리입실
              </button>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch } from 'vue'

import api from '@/api/axios'

const props = defineProps({
  open: { type: Boolean, default: false },
  // 검색 API 호출용 세션코드(E9 path)
  sessionCd: { type: String, default: '' },
  // 부모가 manager-direct POST 중일 때 행 버튼 잠금
  submitting: { type: Boolean, default: false },
})

// close: 닫기 / confirm: 대리입실 확정(userCd) — 부모가 E10 호출
const emit = defineEmits(['close', 'confirm'])

const keyword = ref('')
const searching = ref(false)
const searched = ref(false)
const results = ref([]) // [{ userCd, userNm, deptNm, alreadyEntered }]

const onClose = () => emit('close')

// 검색(E9) — 본 시트가 직접 호출. 세션 사업장/노드 스코프 내 정규직(이름/사번) 검색.
const onSearch = async () => {
  if (searching.value || !props.sessionCd) return
  searching.value = true
  try {
    const { data } = await api.get(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(props.sessionCd)}/eligible-regulars`,
      { params: { keyword: keyword.value || undefined } },
    )
    results.value = Array.isArray(data?.users) ? data.users : []
  } catch (e) {
    console.error('[AdminTbmDirectEntrySheet] 후보 검색 실패:', e?.message)
    results.value = []
  } finally {
    searched.value = true
    searching.value = false
  }
}

// 행 선택 → 부모에 userCd 전달(부모가 manager-direct POST + 닫기/리스트 갱신)
const onPick = (u) => {
  if (!u?.userCd || u.alreadyEntered || props.submitting) return
  emit('confirm', u.userCd)
}

// 열릴 때 초기화
watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      keyword.value = ''
      results.value = []
      searched.value = false
    }
  },
)
</script>

<style scoped>
.de-sheet__dimmer {
  /* 토큰 자급(self-contained) — 시트 패턴(ForceExitSheet/ContentPickSheet 동일) */
  --color-surface: #ffffff;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-primary: #16a34a;
  --color-bg: #f9fafb;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-overlay: rgba(0, 0, 0, 0.45);
  --radius-sm: 6px;
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
  z-index: 130;
}
.de-sheet {
  width: 100%;
  max-width: 414px;
  background: var(--color-surface);
  border-top-left-radius: var(--radius-xl);
  border-top-right-radius: var(--radius-xl);
  padding: var(--space-sm) 0 calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  display: flex;
  flex-direction: column;
  max-height: 80vh;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Pretendard', 'Noto Sans KR',
    sans-serif;
}
.de-sheet__handle {
  width: 36px;
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  margin: var(--space-xs) auto var(--space-sm);
}
.de-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-xs) var(--space-lg) var(--space-sm);
}
.de-sheet__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.de-sheet__close {
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
.de-sheet__search {
  display: flex;
  gap: var(--space-sm);
  padding: 0 var(--space-lg) var(--space-sm);
}
.de-sheet__input {
  flex: 1;
  height: 44px;
  box-sizing: border-box;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
}
.de-sheet__input:focus {
  outline: none;
  border-color: var(--color-primary);
}
.de-sheet__search-btn {
  flex-shrink: 0;
  height: 44px;
  padding: 0 var(--space-lg);
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.de-sheet__search-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.de-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xs) var(--space-lg) var(--space-md);
}
.de-sheet__state {
  margin: var(--space-lg) 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
}
.de-sheet__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.de-sheet__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  padding: var(--space-md);
  background: var(--color-bg);
  border-radius: var(--radius-md);
}
.de-sheet__row-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.de-sheet__row-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.de-sheet__row-dept {
  font-size: 13px;
  color: var(--color-text-secondary);
  word-break: break-all;
}
.de-sheet__row-badge {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.de-sheet__row-btn {
  flex-shrink: 0;
  height: 36px;
  padding: 0 var(--space-md);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.de-sheet__row-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.de-sheet-fade-enter-active,
.de-sheet-fade-leave-active {
  transition: opacity 0.18s ease;
}
.de-sheet-fade-enter-from,
.de-sheet-fade-leave-to {
  opacity: 0;
}
</style>
