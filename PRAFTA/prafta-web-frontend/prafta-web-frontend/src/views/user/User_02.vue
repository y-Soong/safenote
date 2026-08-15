<template>
  <div class="viewComm">
    <ViewHeader
      class="commViewHeader"
      :title="props.title"
      :buttons="localButtons"
      @Search="fnSearch"
      @save="fnSave"
    />

    <!--
    @search="fnSubSearch"
    @create="fnCreate"
    -->

    <!-- 검색 영역 -->
    <div class="viewSearch">
      <div>
        <label>권한명</label>
        <input v-model.trim="authNm" type="text" />
      </div>
    </div>

    <!-- ✅ 테이블 2개 나란히 -->
    <div class="viewBody tables-row">
      <!-- LEFT TABLE -->
      <div class="table-wrapper subtitle-pane" style="flex: 0 0 13%">
        <!-- ⬇️ 소제목 바 -->
        <div class="subtitle">
          <span class="subtitle-icon" aria-hidden="true">
            <!-- 단순 마크 아이콘 (SVG) -->
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M4 4h16v4H4zM4 10h10v10H4z" />
            </svg>
          </span>
          <span class="subtitle-text">권한 리스트</span>
        </div>

        <div
          class="table-box"
          style="--box-h: 65vh; --box-sticky-top: 1px; --box-ox: auto"
        >
          <table class="data-grid w-full table-fixed border-collapse text-sm">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <ThSortable
                  label="권한명"
                  col-key="baimValDNm"
                  :sort-key="authSortKey"
                  :sort-order="authSortOrder"
                  :width="authColWidths.baimValDNm"
                  @sort="authOnSort"
                  @update:width="authOnResize"
                />
              </tr>
            </thead>
            <tbody>
              <tr v-for="(auth, idx) in authSortedData" :key="auth.baimValCd">
                <td style="text-align: center">{{ idx + 1 }}</td>
                <td @dblclick="fnSubSearch(auth)">{{ auth.baimValDNm }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- RIGHT TABLE -->
      <div class="table-wrapper subtitle-pane" style="flex: 1">
        <div class="subtitle-row">
          <div class="subtitle">
            <span class="subtitle-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="18" height="18">
                <path d="M4 4h16v4H4zM4 10h10v10H4z" />
              </svg>
            </span>
            <span class="subtitle-text">메뉴 리스트</span>
          </div>
          <div
            v-if="authMenuList.length > 0 && !isCheckboxColumnHidden"
            class="custom-btn-area menu-batch-control"
          >
            <template v-for="item in batchControlItems" :key="item.field">
              <span class="menu-batch-group">
                <span class="menu-batch-field">{{ item.label }}</span>
                <button
                  type="button"
                  class="btn btn-sm btn-custom"
                  :disabled="checkedCount === 0"
                  @click="fnSetFieldForChecked(item.field, 'Y')"
                >
                  사용
                </button>
                <button
                  type="button"
                  class="btn btn-sm btn-custom"
                  :disabled="checkedCount === 0"
                  @click="fnSetFieldForChecked(item.field, 'N')"
                >
                  미사용
                </button>
              </span>
            </template>
            <span class="menu-batch-divider" />
            <button
              type="button"
              class="btn btn-sm btn-primary"
              :disabled="checkedCount === 0"
              @click="fnSetAllForChecked('Y')"
            >
              일괄 사용
            </button>
            <button
              type="button"
              class="btn btn-sm btn-custom"
              :disabled="checkedCount === 0"
              @click="fnSetAllForChecked('N')"
            >
              일괄 미사용
            </button>
          </div>
        </div>

        <!-- 배지 범례 — 대상 화면이 있을 때만 노출.
             ★제목 옆이 아니라 별도 줄에 둔다. .subtitle-row 는 space-between + nowrap 이라
             긴 문구를 끼우면 우측 일괄제어 버튼들을 밀어낸다. -->
        <p v-if="hasAccessNote" class="menu-note-legend">
          <svg
            width="13"
            height="13"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
          <span>
            배지가 있는 화면은 <b>권한 부여만으로는 이용할 수 없습니다.</b>
            배지에 마우스를 올리면 필요한 조건을 볼 수 있습니다.
          </span>
        </p>

        <div
          class="table-box"
          style="
            --box-h: 65vh;
            --box-sticky-top: 1px;
            --box-ox: auto;
            width: 100%;
          "
        >
          <table class="data-grid w-full table-fixed border-collapse text-sm">
            <thead>
              <tr>
                <th class="event_cell" style="text-align: center; width: 2%">
                  No
                </th>
                <th v-if="!isCheckboxColumnHidden" style="width: 4%">
                  <input
                    id="headChk"
                    v-model="headChk"
                    type="checkbox"
                    @click="fnHeadChk"
                  />
                </th>
                <ThSortable
                  label="대메뉴명"
                  col-key="menuMNm"
                  :sort-key="menuSortKey"
                  :sort-order="menuSortOrder"
                  :width="menuColWidths.menuMNm"
                  @sort="menuOnSort"
                  @update:width="menuOnResize"
                />
                <ThSortable
                  label="소메뉴명"
                  col-key="menuDNm"
                  :sort-key="menuSortKey"
                  :sort-order="menuSortOrder"
                  :width="menuColWidths.menuDNm"
                  @sort="menuOnSort"
                  @update:width="menuOnResize"
                />
                <th class="editableCell" style="width: 8%">사용여부</th>
                <th style="width: 10%">메뉴사용처</th>
                <th class="editableCell" style="width: 10%">조회</th>
                <th class="editableCell" style="width: 10%">생성</th>
                <th class="editableCell" style="width: 10%">삭제</th>
                <th class="editableCell" style="width: 10%">저장</th>
                <th class="editableCell" style="width: 10%">엑셀</th>
              </tr>
            </thead>
            <tbody>
              <template v-if="!authMenuList || authMenuList.length === 0">
                <tr>
                  <td
                    :colspan="isCheckboxColumnHidden ? 10 : 11"
                    class="edu-grid-empty"
                  >
                    등록된 세부 항목이 없습니다.
                  </td>
                </tr>
              </template>
              <template v-else>
                <tr v-for="(menu, idx) in menuSortedData" :key="menu.id">
                  <td style="text-align: center">{{ idx + 1 }}</td>
                  <td v-if="!isCheckboxColumnHidden">
                    <input
                      v-if="menu.menuSrc === '001'"
                      type="checkbox"
                      v-model="menu.chk"
                      :disabled="isRowCheckboxDisabled(menu)"
                    />
                  </td>
                  <td>{{ menu.menuMNm }}</td>
                  <td>
                    <!-- 화면명 + 추가 조건 배지. 컬럼 폭이 좁아지면 배지가 아랫줄로 내려가고
                         화면명은 잘리지 않는다(리사이즈 가능한 표라 wrap 허용). -->
                    <div class="menu-name-cell">
                      <span class="menu-name-text">{{ menu.menuDNm }}</span>
                      <span
                        v-if="accessNoteOf(menu)"
                        class="menu-note-badge"
                        :title="accessNoteOf(menu).detail"
                        tabindex="0"
                        role="note"
                        :aria-label="`추가 조건: ${accessNoteOf(menu).detail}`"
                      >
                        <svg
                          width="12"
                          height="12"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          aria-hidden="true"
                        >
                          <circle cx="12" cy="12" r="10" />
                          <line x1="12" y1="16" x2="12" y2="12" />
                          <line x1="12" y1="8" x2="12.01" y2="8" />
                        </svg>
                        {{ accessNoteOf(menu).badge }}
                      </span>
                    </div>
                  </td>
                  <td>
                    <BaseSelect v-model="menu.useYn" name="useYn">
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
                    <BaseSelect
                      v-model="menu.menuSrc"
                      :readonly="true"
                      name="menuSrc"
                    >
                      <option
                        v-for="opt in (systCodeArr['SYS007'] || []).filter(
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
                    <BaseSelect v-model="menu.btnSrch" name="btnSrch">
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
                    <BaseSelect v-model="menu.btnNew" name="btnNew">
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
                    <BaseSelect v-model="menu.btnDel">
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
                    <BaseSelect v-model="menu.btnSave">
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
                    <BaseSelect v-model="menu.btnExcl">
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
  computed,
  defineProps,
  onMounted,
  getCurrentInstance,
  defineOptions,
} from "vue";
import { useFieldWatcher } from "@/utils/useFieldWatcher";
import axios from "@/api/axios";
import ViewHeader from "@/components/common/ViewHeader.vue";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import BaseSelect from "@/components/common/BaseSelect.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import { useTableSort, useColumnResize } from "@/composables/useTableFeatures.js";

