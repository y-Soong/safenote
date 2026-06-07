<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 1. Title -->
        <div class="modal-header" @mousedown="startDrag">
          <span>점검대상 검색</span>
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

        <!-- 2. 조회 Form -->
        <div class="viewSearch">
          <div class="form-left">
            <label>점검대상명칭</label>
            <input v-model.trim="chkptNm" @keyup.enter="fnSearch" />
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSearch">조회</button>
          </div>
        </div>

        <!-- 3. 그리드 (점검대상명칭 / 관리자 / 비고) — 더블클릭 선택 -->
        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th style="width: 35%">점검대상명칭</th>
                  <th style="width: 25%">관리자</th>
                  <th>비고</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!chkptList || chkptList.length === 0">
                  <tr>
                    <td colspan="3" class="edu-grid-empty">
                      조회된 점검대상이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr
                    v-for="chkpt in chkptList"
                    :key="chkpt.chkptCd"
                    @dblclick="fnSelectRow(chkpt)"
                  >
                    <td>{{ chkpt.chkptNm }}</td>
                    <td>{{ chkpt.mgmtUserNm }}</td>
                    <td>{{ chkpt.chkptDesc }}</td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import {
  defineProps,
  defineEmits,
  ref,
  getCurrentInstance,
  onMounted,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

const props = defineProps({
  cmpnyCd_p: String,
  siteCd_p: String,
  chkLstType_p: String,
  onSelect: Function,
});

const emit = defineEmits(["select", "close"]);

const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const chkptList = ref([]);
const chkptNm = ref("");

onMounted(async () => {
  await fnSearch();
});

// 점검대상 조회 (siteCd + chkLstType 필수)
const fnSearch = async () => {
  chkptList.value = [];

  try {
    const response = await axios.get("/webApi/chkLst04/chkpt-target-lists", {
      params: {
        siteCd: props.siteCd_p,
        chkLstType: props.chkLstType_p,
        chkptNm: chkptNm.value,
      },
    });

    if (response.status === 200) {
      chkptList.value = response.data?.chkptTargetResultList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 더블클릭 선택 → 부모에 (chkptCd, chkptNm) 반환
function fnSelectRow(chkpt) {
  if (typeof props.onSelect === "function") {
    props.onSelect(chkpt.chkptCd, chkpt.chkptNm);
  }
  emit("close");
}
</script>
