<!--
  AdminTbmSessionEditView.vue — 관리자 TBM 세션 수정(교육내용 위주)
  - 진입: 세션 상세 "수정" 버튼 → /AdminTbmSessionEdit?sessionCd=...
  - 백엔드: GET /appApi/admin/tbm/sessions/{sessionCd} (미리채움)
            PUT /appApi/admin/tbm/sessions/{sessionCd} (전체 교체 저장)
  - 편집 범위(사용자 확정): 제목·교육내용·GPS설정·교육시간만 편집 가능.
      교육자료/위험성평가는 읽기전용(잠금)으로 표시하되, 저장 payload 엔 현재값을 그대로 보존한다.
      (수정 EP 는 contents/risks 전체 삭제 후 재삽입 → 빈 배열로 보내면 연계가 전부 삭제됨. 절대 금지.)
  - 수정 가능 상태: DRAFT/OPENED (서버가 TBM_409_010 으로 최종 게이트).
  - saveMode 필드는 보내지 않는다(요청 DTO 에 없음).
  - C1: 권한/스코프는 서버(토큰)만 신뢰. 클라 역할 분기 없음. 회사/사용자코드 전송 안 함.
  - 디자인 토큰: 상세화면(AdminTbmSessionDetailView) 세트를 .admin-tbm-edit-view 루트에 1회 선언.
  - CSS 변수만, <style scoped>, TS 미사용.
-->
<template>
  <div class="admin-tbm-edit-view">
    <!-- 헤더 -->
    <header class="admin-tbm-hd">
      <button type="button" class="admin-tbm-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-admin-tbm-edit-chev-left" />
        </svg>
      </button>
      <h1 class="admin-tbm-hd__title">TBM 세션 수정</h1>
      <span class="admin-tbm-hd__spacer" aria-hidden="true" />
    </header>

    <main class="admin-tbm-edit-body">
      <!-- loading -->
      <p v-if="isLoading" class="admin-tbm-state">불러오는 중…</p>

      <!-- error -->
      <div v-else-if="loadError" class="admin-tbm-state">
        <p class="admin-tbm-state__msg">정보를 불러오지 못했어요.</p>
        <button type="button" class="admin-tbm-retry" @click="onRetry">다시 시도</button>
      </div>

      <form v-else class="admin-tbm-form" @submit.prevent="onSave">
        <!-- 교육 제목 -->
        <div class="admin-tbm-form__field">
          <label class="admin-tbm-form__label" for="tbm-edit-title">교육 제목</label>
          <input
            id="tbm-edit-title"
            v-model.trim="form.title"
            class="admin-tbm-form__input"
            type="text"
            maxlength="200"
            placeholder="교육 제목을 입력하세요"
          />
        </div>

        <!-- 교육 내용 -->
        <div class="admin-tbm-form__field">
          <label class="admin-tbm-form__label" for="tbm-edit-content">교육 내용</label>
          <textarea
            id="tbm-edit-content"
            v-model="form.contentBody"
            class="admin-tbm-form__textarea"
            rows="5"
            maxlength="4000"
            placeholder="교육 내용을 입력하세요"
          ></textarea>
        </div>

        <!-- GPS 검증 여부 -->
        <div class="admin-tbm-form__field">
          <span class="admin-tbm-form__label">GPS 검증</span>
          <div class="admin-tbm-form__radios">
            <label
              v-for="opt in gpsOptions"
              :key="opt.value"
              class="radio-item"
              :class="{ 'is-checked': form.gpsVerifyTypeCd === opt.value }"
            >
              <input
                type="radio"
                name="gpsVerifyType"
                :value="opt.value"
                v-model="form.gpsVerifyTypeCd"
              />
              <span>{{ opt.label }}</span>
            </label>
          </div>
        </div>

        <!-- 검증 반경(DISABLED 아닐 때만) -->
        <div v-if="form.gpsVerifyTypeCd !== 'DISABLED'" class="admin-tbm-form__field">
          <label class="admin-tbm-form__label" for="tbm-edit-radius">검증 반경 (m)</label>
          <input
            id="tbm-edit-radius"
            v-model.number="form.gpsVerifyRadiusM"
            class="admin-tbm-form__input admin-tbm-form__input--narrow"
            type="number"
            min="50"
            max="1000"
            inputmode="numeric"
          />
          <span class="admin-tbm-form__hint">50 ~ 1000m</span>
        </div>

        <!-- 교육 시간(분) — 빈값 허용, 있으면 1~60 정수 -->
        <div class="admin-tbm-form__field">
          <label class="admin-tbm-form__label" for="tbm-edit-edu-minutes">교육 시간 (분)</label>
          <input
            id="tbm-edit-edu-minutes"
            v-model.number="form.eduMinutes"
            class="admin-tbm-form__input admin-tbm-form__input--narrow"
            type="number"
            min="1"
            max="60"
            inputmode="numeric"
            placeholder="선택"
          />
          <span class="admin-tbm-form__hint">1분 이상 60분 이하</span>
        </div>

        <!-- 교육자료(읽기전용 — 수정 화면에서는 변경 불가) -->
        <div class="admin-tbm-form__field">
          <span class="admin-tbm-form__label">교육 자료</span>
          <p class="admin-tbm-form__lock-note">수정 화면에서는 변경할 수 없습니다.</p>
          <ul v-if="contents.length" class="name-list">
            <li v-for="(c, i) in contents" :key="c.mtrlCd || i" class="name-list__item">
              {{ c.title || '(제목 없음)' }}
            </li>
          </ul>
          <p v-else class="admin-tbm-form__empty">등록된 자료가 없어요</p>
        </div>

        <!-- 위험성평가(읽기전용 — 수정 화면에서는 변경 불가) -->
        <div class="admin-tbm-form__field">
          <span class="admin-tbm-form__label">위험성평가</span>
          <p class="admin-tbm-form__lock-note">수정 화면에서는 변경할 수 없습니다.</p>
          <ul v-if="risks.length" class="name-list">
            <li v-for="(r, i) in risks" :key="i" class="name-list__item">
              {{ r.displayName || '(이름 미정)' }}
            </li>
          </ul>
          <p v-else class="admin-tbm-form__empty">연계된 위험성평가가 없어요</p>
        </div>

        <!-- 액션 (F-10 규약: 왼쪽=진행/확정(저장), 오른쪽=이탈(취소)) -->
        <div class="admin-tbm-form__actions">
          <button type="submit" class="btn btn--primary" :disabled="submitting">저장</button>
          <button type="button" class="btn btn--ghost" :disabled="submitting" @click="onCancel">
            취소
          </button>
        </div>
      </form>
    </main>

    <!-- 아이콘 스프라이트 -->
    <svg width="0" height="0" class="admin-tbm-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-admin-tbm-edit-chev-left"
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
import { ref, reactive, computed, getCurrentInstance, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

import api from '@/api/axios'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance() || { proxy: null }

// 공통: alert/confirm 폴백(앱 전역 우선) — AdminTbmCreateForm 패턴 동일
const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}

