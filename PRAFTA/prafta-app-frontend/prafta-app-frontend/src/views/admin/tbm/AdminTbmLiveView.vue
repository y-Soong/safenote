<!--
  AdminTbmLiveView.vue — 관리자 TBM 진행 화면 (IN_PROGRESS)
  - 작업 ID: 001-P5-T-F7 (분해: 001-phase5-admin-tbm-plan.md §2-4, §3-E/§3-F/§3-G)
  - 진입: 세션 상세(OPENED)에서 "교육 시작" → /AdminTbmLive?sessionCd=... (라우트 등록은 developer).
  - 구성: 상단 제목+내용(plain text) / 중단 첨부 자료 슬라이드(TbmMaterialSlider 재사용)
          / 하단 입실 근로자 리스트(AdminTbmAttendeeRow, variant=LIVE) + 행별 강제퇴실 / 푸터 "교육 종료".
  - 백엔드:
      GET /appApi/admin/tbm/sessions/{sessionCd}                    (상세)
      GET /appApi/admin/tbm/sessions/{sessionCd}/attendees?phase=LIVE  (입실자)
      POST .../{sessionCd}/attendees/{attendanceCd}/force-exit       (강제퇴실, T3)
      POST .../{sessionCd}/end                                       (종료, T1/T2)
  - 디자인 토큰: AdminLauncherView/TbmHubView 세트를 .admin-tbm-live-view 루트에 1회 선언.
  - C1: 권한/스코프/상태는 서버만 신뢰. 클라이언트 역할 분기 없음.
  - planner 라운드 스코프: template + style 완성. script 는 선언 + 시트 토글까지.
      ⚠️ API 호출/라우팅/store 연동은 developer(R3) — TODO(developer) 참조.
