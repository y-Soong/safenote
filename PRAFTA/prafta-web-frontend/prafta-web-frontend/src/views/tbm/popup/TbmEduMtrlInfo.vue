<template>
  <Transition name="fade">
    <div
      v-show="true"
      class="modal-overlay prafta-modal-popup"
      @keydown.ctrl.a.stop
      @keydown.meta.a.stop
    >
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 🔹 Title  v-if="visible" -->
        <div class="modal-header" @mousedown="startDrag">
          <span>{{
            isEditMode ? "TBM 교육자료 수정" : "TBM 교육자료 등록"
          }}</span>
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

        <!-- 🔹 Form and Map Container -->
        <div class="content-wrapper">
          <!-- 🔹 Form -->
          <div class="form-container">
            <!-- prafta-033-A: 스코프(회사공통/사업장) + 교육자료 타입 + 사용여부 -->
            <div class="form-row-max">
              <label>스코프</label>
              <div class="scope-radio-group">
                <label class="scope-radio-item">
                  <input
                    type="radio"
                    value="COMMON"
                    v-model="formData.scopeType"
                    :disabled="isEditMode || !canManageCommon"
                    @change="onScopeChange"
                  />
                  회사 공통
                </label>
                <label class="scope-radio-item">
                  <input
                    type="radio"
                    value="SITE"
                    v-model="formData.scopeType"
                    :disabled="isEditMode"
                    @change="onScopeChange"
                  />
                  사업장
                </label>
              </div>

              <label class="inline-label">교육자료 타입</label>
              <div class="inline-select">
                <BaseSelect id="mtrlType" v-model="formData.mtrlType">
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
              </div>

              <label class="inline-label inline-label-sm">사용여부</label>
              <div class="inline-select inline-select-sm">
                <BaseSelect id="useYn" v-model="formData.useYn">
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
              </div>
            </div>

            <div v-if="!canManageCommon" class="scope-hint-row">
              <span class="scope-hint">
                ⓘ 회사 공통은 안전관리자만 등록할 수 있습니다.
              </span>
            </div>

            <div v-if="formData.scopeType === 'SITE'" class="form-row-max">
              <label>사업장</label>
              <div style="width: 35%">
                <BaseSelect
                  id="siteCd"
                  v-model="formData.siteCd"
                  :disabled="isEditMode"
                >
                  <option value="">선택</option>
                  <option
                    v-for="opt in siteList || []"
                    :key="opt.siteCd"
                    :value="opt.siteCd"
                  >
                    {{ opt.siteNm }}
                  </option>
                </BaseSelect>
              </div>
            </div>

            <div class="form-row-max">
              <label>교육자료 제목</label>
              <input
                id="title"
                v-model="formData.title"
                placeholder="교육자료 제목"
              />
            </div>

            <div class="form-row-max">
              <label>교육자료 설명</label>
              <textarea
                id="contents"
                ref="contents"
                style="width: 100%"
                v-model="formData.contents"
              />
            </div>

            <div class="form-row-max edu-grid-row">
              <label>교육자료 세부항목</label>
              <div class="edu-grid-content">
                <div
                  class="overflow-x-auto rounded-md border border-slate-300"
                  style="--box-h: 70vh; --box-sticky-top: 1px; --box-ox: auto"
                >
                  <table
                    class="data-grid w-full table-fixed text-sm text-left rtl:text-right"
                  >
                    <thead>
                      <tr>
                        <th
                          class="event_cell"
                          style="text-align: center; width: 3%"
                        >
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
                        <th style="width: 10%">자료 타입</th>
                        <th style="width: 8%">사용여부</th>
                        <th style="width: 8%">정렬순서</th>
                        <th style="width: 12%">파일</th>
                        <th style="width: 15%">URL</th>
                        <th style="width: 8%">미리보기</th>
                        <th>자료설명</th>
                      </tr>
                    </thead>
                    <tbody>
                      <template
                        v-if="!eduMtrlItemList || eduMtrlItemList.length === 0"
                      >
                        <tr>
                          <td colspan="9" class="edu-grid-empty">
                            등록된 세부 항목이 없습니다.
                          </td>
                        </tr>
                      </template>
                      <template v-else>
                        <tr v-for="(item, idx) in eduMtrlItemList" :key="idx">
                          <td style="text-align: center">{{ idx + 1 }}</td>
                          <td>
                            <input type="checkbox" v-model="item.chk" />
                          </td>
                          <td style="text-align: center">
                            <!-- {{ item.mtrlType }} -->
                            <BaseSelect
                              id="mtrlItemType"
                              v-model="item.mtrlItemType"
                              @update:modelValue="
                                () => onMtrlItemTypeChange(item)
                              "
                            >
                              <option
                                v-for="opt in (
                                  systCodeArr['SYS018'] || []
                                ).filter((o) => o.systValDCd != null)"
                                :key="opt.systValDCd"
                                :value="opt.systValDCd"
                              >
                                {{ opt.systValDNm }}
                              </option>
                            </BaseSelect>
                          </td>
                          <td style="text-align: center">
                            <BaseSelect id="useYn" v-model="item.useYn">
                              <option
                                v-for="opt in (
                                  systCodeArr['SYS003'] || []
                                ).filter((o) => o.systValDCd != null)"
                                :key="opt.systValDCd"
                                :value="opt.systValDCd"
                              >
                                {{ opt.systValDNm }}
                              </option>
                            </BaseSelect>
                          </td>
                          <td style="text-align: left">
                            <input
                              type="number"
                              v-model="item.sortIdx"
                              style="width: 100%"
                            />
                          </td>
                          <td style="text-align: center">
                            <div
                              style="
                                display: flex;
                                align-items: left;
                                justify-content: center;
                              "
                            >
                              <input
                                :ref="(el) => setFileInputRef(el, idx)"
                                type="file"
                                :accept="getFileAccept(item.mtrlItemType)"
                                :disabled="!isFileType(item.mtrlItemType)"
                                @change="(e) => onFileSelected(e, item, idx)"
                                style="display: none"
                              />
                              <button
                                type="button"
                                class="file-upload-btn"
                                :class="{
                                  'file-upload-btn-disabled': !isFileType(
                                    item.mtrlItemType
                                  ),
                                }"
                                :disabled="!isFileType(item.mtrlItemType)"
                                @click="() => handleFileButtonClick(idx)"
                              >
                                <svg
                                  xmlns="http://www.w3.org/2000/svg"
                                  fill="none"
                                  viewBox="0 0 24 24"
                                  stroke-width="1.5"
                                  stroke="currentColor"
                                  style="width: 16px; height: 16px"
                                >
                                  <path
                                    stroke-linecap="round"
                                    stroke-linejoin="round"
                                    d="M18.375 12.739l-7.693 7.693a4.5 4.5 0 01-6.364-6.364l10.94-10.94A3 3 0 1119.5 7.372L8.552 18.32m.009-.01l-.01.01m5.699-9.941l-7.81 7.81a6 6 0 018.486-8.486L19.5 9.5"
                                  />
                                </svg>
                                <span v-if="item.fileNm">{{
                                  item.fileNm
                                }}</span>
                                <span v-else>파일선택</span>
                              </button>
                              <button
                                v-if="canDownloadFile(item)"
                                type="button"
                                class="file-download-btn"
                                title="다운로드"
                                @click="handleFileDownload(item)"
                              >
                                <svg
                                  xmlns="http://www.w3.org/2000/svg"
                                  fill="none"
                                  viewBox="0 0 24 24"
                                  stroke-width="1.5"
                                  stroke="currentColor"
                                  style="width: 16px; height: 16px"
                                >
                                  <path
                                    stroke-linecap="round"
                                    stroke-linejoin="round"
                                    d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3"
                                  />
                                </svg>
                              </button>
                            </div>
                          </td>
                          <td style="text-align: left">
                            <input
                              style="width: 100%"
                              v-model="item.url"
                              :disabled="item.mtrlItemType !== '03'"
                            />
                          </td>
                          <td style="text-align: center">
                            <img
                              v-if="getThumbUrl(item)"
                              :src="getThumbUrl(item)"
                              class="thumb-preview"
                              alt="썸네일"
                            />
                            <span v-else class="thumb-placeholder">{{
                              getMediaTypeIcon(item.mtrlItemType)
                            }}</span>
                          </td>
                          <td style="text-align: left">
                            <input
                              style="width: 100%"
                              v-model="item.mtrlDesc"
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
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-second" @click="fnAddRow">행 추가</button>
            <button class="btn btn-second" @click="fnDelete()">
              선택 행 삭제
            </button>
            <button class="btn btn-second" @click="fnReset">초기화</button>
            <button class="btn btn-primary" @click="fnSave()">저장</button>
            <button class="btn btn-second" @click="$emit('close')">닫기</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  reactive,
  computed,
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { readFileAsBase64 } from "@/utils/fileUtil";
import { useFieldWatcher } from '@/utils/useFieldWatcher';
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import BaseSelect from "@/components/common/BaseSelect.vue";

