<!--
  NoticeListView.vue — 공지 전체목록 ("전체보기", 모바일 앱, 신규)
  - 작업 ID: prafta-app-023-5 (분해: .claude/requests/app_requests/prafta-app-023-tasks.md)
  - UI 명세: UI-app-023-4
  - 정책 출처: prafta-047 §7, 요청서 §1.2/Q4
  - 미러링 원천: GET /appApi/notice01/my-notices (카드와 동일 데이터, slice 없이 전건)
  - 진입: /NoticeList (메인 홈 공지카드 "전체보기")
  - 정렬은 백엔드 신뢰(PIN_YN DESC, PIN_ORDER ASC, INSERT_DATE DESC) — 프론트 재정렬 금지.
  - 페이징/무한스크롤은 본 라운드 범위 밖(전건 반환; 목록 과대 시 follow-up).
  - 참조: MyRequestsView / NearMissManageList (헤더 + 행 리스트 + 빈상태).
  - 디자인 토큰: MainView .home-view 세트를 .notice-list 루트에 1회 재선언.
  - planner 라운드: template + scoped style 완성, script 는 선언 + TODO 만.
  - developer 라운드: GET /my-notices, 행 클릭 → /NoticeDetail?noticeId= 라우팅.
-->
<template>
  <div class="notice-list">
    <!-- 헤더 -->
    <header class="nl-hd">
      <button type="button" class="nl-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-nl-chev-left" />
        </svg>
      </button>
      <h1 class="nl-hd__title">공지사항</h1>
      <span class="nl-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 (스크롤) -->
    <main class="nl-body">
      <!-- 로딩 -->
      <div v-if="isLoading" class="nl-loading" aria-live="polite">불러오는 중...</div>

      <!-- 목록 -->
      <ul v-else-if="notices.length > 0" class="nl-rows">
        <li
          v-for="row in notices"
          :key="row.noticeId"
          class="nl-row"
          role="button"
          tabindex="0"
          @click="onRow(row.noticeId)"
          @keydown.enter="onRow(row.noticeId)"
          @keydown.space.prevent="onRow(row.noticeId)"
        >
          <span v-if="row.isImportant" class="nl-row__imp">중요</span>
          <span class="nl-row__title" :class="{ 'nl-row__title--read': !row.isUnread }">
            {{ row.title }}
          </span>
          <span class="nl-row__meta">{{ row.displayTime }}</span>
          <span v-if="row.isUnread" class="nl-row__unread-dot" aria-label="미열람"></span>
        </li>
      </ul>

      <!-- 빈 상태 -->
      <div v-else class="nl-empty">등록된 공지사항이 없습니다</div>
    </main>

    <!-- 인라인 SVG 스프라이트 -->
    <svg width="0" height="0" class="nl-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-nl-chev-left"
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
import { ref, onMounted, getCurrentInstance } from 'vue'
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

// notices: my-notices 응답 행 배열(전건). 각 행:
//   { noticeId, isImportant, title, displayTime, isUnread }
//   (displayTime = insertDate 가공, isImportant = pinYn==='Y')
const notices = ref([])

// insertDate('YYYY-MM-DD HH:mm') → 목록 메타 'MM-DD' (메인 카드와 동일 포맷).
const toDisplayTime = (insertDate) => {
  if (!insertDate || typeof insertDate !== 'string') return ''
  const datePart = insertDate.split(' ')[0]
  const seg = datePart.split('-')
  if (seg.length === 3) return `${seg[1]}-${seg[2]}`
  return datePart
}

const toRow = (row) => ({
  noticeId: row.noticeId,
  isImportant: !!row.isImportant,
  title: row.title,
  displayTime: toDisplayTime(row.insertDate),
  isUnread: !!row.isUnread,
})

// ───────────────────────────────────────────────────────────
// 핸들러
// ───────────────────────────────────────────────────────────

const onBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/MainView')
}

const onRow = (noticeId) => {
  if (!noticeId) return
  router.push({ path: '/NoticeDetail', query: { noticeId } })
}

// ───────────────────────────────────────────────────────────
// 전체목록 조회 — GET /appApi/notice01/my-notices (카드와 동일 데이터, slice 없이 전건).
//   정렬은 백엔드 신뢰(고정 우선→최신). 페이징 없음(전건).
// ───────────────────────────────────────────────────────────
const loadNotices = async () => {
  isLoading.value = true
  try {
    const { data } = await api.get('/appApi/notice01/my-notices')
    const list = Array.isArray(data?.noticeList) ? data.noticeList : []
    notices.value = list.map(toRow)
  } catch (e) {
    console.warn('[NoticeListView] my-notices 조회 실패:', e?.message)
    notices.value = []
    await showAlert('공지 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isLoading.value = false
  }
}

// onMounted: 최초 진입 로드. 상세 다녀온 뒤 재진입 시 읽음 상태 갱신을 위해
//   라우트 컴포넌트는 매 진입마다 새로 마운트되므로 onMounted 만으로 재호출이 보장된다.
onMounted(() => {
  loadNotices()
})
</script>

<style scoped>
.notice-list {
  --color-primary: #16a34a;
  --color-danger: #ef4444;
  --color-warning-tint: #fffbeb;
  --color-warning-text-deep: #9a3412;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-full: 9999px;

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
.nl-hd {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 8px;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  flex-shrink: 0;
}
.nl-hd__back {
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
.nl-hd__title {
  flex: 1;
  margin: 0;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
}
.nl-hd__spacer {
  width: 44px;
  flex-shrink: 0;
}

/* 본문 */
.nl-body {
  flex: 1;
  overflow-y: auto;
}
.nl-loading,
.nl-empty {
  padding: 48px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.nl-rows {
  list-style: none;
  margin: 0;
  padding: 0;
  background: var(--color-surface);
}
.nl-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-bottom: 0.5px solid var(--color-border-light);
  cursor: pointer;
}
.nl-row:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
}
.nl-row__imp {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 6px;
  background: var(--color-warning-tint);
  color: var(--color-warning-text-deep);
  font-size: 11px;
  font-weight: 500;
  border-radius: 4px;
  flex-shrink: 0;
}
.nl-row__title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nl-row__title--read {
  font-weight: 400;
  color: var(--color-text-secondary);
}
.nl-row__meta {
  font-size: 11px;
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}
.nl-row__unread-dot {
  width: 6px;
  height: 6px;
  background: var(--color-danger);
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
