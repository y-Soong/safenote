<!--
  LeaveChangeHistoryDetailPop.vue — 관리자 발신 연차 변경 요청 이력 상세 (읽기 전용) (prafta-com-016-H)
  유형: frontend-component (웹 관리자 팝업)
  참조 패턴: views/attd/popup/LeaveChangeConfirmPop.vue (상세 레이아웃·모달 골격)
  역할: 관리자→사용자 발신 연차 변경/삭제 요청 1건의 메타 + 진행 상태 타임라인을 조회만 한다.
        ※ 본 팝업은 조회 전용 — 발의/확인/반려 등 쓰기 액션을 절대 노출하지 않는다(그 처리는 Attd_13).
  EP: GET /webApi/attd14/admin-requests/{changeReqId}
-->
<template>
  <div class="modal-overlay" @click.self="onClose">
    <div class="modal-content lch-pop">
      <header class="modal-header">
        <h2 class="modal-title">연차 변경 요청 상세</h2>
        <button
          type="button"
          class="modal-close"
          aria-label="닫기"
          @click="onClose"
        >
          ×
        </button>
      </header>

      <div class="modal-body lch-body">
        <p v-if="loading" class="lch-state">불러오는 중...</p>

        <template v-else-if="detail">
          <!-- 요청 메타 -->
          <dl class="lch-detail">
            <div>
              <dt>발의일시</dt>
              <dd>{{ detail.insertDateText }}</dd>
            </div>
            <div>
              <dt>발의자</dt>
              <dd>{{ detail.initiatorUserNm }}</dd>
            </div>
            <div>
              <dt>대상 사용자</dt>
              <dd>{{ detail.targetUserNm }}</dd>
            </div>
            <div>
              <dt>요청유형</dt>
              <dd>{{ detail.reqTypeNm }}</dd>
            </div>
            <div>
              <dt>대상 연차일</dt>
              <dd>{{ detail.targetStartDateText || "-" }}</dd>
            </div>
            <div v-if="detail.reqType === 'MOVE'">
              <dt>이동 대상일</dt>
              <dd>
                {{ detail.moveTargetDateText || "-" }}
                <!-- 위치선택 확장(2026-08-18): 지정 파트/시각 병기(미지정이면 종전 표시 그대로) -->
                <template v-if="detail.movePosLabel">
                  · {{ detail.movePosLabel }}
                </template>
              </dd>
            </div>
            <div>
              <dt>요청 사유</dt>
              <dd>{{ detail.reqReason || "-" }}</dd>
            </div>
            <div>
              <dt>처리상태</dt>
              <dd>
                <span class="status-badge" :class="detail.statusClass">{{
                  detail.reqStatusNm
                }}</span>
              </dd>
            </div>
          </dl>

          <!-- 진행 이력(상태 타임라인): 요청됨 → 근로자 응답 → 관리자 확인/반려 → 확정/종료 -->
          <div class="lch-timeline">
            <h3 class="lch-timeline__title">진행 이력</h3>
            <ul class="lch-steps">
              <li class="lch-step is-done">
                <span class="lch-step__dot" aria-hidden="true"></span>
                <div class="lch-step__body">
                  <span class="lch-step__label">요청됨</span>
                  <span class="lch-step__time">{{
                    detail.insertDateText
                  }}</span>
                  <span class="lch-step__sub"
                    >{{ detail.initiatorUserNm }} 발의</span
                  >
                </div>
              </li>

              <li class="lch-step" :class="workerStepClass">
                <span class="lch-step__dot" aria-hidden="true"></span>
                <div class="lch-step__body">
                  <span class="lch-step__label">근로자 응답</span>
                  <span class="lch-step__sub">{{
                    detail.workerResponseNm || "대기"
                  }}</span>
                  <span v-if="detail.responseReason" class="lch-step__reason">
                    사유: {{ detail.responseReason }}
                  </span>
                </div>
              </li>

              <li class="lch-step" :class="confirmStepClass">
                <span class="lch-step__dot" aria-hidden="true"></span>
                <div class="lch-step__body">
                  <span class="lch-step__label">{{ confirmStepLabel }}</span>
                  <span v-if="detail.confirmDateText" class="lch-step__time">
                    {{ detail.confirmDateText }}
                  </span>
                  <span v-if="detail.confirmUserNm" class="lch-step__sub">
                    {{ detail.confirmUserNm }}
                  </span>
                  <span v-if="detail.rejectReason" class="lch-step__reason">
                    반려 사유: {{ detail.rejectReason }}
                  </span>
                </div>
              </li>
            </ul>
          </div>
        </template>
      </div>

      <footer class="modal-footer lch-footer">
        <button type="button" class="btn btn-ghost" @click="onClose">
          닫기
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, onMounted } from "vue";
import axios from "@/api/axios";
import { getMessage, MSG } from "@/messages";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { formatYmdDot, formatDateTimeDot } from "@/utils/dateFormat";

const props = defineProps({
  changeReqId: { type: String, default: "" },
});
const emit = defineEmits(["close"]);

