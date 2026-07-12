<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- PRAFTA-WEB_003 v3: AI 슬라이드 패널 포지셔닝 셸.
             ★modal-content-wide 에 position:relative 를 직접 주면 인라인 top/left 가 활성화되어
             팝업이 튀므로(useCenteredDraggable), 자식 셸에 position:relative 를 부여한다. -->
        <div class="risk-popup-shell">
        <!-- 🔹 Title  v-if="visible" -->
        <div class="modal-header" @mousedown="startDrag">
          <span>위험성 평가 정보</span>
          <button class="icon-button" @click="fnRequestClose">
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

        <!-- 메인 컨텐츠 -->
        <div class="risk-assess-content">
          <!-- 왼쪽: 개선 전 -->
          <div class="improvement-section before-section">
            <div class="section-header">개선 전</div>
            <div class="form-container">
              <div class="form-row">
                <label>작업명</label>
                <input v-model="formData.processNm" readonly />
              </div>
              <div class="form-row">
                <label>위험성구분</label>
                <input v-model="formData.processNm" readonly />
              </div>
              <div class="form-row">
                <label>위험성분류</label>
                <input v-model="formData.riskTypeNm" readonly />
              </div>
              <div class="form-row">
                <label>평가요청일자</label>
                <input v-model="formData.initAssessDate" readonly />
              </div>
              <div class="form-row">
                <label>평가요청자</label>
                <input v-model="formData.initAssessorNm" readonly />
              </div>
              <div class="form-row">
                <label>유해요인명</label>
                <input v-model="formData.hazardNm" readonly />
              </div>
              <div class="form-row">
                <label>유해요인설명</label>
                <textarea
                  v-model="formData.initDesc"
                  readonly
                  rows="3"
                ></textarea>
              </div>
              <div class="form-row">
                <label>사진</label>
                <div class="photo-container">
                  <img
                    v-if="beforePhotoUrl"
                    :src="beforePhotoUrl"
                    alt="개선 전 사진"
                    class="photo-preview"
                  />
                  <div v-else class="photo-placeholder">사진 없음</div>
                </div>
              </div>
              <div class="form-row risk-assessment">
                <label>개선 전 위험성 평가</label>
                <div class="risk-evaluation-group">
                  <div class="risk-input-item">
                    <label>빈도</label>
                    <select
                      v-model="formData.initLikelihoodScore"
                      :disabled="
                        props.readOnly ||
                        props.riskAssessmentData.assessmentStatus != '001'
                      "
                    >
                      <option value="">선택</option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                      <option value="5">5</option>
                    </select>
                  </div>
                  <div class="risk-input-item">
                    <label>강도</label>
                    <select
                      v-model="formData.initSeverityScore"
                      :disabled="
                        props.readOnly ||
                        props.riskAssessmentData.assessmentStatus != '001'
                      "
                    >
                      <option value="">선택</option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                    </select>
                  </div>
                  <div class="risk-input-item">
                    <label>위험도</label>
                    <div
                      class="risk-level-display"
                      :class="getRiskLevelClass(formData.initRiskLv)"
                    >
                      {{ formData.initRiskLv || "-" }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 오른쪽: 개선 후 -->
          <div class="improvement-section after-section">
            <div class="section-header section-header--with-action">
              개선 후
              <!-- PRAFTA-WEB_003 v3: 개선예정(002)에서만 AI 분석 진입 -->
              <button
                v-if="
                  !props.readOnly &&
                  formData.assessmentStatus === '002' &&
                  hasAiScope()
                "
                class="btn btn-report ai-open-btn"
                @click="fnOpenAiPanel"
              >
                AI 분석
              </button>
            </div>
            <div class="form-container">
              <div class="form-row">
                <label>진행상태</label>
                <select
                  v-model="formData.assessmentStatus"
                  name="combo"
                  :disabled="props.readOnly"
                >
                  <option
                    v-for="opt in (systCodeArr['SYS011'] || []).filter(
                      (item) => {
                        if (!proxy.$util.isNotEmpty(item.systValDCd))
                          return false;
                        // 지속개선대상(005)은 드롭다운에서 제외('지속개선대상 지정' 버튼으로만 진입)
                        if (item.systValDCd === '005') {
                          return false;
                        }
                        // props.assessmentStatus가 '001'이면 '001' 옵션 제외
                        if (
                          props.riskAssessmentData.assessmentStatus != '001' &&
                          item.systValDCd === '001'
                        ) {
                          return false;
                        }

                        if (
                          props.riskAssessmentData.assessmentStatus == '003' &&
                          (item.systValDCd == '001' ||
                            item.systValDCd == '002' ||
                            item.systValDCd == '004')
                        ) {
                          return false;
                        }

                        if (
                          props.riskAssessmentData.assessmentStatus == '004' &&
                          (item.systValDCd == '001' ||
                            item.systValDCd == '002' ||
                            item.systValDCd == '003')
                        ) {
                          return false;
                        }

                        return true;
                      }
                    )"
                    :key="opt.systValDCd"
                    :value="opt.systValDCd"
                  >
                    {{ opt.systValDNm }}
                  </option>
                </select>
              </div>
              <div
                class="form-row"
                v-if="
                  formData.assessmentStatus == '002' ||
                  formData.assessmentStatus == '003'
                "
              >
                <label>개선예정일자</label>
                <CalendarSrch
                  v-model="formData.revalDate"
                  :readonly="props.readOnly"
                />
              </div>
              <div
                class="form-row"
                v-if="
                  formData.assessmentStatus == '002' ||
                  formData.assessmentStatus == '003'
                "
              >
                <label>임시조치 내용</label>
                <textarea
                  v-model="formData.revalBeforeDesc"
                  placeholder="개선완료 전 임시조치 내용을 입력해 주세요"
                  rows="5"
                  maxlength="500"
                  :readonly="props.readOnly"
                ></textarea>
              </div>
              <div class="form-row" v-if="formData.assessmentStatus == '003'">
                <label>개선완료일자</label>
                <input v-model="formData.revalAssessDate" readonly />
              </div>
              <div class="form-row" v-if="formData.assessmentStatus == '003'">
                <label>개선관리자</label>
                <input v-model="formData.revalAssessorNm" readonly />
              </div>
              <div class="form-row" v-if="formData.assessmentStatus == '003'">
                <label>개선내용</label>
                <textarea
                  v-model="formData.revalDesc"
                  placeholder="개선 내용을 입력해 주세요"
                  rows="5"
                  :readonly="props.readOnly"
                ></textarea>
              </div>
              <div class="form-row" v-if="formData.assessmentStatus == '003'">
                <label>개선사진</label>
                <div class="file-upload-wrapper">
                  <input
                    type="file"
                    ref="fileInput"
                    accept="image/*"
                    @change="onFileSelected"
                    @input="onFileSelected"
                    style="display: none"
                  />
                  <button
                    v-if="!props.readOnly"
                    type="button"
                    :class="[
                      'upload-button',
                      { 'upload-button-small': previewImage || revalPhotoUrl },
                    ]"
                    @click="fileInput?.click()"
                    style="margin-left: -0.5px"
                  >
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke-width="1.5"
                      stroke="currentColor"
                      class="upload-icon"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5"
                      />
                    </svg>
                    <span>사진 업로드</span>
                  </button>
                  <div
                    v-if="previewImage || revalPhotoUrl"
                    class="uploaded-photo-container"
                  >
                    <div class="photo-container">
                      <img
                        :src="previewImage ? previewImage.url : revalPhotoUrl"
                        alt="개선 후 사진"
                        class="photo-preview"
                      />
                      <button
                        v-if="!props.readOnly"
                        type="button"
                        class="delete-photo-button"
                        @click="removePreview"
                        title="사진 삭제"
                      >
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          fill="none"
                          viewBox="0 0 24 24"
                          stroke-width="1.5"
                          stroke="currentColor"
                          class="delete-icon"
                        >
                          <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            d="M6 18L18 6M6 6l12 12"
                          />
                        </svg>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
              <div
                class="form-row risk-assessment"
                v-if="formData.assessmentStatus == '003'"
              >
                <label>개선 후 위험성 평가</label>
                <div class="risk-evaluation-group">
                  <div class="risk-input-item">
                    <label>빈도</label>
                    <select
                      v-model="formData.revalLikelihoodScore"
                      :disabled="props.readOnly"
                    >
                      <option value="">선택</option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                      <option value="5">5</option>
                    </select>
                  </div>
                  <div class="risk-input-item">
                    <label>강도</label>
                    <select
                      v-model="formData.revalSeverityScore"
                      :disabled="props.readOnly"
                    >
                      <option value="">선택</option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                    </select>
                  </div>
                  <div class="risk-input-item">
                    <label>위험도</label>
                    <div
                      class="risk-level-display"
                      :class="getRiskLevelClass(formData.revalRiskLv)"
                    >
                      {{ formData.revalRiskLv || "-" }}
                    </div>
                  </div>
                </div>
              </div>

              <!-- ▼ 참조 아차사고 (개선예정 002 부터 표시, 002 에서만 편집) -->
              <div class="form-row ref-nm-form-row" v-if="showReference">
                <label>참조 아차사고</label>
                <div class="ref-nm-block">
                  <!-- 검색/연결 버튼 (002 에서만) → 조회 팝업 -->
                  <div class="ref-nm-toolbar" v-if="canEditReference">
                    <button class="btn btn-report" @click="fnOpenRefSearch">
                      아차사고 검색/연결
                    </button>
                  </div>

                  <!-- 연결된 아차사고 테이블 (사고번호 | 장소 | 관리) -->
                  <div class="ref-nm-table-wrap">
                    <table class="ref-nm-table">
                      <thead>
                        <tr>
                          <th
                            class="ref-nm-table__del"
                            v-if="canEditReference"
                          ></th>
                          <th>사고번호</th>
                          <th>장소</th>
                          <th class="ref-nm-table__act">관리</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-if="displayLinks.length === 0">
                          <td
                            :colspan="canEditReference ? 4 : 3"
                            class="ref-nm-empty"
                          >
                            연결된 아차사고가 없습니다.
                          </td>
                        </tr>
                        <tr v-for="nm in displayLinks" :key="nm.nearMissId">
                          <td class="ref-nm-table__del" v-if="canEditReference">
                            <button
                              class="btn-x"
                              @click="fnRemoveLink(nm)"
                              title="연결 해제"
                            >
                              x
                            </button>
                          </td>
                          <td>{{ nm.nearMissId }}</td>
                          <td
                            class="ref-nm-table__loc"
                            :title="nm.locationDesc"
                          >
                            {{ nm.locationDesc }}
                          </td>
                          <td class="ref-nm-table__act">
                            <button
                              class="btn btn-cancel"
                              @click="fnOpenNearMissDetail(nm)"
                            >
                              상세
                            </button>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
              <!-- ▲ 참조 아차사고 -->
            </div>
          </div>
        </div>

        <!-- 푸터 버튼 -->
        <div class="modal-footer">
          <div class="footer-buttons-left">
            <button
              class="btn btn-report"
              v-if="['002', '003'].includes(formData.assessmentStatus)"
              @click="fnOpenImprovementPlan()"
            >
              개선실행계획서
            </button>
            <button
              class="btn btn-report"
              v-if="formData.assessmentStatus == '003'"
              @click="fnOpenImprovementReport()"
            >
              개선완료보고서
            </button>
          </div>
          <div class="footer-buttons-right">
            <button class="btn btn-cancel" @click="fnRequestClose">취소</button>
            <button
              class="btn btn-save"
              v-if="
                !props.readOnly &&
                !['003', '004', '005'].includes(
                  props.riskAssessmentData.assessmentStatus
                )
              "
              @click="fnSave()"
            >
              저장
            </button>
            <!-- T6-14C-hook-2: 검토요청(001)/개선예정(002)에서만 노출. 005로 지정 저장 -->
            <button
              class="btn btn-report"
              v-if="
                !props.readOnly &&
                ['001', '002'].includes(
                  props.riskAssessmentData.assessmentStatus
                )
              "
              @click="fnDesignateContinuous()"
            >
              지속개선대상 지정
            </button>
          </div>
        </div>

        <!-- PRAFTA-WEB_003 v3: AI 분석 슬라이드 패널(팝업 전체 오버레이 — 본체는 별도 SFC).
             ★Transition 을 부모에 둔다: v-if 언마운트가 부모에서 일어나므로 열림/닫힘 양방향
             슬라이드(위→아래 진입, 아래→위 퇴장)가 모두 재생된다. -->
        <Transition name="ai-slide">
          <RiskAiAnalysisPanel
            v-if="showAiPanel"
            :scope="aiScopeKeys()"
            :summary="{
              processNm: formData.processNm,
              riskTypeNm: formData.riskTypeNm,
              hazardNm: formData.hazardNm,
              initDesc: formData.initDesc,
            }"
            :photo-url="beforePhotoUrl || ''"
            :has-source-image="!!formData.initFileMgmtCd"
            @close="fnCloseAiPanel"
            @save="fnApplyAiSelection"
            @derived="fnMarkAiDirty"
            @touched="fnMarkAiDirty"
          />
        </Transition>
        </div>
        <!-- ▲ risk-popup-shell -->
      </div>
    </div>
  </Transition>
