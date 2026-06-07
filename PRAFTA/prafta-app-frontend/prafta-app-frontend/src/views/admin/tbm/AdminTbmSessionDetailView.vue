<!--
  AdminTbmSessionDetailView.vue — 관리자 TBM 세션 상세
  - 작업 ID: 001-P5-T-F6 (분해: 001-phase5-admin-tbm-plan.md §2-3, §3 T-A2)
  - 진입: 교육관리 카드 선택 / 개설 성공 후 → /AdminTbmSessionDetail?sessionCd=...
  - 백엔드: GET /appApi/admin/tbm/sessions/{sessionCd} (T-A2) /
            POST .../{sessionCd}/cancel (취소) / POST .../{sessionCd}/regenerate-password (비번 재발급)
  - 표시: 메타(사업장/개설자/등록일/개설일시) + 교육 내용(plain text) + GPS 설정 + 콘텐츠 + 위험성평가
          + (OPENED/IN_PROGRESS) 입실/종료 비밀번호 카드(AdminTbmPwdCard, pwdVisible 서버 산출)
  - 상태별 액션: DRAFT/OPENED → 수정 / 취소(사유). OPENED → "교육 시작"(핸들러 TODO = 후속 R3 라이브).
  - 디자인 토큰: AdminLauncherView/TbmHubView 세트를 .admin-tbm-detail-view 루트에 1회 선언.
  - C1: 권한/스코프/비번노출은 서버(pwdVisible 등)만 신뢰. 클라이언트 역할 분기 없음.
  - CSS 변수만, <style scoped>, TS 미사용.
-->
<template>
  <div class="admin-tbm-detail-view">
    <!-- 헤더 -->
    <header class="admin-tbm-hd">
      <button type="button" class="admin-tbm-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-admin-tbm-detail-chev-left" />
        </svg>
      </button>
      <h1 class="admin-tbm-hd__title">TBM 세션 상세</h1>
      <span class="admin-tbm-hd__spacer" aria-hidden="true" />
    </header>

    <main class="admin-tbm-detail-body">
      <!-- loading -->
      <p v-if="isLoading" class="admin-tbm-state">불러오는 중…</p>

      <!-- error -->
      <div v-else-if="loadError" class="admin-tbm-state">
        <p class="admin-tbm-state__msg">정보를 불러오지 못했어요.</p>
        <button type="button" class="admin-tbm-retry" @click="onRetry">다시 시도</button>
      </div>

      <template v-else-if="session">
        <!-- 제목 + 상태 -->
        <div class="admin-tbm-detail__head">
          <span class="admin-tbm-detail__status" :class="statusToneClass">{{ statusLabel }}</span>
          <h2 class="admin-tbm-detail__title">{{ session.title || 'TBM 세션' }}</h2>
        </div>

        <!-- 입실/종료 비밀번호(OPENED/IN_PROGRESS, pwdVisible 서버 산출) -->
        <AdminTbmPwdCard
          v-if="session.pwdVisible"
          :entry-pwd="session.entryPwd"
          :exit-pwd="session.exitPwd"
          :can-regenerate="canRegenerate"
          :regenerating="regenerating"
          @regenerate="onRegenerate"
        />

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
            <div class="meta__row">
              <dt>등록일</dt>
              <dd>{{ session.insertDate || '-' }}</dd>
            </div>
            <div v-if="session.openedAt" class="meta__row">
              <dt>개설일시</dt>
              <dd>{{ session.openedAt }}</dd>
            </div>
            <div v-if="session.cancelledAt" class="meta__row">
              <dt>취소일시</dt>
              <dd>{{ session.cancelledAt }}</dd>
            </div>
            <div v-if="session.cancelReason" class="meta__row">
              <dt>취소사유</dt>
              <dd>{{ session.cancelReason }}</dd>
            </div>
          </dl>
        </section>

        <!-- 교육 내용(plain text, 줄바꿈 보존) -->
        <section class="card">
          <p class="card__label">교육 내용</p>
          <p class="admin-tbm-content">{{ session.contentBody || '내용이 없어요' }}</p>
        </section>

        <!-- GPS 설정 -->
        <section class="card">
          <p class="card__label">GPS 검증</p>
          <dl class="meta">
            <div class="meta__row">
              <dt>검증 유형</dt>
              <dd>{{ gpsTypeLabel }}</dd>
            </div>
            <div v-if="session.gpsVerifyTypeCd !== 'DISABLED'" class="meta__row">
              <dt>검증 반경</dt>
              <dd>{{ session.gpsVerifyRadiusM != null ? session.gpsVerifyRadiusM + 'm' : '-' }}</dd>
            </div>
            <div
              v-if="session.gpsVerifyTypeCd === 'AUTO' && session.managerGpsLat"
              class="meta__row"
            >
              <dt>개설 좌표</dt>
              <dd>위도 {{ session.managerGpsLat }} / 경도 {{ session.managerGpsLon }}</dd>
            </div>
          </dl>
        </section>

        <!-- 교육 자료 -->
        <section class="card">
          <p class="card__label">교육 자료</p>
          <ul v-if="contents.length" class="name-list">
            <li v-for="(c, i) in contents" :key="c.mtrlCd || i" class="name-list__item">
              {{ c.title || '(제목 없음)' }}
            </li>
          </ul>
          <p v-else class="admin-tbm-state admin-tbm-state--sm">등록된 자료가 없어요</p>
        </section>

        <!-- 위험성평가 -->
        <section class="card">
          <p class="card__label">위험성평가</p>
          <ul v-if="risks.length" class="name-list">
            <li v-for="(r, i) in risks" :key="i" class="name-list__item">
              {{ r.displayName || '(이름 미정)' }}
            </li>
          </ul>
          <p v-else class="admin-tbm-state admin-tbm-state--sm">연계된 위험성평가가 없어요</p>
        </section>

        <!-- 상태별 액션 -->
        <div
          v-if="canEdit || canStart || canGoLive || canGoCompleted"
          class="admin-tbm-detail__actions"
        >
          <button
            v-if="canEdit"
            type="button"
            class="btn btn--ghost"
            :disabled="busy"
            @click="onEdit"
          >
            수정
          </button>
          <button
            v-if="canEdit"
            type="button"
            class="btn btn--danger"
            :disabled="busy"
            @click="onCancel"
          >
            취소
          </button>
          <button
            v-if="canStart"
            type="button"
            class="btn btn--primary"
            :disabled="busy"
            @click="onStart"
          >
            교육 시작
          </button>
          <button
            v-if="canGoLive"
            type="button"
            class="btn btn--primary"
            :disabled="busy"
            @click="onGoLive"
          >
            진행 화면으로
          </button>
          <button
            v-if="canGoCompleted"
            type="button"
            class="btn btn--primary"
            :disabled="busy"
            @click="onGoCompleted"
          >
            종료 화면으로(이수 명단)
          </button>
        </div>
      </template>
    </main>

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="admin-tbm-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-admin-tbm-detail-chev-left"
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
import { useRouter, useRoute } from 'vue-router'

