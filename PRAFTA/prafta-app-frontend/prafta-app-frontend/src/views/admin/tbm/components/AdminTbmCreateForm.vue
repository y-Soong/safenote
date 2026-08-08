<!--
  AdminTbmCreateForm.vue — 탭2 TBM 교육 개설 폼
  - 작업 ID: 001-P5-T-F4 (분해: 001-phase5-admin-tbm-plan.md §2-2, §3 T-A3)
  - 백엔드: POST /appApi/admin/tbm/sessions (saveMode='DRAFT'|'OPENED', T-A3). 보조 옵션 T-K.
  - 필드(요청서 = web popup/TbmSessionForm.vue 정합):
      교육제목 / 교육내용 / GPS검증여부(AUTO 활성화 · MANUAL 수동확인 · DISABLED 비활성화) /
      현재 위치 표시(AUTO 시) / 검증 반경(AUTO·MANUAL 시, 50~1000m) / 교육자료 선택 / 위험성평가 선택
  - 액션(R-A/#D-RE1): 개설(DRAFT 생성) / 닫기(미저장 시 삭제 확인 얼럿).
      OPENED(교육준비) 도달은 세션상세 "교육준비 시작"(/prepare 전이)으로 일원화 → POST /sessions 는 DRAFT 만 허용.
  - GPS: 검증유형(AUTO/MANUAL/DISABLED)·반경은 세션 설정값으로 개설 단계 유지.
      관리자 현재좌표 수집(requestGps)은 교육준비(/prepare) 전이에서 수행 → 개설 폼에서 제거.
  - 교육내용 입력기: web 은 QuillEditor(리치HTML). 모바일은 plain textarea(저장 텍스트, 표시는 contentBody) — 플래그 T5.
  - 디자인 토큰은 부모(.admin-tbm-view)에서 상속. 자료/위험성 선택 시트는 후속 골격(R2) — 본 골격은 트리거/요약만.
  - planner 라운드 스코프: template + style 완성. script 는 선언/TODO + v-model + 단순 검증 + UI 토글만.
      ⚠️ API 호출/저장/라우팅/store 는 developer(R2).
-->
<template>
  <form class="admin-tbm-form" @submit.prevent="onCreate">
    <!-- 사업장(접근가능 사업장 셀렉트 — 기본 현재 사업장. 플래그 T6) -->
    <div class="admin-tbm-form__field">
      <label class="admin-tbm-form__label" for="tbm-site">사업장</label>
      <select id="tbm-site" v-model="form.siteCd" class="admin-tbm-form__select">
        <option value="">선택</option>
        <option v-for="opt in siteOptions" :key="opt.siteCd" :value="opt.siteCd">
          {{ opt.siteNm }}
        </option>
      </select>
    </div>

    <!-- 교육 제목 -->
    <div class="admin-tbm-form__field">
      <label class="admin-tbm-form__label" for="tbm-title">교육 제목</label>
      <input
        id="tbm-title"
        v-model.trim="form.title"
        class="admin-tbm-form__input"
        type="text"
        maxlength="200"
        placeholder="교육 제목을 입력하세요"
      />
    </div>

    <!-- 교육 내용(모바일 textarea — T5) -->
    <div class="admin-tbm-form__field">
      <label class="admin-tbm-form__label" for="tbm-content">교육 내용</label>
      <textarea
        id="tbm-content"
        v-model="form.contentBody"
        class="admin-tbm-form__textarea"
        rows="5"
        maxlength="4000"
        placeholder="교육 내용을 입력하세요 (개설 시 10자 이상)"
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

    <!-- 검증 반경(AUTO·MANUAL 시) -->
    <div v-if="form.gpsVerifyTypeCd !== 'DISABLED'" class="admin-tbm-form__field">
      <label class="admin-tbm-form__label" for="tbm-radius">검증 반경 (m)</label>
      <input
        id="tbm-radius"
        v-model.number="form.gpsVerifyRadiusM"
        class="admin-tbm-form__input admin-tbm-form__input--narrow"
        type="number"
        min="50"
        max="1000"
        inputmode="numeric"
      />
      <span class="admin-tbm-form__hint">50 ~ 1000m</span>
    </div>

    <!-- 교육 시간(분) — 사용자 교육 완료 인정시간. 빈값 허용(개설), 있으면 1~60 -->
    <div class="admin-tbm-form__field">
      <label class="admin-tbm-form__label" for="tbm-edu-minutes">교육 시간 (분)</label>
      <input
        id="tbm-edu-minutes"
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

    <!-- 교육자료 선택 -->
    <div class="admin-tbm-form__field">
      <div class="admin-tbm-form__field-head">
        <span class="admin-tbm-form__label">교육자료</span>
        <button type="button" class="admin-tbm-form__pick" @click="onPickContents">
          교육자료 선택
        </button>
      </div>
      <ul v-if="contentRows.length" class="admin-tbm-form__chips">
        <li v-for="(c, i) in contentRows" :key="c.mtrlCd || i" class="picked-chip">
          <span class="picked-chip__name">{{ c.title }}</span>
          <button
            type="button"
            class="picked-chip__remove"
            aria-label="삭제"
            @click="onRemoveContent(i)"
          >
            ×
          </button>
        </li>
      </ul>
      <p v-else class="admin-tbm-form__empty">선택된 교육자료가 없어요</p>
    </div>

    <!-- 위험성평가 선택 -->
    <div class="admin-tbm-form__field">
      <div class="admin-tbm-form__field-head">
        <span class="admin-tbm-form__label">위험성평가</span>
        <button type="button" class="admin-tbm-form__pick" @click="onPickRisks">
          위험성평가 선택
        </button>
      </div>
      <ul v-if="riskRows.length" class="admin-tbm-form__chips">
        <li v-for="(r, i) in riskRows" :key="riskKey(r)" class="picked-chip">
          <span class="picked-chip__name">{{ r.displayName || '(이름 미정)' }}</span>
          <button
            type="button"
            class="picked-chip__remove"
            aria-label="삭제"
            @click="onRemoveRisk(i)"
          >
            ×
          </button>
        </li>
      </ul>
      <p v-else class="admin-tbm-form__warn">위험성평가가 연계되지 않았어요 (개설은 가능)</p>
    </div>

    <!-- 액션 (F-10 규약: 왼쪽=진행/확정(개설), 오른쪽=이탈(닫기)) -->
    <div class="admin-tbm-form__actions">
      <button type="submit" class="btn btn--primary" :disabled="submitting">개설</button>
      <button type="button" class="btn btn--ghost" :disabled="submitting" @click="onClose">
        닫기
      </button>
    </div>

    <!-- 교육자료/위험성평가 선택 시트 (R2-gap developer 연동) -->
    <AdminTbmContentPickSheet
      :open="contentSheetOpen"
      :site-cd="form.siteCd"
      :selected="contentRows"
      @close="contentSheetOpen = false"
      @confirm="onConfirmContents"
    />
    <AdminTbmRiskPickSheet
      :open="riskSheetOpen"
      :site-cd="form.siteCd"
      :selected="riskRows"
      @close="riskSheetOpen = false"
      @confirm="onConfirmRisks"
    />
  </form>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, onMounted } from 'vue'
