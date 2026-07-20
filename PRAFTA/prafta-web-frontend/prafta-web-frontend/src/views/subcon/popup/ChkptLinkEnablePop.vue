<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <div class="modal-header">
          <span>순회점검 구성 연동</span>
          <button class="icon-button" @click="$emit('close')">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="form-container">
          <p class="chkpt-link-title">'{{ props.siteNm }}'의 순회점검 구성을 연동합니다.</p>
          <ul class="chkpt-link-guide">
            <li>현재 <b>사용중인 점검대상과 점검문항</b>이 상대 회사의 연동 사업장에 복제됩니다.</li>
            <li>복제된 점검대상·문항은 상대 회사가 <b>수정할 수 없으며</b>(점검 담당자 지정만 가능), 이후 내 변경사항이 자동 반영됩니다.</li>
            <li>점검 결과는 <b>연동 회사 전체에서 하나로 통합</b>됩니다. 어느 회사가 점검하든 양쪽 실적에 반영되며, 같은 날 같은 문항은 <b>먼저 점검한 쪽이 완료</b>됩니다.</li>
            <li>해제하면 상대 회사의 자체 점검 구성으로 전환되고(기존 실적은 보존), 결과 통합이 중단됩니다.</li>
          </ul>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">취소</button>
            <button class="btn btn-primary" :disabled="processing" @click="fnEnable">연동</button>
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

// 연동 실행 중복 클릭 방지 플래그(미러 복제 트랜잭션이라 연타 시 불필요한 재요청 발생).
const processing = ref(false);

// 순회점검 구성 연동 실행 — POST /webApi/subcon02/chkpt-link-enable { linkId }.
//   성공 시 복제 완료 안내 → 부모 목록 재조회(onSaved) → 닫기.
//   조건 불충족(비당사자/이미 연동/사업장 연동 아님)은 서버가 404 로 통합 응답한다(존재 비노출).
const fnEnable = async () => {
  if (processing.value) return;
  processing.value = true;

  try {
    const response = await axios.post("/webApi/subcon02/chkpt-link-enable", {
      linkId: props.linkId,
    });

    if (response.status === 200) {
      await proxy.$alert("순회점검 구성이 연동되었습니다.");
      props.onSaved?.();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "연동 처리 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    processing.value = false;
  }
};
</script>

<style scoped>
.chkpt-link-title {
  margin-bottom: var(--space-md, 0.75rem);
  font-weight: 600;
  color: var(--color-text, #374151);
}
.chkpt-link-guide {
  margin: 0;
  padding-left: 1.1rem;
  color: var(--color-text-muted, #6b7280);
  font-size: var(--btn-font-sm, 12px);
  line-height: 1.7;
}
</style>
