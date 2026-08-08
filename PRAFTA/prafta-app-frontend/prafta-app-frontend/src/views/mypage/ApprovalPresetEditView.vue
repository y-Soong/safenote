<!--
  ApprovalPresetEditView.vue — 프리셋 편집/신규 (모바일 앱)
  - 작업 ID: PRAFTA-APP-010-14 (분해: .claude/requests/app_requests/prafta-app-010-plan.md)
  - UI 명세: UI-A014
  - 진입: 카드 탭(수정, query.presetId) / 추가 카드(신규). 헤더 🗑 삭제는 수정 모드만.
  - planner 라운드 스코프: 이름 + 기본토글 + 결재자 리스트(순서/삭제) + 추가시트 + 푸터 (template/style)
  - developer 라운드 스코프(아래 TODO): 상세 조회/저장/삭제/set-default(010-05), 후보 조회, 검증
  - 디자인 토큰: MyLeaveSummaryView 세트를 .preset-edit-view 루트에 1회 선언.
-->
<template>
  <div class="preset-edit-view">
    <!-- 헤더 -->
    <header class="ed-hd">
      <button type="button" class="ed-hd__back" aria-label="뒤로" @click="onBack">
        <svg class="icon" width="22" height="22" aria-hidden="true">
          <use href="#i-ed-chev-left" />
        </svg>
      </button>
      <h1 class="ed-hd__title">프리셋 편집</h1>
      <!-- 수정 모드만 삭제 노출 -->
      <button
        v-if="isEditMode"
        type="button"
        class="ed-hd__delete"
        aria-label="프리셋 삭제"
        @click="onDelete"
      >
        <svg class="icon" width="20" height="20" aria-hidden="true">
          <use href="#i-ed-trash" />
        </svg>
      </button>
      <span v-else class="ed-hd__spacer" aria-hidden="true"></span>
    </header>

    <!-- 본문 -->
    <main class="ed-body">
      <div v-if="isLoading" class="ed-loading" aria-live="polite">불러오는 중...</div>

      <template v-else>
        <!-- 프리셋 이름 -->
        <div class="ed-field">
          <label class="ed-field__label" for="edName">프리셋 이름</label>
          <input
            id="edName"
            v-model="presetNm"
            type="text"
            class="ed-input"
            maxlength="100"
            placeholder="예: 팀 결재선"
          />
          <p v-if="nameError" class="ed-helper ed-helper--danger">{{ nameError }}</p>
        </div>

        <!-- 기본 프리셋 토글 -->
        <div class="ed-toggle-row">
          <div class="ed-toggle-row__text">
            <p class="ed-toggle-row__title">기본 프리셋으로 사용</p>
            <p class="ed-toggle-row__desc">{{ defaultHelperText }}</p>
          </div>
          <button
            type="button"
            class="ed-switch"
            :class="{ 'ed-switch--on': isDefault }"
            role="switch"
            :aria-checked="isDefault"
            :disabled="forceDefaultOn"
            @click="onToggleDefault"
          >
            <span class="ed-switch__thumb" aria-hidden="true"></span>
          </button>
        </div>

        <!-- 결재자 리스트 -->
        <div class="ed-approvers">
          <div class="ed-approvers__head">
            <p class="ed-approvers__title">결재자</p>
            <span class="ed-approvers__hint">위에서 아래 순서대로 결재됩니다</span>
          </div>

          <!-- 빈 상태 -->
          <p v-if="approvers.length === 0" class="ed-approvers__empty">
            결재자를 1명 이상 추가해 주세요
          </p>

          <!-- 결재자 행 -->
          <div v-for="(approver, idx) in approvers" :key="approver.userCd" class="ed-row">
            <span class="ed-row__seq" aria-hidden="true">{{ idx + 1 }}</span>
            <div class="ed-row__info">
              <p class="ed-row__name">{{ approver.userNm }}</p>
              <p class="ed-row__meta">{{ metaOf(approver) }}</p>
            </div>
            <div class="ed-row__actions">
              <button
                type="button"
                class="ed-row__btn"
                aria-label="위로"
                :disabled="idx === 0"
                @click="onMoveUp(idx)"
              >
                <svg class="icon" width="18" height="18" aria-hidden="true">
                  <use href="#i-ed-up" />
                </svg>
              </button>
              <button
                type="button"
                class="ed-row__btn"
                aria-label="아래로"
                :disabled="idx === approvers.length - 1"
                @click="onMoveDown(idx)"
              >
                <svg class="icon" width="18" height="18" aria-hidden="true">
                  <use href="#i-ed-down" />
                </svg>
              </button>
              <button
                type="button"
                class="ed-row__btn ed-row__btn--remove"
                aria-label="삭제"
                @click="onRemoveApprover(idx)"
              >
                <svg class="icon" width="18" height="18" aria-hidden="true">
                  <use href="#i-ed-x" />
                </svg>
              </button>
            </div>
          </div>

          <!-- 결재자 추가 (dashed) -->
          <button type="button" class="ed-add" @click="onAddApprover">
            <svg class="icon" width="18" height="18" aria-hidden="true">
              <use href="#i-ed-plus" />
            </svg>
            결재자 추가
          </button>
        </div>
      </template>
    </main>

    <!-- 푸터 (F-10 규약: 왼쪽=진행/확정(저장), 오른쪽=이탈(취소), 폭 균등) -->
    <footer class="ed-footer">
      <button
        type="button"
        class="ed-btn ed-btn--primary"
        :class="{ 'ed-btn--off': !canSave }"
        :disabled="!canSave"
        @click="onSave"
      >
        저장
      </button>
      <button type="button" class="ed-btn ed-btn--ghost" @click="onCancel">취소</button>
    </footer>

    <!-- 결재자 추가 바텀시트 (010-22) — HB-14(F-6) 통합 공용 시트.
         프리셋 후보는 앱 전용 GET /appApi/mypage/approval-candidates 이므로 source="mypage". -->
    <ApproverPickerSheet
      v-model="pickerOpen"
      :excluded-user-cds="selectedUserCds"
      source="mypage"
      @add="onApproversAdded"
    />

    <!-- 인라인 SVG sprite -->
    <svg width="0" height="0" class="ed-sprite" aria-hidden="true" focusable="false">
      <defs>
        <symbol
          id="i-ed-chev-left"
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
          id="i-ed-trash"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="3 6 5 6 21 6" />
          <path
            d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
          />
        </symbol>
        <symbol
          id="i-ed-up"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="18 15 12 9 6 15" />
        </symbol>
        <symbol
          id="i-ed-down"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polyline points="6 9 12 15 18 9" />
        </symbol>
        <symbol
          id="i-ed-x"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </symbol>
        <symbol
          id="i-ed-plus"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </symbol>
      </defs>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'

