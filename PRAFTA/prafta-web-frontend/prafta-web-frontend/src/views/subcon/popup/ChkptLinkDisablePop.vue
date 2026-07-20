<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <div class="modal-header">
          <span>순회점검 구성 연동 해제</span>
          <button class="icon-button" @click="$emit('close')">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="form-container">
          <p class="chkpt-unlink-warn">'{{ props.siteNm }}'의 순회점검 구성 연동을 해제합니다.</p>
          <ul class="chkpt-unlink-guide">
            <li>연동된 점검대상·문항은 해제 즉시 <b>수신 회사의 자체 점검 구성</b>으로 전환됩니다(잠금 해제).</li>
            <li><b>점검 결과 통합이 중단</b>됩니다. 이후 각 회사의 점검은 자기 회사에만 기록됩니다.</li>
            <li>이미 쌓인 점검 실적·불량조치·사진은 <b>양쪽 모두 그대로 보존</b>됩니다.</li>
            <li>다시 연동하면 <b>새 점검대상·문항이 생성</b>됩니다(기존 항목에 다시 붙지 않습니다).</li>
            <li>사업장 연동은 유지됩니다(사업장 연동까지 끊으려면 '해지'를 사용하세요).</li>
          </ul>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">취소</button>
            <button class="btn btn-primary" :disabled="processing" @click="fnDisable">해제</button>
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

// 해제 중복 클릭 방지 플래그.
const processing = ref(false);

// 순회점검 구성 연동 해제 — POST /webApi/subcon02/chkpt-link-disable { linkId }.
//   해제는 양측(제공/수신) 어느 쪽에서든 가능하다. 성공 시 독립화 안내 → 부모 목록 재조회 → 닫기.
const fnDisable = async () => {
  if (processing.value) return;
  processing.value = true;

  try {
    const response = await axios.post("/webApi/subcon02/chkpt-link-disable", {
      linkId: props.linkId,
    });

    if (response.status === 200) {
      await proxy.$alert("순회점검 구성 연동이 해제되었습니다.");
      props.onSaved?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "해제 처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    processing.value = false;
  }
};
</script>

<style scoped>
.chkpt-unlink-warn {
  margin-bottom: var(--space-md, 0.75rem);
  font-weight: 600;
  color: var(--color-danger, #dc2626);
}
.chkpt-unlink-guide {
  margin: 0;
  padding-left: 1.1rem;
  color: var(--color-text-muted, #6b7280);
  font-size: var(--btn-font-sm, 12px);
  line-height: 1.7;
}
</style>
