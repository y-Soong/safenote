<template>
  <Transition name="fade">
    <div
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
          <span>TBM 교육자료 등록</span>
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
            <div class="form-row-max">
              <label>교육자료 제목</label>
              <input
                id="title"
                v-model="formData.title"
                placeholder="교육자료 제목"
              />
            </div>

            <div class="form-row-max">
              <label>교육자료 타입</label>
              <div style="width: 35%">
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

              <label>사용여부</label>
              <div style="width: 35%">
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
                        <th style="width: 18%">URL</th>
                        <th>자료설명</th>
                      </tr>
                    </thead>
                    <tbody>
                      <template
                        v-if="!eduMtrlItemList || eduMtrlItemList.length === 0"
                      >
                        <tr>
                          <td colspan="8" class="edu-grid-empty">
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
                                :disabled="
                                  item.mtrlItemType !== '01' &&
                                  item.mtrlItemType !== '02'
                                "
                                @change="(e) => onFileSelected(e, item, idx)"
                                style="display: none"
                              />
                              <button
                                type="button"
                                class="file-upload-btn"
                                :class="{
                                  'file-upload-btn-disabled':
                                    item.mtrlItemType !== '01' &&
                                    item.mtrlItemType !== '02',
                                }"
                                :disabled="
                                  item.mtrlItemType !== '01' &&
                                  item.mtrlItemType !== '02'
                                "
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
            <button class="btn btn-primary" @click="fnAddRow">추가</button>
            <button class="btn btn-primary" @click="fnReset">초기화</button>
            <button class="btn btn-primary" @click="fnSave()">저장</button>
            <button class="btn btn-primary" @click="fnDelete()">삭제</button>
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
  defineProps,
  defineEmits,
  onMounted,
  getCurrentInstance,
} from "vue";
import axios from "@/api/axios";
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

const apiBaseUrl = process.env.VUE_APP_API_BASE_URL || '';

const systCodeArr = ref([]);
const baseCodeArr = ref([]);
const eduMtrlItemList = ref([]);
const headChk = ref(false);
const fileInputRefs = ref({});

// ================ Reactive Data ================
const formData = reactive({
  mtrlCd: '' || props.mtrlCd_p,
  title: '',
  contents: '',
  mtrlType: '',
  useYn: '',
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
  if (!proxy.$util.isEmpty(props.mtrlCd_p)) {
    await fnSearch();
  }
});

// ================ API Functions ================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-list", {
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
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

