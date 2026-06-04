<!--
  PresetApproverPickerSheet.vue — 결재자 추가 바텀시트 (프리셋 편집)
  - 작업 ID: PRAFTA-APP-010-22
  - 호출자: ApprovalPresetEditView (010-14)
  - 참조 패턴: OffsiteReasonSheet.vue / views/req/components/BaseBottomSheet.vue
    (transition + dimmer + v-model 오픈)
  - planner 라운드 스코프: 검색 input + 후보 리스트(다중 체크) + 푸터 (template/style)
  - developer 라운드 스코프(아래 TODO): 후보 조회(GET /appApi/mypage/approval-candidates),
    검색 필터, 선택 결과 emit. 본인 제외는 서버 필터.
-->
<template>
  <transition name="aps-fade">
    <div
      v-if="modelValue"
      class="aps__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="결재자 추가"
      @click.self="onCancel"
    >
      <div class="aps">
        <div class="aps__handle" aria-hidden="true"></div>

        <header class="aps__header">
          <h2 class="aps__title">결재자 추가</h2>
          <button type="button" class="aps__close" aria-label="닫기" @click="onCancel">
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
        <div class="aps__search">
          <input
            v-model="keyword"
            type="text"
            class="aps__search-input"
            placeholder="이름으로 검색"
          />
        </div>

        <!-- 후보 리스트 -->
        <div class="aps__body">
          <p v-if="isLoading" class="aps__state">불러오는 중...</p>
          <p v-else-if="filteredCandidates.length === 0" class="aps__state">검색 결과가 없어요</p>

          <button
            v-for="cand in filteredCandidates"
            :key="cand.userCd"
            type="button"
            class="aps__item"
            :class="{ 'aps__item--checked': isChecked(cand.userCd) }"
            :disabled="isExcluded(cand.userCd)"
            @click="onToggle(cand)"
          >
            <div class="aps__item-info">
              <p class="aps__item-name">{{ cand.userNm }}</p>
              <p class="aps__item-meta">{{ metaOf(cand) }}</p>
            </div>
            <span v-if="isExcluded(cand.userCd)" class="aps__item-tag">추가됨</span>
            <span v-else-if="isChecked(cand.userCd)" class="aps__item-check" aria-hidden="true"
              >✓</span
            >
          </button>
        </div>

        <!-- 푸터 -->
        <footer class="aps__footer">
          <button
            type="button"
            class="aps__add-btn"
            :class="{ 'aps__add-btn--off': checkedList.length === 0 }"
            :disabled="checkedList.length === 0"
            @click="onConfirm"
          >
            {{ checkedList.length > 0 ? `${checkedList.length}명 추가` : '추가' }}
          </button>
        </footer>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch, getCurrentInstance } from 'vue'

import api from '@/api/axios'

const { proxy } = getCurrentInstance() || { proxy: null }
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

const props = defineProps({
  // 시트 오픈 (v-model)
  modelValue: {
    type: Boolean,
    default: false,
  },
  // 이미 리스트에 추가된 결재자 userCd (중복 방지 disabled)
  excludedUserCds: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'add'])

// ───────────────────────────────────────────────────────────
// 상태
// ───────────────────────────────────────────────────────────
const isLoading = ref(false)
const keyword = ref('')

// 후보: [{ userCd, userNm, rankNm, nodeNm }]
const candidates = ref([])

// 체크된 userCd 집합 (로컬 — UI 상태)
const checkedSet = ref(new Set())

// ───────────────────────────────────────────────────────────
// 파생값 (단순 필터/표시 — 비즈니스 로직 아님)
// ───────────────────────────────────────────────────────────
const filteredCandidates = computed(() => {
  const kw = keyword.value.trim()
  if (!kw) return candidates.value
  return candidates.value.filter((c) => (c.userNm || '').includes(kw))
})

const checkedList = computed(() => candidates.value.filter((c) => checkedSet.value.has(c.userCd)))

const metaOf = (cand) => [cand.nodeNm, cand.rankNm].filter(Boolean).join(' · ')

