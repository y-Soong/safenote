<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @save="fnSave"
      @create="fnCreate"
    />
    <!-- 
      @create="fnCreate"
      @save="fnSave"
      @delete="fnDelete"
    @excel="fnExcel"-->

    <div class="viewSearch">
      <div>
        <label>사업장번호</label>
        <input v-model.trim="sr_siteNo" type="text" />
      </div>
      <div>
        <label>사업장명</label>
        <input v-model.trim="sr_siteNm" type="text" />
      </div>

      <div>
        <label>사용여부</label>
        <select v-model.trim="sr_useYn" name="combo">
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
          <span class="subtitle-text">사업장 리스트</span>
        </div>

        <div
          class="table-box"
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
                <th class="event_cell" style="width: 10%">사업장번호</th>
                <th class="event_cell" style="width: 10%">사업장명</th>
                <th style="width: 20%">주소</th>
                <th>상세주소</th>
                <th class="editableCell" style="width: 8%">GPS허용용범위</th>
                <th class="editableCell" style="width: 8%">사용여부</th>
                <th style="width: 8%">사업개시일</th>
                <th style="width: 8%">사업종료일</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!siteInfoList || siteInfoList.length === 0">
                <tr>
                  <td colspan="10" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(site, idx) in siteInfoList" :key="site.id">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input type="checkbox" v-model="site.chk" />
                  </td>
                  <td @dblclick="fnSiteInfoPopOpen(site)">{{ site.siteNo }}</td>
                  <td @dblclick="fnSiteInfoPopOpen(site)">{{ site.siteNm }}</td>
                  <td @dblclick="fnSiteInfoPopOpen(site)">{{ site.addr1 }}</td>
                  <td @dblclick="fnSiteInfoPopOpen(site)">{{ site.addr2 }}</td>
                  <td @dblclick="fnSiteInfoPopOpen(site)">
                    {{ site.gpsRange }}
                  </td>
                  <td @dblclick="fnSiteInfoPopOpen(site)">
                    <BaseSelect v-model="site.useYn">
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
                  <td>
                    <CalendarSrch
                      :range="false"
                      :readonly="true"
                      style="width: 150px"
                      v-model="site.strDate"
                    />
                  </td>
                  <td>
                    <CalendarSrch
                      :range="false"
                      :readonly="true"
                      style="width: 150px"
                      v-model="site.endDate"
                    />
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
import BaseSelect from "@/components/common/BaseSelect.vue";
import SiteInfoPop from "./popup/SiteInfoPop.vue";
import CalendarSrch from "@/components/common/CalendarSrch";

// ================ Options ================
defineOptions({ name: "Baim_01" });

// ================ Props & Emits ================
const props = defineProps({
  title: String,
  buttons: Object,
});

// ================ Instance & Composables ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ================ Refs (Variables) ================
const localButtons = ref({ ...props.buttons });
const siteInfoList = ref([]);
const systCodeArr = ref({});

// 조회조건 변수
const sr_siteNo = ref("");
const sr_siteNm = ref("");
const sr_useYn = ref("Y");

// 화면 제어 변수
const p_siteCd = ref("");
const headChk = ref(false);

// ================ Watchers ================
useFieldWatcher(
  siteInfoList,
  (item) => {
    item.chk = true;
  },
  ["chk"]
);

// ================ Life Cycle Functions ================
onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
  await fnSearch();
});

// ================ API Functions ================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-list", {
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
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  siteInfoList.value = [];

  try {
    const response = await axios.get("/webApi/baim01/site-info-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: sr_siteNo.value,
        siteNm: sr_siteNm.value,
        useYn: sr_useYn.value,
      },
    });

    if (response.status === 200) {
      siteInfoList.value = response.data?.siteInfoList || [];
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnSave = async () => {
  const filteredSite = siteInfoList.value.filter((user) => user.chk);
  const dataList = proxy.$util.toCamelCaseKeys(filteredSite);

  if (dataList.length == 0) {
    proxy.$alert("저장할 데이터가 없습니다.");
    return;
  }

  const ok = await proxy.$confirm("저장하시겠습니까 ?");
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/baim01/updateSiteInfo",
      dataList
    );

    if (response.status === 200) {
      proxy.$alert("처리되었습니다.");
      fnSearch();
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "저장 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

// ================ Methods/Functions ================
const fnButtonControll = () => {
  // localButtons.value.search = "N";
  // localButtons.value.create = "N";
  // localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

const fnHeadChk = () => {
  headChk.value = !headChk.value;
  siteInfoList.value.forEach((item) => {
    item.CHK = headChk.value;
  });
};

const fnSiteInfoPopOpen = (siteInfo) => {
  p_siteCd.value = siteInfo.siteCd;

  fnSiteOpenPop(SiteInfoPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: p_siteCd.value,
    onSearch: fnSearch,
    reset: fnCreate,
  });
};

const fnCreate = () => {
  fnSiteOpenPop(SiteInfoPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
  });
};

const fnSiteOpenPop = (popId, param) => {
  openPop(popId, param);
};
</script>

<style scoped></style>
