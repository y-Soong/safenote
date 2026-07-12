<!--
  TbmBeforeStartView.vue — TBM 교육 시작전(입실 완료 ~ 관리자 시작 대기)
  - 작업 ID: PRAFTA-TBM-BEFORE (분해: prafta-app-tbm-user-detail-plan.md §4 F5)
  - 진입: 입실 비번 인증(enter) 성공 후 router.push('/TbmBeforeStart?sessionCd=...')
  - 구성: 세션 메타 + 참석자 리스트(A4, 새로고침) + 퇴실하기(A8)/시작하기(A5 on-demand 분기)
  - 상태 감지: 폴링/푸시 아님. "시작하기" 클릭 시점 GET state(A5) → IN_PROGRESS 면 /TbmInProgress 이동,
    아니면 "아직 관리자가 교육을 시작하지 않았습니다" 토스트(잔류).
  - 퇴실하기: 출결 취소(참석자에서 제거). confirm 만(비번/서명/사유 없음).
  - 디자인 토큰: TbmEntryView 세트를 .tbm-before-view 루트에 1회 선언.
  - planner 라운드 스코프: template + style 완성. script 는 선언/TODO 골격만.
-->
<template>
  <div class="tbm-before-view">
    <!-- 헤더 -->
    <header class="tbm-hd">
      <button type="button" class="tbm-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-tbm-chev-left" />
        </svg>
      </button>
      <h1 class="tbm-hd__title">교육 대기</h1>
      <button type="button" class="tbm-hd__refresh" aria-label="새로고침" @click="onRefresh">
        <svg class="icon" width="20" height="20" aria-hidden="true">
          <use href="#i-tbm-refresh" />
        </svg>
      </button>
    </header>

    <main class="tbm-before-body">
      <p v-if="isLoading" class="tbm-state">불러오는 중…</p>

      <template v-else>
        <!-- 세션 메타 카드 -->
        <section class="card">
          <p class="card__title">{{ session.title || 'TBM 세션' }}</p>
          <p v-if="managerText" class="card__meta">{{ managerText }}</p>
          <div class="callout callout--wait">
            <svg class="icon" width="16" height="16" aria-hidden="true">
              <use href="#i-tbm-clock" />
            </svg>
            <span>관리자가 교육을 시작하면 시작하기를 눌러주세요</span>
          </div>
        </section>

        <!-- 참석자 리스트 -->
        <section class="card">
          <div class="card__head">
            <p class="card__subtitle">참석자 {{ attendees.length }}명</p>
            <button type="button" class="link-btn" @click="onRefresh">새로고침</button>
          </div>

          <p v-if="attendees.length === 0" class="tbm-state tbm-state--sm">
            아직 참석자가 없어요
          </p>
          <ul v-else class="attendee-list">
            <li v-for="(a, i) in attendees" :key="i" class="attendee-list__item">
              <span class="attendee-list__name">{{ a.userNm }}</span>
              <span class="attendee-list__time">{{ a.entryAt }}</span>
            </li>
          </ul>
        </section>
      </template>
    </main>

    <!-- 하단 액션 -->
    <footer class="tbm-before-actions">
      <button type="button" class="btn btn--ghost-danger" @click="onLeaveBefore">
        퇴실하기
      </button>
      <button
        type="button"
        class="btn btn--primary"
        :disabled="checkingState"
        @click="onStart"
      >
        시작하기
      </button>
    </footer>

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="tbm-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-tbm-chev-left"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="15 18 9 12 15 6" />
        </symbol>
        <symbol
          id="i-tbm-refresh"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="23 4 23 10 17 10" />
          <polyline points="1 20 1 14 7 14" />
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
        </symbol>
        <symbol
          id="i-tbm-clock"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <circle cx="12" cy="12" r="9" />
          <polyline points="12 7 12 12 15 14" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import api from '@/api/axios'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert/confirm 폴백(앱 전역 우선, 없으면 window) — MainView 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}
const askConfirm = async (message) => {
  if (proxy?.$confirm) return await proxy.$confirm(message)
  return window.confirm(message)
}