const fnGetBaseinfoList = async () => {
  try {
    const response = await axios.get('/comApi/baseinfo/base-info-list', {
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
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      '조회 중 오류가 발생했습니다.';

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

  const ok = await proxy.$confirm("저장하시겠습니까 ?");
  if (!ok) return;

  // 체크된 항목만
  const list = eduMtrlItemList.value.filter((item) => item?.chk);

  try {
    const saveData = new FormData();

    // 1) 일반 필드들 (항상 문자열로 넣는 게 안전)
    saveData.append("mtrlCd", String(formData.mtrlCd ?? ""));
    saveData.append("title", String(formData.title ?? ""));
    saveData.append("contents", String(formData.contents ?? ""));
    saveData.append("mtrlType", String(formData.mtrlType ?? ""));
    saveData.append("useYn", String(formData.useYn ?? ""));

    // 2) 리스트는 JSON 문자열로 (file은 제외해서 넣기)
    const listForJson = list.map((item) => {
      const { file, ...rest } = item;
      return rest;
    });
    saveData.append("eduMtrlItemList", JSON.stringify(listForJson));

    // 3) 파일은 있는 것만 append (없으면 그냥 안 넣음)
    list.forEach((item, idx) => {
      if (item?.file instanceof File) {

        // const fileName = buildFileName("tbm", item.fileNm || item.file.name);
        const fileName = item.file.name;
        saveData.append(`item_${idx}`, item.file, fileName);
      }
    });

    // 4) axios 호출 (Content-Type은 axios가 boundary 포함해서 자동 세팅)
    const response = await axios.post(
      "/webApi/tbm01/save-tbm-edu-infos",
      saveData
      // headers 직접 지정 안 하는 걸 추천
      // { headers: { "Content-Type": "multipart/form-data" } } // <- 굳이 필요 없음
    );

    if (response.status === 200) {
      fnAlertMsg("처리됐습니다.", () => {
        emit("close");
        props.onSearch();
      });
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "저장 중 오류가 발생했습니다.";

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

      if(response.data?.tbmEduInfoList) {
        formData.title = response.data?.tbmEduInfoList[0].title;
        formData.mtrlType = response.data?.tbmEduInfoList[0].mtrlType;
        formData.useYn = response.data?.tbmEduInfoList[0].useYn;
        formData.contents = response.data?.tbmEduInfoList[0].contents; 
        formData.oriTitle = response.data?.tbmEduInfoList[0].title;
        formData.oriContents = response.data?.tbmEduInfoList[0].contents;
        formData.oriMtrlType = response.data?.tbmEduInfoList[0].mtrlType;
        formData.oriUseYn = response.data?.tbmEduInfoList[0].useYn;
      }
      eduMtrlItemList.value = response.data?.tbmEduItemInfoList ?? [];
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
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "조회 중 오류가 발생했습니다.";

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
  const ok = await proxy.$confirm("삭제하시겠습니까 ?");
  if (!ok) return;

  const filteredData = eduMtrlItemList.value.filter((item) => item?.chk);

  if(filteredData.length === 0) {
    proxy.$alert("삭제할 데이터가 없습니다.");
    return;
  }

  try {
    const response = await axios.post("/webApi/tbm01/delete-tbm-edu-item-infos", filteredData);

    if (response.status === 200) {
      fnAlertMsg("처리됐습니다.", () => {
        emit("close");
        props.onSearch();
      });
    }
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "삭제 중 오류가 발생했습니다.";
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
  if(proxy.$util.isEmpty(formData.title)) {
    proxy.$alert("교육자료 제목을 입력해주세요.");
    return false;
  }
  if(proxy.$util.isEmpty(formData.mtrlType)) {
    proxy.$alert("교육자료 타입을 선택해주세요.");
    return false;
  }
  if(proxy.$util.isEmpty(formData.useYn)) {
    proxy.$alert("사용여부를 선택해주세요.");
    return false;
  }

  if(eduMtrlItemList.value.length === 0) {
    proxy.$alert("교육자료 세부항목을 추가해주세요.");
    return false;
  } else {
    for(let i = 0; i < eduMtrlItemList.value.filter(item => item.chk).length; i++) {
      if(proxy.$util.isEmpty(eduMtrlItemList.value[i].mtrlItemType)) {
        proxy.$alert("자료 타입을 선택해주세요.");
        return false;
      }
      if(proxy.$util.isEmpty(eduMtrlItemList.value[i].useYn)) {
        proxy.$alert("사용여부를 선택해주세요.");
        return false;
      }
      if(eduMtrlItemList.value[i].mtrlItemType === '01' || eduMtrlItemList.value[i].mtrlItemType === '02') {
        const file = eduMtrlItemList.value[i].file;
        const hasNewFile = file && typeof file === 'object' && file instanceof File && file.size > 0;
        const hasExistingFile = eduMtrlItemList.value[i].fileMgmtCd && eduMtrlItemList.value[i].filePath;
        if (!hasNewFile && !hasExistingFile) {
          proxy.$alert("파일을 선택해주세요.");
          return false;
        }
      }
      if(eduMtrlItemList.value[i].mtrlItemType === '03') {
        if(proxy.$util.isEmpty(eduMtrlItemList.value[i].url)) {
          proxy.$alert("URL을 입력해주세요.");
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
        await proxy.$alert('파일 다운로드에 실패했습니다.');
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

/** 자료 타입별 허용 확장자: 01=이미지, 02=동영상 */
const getFileAccept = (mtrlItemType) => {
  if (mtrlItemType === '01') return 'image/*';
  if (mtrlItemType === '02') return 'video/*';
  return '';
};

const onFileSelected = async (event, item, idx) => {
  const file = event.target.files?.[0];
  if (!file) return;
  const mtrlItemType = item.mtrlItemType;
  if (mtrlItemType === '01') {
    if (!file.type.startsWith('image/')) {
      await proxy.$alert('자료 타입이 "이미지"일 경우 이미지 파일만\n첨부할 수 있습니다.');
      event.target.value = '';
      return;
    }
  } else if (mtrlItemType === '02') {
    if (!file.type.startsWith('video/')) {
      await proxy.$alert('자료 타입이 "동영상"일 경우 동영상 파일만\n첨부할 수 있습니다.');
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