</template>

<script setup>
/* eslint-disable */
import {
  ref,
  watch,
  computed,
  nextTick,
  onMounted,
  onBeforeUnmount,
  defineProps,
  defineEmits,
  getCurrentInstance,
} from "vue";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { readFileAsBase64 } from "@/utils/fileUtil";
import { useModal } from "@/utils/useModal";
import NearMissInfo from "@/views/nearMiss/popup/NearMissInfo.vue";
import RefNearMissSearchPop from "@/views/risk/popup/RefNearMissSearchPop.vue";
import { getRiskLevelClass6, isVeryLow } from "@/utils/riskLevel";
import { printImprovementPlan } from "@/utils/print/riskImprovementPlanPrint";
import { printImprovementCompleteReport } from "@/utils/print/riskImprovementCompleteReportPrint";
import { buildFileServingUrl } from "@/utils/fileUrl";
// PRAFTA-WEB_003 v3: AI 분석 슬라이드 패널(본체는 별도 SFC)
import RiskAiAnalysisPanel from "@/views/risk/popup/RiskAiAnalysisPanel.vue";

const systCodeArr = ref([]);
const fileInput = ref(null);
const previewImage = ref(null); // { file, url }

const { proxy } = getCurrentInstance();

onMounted(async () => {
  init();
  await fnGetSystinfoList();
  // prafta-054-5: 연결된 참조 아차사고 목록 초기 로드
  await fnLoadLinkedNearMiss();
  // PRAFTA-WEB_003 v3(commit-on-save): 오픈 시 이전 세션의 미확정(SAVED_YN='N') AI 작업분을 자동 정리.
  //   닫기 시 삭제가 실패했거나 브라우저 비정상 종료로 잔존한 행을 회수한다(best-effort — 오픈을 막지 않음).
  //   TBM 열람(readOnly)에서는 호출하지 않는다.
  if (!props.readOnly && hasAiScope()) {
    fnDiscardUnsavedAi().catch((err) => {
      console.warn("AI 미저장 작업분 정리 실패(오픈은 진행)", err);
    });
  }
});

