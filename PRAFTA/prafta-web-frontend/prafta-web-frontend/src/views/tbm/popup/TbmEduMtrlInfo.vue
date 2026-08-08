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
            <!-- T5-2: 사용 중(취소 외 세션 참조) 교육자료는 내용 수정 불가.
                 단, 교육자료는 재사용되므로 AI 분석 지정만은 변경 가능(잠금 우회 저장 경로). -->
            <div v-if="editLocked" class="lock-hint-row">
              <span class="lock-hint">
                ⓘ TBM 교육에서 사용 중인 교육자료입니다. 내용은 수정할 수
                없으며, AI 분석 지정만 변경할 수 있습니다.
              </span>
            </div>
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
                <BaseSelect
                  id="mtrlType"
                  v-model="formData.mtrlType"
                  :disabled="editLocked"
                >
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
                <BaseSelect
                  id="useYn"
                  v-model="formData.useYn"
                  :disabled="editLocked"
                >
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
              <!-- NoticeCreatePop 패턴: select 대신 사업장명(readonly) + 조회 팝업 버튼 -->
              <div class="site-search-field">
                <input
                  id="siteCd"
                  type="text"
                  :value="siteNm"
                  placeholder="사업장 조회"
                  readonly
                  :disabled="isEditMode"
                  @click="onSiteSearchClick"
                />
                <button
                  class="search-btn"
                  :disabled="isEditMode"
                  @click="onSiteSearchClick"
                >
                  <img
                    class="search_icon"
                    :src="search_icon"
                    alt="사업장 조회"
                  />
                </button>
              </div>
            </div>

            <div class="form-row-max">
              <label>교육자료 제목</label>
              <input
                id="title"
                v-model="formData.title"
                placeholder="교육자료 제목"
                :disabled="editLocked"
              />
            </div>

            <div class="form-row-max">
              <label>교육자료 설명</label>
              <textarea
                id="contents"
                ref="contents"
                style="width: 100%"
                v-model="formData.contents"
                :disabled="editLocked"
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
                            :disabled="editLocked"
                            @click="fnHeadChk"
                          />
                        </th>
                        <th style="width: 10%">자료 타입</th>
                        <th style="width: 8%">사용여부</th>
                        <th style="width: 8%">정렬순서</th>
                        <th style="width: 12%">파일</th>
                        <th style="width: 13%">URL</th>
                        <th>자료설명</th>
                        <th style="width: 6%">AI 분석</th>
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
                            <input
                              type="checkbox"
                              v-model="item.chk"
                              :disabled="editLocked"
                            />
                          </td>
                          <td style="text-align: center">
                            <!-- {{ item.mtrlType }} -->
                            <BaseSelect
                              id="mtrlItemType"
                              v-model="item.mtrlItemType"
                              :disabled="editLocked"
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
                            <BaseSelect
                              id="useYn"
                              v-model="item.useYn"
                              :disabled="editLocked"
                            >
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
                              :disabled="editLocked"
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
                                  !isFileType(item.mtrlItemType) || editLocked
                                "
                                @change="(e) => onFileSelected(e, item, idx)"
                                style="display: none"
                              />
                              <button
                                type="button"
                                class="file-upload-btn"
                                :class="{
                                  'file-upload-btn-disabled':
                                    !isFileType(item.mtrlItemType) ||
                                    editLocked,
                                }"
                                :disabled="
                                  !isFileType(item.mtrlItemType) || editLocked
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
                              :disabled="
                                item.mtrlItemType !== '03' || editLocked
                              "
                            />
                          </td>
                          <td style="text-align: left">
                            <input
                              style="width: 100%"
                              v-model="item.mtrlDesc"
                              :disabled="editLocked"
                            />
                          </td>
                          <!-- AI 분석 지정: 전 타입(이미지 01·동영상 02·유튜브 03·PDF 04) 모두 체크 가능.
                               01·04 는 VLM 자동분석 대상, 02·03 은 자동분석 없이 AI 분석관리 탭에서
                               관리자 입력을 AI 분석내용으로 취급(플로우 동일). 잠금 시에도 지정은 변경 가능. -->
                          <td style="text-align: center">
                            <input
                              type="checkbox"
                              :true-value="'Y'"
                              :false-value="'N'"
                              v-model="item.aiAnalyzeYn"
                            />
                          </td>
                        </tr>
                      </template>
                    </tbody>
                  </table>
                </div>
                <div class="edu-grid-toolbar">
                  <button
                    class="btn btn-second"
                    :disabled="editLocked"
                    @click="fnAddRow"
                  >
                    행 추가
                  </button>
                  <button
                    class="btn btn-second"
                    :disabled="editLocked"
                    @click="fnDelete()"
                  >
                    선택 행 삭제
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <div class="btn-group">
            <!-- 초기화: 신규 등록에서만 노출(입력값 비우기). 수정 모드에서는 원본 복원이 불완전
                 (추가/삭제 행·AI 분석 지정 미복원)해 오해를 부르므로 숨긴다. 되돌리려면 닫고 다시 열면 된다. -->
            <button v-if="!isEditMode" class="btn btn-second" @click="fnReset">
              초기화
            </button>
            <!-- 사용 중(잠금)일 때도 저장 버튼 활성: fnSave 내부에서 AI 분석 지정만 저장하는 경로로 분기 -->
            <button class="btn btn-primary" @click="fnSave()">
              {{ editLocked ? "AI 분석 지정 저장" : "저장" }}
            </button>
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
import { useModal } from "@/utils/useModal";
import BaseSelect from "@/components/common/BaseSelect.vue";
import SiteSearchPop from "@/components/popup/SiteSearchPop.vue";
import search_icon from "@/assets/img/search_icon.png";

