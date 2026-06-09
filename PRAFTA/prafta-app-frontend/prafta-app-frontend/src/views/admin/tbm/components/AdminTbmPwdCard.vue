<!--
  AdminTbmPwdCard.vue — 입실 / 종료 비밀번호 표시 카드 (+ 재발급)
  - 작업 ID: 001-P5-T-F5 (분해: 001-phase5-admin-tbm-plan.md §2-3, §3 T-A4)
  - 노출 조건: pwdVisible(서버 산출) 기준. 입실비번=OPENED 이상, 종료비번=COMPLETED 이상(prafta-051).
  - mode(prafta-051): 'BOTH'(현행)/'ENTRY'(입실만)/'EXIT'(종료만). 재발급 호출(E6/E7)은 부모가 수행.
  - 참조 패턴: web popup/TbmSessionDetail.vue 의 .pwd-section.
  - 디자인 토큰은 부모(.admin-tbm-view / 세션 상세 루트)에서 상속.
  - planner 라운드 스코프: template + style 완성. script 는 props/emits + 단순 표시만(API 호출 없음).
-->
<template>
  <section class="admin-tbm-pwd">
    <p class="admin-tbm-pwd__title">{{ cardTitle }}</p>

    <div class="admin-tbm-pwd__row">
      <div v-if="showEntry" class="admin-tbm-pwd__box">
        <span class="admin-tbm-pwd__label">입실</span>
        <span class="admin-tbm-pwd__value">{{ entryPwd || '------' }}</span>
      </div>
      <div v-if="showExit" class="admin-tbm-pwd__box">
        <span class="admin-tbm-pwd__label">종료</span>
        <span class="admin-tbm-pwd__value">{{ exitPwd || '------' }}</span>
      </div>
    </div>

    <!-- 재발급(OPENED 한정) — 실제 호출은 부모/developer -->
    <button
      v-if="canRegenerate"
      type="button"
      class="admin-tbm-pwd__regen"
      :disabled="regenerating"
      @click="$emit('regenerate')"
    >
      비밀번호 재발급
    </button>

    <p class="admin-tbm-pwd__hint">{{ cardHint }}</p>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 입실 비밀번호(랜덤 6자리)
  entryPwd: { type: String, default: '' },
  // 종료 비밀번호
  exitPwd: { type: String, default: '' },
  // 표시 모드(prafta-051): 'BOTH'=입실+종료(현행) / 'ENTRY'=입실만 / 'EXIT'=종료만
  mode: { type: String, default: 'BOTH' },
  // 재발급 가능 여부(부모가 statusCd 로 판정해 전달)
  canRegenerate: { type: Boolean, default: false },
  // 재발급 진행 중 버튼 잠금
  regenerating: { type: Boolean, default: false },
})

// regenerate → 부모가 (입실/종료) 비번 재발급 호출 후 entryPwd/exitPwd 갱신
defineEmits(['regenerate'])

// 모드별 표시 박스(ENTRY=입실만, EXIT=종료만, BOTH=둘 다)
const showEntry = computed(() => props.mode === 'BOTH' || props.mode === 'ENTRY')
const showExit = computed(() => props.mode === 'BOTH' || props.mode === 'EXIT')

const cardTitle = computed(() => {
  if (props.mode === 'ENTRY') return '입실 비밀번호'
  if (props.mode === 'EXIT') return '종료 비밀번호'
  return '입실 / 종료 비밀번호'
})
const cardHint = computed(() => {
  if (props.mode === 'ENTRY') return '근로자에게 입실 비밀번호를 안내해 주세요.'
  if (props.mode === 'EXIT') return '근로자에게 종료 비밀번호를 안내해 주세요.'
  return '근로자에게 입실/종료 비밀번호를 안내해 주세요.'
})
</script>

<style scoped>
.admin-tbm-pwd {
  background: var(--color-warning-tint);
  border-radius: var(--radius-lg);
  padding: var(--space-md) var(--space-lg);
}
.admin-tbm-pwd__title {
  margin: 0 0 var(--space-sm);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-warning-text);
}
.admin-tbm-pwd__row {
  display: flex;
  gap: var(--space-lg);
}
.admin-tbm-pwd__box {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.admin-tbm-pwd__label {
  font-size: 12px;
  color: var(--color-warning-text);
}
.admin-tbm-pwd__value {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 4px;
  color: var(--color-text-primary);
}
.admin-tbm-pwd__regen {
  margin-top: var(--space-md);
  height: 36px;
  padding: 0 var(--space-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-warning-text);
  border-radius: var(--radius-md);
  color: var(--color-warning-text);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.admin-tbm-pwd__regen:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.admin-tbm-pwd__hint {
  margin: var(--space-sm) 0 0;
  font-size: 12px;
  color: var(--color-warning-text);
}
</style>