import api from '@/api/axios'
import AdminTbmPwdCard from './components/AdminTbmPwdCard.vue'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert/confirm 폴백(앱 전역 우선) — MainView/TbmHubView 패턴 동일
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
const busy = ref(false) // 취소/시작 진행 가드
const regenerating = ref(false)

const session = ref(null) // 상세 헤더(서버 SessionDetailItem)
const contents = ref([]) // [{ mtrlCd, title, ... }]
const risks = ref([]) // [{ displayName, ... }]

// 상태 라벨(SYS046) — 서버 statusNm 우선
const STATUS_LABELS = {
  DRAFT: '작성중',
  OPENED: '개설',
  IN_PROGRESS: '진행중',
  COMPLETED: '종료',
  CANCELLED: '취소',
}
const statusLabel = computed(
  () =>
    session.value?.statusNm ||
    STATUS_LABELS[session.value?.statusCd] ||
    session.value?.statusCd ||
    '-',
)
const statusToneClass = computed(() => {
  switch (session.value?.statusCd) {
    case 'IN_PROGRESS':
      return 'admin-tbm-detail__status--progress'
    case 'OPENED':
      return 'admin-tbm-detail__status--opened'
    case 'COMPLETED':
      return 'admin-tbm-detail__status--completed'
    case 'CANCELLED':
      return 'admin-tbm-detail__status--cancelled'
    case 'DRAFT':
    default:
      return 'admin-tbm-detail__status--draft'
  }
})

const GPS_TYPE_LABELS = { AUTO: '활성화', MANUAL: '수동 확인', DISABLED: '비활성화' }
const gpsTypeLabel = computed(
  () => GPS_TYPE_LABELS[session.value?.gpsVerifyTypeCd] || session.value?.gpsVerifyTypeCd || '-',
)

// 상태별 가용 액션(서버 상태 기준 — 클라 역할 분기 아님)
const canEdit = computed(() => ['DRAFT', 'OPENED'].includes(session.value?.statusCd))
const canStart = computed(() => session.value?.statusCd === 'OPENED')
const canRegenerate = computed(() => session.value?.statusCd === 'OPENED')
// 진행/종료 화면 진입(조회는 스코프 관리자 가능 — statusCd 기준으로 버튼 노출).
const canGoLive = computed(() => session.value?.statusCd === 'IN_PROGRESS')
const canGoCompleted = computed(() => session.value?.statusCd === 'COMPLETED')

