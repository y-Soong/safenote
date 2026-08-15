<!--
  ApproverPickerSheet.vue — 결재자 검색/선택 바텀시트 (F-6 통합본, HB-14)
  - 대체 대상: views/leave/components/LeaveApproverPickerSheet.vue,
               views/mypage/components/PresetApproverPickerSheet.vue,
               views/req/components/AttdApproverPickerSheet.vue
  - 시트 인프라: BaseBottomSheet 재사용 (★신규 시트 인프라 금지 — AttdApproverPickerSheet.vue:5 원칙 승계)
  - props: modelValue(v-model), excludedUserCds, title, source
  - emit : add(picked[] = [{ userCd, userId, userNm, rankNm, nodeNm }])
  - ⚠️ userCd 식별자 기준 체크/중복 판정. 위치 index 사용 금지
        (프리셋은 emit 배열 순서가 STEP_NO 가 된다 — 재인덱싱 금지).
  - ⚠️ 배경 스크롤 잠금/해제는 BaseBottomSheet 소유. 이 컴포넌트에서 중복 구현 금지
        (과거 2회 재발 지점 — 잔결함 F-6).
  - ★ source: 3벌의 props/emits 계약은 동일했으나 조회 엔드포인트는 서로 달랐다(실측).
      'leaveflow'(기본) = GET /appApi/leaveflow/approver-search (keyword/page/size 서버 페이징·서버 검색)
      'mypage'          = GET /appApi/mypage/approval-candidates (전량 1회 조회·클라이언트 이름 필터)
      서버 필터 조건(본인/system 제외 등)이 엔드포인트마다 다르므로 화면별 데이터 출처는 보존한다.
-->
<template>
  <BaseBottomSheet v-model="open" :title="title">
    <div class="picker">
      <div class="picker__search">
        <svg
          class="picker__search-ic"
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
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

      <p v-if="loading" class="picker__state">불러오는 중...</p>
      <p v-else-if="error" class="picker__state picker__state--err">
        결재자를 불러오지 못했어요. 다시 시도해 주세요.
      </p>
      <p v-else-if="results.length === 0" class="picker__state">검색 결과가 없어요</p>

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

    <template #footer>
      <button type="button" class="picker__apply" :disabled="picked.size === 0" @click="onApply">
        선택 추가{{ picked.size > 0 ? ` (${picked.size})` : '' }}
      </button>
    </template>
  </BaseBottomSheet>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import api from '@/api/axios'
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue'

const PAGE_SIZE = 20
const DEBOUNCE_MS = 300

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // 이미 결재선/프리셋에 추가된 userCd (중복 방지 disabled)
  excludedUserCds: { type: Array, default: () => [] },
  title: { type: String, default: '결재자 추가' },
  // 후보 조회 출처: 'leaveflow'(서버 페이징) | 'mypage'(전량 조회 + 클라이언트 필터)
  source: { type: String, default: 'leaveflow' },
})
const emit = defineEmits(['update:modelValue', 'add'])

// 시트 open 양방향 (BaseBottomSheet v-model 프록시)
const open = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

// ── 반응형 상태 ──────────────────────────────────────────────────────────
const keyword = ref('')
// 서버에서 받은 원본 후보 목록(응답 순서 보존 — 재정렬/재인덱싱 금지)
const rows = ref([]) // [{ userCd, userId, userNm, rankNm, nodeNm }]
const picked = ref(new Set()) // 선택된 userCd 집합
const loading = ref(false)
const error = ref(false)
const hasNext = ref(false)
const page = ref(0)

let debounceTimer = null

// mypage 출처는 서버 검색/페이징이 없어 클라이언트에서 이름 필터링한다(기존 프리셋 시트 동작 보존).
const isClientFilter = computed(() => props.source === 'mypage')

// 화면에 노출할 목록 — leaveflow 는 서버 검색 결과 그대로, mypage 는 이름 부분일치 필터.
const results = computed(() => {
  if (!isClientFilter.value) return rows.value
  const kw = keyword.value.trim()
  if (!kw) return rows.value
  return rows.value.filter((r) => (r?.userNm || '').includes(kw))
})

// 표시 헬퍼 (UI 전용 — 허용)
const metaOf = (r) =>
  [r?.nodeNm, r?.rankNm, r?.userId ? `사번 ${r.userId}` : ''].filter(Boolean).join(' · ')