// ── 반응형 상태(developer: 조회 결과로 채움) ──────────────────────
const isLoading = ref(false)

// 세션 메타: { sessionCd, title, managerUserNm }
// 별도 세션 메타 endpoint 가 없어 진입 query(title/managerUserNm)를 표시 보조로 사용한다.
const session = ref({
  sessionCd: '',
  title: '',
  managerUserNm: '',
})

// 참석자: [{ userNm, entryAt }]
const attendees = ref([])

// 시작하기 클릭 시 상태조회(A5) 진행 가드
const checkingState = ref(false)
// 퇴실(leave-before) 제출 가드
const isSubmitting = ref(false)

const managerText = computed(() =>
  session.value.managerUserNm ? `개설자 ${session.value.managerUserNm}` : '',
)

// 참석자 리스트 조회 — GET /appApi/tbm/sessions/{sessionCd}/attendees (A4)
const loadAttendees = async () => {
  if (!session.value.sessionCd) return
  isLoading.value = true
  try {
    const { data } = await api.get(`/appApi/tbm/sessions/${session.value.sessionCd}/attendees`)
    attendees.value = Array.isArray(data?.attendees) ? data.attendees : []
  } catch (e) {
    console.error('[TbmBeforeStart] 참석자 조회 실패:', e?.message)
    showAlert('참석자 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isLoading.value = false
  }
}

// ── 액션 ──────────────────────────────────────────────────────────
const onBack = () => {
  router.replace('/TbmHub')
}

// 참석자 리스트 수동 새로고침(자동 polling 없음 — plan Q7)
//   새로고침 시 본인 출결(my-attendance)도 확인 → 관리자 내보내기(present=false) /
//   강제퇴실(MANAGER_FORCED) 감지 시 안내 후 허브 복귀(대기 화면 이탈 방지).
const onRefresh = async () => {
  await loadAttendees()
  if (!session.value.sessionCd) return
  try {
    const { data } = await api.get(
      `/appApi/tbm/sessions/${session.value.sessionCd}/my-attendance`,
    )
    if (!data?.present) {
      await showAlert('관리자가 참석 인원에서 내보냈어요. 목록으로 돌아갈게요.')
      router.replace('/TbmHub')
      return
    }
    if (data.exitTypeCd === 'MANAGER_FORCED') {
      await showAlert('관리자에 의해 퇴실 처리되었어요. 목록으로 돌아갈게요.')
      router.replace('/TbmHub')
    }
  } catch (e) {
    // 비치명적: 참석자 새로고침 자체는 완료됨 → 조용히 무시
    console.error('[TbmBeforeStart] my-attendance 조회 실패:', e?.message)
  }
}

// 퇴실하기: confirm → POST /appApi/tbm/sessions/{sessionCd}/leave-before (A8, 출결 취소)
//           성공(멱등: alreadyProcessed 도 정상 처리) 시 /TbmHub 복귀
const onLeaveBefore = async () => {
  if (isSubmitting.value) return
  const ok = await askConfirm('퇴실하시겠어요? 참석 인원에서 제외돼요.')
  if (!ok) return
  isSubmitting.value = true
  try {
    await api.post(`/appApi/tbm/sessions/${session.value.sessionCd}/leave-before`)
    await showAlert('퇴실 처리되었어요.')
    router.replace('/TbmHub')
  } catch (e) {
    console.error('[TbmBeforeStart] leave-before 실패:', e?.message)
    showAlert(e?.response?.data?.message || '퇴실 처리에 실패했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isSubmitting.value = false
  }
}

// 시작하기: 본인 출결(my-attendance) 선검사 → GET /appApi/tbm/sessions/{sessionCd}/state (A5) → statusCd 분기
//   관리자 내보내기(present=false) / 강제퇴실(MANAGER_FORCED) 감지 시 → 안내 후 허브 복귀(교육 진입 차단)
//   IN_PROGRESS → /TbmInProgress 이동
//   그 외       → '아직 관리자가 교육을 시작하지 않았습니다' 안내($alert, 잔류)
const onStart = async () => {
  if (checkingState.value) return
  checkingState.value = true
  try {
    // 내보내기/강제퇴실 가드: 관리자가 참석 인원에서 제외한 근로자가 시작하기로 교육에 진입하는 것을 차단한다.
    //   (조회 실패는 비치명적 — 가드를 건너뛰고 state 분기로 진행. 실제 진입 후에도 진행/종료 단계에서 재차 감지됨)
    try {
      const { data: mine } = await api.get(
        `/appApi/tbm/sessions/${session.value.sessionCd}/my-attendance`,
      )
      if (!mine?.present) {
        await showAlert('관리자가 참석 인원에서 내보냈어요. 목록으로 돌아갈게요.')
        router.replace('/TbmHub')
        return
      }
      if (mine.exitTypeCd === 'MANAGER_FORCED') {
        await showAlert('관리자에 의해 퇴실 처리되었어요. 목록으로 돌아갈게요.')
        router.replace('/TbmHub')
        return
      }
    } catch (e) {
      console.error('[TbmBeforeStart] 시작 전 my-attendance 조회 실패:', e?.message)
    }

    const { data } = await api.get(`/appApi/tbm/sessions/${session.value.sessionCd}/state`)
    if (data?.statusCd === 'IN_PROGRESS') {
      router.push({
        path: '/TbmInProgress',
        query: {
          sessionCd: session.value.sessionCd,
          title: session.value.title || '',
          managerUserNm: session.value.managerUserNm || '',
        },
      })
    } else {
      showAlert('아직 관리자가 교육을 시작하지 않았습니다.')
    }
  } catch (e) {
    console.error('[TbmBeforeStart] state 조회 실패:', e?.message)
    showAlert('상태를 확인하지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    checkingState.value = false
  }
}

// ── 진입 ────────────────────────────────────────────────────────
onMounted(() => {
  const sessionCd = route.query.sessionCd || ''
  if (!sessionCd) {
    showAlert('세션 정보가 없어 화면을 열 수 없어요.')
    router.replace('/TbmHub')
    return
  }
  session.value = {
    sessionCd,
    title: route.query.title || '',
    managerUserNm: route.query.managerUserNm || '',
  }
  loadAttendees()
})
</script>

<style scoped>
/* 디자인 토큰 1회 선언(TbmEntryView 세트와 동일) */
.tbm-before-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
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
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;

  min-height: 100%;
  background: var(--color-bg);
  color: var(--color-text-primary);
  display: flex;
  flex-direction: column;
}

/* 헤더 */
.tbm-hd {
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border-light);
}
.tbm-hd__back,
.tbm-hd__refresh {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.tbm-hd__back {
  margin-left: -8px;
}
.tbm-hd__refresh {
  margin-right: -8px;
  color: var(--color-text-secondary);
}
.tbm-hd__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

/* 본문 */
.tbm-before-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.tbm-state {
  margin: var(--space-lg) 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
}
.tbm-state--sm {
  margin: var(--space-md) 0;
  font-size: 13px;
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
.card__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.card__subtitle {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.card__meta {
  margin: var(--space-sm) 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.callout {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-top: var(--space-md);
  padding: 10px 12px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
}
.callout--wait {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}

.link-btn {
  background: transparent;
  border: 0;
  padding: var(--space-xs) 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
  cursor: pointer;
  font-family: inherit;
}

/* 참석자 리스트 */
.attendee-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.attendee-list__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 0.5px solid var(--color-border-light);
  font-size: 14px;
}
.attendee-list__item:last-child {
  border-bottom: 0;
}
.attendee-list__name {
  color: var(--color-text-primary);
  font-weight: 500;
}
.attendee-list__time {
  color: var(--color-text-tertiary);
  font-size: 12px;
}

/* 하단 액션 */
.tbm-before-actions {
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  background: var(--color-surface);
  border-top: 0.5px solid var(--color-border-light);
}
.btn {
  flex: 1;
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
.btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}
.btn--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn--ghost-danger {
  background: var(--color-surface);
  color: var(--color-danger-text);
  border: 1.5px solid var(--color-danger);
}

/* 스프라이트 */
.tbm-sprite {
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