// =========================== Define ===========================
defineOptions({ name: "User_02" });
const props = defineProps({
  title: String,
  buttons: Object,
});

// =========================== Ref ===========================
const localButtons = ref({ ...props.buttons });
const authList = ref([]);
const authMenuList = ref([]);

// 화면권한 부여만으로는 이용할 수 없는 화면의 추가 조건 안내(표시 전용, 서버 SSOT).
//   { "attd_05": { badge, detail }, ... } — 키는 소문자 MENU_D_ID.
//   ⚠️ 인가와 무관하다. 실제 접근 제어는 각 화면의 서버 게이트가 한다.
const menuNotes = ref({});

/** 해당 메뉴 행의 추가 조건 안내(없으면 null). */
const accessNoteOf = (menu) => {
  const id = (menu?.menuDId || "").toLowerCase();
  return id ? menuNotes.value[id] || null : null;
};

/** 현재 목록에 안내 대상 화면이 하나라도 있는지 — 범례 노출 조건. */
const hasAccessNote = computed(() =>
  (authMenuList.value || []).some((m) => accessNoteOf(m) !== null)
);
const { sortKey: authSortKey, sortOrder: authSortOrder, sortedData: authSortedData, onSort: authOnSort } = useTableSort(authList);
const { colWidths: authColWidths, onResize: authOnResize } = useColumnResize({ baimValDNm: 160 });
const { sortKey: menuSortKey, sortOrder: menuSortOrder, sortedData: menuSortedData, onSort: menuOnSort } = useTableSort(authMenuList);
const { colWidths: menuColWidths, onResize: menuOnResize } = useColumnResize({ menuMNm: 160, menuDNm: 160 });
const systCodeArr = ref({});
const baseInfoArr = ref([]);
const authNm = ref("");
const authCd = ref("");
const headChk = ref(false);
const selectedAuth = ref(null); // fnSubSearch 시 선택된 권한 (master/hr/safe 체크용)

