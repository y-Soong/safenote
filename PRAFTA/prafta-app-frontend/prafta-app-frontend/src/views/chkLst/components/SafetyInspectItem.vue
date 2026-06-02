<!--
  SafetyInspectItem.vue — 점검 항목 1개 카드 (prafta-app-011 화면 B)
  - 번호 칩 + 항목명 + 양호/불량 2분할 토글 + 불량 시 사유/사진 펼침.
  - 상태 3종 톤(미답/양호/불량). 색 단독 의존 금지: 칩색 + 토글 위치 + 카드 톤 + 텍스트.
  - 응답값은 부모가 소유. 본 컴포넌트는 변경을 이벤트로 통지.
-->
<template>
  <div class="it" :class="{ ok: answerType === 'Y', bad: answerType === 'N' }">
    <div class="it-head">
      <div class="it-no" aria-hidden="true">{{ index }}</div>
      <div class="it-subj">{{ item.inspectItemSubj }}</div>
    </div>

    <!-- 양호/불량 토글 -->
    <div class="toggle" role="radiogroup" :aria-label="`${index}번 항목: ${item.inspectItemSubj}`">
      <button
        type="button"
        class="tg-i ok"
        :class="{ on: answerType === 'Y' }"
        role="radio"
        :aria-checked="answerType === 'Y'"
        @click="setAnswer('Y')"
      >
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <polyline points="20 6 9 17 4 12" />
        </svg>
        양호
      </button>
      <button
        type="button"
        class="tg-i bad"
        :class="{ on: answerType === 'N' }"
        role="radio"
        :aria-checked="answerType === 'N'"
        @click="setAnswer('N')"
      >
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path
            d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"
          />
          <line x1="12" y1="9" x2="12" y2="13" />
          <line x1="12" y1="17" x2="12.01" y2="17" />
        </svg>
        불량
      </button>
    </div>

    <!-- 불량 펼침 -->
    <SafetyInspectBadForm
      v-if="answerType === 'N'"
      :item-cd="item.inspectItemCd"
      :reason="item.answerDesc || ''"
      :photo="item.photo || null"
      @update:reason="(v) => emit('update:reason', v)"
      @update:photo="(v) => emit('update:photo', v)"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import SafetyInspectBadForm from './SafetyInspectBadForm.vue'

const props = defineProps({
  // 항목 모델 { inspectItemCd, inspectItemSubj, answerType('Y'|'N'|null), answerDesc, photo }
  item: { type: Object, required: true },
  // 1-base 번호
  index: { type: Number, required: true },
})

const emit = defineEmits(['update:answer', 'update:reason', 'update:photo'])

const answerType = computed(() => props.item.answerType ?? null)

const setAnswer = (type) => {
  // 동일 값 재클릭은 무시 (토글 해제 미허용 — 전 항목 응답 필수)
  if (answerType.value === type) return
  emit('update:answer', type)
}
</script>

<style scoped>
.it {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 12px 14px;
}
.it.ok {
  border-color: var(--color-primary-tint-border);
  background: #f7fdf9;
}
.it.bad {
  border-color: #fecaca;
  background: #fef7f7;
}
.it-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.it-no {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--color-border-light);
  color: var(--color-text-secondary);
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 1px;
}
.it.ok .it-no {
  background: var(--color-primary);
  color: #ffffff;
}
.it.bad .it-no {
  background: var(--color-danger);
  color: #ffffff;
}
.it-subj {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  line-height: 1.4;
}
.it.ok .it-subj {
  color: #14532d;
}
.it.bad .it-subj {
  color: #991b1b;
}

/* 토글 */
.toggle {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 10px;
}
.tg-i {
  height: 40px;
  min-height: 40px;
  border-radius: 10px;
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  font-family: inherit;
}
.tg-i.on.ok {
  background: var(--color-primary-tint);
  border-color: var(--color-primary);
  color: var(--color-primary-deep);
}
.tg-i.on.bad {
  background: var(--color-danger-tint);
  border-color: var(--color-danger);
  color: #b91c1c;
}
</style>
