<!--
  AdminTbmHistoryDetailView.vue — 관리자 TBM 이력 상세 (출결 명단)
  - 작업 ID: 001-P5-T-F15 (분해: 001-phase5-admin-tbm-plan.md §2-7, §3-I T-A9 상세)
  - 진입: 이력 리스트(AdminTbmHistoryList) 카드 선택 → /AdminTbmHistoryDetail?sessionCd=...
          (라우트 등록은 developer).
  - 구성: 세션 메타(제목/상태/사업장/개설자/종료일) + 출결 명단(이름/입실·종료/이수상태 배지).
  - 백엔드:
      GET /appApi/admin/tbm/sessions/{sessionCd}                            (상세 — T-A2 재사용)
      GET /appApi/admin/tbm/sessions/{sessionCd}/attendees?phase=COMPLETED  (출결 — R3 기존 재사용)
      ⚠️ T9: 이상신호/이벤트 타임라인은 1차 생략(이름/입실종료/이수상태만).
  - 디자인 토큰: AdminLauncherView/TbmHubView 세트를 .admin-tbm-history-detail-view 루트에 1회 선언.
  - C1: 이수상태/스코프는 서버 산출값만 신뢰. 클라이언트 역할 분기 없음. 이력은 조회 전용(변경 액션 없음).
  - planner 라운드 스코프: template + style 완성. script 는 선언/조회 골격까지.
      ⚠️ 라우팅/추가 가공은 developer(R6) — TODO(developer) 참조.
-->
<template>
  <div class="admin-tbm-history-detail-view">
    <!-- 헤더 -->
    <header class="admin-tbm-hd">
      <button type="button" class="admin-tbm-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-admin-tbm-histd-chev-left" />
        </svg>
      </button>
      <h1 class="admin-tbm-hd__title">TBM 이력 상세</h1>
      <span class="admin-tbm-hd__spacer" aria-hidden="true" />
    </header>

    <main
      class="admin-tbm-history-detail-body"
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
        <!-- 제목 + 상태 -->
        <div class="admin-tbm-history-detail__head">
          <span class="admin-tbm-history-detail__status" :class="statusToneClass">
            {{ statusLabel }}
          </span>
          <h2 class="admin-tbm-history-detail__title">{{ session.title || 'TBM 세션' }}</h2>
        </div>

        <!-- 메타 -->
        <section class="card">
          <p class="card__label">기본 정보</p>
          <dl class="meta">
            <div class="meta__row">
              <dt>사업장</dt>
              <dd>{{ session.siteNm || '-' }}</dd>
            </div>
            <div class="meta__row">
              <dt>개설자</dt>
              <dd>{{ session.managerUserNm || '-' }}</dd>
            </div>
            <div v-if="session.openedAt" class="meta__row">
              <dt>개설일시</dt>
              <dd>{{ session.openedAt }}</dd>
            </div>
            <div v-if="session.endedAt" class="meta__row">
              <dt>종료일시</dt>
              <dd>{{ session.endedAt }}</dd>
            </div>
            <div v-if="session.cancelledAt" class="meta__row">
              <dt>취소일시</dt>
              <dd>{{ session.cancelledAt }}</dd>
            </div>
            <div v-if="session.cancelReason" class="meta__row">
              <dt>취소사유</dt>
              <dd>{{ session.cancelReason }}</dd>
            </div>
            <div v-if="session.managerSignYn === 'Y'" class="meta__row">
              <dt>주관자 서명</dt>
              <dd>{{ session.managerSignedAt }} 서명 완료</dd>
            </div>
          </dl>
        </section>

        <!-- 사후서명 카드(tbm04-manager-sign) — 종료 세션 + 서명 없음 + 개설자 본인일 때만 노출.
             클라 판별은 노출 제어용일 뿐, 서버가 verifyManager 로 재강제한다. -->
        <section v-if="canPostSign" class="card">
          <p class="card__label">주관자 서명</p>
          <p class="admin-tbm-sign-notice">
            이 교육은 주관자 서명 없이 종료되었습니다. 증빙자료 출력을 위해 서명을 등록해 주세요.
          </p>
          <button type="button" class="admin-tbm-sign-btn" @click="onOpenSignSheet">
            서명 등록
          </button>
        </section>

        <!-- 이수 집계 요약 -->
        <section class="card">
          <dl class="summary">
            <div class="summary__item">
              <dt>참여</dt>
              <dd>{{ attendees.length }}</dd>
            </div>
            <div class="summary__item summary__item--ok">
              <dt>이수</dt>
              <dd>{{ completedCount }}</dd>
            </div>
            <div class="summary__item summary__item--ng">
              <dt>미이수</dt>
              <dd>{{ notCompletedCount }}</dd>
            </div>
          </dl>
        </section>

        <!-- 출결 명단 -->
        <section class="card">
          <p class="card__label">출결 명단</p>

          <p v-if="attendeesLoading" class="admin-tbm-state admin-tbm-state--sm">불러오는 중…</p>
          <p
            v-else-if="!attendees.length"
            class="admin-tbm-state admin-tbm-state--sm"
          >
            출결 기록이 없어요
          </p>
          <ul v-else class="admin-tbm-history-detail__list">
            <li v-for="a in attendees" :key="a.attendanceCd">
              <!-- 이력은 조회 전용 — variant=COMPLETED + canManageCompletion=false(변경 버튼 미노출) -->
              <AdminTbmAttendeeRow :attendee="a" variant="COMPLETED" :can-manage-completion="false" />
            </li>
          </ul>
        </section>
      </template>
    </main>

    <!-- 주관자 사후서명 시트(tbm04-manager-sign) -->
    <AdminTbmEndSignSheet
      v-model="signSheetOpen"
      :submitting="signSubmitting"
      :error-msg="signError"
      title="주관자 서명"
      notice="종료된 교육에 주관자 서명을 등록합니다. 등록 후에는 다시 서명할 수 없어요."
      submit-label="서명 등록하기"
      @submit="onSubmitManagerSign"
    />

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="admin-tbm-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-admin-tbm-histd-chev-left"
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
import AdminTbmEndSignSheet from './components/AdminTbmEndSignSheet.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 우선) — AdminTbmLiveView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// ── 상태 ──────────────────────────────────────────────────────────
const sessionCd = computed(() => route.query.sessionCd || '')
const isLoading = ref(false)
const loadError = ref(false)
const attendeesLoading = ref(false)

