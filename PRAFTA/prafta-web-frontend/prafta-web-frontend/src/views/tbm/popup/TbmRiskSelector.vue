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
          <div>
            <label>위험구분</label>
            <select v-model="processCd" name="combo">
              <option value="">전체</option>
              <option
                v-for="opt in baseCodeArr['COM002'] || []"
                :key="opt.baimValDCd"
                :value="opt.baimValDCd"
              >
                {{ opt.baimValDNm }}
              </option>
            </select>
          </div>
          <div>
            <label>위험분류</label>
            <select v-model="riskTypeCd" name="combo">
              <option value="">전체</option>
              <option
                v-for="opt in riskTypeArr.filter((o) => {
                  if (proxy.$util.isEmpty(processCd)) {
                    return proxy.$util.isEmpty(o.processCd);
                  }
                  return (
                    o.processCd == processCd || proxy.$util.isEmpty(o.processCd)
                  );
                })"
                :key="opt.riskTypeCd"
                :value="opt.riskTypeCd"
              >
                {{ opt.riskTypeNm }}
              </option>
            </select>
          </div>
          <div>
            <label>유해요인 설명</label>
            <input
              v-model.trim="hazardDesc"
              placeholder="유해요인 설명"
              @keyup.enter="fnSearch"
            />
          </div>
          <div>
            <label>평가요청일</label>
            <CalendarSrch v-model="initAssessDate" />
          </div>
          <div>
            <label>평가요청자</label>
            <input
              v-model.trim="initAssessorNm"
              placeholder="평가요청자"
              @keyup.enter="fnSearch"
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
                  <th style="width: 30%">위험성평가</th>
                  <th style="width: 13%; text-align: center">평가요청일</th>
                  <th style="width: 12%">평가요청자</th>
                  <th style="width: 15%">유해요인 설명</th>
                  <th style="width: 12%; text-align: center">진행상태</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!filteredList || filteredList.length === 0">
                  <tr>
                    <td colspan="6" class="edu-grid-empty">
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
                    <td style="text-align: center">
                      {{ row.initAssessDate || "-" }}
                    </td>
                    <td>{{ row.initAssessorNm || "-" }}</td>
                    <td>{{ row.hazardNm }}</td>
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
            <button class="btn btn-primary" @click="fnConfirm">
              선택 완료
            </button>
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
  watch,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import CalendarSrch from "@/components/common/CalendarSrch.vue";

const { proxy } = getCurrentInstance();

const props = defineProps({
  siteCd_p: String, // 세션 사업장(스코프 필터)
  selectedKeys_p: Array, // 이미 선택된 키(siteCd|processCd|assessmentCd) 목록
  onConfirm: Function,
});
const emit = defineEmits(["close"]);

const modalRef = ref(null);
const riskList = ref([]);
const selectedMap = ref({}); // key -> row

// 조회조건: 위험구분(COM002) / 위험분류(risk-type) / 유해요인 설명(like)
const processCd = ref("");
const riskTypeCd = ref("");
const hazardDesc = ref("");
const initAssessDate = ref(""); // 평가요청일 필터(YYYY-MM-DD, 6.3 T6-13)
const initAssessorNm = ref(""); // 평가요청자 필터(like, 6.3 T6-13)
const baseCodeArr = ref({}); // COM002(위험구분) 코드 옵션
const riskTypeArr = ref([]); // 위험분류 옵션

const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// 위험성평가 식별키(복합키)
const rowKey = (row) => [row.siteCd, row.processCd, row.assessmentCd].join("|");

// 서버 조회 결과를 그대로 노출(필터는 서버 파라미터로 처리)
const filteredList = computed(() => riskList.value);

// 위험구분 변경 시 위험분류 리셋(Risk_03 패턴)
watch(processCd, () => {
  riskTypeCd.value = "";
});

onMounted(async () => {
  (props.selectedKeys_p || []).forEach((k) => {
    selectedMap.value[k] = true;
  });
  await fnGetBaseinfoList();
  await fnGetRiskTypeList();
  await fnSearch();
});

// 위험구분 코드 옵션(COM002) 로딩 — Risk_03.vue 패턴
const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/base-info-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        baseCodeList: ["COM002"],
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
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

// 위험분류 옵션 로딩(기존 risk03 조회 EP 재사용)
const fnGetRiskTypeList = async () => {
  try {
    const response = await axios.get("/webApi/risk03/risk-type-info-lists", {});
    if (response.status === 200) {
      riskTypeArr.value = response.data?.riskTypeResultList || [];
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
};

const fnSearch = async () => {
  riskList.value = [];
  try {
    const response = await axios.get("/webApi/tbm02/risk-options", {
      params: {
        siteCd: props.siteCd_p || "",
        processCd: processCd.value || "",
        riskTypeCd: riskTypeCd.value || "",
        hazardDesc: hazardDesc.value || "",
        initAssessDate: initAssessDate.value || "",
        initAssessorNm: initAssessorNm.value || "",
      },
    });

    if (response.status === 200) {
      riskList.value = response.data?.riskList || [];
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.")
    );
  }
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
