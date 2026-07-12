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
            <label>사용여부</label>
            <select v-model="useYn" name="combo" @change="fnSearch">
              <option value="">전체</option>
              <option value="Y">사용</option>
              <option value="N">미사용</option>
            </select>
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSearch">조회</button>
          </div>
        </div>

        <!-- 3. 그리드 (점검항목명 / 시행일) — 더블클릭 선택 -->
        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th>점검항목명</th>
                  <th style="width: 25%">시행일</th>
                  <th style="width: 15%">사용여부</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!itemList || itemList.length === 0">
                  <tr>
                    <td colspan="3" class="edu-grid-empty">
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
                    <td>{{ formatStrDate(item.strDate) }}</td>
                    <td>{{ item.useYn === "N" ? "미사용" : "사용" }}</td>
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
const useYn = ref(""); // 사용여부 필터(전체='' 기본) - PRAFTA_COM_001-T5-12.3.1

onMounted(async () => {
  await fnSearch();
});

// PRAFTA_COM_001-T5-12.3.3: 시행일 표기 YYYYMMDD -> YYYY-MM-DD (구값 YYYYMM 은 YYYY-MM)
const formatStrDate = (strDate) => {
  const s = String(strDate ?? "");
  if (s.length === 8)
    return `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`;
  if (s.length === 6) return `${s.slice(0, 4)}-${s.slice(4, 6)}`;
  return s;
};

// 점검문항 조회 (chkLstType 필수, chkLst04 전용 EP)
const fnSearch = async () => {
  itemList.value = [];

  try {
    const response = await axios.get("/webApi/chkLst04/inspect-item-lists", {
      params: {
        chkLstType: props.chkLstType_p,
        inspectItemSubj: inspectItemSubj.value,
        useYn: useYn.value,
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