// ================ Instance ================
const { proxy } = getCurrentInstance();

// ================ define Props, define Emits ================
const props = defineProps({
  mtrlCd_p: String,
  onSearch: Function,
});
const emit = defineEmits(["close"]);

// ================ Ref Data ================
const modalRef = ref(null);

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '';

const systCodeArr = ref([]);
const baseCodeArr = ref([]);
const eduMtrlItemList = ref([]);
const headChk = ref(false);
const fileInputRefs = ref({});
const siteList = ref([]); // prafta-033-A: 사업장 목록

// prafta-033-A: 수정 모드 여부(mtrlCd_p 존재 시). 스코프 변경 잠금에 사용
const isEditMode = computed(() => !proxy.$util.isEmpty(props.mtrlCd_p));

// prafta-033-A: 회사공통 콘텐츠 등록 권한(master/safe)
const canManageCommon = computed(() => {
  const authCd = sessionStorage.getItem("gv_authCd");
  return authCd === "master" || authCd === "safe";
});

// ================ Reactive Data ================
const formData = reactive({
  mtrlCd: '' || props.mtrlCd_p,
  title: '',
  contents: '',
  mtrlType: '',
  useYn: '',
  scopeType: '', // prafta-033-A: COMMON(회사공통) / SITE(사업장)
  siteCd: '',
  oriTitle: '',
  oriContents: '',
  oriMtrlType: '',
  oriUseYn: '',
});