const session = ref(null) // 상세(서버 SessionDetailItem)
const attendees = ref([]) // [{ attendanceCd, userNm, userTypeCd, deptNm, entryAt, exitAt, completionStatusCd }]

// ── 파생값(표시 전용) ──────────────────────────────────────────────
const STATUS_LABELS = { COMPLETED: '종료', CANCELLED: '취소' }
const statusLabel = computed(
  () => session.value?.statusNm || STATUS_LABELS[session.value?.statusCd] || session.value?.statusCd || '-',
)
const statusToneClass = computed(() =>
  session.value?.statusCd === 'CANCELLED'
    ? 'admin-tbm-history-detail__status--cancelled'
    : 'admin-tbm-history-detail__status--completed',
)
const completedCount = computed(
  () => attendees.value.filter((a) => a.completionStatusCd === 'COMPLETED').length,
)
const notCompletedCount = computed(
  () => attendees.value.filter((a) => a.completionStatusCd === 'NOT_COMPLETED').length,
)

// ── 사후서명(tbm04-manager-sign) ──────────────────────────────────
// 노출 3조건 AND: 종료 세션 + 서명 없음 + 개설자 본인(gv_userCd — 노출 제어용, 서버는 verifyManager 재강제).
const myUserCd = sessionStorage.getItem('gv_userCd') || ''
const canPostSign = computed(
  () =>
    session.value?.statusCd === 'COMPLETED' &&
    session.value?.managerSignYn !== 'Y' &&
    !!myUserCd &&
    session.value?.managerUserCd === myUserCd,
)

const signSheetOpen = ref(false)
const signSubmitting = ref(false)
const signError = ref('')

const onOpenSignSheet = () => {
  signError.value = ''
  signSheetOpen.value = true
}

