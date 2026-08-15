<!--
  ApprovalLineSection.vue — 근태 요청 결재선 섹션 (prafta-app-009, UI-009-1)
  - 참조 패턴: views/leave/components/LeaveApplyForm.vue 결재선 섹션(171~267)
  - 역할: 프리셋 칩 + 결재자 순서 리스트 + 추가/삭제(프레젠테이션). 검색은 부모가 연 시트가 담당.
  - v-model: approverList ([{ approverUserCd, userNm, userId, rankNm, nodeNm }]) — 순서 = 결재 단계.
  - ⚠️ approverUserCd 는 식별자. 위치 index 로 재인덱싱하지 않는다(서버가 STEP_NO=배열 순서로 INSERT).
-->
<template>
  <section class="aprv-sec">
    <p class="aprv-sec__title">결재선</p>

    <!-- 프리셋 칩 (2026-08-15: 기본 프리셋 자동 전개로 변경 — 종전 Q2 "자동선택 안 함" 폐기) -->
    <div v-if="presets.length > 0" class="preset-list">
      <button
        v-for="p in presets"
        :key="p.presetId"
        type="button"
        class="preset-chip"
        :class="{ 'preset-chip--on': selectedPresetId === p.presetId }"
        @click="onSelectPreset(p)"
      >
        {{ p.presetNm }}
        <span v-if="p.defaultYn" class="preset-chip__tag">기본</span>
      </button>
    </div>

    <!-- 결재자 순서 리스트 -->
    <ul v-if="modelValue.length > 0" class="aprv-list">
      <li v-for="(ap, idx) in modelValue" :key="ap.approverUserCd" class="aprv-row">
        <span class="aprv-row__step">{{ idx + 1 }}</span>
        <div class="aprv-row__info">
          <p class="aprv-row__name">{{ ap.userNm }}</p>
          <p class="aprv-row__meta">{{ metaOf(ap) }}</p>
        </div>
        <button
          type="button"
          class="aprv-row__del"
          aria-label="결재자 제거"
          @click="onRemove(ap.approverUserCd)"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </li>
    </ul>
    <p v-else class="aprv-empty">결재자를 추가해 주세요</p>

    <button type="button" class="btn-add" @click="$emit('open-picker')">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
        stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <line x1="12" y1="5" x2="12" y2="19" />
        <line x1="5" y1="12" x2="19" y2="12" />
      </svg>
      결재자 추가
    </button>
  </section>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  // 기존 GET /appApi/mypage01/approval-presets(또는 leaveflow/approval-presets)의 presets 배열
  // [{ presetId, presetNm, defaultYn, steps:[{ stepNo, approverUserCd, userNm, userId, rankNm, nodeNm }] }]
  presets: { type: Array, default: () => [] },
  // 결재자 순서 리스트(v-model)
  modelValue: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'open-picker'])

// 선택된 프리셋 표기용(전개 후 직접 편집 시 이탈 표시)
const selectedPresetId = ref('')

// 표시 헬퍼(UI — 허용)
const metaOf = (ap) => [ap?.nodeNm, ap?.rankNm].filter(Boolean).join(' · ')

// 프리셋 선택 → steps 를 결재자 리스트로 전개(STEP_NO=배열 순서 보존). 재선택 토글 해제.
const onSelectPreset = (preset) => {
  if (!preset) return
  if (selectedPresetId.value === preset.presetId) {
    selectedPresetId.value = ''
    emit('update:modelValue', [])
    return
  }
  selectedPresetId.value = preset.presetId
  const list = (preset.steps || []).map((s) => ({
    approverUserCd: s.approverUserCd,
    userNm: s.userNm,
    userId: s.userId,
    rankNm: s.rankNm,
    nodeNm: s.nodeNm,
  }))
  emit('update:modelValue', list)
}

// 기본 프리셋 자동 전개 (2026-08-15 사용자 지시 — 화면 간 동작 통일).
//   기간 연차 신청(LeaveApplyMultiView)이 이미 자동 전개라 나머지 결재선 화면을 그쪽에 맞춘다.
//   ⚠️ 결재선이 비어 있을 때만 적용한다 — 사용자가 직접 편집했거나 부모가 프리필한 값을 덮어쓰지 않는다.
//   ⚠️ 칩 재선택(토글 해제)으로 비워진 뒤에는 재적용하지 않는다(watch 의존값이 안 바뀌므로 자연히 성립).
//   ★ defaultYn 은 엔드포인트마다 형이 다르다 — leaveflow=boolean / mypage=('Y'|'N') 문자열. 둘 다 수용.
const isDefaultPreset = (p) => p?.defaultYn === true || p?.defaultYn === 'Y'

const applyDefaultPreset = () => {
  if (selectedPresetId.value) return
  if ((props.modelValue || []).length > 0) return
  const def = (props.presets || []).find(isDefaultPreset)
  if (def) onSelectPreset(def)
}

// 프리셋은 부모가 비동기로 채우므로 도착 시점에 전개한다(immediate: 이미 있으면 마운트 즉시).
watch(() => props.presets, applyDefaultPreset, { immediate: true })

// 결재자 제거 — userCd 식별자 필터(위치 index 재인덱싱 금지). 프리셋 이탈 표기.
const onRemove = (approverUserCd) => {
  emit('update:modelValue', props.modelValue.filter((a) => a.approverUserCd !== approverUserCd))
  selectedPresetId.value = ''
}

// 부모가 시트 add(picked[]) 수신 후 modelValue 에 직접 추가(append)하면 프리셋 이탈 상태가 되어야 한다.
// 이 컴포넌트는 프리셋 선택 상태(selectedPresetId)만 보유하므로, 부모는 직접 추가 직후 resetPreset() 을
// 호출해 프리셋 배지 선택을 해제한다. 값 가공/중복 dedup 은 부모 폼이 담당(approverUserCds 가 SSOT).
defineExpose({
  resetPreset: () => { selectedPresetId.value = '' },
})
</script>

<style scoped>
.aprv-sec {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}
.aprv-sec__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

/* 프리셋 칩 */
.preset-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}
.preset-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 36px;
  padding: 0 var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.preset-chip--on {
  border-color: var(--color-primary);
  background: var(--color-primary-tint);
  color: var(--color-primary-text-deep);
  font-weight: 500;
}
.preset-chip__tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-surface);
}

/* 결재자 리스트 */
.aprv-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.aprv-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
}
.aprv-row__step {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  background: var(--color-primary-tint);
  color: var(--color-primary-text-deep);
  font-size: 12px;
  font-weight: 600;
}
.aprv-row__info {
  flex: 1;
  min-width: 0;
}
.aprv-row__name {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}
.aprv-row__meta {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}
.aprv-row__del {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 0;
  color: var(--color-text-tertiary);
  cursor: pointer;
}
.aprv-empty {
  margin: 0;
  padding: var(--space-md);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
  background: var(--color-surface);
  border: 0.5px dashed var(--color-border);
  border-radius: var(--radius-md);
}

.btn-add {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 44px;
  background: var(--color-surface);
  border: 0.5px dashed var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  font-family: inherit;
}
.btn-add:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
</style>
