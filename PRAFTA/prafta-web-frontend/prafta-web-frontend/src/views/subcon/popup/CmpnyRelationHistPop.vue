<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div class="modal-content-normal">
        <div class="modal-header">
          <span>연동 이력 — {{ props.otherCmpnyNm }}</span>
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
          <table class="data-grid w-full table-fixed text-sm text-left">
            <thead>
              <tr>
                <th style="width: 160px">일시</th>
                <th style="width: 90px">액션</th>
                <th style="width: 140px">행위자</th>
                <th>설명</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!hists.length">
                <tr>
                  <td colspan="4" class="edu-grid-empty">이력이 없습니다.</td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="row in hists" :key="row.histId">
                  <td>{{ row.actionDtime }}</td>
                  <td>{{ actionLabel(row.actionType) }}</td>
                  <td>{{ row.actionUserNm }}</td>
                  <!-- 자사만 실명, 상대사는 "상대사 처리"(서버 마스킹) -->
                  <td class="desc-cell">{{ row.actionDesc }}</td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="$emit('close')">
              닫기
            </button>
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

const props = defineProps({ relationId: [Number, String], otherCmpnyNm: String });
const emit = defineEmits(["close"]);
const { proxy } = getCurrentInstance();

const hists = ref([]);

const actionLabel = (t) =>
  ({ REQUEST: "요청", ACCEPT: "수락", REJECT: "거부", CANCEL: "취소", TERMINATE: "해지" }[t] || t);

// 진입 시 이력 조회 — GET /webApi/subcon01/relation-hists?relationId=
//   행위자명은 서버가 자사만 실명, 상대사는 "상대사 처리"로 마스킹하여 내려준다(Q4).
onMounted(async () => {
  try {
    const response = await axios.get("/webApi/subcon01/relation-hists", {
      params: { relationId: props.relationId },
    });

    if (response.status === 200) {
      hists.value = response.data?.hists || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "이력 조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
});
</script>

<style scoped>
.desc-cell {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
