<!--
  NearMissManageDetail.vue — 관리자 1차 확인 상세 (모바일 앱, 신규)
  - 작업 ID: PRAFTA-app-012-5 (분해: .claude/requests/app_requests/prafta-app-012-plan.md)
  - UI 명세: UI-app-012-3 (.claude/requests/app_requests/prafta-app-012-ui-spec.md)
  - 설계 출처: .claude/context/near-miss-incident-design.md §5-B(상세)
  - planner 라운드 스코프: 읽기 본문 + 임시조치 입력 + 푸터 2버튼(검토중 전환/반려). template/style 완성.
  - developer 라운드 스코프: A5 상세 조회, A6 상태전환(200)/반려(900), 반려 사유 입력, 라우팅.
  - 앱 관리자 조치범위: 접수(100)→검토중(200) 전환 + 임시조치 메모까지만(정밀조사=웹).
  - 디자인 토큰: MainView 세트를 .near-miss-detail 루트에 1회 선언.
  - 참조: SafetyInspectSavedView(읽기 본문 + 푸터 2버튼).
-->
<template>
  <div class="near-miss-detail">
    <!-- 헤더 -->
    <header class="nmd-hd">
      <button type="button" class="nmd-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-nmd-chev-left" />
        </svg>
      </button>
      <h1 class="nmd-hd__title">사건 상세</h1>
      <span v-if="incident" class="nmd-badge" :class="severityClass(incident.potentialSeverityCd)">
        {{ incident.potentialSeverityNm || '미분류' }}
      </span>
      <span v-else class="nmd-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문(스크롤) -->
    <main class="nmd-body">
      <!-- 로딩 -->
      <div v-if="isLoading" class="nmd-loading" aria-live="polite">불러오는 중...</div>

      <template v-else-if="incident">
        <!-- 읽기 블록 -->
        <dl class="nmd-read">
          <div class="nmd-row">
            <dt>유형</dt>
            <dd>{{ incident.incidentTypeNm }}</dd>
          </div>
          <div class="nmd-row">
            <dt>발생</dt>
            <dd>{{ incident.occurDtime }}</dd>
          </div>
          <div v-if="incident.locationDesc" class="nmd-row">
            <dt>장소</dt>
            <dd>{{ incident.locationDesc }}</dd>
          </div>
          <div v-if="incident.processNm" class="nmd-row">
            <dt>공정</dt>
            <dd>{{ incident.processNm }}</dd>
          </div>
          <div class="nmd-row">
            <dt>경위</dt>
            <dd class="nmd-row__multiline">{{ incident.description }}</dd>
          </div>
          <div class="nmd-row">
            <dt>보고자</dt>
            <dd>{{ incident.reporterNm }} / {{ incident.reportDtime }}</dd>
          </div>
          <div v-if="incident.immediateActionDesc" class="nmd-row">
            <dt>즉시조치</dt>
            <dd class="nmd-row__multiline">{{ incident.immediateActionDesc }}</dd>
          </div>
          <div class="nmd-row">
            <dt>상태</dt>
            <dd>{{ incident.reportStatusNm }}</dd>
          </div>
        </dl>

        <!-- 사진(단일) -->
        <div v-if="incident.filePath" class="nmd-photo">
          <img :src="incident.filePath" :alt="incident.fileName || '현장 사진'" />
        </div>

        <!-- 임시조치 메모 (관리자, ADMIN_TEMP_ACTION_DESC) -->
        <section class="nmd-action">
          <p class="nmd-label">임시조치 (관리자)</p>
          <textarea
            v-model="adminTempActionDesc"
            class="nmd-textarea"
            rows="3"
            placeholder="예) 경보 점검 지시"
            maxlength="500"
            :disabled="!canFirstReview"
          ></textarea>
          <p v-if="!canFirstReview" class="nmd-hint">
            접수 단계에서만 1차 확인을 할 수 있어요. 이후 처리는 웹에서 진행합니다.
          </p>
        </section>
      </template>

      <div v-else class="nmd-empty">사건을 찾을 수 없어요</div>
    </main>

    <!-- 푸터: 반려 / 접수→검토중 전환 -->
    <footer v-if="incident && canFirstReview" class="nmd-footer">
      <button type="button" class="nmd-btn nmd-btn--secondary" @click="onReject">반려</button>
      <button
        type="button"
        class="nmd-btn nmd-btn--primary"
        :disabled="isSubmitting"
        @click="onAdvance"
      >
        {{ isSubmitting ? '처리 중...' : '접수 → 검토중' }}
      </button>
    </footer>
  </div>

  <!-- 인라인 SVG 스프라이트 -->
  <svg width="0" height="0" class="nmd-sprite" aria-hidden="true" focusable="false">
    <defs>
      <symbol
        id="i-nmd-chev-left"
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
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'

