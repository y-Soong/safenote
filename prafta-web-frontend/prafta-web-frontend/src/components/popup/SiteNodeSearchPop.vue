<template>
  <Transition name="fade">
    <div class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 🔹 1. Title 영역 -->
        <div class="modal-header" @mousedown="startDrag">
          <span>부서 검색</span>
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
        <!-- 🔹 2. 조회 Form 영역 -->
        <div class="viewSearch">
          <div class="form-left">
            <div>
              <label>부서번호</label>
              <input v-model="nodeCd" />
            </div>
            <div>
              <label>부서명</label>
              <input v-model="nodeNm" />
            </div>
            <div>
              <label>부서타입</label>
              <select v-model="nodeType" name="combo">
                <option
                  v-for="opt in baseCodeArr['COM004'] || []"
                  :key="opt.baimValDCd"
                  :value="opt.baimValDCd"
                >
                  {{ opt.baimValDNm }}
                </option>
              </select>
            </div>
            <div>
              <label>상위부서명</label>
              <input v-model="parentNodeNm" />
            </div>
          </div>
          <div class="btn-group">
            <button class="btn btn-primary" @click="fnSearch">조회</button>
          </div>
        </div>

        <!-- 🔹 3. 그리드 영역 -->
        <div class="viewBody">
          <div class="table-wrapper">
            <table class="data-grid">
              <thead>
                <tr>
                  <th>부서번호</th>
                  <th>부서명</th>
                  <th>부서타입</th>
                  <th>상위부서명</th>
                  <th>자체근태승인여부</th>
                </tr>
              </thead>
              <tbody>
                <template v-if="!nodeList || nodeList.length === 0">
                  <tr>
                    <td colspan="5" class="edu-grid-empty">
                      등록된 세부 항목이 없습니다.
                    </td>
                  </tr>
                </template>
                <template v-else>
                  <tr
                    v-for="node in nodeList"
                    :key="node.nodeCd"
                    @dblclick="fnSelectRow(node.nodeCd, node.nodeNm)"
                  >
                    <td>{{ node.nodeCd }}</td>
                    <td>{{ node.nodeNm }}</td>
                    <td>
                      <BaseSelect
                        v-model="node.nodeType"
                        name="combo"
                        :readonly="true"
                      >
                        <option
                          v-for="opt in baseCodeArr['COM004'] || []"
                          :key="opt.baimValDCd"
                          :value="opt.baimValDCd"
                        >
                          {{ opt.baimValDNm }}
                        </option>
                      </BaseSelect>
                    </td>
                    <td>{{ node.parentNodeNm }}</td>
                    <td>{{ node.selfAttdApprvYn }}</td>
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
  defineProps,
  defineEmits,
  ref,
  getCurrentInstance,
  onMounted,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import BaseSelect from "@/components/common/BaseSelect.vue";
import axios from "@/api/axios";

const emit = defineEmits(["select", "close"]);
const modalRef = ref(null);
const nodeList = ref([]);
const baseCodeArr = ref([]);
const { proxy } = getCurrentInstance();

const nodeCd = ref("");
const nodeNm = ref("");
const nodeType = ref("");
const parentNodeNm = ref("");

const props = defineProps({
  visible: Boolean,
  cmpnyCd_p: String,
  siteCd_p: String,
  userId_p: String,
  onSelect: Function,
});

// 공통 훅으로 화면 중앙(살짝 위쪽)에 배치 + 드래그 가능
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

onMounted(async () => {
  await fnGetBaseinfoList();
  await fnSearch();
});

const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/base-info-list", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd") || props.cmpnyCd_p,
        baseCodeList: ["COM004"],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.baseInfoList || [];

      const grouped = {};
      resData.forEach((item) => {
        const key = item.baimValCd;
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });

      baseCodeArr.value = grouped;

      nodeType.value = baseCodeArr.value.COM004[0].baimValDCd;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
    // proxy.$alert(err.response.data.message);
  }
};

const fnSearch = async () => {
  nodeList.value = [];
  try {
    const response = await axios.get("/comApi/baseinfo/site-node-lists", {
      params: {
        cmpnyCd: props.cmpnyCd_p,
        siteCd: props.siteCd_p,
        nodeCd: nodeCd.value,
        nodeNm: nodeNm.value,
        nodeType: nodeType.value,
        parentNodeNm: parentNodeNm.value,
      },
    });

    if (response.status === 200) {
      nodeList.value = response.data?.siteNodeInfoList || [];
    }
  } catch (err) {
    alert(err.response.data.message);
  }
};

const fnSelectRow = (nodeCd, nodeNm) => {
  // emit("select", siteCd, siteNo, siteNm); // SITE_CD 부모에 전달
  if (proxy.$util.isEmpty(props.userId_p)) {
    props.onSelect(nodeCd, nodeNm);
  } else {
    props.onSelect(props.userId_p, nodeCd, nodeNm);
  }

  emit("close"); // 팝업 닫기
};
</script>