const init = () => {
  const revalDate = props.riskAssessmentData.revalDate;

  if (proxy.$util.isEmpty(revalDate)) {
    formData.value.revalDate = proxy.$util.getToday();
  }
};

// API 호출
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS011"],
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

      // if (systCodeArr.value.SYS011 && systCodeArr.value.SYS011.length > 0) {
      //   formData.assessmentStatus = systCodeArr.value.SYS011[0].systValDCd;
      // }
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

const props = defineProps({
  riskAssessmentData: {
    type: Object,
    default: () => ({}),
  },
  onSave: {
    type: Function,
    default: null,
  },
  // 읽기전용 모드(TBM 콘솔 등 열람 전용 진입). true 면 편집/저장/지정 비활성.
  readOnly: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["close", "save"]);

const { open: openPop } = useModal();

// 참조 아차사고 상태 (보류-저장 모델: DB 반영은 평가 저장 시 일괄)
const linkedNearMissList = ref([]); // DB 연결 기존 목록(USE_YN='Y')
const pendingLinks = ref([]); // 검색팝업에서 새로 선택(미반영) nearMiss 객체들
const pendingUnlinkIds = ref([]); // 기존 연결 중 해제 예약된 nearMissId

// 화면 표시 목록 = 기존(해제예약 제외) + 신규(보류). nearMissId 중복 제거.
const displayLinks = computed(() => {
  const result = [];
  const seen = new Set();
  linkedNearMissList.value.forEach((nm) => {
    if (pendingUnlinkIds.value.includes(nm.nearMissId)) return;
    if (seen.has(nm.nearMissId)) return;
    seen.add(nm.nearMissId);
    result.push(nm);
  });
  pendingLinks.value.forEach((nm) => {
    if (seen.has(nm.nearMissId)) return;
    seen.add(nm.nearMissId);
    result.push(nm);
  });
  return result;
});

// 참조 아차사고 편집(검색/연결/삭제) 가능 조건:
//   - 화면 드롭다운(formData)의 현재 진행상태가 개선예정(002)/개선완료(003)일 때 노출
//     (개선예정 레코드에서 드롭다운을 개선완료로 바꿔 저장 전이면 그대로 편집 가능)
//   - 단, 평가 정보 자체(원본 레코드)가 확정 상태(개선완료003/미처리대상004/지속개선대상005)면
//     드롭다운 표시값과 무관하게 잠금(이미 확정된 평가는 참조 편집 불가).
const canEditReference = computed(
  () =>
    !props.readOnly &&
    ["002", "003"].includes(formData.value.assessmentStatus) &&
    !["003", "004", "005"].includes(props.riskAssessmentData.assessmentStatus)
);

// 참조 아차사고 영역 표시(002 부터). 005 지속개선대상도 표시(편집은 canEditReference 로 잠금).
const showReference = computed(() =>
  ["002", "003", "004", "005"].includes(formData.value.assessmentStatus)
);

// 연계 API 호출 공통 키 (평가건 PK 3축 + 사업장)
const refLinkKeys = () => ({
  siteCd: props.riskAssessmentData.siteCd,
  processCd: props.riskAssessmentData.processCd,
  assessmentCd: props.riskAssessmentData.assessmentCd,
});

// 아차사고 검색/연결 조회 팝업 열기 (보류-저장: 체크 항목 일괄 수신)
const fnOpenRefSearch = () => {
  openPop(RefNearMissSearchPop, {
    ...refLinkKeys(),
    preselectedIds: displayLinks.value.map((nm) => nm.nearMissId),
    onApply: fnApplyPending,
  });
};

// 검색팝업에서 선택된 아차사고들을 보류 목록에 병합
const fnApplyPending = (selectedList) => {
  (selectedList || []).forEach((nm) => {
    // 해제 예약돼 있던 기존 연결이면 예약 취소
    const uIdx = pendingUnlinkIds.value.indexOf(nm.nearMissId);
    if (uIdx >= 0) {
      pendingUnlinkIds.value.splice(uIdx, 1);
      return;
    }
    const exists =
      linkedNearMissList.value.some((x) => x.nearMissId === nm.nearMissId) ||
      pendingLinks.value.some((x) => x.nearMissId === nm.nearMissId);
    if (!exists) pendingLinks.value.push(nm);
  });
};

// 연결 삭제(보류). 신규 보류면 목록에서만 제거, 기존 DB 연결이면 해제 예약.
const fnRemoveLink = (nm) => {
  const pIdx = pendingLinks.value.findIndex(
    (x) => x.nearMissId === nm.nearMissId
  );
  if (pIdx >= 0) {
    pendingLinks.value.splice(pIdx, 1);
    return;
  }
  if (!pendingUnlinkIds.value.includes(nm.nearMissId)) {
    pendingUnlinkIds.value.push(nm.nearMissId);
  }
};

// 보류된 참조 아차사고 변경(해제→연결)을 기존 EP 로 순차 반영(평가 저장 성공 후 호출).
const fnFlushPendingRefLinks = async () => {
  for (const id of pendingUnlinkIds.value) {
    await axios.post(
      "/webApi/risklink01/unlink",
      { ...refLinkKeys(), nearMissId: id },
      { headers: { "Content-Type": "application/json" } }
    );
  }
  for (const nm of pendingLinks.value) {
    await axios.post(
      "/webApi/risklink01/link",
      { ...refLinkKeys(), nearMissId: nm.nearMissId },
      { headers: { "Content-Type": "application/json" } }
    );
  }
  // 반영 후 보류 상태 초기화
  pendingUnlinkIds.value = [];
  pendingLinks.value = [];
};

// 연결된 아차사고 목록 조회 (USE_YN='Y'). onMounted 에서도 1회 호출
const fnLoadLinkedNearMiss = async () => {
  try {
    const response = await axios.get("/webApi/risklink01/linked-near-miss", {
      params: refLinkKeys(),
    });

    if (response.status === 200) {
      linkedNearMissList.value = response.data?.nearMissList || [];
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 연결 아차사고 상세 열람(읽기전용). 기존 NearMissInfo 팝업 재사용(자체 incident-info 조회)
const fnOpenNearMissDetail = (nm) => {
  openPop(NearMissInfo, {
    nearMissData: {
      cmpnyCd: props.riskAssessmentData.cmpnyCd || "",
      siteCd: props.riskAssessmentData.siteCd || "",
      nearMissId: nm.nearMissId || "",
    },
  });
};

const modalRef = ref(null);
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

// 폼 데이터
const formData = ref({
  // 기본 정보
  cmpnyCd: "",
  siteCd: "",
  processCd: "",
  processNm: "",
  riskTypeCd: "",
  riskTypeNm: "",
  hazardCd: "",
  hazardNm: "",
  assessmentCd: "",
  assessmentStatus: "",
  assessmentStatusNm: "",

  // 초기 평가 정보
  initLikelihoodScore: "",
  initSeverityScore: "",
  initRiskLv: "",
  initDesc: "",
  initAssessorId: "",
  initAssessorNm: "",
  initAssessDate: "",
  initFileMgmtCd: "",
  initFilePath: "",

  // 재평가 정보
  revalDate: "",
  revalBeforeDesc: "",
  revalRiskLv: "",
  revalLikelihoodScore: "",
  revalSeverityScore: "",
  revalAssessorId: "",
  revalAssessorNm: "",
  revalFileMgmtCd: "",
  revalFilePath: "",
  revalAssessDate: "",

  // 개선 전 사진
  beforePhoto: "",

  // 개선 후 정보
  revalDesc: "",
  afterFrequency: "",
  afterIntensity: "",
});

// 개선 전 사진 URL 생성 (initFilePath + initFileMgmtCd)
//   서빙 URL 조립은 공용 유틸(buildFileServingUrl)로 일원화. fileMgmtCd 는 확장자 포함명.
const beforePhotoUrl = computed(
  () =>
    buildFileServingUrl(
      formData.value.initFilePath,
      formData.value.initFileMgmtCd
    ) || null
);

// 개선 후 사진 URL 생성 (revalFilePath + revalFileMgmtCd)
const revalPhotoUrl = computed(
  () =>
    buildFileServingUrl(
      formData.value.revalFilePath,
      formData.value.revalFileMgmtCd
    ) || null
);

// 위험도 계산 (빈도 × 강도)
const calculateRiskLevel = (frequency, intensity) => {
  if (!frequency || !intensity) return "";
  return String(Number(frequency) * Number(intensity));
};

watch(
  () => formData.value.assessmentStatus,
  (newVal) => {
    if (newVal == "003") {
      if (proxy.$util.isEmpty(formData.value.revalAssessorId)) {
        formData.value.revalAssessorId = sessionStorage.getItem("gv_userCd");
        formData.value.revalAssessorNm = sessionStorage.getItem("gv_userNm");
      }

      if (proxy.$util.isEmpty(formData.value.revalLikelihoodScore)) {
        formData.value.revalLikelihoodScore =
          formData.value.initLikelihoodScore;
      }

      if (proxy.$util.isEmpty(formData.value.revalSeverityScore)) {
        formData.value.revalSeverityScore = formData.value.initSeverityScore;
      }
    } else {
      /* 개선완료 관련 데이터 초기화화 */
      // formData.value.revalAssessorId = '';
      // formData.value.revalAssessorNm = '';
      // formData.value.revalLikelihoodScore = '';
      // formData.value.revalSeverityScore = '';
      // formData.value.revalRiskLv = '';
      // formData.value.afterPhoto = '';
      // if (fileInput.value) {
      //   fileInput.value.value = ''; // 파일 입력 초기화
      // }
    }
  }
);

// 개선 전 빈도/강도 변경 감시
watch(
  () => [formData.value.initLikelihoodScore, formData.value.initSeverityScore],
  ([freq, intensity]) => {
    formData.value.initRiskLv = calculateRiskLevel(freq, intensity);
  }
);

// 개선 후 빈도/강도 변경 감시
watch(
  () => [
    formData.value.revalLikelihoodScore,
    formData.value.revalSeverityScore,
  ],
  ([freq, intensity]) => {
    formData.value.revalRiskLv = calculateRiskLevel(freq, intensity);
  }
);

// props 데이터가 변경되면 폼 데이터 업데이트
watch(
  () => props.riskAssessmentData,
  (newData) => {
    if (newData) {
      formData.value = {
        // 기본 정보
        cmpnyCd: newData.cmpnyCd || "",
        siteCd: newData.siteCd || "",
        processCd: newData.processCd || "",
        processNm: newData.processNm || "",
        riskTypeCd: newData.riskTypeCd || "",
        riskTypeNm: newData.riskTypeNm || "",
        hazardCd: newData.hazardCd || "",
        hazardNm: newData.hazardNm || "",
        assessmentCd: newData.assessmentCd || "",
        assessmentStatus: newData.assessmentStatus || "",
        assessmentStatusNm: newData.assessmentStatusNm || "",
        // 초기 평가 정보
        initLikelihoodScore: newData.initLikelihoodScore || "",
        initSeverityScore: newData.initSeverityScore || "",
        initRiskLv: newData.initRiskLv || "",
        initDesc: newData.initDesc || "",
        initAssessorId: newData.initAssessorId || "",
        initAssessorNm: newData.initAssessorNm || "",
        initAssessDate: newData.initAssessDate || "",
        initFileMgmtCd: newData.initFileMgmtCd || "",
        initFilePath: newData.initFilePath || "",
        // 재평가 정보
        revalDate: newData.revalDate || "",
        revalBeforeDesc: newData.revalBeforeDesc || "",
        revalLikelihoodScore: newData.revalLikelihoodScore || "",
        revalSeverityScore: newData.revalSeverityScore || "",
        revalRiskLv: newData.revalRiskLv || "",
        revalAssessorId: newData.revalAssessorId || "",
        revalAssessorNm: newData.revalAssessorNm || "",

        revalFileMgmtCd: newData.revalFileMgmtCd || "",
        revalFilePath: newData.revalFilePath || "",
        revalAssessDate: newData.revalAssessDate || "",
        // 개선 전 사진
        beforePhoto: newData.beforePhoto || "",
        // 개선 후 정보
        revalDesc: newData.revalDesc || "",
      };
    }
  },
  { immediate: true }
);

// 위험도에 따른 클래스 반환(6단계 공용 유틸 위임 — Risk_02 권위 기준 일원화)
const getRiskLevelClass = (riskLevel) => getRiskLevelClass6(riskLevel);

// 이미지 파일 확인 함수
const isImageFile = (file) =>
  file && typeof file.type === "string" && file.type.startsWith("image/");

// 파일 선택 콜백
const onFileSelected = (evt) => {
  try {
    const input = evt?.target;
    const file = input?.files?.[0];

    if (!file) return;

    if (!isImageFile(file)) {
      proxy.$alert(getMessage(MSG.IMAGE_ONLY));
      if (input) input.value = "";
      return;
    }

    // 기존 프리뷰/URL 정리
    if (previewImage.value?.url) {
      URL.revokeObjectURL(previewImage.value.url);
    }

    const url = URL.createObjectURL(file);
    previewImage.value = { file, url };

    // 같은 파일 재선택 허용
    if (input) input.value = "";
  } catch (err) {
    const msg =
      err?.response?.data?.message ||
      err?.message ||
      "파일을 읽는 중 오류가 발생했습니다.";

    proxy.$alert(msg);
  }
};

// 프리뷰 삭제
const removePreview = async () => {
  try {
    if (previewImage.value?.url) {
      URL.revokeObjectURL(previewImage.value.url);
    }
    previewImage.value = null;
    if (fileInput.value) {
      fileInput.value.value = "";
    }
  } catch (e) {
    const msg =
      e?.response?.data?.message ||
      e?.message ||
      "이미지 삭제 중 오류가 발생했습니다.";

    await proxy.$alert(msg);
  }
};

// 파일명 생성 함수
// const buildFileName = (prefix, originalName = 'photo.jpg') => {
//   const ts = new Date().toISOString().replace(/[:.]/g, '');
//   const safe = String(originalName).replace(/[^\w.\-]+/g, '_');
//   return `${prefix}_${ts}_${safe}`;
// };

// T6-14C-hook-2: 지속개선대상(005) 지정 저장. 기존 save-assessments 재사용.
const fnDesignateContinuous = async () => {
  // PRAFTA_COM_001_T6 Low-6: 보류중 참조 아차사고 변경이 남아 있으면 먼저 저장하도록 안내하고 중단한다.
  //   005 지정 후에는 참조 편집이 불가하므로, 미저장 변경이 후속 반영(422)에서 유실되는 혼란을 방지한다.
  if (
    (pendingLinks.value && pendingLinks.value.length > 0) ||
    (pendingUnlinkIds.value && pendingUnlinkIds.value.length > 0)
  ) {
    await proxy.$alert("참조 아차사고 변경을 먼저 저장한 뒤 지정해 주세요.");
    return;
  }

  const ok = await proxy.$confirm(
    "이 항목을 지속개선대상으로 지정할까요? 지정 후에는 수정할 수 없습니다."
  );
  if (!ok) return;
  formData.value.assessmentStatus = "005";
  // 이미 지정 확인을 받았으므로 fnSave 의 저장 확인창은 생략한다(중복 Confirm 방지).
  await fnSave({ skipConfirm: true });
};

// 저장 처리
const fnSave = async ({ skipConfirm = false } = {}) => {
  // T6-14B-1: 개선완료(003) 저장은 개선 후 위험도가 "매우낮음"(1~3)일 때만 허용
  if (
    formData.value.assessmentStatus === "003" &&
    !isVeryLow(formData.value.revalRiskLv)
  ) {
    await proxy.$alert(
      '개선완료는 개선 후 위험도가 "매우낮음"(1~3)일 때만 저장할 수 있습니다.'
    );
    return;
  }

  // varchar(500) 백스톱: 조립 가드/maxlength 를 우회한 초과값 방어.
  //   initDesc 검사는 AI 유해요인을 실제 반영해 전송하는 경우(aiHazardApplied)에만 수행 —
  //   전송하지도 않는 필드(readonly 원본) 길이로 저장을 막지 않는다(사용자 해소 불가 차단 방지).
  //   revalBeforeDesc 는 항상 전송되므로 상시 검사.
  if (
    (aiHazardApplied.value &&
      (formData.value.initDesc || "").length > DESC_MAX_LEN) ||
    (formData.value.revalBeforeDesc || "").length > DESC_MAX_LEN
  ) {
    await proxy.$alert(
      `유해요인설명/임시조치 내용은 최대 ${DESC_MAX_LEN}자까지 저장할 수 있습니다.`
    );
    return;
  }

  if (!skipConfirm) {
    const ok = await proxy.$confirm(getMessage(MSG.SAVE_CONFIRM));
    if (!ok) return;
  }

  try {
    const requestBody = {
      siteCd: props.riskAssessmentData.siteCd,
      assessmentCd: props.riskAssessmentData.assessmentCd,
      assessmentStatus: formData.value.assessmentStatus,
      processCd: props.riskAssessmentData.processCd,
      initLikelihoodScore: formData.value.initLikelihoodScore || 0,
      initSeverityScore: formData.value.initSeverityScore || 0,
      initRiskLv: formData.value.initRiskLv || 0,
      // WEB_003 저장 액션: AI 유해요인을 실제 반영한 경우에만 initDesc 전송.
      //   미포함 시 BE 는 null 수신 → mapper <if>가 스킵해 INIT_DESC 원본 보존
      //   (근로자가 앱으로 갱신한 값을 팝업 로드 시점 스냅샷으로 되돌리는 사고 방지).
      ...(aiHazardApplied.value ? { initDesc: formData.value.initDesc } : {}),
      revalDate: formData.value.revalDate,
      revalBeforeDesc: formData.value.revalBeforeDesc,
      revalLikelihoodScore: formData.value.revalLikelihoodScore || 0,
      revalSeverityScore: formData.value.revalSeverityScore || 0,
      revalRiskLv: formData.value.revalRiskLv || 0,
      revalDesc: formData.value.revalDesc,
    };

    if (previewImage.value?.file) {
      requestBody.itemBase64 = await readFileAsBase64(previewImage.value.file);
      requestBody.itemOriginalFilename = previewImage.value.file.name;
    }

    const response = await axios.post(
      "/webApi/risk03/save-assessments",
      requestBody,
      { headers: { "Content-Type": "application/json" } }
    );
    if (response.status === 200) {
      // 평가 저장 성공 후 참조 아차사고 보류 변경을 일괄 반영(해제 → 연결 순).
      //   기존 /risklink01/unlink·/link EP 재사용(신규 batch EP 없음).
      //   편집은 002 에서만 가능하므로 003 저장 시점엔 pending 이 비어 있음.
      await fnFlushPendingRefLinks();

      // PRAFTA-WEB_003 v3(commit-on-save): 이번 세션에 AI 를 건드렸으면(aiDirty) 평가 저장 성공 후
      //   AI 도출 결과를 SAVED_YN='Y' 로 확정한다. aiDirty=false(AI 미사용)면 호출하지 않는다.
      //   readOnly(TBM 열람) 진입에서는 확정도 호출하지 않는다.
      if (!props.readOnly && aiDirty.value) {
        try {
          await axios.post(
            "/webApi/riskai01/commit",
            { ...aiScopeKeys() },
            { headers: { "Content-Type": "application/json" } }
          );
        } catch (err) {
          console.warn("AI 분석 결과 확정 실패(저장은 완료)", err);
          await proxy.$alert(
            "평가는 저장됐지만 AI 분석 결과 확정에 실패했습니다. 팝업을 다시 열면 AI 분석 내용이 초기화됩니다."
          );
        }
      }
      // 확정 성공/실패 무관하게 미저장 닫기 가드 해제 — 확정 실패분은 다음 오픈에서 자동 정리되므로
      //   닫기 시 중복 확인창(fnRequestClose)을 띄우지 않는다.
      aiDirty.value = false;

      proxy.$alert(getMessage(MSG.SAVE_COMPLETED));
      if (props.onSave && typeof props.onSave === "function") {
        props.onSave(response.data);
      }
      emit("close");
    } else {
      const msg = response.data?.message || "저장 중 오류가 발생했습니다.";
      await proxy.$alert(msg);
    }
  } catch (err) {
    const msg = resolveApiErrorMessage(err, "저장 중 오류가 발생했습니다.");
    await proxy.$alert(msg);
  }
};

// 진행상태 이름 가져오기
const getAssessmentStatusName = (statusCd) => {
  if (!statusCd || !systCodeArr.value["SYS011"]) return "";
  const status = systCodeArr.value["SYS011"].find(
    (item) => item.systValDCd === statusCd
  );
  return status ? status.systValDNm : "";
};

// 개선실행계획서 열기
const fnOpenImprovementPlan = () => {
  // 개선실행계획서 인쇄 HTML 빌더는 공용 유틸로 추출(@/utils/print/riskImprovementPlanPrint).
  //   동작/출력은 기존과 1:1 동일. 진행상태명/개선 전 사진 URL 은 기존 방식대로 산출해 전달한다.
  const opened = printImprovementPlan({
    processNm: formData.value.processNm,
    riskTypeNm: formData.value.riskTypeNm,
    initAssessDate: formData.value.initAssessDate,
    initAssessorNm: formData.value.initAssessorNm,
    hazardNm: formData.value.hazardNm,
    initDesc: formData.value.initDesc,
    initLikelihoodScore: formData.value.initLikelihoodScore,
    initSeverityScore: formData.value.initSeverityScore,
    initRiskLv: formData.value.initRiskLv,
    assessmentStatus: formData.value.assessmentStatus,
    assessmentStatusName: getAssessmentStatusName(
      formData.value.assessmentStatus
    ),
    revalDate: formData.value.revalDate,
    revalBeforeDesc: formData.value.revalBeforeDesc,
    beforePhotoUrl: beforePhotoUrl.value,
  });
  if (!opened) {
    proxy.$alert(getMessage(MSG.POPUP_BLOCKED));
  }
};

// 개선완료보고서 열기
//   인쇄 HTML 빌더는 공용 유틸로 추출(@/utils/print/riskImprovementCompleteReportPrint).
//   동작/출력은 기존 인라인 빌더와 1:1 동일. 사고 일괄출력(AcctSafetyPrintPop)과 단일 출처 공유.
const fnOpenImprovementReport = () => {
  const opened = printImprovementCompleteReport({
    processNm: formData.value.processNm,
    riskTypeNm: formData.value.riskTypeNm,
    initAssessDate: formData.value.initAssessDate,
    initAssessorNm: formData.value.initAssessorNm,
    hazardNm: formData.value.hazardNm,
    initDesc: formData.value.initDesc,
    initLikelihoodScore: formData.value.initLikelihoodScore,
    initSeverityScore: formData.value.initSeverityScore,
    initRiskLv: formData.value.initRiskLv,
    assessmentStatusName: getAssessmentStatusName(
      formData.value.assessmentStatus
    ),
    revalDate: formData.value.revalDate,
    revalBeforeDesc: formData.value.revalBeforeDesc,
    revalAssessorNm: formData.value.revalAssessorNm,
    revalAssessDate: formData.value.revalAssessDate,
    revalDesc: formData.value.revalDesc,
    revalLikelihoodScore: formData.value.revalLikelihoodScore,
    revalSeverityScore: formData.value.revalSeverityScore,
    revalRiskLv: formData.value.revalRiskLv,
    beforePhotoUrl: beforePhotoUrl.value,
    // 개선 후 사진: 방금 업로드한 미저장 프리뷰(blob:)가 있으면 우선 사용(기존 동작 유지)
    afterPhotoUrl: previewImage?.value?.url || revalPhotoUrl.value,
  });
  if (!opened) {
    proxy.$alert(getMessage(MSG.POPUP_BLOCKED));
  }
};

// ==========================================================================
// PRAFTA-WEB_003 v3: AI 분석 슬라이드 패널 (본체는 RiskAiAnalysisPanel.vue)
// ==========================================================================
const showAiPanel = ref(false);

// AI 분석 미저장 dirty 플래그: 이번 팝업 세션에서 도출 수행 또는 결과 반영 후 아직 평가 저장 전이면 true
const aiDirty = ref(false);

// AI 유해요인 반영 여부: fnApplyAiSelection 에서 hazardLines 를 실제 반영했을 때만 true.
//   fnSave 는 이 값이 true 일 때만 initDesc 를 전송(미포함 시 BE mapper <if>가 스킵해
//   근로자가 그 사이 앱으로 갱신한 INIT_DESC 원본을 스냅샷으로 되돌리지 않는다).
const aiHazardApplied = ref(false);

// 설명 필드 최대 글자수 — TB_RISK_ASSESSMENT.INIT_DESC / REVAL_BEFORE_DESC varchar(500) 근거.
//   초과 시 MySQL strict 모드에서 저장 전체가 실패하므로 FE 에서 선차단한다.
const DESC_MAX_LEN = 500;

// AI 반영 블록 시작 마커(재저장 시 마커부터 끝까지 교체 — 중복 축적 방지.
//   BE RiskAi01ServiceImpl.stripAiAppendix 의 마커와 동일 문자열 유지 필수)
const AI_HAZARD_MARKER = "[AI분석 유해요인]";
const AI_MEASURE_MARKER = "[AI분석 개선안]";

// AI 반영 텍스트 조립: 기존 텍스트에 마커가 이미 있으면 마커 위치부터 끝까지 제거(직전 공백/개행 trim)
//   → 남은 원본 뒤에 "마커 + 넘버링 목록" 블록을 덧붙인다(원본이 비면 블록만).
const buildAiAppendedText = (baseText, marker, lines) => {
  let base = baseText || "";
  const pos = base.indexOf(marker);
  if (pos >= 0) {
    base = base.slice(0, pos).replace(/\s+$/, "");
  }
  const numbered = lines
    .map((line, i) => String(i + 1) + ". " + line)
    .join("\n");
  const block = marker + "\n" + numbered;
  return base ? base + "\n" + block : block;
};

// 패널에서 도출 수행 알림(@derived) — 미저장 닫기 가드용 dirty 마킹
const fnMarkAiDirty = () => {
  aiDirty.value = true;
};

// 패널 [저장](@save): 선택된 유해요인 → 유해요인설명(initDesc), 개선안 → 임시조치 내용(revalBeforeDesc)에
//   조립 반영 후 패널을 닫고 팝업으로 복귀. 한쪽 배열이 비면 그 필드는 건드리지 않는다(기존 AI 블록 유지).
const fnApplyAiSelection = async (payload) => {
  const hazardLines = payload?.hazardLines || [];
  const measureLines = payload?.measureLines || [];

  // 조립 결과를 먼저 계산(반영 전) — varchar(500) 초과 시 all-or-nothing 으로 둘 다 반영하지 않는다
  const nextInitDesc =
    hazardLines.length > 0
      ? buildAiAppendedText(
          formData.value.initDesc,
          AI_HAZARD_MARKER,
          hazardLines
        )
      : null;
  const nextRevalBeforeDesc =
    measureLines.length > 0
      ? buildAiAppendedText(
          formData.value.revalBeforeDesc,
          AI_MEASURE_MARKER,
          measureLines
        )
      : null;

  const overFields = [];
  if (nextInitDesc !== null && nextInitDesc.length > DESC_MAX_LEN) {
    overFields.push(`유해요인설명 ${nextInitDesc.length}자`);
  }
  if (
    nextRevalBeforeDesc !== null &&
    nextRevalBeforeDesc.length > DESC_MAX_LEN
  ) {
    overFields.push(`임시조치 내용 ${nextRevalBeforeDesc.length}자`);
  }
  if (overFields.length > 0) {
    // 패널을 닫지 않고 유지 — 관리자가 패널에서 선택 항목을 줄여 재시도할 수 있게 한다.
    //   aiDirty 도 여기서는 설정하지 않는다(derived 이벤트로 이미 true 일 수 있음 — 그대로 둠).
    await proxy.$alert(
      `반영 결과가 최대 글자수(${DESC_MAX_LEN}자)를 초과하여 반영할 수 없습니다.\n` +
        `(${overFields.join(", ")} / 한도 ${DESC_MAX_LEN}자)\n` +
        "선택 항목을 줄인 뒤 다시 저장해 주세요."
    );
    return;
  }

  if (nextInitDesc !== null) {
    formData.value.initDesc = nextInitDesc;
    aiHazardApplied.value = true;
  }
  if (nextRevalBeforeDesc !== null) {
    formData.value.revalBeforeDesc = nextRevalBeforeDesc;
  }
  aiDirty.value = true;
  showAiPanel.value = false;
};

// 미저장(SAVED_YN='N') AI 도출 행 정리(commit-on-save). 오픈/닫기 공용.
//   확정(Y) 행은 서버가 보존한다. 실패는 호출부에서 처리(오픈=경고만, 닫기=사용자 알림).
const fnDiscardUnsavedAi = async () => {
  await axios.post(
    "/webApi/riskai01/discard-unsaved",
    { ...aiScopeKeys() },
    { headers: { "Content-Type": "application/json" } }
  );
};

// 미저장 닫기 가드: AI 분석 dirty 상태면 확인 후 서버의 미확정(SAVED_YN='N') 도출 행을 정리하고 닫는다.
//   fnSave 성공 경로의 emit("close")는 저장 완료라 이 가드를 타지 않는다.
const fnRequestClose = async () => {
  if (!aiDirty.value) {
    emit("close");
    return;
  }
  const ok = await proxy.$confirm(
    "AI 분석 결과가 저장되지 않았습니다. 팝업을 닫으면 AI 분석 내용이 초기화됩니다. 닫을까요?"
  );
  if (!ok) return;
  // 미확정 작업분 삭제(commit-on-save). 실패해도 닫기는 진행하되, 조용히 묻지 않고 사용자에게 알린다
  //   (다음 오픈에서 자동 정리되므로 데이터 잔존은 일시적 — 진단 가능성 확보).
  try {
    await fnDiscardUnsavedAi();
  } catch (err) {
    console.warn("AI 분석 초기화 실패(닫기는 진행)", err);
    await proxy.$alert(
      "AI 분석 내용 초기화에 실패했습니다. 팝업을 다시 열면 자동으로 정리됩니다."
    );
  }
  emit("close");
};

// 평가건 스코프 키(3축). CMPNY_CD 는 서버가 JWT 로만 도출하므로 바디에 싣지 않는다.
const aiScopeKeys = () => ({
  siteCd: props.riskAssessmentData.siteCd,
  processCd: props.riskAssessmentData.processCd,
  assessmentCd: props.riskAssessmentData.assessmentCd,
});

// 스코프 키가 모두 존재하는지(신규/미확정 레코드 방어)
const hasAiScope = () => {
  const k = aiScopeKeys();
  return !!(k.siteCd && k.processCd && k.assessmentCd);
};

const fnOpenAiPanel = () => {
  if (!hasAiScope()) return;
  showAiPanel.value = true;
};

const fnCloseAiPanel = () => {
  showAiPanel.value = false;
};

// 정리
onBeforeUnmount(() => {
  if (previewImage.value?.url) {
    URL.revokeObjectURL(previewImage.value.url);
  }
});
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content-wide {
  width: 95vw;
  max-width: 1600px;
  height: 92vh;
  max-height: 92vh;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
}

/* 헤더/푸터는 항상 고정 노출(줄어들지 않도록) */
.modal-header,
.modal-footer {
  flex-shrink: 0;
}

.risk-assess-content {
  display: flex;
  flex: 1;
  overflow: hidden;
  padding: 1.5rem;
  gap: 1.5rem;
  min-height: 0;
}

.improvement-section {
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.section-header {
  background: #f5f5f5;
  padding: 0.75rem 1rem;
  font-weight: bold;
  border-bottom: 1px solid #ddd;
}

.form-container {
  flex: 1;
  padding: 1rem;
  /* display: flex; */
  flex-direction: column;
  gap: 1rem;
  overflow-y: auto;
  overflow-x: hidden;
  min-height: 0;
  align-items: flex-start;
  text-align: left;
}

.form-row {
  display: flex !important;
  align-items: center !important;
  gap: 0.5rem;
  text-align: left;
  margin-bottom: 0.5rem;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.form-row label {
  flex: 0 0 120px;
  font-weight: 500;
  font-size: 0.9rem;
  text-align: left;
  color: #333;
}

.form-row input,
.form-row select,
.form-row textarea {
  flex: 1 1 0%;
  min-width: 0;
  padding: 0.4rem 0.6rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.9rem;
  text-align: left;
  box-sizing: border-box;
}

.form-row input:read-only {
  background: #f5f5f5;
  cursor: not-allowed;
}

.form-row textarea {
  resize: vertical;
  min-height: 100px;
}

.photo-row {
  flex-direction: column !important;
  align-items: center !important;
  flex: 1;
  min-height: 200px;
}

.photo-row label {
  flex: 0 0 auto;
  margin-bottom: 0.5rem;
  /* align-self: flex-start; */
}

/* .photo-row .photo-container {
  width: 80%;
  max-width: 80%;
  min-width: 80%;
} */

.photo-container {
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 4px;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  background: #f9f9f9;
  min-height: 200px;
  padding: 0.5rem;
  position: relative;
}

.photo-container .photo-placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.photo-preview {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.photo-placeholder {
  color: #999;
  font-size: 0.9rem;
  max-width: 100%;
  max-height: 100%;
}

.risk-assessment {
  margin-top: 0;
}

.risk-evaluation-group {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex: 1 1 auto;
}

.risk-input-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 0 0 auto;
}

.risk-input-item label {
  font-size: 0.85rem;
  font-weight: 500;
  flex: 0 0 auto;
  white-space: nowrap;
  color: #333;
}

.risk-input-item select {
  padding: 0.4rem 0.6rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.9rem;
  text-align: left;
  box-sizing: border-box;
  min-width: 80px;
}

.risk-level-display {
  padding: 0.4rem 0.6rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  text-align: center;
  font-weight: bold;
  min-height: 2.2rem;
  min-width: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  font-size: 0.9rem;
}

/* 위험도 등급 색(6단계). Risk_02 관리기준표 팔레트와 동일 권위. */
.risk-very-high {
  background: #ff4444;
  color: #fff;
}

.risk-high {
  background: #ff8800;
  color: #fff;
}

.risk-slightly-high {
  background: #ffaa00;
  color: #1f1e1e;
}

.risk-normal {
  background: #ffd700;
  color: #1f1e1e;
}

.risk-low {
  background: #90ee90;
  color: #1f1e1e;
}

.risk-very-low {
  background: #228b22;
  color: #fff;
}

.readonly-input {
  background-color: #f5f5f5;
  cursor: not-allowed;
  border: 1px solid #ddd;
  padding: 0.5rem;
  border-radius: 4px;
  width: 100%;
  box-sizing: border-box;
}

.textarea-row {
  flex-direction: column !important;
  align-items: flex-start !important;
  flex: 1;
}

.textarea-row label {
  flex: 0 0 auto;
  margin-bottom: 0.5rem;
}

.textarea-row textarea {
  width: 100%;
}

.photo-upload-row {
  flex-direction: column !important;
  align-items: flex-start !important;
  flex: 1;
}

.photo-upload-row label {
  flex: 0 0 auto;
  margin-bottom: 0.5rem;
}

.photo-upload-row .file-upload-container {
  width: 100%;
}

.file-upload-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: flex-start;
  flex: 1 1 auto;
  width: 100%;
}

.file-upload-container {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: flex-start;
  flex: 1 1 auto;
  width: 100%;
}

.upload-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1rem;
  border: 2px dashed #ccc;
  border-radius: 4px;
  background: #f9f9f9;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  width: 100%;
  box-sizing: border-box;
  min-height: 60px;
}

.upload-button-small {
  padding: 0.5rem;
  min-height: 30px;
}

.upload-button-small .upload-icon {
  width: 18px;
  height: 18px;
}

.upload-button-small span {
  font-size: 0.85rem;
}

.upload-button span {
  white-space: nowrap;
}

.upload-button:hover {
  border-color: #30796a;
  background: #f0f0f0;
}

.upload-icon {
  width: 24px;
  height: 24px;
  color: #666;
  transition: all 0.2s;
}

.uploaded-photo-container {
  width: 100%;
  margin-top: 0.5rem;
}

.uploaded-photo-container .photo-container {
  position: relative;
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 4px;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  background: #f9f9f9;
  min-height: 200px;
  padding: 0.5rem;
  width: 100%;
  box-sizing: border-box;
}

.uploaded-photo-container .photo-preview {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  width: 100%;
  height: auto;
}

.delete-photo-button {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  z-index: 10;
}

.delete-photo-button:hover {
  background: rgba(220, 53, 69, 0.8);
  transform: scale(1.1);
}

.delete-icon {
  width: 16px;
  height: 16px;
  color: white;
  stroke-width: 2.5;
}

.modal-footer {
  justify-content: space-between;
}

.footer-buttons-left,
.footer-buttons-right {
  display: flex;
  gap: 0.5rem;
}

.btn {
  padding: 0 1rem;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  font-family: "Pretendard", sans-serif;
  cursor: pointer;
  white-space: nowrap;
  transition:
    background 0.2s,
    box-shadow 0.2s;
}

.btn-cancel {
  background: #ffffff;
  color: #374151;
  border: 1px solid #e5e7eb;
}

.btn-cancel:hover {
  background: #f9fafb;
}

.btn-save {
  background: #16a34a;
  color: #ffffff;
  border: none;
}

.btn-save:hover {
  background: #15803d;
}

.btn-report {
  background: #ffffff;
  color: #16a34a;
  border: 1px solid #16a34a;
}

.btn-report:hover {
  background: rgba(22, 163, 74, 0.06);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* prafta-054-5: 참조 아차사고 (개선 후 영역 내 form-row 형태) */
.ref-nm-form-row {
  align-items: flex-start !important;
}
.ref-nm-form-row > label {
  padding-top: 0.4rem;
}
.ref-nm-block {
  flex: 1 1 0%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.ref-nm-toolbar {
  display: flex;
  justify-content: flex-end;
}
.ref-nm-table-wrap {
  max-height: 180px;
  overflow-y: auto;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
}
.ref-nm-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8rem;
  table-layout: fixed;
}
.ref-nm-table th,
.ref-nm-table td {
  border: 1px solid var(--color-border, #e5e7eb);
  padding: 0.4rem 0.5rem;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ref-nm-table thead th {
  position: sticky;
  top: 0;
  background: #f3f4f6;
  font-weight: 600;
  z-index: 1;
}
.ref-nm-table__loc {
  white-space: normal;
}
.ref-nm-table__act {
  width: 96px;
  text-align: center;
  white-space: nowrap;
}
.ref-nm-table__act .btn {
  padding: 0.2rem 0.45rem;
  font-size: 0.75rem;
}
.ref-nm-empty {
  color: #888;
  font-size: 0.8125rem;
  text-align: center;
  padding: 0.75rem 0;
}
/* 연결 해제(x) 컬럼 */
.ref-nm-table__del {
  width: 32px;
  text-align: center;
}
.btn-x {
  border: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-surface, #ffffff);
  color: var(--color-danger, #b91c1c);
  border-radius: var(--radius-md, 4px);
  cursor: pointer;
  line-height: 1;
  padding: var(--space-xxs, 0.125rem) var(--space-xs, 0.4rem);
  font-size: var(--font-size-xs, 0.75rem);
}
.btn-x:hover {
  background: var(--color-danger-soft, rgba(185, 28, 28, 0.06));
}

/* PRAFTA-WEB_003 v3: AI 분석 슬라이드 패널 포지셔닝 셸.
   ★.modal-content-wide 에 position:relative 를 직접 주지 않는다(인라인 top/left 활성화로 팝업 튐).
   기존 자식 3개(modal-header/risk-assess-content/modal-footer)를 이 셸로 감싸고,
   패널(RiskAiAnalysisPanel)은 셸의 마지막 자식(position:absolute; inset:0)으로 덮는다. */
.risk-popup-shell {
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* PRAFTA-WEB_003 v3: 개선 후 섹션 헤더 우측 "AI 분석" 진입 버튼 */
.section-header--with-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}
.ai-open-btn {
  height: var(--btn-height-sm, 28px);
}

/* PRAFTA-WEB_003 v3: AI 패널 상→하 슬라이드(열림)/하→상(닫힘) 트랜지션.
   Transition 직계 자식 = RiskAiAnalysisPanel 루트 엘리먼트(absolute inset:0)에 적용된다. */
.ai-slide-enter-active,
.ai-slide-leave-active {
  transition: transform 0.3s ease;
}
.ai-slide-enter-from,
.ai-slide-leave-to {
  transform: translateY(-100%);
}
.ai-slide-enter-to,
.ai-slide-leave-from {
  transform: translateY(0);
}
</style>
