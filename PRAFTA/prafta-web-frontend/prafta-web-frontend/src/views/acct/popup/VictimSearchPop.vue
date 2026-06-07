<template>
  <Transition name="fade">
    <div
      v-show="true"
      class="modal-overlay prafta-modal-popup"
      @click.self="$emit('close')"
    >
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 1. Title -->
        <div class="modal-header" @mousedown="startDrag">
          <span>재해자 검색 (정규 + 일용)</span>
          <button class="icon-button" @click="$emit('close')" aria-label="닫기">
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
            <label>사용자유형</label>
            <BaseSelect v-model="userTypeCd">
              <option value="">— 전체 —</option>
              <option value="REGULAR">정규</option>
              <option value="DAILY">일용</option>
            </BaseSelect>
            <label>이름</label>
            <input v-model.trim="userNm" @keyup.enter="fnSearch" />
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSearch">조회</button>
          </div>
        </div>

        <!-- 3. 그리드 -->
        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th>유형</th>
                  <th>이름</th>
                  <th>휴대폰</th>
                  <th>소속</th>
                  <th>사업장</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!rows || rows.length === 0">
                  <tr>
                    <td colspan="5" class="edu-grid-empty">
                      조회된 재해자가 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr
                    v-for="item in rows"
                    :key="item.userTypeCd + '_' + item.userCd"
                    @dblclick="fnSelectRow(item)"
                    style="cursor: pointer"
                  >
                    <td>{{ item.userTypeNm || item.userTypeCd }}</td>
                    <td>{{ item.userNm }}</td>
                    <td>{{ item.mblNoLast4 ? "****" + item.mblNoLast4 : "-" }}</td>
                    <td>{{ item.nodeNm || "-" }}</td>
                    <td>{{ item.siteNm || "-" }}</td>
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
import {
  ref,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import BaseSelect from "@/components/common/BaseSelect.vue";

const props = defineProps({
  siteCd: { type: String, default: "" },
  onSelect: Function,
});
const emit = defineEmits(["close", "select"]);

const { proxy } = getCurrentInstance();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const userTypeCd = ref("");
const userNm = ref("");
const rows = ref([]);

onMounted(() => {
  fnSearch();
});

const fnSearch = async () => {
  rows.value = [];
  try {
    const response = await axios.get("/webApi/acct01/victim-search", {
      params: {
        siteCd: props.siteCd,
        userNm: userNm.value,
        userTypeCd: userTypeCd.value,
      },
    });
    if (response.status === 200) {
      rows.value = response.data?.victimList || [];
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const fnSelectRow = (item) => {
  if (typeof props.onSelect === "function") {
    props.onSelect(item);
  } else {
    emit("select", item);
  }
  emit("close");
};
</script>
