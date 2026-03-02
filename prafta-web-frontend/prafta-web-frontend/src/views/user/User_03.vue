<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @search="fnSearch"
      @save="fnSave"
    />

    <!-- 검색 영역 -->
    <div class="viewSearch">
      <div>
        <label>사용자ID</label>
        <input v-model.trim="userId" type="text" />
      </div>
      <div>
        <label>사용자명</label>
        <input v-model.trim="userNm" type="text" />
      </div>
    </div>

    <!-- ✅ 테이블 2개 나란히 -->
    <div class="viewBody tables-row">
      <!-- LEFT TABLE -->
      <div class="table-wrapper subtitle-pane" style="flex: 0 0 25%">
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
          class="table-box"
          style="--box-h: 70vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid w-full table-fixed border-collapse text-sm">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <th style="width: 25%">사용자ID</th>
                <th style="width: 25%">사용자명</th>
                <th>전화번호</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(user, idx) in userActList" :key="user.id">
                <td style="text-align: center">{{ idx + 1 }}</td>
                <td @dblclick="fnSiteInfoSearch(user)">{{ user.userId }}</td>
                <td @dblclick="fnSiteInfoSearch(user)">{{ user.userNm }}</td>
                <td @dblclick="fnSiteInfoSearch(user)">
                  {{ proxy.$util.formatPhoneNumber(user.mblNo) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- RIGHT TABLE -->
      <div class="table-wrapper subtitle-pane" style="flex: 1">
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
          ref="topBox"
          style="--box-h: 32vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid w-full table-fixed border-collapse text-sm">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <th style="width: 4%">
                  <input
                    id="headChkMain"
                    v-model="headChkMain"
                    type="checkbox"
                    @click="fnHeadChk('headChkMain')"
                  />
                </th>
                <th style="width: 15%">사업장코드</th>
                <th style="width: 15%">사업장명</th>
                <th>주소</th>
                <th style="width: 20%">전화번호</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!siteUnallocList || siteUnallocList.length === 0">
                <tr>
                  <td colspan="6" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(site, idx) in siteUnallocList" :key="site.siteCd">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input type="checkbox" v-model="site.chk" />
                  </td>
                  <td>{{ site.siteNo }}</td>
                  <td>{{ site.siteNm }}</td>
                  <td>{{ site.addr }}</td>
                  <td>{{ proxy.$util.formatPhoneNumber(site.telNo) }}</td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <!-- ▲▼ 화살표 바 -->
        <div class="section-arrows" role="group" aria-label="섹션 이동">
          <button
            class="arrow-btn"
            aria-label="위로 이동"
            @click="fnScrollTo('top')"
          >
            <!-- ▲ -->
            <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
              <polygon points="12,6 4,18 20,18" />
            </svg>
          </button>

          <button
            class="arrow-btn"
            aria-label="아래로 이동"
            @click="fnScrollTo('bottom')"
          >
            <!-- ▼ -->
            <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
              <polygon points="4,6 20,6 12,18" />
            </svg>
          </button>
        </div>

        <!-- ⬇️ 소제목 바 -->
        <div class="subtitle" style="margin-top: 10px">
          <span class="subtitle-icon" aria-hidden="true">
            <!-- 단순 마크 아이콘 (SVG) -->
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">권한을 갖고있는 사업장</span>
        </div>

        <div
          class="table-box"
          ref="bottomBox"
          style="--box-h: 30vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <th style="width: 4%">
                  <input
                    id="headChkSub"
                    v-model="headChkSub"
                    type="checkbox"
                    @click="fnHeadChk('headChkSub')"
                  />
                </th>
                <th style="width: 15%">사업장코드</th>
                <th style="width: 15%">사업장명</th>
                <th>주소</th>
                <th style="width: 20%">전화번호</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!siteAllocList || siteAllocList.length === 0">
                <tr>
                  <td colspan="6" class="edu-grid-empty">
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(site, idx) in siteAllocList" :key="site.id">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td>
                    <input
                      type="checkbox"
                      v-model="site.chk"
                      :disabled="isLocked(site)"
                    />
                  </td>
                  <td>{{ site.siteNo }}</td>
                  <td>{{ site.siteNm }}</td>
                  <td>{{ site.addr }}</td>
                  <td>{{ site.telNo }}</td>
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
/* eslint-disable */
import {
  ref,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from 'vue';
import axios from '@/api/axios';
import ViewHeader from '@/components/common/ViewHeader.vue';

// =========================== Define ===========================
defineOptions({ name: 'User_03' });
const props = defineProps({
  title: String,
  buttons: Object,
});

// =========================== Ref ===========================
const userActList = ref([]);
const siteInfoList = ref([]);
const siteUnallocList = ref([]);
const siteAllocList = ref([]);
const systCodeArr = ref({});
const localButtons = ref({ ...props.buttons });
const userId = ref('');
const userNm = ref('');
const topBox = ref(null);
const bottomBox = ref(null);
const headChkMain = ref(false);
const headChkSub = ref(false);
const currentUserSiteCd = ref('');
const currentUserId = ref('');

// =========================== Data ===========================
const { proxy } = getCurrentInstance();

// =========================== Life Cycle ===========================
onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
  await fnSearch();
});

// =========================== Methods ===========================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-list", {
      params: {
        systCodeList: ["SYS002", "SYS003", "SYS007"],
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
  try {
    const response = await axios.get("/webApi/user01/user-info-lists", {
      params: {
        userId: userId.value,
        userNm: userNm.value,
      },
    });

    if (response.status === 200) {
      userActList.value = response.data.userInfoList;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnSiteInfoSearch = async (user) => {
  currentUserSiteCd.value = user.siteCd;
  currentUserId.value = user.userId;

  fnSiteInfoSearchTran();
};

const fnSiteInfoSearchTran = async () => {
  try {
    const response = await axios.get("/webApi/user03/site-info-lists", {
      params: {
        userId: currentUserId.value,
      },
    });

    if (response.status === 200) {
      siteInfoList.value = response.data.siteInfoList;
      const norm = (v) =>
        String(v ?? "")
          .trim()
          .toUpperCase();
      const split = siteInfoList.value.reduce(
        (acc, row) => {
          (norm(row.allocYn) === "Y" ? acc.alloc : acc.unalloc).push(row);
          return acc;
        },
        { alloc: [], unalloc: [] }
      );

      siteAllocList.value = split.alloc;
      siteUnallocList.value = split.unalloc;
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnSave = async (dataList) => {
  if (dataList.length == 0) {
    proxy.$alert("저장할 데이터가 없습니다.");
    return;
  }

  const ok = await proxy.$confirm("저장하시겠습니까 ?");
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/user03/updateUserSiteAuth",
      dataList
    );

    if (response.status === 200) {
      proxy.$alert("처리되었습니다.");
      fnSiteInfoSearchTran();
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "저장 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

function fnButtonControll() {
  // localButtons.search = 'N';
  localButtons.value.create = "N";
  // localButtons.save = 'N';
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
}

function fnScrollTo(id) {
  const el = id === "top" ? topBox.value : bottomBox.value;
  if (!el) return;
  // 섹션이 화면 중앙 근처로 오도록 스크롤
  el.scrollIntoView({ behavior: "smooth", block: "center" });

  // 1) 대상 리스트 선택
  const src = id === "top" ? siteAllocList.value : siteUnallocList.value;

  // 2) 체크된 행만 추출 (CHK가 boolean일 수도, 'Y'/'N'일 수도 있으니 둘 다 처리)
  const filterdDataList = (Array.isArray(src) ? src : []).filter(
    (s) => s.chk === true || s.chk === "Y"
  );

  // 3) 값 토글 및 추가 세팅
  for (const row of filterdDataList) {
    row.allocYn = row.allocYn === "Y" ? "N" : "Y";
    row.userId = currentUserId.value;
  }

  // const dataList = proxy.$util.toCamelCaseKeys(filterdDataList);

  fnSave(filterdDataList);
}

function fnHeadChk(targetId) {
  if (targetId == "headChkMain") {
    headChkMain.value = !headChkMain.value;
    siteUnallocList.value.forEach((item) => {
      if (!isLocked(item)) {
        item.chk = headChkMain.value;
      }
    });
  } else if (targetId == "headChkSub") {
    headChkSub.value = !headChkSub.value;
    siteAllocList.value.forEach((item) => {
      if (!isLocked(item)) {
        item.chk = headChkSub.value;
      }
    });
  }
}

function isLocked(site) {
  return String(site?.siteCd ?? "").trim() === currentUserSiteCd.value;
}
</script>

<style scoped>
/* ▼▼ 섹션 사이 화살표 바 (PRAFTA UI: 중립 베이스) ▼▼ */
.section-arrows {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 28px;
  border-radius: var(--input-radius, 10px);
  background: var(--card-bg, #ffffff);
}

.section-arrows .arrow-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 50%;
  height: 24px;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 6px;
  background: var(--color-bg, #f9fafb);
  cursor: pointer;
  transition: transform 0.12s ease, background-color 0.12s ease,
    box-shadow 0.12s ease;
}

.section-arrows .arrow-btn:hover {
  background: var(--color-border-strong, #d1d5db);
  transform: translateY(-1px);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.section-arrows .arrow-btn:active {
  transform: translateY(0);
  box-shadow: none;
}

.section-arrows .arrow-btn svg {
  display: block;
}
</style>