import api from '@/api/axios'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// 진입 query (목록 카드 → nearMissId)
const nearMissId = computed(() => route.query.nearMissId || '')

// ───────────────────────────────────────────────────────────
// 상태
// ───────────────────────────────────────────────────────────
const incident = ref(null) // IncidentResult
const adminTempActionDesc = ref('')
const isLoading = ref(false)
const isSubmitting = ref(false)

// 앱 1차 확인 가능 여부: 현재 상태가 '접수(100)' 일 때만(앱은 100→200 만 허용)
const canFirstReview = computed(() => incident.value?.reportStatusCd === '100')

const severityClass = (code) => {
  if (code === '300') return 'nmd-badge--critical'
  if (code === '200') return 'nmd-badge--major'
  if (code === '100') return 'nmd-badge--minor'
  return 'nmd-badge--none'
}

// ───────────────────────────────────────────────────────────
// 이벤트
// ───────────────────────────────────────────────────────────
const onBack = () => {
  router.back()
}

// 접수 → 검토중(200) 전환 (임시조치 메모 동반)
const onAdvance = async () => {
  if (isSubmitting.value) return
  if (!nearMissId.value) return
  isSubmitting.value = true
  try {
    // 식별자(cmpnyCd/userCd/siteCd)는 axios 인터셉터가 gv_* 자동 주입.
    await api.post('/appApi/nearmiss/change-status', {
      nearMissId: nearMissId.value,
      reportStatusCd: '200',
      adminTempActionDesc: adminTempActionDesc.value.trim() || null,
    })
    await showAlert('검토중으로 변경했어요')
    // 목록은 router.back 진입 시 onMounted 재조회로 동기화된다.
    router.back()
  } catch (err) {
    handleChangeError(err, '상태를 변경하지 못했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isSubmitting.value = false
  }
}

// 반려(900) — 사유 필수
const onReject = async () => {
  if (isSubmitting.value) return
  if (!nearMissId.value) return

  // 반려 사유 입력(간이 prompt). $confirm 폴백 패턴과 동일하게 전역 prompt 가 없으면 window.prompt 사용.
  const reason = await askReason('반려 사유를 입력해주세요.')
  if (reason === null) return // 취소
  if (!reason.trim()) {
    showAlert('반려 사유를 입력해주세요.')
    return
  }

  isSubmitting.value = true
  try {
    await api.post('/appApi/nearmiss/change-status', {
      nearMissId: nearMissId.value,
      reportStatusCd: '900',
      rejectReason: reason.trim(),
    })
    await showAlert('반려 처리했어요')
    router.back()
  } catch (err) {
    handleChangeError(err, '반려 처리에 실패했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isSubmitting.value = false
  }
}

// 반려 사유 입력 — 전역 prompt 컴포넌트 부재 환경이라 window.prompt 사용(취소 시 null).
const askReason = (message) => {
  const input = window.prompt(message, '')
  return Promise.resolve(input)
}

// 상태전환 공통 에러 처리. 422(앱 비허용 전이)/403/404 는 서버 message 우선 노출.
const handleChangeError = (err, fallbackMessage) => {
  console.error('[NearMissManageDetail] 상태전환 실패:', err?.message)
  showAlert(err?.response?.data?.message || fallbackMessage)
}

// ───────────────────────────────────────────────────────────
// A5 상세 조회 — assertSiteAccess(서버) 통과 시 incidentInfo 매핑.
// ───────────────────────────────────────────────────────────
const loadDetail = async () => {
  if (!nearMissId.value) {
    await showAlert('사건을 찾을 수 없어요')
    router.back()
    return
  }
  isLoading.value = true
  try {
    const res = await api.get('/appApi/nearmiss/detail', {
      params: { nearMissId: nearMissId.value },
    })
    const info = res?.data?.incidentInfo || null
    incident.value = info
    // 기존 관리자 임시조치가 있으면 입력칸 초기값으로 표시.
    adminTempActionDesc.value = info?.adminTempActionDesc || ''
  } catch (err) {
    const status = err?.response?.status
    if (status === 404) {
      await showAlert('사건을 찾을 수 없어요')
      router.back()
      return
    }
    if (status === 403) {
      await showAlert('권한이 없어요')
      router.back()
      return
    }
    console.error('[NearMissManageDetail] 상세 조회 실패:', err?.message)
    await showAlert(err?.response?.data?.message || '사건 정보를 불러오지 못했어요.')
    router.back()
  } finally {
    isLoading.value = false
  }
}

// ───────────────────────────────────────────────────────────
// 라이프사이클
// ───────────────────────────────────────────────────────────
onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.near-miss-detail {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-danger: #ef4444;
  --color-danger-tint: #fef2f2;
  --color-warning: #f59e0b;
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

  position: relative;
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
.nmd-hd {
  height: 56px;
  flex-shrink: 0;
  background: var(--color-surface);
  border-bottom: 0.5px solid var(--color-border);
  display: grid;
  grid-template-columns: 44px 1fr auto;
  align-items: center;
  padding-right: 16px;
  position: sticky;
  top: 0;
  z-index: 10;
  padding-top: env(safe-area-inset-top);
}
.nmd-hd__back {
  width: 44px;
  height: 44px;
  background: transparent;
  border: 0;
  color: var(--color-text-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-family: inherit;
}
.nmd-hd__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
.nmd-hd__spacer {
  width: 44px;
  height: 44px;
}

/* 본문 */
.nmd-body {
  flex: 1;
  padding: 14px 16px 24px;
  overflow-y: auto;
}
.nmd-loading,
.nmd-empty {
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-secondary);
}

/* 읽기 블록 */
.nmd-read {
  margin: 0 0 16px;
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 6px 14px;
}
.nmd-row {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 0.5px solid var(--color-border-light);
}
.nmd-row:last-child {
  border-bottom: 0;
}
.nmd-row dt {
  font-size: 13px;
  color: var(--color-text-secondary);
}
.nmd-row dd {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-primary);
}
.nmd-row__multiline {
  white-space: pre-wrap;
  line-height: 1.5;
}

/* 사진 */
.nmd-photo {
  margin-bottom: 16px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 0.5px solid var(--color-border);
}
.nmd-photo img {
  width: 100%;
  display: block;
  object-fit: cover;
}

/* 임시조치 */
.nmd-action {
  margin-bottom: 8px;
}
.nmd-label {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
}
.nmd-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text-primary);
  background: var(--color-surface);
  font-family: inherit;
  box-sizing: border-box;
  resize: vertical;
  line-height: 1.5;
}
.nmd-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}
.nmd-textarea:disabled {
  background: var(--color-bg);
  color: var(--color-text-tertiary);
}
.nmd-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
  line-height: 1.5;
}

/* 푸터 */
.nmd-footer {
  flex-shrink: 0;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  display: flex;
  gap: 8px;
}
.nmd-btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  font-family: inherit;
}
.nmd-btn--primary {
  flex: 2;
  background: var(--color-primary);
  color: #ffffff;
}
.nmd-btn--primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.nmd-btn--secondary {
  background: var(--color-surface);
  color: var(--color-danger);
  border: 1.5px solid var(--color-danger);
}

/* 잠재중대성 배지 */
.nmd-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
}
.nmd-badge--minor {
  background: var(--color-primary-tint);
  color: var(--color-primary);
}
.nmd-badge--major {
  background: var(--color-warning-tint);
  color: var(--color-warning-text);
}
.nmd-badge--critical {
  background: var(--color-danger-tint);
  color: var(--color-danger);
}
.nmd-badge--none {
  background: var(--color-border-light);
  color: var(--color-text-tertiary);
}

.nmd-sprite {
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