import api from '@/api/axios'

import ApproverPickerSheet from '@/components/common/ApproverPickerSheet.vue'

const router = useRouter()
const route = useRoute()
const { proxy } = getCurrentInstance() || { proxy: null }

const showAlert = (message) => {
  if (proxy?.$alert) return proxy.$alert(message)
  window.alert(message)
  return Promise.resolve()
}
const askConfirm = async (message, variant) => {
  if (proxy?.$confirm) return await proxy.$confirm(message, { variant })
  return window.confirm(message)
}

// ───────────────────────────────────────────────────────────
// 모드 판정 (query.presetId 유무) — 단순 분기(허용)
// ───────────────────────────────────────────────────────────
const presetId = ref(route.query.presetId || '')
const isEditMode = computed(() => !!presetId.value)

// ───────────────────────────────────────────────────────────
// 상태
// ───────────────────────────────────────────────────────────
const isLoading = ref(true)

const presetNm = ref('')
const isDefault = ref(false)
// approvers: [{ userCd, userNm, rankNm, nodeNm }]
const approvers = ref([])

// 서버 에러
const nameError = ref('')

// 바텀시트 토글 (UI — 허용)
const pickerOpen = ref(false)

// 신규 + 첫 프리셋이면 기본 ON 강제(OFF 불가). 보유 프리셋 0개 여부로 판정.
const isFirstPreset = ref(false)

// 저장/삭제 진행 중 (중복 제출 방지)
const isSaving = ref(false)
const isDeleting = ref(false)