// GPS 검증 옵션(SYS048 — 개설 폼과 동일)
const gpsOptions = [
  { value: 'AUTO', label: '활성화' },
  { value: 'MANUAL', label: '수동 확인' },
  { value: 'DISABLED', label: '비활성화' },
]

// ── 상태 ──────────────────────────────────────────────────────────
const sessionCd = computed(() => route.query.sessionCd || '')
const isLoading = ref(false)
const loadError = ref(false)
const submitting = ref(false)

// 편집 대상 필드(서버 session 에서 미리채움). gpsManualConfirmYn/managerGpsLat/Lon 은
// 입력 UI 없이 기존값을 그대로 보존한다(hidden state).
const form = reactive({
  title: '',
  contentBody: '',
  gpsVerifyTypeCd: 'AUTO',
  gpsVerifyRadiusM: 100,
  eduMinutes: null,
  gpsManualConfirmYn: 'N',
  managerGpsLat: null,
  managerGpsLon: null,
})

// 읽기전용 보존 목록(저장 payload 에 그대로 다시 실어 연계 보존).
const contents = ref([]) // [{ mtrlCd, title, overrideDesc, ... }]
const risks = ref([]) // [{ siteCd, processCd, assessmentCd, displayName, ... }]

// ── 조회(미리채움) ──────────────────────────────────────────────────
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
    const s = data?.session || null
    if (!s) {
      loadError.value = true
      return
    }
    form.title = s.title || ''
    form.contentBody = s.contentBody || ''
    form.gpsVerifyTypeCd = s.gpsVerifyTypeCd || 'AUTO'
    form.gpsVerifyRadiusM = s.gpsVerifyRadiusM != null ? s.gpsVerifyRadiusM : 100
    form.eduMinutes = s.eduMinutes != null ? s.eduMinutes : null
    form.gpsManualConfirmYn = s.gpsManualConfirmYn || 'N'
    form.managerGpsLat = s.managerGpsLat != null ? s.managerGpsLat : null
    form.managerGpsLon = s.managerGpsLon != null ? s.managerGpsLon : null
    contents.value = Array.isArray(data?.contents) ? data.contents : []
    risks.value = Array.isArray(data?.risks) ? data.risks : []
  } catch (e) {
    console.error('[AdminTbmSessionEditView] 상세 조회 실패:', e?.message)
    loadError.value = true
  } finally {
    isLoading.value = false
  }
}