// ================ Instance ================
const { proxy } = getCurrentInstance();
const { open: openPop } = useModal();

// ================ define Props, define Emits ================
const props = defineProps({
  mtrlCd_p: String,
  onSearch: Function,
});
const emit = defineEmits(["close"]);

// ================ Ref Data ================
const modalRef = ref(null);

const systCodeArr = ref([]);
const baseCodeArr = ref([]);
const eduMtrlItemList = ref([]);
const headChk = ref(false);
const fileInputRefs = ref({});
const siteList = ref([]); // prafta-033-A: 사업장 목록
const siteNm = ref(""); // 사업장명 표시용(조회 팝업 선택값)
const locked = ref(false); // T5-2: 사용 중(취소 외 세션 참조) 여부. 'Y'면 수정/저장 차단

// prafta-033-A: 수정 모드 여부(mtrlCd_p 존재 시). 스코프 변경 잠금에 사용
const isEditMode = computed(() => !proxy.$util.isEmpty(props.mtrlCd_p));

// prafta-033-A: 회사공통 콘텐츠 등록 권한(master/safe)
const canManageCommon = computed(() => {
  const authCd = sessionStorage.getItem("gv_authCd");
  return authCd === "master" || authCd === "safe";
});

// T5-2: 수정 모드 + 사용 중(잠금) 일 때 편집/저장 차단(열람만 허용)
const editLocked = computed(() => isEditMode.value && locked.value);

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
      siteNm.value =
        resolveSiteNm(formData.siteCd) ||
        sessionStorage.getItem("gv_siteNm") ||
        "";
    }
  }
});

// ================ API Functions ================
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS003", "SYS018", "SYS056"],
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

// 사업장코드 → 사업장명 매핑(siteList 기준). 표시용 siteNm 복원에 사용
const resolveSiteNm = (cd) => {
  if (proxy.$util.isEmpty(cd)) return '';
  const found = (siteList.value || []).find((s) => s.siteCd === cd);
  return found ? found.siteNm : '';
};

// 사업장 조회 팝업 열기(수정 모드에서는 잠금)
const onSiteSearchClick = () => {
  if (isEditMode.value) return;
  fnOpenSiteSearch();
};

const fnOpenSiteSearch = () => {
  openPop(SiteSearchPop, {
    cmpnyCd_p: sessionStorage.getItem('gv_cmpnyCd'),
    siteNo_p: '',
    siteNm_p: '',
    // SiteSearchPop onSelect 인자 순서 = (siteCd, siteNo, siteNm)
    onSelect: (siteCdVal, siteNoVal, siteNmVal) => {
      formData.siteCd = siteCdVal ?? '';
      siteNm.value = siteNmVal ?? '';
    },
  });
};

/** 서버 전송용 파일명 생성 (RiskAssessInfo와 동일 패턴) */
// const buildFileName = (prefix, originalName = "file") => {
//   const ts = new Date().toISOString().replace(/[:.]/g, "");
//   const safe = String(originalName).replace(/[^\w.\-]+/g, "_");
//   return `${prefix}_${ts}_${safe}`;
// };