// 현재 사용자가 보유한 기본 프리셋이 본 프리셋 하나뿐인지(유일 기본 OFF 차단 판정용).
//   수정 모드에서 진입 시 본 프리셋이 유일한 기본이면 OFF 불가.
const isOnlyDefault = ref(false)

// 변경 감지(취소 시 확인). 진입 직후 스냅샷과 비교.
const initialSnapshot = ref('')
const currentSnapshot = computed(() =>
  JSON.stringify({
    presetNm: presetNm.value,
    isDefault: isDefault.value,
    userCds: approvers.value.map((a) => a.userCd),
  }),
)
const isDirty = computed(() => initialSnapshot.value !== currentSnapshot.value)

// ───────────────────────────────────────────────────────────
// 파생값 (단순 표시 — 비즈니스 로직 아님)
// ───────────────────────────────────────────────────────────
const selectedUserCds = computed(() => approvers.value.map((a) => a.userCd))

const metaOf = (approver) => {
  return [approver.nodeNm, approver.rankNm].filter(Boolean).join(' · ')
}

// 신규 첫 프리셋: 기본 토글 ON 고정·OFF 불가
const forceDefaultOn = computed(() => !isEditMode.value && isFirstPreset.value)

const defaultHelperText = computed(() =>
  forceDefaultOn.value ? '첫 프리셋은 자동으로 기본이 됩니다' : '연차 신청 시 자동 적용됩니다.',
)

// 저장 가능: 이름 1자+ AND 결재자 1명+ (본인 미포함/중복 없음은 서버 검증 + developer 보완)
const canSave = computed(
  () => !!presetNm.value && !!presetNm.value.trim() && approvers.value.length > 0,
)

// ───────────────────────────────────────────────────────────
// 기본 토글 (UI 토글 — 허용. 유일 기본 OFF 차단은 developer가 보유 프리셋 상태로 판정)
// ───────────────────────────────────────────────────────────
const onToggleDefault = () => {
  if (forceDefaultOn.value) return
  // 현재 ON 상태를 끄려는데, 본 프리셋이 유일한 기본 프리셋이면 차단(최소 1개 기본 필요).
  if (isDefault.value && isOnlyDefault.value) {
    showAlert('최소 1개의 기본 프리셋이 필요해요. 다른 프리셋을 기본으로 지정한 뒤 변경해 주세요.')
    return
  }
  isDefault.value = !isDefault.value
}

// ───────────────────────────────────────────────────────────
// 결재자 순서 변경 / 삭제 (로컬 배열 조작 — UI 상태, 허용)
// ───────────────────────────────────────────────────────────
const onMoveUp = (idx) => {
  if (idx === 0) return
  const arr = approvers.value
  ;[arr[idx - 1], arr[idx]] = [arr[idx], arr[idx - 1]]
}
const onMoveDown = (idx) => {
  const arr = approvers.value
  if (idx === arr.length - 1) return
  ;[arr[idx + 1], arr[idx]] = [arr[idx], arr[idx + 1]]
}
const onRemoveApprover = (idx) => {
  approvers.value.splice(idx, 1)
}

// ───────────────────────────────────────────────────────────
// 결재자 추가 시트 (010-22 → 010-05 candidates)
// ───────────────────────────────────────────────────────────
const onAddApprover = () => {
  pickerOpen.value = true
}
const onApproversAdded = (added) => {
  // 시트가 emit한 결재자({ userCd, userNm, rankNm, nodeNm })를 리스트 맨 아래에 순서대로 추가.
  //   이미 추가된 항목은 시트에서 disabled 되므로 중복 가능성은 낮으나 방어적으로 중복 제거.
  const existing = new Set(approvers.value.map((a) => a.userCd))
  const merged = (added || []).filter((a) => a && !existing.has(a.userCd))
  approvers.value.push(...merged)
  pickerOpen.value = false
}