-->
<template>
  <div class="admin-tbm-live-view">
    <!-- 헤더 -->
    <header class="admin-tbm-hd">
      <button type="button" class="admin-tbm-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-admin-tbm-live-chev-left" />
        </svg>
      </button>
      <h1 class="admin-tbm-hd__title">TBM 진행</h1>
      <span class="admin-tbm-hd__spacer" aria-hidden="true" />
    </header>

    <main
      class="admin-tbm-live-body"
      ref="scrollRef"
      @touchstart.passive="onPullStart"
      @touchmove="onPullMove"
      @touchend="onPullEnd"
      @touchcancel="onPullEnd"
    >
      <!-- 당겨서 새로고침 인디케이터 — 스크롤 최상단에서 아래로 당기면 노출 -->
      <PullRefreshIndicator v-bind="indicatorProps" />

      <!-- loading -->
      <p v-if="isLoading" class="admin-tbm-state">불러오는 중…</p>

      <!-- error -->
      <div v-else-if="loadError" class="admin-tbm-state">
        <p class="admin-tbm-state__msg">정보를 불러오지 못했어요.</p>
        <button type="button" class="admin-tbm-retry" @click="onRetry">다시 시도</button>
      </div>

      <template v-else-if="session">
        <!-- 제목 + 진행중 배지 -->
        <div class="admin-tbm-live__head">
          <span class="admin-tbm-live__status">진행중</span>
          <h2 class="admin-tbm-live__title">{{ session.title || 'TBM 세션' }}</h2>
        </div>

        <!-- 교육 내용(plain text, 줄바꿈 보존) -->
        <section class="card">
          <p class="card__label">교육 내용</p>
          <p class="admin-tbm-content">{{ session.contentBody || '내용이 없어요' }}</p>
        </section>

        <!-- 첨부 자료 슬라이드(사용자 TBM 컴포넌트 재사용 — 비파괴 import) -->
        <section class="card">
          <p class="card__label">교육 자료</p>
          <TbmMaterialSlider :materials="materials" />
        </section>

        <!-- 입실 근로자 리스트 -->
        <section class="card">
          <div class="card__head">
            <p class="card__label">입실 근로자</p>
            <span class="admin-tbm-live__count">{{ attendees.length }}명</span>
          </div>

          <p v-if="attendeesLoading" class="admin-tbm-state admin-tbm-state--sm">불러오는 중…</p>
          <p
            v-else-if="!attendees.length"
            class="admin-tbm-state admin-tbm-state--sm"
          >
            아직 입실한 근로자가 없어요
          </p>
          <ul v-else class="admin-tbm-live__list">
            <li v-for="a in attendees" :key="a.attendanceCd">
              <AdminTbmAttendeeRow
                :attendee="a"
                variant="LIVE"
                :can-force-exit="true"
                @force-exit="onOpenForceExit"
              />
            </li>
          </ul>
        </section>
      </template>
    </main>

    <!-- 푸터: 교육 종료 -->
    <footer v-if="session && !isLoading && !loadError" class="admin-tbm-live-footer">
      <button type="button" class="btn btn--primary" :disabled="ending" @click="onEnd">
        교육 종료
      </button>
    </footer>

    <!-- 강제 퇴실 시트 -->
    <AdminTbmForceExitSheet
      :open="forceExitOpen"
      :attendee="targetAttendee"
      :submitting="forceExiting"
      @close="onCloseForceExit"
      @confirm="onConfirmForceExit"
    />

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="admin-tbm-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-admin-tbm-live-chev-left"
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
import { ref, computed, getCurrentInstance, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import api from '@/api/axios'
import { usePullToRefresh } from '@/composables/usePullToRefresh'
import PullRefreshIndicator from '@/components/common/PullRefreshIndicator.vue'
import AdminTbmAttendeeRow from './components/AdminTbmAttendeeRow.vue'
import AdminTbmForceExitSheet from './components/AdminTbmForceExitSheet.vue'
import TbmMaterialSlider from '@/views/tbm/components/TbmMaterialSlider.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert/confirm 폴백(앱 전역 우선) — SessionDetailView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}
const askConfirm = async (message) => {
  if (proxy?.$confirm) return await proxy.$confirm(message)
  return window.confirm(message)
}

// ── 상태 ──────────────────────────────────────────────────────────
const sessionCd = computed(() => route.query.sessionCd || '')
const isLoading = ref(false)
const loadError = ref(false)
const attendeesLoading = ref(false)
const ending = ref(false) // 종료 진행 가드

const session = ref(null) // 상세(서버 SessionDetailItem)
const materials = ref([]) // TbmMaterialSlider 계약: [{ mtrlCd, title, items:[{ type,url,desc,sortIdx }] }]
const attendees = ref([]) // [{ attendanceCd, userNm, userTypeCd, deptNm, entryAt, exitAt, completionStatusCd }]

// 강제퇴실 시트
const forceExitOpen = ref(false)
const forceExiting = ref(false)
const targetAttendee = ref(null)

// ── 조회 ──────────────────────────────────────────────────────────
// 세션 상세 로드 + IN_PROGRESS 상태 검증 후 자료(슬라이드용) 로드.
const loadDetail = async () => {
  if (!sessionCd.value) {
    loadError.value = true
    return
  }
  isLoading.value = true
  loadError.value = false
  try {
    const { data } = await api.get(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}`,
    )
    session.value = data?.session || null
    if (!session.value) {
      loadError.value = true
      return
    }
    // 진행 화면은 IN_PROGRESS 만 유효. 이미 종료/취소 등이면 안내 후 복귀.
    if (session.value.statusCd !== 'IN_PROGRESS') {
      await showAlert('진행 중인 교육이 아니에요.')
      router.back()
      return
    }
    await loadMaterials()
  } catch (e) {
    console.error('[AdminTbmLiveView] 상세 조회 실패:', e?.message)
    loadError.value = true
  } finally {
    isLoading.value = false
  }
}

// 슬라이드용 자료 항목 로드 — GET .../{sessionCd}/contents.
// 응답 materials([{ mtrlCd, title, items:[{ type, previewUrl, url, itemDesc, sortIdx }] }])를
// TbmMaterialSlider 계약(items.desc)에 맞춰 매핑.
//   previewUrl 은 서버가 발급한 서명 절대 URL(파일형). 파일형은 previewUrl, 외부링크형은 url 사용.
const loadMaterials = async () => {
  try {
    const { data } = await api.get(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/contents`,
    )
    const list = Array.isArray(data?.materials) ? data.materials : []
    materials.value = list.map((m) => ({
      mtrlCd: m.mtrlCd,
      title: m.title,
      items: (m.items || []).map((it) => ({
        type: it.type,
        url: it.previewUrl || it.url,
        desc: it.itemDesc,
        sortIdx: it.sortIdx,
      })),
    }))
  } catch (e) {
    console.error('[AdminTbmLiveView] 자료 조회 실패:', e?.message)
    materials.value = []
  }
}