const fnSave = async () => {
  // T5-2: 사용 중(취소 외 세션 참조) 교육자료는 내용 저장 차단(서버에서도 TBM_409_055 로 거부).
  //   단, 교육자료는 재사용되므로 AI 분석 지정만은 전용 경로로 저장 허용(잠금 우회).
  if (editLocked.value) {
    await fnSaveAiAnalyze();
    return;
  }

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
          // TBM_AI-RB-1: 저장 payload 에서 제외(불필요 전송). aiAnalyzeYn 은 ...rest 로 유지(RA 영속 대상).
          aiStatus,
          aiConfirmDesc,
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

    const response = await axios.post(
      "/webApi/tbm01/save-tbm-edu-infos",
      requestBody,
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      // TBM_AI-RB-1: 저장 성공 → 목록 갱신 후 팝업 종료. 분석/확정은 [AI 분석 관리] 탭으로 이관됨.
      //   (AI_ANALYZE_YN 영속·체크해제 시 AI_STATUS='NONE' 리셋은 RA 서버가 처리.)
      await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      props.onSearch();
      emit("close");
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

/**
 * 사용 중(잠금) 교육자료의 AI 분석 지정만 저장.
 * 내용/파일/그리드 항목은 잠금 유지, 분석 대상 타입(이미지 01·PDF 04)의 AI_ANALYZE_YN 만 서버로 전송.
 * 서버(update-tbm-edu-ai-analyze)는 잠금 검증을 생략하되 권한/회사 스코프(IDOR)는 유지한다.
 */
const fnSaveAiAnalyze = async () => {
  // 저장된 항목(mtrlItemCd 존재)의 AI 분석 지정을 전 타입 전송(신규 미저장 행은 잠금 상태에서 생성 불가).
  //   01·04 는 VLM 자동분석, 02·03 은 관리자 입력을 AI 분석내용으로 취급 — 지정 플로우는 동일.
  const items = eduMtrlItemList.value
    .filter((item) => !proxy.$util.isEmpty(item.mtrlItemCd))
    .map((item) => ({
      mtrlItemCd: String(item.mtrlItemCd),
      aiAnalyzeYn: item.aiAnalyzeYn === "Y" ? "Y" : "N",
    }));

  if (items.length === 0) {
    await proxy.$alert("저장할 세부 항목이 없습니다.");
    return;
  }

  const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
  if (!ok) return;

  try {
    const response = await axios.post(
      "/webApi/tbm01/update-tbm-edu-ai-analyze",
      {
        mtrlCd: String(formData.mtrlCd ?? ""),
        itemList: items,
      },
      { headers: { "Content-Type": "application/json" } }
    );

    if (response.status === 200) {
      await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
      props.onSearch();
      emit("close");
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
        siteNm.value = master.siteNm || resolveSiteNm(formData.siteCd);
        // T5-2: 사용 중(취소 외 세션 참조) 여부. 'Y'면 편집/저장 잠금
        locked.value = master.lockedYn === 'Y';
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
        // TBM_AI-RB-1: AI 분석 토글 기본값(저장 payload 영속 대상). 상태/확정서술은 [AI 분석 관리] 탭 소관.
        if (item.aiAnalyzeYn == null) item.aiAnalyzeYn = "N";
      });
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");

    await proxy.$alert(msg);
  }
};

/** 초기화: ori* 값을 현재 필드로 복사(신규 등록 전용 — ori* 가 빈 값이라 입력값 비우기로 동작) */
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
    // 원본 백업이 없는 행(= 신규 추가행)은 첨부 표시 필드도 함께 비운다.
    //   file 만 지우면 파일명은 남고 실제 파일은 없는 불일치 상태가 된다.
    if (item.oriFileNm === undefined) {
      item.fileNm = null;
      item.fileExt = null;
      item.filePath = null;
      item.fileMgmtCd = null;
    }
  });
};