import api from '@/api/axios'
import AdminTbmContentPickSheet from './AdminTbmContentPickSheet.vue'
import AdminTbmRiskPickSheet from './AdminTbmRiskPickSheet.vue'

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

// created: 개설(DRAFT) 성공 시 { sessionCd, saveMode } 전달(부모가 세션상세로 이동).
//   비번(entryPwd/exitPwd)은 교육준비(/prepare) 시 발급 → 개설 단계에서는 전달 안 함.
// close: 닫기(미저장 삭제 확인 후)
const emit = defineEmits(['created', 'close'])

// GPS 검증 옵션(SYS048 — 마이그레이션 정합)
const gpsOptions = [
  { value: 'AUTO', label: '활성화' },
  { value: 'MANUAL', label: '수동 확인' },
  { value: 'DISABLED', label: '비활성화' },
]

// ── 폼 상태(developer: 초기값/리셋 보완) ──────────────────────
const form = reactive({
  siteCd: '',
  title: '',
  contentBody: '',
  gpsVerifyTypeCd: 'AUTO',
  gpsVerifyRadiusM: 100,
  eduMinutes: null, // 교육 인정시간(분). 개설 시 빈값 허용, 있으면 1~60.
})

// 선택 목록(자료/위험성). 선택 시트(R2-gap)에서 다중선택 → 칩 반영.
const contentRows = ref([]) // [{ mtrlCd, title }]
const riskRows = ref([]) // [{ siteCd, processCd, assessmentCd, displayName }]

