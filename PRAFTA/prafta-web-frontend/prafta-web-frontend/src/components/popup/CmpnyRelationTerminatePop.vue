<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-narrow">
        <div class="modal-header">
          <span>연동 해지</span>
          <button class="icon-button" @click="$emit('close')">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div class="form-container">
          <p class="reg-guide">
            '{{ props.otherCmpnyNm }}' 회사와의 연동을 해지합니다. 해지 시 아래 산하 연동 건이 함께 정리됩니다.
          </p>
          <!-- 산하 연동 영향 요약 (GET relation-terminate-summary) -->
          <ul v-if="impacts.length" class="impact-list">
            <li v-for="(it, idx) in impacts" :key="idx">{{ it.label }} {{ it.count }}건</li>
          </ul>
          <p v-else class="impact-empty">해지 시 함께 정리될 연동 건이 없습니다.</p>
        </div>
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">취소</button>
            <button class="btn btn-primary" @click="fnTerminate">해지</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, onMounted, defineProps, defineEmits, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

const props = defineProps({ relationId: [Number, String], otherCmpnyNm: String, onSaved: Function });
const emit = defineEmits(["close"]);
const { proxy } = getCurrentInstance();

const impacts = ref([]); // [{ impactType, label, count }]

// 해지 중복 클릭 방지 플래그.
const saving = ref(false);

// 진입 시 산하 연동 영향 요약 조회 — GET /webApi/subcon01/relation-terminate-summary?relationId=
//   T1 시점 응답 = { impacts: [] } → 빈 배열이면 "함께 정리될 연동 건 없음" 문구 표시.
onMounted(async () => {
  try {
    const response = await axios.get("/webApi/subcon01/relation-terminate-summary", {
      params: { relationId: props.relationId },
    });

    if (response.status === 200) {
      impacts.value = response.data?.impacts || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "해지 영향 조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
});

// 해지 — POST /webApi/subcon01/relation-terminate { relationId }.
//   성공: 알림 → 부모 목록 재조회(onSaved) → 닫기.
const fnTerminate = async () => {
  const ok = await proxy.$confirm("정말 해지하시겠습니까?");
  if (!ok) return;

  if (saving.value) return;
  saving.value = true;

  try {
    const response = await axios.post("/webApi/subcon01/relation-terminate", {
      relationId: props.relationId,
    });

    if (response.status === 200) {
      await proxy.$alert("처리되었습니다.");
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
.impact-list {
  margin: var(--space-sm, 0.5rem) 0 0;
  padding-left: 1.25rem;
  color: var(--color-warning-text, #b45309);
}
.impact-empty {
  margin-top: var(--space-sm, 0.5rem);
  color: var(--color-text-muted, #6b7280);
}
</style>
