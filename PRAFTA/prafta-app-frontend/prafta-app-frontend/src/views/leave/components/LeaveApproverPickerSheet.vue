<!--
  LeaveApproverPickerSheet.vue — 연차 결재자 추가 바텀시트 (prafta-app-018-C)
  - 분해: .claude/requests/app_requests/prafta-app-018-C-tasks.md
  - 참조 패턴: views/mypage/components/PresetApproverPickerSheet.vue
      (transition + dimmer + 검색 input + 다중 체크 + 푸터)
  - 차이점: 후보 조회 엔드포인트가 018-A GET /appApi/leaveflow/approver-search?keyword=&page=&size= (페이징 + 서버 필터).
  - props:
      modelValue       : 시트 오픈 (v-model)
      excludedUserCds  : 이미 결재선에 추가된 userCd (중복 방지 disabled)
  - emits:
      update:modelValue (닫기), add(picked[])  ← picked: [{ userCd, userId, userNm, rankNm, nodeNm }]
  - ⚠️ userCd 식별자 기준 체크/중복 판정. 위치 index 사용 금지.
-->
<template>
  <transition name="laps-fade">
    <div
      v-if="modelValue"
      class="laps__dimmer"
      role="dialog"
      aria-modal="true"
      aria-label="결재자 추가"
      @click.self="onCancel"
      @touchmove.self.prevent
    >
      <div class="laps">
        <div class="laps__handle" aria-hidden="true"></div>

        <header class="laps__header">
          <h2 class="laps__title">결재자 추가</h2>
          <button type="button" class="laps__close" aria-label="닫기" @click="onCancel">
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
        <div class="laps__search">
          <input
            v-model="keyword"
            type="text"
            class="laps__search-input"
            placeholder="이름으로 검색"
            @keyup.enter="onSearch"
          />
        </div>

        <!-- 후보 리스트 -->
        <div class="laps__body">
          <p v-if="isLoading" class="laps__state">불러오는 중...</p>
          <p v-else-if="candidates.length === 0" class="laps__state">검색 결과가 없어요</p>

          <button
            v-for="cand in candidates"
            :key="cand.userCd"
            type="button"
            class="laps__item"
            :class="{ 'laps__item--checked': isChecked(cand.userCd) }"
            :disabled="isExcluded(cand.userCd)"
            @click="onToggle(cand)"
          >
            <div class="laps__item-info">
              <p class="laps__item-name">{{ cand.userNm }}</p>
              <p class="laps__item-meta">{{ metaOf(cand) }}</p>
            </div>
            <span v-if="isExcluded(cand.userCd)" class="laps__item-tag">추가됨</span>
            <span v-else-if="isChecked(cand.userCd)" class="laps__item-check" aria-hidden="true"
              >✓</span
            >
          </button>

          <!-- 더 보기 (hasNext) -->
          <button v-if="hasNext && !isLoading" type="button" class="laps__more" @click="onLoadMore">
            더 보기
          </button>
        </div>

        <!-- 푸터 -->
        <footer class="laps__footer">
          <button
            type="button"
            class="laps__add-btn"
            :class="{ 'laps__add-btn--off': checkedList.length === 0 }"
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
import { ref, computed, watch, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import api from '@/api/axios'

const { proxy } = getCurrentInstance() || { proxy: null }
const showAlert = (m) => (proxy?.$alert ? proxy.$alert(m) : window.alert(m))

const PAGE_SIZE = 20

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // 이미 결재선에 추가된 userCd (중복 방지 disabled)
  excludedUserCds: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'add'])

// ── 반응형 상태 (developer: 조회/페이징/체크 초기화 로직 보완) ────────────
const isLoading = ref(false)
const keyword = ref('')
// 후보 누적: [{ userCd, userId, userNm, rankNm, nodeNm }]
const candidates = ref([])
const page = ref(0)
const hasNext = ref(false)
// 체크된 userCd 집합 (UI 상태)
const checkedSet = ref(new Set())

// ── 파생값 (단순 표시/필터 — UI) ─────────────────────────────────────────
const checkedList = computed(() => candidates.value.filter((c) => checkedSet.value.has(c.userCd)))
const metaOf = (cand) => [cand?.nodeNm, cand?.rankNm].filter(Boolean).join(' · ')
const isExcluded = (userCd) => props.excludedUserCds.includes(userCd)
const isChecked = (userCd) => checkedSet.value.has(userCd)

// ── 토글 (UI — 허용) ─────────────────────────────────────────────────────
const onToggle = (cand) => {
  if (isExcluded(cand.userCd)) return
  const next = new Set(checkedSet.value)
  if (next.has(cand.userCd)) next.delete(cand.userCd)
  else next.add(cand.userCd)
  checkedSet.value = next
}

