<!--
  AdminBoardView.vue — 관리자 안전자료실(Archive) 목록 + 검색 [prafta-app-025 J1-8]
  - 진입: AdminLauncherView 본문 "게시판"(moduleActiveMap.BOARD===true) → router.push('/AdminBoard').
  - 동작: 자료타입/등록월/키워드 검색 + 목록 + 헤더 "등록"(서버가 등록 시 역할게이트 강제) + 행 → 상세.
  - 백엔드:
      GET  /appApi/notice02/archive-types                                   (B1 자료타입 드롭다운, COM008 USE_YN='Y')
      GET  /appApi/notice02/archive-lists?archiveTypeCd&registMonth&titleKeyword  (B2 목록, 최신순)
  - C1: 등록 버튼은 항상 노출(클라 역할 분기 금지). 권한 없으면 등록 시 서버 403(ARCHIVE_403_002) 안내.
        자료실=회사 전체 공통(사업장 스코프 없음). 정렬은 백엔드 신뢰(INSERT_DATE DESC) — 프론트 재정렬 금지.
  - 참조 패턴: views/notice/NoticeListView.vue(헤더+행 리스트+빈/로딩) + AdminLauncherView 디자인 토큰.
  - planner 라운드 범위: template + scoped style 완성, script 는 import/ref 선언 + UI(검색 입력 바인딩)만.
  - developer 라운드 범위(TODO):
      (1) GET archive-types → archiveTypeOptions 바인딩(드롭다운 "전체" + COM008 목록)
      (2) GET archive-lists(검색 파라미터) → rows 바인딩 + 빈/로딩/에러 처리
      (3) 검색 버튼 → 재조회. 등록월(YYYY-MM)/키워드/자료타입 파라미터 가공
      (4) 행 클릭 → /AdminBoardDetail?noticeId= 라우팅
      (5) 등록 버튼 → /AdminBoardForm 라우팅
      (6) 뒤로 → router 복귀(관리자 모드)
  - 디자인 토큰: AdminLauncherView/NoticeListView 동형 inline scoped 토큰(루트 1회 선언).
-->
<template>
  <div class="admin-board">
    <!-- 헤더: 뒤로 / 타이틀 / 등록 -->
    <header class="ab-hd">
      <button type="button" class="ab-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-ab-chev-left" />
        </svg>
      </button>
      <h1 class="ab-hd__title">안전자료실</h1>
      <button type="button" class="ab-hd__create" @click="onCreate">
        <svg class="icon" width="16" height="16" aria-hidden="true">
          <use href="#i-ab-plus" />
        </svg>
        <span>등록</span>
      </button>
    </header>

    <!-- 검색 바 (자료타입 "전체" 허용 / 등록월 / 키워드) -->
    <div class="ab-search">
      <select v-model="searchTypeCd" class="ab-search__select" aria-label="자료타입">
        <option value="">전체</option>
        <option v-for="t in archiveTypeOptions" :key="t.archiveTypeCd" :value="t.archiveTypeCd">
          {{ t.archiveTypeNm }}
        </option>
      </select>
      <!-- 등록월: 공통 월 선택 시트 트리거(표시는 점 YYYY.MM). searchMonth 계약 'YYYY-MM' 유지. -->
      <button
        type="button"
        class="ab-search__month ab-search__month--btn"
        :class="{ 'ab-search__month--placeholder': !searchMonth }"
        aria-label="등록월"
        @click="showMonthPicker = true"
      >
        {{ monthLabel || '등록월' }}
      </button>
      <MonthPickerSheet
        v-model="showMonthPicker"
        :year-month="searchMonthCompact"
        @confirm="onConfirmMonth"
      />
      <div class="ab-search__kw">
        <input
          v-model="searchKeyword"
          type="text"
          class="ab-search__input"
          placeholder="제목 또는 내용"
          @keydown.enter="onSearch"
        />
        <button type="button" class="ab-search__btn" aria-label="검색" @click="onSearch">
          <svg class="icon" width="18" height="18" aria-hidden="true">
            <use href="#i-ab-search" />
          </svg>
        </button>
      </div>
    </div>

    <!-- 본문 (스크롤) -->
    <main class="ab-body">
      <!-- 로딩 -->
      <div v-if="isLoading" class="ab-loading" aria-live="polite">불러오는 중...</div>

      <!-- 목록 -->
      <ul v-else-if="rows.length > 0" class="ab-rows">
        <li
          v-for="row in rows"
          :key="row.noticeId"
          class="ab-row"
          role="button"
          tabindex="0"
          @click="onRow(row.noticeId)"
          @keydown.enter="onRow(row.noticeId)"
          @keydown.space.prevent="onRow(row.noticeId)"
        >
          <div class="ab-row__top">
            <span v-if="row.archiveTypeNm" class="ab-row__type">{{ row.archiveTypeNm }}</span>
            <span class="ab-row__title">{{ row.title }}</span>
            <span v-if="row.fileCnt > 0" class="ab-row__file" aria-label="첨부">
              <svg class="icon" width="14" height="14" aria-hidden="true">
                <use href="#i-ab-clip" />
              </svg>
              {{ row.fileCnt }}
            </span>
          </div>
          <p class="ab-row__meta">
            <span v-if="row.insertUserNm">{{ row.insertUserNm }} · </span>{{ row.insertDate }}
          </p>
        </li>
      </ul>

      <!-- 빈 상태 -->
      <div v-else class="ab-empty">등록된 자료가 없습니다</div>
    </main>

    <!-- 인라인 SVG 스프라이트 -->
    <svg width="0" height="0" class="ab-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol id="i-ab-chev-left" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol id="i-ab-plus" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </symbol>
        <symbol id="i-ab-search" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="7" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
        </symbol>
        <symbol id="i-ab-clip" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

