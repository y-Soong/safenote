<!--
  AdminTbmCompletedView.vue — 관리자 TBM 종료 화면 (COMPLETED)
  - 작업 ID: 001-P5-T-F11 (분해: 001-phase5-admin-tbm-plan.md §2-5, §3-F/§3-H)
  - 진입: 진행화면에서 "교육 종료" 후 / 교육관리·이력에서 종료 세션 선택 → /AdminTbmCompleted?sessionCd=...
          (라우트 등록은 developer).
  - 구성: 이수 근로자 리스트(AdminTbmAttendeeRow, variant=COMPLETED, 이수/미이수 배지)
          + (GPS 검증 세션 한정: session.gpsVerifyTypeCd !== 'DISABLED') 개별 미이수 처리 버튼.
  - 백엔드:
      GET /appApi/admin/tbm/sessions/{sessionCd}                          (상세)
      GET /appApi/admin/tbm/sessions/{sessionCd}/attendees?phase=COMPLETED  (이수자)
      POST .../{sessionCd}/attendees/{attendanceCd}/completion             (개별 미이수, T4)
  - 디자인 토큰: AdminLauncherView/TbmHubView 세트를 .admin-tbm-completed-view 루트에 1회 선언.
  - C1: GPS 세션 여부/이수상태는 서버 산출값만 신뢰. 클라이언트 역할 분기 없음.
  - planner 라운드 스코프: template + style 완성. script 는 선언 + 시트 토글까지.
      ⚠️ API 호출/라우팅/store 연동은 developer(R3/R4) — TODO(developer) 참조.
-->
<template>
  <div class="admin-tbm-completed-view">
    <!-- 헤더 -->
    <header class="admin-tbm-hd">
      <button type="button" class="admin-tbm-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-admin-tbm-completed-chev-left" />
        </svg>
      </button>
      <h1 class="admin-tbm-hd__title">TBM 종료</h1>
      <span class="admin-tbm-hd__spacer" aria-hidden="true" />
    </header>

    <main
      class="admin-tbm-completed-body"
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
        <!-- 제목 + 종료 배지 -->
        <div class="admin-tbm-completed__head">
          <span class="admin-tbm-completed__status">종료</span>
          <h2 class="admin-tbm-completed__title">{{ session.title || 'TBM 세션' }}</h2>
        </div>

        <!-- 종료 비밀번호(COMPLETED, pwdVisible 서버 산출) + 재발급(E7) -->
        <AdminTbmPwdCard
          v-if="session.pwdVisible"
          mode="EXIT"
          :exit-pwd="session.exitPwd"
          :can-regenerate="true"
          :regenerating="regenerating"
          @regenerate="onRegenerateExit"
        />

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

        <!-- 이수 근로자 리스트 -->
        <section class="card">
          <p class="card__label">이수 근로자</p>

          <p v-if="attendeesLoading" class="admin-tbm-state admin-tbm-state--sm">불러오는 중…</p>
          <p
            v-else-if="!attendees.length"
            class="admin-tbm-state admin-tbm-state--sm"
          >
            출결 기록이 없어요
          </p>
          <ul v-else class="admin-tbm-completed__list">
            <li v-for="a in attendees" :key="a.attendanceCd">
              <AdminTbmAttendeeRow
                :attendee="a"
                variant="COMPLETED"
                :can-manage-completion="canManageCompletion"
                @toggle-completion="onOpenCompletion"
              />
            </li>
          </ul>

          <!-- GPS 비검증 세션 안내(개별 미이수 미노출 사유) -->
          <p v-if="!canManageCompletion && attendees.length" class="admin-tbm-completed__note">
            GPS 검증 세션이 아니라 개별 이수 상태 변경은 제공되지 않아요.
          </p>
        </section>
      </template>
    </main>

    <!-- 개별 미이수 처리 시트 (GPS 세션 한정) -->
    <AdminTbmCompletionSheet
      :open="completionOpen"
      :attendee="targetAttendee"
      :submitting="completionSaving"
      @close="onCloseCompletion"
      @confirm="onConfirmCompletion"
    />

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="admin-tbm-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-admin-tbm-completed-chev-left"
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
import AdminTbmCompletionSheet from './components/AdminTbmCompletionSheet.vue'
import AdminTbmPwdCard from './components/AdminTbmPwdCard.vue'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert 폴백(앱 전역 우선)
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

// 개별 미이수 처리 시트
const completionOpen = ref(false)
const completionSaving = ref(false)
const targetAttendee = ref(null)

// 종료 비밀번호 재발급(E7) 진행 가드
const regenerating = ref(false)

