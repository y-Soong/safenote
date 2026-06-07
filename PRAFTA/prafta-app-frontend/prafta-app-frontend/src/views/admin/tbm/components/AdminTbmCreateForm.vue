<!--
  AdminTbmCreateForm.vue — 탭2 TBM 교육 개설 폼
  - 작업 ID: 001-P5-T-F4 (분해: 001-phase5-admin-tbm-plan.md §2-2, §3 T-A3)
  - 백엔드: POST /appApi/admin/tbm/sessions (saveMode='DRAFT'|'OPENED', T-A3). 보조 옵션 T-K.
  - 필드(요청서 = web popup/TbmSessionForm.vue 정합):
      교육제목 / 교육내용 / GPS검증여부(AUTO 활성화 · MANUAL 수동확인 · DISABLED 비활성화) /
      현재 위치 표시(AUTO 시) / 검증 반경(AUTO·MANUAL 시, 50~1000m) / 교육자료 선택 / 위험성평가 선택
  - 액션(요청서): 임시저장(DRAFT) / 개설하기(OPENED, 비번 발급) / 닫기(미저장 시 삭제 확인 얼럿)
  - GPS: 앱 GPS 브리지 requestGps() 사용(TbmHubView 패턴). webview 네이티브 위치.
  - 교육내용 입력기: web 은 QuillEditor(리치HTML). 모바일은 plain textarea(저장 텍스트, 표시는 contentBody) — 플래그 T5.
  - 디자인 토큰은 부모(.admin-tbm-view)에서 상속. 자료/위험성 선택 시트는 후속 골격(R2) — 본 골격은 트리거/요약만.
  - planner 라운드 스코프: template + style 완성. script 는 선언/TODO + v-model + 단순 검증 + UI 토글만.
      ⚠️ API 호출/저장/라우팅/store 는 developer(R2).
-->
<template>
  <form class="admin-tbm-form" @submit.prevent="onOpen">
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
            @change="onChangeGpsType"
          />
          <span>{{ opt.label }}</span>
        </label>
      </div>
    </div>

    <!-- 현재 위치(AUTO 시) -->
    <div v-if="form.gpsVerifyTypeCd === 'AUTO'" class="admin-tbm-form__field">
      <span class="admin-tbm-form__label">현재 위치</span>
      <div class="admin-tbm-form__gps">
        <span v-if="gpsStatus === 'ok'" class="gps-ok">
          위도 {{ form.managerGpsLat }} / 경도 {{ form.managerGpsLon }}
        </span>
        <span v-else-if="gpsStatus === 'loading'" class="gps-muted">위치 수집 중…</span>
        <span v-else class="gps-muted">위치를 가져오지 못했어요</span>
        <button type="button" class="admin-tbm-form__gps-btn" @click="onCaptureGps">
          위치 다시 가져오기
        </button>
      </div>
    </div>

    <!-- 수동 확인 체크(MANUAL 시) -->
    <div v-if="form.gpsVerifyTypeCd === 'MANUAL'" class="admin-tbm-form__field">
      <label class="radio-item" :class="{ 'is-checked': manualConfirm }">
        <input type="checkbox" v-model="manualConfirm" />
        <span>개설 위치를 직접 확인했습니다</span>
      </label>
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

    <!-- 액션 -->
    <div class="admin-tbm-form__actions">
      <button type="button" class="btn btn--ghost" :disabled="submitting" @click="onClose">
        닫기
      </button>
      <button type="button" class="btn btn--second" :disabled="submitting" @click="onDraft">
        임시저장
      </button>
      <button type="submit" class="btn btn--primary" :disabled="submitting">개설하기</button>
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
import { requestGps } from '@/utils/gpsBridge'
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

// created: 개설/임시저장 성공 시 { sessionCd, saveMode, entryPwd, exitPwd } 전달(부모가 상세로 이동)
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
  managerGpsLat: '',
  managerGpsLon: '',
  gpsVerifyRadiusM: 100,
})
const manualConfirm = ref(false)
const gpsStatus = ref('idle') // idle | loading | ok | fail

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

// ── GPS ──────────────────────────────────────────────────────────
// GPS 유형 변경 시: AUTO 면 위치 수집 시도.
const onChangeGpsType = () => {
  if (form.gpsVerifyTypeCd === 'AUTO') onCaptureGps()
}

