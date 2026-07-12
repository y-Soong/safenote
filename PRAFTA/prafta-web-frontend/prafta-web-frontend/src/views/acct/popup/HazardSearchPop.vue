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
        <!-- 1. Title 영역 -->
        <div class="modal-header" @mousedown="startDrag">
          <span>유해요인 검색 (다중 선택)</span>
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

        <!-- 2. 조회 Form 영역 (위험구분/위험분류는 부모 선택값 종속 → 유해요인명만 검색) -->
        <div class="viewSearch">
          <div class="form-left">
            <label>유해요인명</label>
            <input v-model.trim="hazardNm" @keyup.enter="fnSearch" />
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSearch">조회</button>
            <button class="btn btn-primary" @click="fnSelect">선택 완료</button>
          </div>
        </div>

        <!-- 3. 그리드 영역 -->
        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th class="check-col">
                    <input
                      type="checkbox"
                      :checked="isAllChecked"
                      :indeterminate.prop="isIndeterminate"
                      @change="toggleAll"
                    />
                  </th>
                  <th>위험분류</th>
                  <th>유해요인</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!filteredRows || filteredRows.length === 0">
                  <tr>
                    <td colspan="3" class="edu-grid-empty">
                      조회된 유해요인이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr
                    v-for="item in filteredRows"
                    :key="item.hazardCd"
                    :class="{ selected: selectedMap[item.hazardCd] }"
                    @click="toggleRow(item.hazardCd)"
                  >
                    <td class="check-col">
                      <input
                        type="checkbox"
                        :checked="!!selectedMap[item.hazardCd]"
                        @click.stop
                        @change="toggleRow(item.hazardCd)"
                      />
                    </td>
                    <td>{{ item.riskTypeNm || "-" }}</td>
                    <td>{{ item.hazardNm }}</td>
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
  reactive,
  computed,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";

const props = defineProps({
  siteCd: { type: String, default: "" }, // 부모(등록팝업)에서 선택된 사업장
  processCd: { type: String, default: "" }, // 위험구분(위험성평가) — 부모 선택값
  riskTypeCd: { type: String, default: "" }, // 위험분류(위험성평가) — 부모 선택값
  selectedCds: { type: Array, default: () => [] }, // 사전 선택된 유해요인코드
  onSelect: Function, // 다건 선택 결과 콜백 (useModal 환경)
});
const emit = defineEmits(["close", "select"]);

const { proxy } = getCurrentInstance();

// 반응형 상태
const hazardNm = ref(""); // 유해요인명 검색어
const rows = ref([]); // HAZARD 옵션 [{ hazardCd, hazardNm, riskTypeCd, riskTypeNm }]
const selectedMap = reactive({}); // { [hazardCd]: true }
const modalRef = ref(null);

const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// 유해요인명 검색어로 클라이언트 필터(옵션 EP는 이름 파라미터가 없음)
const filteredRows = computed(() => {
  const kw = hazardNm.value.trim();
  if (!kw) return rows.value;
  return rows.value.filter((r) => (r.hazardNm || "").includes(kw));
});

const selectedCount = computed(
  () => filteredRows.value.filter((r) => selectedMap[r.hazardCd]).length
);
const isAllChecked = computed(
  () =>
    filteredRows.value.length > 0 &&
    selectedCount.value === filteredRows.value.length
);
const isIndeterminate = computed(
  () =>
    selectedCount.value > 0 && selectedCount.value < filteredRows.value.length
);

onMounted(async () => {
  // 사전 선택값 반영
  (props.selectedCds || []).forEach((cd) => {
    if (cd) selectedMap[cd] = true;
  });
  await fnGetHazardOptions();
});

// 위험성평가 3계층 옵션 조회 후 HAZARD 만 추출(위험분류명은 RISK_TYPE 행에서 매핑)
const fnGetHazardOptions = async () => {
  rows.value = [];
  try {
    const response = await axios.get("/webApi/acct01/risk/category-options", {
      params: {
        siteCd: props.siteCd,
        processCd: props.processCd,
        riskTypeCd: props.riskTypeCd,
      },
    });
    if (response.status === 200) {
      const list = response.data?.categoryOptionList || [];
      const riskTypeNameMap = {};
      list.forEach((o) => {
        if (o.categoryType === "RISK_TYPE") riskTypeNameMap[o.code] = o.name;
      });
      rows.value = list
        .filter((o) => o.categoryType === "HAZARD")
        .map((o) => ({
          hazardCd: o.code,
          hazardNm: o.name,
          riskTypeCd: o.parentCode,
          riskTypeNm: riskTypeNameMap[o.parentCode] || "",
        }));
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "유해요인 조회 중 오류가 발생했습니다.")
    );
  }
};

// 조회 버튼: 이름 검색어가 바뀌면 클라이언트 필터만 갱신되므로 별도 호출 없이 재조회만 트리거
const fnSearch = () => {
  // filteredRows computed 가 hazardNm 에 반응하므로 동작 보장용 no-op(명시적 트리거)
};

const toggleRow = (hazardCd) => {
  if (selectedMap[hazardCd]) delete selectedMap[hazardCd];
  else selectedMap[hazardCd] = true;
};

const toggleAll = () => {
  if (isAllChecked.value) {
    filteredRows.value.forEach((r) => delete selectedMap[r.hazardCd]);
  } else {
    filteredRows.value.forEach((r) => {
      selectedMap[r.hazardCd] = true;
    });
  }
};

const fnSelect = async () => {
  // 화면 필터와 무관하게 체크된 전체를 반환(검색어로 가려진 기존 선택 보존)
  const selected = rows.value.filter((r) => selectedMap[r.hazardCd]);
  if (selected.length === 0) {
    await proxy.$alert("선택된 유해요인이 없습니다.");
    return;
  }
  if (typeof props.onSelect === "function") {
    props.onSelect(selected);
  } else {
    emit("select", selected);
  }
  emit("close");
};
</script>

<style scoped>
.viewSearch .form-left {
  align-items: center;
}
/* 유해요인명 입력칸을 기본(120px)보다 길게 */
.viewSearch .form-left input {
  width: 200px;
}
.check-col {
  width: 36px;
  text-align: center;
}
tbody tr {
  cursor: pointer;
}
tbody tr.selected {
  background: var(--color-primary-soft, rgba(22, 163, 74, 0.08));
}
</style>
