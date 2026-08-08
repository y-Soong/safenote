<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-narrow"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>연동 회사 지정</span>
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
          <p class="share-guide">
            연동(수락 완료) 관계가 수립된 회사만 지정할 수 있습니다. 지정한 회사
            직원은 이 교육에 입실·이수할 수 있으며, 지정받은 회사는 자기 연동
            회사에 다시 지정할 수 있습니다.
          </p>

          <div class="table-wrapper">
            <table class="data-grid sub-grid">
              <thead>
                <tr>
                  <th style="width: 12%; text-align: center">선택</th>
                  <th>회사명</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="isLoading">
                  <td colspan="2" class="edu-grid-empty">조회 중...</td>
                </tr>
                <tr v-else-if="!candidates.length">
                  <td colspan="2" class="edu-grid-empty">
                    지정 가능한 연동 회사가 없습니다.
                  </td>
                </tr>
                <tr v-else v-for="row in candidates" :key="row.cmpnyCd">
                  <td style="text-align: center">
                    <input
                      type="radio"
                      :value="row.cmpnyCd"
                      v-model="selectedCmpnyCd"
                    />
                  </td>
                  <td>{{ row.cmpnyNm }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- F-10 규약: 왼쪽=진행/확정(지정), 오른쪽=이탈(취소), 폭 균등 -->
        <div class="modal-footer">
          <div class="btn-group">
            <button
              class="btn btn-primary"
              :disabled="!selectedCmpnyCd || isBusy"
              @click="fnDesignate"
            >
              지정
            </button>
            <button class="btn btn-second" @click="$emit('close')">취소</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import { ref, defineProps, defineEmits, onMounted, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

const props = defineProps({
  sessionCd_p: String, // 대상 세션(필수)
  onSaved: Function, // 지정 성공 시 부모 재조회 콜백
});
const emit = defineEmits(["close"]);
const { proxy } = getCurrentInstance();

const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {});

// ── 반응형 상태 ────────────────────────────────────────────────
const candidates = ref([]); // [{ cmpnyCd, cmpnyNm }]
const selectedCmpnyCd = ref("");
const isLoading = ref(false);
const isBusy = ref(false);

// ── 조회 ───────────────────────────────────────────────────────
// 지정 후보 = 내 회사와 연동 관계(ACCEPTED)인 회사 − 개설사 − 이미 이 세션 체인에 있는 회사(서버 산출).
const fnSearch = async () => {
  if (proxy.$util.isEmpty(props.sessionCd_p)) return;

  isLoading.value = true;
  try {
    const response = await axios.get("/webApi/tbm02/session-share-candidates", {
      params: { sessionCd: props.sessionCd_p },
    });
    if (response.status === 200) {
      candidates.value = response.data?.candidateList || [];
    }
  } catch (err) {
    candidates.value = [];
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  } finally {
    isLoading.value = false;
  }
};

// ── 지정 ───────────────────────────────────────────────────────
// 서버 가드(중복 지정 / 관계 미수립 / 상태 불가)는 에러 메시지를 그대로 노출한다.
const fnDesignate = async () => {
  if (proxy.$util.isEmpty(selectedCmpnyCd.value) || isBusy.value) return;

  const selected = candidates.value.find(
    (c) => c.cmpnyCd === selectedCmpnyCd.value
  );
  const cmpnyNm = selected ? selected.cmpnyNm : "";

  const confirmed = await proxy.$confirm(
    `'${cmpnyNm}'을(를) 이 교육의 연동 회사로 지정하시겠습니까?`
  );
  if (!confirmed) return;

  isBusy.value = true;
  try {
    const response = await axios.post("/webApi/tbm02/session-share-designate", {
      sessionCd: props.sessionCd_p,
      shareCmpnyCd: selectedCmpnyCd.value,
    });
    if (response.status === 200) {
      await proxy.$alert("연동 회사로 지정했습니다.");
      if (props.onSaved) props.onSaved();
      emit("close");
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.")
    );
  } finally {
    isBusy.value = false;
  }
};

onMounted(() => {
  fnSearch();
});
</script>

<style scoped>
.share-guide {
  margin: 0 0 var(--space-md, 0.75rem);
  color: var(--color-text-muted);
  line-height: 1.5;
}
.table-wrapper {
  max-height: 40vh;
  overflow-y: auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 0.375rem);
}
/* F-10 규약: 좌우 버튼 폭 균등 */
.modal-footer .btn-group .btn {
  flex: 1;
}
</style>
