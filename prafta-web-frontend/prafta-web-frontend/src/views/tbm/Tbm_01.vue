<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="props.buttons"
      @search="fnSearch"
      @create="fnCreate"
      @save="fnSave"
      @delete="fnDelete"
    />
    <!-- 
      @create="fnCreate"
      @save="fnSave"
      @delete="fnDelete"
      @excel="fnExcel" -->

    <div class="viewSearch">
      <div>
        <label>자료유형</label>
        <select v-model.trim="mtrlType" name="combo">
          <option
            v-for="opt in baseCodeArr['COM003'] || []"
            :key="opt.baimValDCd"
            :value="opt.baimValDCd"
          >
            {{ opt.baimValDNm }}
          </option>
        </select>
      </div>
      <div>
        <label>자료명</label>
        <input v-model.trim="mtrlTitle" type="text" />
      </div>
      <div>
        <label>사용여부</label>
        <select v-model.trim="useYn" name="combo">
          <option
            v-for="opt in systCodeArr['SYS003'] || []"
            :key="opt.systValDCd"
            :value="opt.systValDCd"
          >
            {{ opt.systValDNm }}
          </option>
        </select>
      </div>
    </div>

    <div class="viewBody">
      <div class="table-wrapper subtitle-pane">
        <!-- ⬇️ 소제목 바 -->
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <!-- 단순 마크 아이콘 (SVG) -->
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">사용자 리스트</span>
        </div>

        <div
          class="table-box overflow-x-auto rounded-md border border-slate-300"
          style="--box-h: 70vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table
            class="data-grid w-full table-fixed text-sm text-left rtl:text-right"
          >
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <th style="width: 4%">
                  <input
                    id="headChk"
                    v-model="headChk"
                    type="checkbox"
                    @click="fnHeadChk"
                  />
                </th>
                <ThSortable
                  label="교육자료명"
                  col-key="title"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.title"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <th class="event_cell" style="width: 10%">교육자료 타입</th>
                <th style="width: 15%">사용여부</th>
                <ThSortable
                  label="교육자료 설명"
                  col-key="contents"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.contents"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="등록된 교육자료 수"
                  col-key="mtrlCnt"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.mtrlCnt"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="등록자"
                  col-key="insertNm"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.insertNm"
                  @sort="onSort"
                  @update:width="onResize"
                />
                <ThSortable
                  label="등록일자"
                  col-key="insertDate"
                  :sort-key="sortKey"
                  :sort-order="sortOrder"
                  :width="colWidths.insertDate"
                  @sort="onSort"
                  @update:width="onResize"
                />
              </tr>
            </thead>
            <tbody>
              <template
                v-if="
                  !tbmEduInfoResultList || tbmEduInfoResultList.length === 0
                "
              >
                <tr>
                  <td colspan="9" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(info, idx) in sortedData" :key="info.id">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input type="checkbox" v-model="info.chk" />
                  </td>
                  <td @dblclick="fnTbmEduMtrlInfoPopOpen(info)">
                    {{ info.title }}
                  </td>
                  <td>
                    <BaseSelect v-model="info.mtrlType">
                      <option
                        v-for="opt in (baseCodeArr['COM003'] || []).filter(
                          (o) => o.baimValDCd != null
                        )"
                        :key="opt.baimValDCd"
                        :value="opt.baimValDCd"
                      >
                        {{ opt.baimValDNm }}
                      </option>
                    </BaseSelect>
                  </td>
                  <td @dblclick="fnTbmEduMtrlInfoPopOpen(info)">
                    <BaseSelect v-model="info.useYn">
                      <option
                        v-for="opt in (systCodeArr['SYS003'] || []).filter(
                          (o) => o.systValDCd != null
                        )"
                        :key="opt.systValDCd"
                        :value="opt.systValDCd"
                      >
                        {{ opt.systValDNm }}
                      </option>
                    </BaseSelect>
                  </td>
                  <td @dblclick="fnTbmEduMtrlInfoPopOpen(info)">
                    {{ info.contents }}
                  </td>
                  <td @dblclick="fnTbmEduMtrlInfoPopOpen(info)">
                    {{ info.mtrlCnt }}
                  </td>
                  <td @dblclick="fnTbmEduMtrlInfoPopOpen(info)">
                    {{ info.insertNm }}
                  </td>
                  <td @dblclick="fnTbmEduMtrlInfoPopOpen(info)">
                    {{ info.insertDate }}
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// ================ Imports ================
import {
  ref,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useModal } from "@/utils/useModal";