// ── 파생값(표시 전용) ──────────────────────────────────────────────
// 개별 미이수 처리 노출 = GPS 검증 세션(AUTO/MANUAL). 서버 산출값(gpsVerifyTypeCd) 기준(T4).
const canManageCompletion = computed(
  () => !!session.value && session.value.gpsVerifyTypeCd !== 'DISABLED',
)
// [정합성 수정] 카운트 정의를 서버(앱/웹)와 일치시킨다.
//   attendees 는 loadAttendees 에서 실입실(entryAt 존재)만 담는다 → attendees.length = 참석자수.
//   이수 = COMPLETED. 미이수 = 실입실 + 미완료(상태가 COMPLETED 가 아님: NULL=미완료 또는 NOT_COMPLETED).
//   (관리자 종료 자동이수 폐지로, 완료하지 않은 입실자는 상태 NULL 로 남아 미이수에 포함되어야 한다.)
const completedCount = computed(
  () => attendees.value.filter((a) => a.completionStatusCd === 'COMPLETED').length,
)
const notCompletedCount = computed(
  () => attendees.value.filter((a) => a.completionStatusCd !== 'COMPLETED').length,
)

// ── 조회 ──────────────────────────────────────────────────────────
// 세션 상세 로드 — GET /appApi/admin/tbm/sessions/{sessionCd}.
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
    console.error('[AdminTbmCompletedView] 상세 조회 실패:', e?.message)
    loadError.value = true
  } finally {
    isLoading.value = false
  }
}

// 이수 근로자 리스트 로드 — GET .../{sessionCd}/attendees?phase=COMPLETED.
const loadAttendees = async () => {
  if (!sessionCd.value) return
  attendeesLoading.value = true
  try {
    const { data } = await api.get(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/attendees`,
      { params: { phase: 'COMPLETED' } },
    )
    // [정합성 수정] 미입실자(entryAt 없음)는 참석/이수/미이수 어디에도 잡히지 않도록 실입실만 남긴다.
    const rows = Array.isArray(data?.attendees) ? data.attendees : []
    attendees.value = rows.filter((a) => a.entryAt)
  } catch (e) {
    console.error('[AdminTbmCompletedView] 이수자 조회 실패:', e?.message)
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
// 뒤로(이전 화면 복귀)
const onBack = () => {
  router.back()
}

// 개별 미이수 처리 시트 열기/닫기
const onOpenCompletion = (attendee) => {
  targetAttendee.value = attendee
  completionOpen.value = true
}
const onCloseCompletion = () => {
  completionOpen.value = false
  targetAttendee.value = null
}

// 이수 상태 변경 확정(미이수 시 사유 10자 이상은 시트에서 1차 검증, 서버가 최종 권위).
// POST .../{sessionCd}/attendees/{attendanceCd}/completion { completionStatusCd, reason }
//   성공 시 해당 행 completionStatusCd 갱신 + 시트 닫기.
const onConfirmCompletion = async ({ completionStatusCd, reason } = {}) => {
  const target = targetAttendee.value
  if (!target?.attendanceCd || completionSaving.value) return
  completionSaving.value = true
  try {
    const { data } = await api.post(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/attendees/${encodeURIComponent(target.attendanceCd)}/completion`,
      { completionStatusCd, reason: reason || '' },
    )
    const newStatus = data?.completionStatusCd || completionStatusCd
    const idx = attendees.value.findIndex((a) => a.attendanceCd === target.attendanceCd)
    if (idx >= 0) {
      attendees.value[idx] = { ...attendees.value[idx], completionStatusCd: newStatus }
    }
    onCloseCompletion()
  } catch (e) {
    const msg =
      e?.response?.data?.message || '이수 상태 변경에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    completionSaving.value = false
  }
}

// 종료 비밀번호 재발급(COMPLETED, E7) — POST .../{sessionCd}/regenerate-exit-password
const onRegenerateExit = async () => {
  if (regenerating.value) return
  const confirmFn = proxy?.$confirm
    ? proxy.$confirm
    : (m) => Promise.resolve(window.confirm(m))
  const ok = await confirmFn('종료 비밀번호를 재발급할까요? 기존 종료 비밀번호는 사용할 수 없게 돼요.')
  if (!ok) return
  regenerating.value = true
  try {
    const { data } = await api.post(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/regenerate-exit-password`,
    )
    if (data?.exitPwd && session.value) {
      session.value.exitPwd = data.exitPwd
    }
  } catch (e) {
    const msg = e?.response?.data?.message || '재발급에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    regenerating.value = false
  }
}

// 당겨서 새로고침 — 상세+이수자 명단을 함께 재조회(각 함수 자체 try/catch 격리).
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
.admin-tbm-completed-view {
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
.admin-tbm-completed-body {
  flex: 1;
  min-height: 0;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* 제목 + 상태 */
.admin-tbm-completed__head {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.admin-tbm-completed__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.admin-tbm-completed__status {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
  background: var(--color-bg);
  color: var(--color-text-primary);
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

/* 이수자 리스트 */
.admin-tbm-completed__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.admin-tbm-completed__note {
  margin: var(--space-md) 0 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
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