import api from '@/api/axios'
import { formatYmDot } from '@/utils/approvalFormat'
import MonthPickerSheet from '@/components/common/MonthPickerSheet.vue'

const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 $alert 우선) — NoticeListView/AdminLauncherView 동형.
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 상태 ─────────────────────────────────────────────────────────────
const isLoading = ref(true)

// 검색 입력(UI 바인딩 — 허용 범위). 자료타입 ''=전체.
const searchTypeCd = ref('')
const searchMonth = ref('') // 'YYYY-MM' (서버 registMonth 계약 — 불변)
const searchKeyword = ref('')

// 월 선택 시트 표시 + 표시/계약 변환
const showMonthPicker = ref(false)
// 트리거 표시(점 'YYYY.MM'). 빈값이면 placeholder.
const monthLabel = computed(() => formatYmDot(searchMonth.value))
// 시트 계약은 'YYYYMM' — searchMonth('YYYY-MM')에서 대시 제거하여 전달.
const searchMonthCompact = computed(() => searchMonth.value.replace('-', ''))
// 시트 confirm('YYYYMM') → searchMonth('YYYY-MM')로 재조립(계약 유지).
const onConfirmMonth = (yyyymm) => {
  const s = String(yyyymm || '')
  if (s.length >= 6) searchMonth.value = `${s.slice(0, 4)}-${s.slice(4, 6)}`
}

// 자료타입 드롭다운 옵션 (B1 응답 — [{ archiveTypeCd, archiveTypeNm }])
const archiveTypeOptions = ref([])

// 목록 행 (B2 응답 archiveList — [{ noticeId, archiveTypeNm, title, fileCnt, insertUserNm, insertDate }])
const rows = ref([])

// ── 핸들러 ────────────────────────────────────────────────────────────
const onBack = () => {
  // 히스토리 있으면 뒤로, 없으면 관리자 홈 폴백.
  if (window.history.length > 1) router.back()
  else router.push('/AdminHome')
}

const onSearch = () => {
  // 현재 검색 입력값으로 목록 재조회.
  loadList()
}

const onRow = (noticeId) => {
  if (!noticeId) return
  // 상세 화면으로 noticeId 전달.
  router.push({ path: '/AdminBoardDetail', query: { noticeId } })
}

const onCreate = () => {
  // 등록 폼 진입. 실제 등록 권한(master/hr/safe)은 서버 save-archive 가 최종 강제(C1).
  router.push('/AdminBoardForm')
}