// =========================== Computed ===========================
const batchControlItems = [
  { field: "useYn", label: "사용여부" },
  { field: "btnSrch", label: "조회" },
  { field: "btnNew", label: "생성" },
  { field: "btnDel", label: "삭제" },
  { field: "btnSave", label: "저장" },
  { field: "btnExcl", label: "엑셀" },
];
const checkedCount = computed(
  () =>
    authMenuList.value.filter(
      (m) => m.chk === true && !isRowCheckboxDisabled(m)
    ).length
);

/** menuMId 추출 (예: Baim_01 -> baim, chkLst_02 -> chklst) */
const getMenuModuleId = (menu) => {
  const raw = menu.menuMId ?? menu.menuId ?? menu.menuCd ?? menu.path ?? "";
  const s = String(raw).toLowerCase();
  const beforeUnderscore = s.split("_")[0] || s;
  return beforeUnderscore;
};

/** master: 체크박스 숨김, hr: baim/attd/user 비활성화, safe: baim/user/risk/tbm/chkLst 비활성화 */
const isRowCheckboxDisabled = (menu) => {
  const baimValDCd =
    selectedAuth.value?.baimValDCd ?? selectedAuth.value?.baimValDCd ?? "";

  if (baimValDCd === "master") return true;
  const moduleId = getMenuModuleId(menu);
  if (baimValDCd === "hr") {
    return ["baim", "attd", "user"].includes(moduleId);
  }
  if (baimValDCd === "safe") {
    return ["baim", "user", "risk", "tbm", "chklst", "nearmiss"].includes(moduleId);
  }
  return false;
};

/** master일 때 체크박스 열 숨김 여부 */
const isCheckboxColumnHidden = computed(
  () =>
    (selectedAuth.value?.baimValDCd ?? selectedAuth.value?.baimValDCd ?? "") ===
    "master"
);

// =========================== Data ===========================
const { proxy } = getCurrentInstance();

// =========================== Life Cycle ===========================
onMounted(async () => {
  fnButtonControll();
  await fnGetSystinfoList();
  await fnSearch();
});

// =========================== Watch, Watcher ===========================
useFieldWatcher(
  authMenuList,
  (item) => {
    item.chk = true;
  },
  ["chk"]
);