import { useFieldWatcher } from "@/utils/useFieldWatcher";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import BaseSelect from "@/components/common/BaseSelect.vue";
import TbmEduMtrlInfo from "./popup/TbmEduMtrlInfo.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

// ================ Options ================
defineOptions({ name: "Tbm_01" });

// ================ Props & Emits ================
const props = defineProps({
  title: String,
  buttons: Object,
});

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ================ Refs (Variables) ================
const tbmEduInfoResultList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } =
  useTableSort(tbmEduInfoResultList);
const { colWidths, onResize } = useColumnResize({
  title: 130,
  contents: 180,
  mtrlCnt: 130,
  insertNm: 110,
  insertDate: 110,
});
const systCodeArr = ref([]);
const baseCodeArr = ref([]);

// 조회조건 변수
const mtrlType = ref();
const mtrlTitle = ref();
const useYn = ref();

// 화면 제어 변수
const headChk = ref(false);

// ================ Watchers ================
useFieldWatcher(
  tbmEduInfoResultList,
  (item) => {
    item.chk = true;
  },
  ["chk"]
);

// ================ Life Cycle Functions ================
onMounted(async () => {
  await fnGetSystinfoList();
  await fnGetBaseinfoList();
  await fnSearch();
});

// ================ API Functions ================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS003"],
      },
    });

    if (response.status === 200) {
      const resData = response.data?.systInfoList || [];

      const grouped = {};
      resData.forEach((item) => {
        const key = item.systValCd;
        if (!grouped[key]) {
          grouped[key] = [];
        }
        grouped[key].push(item);
      });

      systCodeArr.value = grouped;

      useYn.value = systCodeArr.value.SYS003[0].systValDCd;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/base-info-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        baseCodeList: ["COM003"],
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

      mtrlType.value = baseCodeArr.value.COM003[0].baimValDCd;

      // mtrlType.value = baseCodeArr.value.COM003.filter(
      //   (o) => o.baimValDCd != null
      // )[0].baimValDCd;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  tbmEduInfoResultList.value = [];

  try {
    const response = await axios.get("/webApi/tbm01/tbm-edu-infos", {
      params: {
        mtrlType: mtrlType.value,
        title: mtrlTitle.value,
        useYn: useYn.value,
      },
    });

    if (response.status === 200) {
      tbmEduInfoResultList.value = response.data?.tbmEduInfoResultList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSave = async () => {
  const filteredUsers = tbmEduInfoResultList.value.filter((user) => user.chk);

  if (filteredUsers.length == 0) {
    proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/tbm01/save-tbm-edus",
      filteredUsers
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnDelete = async () => {
  const filteredUsers = tbmEduInfoResultList.value.filter((user) => user.chk);

  if (filteredUsers.length == 0) {
    proxy.$alert(getMessage(MSG.DELETE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.DELETE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/tbm01/delete-tbm-edus",
      filteredUsers
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSearch();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "삭제 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

// ================ Methods/Functions ================
const fnCreate = () => {
  openPop(TbmEduMtrlInfo, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    onSearch: fnSearch,
  });
};

const fnHeadChk = () => {
  headChk.value = !headChk.value;
  tbmEduInfoResultList.value.forEach((item) => {
    item.chk = headChk.value;
  });
};

const fnTbmEduMtrlInfoPopOpen = (info) => {
  openPop(TbmEduMtrlInfo, {
    mtrlCd_p: info.mtrlCd,
    onSearch: fnSearch,
  });
};
</script>

<style scoped></style>
