<!--
  AttdApproverPickerSheet.vue — 결재자 검색/선택 바텀시트 (prafta-app-009, UI-009-2)
  - 참조 패턴: views/leave/components/LeaveApproverPickerSheet.vue (approver-search 호출)
  - D1 풀 공유: 결재자 검색은 기존 GET /appApi/leaveflow/approver-search 재사용(신규 endpoint 없음).
  - 시트 인프라: req 공통 BaseBottomSheet 재사용(신규 시트 인프라 금지).
  - v-model: open(시트 표시), props.excludedUserCds(이미 선택된 결재자 제외).
  - emit: add(picked[] = [{ userCd, userId, userNm, rankNm, nodeNm }]).
  - ⚠️ userCd 식별자 기준 체크/중복 판정. 위치 index 사용 금지.
-->
<template>
  <BaseBottomSheet v-model="open" title="결재자 추가">
    <div class="picker">
      <!-- 검색 -->
      <div class="picker__search">
        <svg class="picker__search-ic" width="18" height="18" viewBox="0 0 24 24" fill="none"
          stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
          aria-hidden="true">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          v-model="keyword"
          class="picker__search-input"
          type="text"
          placeholder="이름으로 검색"
          @input="onKeywordInput"
        />
      </div>

      <!-- 상태별 -->
      <p v-if="loading" class="picker__state">불러오는 중...</p>
      <p v-else-if="error" class="picker__state picker__state--err">
        결재자를 불러오지 못했어요. 다시 시도해 주세요.
      </p>
      <p v-else-if="results.length === 0" class="picker__state">검색 결과가 없어요</p>

      <!-- 결과 리스트 (다중 선택) -->
      <ul v-else class="picker__list">
        <li v-for="r in results" :key="r.userCd" class="picker__item">
          <label class="picker__check">
            <input
              type="checkbox"
              :checked="picked.has(r.userCd)"
              :disabled="excludedUserCds.includes(r.userCd)"
              @change="onToggle(r)"
            />
            <span class="picker__item-info">
              <span class="picker__item-name">{{ r.userNm }}</span>
              <span class="picker__item-meta">{{ metaOf(r) }}</span>
            </span>
            <span v-if="excludedUserCds.includes(r.userCd)" class="picker__item-tag">추가됨</span>
          </label>
        </li>
      </ul>

      <button v-if="hasNext && !loading" type="button" class="picker__more" @click="onLoadMore">
        더 보기
      </button>
    </div>

    <!-- sticky 추가 버튼 -->
    <template #footer>
      <button type="button" class="picker__apply" :disabled="picked.size === 0" @click="onApply">
        선택 추가{{ picked.size > 0 ? ` (${picked.size})` : '' }}
      </button>
    </template>
  </BaseBottomSheet>
</template>

<script setup>
import { ref, computed, watch, getCurrentInstance } from 'vue'
import api from '@/api/axios'
import BaseBottomSheet from './BaseBottomSheet.vue'

const { proxy } = getCurrentInstance() || { proxy: null }
const showAlert = (m) => (proxy?.$alert ? proxy.$alert(m) : window.alert(m))

const PAGE_SIZE = 20
const DEBOUNCE_MS = 300

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  excludedUserCds: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'add'])

// 시트 open 양방향 (BaseBottomSheet v-model 프록시)
const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

// 검색 상태
const keyword = ref('')
const results = ref([]) // [{ userCd, userId, userNm, rankNm, nodeNm }]
const picked = ref(new Set()) // 선택된 userCd 집합
const loading = ref(false)
const error = ref(false)
const hasNext = ref(false)
const page = ref(0)

let debounceTimer = null

// 표시 헬퍼 (UI — 허용)
const metaOf = (r) => [r?.nodeNm, r?.rankNm, r?.userId ? `사번 ${r.userId}` : '']
  .filter(Boolean).join(' · ')

// 선택 토글 (UI 상태 — 허용). 제외 대상(이미 추가됨)은 무시.
const onToggle = (r) => {
  if (props.excludedUserCds.includes(r.userCd)) return
  const next = new Set(picked.value)
  if (next.has(r.userCd)) next.delete(r.userCd)
  else next.add(r.userCd)
  picked.value = next
}

// 결재자 검색 호출 — GET /appApi/leaveflow/approver-search?keyword=&page=&size=
//   append=false 면 results 교체(검색/오픈), true 면 누적(더보기). userCd dedup.
const loadCandidates = async (append) => {
  if (loading.value) return
  loading.value = true
  error.value = false
  try {
    const res = await api.get('/appApi/leaveflow/approver-search', {
      params: { keyword: keyword.value || '', page: page.value, size: PAGE_SIZE },
    })
    const list = res?.data?.approvers || []
    if (append) {
      const existing = new Set(results.value.map((c) => c.userCd))
      results.value = [...results.value, ...list.filter((c) => !existing.has(c.userCd))]
    } else {
      results.value = list
    }
    hasNext.value = Boolean(res?.data?.hasNext)
  } catch (e) {
    console.error('[AttdApproverPicker] 결재자 검색 실패:', e?.message)
    error.value = true
  } finally {
    loading.value = false
  }
}

// 검색어 입력 — 디바운스(300ms) 후 page=0 리셋하여 첫 페이지 조회.
const onKeywordInput = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    page.value = 0
    loadCandidates(false)
  }, DEBOUNCE_MS)
}

// 다음 페이지 append.
const onLoadMore = () => {
  page.value += 1
  loadCandidates(true)
}

// 선택 결재자 emit (results 에서 picked 만 추려 객체 배열로). 시트 닫기.
const onApply = () => {
  const selected = results.value.filter((r) => picked.value.has(r.userCd))
  if (selected.length === 0) {
    showAlert('추가할 결재자를 선택해 주세요.')
    return
  }
  emit('add', selected)
  picked.value = new Set()
  open.value = false
}

// 시트 오픈 시 검색어/페이지/체크/후보 초기화 후 첫 페이지 조회.
watch(
  () => props.modelValue,
  (isOpen) => {
    if (!isOpen) return
    keyword.value = ''
    page.value = 0
    results.value = []
    hasNext.value = false
    error.value = false
    picked.value = new Set()
    loadCandidates(false)
  },
)
</script>

<style scoped>
.picker {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  padding: var(--space-sm) 0;
}
.picker__search {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  height: 44px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
}
.picker__search-ic {
  flex-shrink: 0;
  color: var(--color-text-tertiary);
}
.picker__search-input {
  flex: 1;
  min-width: 0;
  border: 0;
  background: transparent;
  font-size: 14px;
  color: var(--color-text-primary);
  font-family: inherit;
}
.picker__search-input:focus {
  outline: none;
}
.picker__state {
  margin: 0;
  padding: var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.picker__state--err {
  color: var(--color-danger);
}
.picker__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  max-height: 50vh;
  overflow-y: auto;
}
.picker__item {
  border-radius: var(--radius-md);
}
.picker__check {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  min-height: 48px;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
}
.picker__item-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}
.picker__item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.picker__item-meta {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.picker__item-tag {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.picker__more {
  height: 40px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
}
.picker__apply {
  width: 100%;
  height: 48px;
  background: var(--color-primary);
  border: 0;
  border-radius: var(--radius-md);
  color: var(--color-surface);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
}
.picker__apply:disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}
</style>