// 공통 훅으로 화면 중앙(살짝 위쪽)에 배치 + 드래그 가능
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// ================ Life Cycle Functions ================
onMounted(async () => {
  await fnGetSystinfoList();
  await fnGetBaseinfoList();
  await fnGetSiteList();
  if (!proxy.$util.isEmpty(props.mtrlCd_p)) {
    await fnSearch();
  } else {
    // 신규 등록: 권한에 따라 기본 스코프 결정(공통 권한 없으면 사업장 기본 + 본인 사업장 선택)
    if (canManageCommon.value) {
      formData.scopeType = "COMMON";
    } else {
      formData.scopeType = "SITE";
      formData.siteCd = sessionStorage.getItem("gv_siteCd") || "";
    }
  }
});

// ================ API Functions ================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS003", "SYS018"],
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

      formData.useYn = systCodeArr.value.SYS003.filter(
        (o) => o.systValDCd != null
      )[0].systValDCd;

    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get('/comApi/baseinfo/base-info-lists', {
      params: {
        cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
        baseCodeList: ['COM003'],
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

      formData.mtrlType = baseCodeArr.value.COM003.filter(
        (o) => o.baimValDCd != null
      )[0].baimValDCd;
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, '조회 중 오류가 발생했습니다.');

    await proxy.$alert(msg);
  }
};

// prafta-033-A: 사업장 목록 조회(스코프=사업장 선택용)
const fnGetSiteList = async () => {
  try {
    const response = await axios.get('/comApi/baseinfo/site-lists', {
      params: {
        cmpnyCd: sessionStorage.getItem('gv_cmpnyCd'),
        siteNo: '',
        siteNm: '',
      },
    });

    if (response.status === 200) {
      siteList.value = response.data?.siteInfoResultList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, '조회 중 오류가 발생했습니다.');

    await proxy.$alert(msg);
  }
};

/** 서버 전송용 파일명 생성 (RiskAssessInfo와 동일 패턴) */
// const buildFileName = (prefix, originalName = "file") => {
//   const ts = new Date().toISOString().replace(/[:.]/g, "");
//   const safe = String(originalName).replace(/[^\w.\-]+/g, "_");
//   return `${prefix}_${ts}_${safe}`;
// };