// 입실 근로자 리스트 로드 — GET .../{sessionCd}/attendees?phase=LIVE.
const loadAttendees = async () => {
  if (!sessionCd.value) return
  attendeesLoading.value = true
  try {
    const { data } = await api.get(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/attendees`,
      { params: { phase: 'LIVE' } },
    )
    attendees.value = Array.isArray(data?.attendees) ? data.attendees : []
  } catch (e) {
    console.error('[AdminTbmLiveView] 입실자 조회 실패:', e?.message)
    attendees.value = []
  } finally {
    attendeesLoading.value = false
  }
}

const onRetry = () => {
  loadDetail()
  loadAttendees()
}

// ── 액션 ──────────────────────────────────────────────────────────
// 뒤로(세션 상세 등 이전 화면 복귀)
const onBack = () => {
  router.back()
}

// 강제 퇴실 시트 열기/닫기
const onOpenForceExit = (attendee) => {
  targetAttendee.value = attendee
  forceExitOpen.value = true
}
const onCloseForceExit = () => {
  forceExitOpen.value = false
  targetAttendee.value = null
}

// 강제 퇴실 확정(사유는 빈 문자열 허용 — T3).
// POST .../{sessionCd}/attendees/{attendanceCd}/force-exit { reason } → 성공 시 목록 갱신 + 시트 닫기.
const onConfirmForceExit = async (reason) => {
  const target = targetAttendee.value
  if (!target?.attendanceCd || forceExiting.value) return
  forceExiting.value = true
  try {
    await api.post(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/attendees/${encodeURIComponent(target.attendanceCd)}/force-exit`,
      { reason: reason || '' },
    )
    onCloseForceExit()
    await loadAttendees()
  } catch (e) {
    const msg = e?.response?.data?.message || '강제 퇴실에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    forceExiting.value = false
  }
}

// 교육 종료(→ COMPLETED, T1 개설자만). [정합성 수정] 자동이수 폐지:
//   종료는 세션 상태만 전이하며, 근로자는 종료 후에도 직접 완료(서명)해야 이수된다.
// confirm 후 POST .../{sessionCd}/end → 성공 시 종료화면(/AdminTbmCompleted) 으로 replace.
const onEnd = async () => {
  if (ending.value) return
  const ok = await askConfirm('교육을 종료할까요? 종료 후에도 근로자가 직접 완료(서명)해야 이수 처리돼요.')
  if (!ok) return
  ending.value = true
  try {
    await api.post(`/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/end`)
    router.replace({ path: '/AdminTbmCompleted', query: { sessionCd: sessionCd.value } })
  } catch (e) {
    const msg = e?.response?.data?.message || '종료에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    ending.value = false
  }
}

// 당겨서 새로고침 — 즉시 1회 재조회(상세+입실자). 진행 화면이지만 폴링 타이머는 없어 재조회만 수행.
const scrollRef = ref(null)
const { onPullStart, onPullMove, onPullEnd, indicatorProps } = usePullToRefresh(
  scrollRef,
  async () => {
    await Promise.all([loadDetail(), loadAttendees()])
  },
)

onMounted(() => {
  loadDetail()
  loadAttendees()
})
</script>

<style scoped>
/* 디자인 토큰 1회 선언(AdminLauncherView/TbmHubView 세트) — 자식(AttendeeRow) scoped 가 상속 */
.admin-tbm-live-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-primary-tint-border: #dcfce7;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-danger-text: #b91c1c;
  --color-warning-tint: #fffbeb;
  --color-warning-text: #b45309;
  --color-text-primary: #111827;
  --color-text-secondary: #6b7280;
  --color-text-tertiary: #9ca3af;
  --color-border: #e5e7eb;
  --color-border-light: #f3f4f6;
  --color-surface: #ffffff;
  --color-bg: #f9fafb;
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-full: 9999px;
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

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
.admin-tbm-hd {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.admin-tbm-hd__back {
  width: 36px;
  height: 36px;
  margin-left: -8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.admin-tbm-hd__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.admin-tbm-hd__spacer {
  width: 36px;
}

/* 본문 */
.admin-tbm-live-body {
  flex: 1;
  min-height: 0;
  padding: var(--space-md) var(--space-lg) var(--space-lg);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* 제목 + 상태 */
.admin-tbm-live__head {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.admin-tbm-live__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.admin-tbm-live__status {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
  background: var(--color-primary);
  color: var(--color-surface);
}

/* 카드 */
.card {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
}
.card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-sm);
}
.card__label {
  margin: 0 0 var(--space-sm);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.card__head .card__label {
  margin: 0;
}
.admin-tbm-live__count {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
}

/* 교육 내용(plain text) */
.admin-tbm-content {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

/* 입실자 리스트 */
.admin-tbm-live__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

/* 상태 메시지 */
.admin-tbm-state {
  margin: var(--space-lg) 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
}
.admin-tbm-state--sm {
  margin: 0;
  font-size: 13px;
}
.admin-tbm-state__msg {
  margin: 0 0 var(--space-sm);
}
.admin-tbm-retry {
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

/* 푸터 */
.admin-tbm-live-footer {
  position: sticky;
  bottom: 0;
  padding: var(--space-md) var(--space-lg) calc(var(--space-md) + env(safe-area-inset-bottom, 0px));
  background: var(--color-surface);
  border-top: 0.5px solid var(--color-border-light);
}
.btn {
  width: 100%;
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: inherit;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}

/* 스프라이트 */
.admin-tbm-sprite {
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
