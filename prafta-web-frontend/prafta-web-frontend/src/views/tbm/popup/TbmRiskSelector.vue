<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>위험성평가 선택</span>
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

        <div class="viewSearch">
          <div class="form-left">
            <label>검색</label>
            <input
              v-model.trim="searchKeyword"
              placeholder="공정/유해요인"
              @keyup.enter="fnFilter"
            />
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSearch">조회</button>
          </div>
        </div>

        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th style="width: 5%; text-align: center">선택</th>
                  <th style="width: 40%">위험성평가</th>
                  <th style="width: 15%">공정</th>
                  <th style="width: 12%; text-align: center">진행상태</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!filteredList || filteredList.length === 0">
                  <tr>
                    <td colspan="4" class="edu-grid-empty">
                      조회된 위험성평가가 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr
                    v-for="row in filteredList"
                    :key="rowKey(row)"
                    @dblclick="fnToggle(row)"
                  >
                    <td style="text-align: center">
                      <input
                        type="checkbox"
                        :checked="isSelected(row)"
                        @change="fnToggle(row)"
                      />
                    </td>
                    <td>{{ row.displayName || "(이름 미정)" }}</td>
                    <td>{{ row.processNm || row.processCd }}</td>
                    <td style="text-align: center">
                      {{ row.assessmentStatusNm || row.assessmentStatus }}
                    </td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnConfirm">선택 완료</button>
            <button class="btn btn-second" @click="$emit('close')">닫기</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import {
  ref,
  computed,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";

const { proxy } = getCurrentInstance();

const props = defineProps({
  siteCd_p: String, // 세션 사업장(스코프 필터)
  selectedKeys_p: Array, // 이미 선택된 키(siteCd|processCd|assessmentCd) 목록
  onConfirm: Function,
});
const emit = defineEmits(["close"]);

const modalRef = ref(null);
const riskList = ref([]);
const searchKeyword = ref("");
const filterKeyword = ref("");
const selectedMap = ref({}); // key -> row

const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// 위험성평가 식별키(복합키)
const rowKey = (row) =>
  [row.siteCd, row.processCd, row.assessmentCd].join("|");

const filteredList = computed(() => {
  const kw = (filterKeyword.value || "").trim();
  if (!kw) return riskList.value;
  return riskList.value.filter(
    (r) =>
      (r.displayName || "").indexOf(kw) >= 0 ||
      (r.processNm || "").indexOf(kw) >= 0 ||
      (r.hazardNm || "").indexOf(kw) >= 0
  );
});

onMounted(async () => {
  (props.selectedKeys_p || []).forEach((k) => {
    selectedMap.value[k] = true;
  });
  await fnSearch();
});

const fnSearch = async () => {
  riskList.value = [];
  try {
    const response = await axios.get("/webApi/tbm02/risk-options", {
      params: {
        siteCd: props.siteCd_p || "",
        searchKeyword: searchKeyword.value,
      },
    });

    if (response.status === 200) {
      riskList.value = response.data?.riskList || [];
      filterKeyword.value = searchKeyword.value;
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const fnFilter = () => {
  filterKeyword.value = searchKeyword.value;
};

const isSelected = (row) => !!selectedMap.value[rowKey(row)];

const fnToggle = (row) => {
  const key = rowKey(row);
  if (selectedMap.value[key]) {
    delete selectedMap.value[key];
  } else {
    selectedMap.value[key] = row;
  }
};

const fnConfirm = () => {
  const selected = riskList.value.filter((r) => selectedMap.value[rowKey(r)]);
  if (typeof props.onConfirm === "function") {
    props.onConfirm(selected);
  }
  emit("close");
};
</script>

<style scoped></style>
