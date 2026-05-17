<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide modal-content-excel-upload"
        ref="modalRef"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
      >
        <!-- 헤더 -->
        <div class="modal-header" @mousedown="startDrag">
          <span>엑셀 업로드</span>
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

        <!-- 조회 조건 -->
        <div class="popup-search-bar">
          <!-- Row 1: 조건 필드 + 조회 버튼 -->
          <div class="popup-search-row">
            <div class="popup-search-item">
              <label>사업장</label>
              <input
                id="siteNo"
                type="text"
                v-model="siteNo"
                placeholder="사업장코드"
                :disabled="siteDisabled"
                class="input-short"
                @blur="focusKill"
              />
              <button
                class="search-btn"
                :disabled="siteDisabled"
                @click="fnSiteSearchPopOpen"
              >
                <img class="search_icon" :src="search_icon" alt="검색" />
              </button>
              <input
                id="siteNm"
                type="text"
                v-model="siteNm"
                placeholder="사업장명"
                :disabled="siteDisabled"
                class="input-short"
                @blur="focusKill"
              />
            </div>
            <div class="popup-search-item">
              <label>소속부서</label>
              <input
                id="nodeCd"
                type="text"
                v-model="nodeCd"
                placeholder="부서코드"
                :disabled="nodeDisabled"
                class="input-short"
                @blur="focusKill"
              />
              <button
                class="search-btn"
                :disabled="nodeDisabled"
                @click="fnNodeSearchPopOpen"
              >
                <img class="search_icon" :src="search_icon" alt="검색" />
              </button>
              <input
                id="nodeNm"
                type="text"
                v-model="nodeNm"
                placeholder="부서명"
                :disabled="nodeDisabled"
                class="input-short"
                @blur="focusKill"
              />
            </div>
            <div class="popup-search-item">
              <label class="popup-checkbox-label">
                <input type="checkbox" v-model="incSubNodeYn" />
                하위부서 조회
              </label>
            </div>
            <div class="popup-search-item">
              <label>조회월</label>
              <CalendarSrchMonth
                :range="false"
                style="width: 100px"
                v-model="workYm"
              />
            </div>
            <button class="btn btn-primary btn-sm" @click="fnSearch">
              조회
            </button>

            <!-- Row 2: 액션 버튼 (우측 정렬) -->
            <div class="popup-action-row">
              <button class="btn btn-custom" @click="fnDownloadTemplate">
                양식 다운로드
              </button>
              <button class="btn btn-second" @click="fnUploadExcelFile">
                엑셀 업로드
              </button>
            </div>
          </div>
        </div>

        <!-- 테이블 영역 -->
        <div class="modal-body popup-table-body">
          <div v-if="loading" class="popup-status">조회 중...</div>
          <div v-else-if="userList.length === 0" class="popup-status">
            조회 결과가 없습니다.
          </div>
          <div v-else class="popup-table-outer">
            <table class="popup-table" @selectstart.prevent>
              <thead>
                <tr>
                  <th class="th-seq sticky-col-seq sticky-top">순번</th>
                  <th class="th-chk sticky-col-chk sticky-top">
                    <input type="checkbox" v-model="allChecked" />
                  </th>
                  <ThSortable
                    label="사용자ID"
                    col-key="userId"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.userId"
                    class="th-uid sticky-col-uid sticky-top"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <ThSortable
                    label="사용자명"
                    col-key="userNm"
                    :sort-key="sortKey"
                    :sort-order="sortOrder"
                    :width="colWidths.userNm"
                    class="th-unm sticky-col-unm sticky-top"
                    @sort="onSort"
                    @update:width="onResize"
                  />
                  <th
                    v-for="d in daysInMonth"
                    :key="d.workYmd"
                    class="th-day sticky-top"
                    :class="{
                      'head-sun': d.dow === '일',
                      'head-sat': d.dow === '토',
                      'head-holiday':
                        d.holidayYn !== 'N' && d.dow !== '일' && d.dow !== '토',
                    }"
                  >
                    {{ parseInt(d.workYmd.slice(6)) }}({{ d.dow }})
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(user, rowIdx) in sortedData" :key="user.userCd">
                  <td class="td-seq sticky-col-seq">{{ rowIdx + 1 }}</td>
                  <td class="td-chk sticky-col-chk">
                    <input
                      type="checkbox"
                      v-model="checkedRows"
                      :value="user.userCd"
                    />
                  </td>
                  <td class="td-uid sticky-col-uid">{{ user.userId }}</td>
                  <td class="td-unm sticky-col-unm">{{ user.userNm }}</td>
                  <td
                    v-for="d in daysInMonth"
                    :key="d.workYmd"
                    class="td-day"
                    :class="[
                      d.dow === '일' ? 'td-sun' : '',
                      d.dow === '토' ? 'td-sat' : '',
                      d.holidayYn !== 'N' && d.dow !== '일' && d.dow !== '토'
                        ? 'td-holiday'
                        : '',
                    ]"
                  >
                    <span
                      class="td-val"
                      :class="{
                        'val-muted':
                          (d.weekendYn === 'Y' || d.holidayYn === 'Y') &&
                          !getCellNmValue(user.userCd, d.workYmd),
                      }"
                    >
                      {{
                        getCellNmValue(user.userCd, d.workYmd) ||
                        (d.weekendYn === "Y" || d.holidayYn === "Y" ? "-" : "")
                      }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 숨김 파일 입력 (엑셀 업로드용) -->
        <input
          ref="uploadFileRef"
          type="file"
          accept=".xlsx,.xls"
          style="display: none"
          @change="fnProcessExcelFile"
        />

        <!-- 푸터 -->
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-second" @click="$emit('close')">닫기</button>
            <button class="btn btn-primary" @click="fnSave">저장</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import * as XLSX from "xlsx";
import axios from "@/api/axios";
import { useModal } from "@/utils/useModal";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import { exportStyledExcel } from "@/utils/excelExport";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import search_icon from "@/assets/img/search_icon.png";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import SiteNodeSearchPop from "@/components/popup/SiteNodeSearchPop.vue";
import CalendarSrchMonth from "@/components/common/CalendarSrchMonth.vue";
import ThSortable from "@/components/common/ThSortable.vue";
import {
  useTableSort,
  useColumnResize,
} from "@/composables/useTableFeatures.js";

const props = defineProps({
  siteCd_p: { type: String, default: "" },
  siteNo_p: { type: String, default: "" },
  siteNm_p: { type: String, default: "" },
  nodeCd_p: { type: String, default: "" },
  nodeNm_p: { type: String, default: "" },
  incSubNodeYn_p: { type: Boolean, default: false },
  workYm_p: { type: String, default: "" },
  onSaved: { type: Function, default: null },
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();
const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 2,
});

// ── 조회 조건 (편집 가능 로컬 상태) ──────────────────────────
const siteCd = ref(props.siteCd_p);
const siteNo = ref(props.siteNo_p);
const siteNm = ref(props.siteNm_p);
const siteDisabled = ref(false);
const nodeCd = ref(props.nodeCd_p);
const nodeNm = ref(props.nodeNm_p);
const nodeDisabled = computed(() => !siteCd.value);
const incSubNodeYn = ref(props.incSubNodeYn_p);
const workYm = ref(props.workYm_p);

// ── 데이터 ────────────────────────────────────────────────────
const loading = ref(false);
const userList = ref([]);
const { sortKey, sortOrder, sortedData, onSort } = useTableSort(userList);
const { colWidths, onResize } = useColumnResize({ userId: 100, userNm: 100 });
const daysInMonth = ref([]);
const scheduleData = ref({});
const schTypeList = ref([]);
const leaveTypeList = ref([]);

// ── 행 체크박스 ───────────────────────────────────────────────
const checkedRows = ref([]);
const allChecked = computed({
  get: () =>
    userList.value.length > 0 &&
    checkedRows.value.length === userList.value.length,
  set: (val) => {
    checkedRows.value = val ? userList.value.map((u) => u.userCd) : [];
  },
});

// ── 근무타입 표시명 조회 (schCd → 근무타입명) ────────────────
const getSchTypeNm = (schCd) => {
  const sch = schTypeList.value.find((s) => s.schCd === schCd);
  return sch ? sch.schNm : schCd;
};

// ── 셀 값 조회 (코드 → 표시명) ───────────────────────────────
const getCellNmValue = (userCd, workYmd) => {
  const code = scheduleData.value[`${userCd}_${workYmd}`];
  if (!code) return "";
  const sch = schTypeList.value.find((s) => s.schCd === code);
  if (sch) return sch.schNm;
  const leave = leaveTypeList.value.find((l) => l.leaveCd === code);
  if (leave) return leave.leaveNm;
  return code;
};

// ── 사업장 조회 ───────────────────────────────────────────────
const fnResetTableData = () => {
  userList.value = [];
  daysInMonth.value = [];
  scheduleData.value = {};
  checkedRows.value = [];
};

const fnSrchSiteInfo = async () => {
  fnResetTableData();
  try {
    const res = await axios.get("/comApi/baseinfo/site-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteNo: siteNo.value,
        siteNm: siteNm.value,
      },
    });
    const list = res.data?.siteInfoResultList ?? [];
    if (list.length === 1) {
      siteCd.value = list[0].siteCd;
      siteNo.value = list[0].siteNo;
      siteNm.value = list[0].siteNm;
      nodeCd.value = "";
      nodeNm.value = "";
    } else if (list.length > 1) {
      fnSiteSearchPopOpen();
    } else {
      siteCd.value = "";
      siteNo.value = "";
      siteNm.value = "";
      nodeCd.value = "";
      nodeNm.value = "";
    }
  } catch (err) {
    await proxy.$alert(resolveApiErrorMessage(err, "조회 오류"));
  }
};

