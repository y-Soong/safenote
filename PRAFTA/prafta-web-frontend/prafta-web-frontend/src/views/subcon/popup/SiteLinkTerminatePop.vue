<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <div class="modal-header">
          <span>사업장 연동 해지</span>
          <button class="icon-button" @click="$emit('close')">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
              class="w-6 h-6"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>
        <div class="form-container">
          <p class="terminate-warn">'{{ props.siteNm }}' 연동을 해지합니다.</p>
          <ul class="terminate-guide">
            <li>
              해지 즉시 수신 회사의 <b>일반(독립) 사업장</b>으로 전환됩니다.
              데이터 이동이나 근로자 회수는 없습니다.
            </li>
            <li>
              수신 회사가 다른 회사에 재연동한 하위 연동은 <b>계속 유지</b>되며,
              이후에는 수신 회사의 수정이 하위로 반영됩니다.
            </li>
            <li>
              같은 상대와 다시 연동하면 <b>새 미러 사업장이 생성</b>됩니다. 기존
              독립 사업장의 정리는 수신 회사 몫입니다.
            </li>
          </ul>
        </div>
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">
              취소
            </button>
            <button class="btn btn-primary" @click="fnTerminate">해지</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, defineProps, defineEmits, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

const props = defineProps({ linkId: [Number, String], siteNm: String, onSaved: Function });
const emit = defineEmits(["close"]);
const { proxy } = getCurrentInstance();

// 해지 중복 클릭 방지 플래그.
const saving = ref(false);

// 해지 — POST /webApi/subcon02/site-link-terminate { linkId }.
//   성공 시 독립화(일반 사업장 전환) 안내 → 부모 목록 재조회(onSaved) → 닫기.
const fnTerminate = async () => {
  if (saving.value) return;
  saving.value = true;

  try {
    const response = await axios.post("/webApi/subcon02/site-link-terminate", {
      linkId: props.linkId,
    });

    if (response.status === 200) {
      await proxy.$alert("해지되었습니다. 해당 사업장은 수신 회사의 독립 사업장으로 전환되었습니다.");
      props.onSaved?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "해지 처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.terminate-warn {
  margin-bottom: var(--space-md, 0.75rem);
  font-weight: 600;
  color: var(--color-danger, #dc2626);
}
.terminate-guide {
  margin: 0;
  padding-left: 1.1rem;
  color: var(--color-text-muted, #6b7280);
  font-size: var(--btn-font-sm, 12px);
  line-height: 1.7;
}
</style>