// =========================== Methods ===========================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS003", "SYS007"],
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
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  authList.value = [];
  authMenuList.value = [];

  try {
    const response = await axios.get("/comApi/baseinfo/base-infos", {
      params: {
        code: "COM005",
        nameD: authNm.value,
      },
    });

    if (response.status === 200) {
      console.log(response.data);
      authList.value = response.data.baseInfoList;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSubSearch = async (auth) => {
  authMenuList.value = [];
  selectedAuth.value = auth ?? null;

  if (proxy.$util.isNotEmpty(auth)) {
    authCd.value = auth.baimValDCd;
  }

  try {
    const response = await axios.get("/webApi/user02/auth-menu-lists", {
      params: {
        authCd: authCd.value,
      },
    });

    if (response.status === 200) {
      authMenuList.value = response.data.authMenuList;
      // 화면별 추가 조건 안내(표시 전용). 서버가 소문자 MENU_D_ID 로 키를 준다.
      menuNotes.value = response.data.menuNotes || {};
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSave = async () => {
  const filteredMenu = authMenuList.value.filter(
    (menu) => menu.chk && !isRowCheckboxDisabled(menu)
  );

  for (var i = 0; i < filteredMenu.length; i++) {
    filteredMenu[i].authCd = authCd.value;
  }

  if (filteredMenu.length == 0) {
    proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/user02/update-auth-menu-infos",
      filteredMenu
    );

    if (response.status === 200) {
      proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      fnSubSearch(selectedAuth.value);
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnButtonControll = () => {
  // localButtons.value.search = "N";
  localButtons.value.create = "N";
  // localButtons.value.save = "N";
  localButtons.value.delete = "N";
  localButtons.value.excel = "N";
};

const fnHeadChk = () => {
  headChk.value = !headChk.value;
  authMenuList.value.forEach((item) => {
    if (!isRowCheckboxDisabled(item)) {
      item.chk = headChk.value;
    }
  });
};

/** chk=true 항목에 대해 지정 필드를 value로 설정 */
const fnSetFieldForChecked = (field, value) => {
  authMenuList.value
    .filter((m) => m.chk === true)
    .forEach((m) => {
      m[field] = value;
    });
};

/** chk=true 항목에 대해 사용여부·조회·생성·삭제·저장·엑셀 전부 value로 설정 */
const fnSetAllForChecked = (value) => {
  const fields = ["useYn", "btnSrch", "btnNew", "btnDel", "btnSave", "btnExcl"];
  authMenuList.value
    .filter((m) => m.chk === true)
    .forEach((m) => {
      fields.forEach((f) => {
        m[f] = value;
      });
    });
};
</script>

<style scoped>
/* custom-btn-area 내부: subtitle-row 우측에 Baim_06 방식 배치 */
.menu-batch-control {
  flex-wrap: wrap;
}
.menu-batch-group {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
}
.menu-batch-field {
  font-size: 11px;
  color: var(--color-text-muted, #6b7280);
  margin-right: 0.1rem;
}
.menu-batch-divider {
  width: 1px;
  height: 1rem;
  background: var(--color-border, #d1d5db);
  margin: 0 0.25rem;
}
.menu-batch-control .btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ── 화면 추가 조건 안내 (2026-08-16) ──────────────────────────────────────
   오류가 아니라 정보다. 경고색(빨강/주황)을 쓰지 않고 중립적인 파랑 계열로 둔다.
   관리자가 "뭔가 잘못됐나?" 로 읽지 않게 하는 것이 요점. */

/* 화면명 셀 — 배지가 붙어도 화면명이 잘리지 않도록 wrap 허용.
   표 컬럼이 리사이즈 가능하므로 폭이 좁아지면 배지가 아랫줄로 내려간다. */
.menu-name-cell {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.25rem 0.35rem;
  min-width: 0;
}
.menu-name-text {
  min-width: 0;
}

.menu-note-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.15rem;
  flex-shrink: 0;
  padding: 0.05rem 0.35rem;
  border-radius: 999px;
  border: 1px solid var(--color-info-border, #bfdbfe);
  background: var(--color-info-bg, #eff6ff);
  color: var(--color-info-text, #1d4ed8);
  font-size: 10.5px;
  font-weight: 500;
  line-height: 1.6;
  white-space: nowrap;
  /* 전문은 title 툴팁으로만 노출한다 — 표 안에 다 적으면 행이 뭉개진다. */
  cursor: help;
}
/* 키보드 사용자도 툴팁 대상임을 알 수 있게 포커스 링을 준다(tabindex="0"). */
.menu-note-badge:focus-visible {
  outline: none;
  box-shadow: 0 0 0 var(--focus-ring-width, 3px) var(--color-focus-ring);
}

/* 범례 — 표 바로 위 한 줄. 배지와 같은 파랑 아이콘을 써서 같은 것을 가리킴을 보인다. */
.menu-note-legend {
  display: flex;
  align-items: flex-start;
  gap: 0.3rem;
  margin: 0 0 0.35rem;
  font-size: 11px;
  line-height: 1.5;
  color: var(--color-text-muted, #6b7280);
}
.menu-note-legend svg {
  flex-shrink: 0;
  margin-top: 0.1rem;
  color: var(--color-info-text, #1d4ed8);
}
.menu-note-legend b {
  font-weight: 600;
  color: var(--color-text-strong, #374151);
}
</style>