const fnDelete = async () => {
  // T5-2: 사용 중(취소 외 세션 참조) 교육자료는 세부항목 삭제 차단(서버에서도 TBM_409_055 로 거부)
  if (editLocked.value) {
    await proxy.$alert("이미 사용된 교육자료는 수정할 수 없습니다.");
    return;
  }

  // 체크된 행을 저장행(mtrlItemCd 존재)과 미저장 추가행(mtrlItemCd 없음)으로 분리
  const checkedRows = eduMtrlItemList.value.filter((item) => item?.chk);

  if (checkedRows.length === 0) {
    proxy.$alert(getMessage(MSG.DELETE_DATA_REQUIRED));
    return;
  }

  const savedRows = checkedRows.filter(
    (item) => !proxy.$util.isEmpty(item.mtrlItemCd)
  );
  const unsavedRows = checkedRows.filter((item) =>
    proxy.$util.isEmpty(item.mtrlItemCd)
  );

  const ok = await proxy.$confirm(getMessage(MSG.DELETE_CONFIRM), {
    variant: "danger",
  });
  if (!ok) return;

  // 미저장 추가행은 화면에서만 제거(서버 통신 없음)
  if (unsavedRows.length > 0) {
    eduMtrlItemList.value = eduMtrlItemList.value.filter(
      (item) => !unsavedRows.includes(item)
    );
  }

  // 저장행이 없으면 화면 삭제만 하고 종료
  if (savedRows.length === 0) {
    await proxy.$alert(getMessage(MSG.SAVE_SUCCESS));
    return;
  }

  try {
    const response = await axios.post(
      "/webApi/tbm01/delete-tbm-edu-item-infos",
      savedRows
    );

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
    // 저장 대상(체크된 행)만 검증. 인덱스 기반 접근은 부분 선택 시 엉뚱한 행을 검사하므로
    // 체크된 행 리스트 자체를 순회한다.
    const checkedItems = eduMtrlItemList.value.filter((item) => item.chk);
    if (checkedItems.length === 0) {
      proxy.$alert(getMessage(MSG.DELETE_DATA_REQUIRED));
      return false;
    }
    for (const target of checkedItems) {
      if(proxy.$util.isEmpty(target.mtrlItemType)) {
        proxy.$alert(getMessage(MSG.MATERIAL_TYPE_REQUIRED));
        return false;
      }
      if(proxy.$util.isEmpty(target.useYn)) {
        proxy.$alert(getMessage(MSG.USE_YN_REQUIRED));
        return false;
      }
      if(target.mtrlItemType === '01' || target.mtrlItemType === '02' || target.mtrlItemType === '04') {
        const file = target.file;
        const hasNewFile = file && typeof file === 'object' && file instanceof File && file.size > 0;
        const hasExistingFile = target.fileMgmtCd && target.filePath;
        if (!hasNewFile && !hasExistingFile) {
          proxy.$alert(getMessage(MSG.FILE_REQUIRED));
          return false;
        }
      }
      if(target.mtrlItemType === '03') {
        if(proxy.$util.isEmpty(target.url)) {
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

/** 서버 파일 URL — 서명 URL 전환: 서버가 발급한 서명 절대 URL(item.fileUrl)을 그대로 사용.
 *  (기존 baseUrl + filePath + fileMgmtCd + fileExt 수동 조립 제거.) 파일 없으면 빈 문자열. */
const getDownloadUrl = (item) => item?.fileUrl || '';

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
  // 타입 변경 시 AI 분석 지정 초기화(변경된 자료에 이전 지정이 남지 않도록 안전 기본값)
  item.aiAnalyzeYn = "N";
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
    siteNm.value = '';
  } else if (formData.scopeType === 'SITE' && proxy.$util.isEmpty(formData.siteCd)) {
    formData.siteCd = sessionStorage.getItem('gv_siteCd') || '';
    siteNm.value =
      resolveSiteNm(formData.siteCd) ||
      sessionStorage.getItem('gv_siteNm') ||
      '';
  }
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
  // 신규 추가행 기본값 보강: 빈문자열/타입 정합(서버 INSERT 시 누락 필드 방지)
  eduMtrlItemList.value.push({
    chk: true,
    mtrlItemType: '01',
    useYn: 'Y',
    sortIdx: eduMtrlItemList.value.length + 1,
    mtrlDesc: '',
    url: '',
    fileNm: '',
    fileMgmtCd: null,
    filePath: null,
    fileExt: null,
    file: null,
    // TBM_AI-RB-1: 신규 행 AI 분석 지정 기본값(저장 시 RA 영속)
    aiAnalyzeYn: 'N',
  });
}

</script>

<style scoped>
/* 사업장 조회: 사업장명(readonly) + 돋보기 버튼 (NoticeCreatePop 패턴) */
.site-search-field {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 18%;
}

.site-search-field input {
  flex: 1 1 auto;
  min-width: 0;
  cursor: pointer;
}

.site-search-field input:disabled {
  cursor: not-allowed;
}

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

/* T5-2: 사용 중 교육자료 수정 불가 안내 */
.lock-hint-row {
  display: flex;
  align-items: center;
  padding: 0.5rem 0.75rem;
  background: var(--color-warning-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--btn-radius);
}

.lock-hint {
  font-size: var(--btn-font-sm);
  color: var(--color-warning-text);
  font-weight: 600;
}

.content-wrapper {
  /* display: flex; */
  gap: 1rem;
  /* 상단 패딩 제거: 전역 .form-container padding(20px)과 겹쳐 헤더-안내문구 간격이 과했음(약 39px). */
  padding: 0 1.2rem 1.2rem;
  /* 상단에서 줄인 약 27px(패딩 1.2rem + form-container 상단 8px)만큼 본문 높이도 축소해
     하단에 생기던 빈 공간 제거 → 팝업 세로 길이도 그만큼 짧아진다. */
  height: calc(100% - 87px);
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

/* 헤더와 첫 요소(안내문구/스코프) 사이 간격 축소:
   전역 .prafta-modal-popup .form-container 의 상단 padding(20px)만 축소(좌우/하단은 유지). */
.content-wrapper .form-container {
  padding-top: 12px;
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

/* 세부항목 테이블 전용 툴바(우측 하단 정렬) */
.edu-grid-toolbar {
  display: flex;
  justify-content: flex-end;
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