// 선택 시트 열림 상태(R2-gap)
const contentSheetOpen = ref(false)
const riskSheetOpen = ref(false)

// 사업장 옵션(access-context.accessibleSites 와 동일 소스 — GET /appApi/admin/tbm/site-options)
const siteOptions = ref([])

// 저장 진행 가드
const submitting = ref(false)

const riskKey = (r) => [r.siteCd, r.processCd, r.assessmentCd].join('|')

// 사업장 옵션 조회. 단일 사업장이면 기본 선택.
const loadSiteOptions = async () => {
  try {
    const { data } = await api.get('/appApi/admin/tbm/site-options')
    siteOptions.value = Array.isArray(data?.sites) ? data.sites : []
    if (!form.siteCd && siteOptions.value.length === 1) {
      form.siteCd = siteOptions.value[0].siteCd
    }
  } catch (e) {
    console.error('[AdminTbmCreateForm] 사업장 옵션 조회 실패:', e?.message)
  }
}

// ── 자료/위험성 선택 ───────────────────────────────────────────────
// 트리거: 사업장 선택 후 시트 open. 시트가 현재 선택(contentRows/riskRows)을 seed 로 받아
//   다중선택 결과 전체를 confirm 으로 돌려준다(기존 선택 유지/병합).
const onPickContents = () => {
  if (!form.siteCd) {
    showAlert('먼저 사업장을 선택해 주세요.')
    return
  }
  contentSheetOpen.value = true
}
// 시트 확인: 선택 전체([{ mtrlCd, title }])를 contentRows 에 반영(기존 overrideDesc 보존).
const onConfirmContents = (picked) => {
  const prev = new Map(contentRows.value.map((c) => [c.mtrlCd, c]))
  contentRows.value = (picked || []).map((c) => ({
    mtrlCd: c.mtrlCd,
    title: c.title,
    overrideDesc: prev.get(c.mtrlCd)?.overrideDesc || null,
  }))
  contentSheetOpen.value = false
}
const onRemoveContent = (i) => {
  contentRows.value.splice(i, 1)
}
const onPickRisks = () => {
  if (!form.siteCd) {
    showAlert('먼저 사업장을 선택해 주세요.')
    return
  }
  riskSheetOpen.value = true
}
// 시트 확인: 선택 전체([{ siteCd, processCd, assessmentCd, displayName }])를 riskRows 에 반영.
const onConfirmRisks = (picked) => {
  riskRows.value = (picked || []).map((r) => ({
    siteCd: r.siteCd,
    processCd: r.processCd,
    assessmentCd: r.assessmentCd,
    displayName: r.displayName,
  }))
  riskSheetOpen.value = false
}
const onRemoveRisk = (i) => {
  riskRows.value.splice(i, 1)
}

// ── 검증(클라이언트 1차 — 서버가 최종 권위) ──────────────────────
// 개설(DRAFT)은 단순 필수 검증만. GPS 좌표/내용 길이 등 OPENED 게이트는 교육준비(/prepare) 단계로 이관.
const validate = () => {
  if (!form.siteCd) {
    showAlert('사업장을 선택해 주세요.')
    return false
  }
  if (!form.title) {
    showAlert('교육 제목을 입력해 주세요.')
    return false
  }
  // 교육 시간: 빈값 허용(개설). 값이 있으면 1~60 정수만.
  if (form.eduMinutes !== null && form.eduMinutes !== '' && form.eduMinutes !== undefined) {
    const m = Number(form.eduMinutes)
    if (!Number.isInteger(m) || m < 1 || m > 60) {
      showAlert('교육 시간은 1분 이상 60분 이하로 입력해 주세요.')
      return false
    }
  }
  return true
}