const fnSave = async () => {
  if (!dataValidationChk()) return;

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  // 체크된 항목만
  const list = eduMtrlItemList.value.filter((item) => item?.chk);

  try {
    const itemListPayload = await Promise.all(
      list.map(async (item) => {
        const {
          file,
          chk,
          oriSortIdx,
          oriMtrlItemType,
          oriMtrlDesc,
          oriFileMgmtCd,
          oriFileNm,
          oriFilePath,
          oriFileExt,
          ...rest
        } = item;
        const row = { ...rest };
        if (file instanceof File) {
          row.itemBase64 = await readFileAsBase64(file);
          row.itemOriginalFilename = file.name;
        }
        return row;
      })
    );

    console.log("itemListPayload :: ");
    console.log(itemListPayload);

    // prafta-033-A: 스코프 -> SITE_CD. COMMON 이면 빈 값(서버에서 NULL=회사공통 처리)
    const saveSiteCd =
      formData.scopeType === "SITE" ? String(formData.siteCd ?? "") : "";

    const requestBody = {
      mtrlCd: String(formData.mtrlCd ?? ""),
      title: String(formData.title ?? ""),
      contents: String(formData.contents ?? ""),
      mtrlType: String(formData.mtrlType ?? ""),
      useYn: String(formData.useYn ?? ""),
      siteCd: saveSiteCd,
      tbmEduItemInfoModelList: itemListPayload,
    };

    console.log(requestBody);

    const response = await axios.post(
      "/webApi/tbm01/save-tbm-edu-infos",
      requestBody,
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      fnAlertMsg(getMessage(MSG.SAVE_SUCCESS), () => {
        emit("close");
        props.onSearch();
      });
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

const fnSearch = async () => {
  try {
    const response = await axios.get("/webApi/tbm01/tbm-edu-infos", {
      params: {
        mtrlCd: formData.mtrlCd,
        mtrlType: '',
        title: '',
        useYn: '',
      },
    });

    if (response.status === 200) {

      if(response.data?.tbmEduInfoResultList) {
        const master = response.data?.tbmEduInfoResultList[0];
        formData.title = master.title;
        formData.mtrlType = master.mtrlType;
        formData.useYn = master.useYn;
        formData.contents = master.contents;
        formData.oriTitle = master.title;
        formData.oriContents = master.contents;
        formData.oriMtrlType = master.mtrlType;
        formData.oriUseYn = master.useYn;
        // prafta-033-A: 스코프 복원(수정 모드에서는 잠금)
        formData.scopeType = master.isCommonContent === 'Y' ? 'COMMON' : 'SITE';
        formData.siteCd = master.siteCd || '';
      }
      eduMtrlItemList.value = response.data?.tbmEduItemInfoResultList ?? [];

      // 초기화 버튼용 원본 값 저장 (복사 소스)
      eduMtrlItemList.value.forEach((item) => {
        item.oriSortIdx = item.sortIdx;
        item.oriMtrlItemType = item.mtrlItemType;
        item.oriMtrlDesc = item.mtrlDesc;
        item.oriFileMgmtCd = item.fileMgmtCd;
        item.oriFileNm = item.fileNm;
        item.oriFilePath = item.filePath;
        item.oriFileExt = item.fileExt;
      });
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

/** 초기화: ori* 값을 현재 필드로 복사 */
const fnReset = () => {
  if(formData.oriTitle !== undefined) formData.title = formData.oriTitle;
  if(formData.oriContents !== undefined) formData.contents = formData.oriContents;
  if(formData.oriMtrlType !== undefined) formData.mtrlType = formData.oriMtrlType;
  if(formData.oriUseYn !== undefined) formData.useYn = formData.oriUseYn;

  eduMtrlItemList.value.forEach((item) => {
    if (item.oriSortIdx !== undefined) item.sortIdx = item.oriSortIdx;
    if (item.oriMtrlItemType !== undefined) item.mtrlItemType = item.oriMtrlItemType;
    if (item.oriMtrlDesc !== undefined) item.mtrlDesc = item.oriMtrlDesc;
    if (item.oriFileMgmtCd !== undefined) item.fileMgmtCd = item.oriFileMgmtCd;
    if (item.oriFileNm !== undefined) item.fileNm = item.oriFileNm;
    if (item.oriFilePath !== undefined) item.filePath = item.oriFilePath;
    if (item.oriFileExt !== undefined) item.fileExt = item.oriFileExt;
    if (item.oriUrl !== undefined) item.url = item.oriUrl;
    if (item.oriUseYn !== undefined) item.useYn = item.oriUseYn;
    item.file = null; // 새로 첨부한 파일은 제거
  });
  if (typeof props.reset === 'function') props.reset();
};

const fnDelete = async () => {
  const ok = await proxy.$confirm(getMessage(MSG.DELETE_CONFIRM));
  if (!ok) return;

  const filteredData = eduMtrlItemList.value.filter((item) => item?.chk);

  if(filteredData.length === 0) {
    proxy.$alert(getMessage(MSG.DELETE_DATA_REQUIRED));
    return;
  }

  try {
    const response = await axios.post("/webApi/tbm01/delete-tbm-edu-item-infos", filteredData);

    if (response.status === 200) {
      fnAlertMsg(getMessage(MSG.SAVE_SUCCESS), () => {
        emit("close");
        props.onSearch();
      });
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "삭제 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// =========================== Watch, Watcher ===========================
useFieldWatcher(
  eduMtrlItemList,
  (item) => {
    item.chk = true;
  },
  ['chk']
);

// ================ User Functions ================
const fnAlertMsg = async (message, afterConfirmCallback) => {
  await proxy.$alert(message);
  if (afterConfirmCallback) {
    afterConfirmCallback();
  }
};

const dataValidationChk = () => {
  // prafta-033-A: 스코프 검증
  if(proxy.$util.isEmpty(formData.scopeType)) {
    proxy.$alert('스코프를 선택해 주세요.');
    return false;
  }
  if(formData.scopeType === 'COMMON' && !canManageCommon.value) {
    proxy.$alert('회사 공통 콘텐츠는 안전관리자만 등록할 수 있습니다.');
    return false;
  }
  if(formData.scopeType === 'SITE' && proxy.$util.isEmpty(formData.siteCd)) {
    proxy.$alert('사업장을 선택해 주세요.');
    return false;
  }
  if(proxy.$util.isEmpty(formData.title)) {
    proxy.$alert(getMessage(MSG.EDU_TITLE_REQUIRED));
    return false;
  }
  if(proxy.$util.isEmpty(formData.mtrlType)) {
    proxy.$alert(getMessage(MSG.EDU_TYPE_REQUIRED));
    return false;
  }
  if(proxy.$util.isEmpty(formData.useYn)) {
    proxy.$alert(getMessage(MSG.USE_YN_REQUIRED));
    return false;
  }

  if(eduMtrlItemList.value.length === 0) {
    proxy.$alert(getMessage(MSG.EDU_ITEM_REQUIRED));
    return false;
  } else {
    for(let i = 0; i < eduMtrlItemList.value.filter(item => item.chk).length; i++) {
      if(proxy.$util.isEmpty(eduMtrlItemList.value[i].mtrlItemType)) {
        proxy.$alert(getMessage(MSG.MATERIAL_TYPE_REQUIRED));
        return false;
      }
      if(proxy.$util.isEmpty(eduMtrlItemList.value[i].useYn)) {
        proxy.$alert(getMessage(MSG.USE_YN_REQUIRED));
        return false;
      }
      if(eduMtrlItemList.value[i].mtrlItemType === '01' || eduMtrlItemList.value[i].mtrlItemType === '02' || eduMtrlItemList.value[i].mtrlItemType === '04') {
        const file = eduMtrlItemList.value[i].file;
        const hasNewFile = file && typeof file === 'object' && file instanceof File && file.size > 0;
        const hasExistingFile = eduMtrlItemList.value[i].fileMgmtCd && eduMtrlItemList.value[i].filePath;
        if (!hasNewFile && !hasExistingFile) {
          proxy.$alert(getMessage(MSG.FILE_REQUIRED));
          return false;
        }
      }
      if(eduMtrlItemList.value[i].mtrlItemType === '03') {
        if(proxy.$util.isEmpty(eduMtrlItemList.value[i].url)) {
          proxy.$alert(getMessage(MSG.URL_REQUIRED));
          return false;
        }
      }
    }
  }

  return true;
}

const fnHeadChk = () => {
  headChk.value = !headChk.value;
  eduMtrlItemList.value.forEach((item) => {
    item.chk = headChk.value;
  });
}

const setFileInputRef = (el, idx) => {
  if (el) {
    fileInputRefs.value[idx] = el;
  }
}

const handleFileButtonClick = (idx) => {
  const fileInput = fileInputRefs.value[idx];
  if (fileInput) {
    fileInput.click();
  }
};

/** 서버 파일: 서비스 URL + filePath + fileMgmtCd + fileExt (fileMgmtCd와 fileExt 사이에는 '/' 없음) */
const getDownloadUrl = (item) => {
  if (!item?.filePath || !item?.fileMgmtCd) return '';
  const base = (apiBaseUrl || '').replace(/\/$/, '');
  const path = (item.filePath || '').replace(/^\//, '').replace(/\/$/, '');
  const code = (item.fileMgmtCd || '').replace(/^\//, '');
  const ext = (item.fileExt || '').replace(/^\//, '');
  return [base, path, code + ext].filter(Boolean).join('/');
};

/** 다운로드 버튼 표시: 서버 파일(filePath+fileMgmtCd) 또는 새로 첨부한 파일(item.file)이 있을 때 */
const canDownloadFile = (item) => {
  if (!item) return false;
  if (item.filePath && item.fileMgmtCd) return true;
  if (item.file && item.file instanceof File) return true;
  return false;
};

const handleFileDownload = async (item) => {
  if (!item) return;
  if (item.filePath && item.fileMgmtCd) {
    const url = getDownloadUrl(item);
    if (url) {
      try {
        const response = await fetch(url);
        if (!response.ok) throw new Error('파일 다운로드 실패');
        const blob = await response.blob();
        const blobUrl = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = blobUrl;
        a.download = item.fileNm || 'download';
        a.rel = 'noopener';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(blobUrl);
      } catch (error) {
        await proxy.$alert(getMessage(MSG.FILE_DOWNLOAD_FAILED));
      }
    }
    return;
  }
  if (item.file && item.file instanceof File) {
    const url = URL.createObjectURL(item.file);
    const a = document.createElement('a');
    a.href = url;
    a.download = item.fileNm || item.file.name || 'download';
    a.rel = 'noopener';
    a.click();
    URL.revokeObjectURL(url);
  }
};

/** 자료 타입 변경 시 해당 행의 파일/URL 관련 필드 초기화 */
const onMtrlItemTypeChange = (item) => {
  if (!item) return;
  item.fileMgmtCd = null;
  item.filePath = null;
  item.fileExt = null;
  item.fileNm = null;
  item.file = null;
  item.url = null;
};

/** prafta-033-A: 파일 첨부 대상 타입(01 이미지/02 동영상/04 PDF). 03 유튜브는 URL */
const isFileType = (mtrlItemType) =>
  mtrlItemType === '01' || mtrlItemType === '02' || mtrlItemType === '04';

/** 자료 타입별 허용 확장자: 01=이미지, 02=동영상, 04=PDF */
const getFileAccept = (mtrlItemType) => {
  if (mtrlItemType === '01') return 'image/*';
  if (mtrlItemType === '02') return 'video/*';
  if (mtrlItemType === '04') return 'application/pdf';
  return '';
};

/** prafta-033-A: 스코프 변경 시 사업장 선택값 정리 */
const onScopeChange = () => {
  if (formData.scopeType === 'COMMON') {
    formData.siteCd = '';
  } else if (formData.scopeType === 'SITE' && proxy.$util.isEmpty(formData.siteCd)) {
    formData.siteCd = sessionStorage.getItem('gv_siteCd') || '';
  }
};

/** prafta-033-A: 썸네일 미리보기 URL(서버 썸네일 파일 또는 이미지 파일 자체) */
const getThumbUrl = (item) => {
  if (!item) return '';
  // 이미지 타입은 본 파일 자체를 썸네일로 사용
  if (item.mtrlItemType === '01' && item.filePath && item.fileMgmtCd) {
    return getDownloadUrl(item);
  }
  return '';
};

/** prafta-033-A: 썸네일 없는 경우 타입별 아이콘 문자 */
const getMediaTypeIcon = (mtrlItemType) => {
  if (mtrlItemType === '02') return '동영상';
  if (mtrlItemType === '03') return '유튜브';
  if (mtrlItemType === '04') return 'PDF';
  if (mtrlItemType === '01') return '이미지';
  return '-';
};

const onFileSelected = async (event, item, idx) => {
  const file = event.target.files?.[0];
  if (!file) return;
  const mtrlItemType = item.mtrlItemType;
  if (mtrlItemType === '01') {
    if (!file.type.startsWith('image/')) {
      await proxy.$alert(getMessage(MSG.IMAGE_FILE_ONLY));
      event.target.value = '';
      return;
    }
  } else if (mtrlItemType === '02') {
    if (!file.type.startsWith('video/')) {
      await proxy.$alert(getMessage(MSG.VIDEO_FILE_ONLY));
      event.target.value = '';
      return;
    }
  } else if (mtrlItemType === '04') {
    // prafta-033-A: PDF 타입 - 확장자/타입 검증
    const isPdf =
      file.type === 'application/pdf' ||
      /\.pdf$/i.test(file.name || '');
    if (!isPdf) {
      await proxy.$alert('PDF 파일만 업로드할 수 있습니다.');
      event.target.value = '';
      return;
    }
  }
  item.fileNm = file.name;
  item.file = file;
  // 새 파일을 첨부하면 기존 서버 파일 정보 초기화
  item.fileMgmtCd = null;
  item.filePath = null;
  item.fileExt = null;
}

const fnAddRow = () => {
  eduMtrlItemList.value.push({
    chk: true,
    mtrlItemType: '01',
    useYn: 'Y',
    sortIdx: eduMtrlItemList.value.length + 1,
  });
}

</script>

<style scoped>
/* prafta-033-A: 스코프 라디오/안내/썸네일 */
.scope-radio-group {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 0 0 auto;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.scope-radio-item {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  cursor: pointer;
  color: var(--color-text);
  white-space: nowrap;
}

/* prafta-033-A: 라디오 버튼은 form-row-max input:focus의 box-shadow(ring)를 상속받지 않도록 무력화 */
.scope-radio-item input[type="radio"],
.scope-radio-item input[type="radio"]:focus,
.scope-radio-item input[type="radio"]:focus-visible {
  outline: none;
  box-shadow: none;
  border: none;
  background: transparent;
  padding: 0;
  flex: 0 0 auto;
}

/* prafta-033-A: 스코프 행 인라인 라벨/SelectBox (교육자료 타입, 사용여부) */
.inline-label {
  flex: 0 0 100px !important;
  margin-left: 1rem;
}

.inline-label-sm {
  flex: 0 0 64px !important;
  margin-left: 0.5rem;
}

.inline-select {
  flex: 0 0 180px;
}

.inline-select-sm {
  flex: 0 0 110px;
}

.scope-hint-row {
  display: flex;
  align-items: center;
  padding-left: 128px; /* label flex 120px + gap 0.5rem 정렬 */
}

.scope-hint {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
  white-space: nowrap;
}

.thumb-preview {
  max-width: 48px;
  max-height: 48px;
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
  object-fit: cover;
}

.thumb-placeholder {
  font-size: var(--btn-font-sm);
  color: var(--color-text-muted);
}

.content-wrapper {
  /* display: flex; */
  gap: 1rem;
  padding: 1.2rem;
  height: calc(100% - 60px);
  overflow: hidden;
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  flex: 0 0 850px;
  overflow-y: auto;
  padding-right: 0.5rem;
}

.edu-grid-row {
  align-items: flex-start;
}

.edu-grid-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.edu-grid-table-wrapper {
  border: 1px solid #e3e6eb;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
  max-height: 240px;
  overflow-y: auto;
}

.edu-grid-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.edu-grid-table th {
  background: #f6f8fb;
  color: #333;
  font-weight: 600;
  padding: 0.6rem 0.5rem;
  border-bottom: 1px solid #e3e6eb;
  text-align: center;
}

.edu-grid-table td {
  padding: 0.55rem 0.5rem;
  border-bottom: 1px solid #eef1f5;
  text-align: center;
  color: #444;
}

.edu-grid-table tbody tr:nth-child(even) td {
  background: #fafbfd;
}

.file-upload-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: #f9f9f9;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.85rem;
  min-height: 28px;
  text-align: left;
  color: #333;
  width: 80%;
  max-width: none;
  min-width: 0;
  overflow: hidden;
}

.file-upload-btn:hover:not(.file-upload-btn-disabled) {
  border-color: #30796a;
  background: #f0f0f0;
}

.file-upload-btn-disabled {
  background: #f5f5f5;
  color: #999;
  cursor: not-allowed;
  opacity: 0.6;
}

.file-upload-btn span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
  text-align: left;
}

.file-download-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: #f9f9f9;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 28px;
  padding: 0.25rem 0.5rem;
  color: #333;
}

.file-download-btn:hover {
  border-color: #30796a;
  background: #f0f0f0;
}
</style>