// 선택 토글 (UI 상태 — 허용). 제외 대상(이미 추가됨)은 무시.
const onToggle = (r) => {
  if (props.excludedUserCds.includes(r.userCd)) return
  const next = new Set(picked.value)
  if (next.has(r.userCd)) next.delete(r.userCd)
  else next.add(r.userCd)
  picked.value = next
}

// 후보 조회 — append=false 면 rows 교체(오픈/검색), true 면 누적(더보기). userCd dedup.
const loadCandidates = async (append) => {
  if (loading.value) return
  loading.value = true
  error.value = false
  try {
    if (isClientFilter.value) {
      // 프리셋 편집(앱 전용 D2): 본인/system 제외·동일 사업장은 서버 필터. 페이징 없음.
      const res = await api.get('/appApi/mypage/approval-candidates')
      rows.value = res?.data?.candidates || []
      hasNext.value = false
      return
    }
    const res = await api.get('/appApi/leaveflow/approver-search', {
      params: { keyword: keyword.value || '', page: page.value, size: PAGE_SIZE },
    })
    const list = res?.data?.approvers || []
    if (append) {
      const existing = new Set(rows.value.map((c) => c.userCd))
      rows.value = [...rows.value, ...list.filter((c) => !existing.has(c.userCd))]
    } else {
      rows.value = list
    }
    hasNext.value = Boolean(res?.data?.hasNext)
  } catch (e) {
    console.error('[ApproverPicker] 결재자 검색 실패:', e?.message)
    error.value = true
  } finally {
    loading.value = false
  }
}

// 검색어 입력 — 서버 검색(leaveflow)만 디바운스(300ms) 후 page=0 리셋하여 첫 페이지 조회.
//   클라이언트 필터(mypage)는 results computed 가 즉시 반영하므로 재조회하지 않는다.
const onKeywordInput = () => {
  if (isClientFilter.value) return
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

// 선택 결재자 emit — rows 순서를 그대로 유지해 추린다(프리셋 STEP_NO = 이 배열 순서). 시트 닫기.
const onApply = () => {
  const selected = rows.value.filter((r) => picked.value.has(r.userCd))
  if (selected.length === 0) return
  emit('add', selected)
  picked.value = new Set()
  open.value = false
}

// 시트 오픈 시 검색어/페이지/체크/후보 초기화 후 첫 페이지 조회.
watch(
  () => props.modelValue,
  (isOpen) => {
    if (!isOpen) return
    if (debounceTimer) clearTimeout(debounceTimer)
    keyword.value = ''
    page.value = 0
    rows.value = []
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
  gap: var(--space-sm, 8px);
  padding: var(--space-sm, 8px) 0;
}
.picker__search {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 8px);
  height: 44px;
  padding: 0 var(--space-md, 12px);
  background: var(--color-surface, #ffffff);
  border: 0.5px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-md, 10px);
}
.picker__search-ic {
  flex-shrink: 0;
  color: var(--color-text-tertiary, #9ca3af);
}
.picker__search-input {
  flex: 1;
  min-width: 0;
  border: 0;
  background: transparent;
  font-size: 14px;
  color: var(--color-text-primary, #111827);
  font-family: inherit;
}
.picker__search-input:focus {
  outline: none;
}
.picker__state {
  margin: 0;
  padding: var(--space-lg, 16px);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary, #9ca3af);
}
.picker__state--err {
  color: var(--color-danger, #ef4444);
}
.picker__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs, 4px);
  max-height: 50vh;
  overflow-y: auto;
}
.picker__item {
  border-radius: var(--radius-md, 10px);
}
.picker__check {
  display: flex;
  align-items: center;
  gap: var(--space-sm, 8px);
  min-height: 48px;
  padding: var(--space-sm, 8px) var(--space-md, 12px);
  background: var(--color-surface, #ffffff);
  border: 0.5px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-md, 10px);
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
  color: var(--color-text-primary, #111827);
}
.picker__item-meta {
  font-size: 12px;
  color: var(--color-text-secondary, #6b7280);
}
.picker__item-tag {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--color-text-tertiary, #9ca3af);
}
.picker__more {
  height: 40px;
  background: var(--color-surface, #ffffff);
  border: 0.5px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-md, 10px);
  color: var(--color-text-secondary, #6b7280);
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
}
.picker__apply {
  width: 100%;
  height: 48px;
  background: var(--color-primary, #16a34a);
  border: 0;
  border-radius: var(--radius-md, 10px);
  color: var(--color-surface, #ffffff);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
}
.picker__apply:disabled {
  background: var(--color-border, #e5e7eb);
  color: var(--color-text-tertiary, #9ca3af);
  cursor: not-allowed;
}
</style>