// 현재 위치 수집(앱 GPS 브리지 — TbmHubView/출퇴근 패턴 재사용).
const onCaptureGps = async () => {
  gpsStatus.value = 'loading'
  try {
    const gps = await requestGps()
    if (gps?.status === 'OK' && gps.lat != null && gps.lon != null) {
      form.managerGpsLat = String(gps.lat)
      form.managerGpsLon = String(gps.lon)
      gpsStatus.value = 'ok'
    } else {
      form.managerGpsLat = ''
      form.managerGpsLon = ''
      gpsStatus.value = 'fail'
    }
  } catch (e) {
    console.error('[AdminTbmCreateForm] GPS 수집 실패:', e?.message)
    gpsStatus.value = 'fail'
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
// 단순 필수/길이 검증만(허용 범위). 비즈니스 분기는 서버.
const validate = (mode) => {
  if (!form.siteCd) {
    showAlert('사업장을 선택해 주세요.')
    return false
  }
  if (!form.title) {
    showAlert('교육 제목을 입력해 주세요.')
    return false
  }
  if (mode === 'OPENED') {
    if ((form.contentBody || '').trim().length < 10) {
      showAlert('교육 내용을 10자 이상 입력해 주세요.')
      return false
    }
    if (form.gpsVerifyTypeCd === 'AUTO' && (!form.managerGpsLat || !form.managerGpsLon)) {
      showAlert('현재 위치를 가져오지 못했어요. 위치를 다시 가져오거나 수동 확인을 선택해 주세요.')
      return false
    }
    if (form.gpsVerifyTypeCd === 'MANUAL' && !manualConfirm.value) {
      showAlert('수동 확인 체크박스를 확인해 주세요.')
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
    contentRows.value.length ||
    riskRows.value.length
  )

// 저장 payload 구성(서버 계약 = T-A3). 식별자(회사/사용자)는 서버가 토큰에서 채운다(전송 안 함).
const buildPayload = (saveMode) => ({
  saveMode,
  siteCd: form.siteCd,
  title: form.title,
  contentBody: form.contentBody,
  gpsVerifyTypeCd: form.gpsVerifyTypeCd,
  managerGpsLat: form.gpsVerifyTypeCd === 'AUTO' ? form.managerGpsLat : '',
  managerGpsLon: form.gpsVerifyTypeCd === 'AUTO' ? form.managerGpsLon : '',
  gpsVerifyRadiusM: form.gpsVerifyTypeCd === 'DISABLED' ? null : form.gpsVerifyRadiusM,
  gpsManualConfirmYn: form.gpsVerifyTypeCd === 'MANUAL' && manualConfirm.value ? 'Y' : 'N',
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

// 공통 저장 — POST /appApi/admin/tbm/sessions. 성공 시 created emit(부모가 상세로 이동).
const submitSession = async (saveMode) => {
  submitting.value = true
  try {
    const { data } = await api.post('/appApi/admin/tbm/sessions', buildPayload(saveMode))
    if (!data?.sessionCd) {
      await showAlert('저장에 실패했어요. 잠시 후 다시 시도해 주세요.')
      return
    }
    if (data.warningMessage) {
      await showAlert(data.warningMessage)
    }
    emit('created', {
      sessionCd: data.sessionCd,
      saveMode: data.statusCd || saveMode,
      entryPwd: data.entryPwd,
      exitPwd: data.exitPwd,
    })
  } catch (e) {
    const msg = e?.response?.data?.message || '저장에 실패했어요. 잠시 후 다시 시도해 주세요.'
    await showAlert(msg)
  } finally {
    submitting.value = false
  }
}

// ── 액션 ──────────────────────────────────────────────────────────
// 임시저장(DRAFT)
const onDraft = async () => {
  if (submitting.value) return
  if (!validate('DRAFT')) return
  const ok = await askConfirm('임시저장하시겠어요?')
  if (!ok) return
  await submitSession('DRAFT')
}

// 개설하기(OPENED)
const onOpen = async () => {
  if (submitting.value) return
  if (!validate('OPENED')) return
  const ok = await askConfirm('TBM 교육을 개설하시겠어요? (입실/종료 비밀번호가 발급돼요.)')
  if (!ok) return
  await submitSession('OPENED')
}

// 닫기(미저장 삭제 확인 — 요청서: "저장하지 않은 내용은 삭제됩니다")
const onClose = async () => {
  if (isDirty()) {
    const ok = await askConfirm('저장하지 않은 내용은 삭제됩니다. 닫으시겠어요?')
    if (!ok) return
  }
  emit('close')
}

// 진입 시: 사업장 옵션 로드 + 기본 AUTO 모드 현재 위치 1회 수집.
onMounted(async () => {
  await loadSiteOptions()
  if (form.gpsVerifyTypeCd === 'AUTO') onCaptureGps()
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

/* GPS 현재 위치 */
.admin-tbm-form__gps {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.gps-ok {
  font-size: 14px;
  color: var(--color-text-primary);
}
.gps-muted {
  font-size: 13px;
  color: var(--color-text-tertiary);
}
.admin-tbm-form__gps-btn {
  align-self: flex-start;
  height: 36px;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
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
.btn--second {
  background: var(--color-surface);
  color: var(--color-primary);
  border: 1.5px solid var(--color-primary);
}
.btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}
</style>
