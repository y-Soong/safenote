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
          <span>문항 변경이력</span>
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

        <!-- 2. 대상 문항 정보 -->
        <div class="viewSearch">
          <div class="form-left">
            <label>점검항목명</label>
            <input :value="inspectItemSubj_p" disabled />
          </div>
        </div>

        <!-- 3. 이력 그리드 (변경 후 상태 스냅샷, 최신순) -->
        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th style="width: 15%">변경일시</th>
                  <th style="width: 10%">변경유형</th>
                  <th>점검항목명</th>
                  <th style="width: 12%">시행일</th>
                  <th style="width: 8%">정렬순서</th>
                  <th style="width: 8%">사용여부</th>
                  <th style="width: 10%">변경자</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!histList || histList.length === 0">
                  <tr>
                    <td colspan="7" class="edu-grid-empty">
                      변경이력이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr v-for="(item, idx) in histList" :key="idx">
                    <td>{{ item.chgDtime }}</td>
                    <td>{{ item.chgTypeNm }}</td>
                    <td>{{ item.inspectItemSubj }}</td>
                    <td>{{ formatStrDate(item.strDate) }}</td>
                    <td style="text-align: center">{{ item.sortIdx ?? "-" }}</td>
                    <td>{{ item.useYn === "N" ? "미사용" : "사용" }}</td>
                    <td>{{ item.chgUserNm || "-" }}</td>
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
  siteCd_p: String, // PRAFTA-SUBCON-T0-05: 사업장 키 관통
  chkLstType_p: String,
  inspectItemCd_p: String,
  inspectItemSubj_p: String,
});

defineEmits(["close"]);

const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const histList = ref([]);

onMounted(async () => {
  await fnSearch();
});

// 시행일 표기 YYYYMMDD -> YYYY-MM-DD (구값 YYYYMM 은 YYYY-MM)
const formatStrDate = (strDate) => {
  const s = String(strDate ?? "");
  if (s.length === 8)
    return `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`;
  if (s.length === 6) return `${s.slice(0, 4)}-${s.slice(4, 6)}`;
  return s;
};

// 문항 변경이력 조회 (최신순, 각 행은 변경 '후' 상태 스냅샷)
const fnSearch = async () => {
  histList.value = [];

  try {
    const response = await axios.get(
      "/webApi/chkLst02/chkpt-inspect-item-hists",
      {
        params: {
          siteCd: props.siteCd_p, // PRAFTA-SUBCON-T0-05: 사업장 키 관통
          chkLstType: props.chkLstType_p,
          inspectItemCd: props.inspectItemCd_p,
        },
      }
    );

    if (response.status === 200) {
      histList.value = response.data?.chkptInspectItemHistResultList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};
</script>