// ───────────────────────────────────────────────────────────
// 푸터 / 삭제 / 라우팅
// ───────────────────────────────────────────────────────────
const onSave = async () => {
  if (!canSave.value || isSaving.value) return
  nameError.value = ''
  isSaving.value = true
  try {
    const body = {
      presetId: isEditMode.value ? presetId.value : null, // null=신규
      presetNm: presetNm.value.trim(),
      defaultYn: isDefault.value ? 'Y' : 'N',
      approverUserCds: selectedUserCds.value,
    }
    await api.post('/appApi/mypage/approval-presets', body)
    router.push('/ApprovalPresetList')
  } catch (e) {
    const errorCode = e?.response?.data?.errorCode
    const nameErrors = {
      PRESET_NAME_REQUIRED: '프리셋 이름을 입력해 주세요.',
      PRESET_NAME_DUPLICATED: '이미 같은 이름의 프리셋이 있어요.',
    }
    const alertErrors = {
      PRESET_APPROVER_REQUIRED: '결재자를 1명 이상 추가해 주세요.',
      PRESET_APPROVER_DUPLICATED: '중복된 결재자가 있어요.',
      PRESET_SELF_APPROVAL: '본인은 결재자로 지정할 수 없어요.',
      PRESET_APPROVER_INVALID: '선택할 수 없는 결재자가 포함되어 있어요.',
    }
    if (nameErrors[errorCode]) {
      nameError.value = nameErrors[errorCode]
    } else if (alertErrors[errorCode]) {
      showAlert(alertErrors[errorCode])
    } else {
      showAlert(e?.response?.data?.message || '저장에 실패했어요. 잠시 후 다시 시도해 주세요.')
    }
  } finally {
    isSaving.value = false
  }
}
const onDelete = async () => {
  if (!isEditMode.value || isDeleting.value) return
  const ok = await askConfirm(`'${presetNm.value || '이 프리셋'}'을(를) 삭제할까요?`, 'danger')
  if (!ok) return
  isDeleting.value = true
  try {
    // 기본 프리셋 삭제 시 다른 프리셋으로의 자동 승격은 서버가 처리.
    await api.post('/appApi/mypage/approval-presets/delete', { presetId: presetId.value })
    router.push('/ApprovalPresetList')
  } catch (e) {
    showAlert(e?.response?.data?.message || '삭제에 실패했어요. 잠시 후 다시 시도해 주세요.')
  } finally {
    isDeleting.value = false
  }
}
const onCancel = async () => {
  // 변경 사항이 있으면 이탈 확인.
  if (isDirty.value) {
    const ok = await askConfirm('저장하지 않고 나갈까요?')
    if (!ok) return
  }
  router.push('/ApprovalPresetList')
}
const onBack = () => {
  router.push('/ApprovalPresetList')
}

