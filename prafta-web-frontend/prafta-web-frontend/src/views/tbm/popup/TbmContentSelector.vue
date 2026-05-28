<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <div class="modal-header" @mousedown="startDrag">
          <span>콘텐츠 선택</span>
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
            <label>제목</label>
            <input v-model.trim="searchKeyword" @keyup.enter="fnSearch" />
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
                  <th style="width: 10%">스코프</th>
                  <th style="width: 35%">콘텐츠 제목</th>
                  <th style="width: 15%">카테고리</th>
                  <th style="width: 10%; text-align: center">항목수</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!contentList || contentList.length === 0">
                  <tr>
                    <td colspan="5" class="edu-grid-empty">
                      조회된 콘텐츠가 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr
                    v-for="row in contentList"
                    :key="row.mtrlCd"
                    @dblclick="fnToggle(row)"
                  >
                    <td style="text-align: center">
                      <input
                        type="checkbox"
                        :checked="isSelected(row.mtrlCd)"
                        @change="fnToggle(row)"
                      />
                    </td>
                    <td style="text-align: center">
                      <span
                        class="scope-badge"
                        :class="
                          row.isCommonContent === 'Y'
                            ? 'scope-badge-common'
                            : 'scope-badge-site'
                        "
                      >
                        {{ row.isCommonContent === "Y" ? "회사공통" : "사업장" }}
                      </span>
                    </td>
                    <td>{{ row.title }}</td>
                    <td>{{ row.mtrlTypeNm || row.mtrlType }}</td>
                    <td style="text-align: center">{{ row.itemCnt }}</td>
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
  selectedMtrlCds_p: Array, // 이미 선택된 묶음코드 목록
  onConfirm: Function, // 선택 완료 콜백(선택 객체 배열 전달)
});
const emit = defineEmits(["close"]);

const modalRef = ref(null);
const contentList = ref([]);
const searchKeyword = ref("");
const selectedMap = ref({}); // mtrlCd -> row

const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

onMounted(async () => {
  // 기존 선택값 복원
  (props.selectedMtrlCds_p || []).forEach((cd) => {
    selectedMap.value[cd] = true;
  });
  await fnSearch();
});

const fnSearch = async () => {
  contentList.value = [];
  try {
    const response = await axios.get("/webApi/tbm02/content-options", {
      params: {
        siteCd: props.siteCd_p || "",
        searchKeyword: searchKeyword.value,
      },
    });

    if (response.status === 200) {
      contentList.value = response.data?.contentList || [];
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const isSelected = (mtrlCd) => !!selectedMap.value[mtrlCd];

const fnToggle = (row) => {
  if (selectedMap.value[row.mtrlCd]) {
    delete selectedMap.value[row.mtrlCd];
  } else {
    selectedMap.value[row.mtrlCd] = row;
  }
};

const fnConfirm = () => {
  const selected = contentList.value.filter((r) => selectedMap.value[r.mtrlCd]);
  if (typeof props.onConfirm === "function") {
    props.onConfirm(selected);
  }
  emit("close");
};
</script>

<style scoped>
.scope-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  font-weight: 600;
}

.scope-badge-common {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.scope-badge-site {
  background: var(--color-bg);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}
</style>