// ── 조회 ──────────────────────────────────────────────────────────
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
    contents.value = Array.isArray(data?.contents) ? data.contents : []
    risks.value = Array.isArray(data?.risks) ? data.risks : []
    if (!session.value) loadError.value = true
  } catch (e) {
    console.error('[AdminTbmSessionDetailView] 상세 조회 실패:', e?.message)
    loadError.value = true
  } finally {
    isLoading.value = false
  }
}

const onRetry = () => loadDetail()

// ── 액션 ──────────────────────────────────────────────────────────
const onBack = () => {
  router.back()
}

// 수정: 후속 라운드(수정 폼 골격) 소관. 현재 라운드는 진입 안내만.
const onEdit = () => {
  showAlert('수정 화면은 다음 업데이트에서 제공됩니다.')
}

// 취소(사유 필수) — POST .../{sessionCd}/cancel
const onCancel = async () => {
  if (busy.value) return
  const reason = window.prompt('취소 사유를 입력해 주세요.')
  if (reason == null) return
  if (!reason.trim()) {
    await showAlert('취소 사유를 입력해 주세요.')
    return
  }
  busy.value = true
  try {
    await api.post(`/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/cancel`, {
      cancelReason: reason.trim(),
    })
    await showAlert('TBM 교육이 취소되었어요.')
    router.back()
  } catch (e) {
    const msg = e?.response?.data?.message || '취소에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    busy.value = false
  }
}

// 교육 시작(→ IN_PROGRESS, 개설자만): POST .../{sessionCd}/start → 성공 시 진행 화면 라우팅.
// 개설자가 아니면 서버가 에러를 내려보내며, 그 메시지를 표시한다.
const onStart = async () => {
  if (busy.value) return
  const ok = await askConfirm('교육을 시작할까요? 시작하면 입실자 진행 관리가 가능해요.')
  if (!ok) return
  busy.value = true
  try {
    await api.post(`/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/start`)
    router.push({ path: '/AdminTbmLive', query: { sessionCd: sessionCd.value } })
  } catch (e) {
    const msg = e?.response?.data?.message || '교육 시작에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    busy.value = false
  }
}

// 진행 화면으로(IN_PROGRESS) / 종료 화면으로(COMPLETED) — 상태별 진입.
const onGoLive = () => {
  router.push({ path: '/AdminTbmLive', query: { sessionCd: sessionCd.value } })
}
const onGoCompleted = () => {
  router.push({ path: '/AdminTbmCompleted', query: { sessionCd: sessionCd.value } })
}

// 비밀번호 재발급(OPENED만) — POST .../{sessionCd}/regenerate-password
const onRegenerate = async () => {
  if (regenerating.value) return
  const ok = await askConfirm(
    '입실/종료 비밀번호를 재발급할까요? 기존 비밀번호는 사용할 수 없게 돼요.',
  )
  if (!ok) return
  regenerating.value = true
  try {
    const { data } = await api.post(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}/regenerate-password`,
    )
    if (data?.entryPwd && session.value) {
      session.value.entryPwd = data.entryPwd
      session.value.exitPwd = data.exitPwd
    }
  } catch (e) {
    const msg = e?.response?.data?.message || '재발급에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    regenerating.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
/* 디자인 토큰 1회 선언(AdminLauncherView/TbmHubView 세트) — 자식(AdminTbmPwdCard) scoped 가 상속 */
.admin-tbm-detail-view {
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

  min-height: 100%;
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
.admin-tbm-detail-body {
  flex: 1;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

/* 상태/제목 */
.admin-tbm-detail__head {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.admin-tbm-detail__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.admin-tbm-detail__status {
  flex-shrink: 0;
  padding: 2px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}
.admin-tbm-detail__status--progress {
  background: var(--color-primary);
  color: var(--color-surface);
}
.admin-tbm-detail__status--opened {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.admin-tbm-detail__status--draft {
  background: var(--color-bg);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}
.admin-tbm-detail__status--completed {
  background: var(--color-bg);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
}
.admin-tbm-detail__status--cancelled {
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

/* 교육 내용(plain text) */
.admin-tbm-content {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

/* 이름 리스트 */
.name-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.name-list__item {
  font-size: 14px;
  color: var(--color-text-primary);
  padding: 10px var(--space-md);
  background: var(--color-bg);
  border-radius: var(--radius-md);
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
  text-align: left;
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

/* 액션 */
.admin-tbm-detail__actions {
  display: flex;
  gap: var(--space-sm);
  margin-top: var(--space-sm);
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
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
}
.btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}
.btn--danger {
  background: var(--color-surface);
  color: var(--color-danger-text);
  border: 1.5px solid var(--color-danger);
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