// ── 조회 ──────────────────────────────────────────────────────────────
// GET /appApi/notice02/archive-types → archiveTypeOptions.
//   코드그룹(COM008) 미주입/빈 목록이면 "전체"만 노출(정상). 실패는 빈 옵션 폴백(목록 조회는 진행).
const loadTypes = async () => {
  try {
    const { data } = await api.get('/appApi/notice02/archive-types')
    archiveTypeOptions.value = Array.isArray(data?.typeList)
      ? data.typeList.map((t) => ({ archiveTypeCd: t.archiveTypeCd, archiveTypeNm: t.archiveTypeNm }))
      : []
  } catch (e) {
    console.warn('[AdminBoardView] archive-types 조회 실패:', e?.message)
    archiveTypeOptions.value = []
  }
}

// GET /appApi/notice02/archive-lists(검색 파라미터) → rows.
//   등록월(searchMonth 'YYYY-MM')은 registMonth 로 그대로 전달 — 서버가 startDate/endDate 로 변환.
//   빈 검색값은 전송에서 제외(전체 조회). 정렬은 서버(INSERT_DATE DESC) 신뢰 — 프론트 재정렬 금지.
const loadList = async () => {
  isLoading.value = true
  try {
    const params = {}
    if (searchTypeCd.value) params.archiveTypeCd = searchTypeCd.value
    if (searchMonth.value) params.registMonth = searchMonth.value
    if (searchKeyword.value) params.titleKeyword = searchKeyword.value

    const { data } = await api.get('/appApi/notice02/archive-lists', { params })
    rows.value = Array.isArray(data?.archiveList) ? data.archiveList : []
  } catch (e) {
    console.warn('[AdminBoardView] archive-lists 조회 실패:', e?.message)
    rows.value = []
    await showAlert('자료 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadTypes()
  loadList()
})
</script>

<style scoped>
.admin-board {
  /* 디자인 토큰(AdminLauncherView/NoticeListView 세트) */
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-sm: 6px;
  --radius-md: 10px;

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
.ab-hd {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 8px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  flex-shrink: 0;
}
.ab-hd__back {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  color: var(--color-text-primary);
  cursor: pointer;
}
.ab-hd__title {
  flex: 1;
  margin: 0;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
}
.ab-hd__create {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 10px;
  margin-right: 4px;
  border: 0;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
}

/* 검색 바 */
.ab-search {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
}
.ab-search__select,
.ab-search__month,
.ab-search__input {
  height: 38px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 10px;
  font-size: 13px;
  color: var(--color-text-primary);
  background: var(--color-surface);
  font-family: inherit;
}
.ab-search__select {
  flex: 0 0 auto;
  min-width: 110px;
}
.ab-search__month {
  flex: 0 0 auto;
}
.ab-search__month--btn {
  display: inline-flex;
  align-items: center;
  min-width: 110px;
  text-align: left;
  cursor: pointer;
}
.ab-search__month--placeholder {
  color: var(--color-text-tertiary);
}
.ab-search__kw {
  display: flex;
  flex: 1 1 100%;
  gap: 8px;
}
.ab-search__input {
  flex: 1;
  min-width: 0;
}
.ab-search__btn {
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
  background: var(--color-primary-tint);
  color: var(--color-primary);
  cursor: pointer;
}

/* 본문 */
.ab-body {
  flex: 1;
  overflow-y: auto;
}
.ab-loading,
.ab-empty {
  padding: 48px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.ab-rows {
  list-style: none;
  margin: 0;
  padding: 0;
  background: var(--color-surface);
}
.ab-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border-bottom: 0.5px solid var(--color-border-light);
  cursor: pointer;
}
.ab-row:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
}
.ab-row__top {
  display: flex;
  align-items: center;
  gap: 6px;
}
.ab-row__type {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 6px;
  background: var(--color-primary-tint);
  border: 1px solid var(--color-primary-tint-border);
  color: var(--color-primary);
  font-size: 11px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}
.ab-row__title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ab-row__file {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}
.ab-row__meta {
  margin: 0;
  font-size: 11px;
  color: var(--color-text-tertiary);
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