// ── 소속부서 조회 ─────────────────────────────────────────────
const fnSrchNodeInfo = async () => {
  if (!siteCd.value) return;
  try {
    const res = await axios.get("/comApi/baseinfo/site-node-lists", {
      params: {
        cmpnyCd: sessionStorage.getItem("gv_cmpnyCd"),
        siteCd: siteCd.value,
        nodeCd: nodeCd.value,
        nodeNm: nodeNm.value,
      },
    });
    const list = res.data?.siteNodeInfoList ?? [];
    if (list.length === 1) {
      nodeCd.value = list[0].nodeCd ?? "";
      nodeNm.value = list[0].nodeNm ?? "";
    } else if (list.length > 1) {
      fnNodeSearchPopOpen();
    } else {
      nodeCd.value = "";
      nodeNm.value = "";
    }
  } catch (err) {
    await proxy.$alert(resolveApiErrorMessage(err, "조회 오류"));
  }
};

// ── focusKill ─────────────────────────────────────────────────
const focusKill = (e) => {
  if (e.target.id === "siteNo") {
    if (proxy.$util.isEmpty(siteNo.value)) {
      siteCd.value = "";
      siteNm.value = "";
      nodeCd.value = "";
      nodeNm.value = "";
      fnResetTableData();
    } else {
      siteNm.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "siteNm") {
    if (proxy.$util.isEmpty(siteNm.value)) {
      siteCd.value = "";
      siteNo.value = "";
      nodeCd.value = "";
      nodeNm.value = "";
      fnResetTableData();
    } else {
      siteNo.value = "";
      fnSrchSiteInfo();
    }
  } else if (e.target.id === "nodeCd") {
    if (proxy.$util.isEmpty(nodeCd.value)) {
      nodeNm.value = "";
    } else {
      nodeNm.value = "";
      fnSrchNodeInfo();
    }
  } else if (e.target.id === "nodeNm") {
    if (proxy.$util.isEmpty(nodeNm.value)) {
      nodeCd.value = "";
    } else {
      nodeCd.value = "";
      fnSrchNodeInfo();
    }
  }
};

// ── 사업장 팝업 ───────────────────────────────────────────────
const fnSiteSearchPopOpen = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteNo_p: "",
    siteNm_p: "",
    onSelect: (siteCdVal, siteNoVal, siteNmVal) => {
      siteCd.value = siteCdVal;
      siteNo.value = siteNoVal;
      siteNm.value = siteNmVal;
      nodeCd.value = "";
      nodeNm.value = "";
      userList.value = [];
      daysInMonth.value = [];
      scheduleData.value = {};
      checkedRows.value = [];
    },
  });
};