const isExcluded = (userCd) => props.excludedUserCds.includes(userCd)
const isChecked = (userCd) => checkedSet.value.has(userCd)

// ───────────────────────────────────────────────────────────
// 토글 (UI — 허용)
// ───────────────────────────────────────────────────────────
const onToggle = (cand) => {
  if (isExcluded(cand.userCd)) return
  const next = new Set(checkedSet.value)
  if (next.has(cand.userCd)) next.delete(cand.userCd)
  else next.add(cand.userCd)
  checkedSet.value = next
}

// ───────────────────────────────────────────────────────────
// 푸터
// ───────────────────────────────────────────────────────────
const onConfirm = () => {
  // 선택한 후보를 부모(프리셋 편집)에 순서대로 전달
  emit('add', checkedList.value)
}
const onCancel = () => {
  emit('update:modelValue', false)
}

// ───────────────────────────────────────────────────────────
// 시트가 열릴 때 후보 조회 + 체크 초기화
// ───────────────────────────────────────────────────────────
const loadCandidates = async () => {
  // GET /appApi/mypage/approval-candidates (앱 전용 D2). 본인/system 제외·동일 사업장은 서버 필터.
  isLoading.value = true
  try {
    const { data } = await api.get('/appApi/mypage/approval-candidates')
    candidates.value = data?.candidates || []
  } catch (e) {
    console.warn('[PresetPicker] 결재자 후보 조회 실패:', e?.message)
    candidates.value = []
    showAlert('결재자 후보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isLoading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    checkedSet.value = new Set()
    keyword.value = ''
    loadCandidates()
  },
)
</script>

<style scoped>
.aps__dimmer {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.aps {
  width: 100%;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  background: var(--color-surface, #fff);
  border-radius: var(--radius-lg, 14px) var(--radius-lg, 14px) 0 0;
}
.aps__handle {
  width: 36px;
  height: 4px;
  margin: 8px auto 0;
  border-radius: var(--radius-full, 9999px);
  background: var(--color-border, #e5e7eb);
}
.aps__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px 4px;
}
.aps__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary, #111827);
}
.aps__close {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-secondary, #6b7280);
}

/* 검색 */
.aps__search {
  padding: 8px 16px;
}
.aps__search-input {
  width: 100%;
  height: 44px;
  padding: 0 12px;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-md, 10px);
  font-size: 15px;
  color: var(--color-text-primary, #111827);
  background: var(--color-surface, #fff);
  box-sizing: border-box;
  outline: none;
  font-family: inherit;
}
.aps__search-input:focus {
  border-color: var(--color-primary, #16a34a);
}

/* 후보 리스트 */
.aps__body {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 8px;
}
.aps__state {
  padding: 32px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary, #9ca3af);
}
.aps__item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 52px;
  padding: 8px 4px;
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--color-border-light, #f3f4f6);
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
.aps__item:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.aps__item-info {
  flex: 1;
  min-width: 0;
}
.aps__item-name {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary, #111827);
}
.aps__item-meta {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--color-text-secondary, #6b7280);
}
.aps__item-check {
  color: var(--color-primary, #16a34a);
  font-weight: 700;
}
.aps__item-tag {
  font-size: 12px;
  color: var(--color-text-tertiary, #9ca3af);
}

/* 푸터 */
.aps__footer {
  flex-shrink: 0;
  padding: 12px 16px;
  border-top: 1px solid var(--color-border-light, #f3f4f6);
}
.aps__add-btn {
  width: 100%;
  height: 48px;
  background: var(--color-primary, #16a34a);
  color: var(--color-surface, #fff);
  border: 0;
  border-radius: var(--radius-md, 10px);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.aps__add-btn--off {
  background: var(--color-border, #e5e7eb);
  color: var(--color-text-tertiary, #9ca3af);
  cursor: not-allowed;
}

/* transition */
.aps-fade-enter-active,
.aps-fade-leave-active {
  transition: opacity 0.2s ease;
}
.aps-fade-enter-from,
.aps-fade-leave-to {
  opacity: 0;
}
</style>