// ───────────────────────────────────────────────────────────
// 진입 시: 수정 모드면 상세 조회(010-05). 신규면 빈 폼 + 첫 프리셋 여부 판정.
// ───────────────────────────────────────────────────────────
onMounted(async () => {
  try {
    // 보유 프리셋 목록으로 첫 프리셋 여부 / 유일 기본 여부를 판정.
    let allPresets = []
    try {
      const listRes = await api.get('/appApi/mypage/approval-presets')
      allPresets = listRes?.data?.presets || []
    } catch (e) {
      console.warn('[PresetEdit] 프리셋 목록 조회 실패(판정 기본값 사용):', e?.message)
    }
    const defaultCount = allPresets.filter((p) => p.defaultYn === 'Y').length

    if (isEditMode.value) {
      // 수정: 상세 조회 후 폼 주입.
      const { data } = await api.get(
        `/appApi/mypage/approval-presets/${encodeURIComponent(presetId.value)}`,
      )
      presetNm.value = data?.presetNm || ''
      isDefault.value = data?.defaultYn === 'Y'
      approvers.value = (data?.steps || []).map((s) => ({
        userCd: s.approverUserCd,
        userNm: s.userNm,
        rankNm: s.rankNm,
        nodeNm: s.nodeNm,
      }))
      // 본 프리셋이 기본이고, 기본 프리셋이 이것 하나뿐이면 OFF 차단.
      isOnlyDefault.value = isDefault.value && defaultCount <= 1
    } else {
      // 신규: 보유 프리셋 0개면 첫 프리셋 → 기본 ON 강제.
      isFirstPreset.value = allPresets.length === 0
      if (isFirstPreset.value) isDefault.value = true
    }

    // 이탈 확인용 초기 스냅샷 확정.
    initialSnapshot.value = currentSnapshot.value
  } catch (e) {
    console.warn('[PresetEdit] 프리셋 상세 조회 실패:', e?.message)
    const errorCode = e?.response?.data?.errorCode
    if (errorCode === 'PRESET_NOT_FOUND') {
      await showAlert('이미 삭제된 프리셋이에요.')
    } else if (errorCode === 'PRESET_FORBIDDEN') {
      await showAlert('접근할 수 없는 프리셋이에요.')
    } else {
      await showAlert('프리셋을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
    }
    router.push('/ApprovalPresetList')
    return
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
.preset-edit-view {
  --color-primary: #16a34a;
  --color-primary-tint: #f0fdf4;
  --color-danger: #ef4444;
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
.ed-hd {
  height: 56px;
  flex-shrink: 0;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-lg);
  border-bottom: 1px solid var(--color-border-light);
}
.ed-hd__back,
.ed-hd__delete {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-primary);
  font-family: inherit;
}
.ed-hd__back {
  margin-left: -10px;
}
.ed-hd__delete {
  margin-right: -10px;
  color: var(--color-danger);
}
.ed-hd__title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
}
.ed-hd__spacer {
  width: 44px;
}

/* 본문 */
.ed-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-lg) var(--space-lg) 88px;
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}
.ed-loading {
  padding: 48px var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* 이름 필드 */
.ed-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.ed-field__label {
  font-size: 13px;
  color: var(--color-text-secondary);
}
.ed-input {
  height: 46px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  background: var(--color-surface);
  box-sizing: border-box;
  outline: none;
  font-family: inherit;
}
.ed-input:focus {
  border-color: var(--color-primary);
}
.ed-helper {
  margin: 0;
  font-size: 12px;
}
.ed-helper--danger {
  color: var(--color-danger);
}

/* 기본 토글 행 */
.ed-toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  padding: var(--space-md) var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
}
.ed-toggle-row__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.ed-toggle-row__desc {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.ed-switch {
  position: relative;
  flex-shrink: 0;
  width: 48px;
  height: 28px;
  border-radius: var(--radius-full);
  background: var(--color-border);
  border: 0;
  cursor: pointer;
  transition: background 0.15s;
  padding: 0;
}
.ed-switch--on {
  background: var(--color-primary);
}
.ed-switch:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.ed-switch__thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 22px;
  height: 22px;
  border-radius: var(--radius-full);
  background: var(--color-surface);
  transition: transform 0.15s;
}
.ed-switch--on .ed-switch__thumb {
  transform: translateX(20px);
}

/* 결재자 리스트 */
.ed-approvers {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.ed-approvers__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.ed-approvers__title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-primary);
}
.ed-approvers__hint {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.ed-approvers__empty {
  margin: 0;
  padding: 24px var(--space-lg);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
  background: var(--color-bg);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
}

/* 결재자 행 */
.ed-row {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
}
.ed-row__seq {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  background: var(--color-primary-tint);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 700;
}
.ed-row__info {
  flex: 1;
  min-width: 0;
}
.ed-row__name {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.ed-row__meta {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.ed-row__actions {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}
.ed-row__btn {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-tertiary);
  font-family: inherit;
}
.ed-row__btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.ed-row__btn--remove {
  color: var(--color-danger);
}

/* 결재자 추가 (dashed) */
.ed-add {
  width: 100%;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: transparent;
  color: var(--color-primary);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}

/* 푸터 */
.ed-footer {
  flex-shrink: 0;
  display: flex;
  gap: var(--space-sm);
  background: var(--color-surface);
  border-top: 1px solid var(--color-border-light);
  padding: var(--space-md) var(--space-lg);
}
.ed-btn {
  flex: 1;
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  border: 0;
}
.ed-btn--ghost {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}
.ed-btn--primary {
  background: var(--color-primary);
  color: var(--color-surface);
}
.ed-btn--off {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}

.ed-sprite {
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
