<!--
  MobileVerificationField.vue — 휴대폰 입력 + 인증요청/확인 (마이페이지 연락처)
  - 작업 ID: PRAFTA-APP-010-20
  - 호출자: ProfileEditView (010-11)
  - 참조 패턴: PhoneAuthView.vue (인증요청 카운트다운 / 인증번호 / 포맷)
  - planner 라운드 스코프: 입력 UI + 카운트다운 + 성공 표시 (template/style)
  - developer 라운드 스코프(아래 TODO): 발송/검증 API(010-03)는 부모(ProfileEdit)가 처리,
    본 컴포넌트는 request-code / verify-code 이벤트만 emit.
-->
<template>
  <div class="mvf">
    <!-- 휴대폰 입력 + 인증요청 -->
    <div class="mvf-field">
      <label class="mvf-field__label" for="mvfMblNo">휴대폰</label>
      <div class="mvf-row">
        <input
          id="mvfMblNo"
          :value="modelValue"
          type="tel"
          class="mvf-input"
          maxlength="13"
          placeholder="010-0000-0000"
          :disabled="verified"
          @input="onMblInput"
        />
        <button
          type="button"
          class="mvf-btn"
          :disabled="resendTimer > 0 || verified"
          @click="onRequestCode"
        >
          {{ resendTimer > 0 ? `${resendTimer}초 후 재요청` : '인증요청' }}
        </button>
      </div>
    </div>

    <!-- 인증번호 입력 + 확인 -->
    <div class="mvf-field">
      <label class="mvf-field__label" for="mvfCode">인증번호</label>
      <div class="mvf-row">
        <input
          id="mvfCode"
          v-model="code"
          type="tel"
          class="mvf-input"
          maxlength="6"
          placeholder="6자리"
          :disabled="!codeSent || verified"
        />
        <button
          type="button"
          class="mvf-btn"
          :disabled="!codeSent || verified || code.length !== 6"
          @click="onVerifyCode"
        >
          확인
        </button>
        <span v-if="verified" class="mvf-ok" aria-label="인증 완료">인증 완료</span>
      </div>
      <p v-if="verifyError" class="mvf-helper mvf-helper--danger">{{ verifyError }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'

const props = defineProps({
  // 휴대폰 번호 (v-model)
  modelValue: {
    type: String,
    default: '',
  },
  // 인증 완료 여부 (부모가 verify 성공 시 true 주입)
  verified: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue', 'request-code', 'verify-code'])

// ───────────────────────────────────────────────────────────
// 로컬 상태 (UI — 허용)
// ───────────────────────────────────────────────────────────
const code = ref('')
const codeSent = ref(false)
const verifyError = ref('')

// 재요청 60초 카운트다운 (PhoneAuthView 패턴)
const resendTimer = ref(0)
let resendInterval = null

// ───────────────────────────────────────────────────────────
// 휴대폰 입력 바인딩 (v-model)
// ───────────────────────────────────────────────────────────
const onMblInput = (e) => {
  emit('update:modelValue', e.target.value)
}

// ───────────────────────────────────────────────────────────
// 인증요청 — 부모에게 발송 위임(010-03). 부모 성공 시 startCountdown 호출 노출.
// ───────────────────────────────────────────────────────────
const onRequestCode = () => {
  // 숫자만 추출하여 부모에 전달 (포맷 정규화는 developer가 $util.formatPhoneNumber로 보완 가능)
  const phone = (props.modelValue || '').replace(/\D+/g, '')
  emit('request-code', phone)
}

// 부모가 발송 성공 시 호출하도록 노출 (카운트다운 시작 + 인증번호 활성화)
const startCountdown = () => {
  codeSent.value = true
  verifyError.value = ''
  if (resendInterval) clearInterval(resendInterval)
  resendTimer.value = 60
  resendInterval = setInterval(() => {
    resendTimer.value -= 1
    if (resendTimer.value <= 0) {
      clearInterval(resendInterval)
      resendInterval = null
    }
  }, 1000)
}

// ───────────────────────────────────────────────────────────
// 인증번호 확인 — 부모에게 검증 위임(010-03)
// ───────────────────────────────────────────────────────────
const onVerifyCode = () => {
  const phone = (props.modelValue || '').replace(/\D+/g, '')
  emit('verify-code', { phone, code: code.value })
}

// 부모가 검증 실패 시 호출하도록 노출
const setVerifyError = (message) => {
  verifyError.value = message
}

// 휴대폰 번호가 바뀌면 인증 상태 초기화 (재인증 필요)
watch(
  () => props.modelValue,
  () => {
    codeSent.value = false
    code.value = ''
    verifyError.value = ''
  }
)

onBeforeUnmount(() => {
  if (resendInterval) {
    clearInterval(resendInterval)
    resendInterval = null
  }
})

// 부모(ProfileEdit)가 호출할 수 있도록 노출
defineExpose({ startCountdown, setVerifyError })
</script>

<style scoped>
.mvf {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}
.mvf-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.mvf-field__label {
  font-size: 13px;
  color: var(--color-text-secondary);
}
.mvf-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.mvf-input {
  flex: 1;
  /* min-width:0 — flex item 기본 min-width:auto 때문에 input 이 고유 너비 아래로
     줄어들지 않아 버튼과 합산 시 행이 컨테이너를 넘쳐 가로 스크롤이 생긴다.
     0 으로 풀어 정상 축소시켜 윗 입력들과 우측 끝단을 맞춘다. */
  min-width: 0;
  height: 46px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  color: var(--color-text-primary);
  background: var(--color-surface);
  box-sizing: border-box;
  outline: none;
  font-family: inherit;
}
.mvf-input:focus {
  border-color: var(--color-primary);
}
.mvf-input:disabled {
  background: var(--color-bg);
  color: var(--color-text-secondary);
}
.mvf-btn {
  height: 46px;
  flex-shrink: 0;
  padding: 0 var(--space-md);
  background: var(--color-primary);
  color: var(--color-surface);
  border: 0;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  font-family: inherit;
}
.mvf-btn:disabled {
  background: var(--color-border);
  color: var(--color-text-tertiary);
  cursor: not-allowed;
}
.mvf-ok {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
}
.mvf-helper {
  margin: 0;
  font-size: 12px;
}
.mvf-helper--danger {
  color: var(--color-danger);
}
</style>
