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
          <span>점검문항 검색</span>
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
            <label>점검항목명</label>
            <input v-model.trim="inspectItemSubj" @keyup.enter="fnSearch" />
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSearch">조회</button>
          </div>
        </div>

        <!-- 3. 그리드 (점검항목명 / 시행월) — 더블클릭 선택 -->
        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th>점검항목명</th>
                  <th style="width: 25%">시행월</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!itemList || itemList.length === 0">
                  <tr>
                    <td colspan="2" class="edu-grid-empty">
                      조회된 점검문항이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr
                    v-for="item in itemList"
                    :key="item.inspectItemCd"
                    @dblclick="fnSelectRow(item)"
                  >
                    <td>{{ item.inspectItemSubj }}</td>
                    <td>{{ item.strDate }}</td>
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

const itemList = ref([]);
const inspectItemSubj = ref("");

onMounted(async () => {
  await fnSearch();
});

// 점검문항 조회 (chkLstType 필수, chkLst04 전용 EP)
const fnSearch = async () => {
  itemList.value = [];

  try {
    const response = await axios.get("/webApi/chkLst04/inspect-item-lists", {
      params: {
        chkLstType: props.chkLstType_p,
        inspectItemSubj: inspectItemSubj.value,
      },
    });

    if (response.status === 200) {
      itemList.value = response.data?.inspectItemResultList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 더블클릭 선택 → 부모에 (inspectItemCd, inspectItemSubj) 반환
function fnSelectRow(item) {
  if (typeof props.onSelect === "function") {
    props.onSelect(item.inspectItemCd, item.inspectItemSubj);
  }
  emit("close");
}
</script>