const onRetry = () => loadDetail()

// ── 검증(클라이언트 1차 — 서버가 최종 권위) ──────────────────────
const validate = () => {
  if (!form.title) {
    showAlert('교육 제목을 입력해 주세요.')
    return false
  }
  if (form.title.length > 200) {
    showAlert('교육 제목은 200자 이하로 입력해 주세요.')
    return false
  }
  // 교육 시간: 빈값 허용. 값이 있으면 1~60 정수만.
  if (form.eduMinutes !== null && form.eduMinutes !== '' && form.eduMinutes !== undefined) {
    const m = Number(form.eduMinutes)
    if (!Number.isInteger(m) || m < 1 || m > 60) {
      showAlert('교육 시간은 1분 이상 60분 이하로 입력해 주세요.')
      return false
    }
  }
  return true
}

// 저장 payload 구성(전체 교체 EP 계약). contents/risks 는 현재값을 그대로 다시 실어 연계 보존.
// saveMode 는 보내지 않는다(요청 DTO 에 없음). 식별자(회사/사용자)는 서버가 토큰에서 채운다.
const buildPayload = () => ({
  title: form.title,
  contentBody: form.contentBody,
  gpsVerifyTypeCd: form.gpsVerifyTypeCd,
  managerGpsLat: form.managerGpsLat,
  managerGpsLon: form.managerGpsLon,
  gpsVerifyRadiusM: form.gpsVerifyTypeCd === 'DISABLED' ? null : Number(form.gpsVerifyRadiusM),
  eduMinutes:
    form.eduMinutes === null || form.eduMinutes === '' || form.eduMinutes === undefined
      ? null
      : Number(form.eduMinutes),
  gpsManualConfirmYn: form.gpsManualConfirmYn,
  contents: contents.value.map((c, i) => ({
    mtrlCd: c.mtrlCd,
    displayOrder: i,
    overrideDesc: c.overrideDesc || null,
  })),
  risks: risks.value.map((r, i) => ({
    siteCd: r.siteCd,
    processCd: r.processCd,
    assessmentCd: r.assessmentCd,
    displayOrder: i,
  })),
})

// ── 액션 ──────────────────────────────────────────────────────────
const onBack = () => {
  router.back()
}

const onCancel = () => {
  router.back()
}

// 저장 — PUT /appApi/admin/tbm/sessions/{sessionCd}. 성공 시 상세로 복귀(상세가 새로 로드되게).
const onSave = async () => {
  if (submitting.value) return
  if (!validate()) return
  submitting.value = true
  try {
    await api.put(
      `/appApi/admin/tbm/sessions/${encodeURIComponent(sessionCd.value)}`,
      buildPayload(),
    )
    await showAlert('수정되었습니다.')
    router.replace({ path: '/AdminTbmSessionDetail', query: { sessionCd: sessionCd.value } })
  } catch (e) {
    const msg = e?.response?.data?.message || '수정에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    submitting.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
/* 디자인 토큰 1회 선언(AdminTbmSessionDetailView 세트) */
.admin-tbm-edit-view {
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

/* 본문(스크롤 컨테이너 — 문서 스크롤 누수 방지) */
.admin-tbm-edit-body {
  flex: 1;
  min-height: 0;
  padding: var(--space-md) var(--space-lg) calc(var(--space-lg) + env(safe-area-inset-bottom, 0px));
  overflow-y: auto;
}

/* 폼 */
.admin-tbm-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  padding-bottom: var(--space-lg);
}
.admin-tbm-form__field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.admin-tbm-form__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.admin-tbm-form__hint {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.admin-tbm-form__lock-note {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

/* 인풋/셀렉트/텍스트영역 */
.admin-tbm-form__input,
.admin-tbm-form__textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 0 var(--space-md);
  height: 44px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
}
.admin-tbm-form__input--narrow {
  width: 140px;
}
.admin-tbm-form__textarea {
  height: auto;
  padding: var(--space-md);
  resize: vertical;
  line-height: 1.5;
}
.admin-tbm-form__input:focus,
.admin-tbm-form__textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* GPS 라디오 */
.admin-tbm-form__radios {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.radio-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 10px var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text-primary);
  cursor: pointer;
  background: var(--color-surface);
}
.radio-item.is-checked {
  border-color: var(--color-primary);
  background: var(--color-primary-tint);
  color: var(--color-primary);
  font-weight: 600;
}

/* 읽기전용 이름 리스트(상세화면 .name-list 재사용) */
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
.admin-tbm-form__empty {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* 상태 메시지 */
.admin-tbm-state {
  margin: var(--space-lg) 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-tertiary);
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
.admin-tbm-form__actions {
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
