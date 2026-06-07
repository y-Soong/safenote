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
          <span>점검대상 검색 (다중 선택)</span>
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

        <!-- 2. 조회 Form 영역 -->
        <div class="viewSearch">
          <div class="form-left">
            <label>점검구분</label>
            <BaseSelect v-model="chklstType">
              <option value="">— 전체 —</option>
              <option
                v-for="opt in baseCodeArr['COM001'] || []"
                :key="opt.baimValDCd"
                :value="opt.baimValDCd"
              >
                {{ opt.baimValDNm }}
              </option>
            </BaseSelect>
            <label>점검대상명</label>
            <input v-model.trim="chkptNm" @keyup.enter="fnSearch" />
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSearch">조회</button>
            <button class="btn btn-primary" @click="fnSelect">
              선택 완료
            </button>
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
                  <th>점검구분</th>
                  <th>점검대상명</th>
                  <th>관리자</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!rows || rows.length === 0">
                  <tr>
                    <td colspan="4" class="edu-grid-empty">
                      조회된 점검대상이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr
                    v-for="item in rows"
                    :key="item.chkptCd"
                    :class="{ selected: selectedMap[item.chkptCd] }"
                    @click="toggleRow(item.chkptCd)"
                  >
                    <td class="check-col">
                      <input
                        type="checkbox"
                        :checked="!!selectedMap[item.chkptCd]"
                        @click.stop
                        @change="toggleRow(item.chkptCd)"
                      />
                    </td>
                    <td>{{ item.chklstTypeNm || item.chklstType }}</td>
                    <td>{{ item.chkptNm }}</td>
                    <td>{{ item.mgmtUserNm || "-" }}</td>
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
import BaseSelect from "@/components/common/BaseSelect.vue";

const props = defineProps({
  siteCd: { type: String, default: "" }, // 부모(등록팝업)에서 선택된 사업장
  onSelect: Function, // 다건 선택 결과 콜백 (useModal 환경)
});
const emit = defineEmits(["close", "select"]);

const { proxy } = getCurrentInstance();

// 반응형 상태
const chklstType = ref("");
const chkptNm = ref("");
const rows = ref([]); // 조회 결과 (ChkptOptionResult[])
const selectedMap = reactive({}); // { [chkptCd]: true }
const baseCodeArr = ref({}); // COM001 점검구분 코드
const modalRef = ref(null);

// 공통 훅: 화면 중앙(살짝 위쪽) 배치 + 드래그
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

const selectedCount = computed(
  () => Object.values(selectedMap).filter(Boolean).length
);
const isAllChecked = computed(
  () => rows.value.length > 0 && selectedCount.value === rows.value.length
);
const isIndeterminate = computed(
  () => selectedCount.value > 0 && selectedCount.value < rows.value.length
);

onMounted(async () => {
  await fnGetBaseinfoList();
  await fnSearch();
});

// 점검구분(COM001) 코드 조회 (baimValCd 기준 그룹핑)
const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/base-info-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        baseCodeList: ["COM001"],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.baseInfoList || [];
      const grouped = {};
      resData.forEach((item) => {
        const key = item.baimValCd;
        if (!grouped[key]) grouped[key] = [];
        grouped[key].push(item);
      });
      baseCodeArr.value = grouped;
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "점검구분 조회 중 오류가 발생했습니다.")
    );
  }
};

// 점검대상 검색 (식별자 cmpnyCd 는 서버 JWT 도출 → 미전송)
const fnSearch = async () => {
  rows.value = [];
  Object.keys(selectedMap).forEach((k) => delete selectedMap[k]);

  try {
    const response = await axios.get(
      "/webApi/acct01/patrol/chkpt-options",
      {
        params: {
          siteCd: props.siteCd,
          chklstType: chklstType.value,
          chkptNm: chkptNm.value,
        },
      }
    );

    if (response.status === 200) {
      rows.value = response.data?.chkptOptionList || [];
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const toggleRow = (chkptCd) => {
  if (selectedMap[chkptCd]) delete selectedMap[chkptCd];
  else selectedMap[chkptCd] = true;
};

const toggleAll = () => {
  if (isAllChecked.value) {
    Object.keys(selectedMap).forEach((k) => delete selectedMap[k]);
  } else {
    rows.value.forEach((r) => {
      selectedMap[r.chkptCd] = true;
    });
  }
};

const fnSelect = async () => {
  const selected = rows.value.filter((r) => selectedMap[r.chkptCd]);
  if (selected.length === 0) {
    await proxy.$alert("선택된 점검대상이 없습니다.");
    return;
  }
  // useModal 환경에서는 onSelect 콜백, 일반 부모에서는 emit 둘 다 지원
  if (typeof props.onSelect === "function") {
    props.onSelect(selected);
  } else {
    emit("select", selected);
  }
  emit("close");
};
</script>

<style scoped>
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