// ── 검색/페이징 (approver-search 호출) ───────────────────────────────────
// 공통 조회 helper. append=false 면 candidates 교체(검색/오픈), true 면 누적(더보기).
const loadCandidates = async (append) => {
  if (isLoading.value) return
  isLoading.value = true
  try {
    const res = await api.get('/appApi/leaveflow/approver-search', {
      params: { keyword: keyword.value || '', page: page.value, size: PAGE_SIZE },
    })
    const list = res?.data?.approvers || []
    if (append) {
      // userCd dedup append.
      const existing = new Set(candidates.value.map((c) => c.userCd))
      candidates.value = [...candidates.value, ...list.filter((c) => !existing.has(c.userCd))]
    } else {
      candidates.value = list
    }
    hasNext.value = Boolean(res?.data?.hasNext)
  } catch (e) {
    console.error('[LeaveApproverPicker] 결재자 검색 실패:', e?.message)
    showAlert('결재자 목록을 불러오지 못했어요.')
  } finally {
    isLoading.value = false
  }
}

const onSearch = () => {
  page.value = 0
  loadCandidates(false)
}
const onLoadMore = () => {
  page.value += 1
  loadCandidates(true)
}

// ── 푸터 ─────────────────────────────────────────────────────────────────
const onConfirm = () => {
  emit('add', checkedList.value)
}
const onCancel = () => {
  emit('update:modelValue', false)
}

// ── 배경(뒤 화면) 스크롤 잠금 ─────────────────────────────────────────────
// 시트가 열린 동안 document 스크롤을 막아, 시트 대신 뒤의 연차 신청 화면이
//   스크롤되던 문제(스크롤 누수)를 차단한다. (BaseBottomSheet 패턴 이식)
let prevBodyOverflow = ''
let prevHtmlOverflow = ''
const lockBackgroundScroll = () => {
  prevBodyOverflow = document.body.style.overflow
  // 일부 WebView 는 <html>(documentElement)이 스크롤 컨테이너이므로 둘 다 잠근다.
  prevHtmlOverflow = document.documentElement.style.overflow
  document.body.style.overflow = 'hidden'
  document.documentElement.style.overflow = 'hidden'
}
const unlockBackgroundScroll = () => {
  document.body.style.overflow = prevBodyOverflow
  document.documentElement.style.overflow = prevHtmlOverflow
}

onMounted(() => {
  // 초기 open=true 로 마운트되면 즉시 잠금.
  if (props.modelValue) lockBackgroundScroll()
})
onBeforeUnmount(() => {
  // 열린 채 언마운트돼도 잠금 잔존 방지(결재 불필요 종류 선택 시 시트 언마운트 대비).
  unlockBackgroundScroll()
})

// 시트 오픈 시 배경 스크롤 잠금 + 검색어/페이지/체크/후보 초기화 후 첫 페이지 조회. 닫힘 시 잠금 해제.
watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      lockBackgroundScroll()
      keyword.value = ''
      page.value = 0
      candidates.value = []
      hasNext.value = false
      checkedSet.value = new Set()
      loadCandidates(false)
    } else {
      unlockBackgroundScroll()
    }
  },
)
</script>

<style scoped>
.laps__dimmer {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.laps {
  width: 100%;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  background: var(--color-surface, #fff);
  border-radius: var(--radius-lg, 14px) var(--radius-lg, 14px) 0 0;
}
.laps__handle {
  width: 36px;
  height: 4px;
  margin: 8px auto 0;
  border-radius: var(--radius-full, 9999px);
  background: var(--color-border, #e5e7eb);
}
.laps__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px 4px;
}
.laps__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary, #111827);
}
.laps__close {
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
.laps__search {
  padding: 8px 16px;
}
.laps__search-input {
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
.laps__search-input:focus {
  border-color: var(--color-primary, #16a34a);
}

/* 후보 리스트 */
.laps__body {
  flex: 1;
  overflow-y: auto;
  /* 시트 끝 스크롤이 뒤 화면으로 전파(chaining)되지 않게 한다. */
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  padding: 0 16px 8px;
}
.laps__state {
  padding: 32px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary, #9ca3af);
}
.laps__item {
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
.laps__item:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.laps__item-info {
  flex: 1;
  min-width: 0;
}
.laps__item-name {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary, #111827);
}
.laps__item-meta {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--color-text-secondary, #6b7280);
}
.laps__item-check {
  color: var(--color-primary, #16a34a);
  font-weight: 700;
}
.laps__item-tag {
  font-size: 12px;
  color: var(--color-text-tertiary, #9ca3af);
}
.laps__more {
  width: 100%;
  height: 40px;
  margin-top: 8px;
  background: transparent;
  border: 0.5px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-md, 10px);
  font-size: 13px;
  color: var(--color-text-secondary, #6b7280);
  cursor: pointer;
  font-family: inherit;
}

/* 푸터 */
.laps__footer {
  flex-shrink: 0;
  padding: 12px 16px;
  border-top: 1px solid var(--color-border-light, #f3f4f6);
}
.laps__add-btn {
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
.laps__add-btn--off {
  background: var(--color-border, #e5e7eb);
  color: var(--color-text-tertiary, #9ca3af);
  cursor: not-allowed;
}

/* transition */
.laps-fade-enter-active,
.laps-fade-leave-active {
  transition: opacity 0.2s ease;
}
.laps-fade-enter-from,
.laps-fade-leave-to {
  opacity: 0;
}
</style>