const { proxy } = getCurrentInstance();

const loading = ref(true);
const detail = ref(null);

// 코드 → 라벨 매핑은 하드코딩하지 않고 TB_SYST_VAL_D 에서 단일 출처로 로드한다.
//   SYS071 요청유형 / SYS072 요청상태 / SYS073 근로자 응답 (/comApi/baseinfo/syst-info-lists).
const systCodeArr = ref({});

// 그룹(SYSxxx) + 상세코드(systValDCd) → 라벨(systValDNm). 미로딩/미일치 시 코드값 폴백.
const codeNm = (group, cd) => {
  if (cd == null || cd === "") return cd;
  const hit = (systCodeArr.value[group] || []).find((o) => o.systValDCd === cd);
  return hit ? hit.systValDNm : cd;
};

// 처리상태 배지 색 분기(라벨이 아닌 표시 클래스라 코드테이블 대상 아님 — 유지).
const STATUS_CLASS = {
  CONFIRMED: "is-confirmed",
  REJECTED: "is-rejected",
  CLOSED: "is-closed",
  REQUESTED: "is-pending",
  AGREED: "is-pending",
};

const fmtYmd = (ymd) => formatYmdDot(ymd);
const fmtDateTime = (v) => formatDateTimeDot(v);

// ── 위치선택 확장(2026-08-18): 이동 대상 지정 파트/시각 병기 ──
//   미지정(null/구서버 미수신)이면 빈 값 → 종전 표시 바이트 그대로(무회귀). Attd_10 로컬 헬퍼 미러.
//   반차 파트는 대상일 경계 조회 없이 "시작 기준(늦게 출근)/종료 기준(일찍 퇴근)" 고정 표기.
//   시간차 종료는 시작+원 분량(leaveMinutes) 클라 파생(표시 전용), 분량 결손 시 "HH:MM 시작" 폴백.
const LEAVE_CHANGE_HALF_PART_NM = {
  START: "시작 기준(늦게 출근)",
  END: "종료 기준(일찍 퇴근)",
};
const fmtTime = (hhmm) => {
  const v = String(hhmm ?? "");
  return v.length >= 4 ? `${v.slice(0, 2)}:${v.slice(2, 4)}` : "";
};
const lcHhmmToMin = (hhmm) => {
  const v = String(hhmm ?? "");
  if (v.length !== 4) return null;
  const h = parseInt(v.slice(0, 2), 10);
  const m = parseInt(v.slice(2, 4), 10);
  if (Number.isNaN(h) || Number.isNaN(m)) return null;
  if (h < 0 || h > 23 || m < 0 || m > 59) return null;
  return h * 60 + m;
};
const leaveChangeMovePosLabel = (row) => {
  if (row?.moveTargetHalfPart)
    return LEAVE_CHANGE_HALF_PART_NM[row.moveTargetHalfPart] || "";
  const s = lcHhmmToMin(row?.moveTargetStartTime);
  if (s == null) return "";
  const dur = Number(row?.leaveMinutes);
  if (!Number.isFinite(dur) || dur <= 0)
    return `${fmtTime(row.moveTargetStartTime)} 시작`;
  // 자정 넘김(END<START)은 익일 저장 규약 — 시각만 모듈러 표기
  const e = (s + dur) % 1440;
  const pad = (n) => String(n).padStart(2, "0");
  return `${fmtTime(row.moveTargetStartTime)}~${pad(Math.floor(e / 60))}:${pad(e % 60)}`;
};

// 근로자 응답 단계 상태(완료/거부/대기) 색 분기
const workerStepClass = computed(() => {
  const w = detail.value?.workerResponse;
  if (w === "AGREE") return "is-done";
  if (w === "REJECT") return "is-rejected";
  return "is-pending";
});

// 관리자 확인/반려 단계: 상태에 따라 라벨/색 분기
const confirmStepLabel = computed(() => {
  const s = detail.value?.reqStatus;
  if (s === "CONFIRMED") return "관리자 확정";
  if (s === "REJECTED") return "관리자 반려";
  if (s === "CLOSED") return "종료";
  return "관리자 확인 대기";
});
const confirmStepClass = computed(() => {
  const s = detail.value?.reqStatus;
  if (s === "CONFIRMED") return "is-done";
  if (s === "REJECTED") return "is-rejected";
  if (s === "CLOSED") return "is-closed";
  return "is-pending";
});

const onClose = () => emit("close");

// 코드 로드 — 요청유형(SYS071)/요청상태(SYS072)/응답(SYS073) 라벨 단일 출처(TB_SYST_VAL_D).
const fnGetSystinfoList = async () => {
  try {
    const res = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: { systCodeList: ["SYS071", "SYS072", "SYS073"] },
    });
    if (res.status === 200) {
      const list = res.data?.systInfoList ?? [];
      const grouped = {};
      list.forEach((item) => {
        const key = item.systValCd;
        if (!grouped[key]) grouped[key] = [];
        grouped[key].push(item);
      });
      systCodeArr.value = grouped;
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR_DEFAULT))
    );
  }
};