// 서명 등록 — POST .../{sessionCd}/manager-sign (multipart 'item').
// 성공 시 시트 닫고 상세 재조회(서명시각 메타 행으로 전환). 409 는 서버 message 표기 후 재조회.
const onSubmitManagerSign = async ({ signFile }) => {
  if (signSubmitting.value) return
  signError.value = ''
  if (!signFile) {
    signError.value = '서명 이미지를 만들지 못했어요. 다시 시도해 주세요.'
    return
  }

  const formData = new FormData()
  formData.append('item', signFile)

  signSubmitting.value = true
  try {
    await api.post(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/manager-sign`,
      formData,
    )
    signSheetOpen.value = false
    await loadDetail()
  } catch (e) {
    const status = e?.response?.status
    const msg = e?.response?.data?.message || '서명 등록에 실패했어요. 잠시 후 다시 시도해 주세요.'
    if (status === 409) {
      // 이미 서명됨/상태 변경 경합 — 안내 후 최신 상태로 재조회(시트 닫음).
      signSheetOpen.value = false
      await showAlert(msg)
      await loadDetail()
    } else {
      signError.value = msg
    }
  } finally {
    signSubmitting.value = false
  }
}

// ── 조회 ──────────────────────────────────────────────────────────
// 세션 상세 — GET /appApi/admin/tbm/sessions/{sessionCd} (T-A2 재사용).
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
    if (!session.value) loadError.value = true
  } catch (e) {
    console.error('[AdminTbmHistoryDetailView] 상세 조회 실패:', e?.message)
    loadError.value = true
  } finally {
    isLoading.value = false
  }
}

// 출결 명단 — GET .../{sessionCd}/attendees?phase=COMPLETED (R3 기존 endpoint 재사용).
const loadAttendees = async () => {
  if (!sessionCd.value) return
  attendeesLoading.value = true
  try {
    const { data } = await api.get(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/attendees`,
      { params: { phase: 'COMPLETED' } },
    )
    attendees.value = Array.isArray(data?.attendees) ? data.attendees : []
  } catch (e) {
    console.error('[AdminTbmHistoryDetailView] 출결 조회 실패:', e?.message)
    attendees.value = []
  } finally {
    attendeesLoading.value = false
  }
}

const onRetry = () => {
  loadDetail()
  loadAttendees()
}

const onBack = () => {
  router.back()
}

// 당겨서 새로고침 — 스크롤 최상단에서 더 당기면 상세/출결을 함께 재조회(각 함수 자체 try/catch 격리).
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
.admin-tbm-history-detail-view {
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
.admin-tbm-history-detail-body {
  flex: 1;
  min-height: 0;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* 제목 + 상태 */
.admin-tbm-history-detail__head {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.admin-tbm-history-detail__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.admin-tbm-history-detail__status {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}
.admin-tbm-history-detail__status--completed {
  background: var(--color-bg);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
}
.admin-tbm-history-detail__status--cancelled {
  background: var(--color-bg);
  color: var(--color-danger);
  border: 1px solid var(--color-border);
}

/* 카드 */
.card {
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
}
.card__label {
  margin: 0 0 var(--space-sm);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

/* 메타 정의리스트 */
.meta {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.meta__row {
  display: flex;
  gap: var(--space-md);
}
.meta__row dt {
  flex-shrink: 0;
  width: 72px;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.meta__row dd {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-primary);
  word-break: break-all;
}

/* 사후서명 카드(tbm04-manager-sign) */
.admin-tbm-sign-notice {
  margin: 0 0 var(--space-md);
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-secondary);
  word-break: keep-all;
}
.admin-tbm-sign-btn {
  width: 100%;
  height: 44px;
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}

/* 이수 집계 요약 */
.summary {
  margin: 0;
  display: flex;
  gap: var(--space-sm);
}
.summary__item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-md) 0;
  background: var(--color-bg);
  border-radius: var(--radius-md);
}
.summary__item dt {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.summary__item dd {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.summary__item--ok dd {
  color: var(--color-primary);
}
.summary__item--ng dd {
  color: var(--color-danger-text);
}

/* 출결 리스트 */
.admin-tbm-history-detail__list {
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