// ── 소속부서 팝업 ─────────────────────────────────────────────
const fnNodeSearchPopOpen = () => {
  if (!siteCd.value) {
    proxy.$alert(getMessage(MSG.SITE_REQUIRED));
    return;
  }
  openPop(SiteNodeSearchPop, {
    cmpnyCd_p: sessionStorage.getItem("gv_cmpnyCd"),
    siteCd_p: siteCd.value,
    nodeCd_p: "",
    userCd_p: "",
    onSelect: (nodeCdVal, nodeNmVal) => {
      nodeCd.value = nodeCdVal ?? "";
      nodeNm.value = nodeNmVal ?? "";
    },
  });
};

// ── 조회 ──────────────────────────────────────────────────────
const fnSearch = async () => {
  if (!siteCd.value) {
    await proxy.$alert(
      getMessage(MSG.REQUIRED_FIELD_MISSING, { fieldLabel: "사업장" })
    );
    return;
  }
  if (!nodeCd.value) {
    await proxy.$alert(
      getMessage(MSG.REQUIRED_FIELD_MISSING, { fieldLabel: "소속부서" })
    );
    return;
  }

  loading.value = true;
  userList.value = [];
  scheduleData.value = {};
  daysInMonth.value = [];
  checkedRows.value = [];

  try {
    const [mainRes, schRes, leaveRes] = await Promise.all([
      axios.get("/webApi/attd05/user-work-plans", {
        params: {
          workYm: workYm.value,
          siteCd: siteCd.value,
          nodeCd: nodeCd.value,
          incSubNodeYn: incSubNodeYn.value ? "Y" : "N",
          userNm: "",
        },
      }),
      axios.get("/webApi/attd05/sch-type-lists", {
        params: { siteCd: siteCd.value },
      }),
      axios.get("/webApi/attd05/leave-type-lists", {}),
    ]);

    schTypeList.value = schRes.data?.schTypeResultList ?? [];
    leaveTypeList.value = leaveRes.data?.leaveTypeResultList ?? [];

    if (mainRes.status === 200) {
      userList.value = mainRes.data.userListResultList ?? [];
      daysInMonth.value = mainRes.data.dayResultList ?? [];
      // mainRes.data.schedResultList?.forEach((item) => {
      //   scheduleData.value[`${item.userCd}_${item.workYmd}`] = item.workPlanCd;
      // });
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  } finally {
    loading.value = false;
  }
};

// ── 양식 다운로드 ─────────────────────────────────────────────
const fnDownloadTemplate = async () => {
  if (
    userList.value.length === 0 ||
    schTypeList.value.length === 0 ||
    leaveTypeList.value.length === 0
  ) {
    await proxy.$alert("조회 후 다시 시도해주세요.");
    return;
  }

  const dayColumns = daysInMonth.value.map((d) => ({
    header: `${parseInt(d.workYmd.slice(6))}(${d.dow})`,
    fixed: false,
    width: 10,
  }));

  await exportStyledExcel({
    fileName: `스케줄관리_${workYm.value}.xlsx`,
    sheets: [
      {
        name: "스케줄관리",
        columns: [
          { header: "사용자ID", fixed: true, width: 14 },
          { header: "사용자명", fixed: true, width: 14 },
          ...dayColumns,
        ],
        data: userList.value.map((user) => [
          user.userId,
          user.userNm,
          ...daysInMonth.value.map(
            (d) => getCellNmValue(user.userCd, d.workYmd) || ""
          ),
        ]),
      },
      {
        name: "근무타입",
        columns: [
          { header: "근무타입코드", fixed: false, width: 20 },
          { header: "근무타입명", fixed: false, width: 25 },
        ],
        data: schTypeList.value.map((s) => [s.schNo, s.schNm]),
      },
      {
        name: "연차타입",
        columns: [
          { header: "연차타입코드", fixed: false, width: 20 },
          { header: "연차타입명", fixed: false, width: 25 },
        ],
        data: leaveTypeList.value.map((l) => [l.leaveCd, l.leaveNm]),
      },
    ],
  });
};

// ── 엑셀 업로드 ───────────────────────────────────────────────
const uploadFileRef = ref(null);

const fnUploadExcelFile = () => {
  if (userList.value.length === 0) {
    proxy.$alert("조회 후 다시 시도해주세요.");
    return;
  }
  uploadFileRef.value?.click();
};

const fnProcessExcelFile = (event) => {
  const file = event.target.files?.[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = async (e) => {
    try {
      const data = new Uint8Array(e.target.result);
      const wb = XLSX.read(data, { type: "array" });
      const ws = wb.Sheets["스케줄관리"];
      if (!ws) {
        await proxy.$alert("'스케줄관리' 시트를 찾을 수 없습니다.");
        return;
      }

      const rows = XLSX.utils.sheet_to_json(ws, { header: 1, defval: "" });
      if (rows.length < 2) {
        await proxy.$alert("데이터가 없습니다.");
        return;
      }

      const headerRow = rows[0];

      // colIdx → workYmd 매핑 (헤더: "1(일)", "2(월)" 등)
      const dayColMap = {};
      for (let i = 2; i < headerRow.length; i++) {
        const dayNum = parseInt(String(headerRow[i]));
        if (!isNaN(dayNum)) {
          const day = daysInMonth.value.find(
            (d) => parseInt(d.workYmd.slice(6)) === dayNum
          );
          if (day) dayColMap[i] = day.workYmd;
        }
      }

      // userId → userCd 매핑
      const userMap = {};
      userList.value.forEach((u) => {
        userMap[u.userId] = u.userCd;
      });

      let updated = 0;
      const unmatched = [];
      const updatedUserCds = new Set();

      for (let r = 1; r < rows.length; r++) {
        const row = rows[r];
        const userId = String(row[0] ?? "").trim();
        if (!userId) continue;

        const userCd = userMap[userId];
        if (!userCd) {
          unmatched.push(userId);
          continue;
        }

        for (const [colIdxStr, workYmd] of Object.entries(dayColMap)) {
          const colIdx = Number(colIdxStr);
          const rawVal = String(row[colIdx] ?? "").trim();

          if (!rawVal) {
            delete scheduleData.value[`${userCd}_${workYmd}`];
            continue;
          }

          // schNo → schCd 변환, schCd 직접 매칭, leaveNo → leaveCd 변환, leaveCd 직접 매칭
          let resolvedCode;
          const schByNo = schTypeList.value.find(
            (s) => String(s.schNo) === rawVal
          );
          if (schByNo) {
            resolvedCode = schByNo.schCd;
          } else {
            const schByCd = schTypeList.value.find((s) => s.schCd === rawVal);
            if (schByCd) {
              resolvedCode = schByCd.schCd;
            } else {
              const leaveByNo = leaveTypeList.value.find(
                (l) => String(l.leaveNo) === rawVal
              );
              if (leaveByNo) {
                resolvedCode = leaveByNo.leaveCd;
              } else {
                const leaveByCd = leaveTypeList.value.find(
                  (l) => l.leaveCd === rawVal
                );
                resolvedCode = leaveByCd ? leaveByCd.leaveCd : rawVal;
              }
            }
          }

          scheduleData.value[`${userCd}_${workYmd}`] = resolvedCode;
          updatedUserCds.add(userCd);
          updated++;
        }
      }

      // 데이터가 들어간 행은 자동 체크
      if (updatedUserCds.size > 0) {
        const merged = new Set([...checkedRows.value, ...updatedUserCds]);
        checkedRows.value = [...merged];
      }

      let msg = `${updated}개의 셀이 업데이트되었습니다.`;
      if (unmatched.length > 0) {
        msg += `\n매칭 실패 사용자 ID: ${unmatched.join(", ")}`;
      }
      await proxy.$alert(msg);
    } catch (err) {
      await proxy.$alert(
        "엑셀 파일 처리 중 오류가 발생했습니다: " + err.message
      );
    } finally {
      event.target.value = "";
    }
  };
  reader.readAsArrayBuffer(file);
};

// ── 저장 ──────────────────────────────────────────────────────
const fnSave = async () => {
  if (checkedRows.value.length === 0) {
    await proxy.$alert("선택된 항목이 없습니다.");
    return;
  }

  const cmpnyCd = sessionStorage.getItem("gv_cmpnyCd");
  const saveList = Object.entries(scheduleData.value)
    .filter(([key, workPlanCd]) => {
      if (!workPlanCd) return false;
      const userCd = key.substring(0, key.length - 9);
      return checkedRows.value.includes(userCd);
    })
    .map(([key, workPlanCd]) => {
      const workYmd = key.slice(-8);
      const userCd = key.substring(0, key.length - 9);
      return { cmpnyCd, siteCd: siteCd.value, userCd, workYmd, workPlanCd };
    });

  if (saveList.length === 0) {
    await proxy.$alert(getMessage(MSG.SAVE_DATA_REQUIRED));
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/attd05/save-user-work-plans",
      saveList
    );
    if (response.status === 200) {
      const skippedList = response.data?.skippedList ?? [];
      if (skippedList.length > 0) {
        // 서버 검증으로 스킵된 셀 목록을 사용자에게 표시
        const detail = skippedList
          .map((s) => {
            const schNm = getSchTypeNm(s.workPlanCd);
            return `· ${s.workYmd} / ${schNm} : ${s.reason}`;
          })
          .join("\n");
        await proxy.$alert(
          `${response.data.savedCount}건 저장되었습니다.\n` +
            `아래 ${skippedList.length}건은 근무타입 지정이 불가하여 제외되었습니다.\n${detail}`
        );
      } else {
        await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      }
      emit("close");
      props.onSaved?.();
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

onMounted(() => {
  fnSearch();
});
</script>

<style scoped>
/* ── 팝업 크기 / 드래그 ─────────────────────────────────────── */
.modal-content-excel-upload {
  width: min(95vw, 1400px);
  height: min(90vh, 820px);
  position: absolute;
}

.modal-header {
  cursor: move;
}

/* ── 조회 조건 바 ───────────────────────────────────────────── */
.popup-search-bar {
  display: flex;
  flex-direction: column;
  gap: 0;
  border-bottom: 1px solid var(--modal-border, #e5e7eb);
  background: #fafafa;
  font-size: 0.8125rem;
  font-family: "Pretendard", sans-serif;
}

.popup-search-row {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  padding: 0.5rem 1rem;
  flex-wrap: wrap;
}

.popup-action-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0;
  margin-left: auto;
  border-top: 1px solid var(--modal-border, #e5e7eb);
}

.popup-search-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.popup-search-item label {
  font-weight: 600;
  color: #374151;
  white-space: nowrap;
  font-size: 0.8125rem;
}

.input-short {
  height: 26px;
  padding: 0 0.45rem;
  border: 1px solid #d1d5db;
  border-radius: 5px;
  font-size: 0.8125rem;
  background: #fff;
  color: #111827;
  width: 90px;
}

.input-short:disabled {
  background: #f3f4f6;
  color: #6b7280;
  cursor: not-allowed;
}

.popup-checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.8125rem;
  color: #374151;
  cursor: pointer;
  user-select: none;
  font-weight: 600;
}

.popup-checkbox-label input[type="checkbox"] {
  width: 13px;
  height: 13px;
  accent-color: var(--color-primary, #16a34a);
  flex-shrink: 0;
  cursor: pointer;
}

/* ── 테이블 바디 ────────────────────────────────────────────── */
.popup-table-body {
  padding: 0.75rem !important;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.popup-status {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 120px;
  font-size: 0.875rem;
  color: #6b7280;
}

.popup-table-outer {
  flex: 1;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

/* ── 테이블 ─────────────────────────────────────────────────── */
.popup-table {
  border-collapse: separate;
  border-spacing: 0;
  table-layout: auto;
  width: max-content;
  min-width: 100%;
  font-family: "Pretendard", sans-serif;
  font-size: 0.8125rem;
}

/* ── sticky 컬럼 위치 정의 ──────────────────────────────────── */
.sticky-col-seq {
  position: sticky;
  left: 0;
  z-index: 2;
  background: #fff;
}
.sticky-col-chk {
  position: sticky;
  left: 40px;
  z-index: 2;
  background: #fff;
}
.sticky-col-uid {
  position: sticky;
  left: 80px;
  z-index: 2;
  background: #fff;
}
.sticky-col-unm {
  position: sticky;
  left: 190px;
  z-index: 2;
  background: #fff;
}

.sticky-top {
  position: sticky;
  top: 0;
  z-index: 1;
}

/* ── 헤더 기본 ─────────────────────────────────────────────── */
.th-seq,
.th-chk,
.th-uid,
.th-unm,
.th-day {
  background: #f3f4f6;
  border-bottom: 2px solid #d1d5db;
  border-right: 1px solid #e5e7eb;
  padding: 0.45rem 0.4rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #111827;
  white-space: nowrap;
  text-align: center;
}

/* 좌측 sticky 헤더 셀: z-index 최상위 + 배경 명시 */
.th-seq,
.th-chk,
.th-uid,
.th-unm {
  z-index: 4 !important;
  background: #f3f4f6 !important;
}

.th-seq {
  min-width: 40px;
  width: 40px;
}

.th-chk {
  min-width: 40px;
  width: 40px;
}

.th-uid {
  min-width: 110px;
  width: 110px;
  border-right: 2px solid #c7cdd6;
}

.th-unm {
  min-width: 110px;
  width: 110px;
  border-right: 2px solid #c7cdd6;
}

.th-day {
  min-width: 90px;
}

.head-sun {
  color: #ef4444;
}
.head-sat {
  color: #3b82f6;
}
.head-holiday {
  color: #ef4444;
}

/* ── 데이터 셀 ─────────────────────────────────────────────── */
.td-seq {
  min-width: 40px;
  width: 40px;
  border-bottom: 1px solid #e5e7eb;
  border-right: 1px solid #e5e7eb;
  padding: 0.35rem 0.2rem;
  text-align: center;
  font-size: 0.75rem;
  color: #6b7280;
}

.td-chk {
  min-width: 40px;
  width: 40px;
  border-bottom: 1px solid #e5e7eb;
  border-right: 1px solid #e5e7eb;
  padding: 0.35rem 0.2rem;
  text-align: center;
}

.td-chk input[type="checkbox"],
.th-chk input[type="checkbox"] {
  width: 13px;
  height: 13px;
  cursor: pointer;
  accent-color: var(--color-primary, #16a34a);
}

.td-uid {
  min-width: 110px;
  width: 110px;
  border-bottom: 1px solid #e5e7eb;
  border-right: 2px solid #c7cdd6;
  padding: 0.35rem 0.5rem;
  font-size: 0.8125rem;
  color: #374151;
  white-space: nowrap;
}

.td-unm {
  min-width: 110px;
  width: 110px;
  border-bottom: 1px solid #e5e7eb;
  border-right: 2px solid #c7cdd6;
  padding: 0.35rem 0.5rem;
  font-size: 0.8125rem;
  color: #374151;
  white-space: nowrap;
  text-align: center;
}

.td-day {
  min-width: 90px;
  border: 1px solid #e5e7eb;
  padding: 0.3rem;
  text-align: center;
  white-space: pre-line;
  vertical-align: middle;
  font-size: 0.78rem;
  color: #374151;
}

.td-sun {
  background: rgba(239, 68, 68, 0.04);
  color: #ef4444;
}
.td-sat {
  background: rgba(59, 130, 246, 0.04);
  color: #3b82f6;
}
.td-holiday {
  background: rgba(239, 68, 68, 0.04);
  color: #ef4444;
}

.td-val {
  display: block;
}
.val-muted {
  color: #9ca3af;
}
</style>