// 미저장 변경 여부(닫기 확인용 — 단순 dirty 체크)
const isDirty = () =>
  !!(
    form.title ||
    (form.contentBody || '').trim() ||
    (form.eduMinutes !== null && form.eduMinutes !== '' && form.eduMinutes !== undefined) ||
    contentRows.value.length ||
    riskRows.value.length
  )

// 저장 payload 구성(서버 계약 = T-A3, #D-RE1: DRAFT 만 허용). 식별자(회사/사용자)는 서버가 토큰에서 채운다(전송 안 함).
// 관리자 현재좌표(managerGpsLat/Lon)·수동확인(gpsManualConfirmYn)은 개설 단계 미수집 → 교육준비(/prepare) 에서 전송.
const buildPayload = () => ({
  saveMode: 'DRAFT',
  siteCd: form.siteCd,
  title: form.title,
  contentBody: form.contentBody,
  gpsVerifyTypeCd: form.gpsVerifyTypeCd,
  gpsVerifyRadiusM: form.gpsVerifyTypeCd === 'DISABLED' ? null : form.gpsVerifyRadiusM,
  eduMinutes:
    form.eduMinutes === null || form.eduMinutes === '' || form.eduMinutes === undefined
      ? null
      : Number(form.eduMinutes),
  contents: contentRows.value.map((c, i) => ({
    mtrlCd: c.mtrlCd,
    displayOrder: i,
    overrideDesc: c.overrideDesc || null,
  })),
  risks: riskRows.value.map((r, i) => ({
    siteCd: r.siteCd,
    processCd: r.processCd,
    assessmentCd: r.assessmentCd,
    displayOrder: i,
  })),
})

// 개설(DRAFT) 저장 — POST /appApi/admin/tbm/sessions. 성공 시 created emit(부모가 세션상세로 이동).
const submitSession = async () => {
  submitting.value = true
  try {
    const { data } = await api.post('/appApi/admin/tbm/sessions', buildPayload())
    if (!data?.sessionCd) {
      await showAlert('저장에 실패했어요. 잠시 후 다시 시도해 주세요.')
      return
    }
    if (data.warningMessage) {
      await showAlert(data.warningMessage)
    }
    emit('created', {
      sessionCd: data.sessionCd,
      saveMode: data.statusCd || 'DRAFT',
    })
  } catch (e) {
    const msg = e?.response?.data?.message || '저장에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    submitting.value = false
  }
}

// ── 액션 ──────────────────────────────────────────────────────────
// 개설(DRAFT 생성). OPENED(교육준비) 도달은 세션상세 "교육준비 시작"(/prepare)에서 처리.
const onCreate = async () => {
  if (submitting.value) return
  if (!validate()) return
  const ok = await askConfirm('TBM 교육을 개설하시겠어요?')
  if (!ok) return
  await submitSession()
}

// 닫기(미저장 삭제 확인 — 요청서: "저장하지 않은 내용은 삭제됩니다")
const onClose = async () => {
  if (isDirty()) {
    const ok = await askConfirm('저장하지 않은 내용은 삭제됩니다. 닫으시겠어요?')
    if (!ok) return
  }
  emit('close')
}

// 진입 시: 사업장 옵션 로드(현재좌표 수집은 교육준비 단계로 이관 → 개설 폼에서 미수행).
onMounted(async () => {
  await loadSiteOptions()
})
</script>

<style scoped>
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
.admin-tbm-form__field-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

/* 인풋/셀렉트/텍스트영역 */
.admin-tbm-form__input,
.admin-tbm-form__select,
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
.admin-tbm-form__select:focus,
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

/* 자료/위험성 선택 트리거 + 칩 */
.admin-tbm-form__pick {
  height: 32px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-full);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.admin-tbm-form__chips {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.picked-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  padding: 10px var(--space-md);
  background: var(--color-bg);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
}
.picked-chip__name {
  font-size: 14px;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.picked-chip__remove {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  background: transparent;
  border: 0;
  font-size: 18px;
  line-height: 1;
  color: var(--color-text-tertiary);
  cursor: pointer;
  font-family: inherit;
}
.admin-tbm-form__empty {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.admin-tbm-form__warn {
  margin: 0;
  font-size: 13px;
  color: var(--color-danger-text);
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
</style>