// 상세 조회: GET /webApi/attd14/admin-requests/{changeReqId} (읽기 전용)
const fnLoadDetail = async () => {
  loading.value = true;
  try {
    const res = await axios.get(
      `/webApi/attd14/admin-requests/${props.changeReqId}`
    );
    if (res.status === 200) {
      const d = res.data?.detail;
      if (d) {
        detail.value = {
          changeReqId: d.changeReqId,
          reqStatus: d.reqStatus,
          reqType: d.reqType,
          workerResponse: d.workerResponse,
          targetUserNm: d.targetUserNm,
          initiatorUserNm: d.initiatorUserNm,
          confirmUserNm: d.confirmUserNm,
          insertDateText: fmtDateTime(d.insertDate),
          confirmDateText: fmtDateTime(d.confirmDate),
          targetStartDateText: fmtYmd(d.targetStartDate),
          moveTargetDateText: fmtYmd(d.moveTargetDate),
          reqReason: d.reqReason,
          responseReason: d.responseReason,
          rejectReason: d.rejectReason,
          reqTypeNm: codeNm("SYS071", d.reqType),
          reqStatusNm: codeNm("SYS072", d.reqStatus),
          workerResponseNm: codeNm("SYS073", d.workerResponse),
          statusClass: STATUS_CLASS[d.reqStatus] || "is-pending",
          // 위치선택 확장(2026-08-18): 미지정이면 빈 값 → 병기 미노출(종전 표시 그대로)
          movePosLabel: leaveChangeMovePosLabel(d),
        };
      }
    }
  } catch (err) {
    await proxy.$alert(
      resolveApiErrorMessage(err, getMessage(MSG.SEARCH_ERROR))
    );
    emit("close");
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  // 라벨 코드를 먼저 로드해야 상세 매핑이 정확하다.
  await fnGetSystinfoList();
  if (props.changeReqId) await fnLoadDetail();
  else loading.value = false;
});
</script>

<style scoped>
.lch-pop {
  width: 460px;
  max-width: 92vw;
  max-height: 82vh;
  /* 기본 modal-content 의 20px 패딩 제거 → 헤더/본문/푸터가 박스 끝에 밀착.
     overflow:hidden 으로 헤더/푸터 모서리를 16px 라운드에 맞춰 클립. */
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.lch-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: var(--card-padding, 20px);
}

.lch-state {
  text-align: center;
  padding: var(--card-padding, 20px);
  color: var(--color-text-muted);
}

.lch-detail {
  display: grid;
  gap: var(--space-sm, 8px);
  margin: 0;
}
.lch-detail div {
  display: flex;
  gap: var(--space-sm, 8px);
  font-size: var(--btn-font, 11px);
}
.lch-detail dt {
  width: 84px;
  flex-shrink: 0;
  color: var(--color-text-muted);
}
.lch-detail dd {
  margin: 0;
  color: var(--color-text-strong);
}

/* 처리상태 배지 */
.status-badge {
  display: inline-block;
  min-width: 48px;
  padding: 2px var(--btn-padding-sm);
  border-radius: var(--btn-radius);
  font-size: var(--btn-font-sm);
  line-height: 1.6;
}
.status-badge.is-confirmed {
  background: var(--color-primary);
  color: var(--color-surface);
}
.status-badge.is-rejected {
  background: var(--color-danger);
  color: var(--color-surface);
}
.status-badge.is-pending {
  background: var(--color-bg);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}
.status-badge.is-closed {
  background: var(--color-border);
  color: var(--color-text);
}

/* 진행 이력 타임라인 */
.lch-timeline {
  margin-top: var(--card-padding, 20px);
  padding-top: var(--space-sm, 8px);
  border-top: 1px solid var(--color-border);
}
.lch-timeline__title {
  margin: 0 0 var(--space-sm, 8px);
  font-size: var(--btn-font, 11px);
  font-weight: 600;
  color: var(--color-text-strong);
}
.lch-steps {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm, 8px);
}
.lch-step {
  display: flex;
  gap: var(--space-sm, 8px);
  align-items: flex-start;
}
.lch-step__dot {
  width: 10px;
  height: 10px;
  margin-top: 3px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--color-border);
  border: 1px solid var(--color-border-strong);
}
.lch-step.is-done .lch-step__dot {
  background: var(--color-primary);
  border-color: var(--color-primary);
}
.lch-step.is-rejected .lch-step__dot {
  background: var(--color-danger);
  border-color: var(--color-danger);
}
.lch-step.is-closed .lch-step__dot {
  background: var(--color-text-muted);
  border-color: var(--color-text-muted);
}
.lch-step__body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: var(--btn-font, 11px);
}
.lch-step__label {
  font-weight: 600;
  color: var(--color-text-strong);
}
.lch-step__time,
.lch-step__sub {
  color: var(--color-text-muted);
}
.lch-step__reason {
  color: var(--color-text);
}

.lch-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm, 8px);
  padding: var(--space-sm, 8px) var(--card-padding, 20px);
  border-top: 1px solid var(--color-border);
}
</style>
