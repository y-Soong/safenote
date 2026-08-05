<!--
  AdminTbmMaterialItemRow.vue — 교육자료 항목(item) 입력 행 (등록/수정 폼 내부)
  - 작업 ID: 001-P5-T-F13-R (분해: 001-phase5-admin-tbm-plan.md §2-6, §3-J T-A10 등록)
  - 부모: AdminTbmMaterialFormView — 항목 다중 추가/삭제/순서.
  - 항목 1건: { mtrlItemType(SYS018), url, fileName, mtrlDesc, sortIdx }
      type = MTRL_ITEM_TYPE(SYS018): '01' 이미지 / '02' 동영상 / '03' 유튜브URL(외부링크) / '04' PDF
      - 파일형(01/02/04): 파일 선택 input(선택 파일명 표시). 실제 업로드는 부모/developer.
      - URL형(03): URL 직접 입력.
  - v-model 패턴: modelValue(항목 객체) ↔ update:modelValue. 파일 선택 시 file 이벤트로 부모에 원본 전달.
  - 디자인 토큰은 부모(.admin-tbm-material-form-view 루트)에서 상속(자급 안 함).
  - planner 라운드 스코프: template + style 완성. script 는 props/emits + v-model + UI 토글만.
      ⚠️ 실제 업로드 호출은 developer(T8) — file 이벤트로 부모에 위임.
-->
<template>
  <div class="mtrl-item-row">
    <div class="mtrl-item-row__top">
      <span class="mtrl-item-row__idx">#{{ index + 1 }}</span>
      <div class="mtrl-item-row__top-actions">
        <button
          type="button"
          class="mtrl-item-row__move"
          aria-label="위로"
          :disabled="index === 0"
          @click="$emit('move-up', index)"
        >
          ↑
        </button>
        <button
          type="button"
          class="mtrl-item-row__move"
          aria-label="아래로"
          :disabled="isLast"
          @click="$emit('move-down', index)"
        >
          ↓
        </button>
        <button
          type="button"
          class="mtrl-item-row__remove"
          aria-label="항목 삭제"
          @click="$emit('remove', index)"
        >
          ×
        </button>
      </div>
    </div>

    <!-- 항목 타입(SYS018) -->
    <div class="mtrl-item-row__field">
      <label class="mtrl-item-row__label" :for="`mtrl-item-type-${index}`">항목 타입</label>
      <select
        :id="`mtrl-item-type-${index}`"
        class="mtrl-item-row__select"
        :value="model.mtrlItemType"
        @change="onChange('mtrlItemType', $event.target.value)"
      >
        <option v-for="t in itemTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
      </select>
    </div>

    <!-- 파일형(01/02/04): 파일 선택 -->
    <div v-if="isFileType" class="mtrl-item-row__field">
      <span class="mtrl-item-row__label">파일</span>
      <div class="mtrl-item-row__file">
        <label class="mtrl-item-row__file-btn">
          파일 선택
          <input
            class="mtrl-item-row__file-input"
            type="file"
            :accept="fileAccept"
            @change="onPickFile"
          />
        </label>
        <span class="mtrl-item-row__file-name">{{ model.fileName || '선택된 파일 없음' }}</span>
      </div>
    </div>

    <!-- URL형(03): URL 입력 -->
    <div v-else class="mtrl-item-row__field">
      <label class="mtrl-item-row__label" :for="`mtrl-item-url-${index}`">URL</label>
      <input
        :id="`mtrl-item-url-${index}`"
        class="mtrl-item-row__input"
        type="url"
        maxlength="1000"
        placeholder="https://"
        :value="model.url"
        @input="onChange('url', $event.target.value)"
      />
    </div>

    <!-- 설명(MTRL_DESC) -->
    <div class="mtrl-item-row__field">
      <label class="mtrl-item-row__label" :for="`mtrl-item-desc-${index}`">설명 (선택)</label>
      <input
        :id="`mtrl-item-desc-${index}`"
        class="mtrl-item-row__input"
        type="text"
        maxlength="500"
        placeholder="항목 설명"
        :value="model.mtrlDesc"
        @input="onChange('mtrlDesc', $event.target.value)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 항목 객체: { mtrlItemType, url, fileName, mtrlDesc, sortIdx }
  modelValue: { type: Object, default: () => ({}) },
  index: { type: Number, default: 0 },
  isLast: { type: Boolean, default: false },
})

// update:modelValue: 항목 변경 / file: 파일 원본(File) 전달(부모/업로드는 developer)
// remove / move-up / move-down: 순서 제어
const emit = defineEmits(['update:modelValue', 'file', 'remove', 'move-up', 'move-down'])

// SYS018 항목 타입(마이그레이션 prafta-033-A-codes.sql 정합)
const itemTypes = [
  { value: '01', label: '이미지' },
  { value: '02', label: '동영상' },
  { value: '03', label: '유튜브/외부 URL' },
  { value: '04', label: 'PDF' },
]

const model = computed(() => props.modelValue || {})

// 파일형(이미지/동영상/PDF) vs URL형(03)
const isFileType = computed(() => ['01', '02', '04'].includes(model.value.mtrlItemType))
const fileAccept = computed(() => {
  switch (model.value.mtrlItemType) {
    case '01':
      return 'image/*'
    case '02':
      return 'video/*'
    case '04':
      return 'application/pdf'
    default:
      return ''
  }
})

// 필드 변경 → 부모로 병합 emit(불변 업데이트)
const onChange = (key, value) => {
  emit('update:modelValue', { ...model.value, [key]: value })
}

// 파일 선택: 파일명만 로컬 반영 + 원본 File 은 file 이벤트로 부모에 전달(업로드는 developer).
const onPickFile = (e) => {
  const file = e?.target?.files?.[0]
  if (!file) return
  emit('update:modelValue', { ...model.value, fileName: file.name })
  emit('file', { index: props.index, file })
}
</script>

<style scoped>
.mtrl-item-row {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  padding: var(--space-md);
  background: var(--color-bg);
  border: 0.5px solid var(--color-border);
  border-radius: var(--radius-md);
}
.mtrl-item-row__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.mtrl-item-row__idx {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-secondary);
}
.mtrl-item-row__top-actions {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}
.mtrl-item-row__move,
.mtrl-item-row__remove {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
}
.mtrl-item-row__move:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.mtrl-item-row__remove {
  color: var(--color-danger-text);
  border-color: var(--color-danger);
  font-size: 18px;
  line-height: 1;
}

.mtrl-item-row__field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.mtrl-item-row__label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
}
.mtrl-item-row__input,
.mtrl-item-row__select {
  width: 100%;
  box-sizing: border-box;
  height: 40px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
}
.mtrl-item-row__input:focus,
.mtrl-item-row__select:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* 파일 선택 */
.mtrl-item-row__file {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.mtrl-item-row__file-btn {
  flex-shrink: 0;
  height: 36px;
  padding: 0 var(--space-md);
  display: inline-flex;
  align-items: center;
  background: var(--color-surface);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
/* 버튼처럼 쓰는 label(파일 선택) — 전역 base 에서 탭 하이라이트를 끈 대신
   누르는 대상(버튼 박스) 자체에만 눌림 피드백을 남긴다. */
.mtrl-item-row__file-btn:active {
  background: var(--color-primary-tint);
}
.mtrl-item-row__file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  overflow: hidden;
}
.mtrl-item-row__file-name {
  min-width: 0;
  font-size: 13px;
  color: var(--color-text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
