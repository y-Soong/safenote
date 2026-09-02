<!--
  LocationConsentHistPop.vue — 위치정보 동의 이력 팝업 (위치정보 동의철회·중지 S5)

  - 백엔드: GET /webApi/location01/consent-histories?siteCd=&userCd=
  - 전이 이력(상태가 바뀌었다)과 파기 이력(그래서 무엇을 지웠다)을 한 화면에 함께 둔다.
    책임 추궁은 둘을 대조할 수 있을 때 성립한다.
  - ★파기 이력에는 좌표가 없다(테이블 자체에 담지 않는다). 남는 것은 건수·기간·사유·주체뿐이다.
  - 조회 전용. 이 팝업에서 상태를 바꾸는 동작은 제공하지 않는다.
-->
<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-show="true"
        ref="overlayRef"
        class="modal-overlay prafta-modal-popup"
        tabindex="-1"
        @click.self="onClose"
        @keydown.esc="onClose"
      >
        <div class="lch-pop" role="dialog" aria-modal="true">
          <div class="lch-pop__header">
            <span class="lch-pop__title">
              위치정보 동의 이력
              <span v-if="userNm_p" class="lch-pop__sub">· {{ userNm_p }}</span>
            </span>
            <button
              class="lch-pop__close"
              type="button"
              aria-label="닫기"
              @click="onClose"
            >
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
              >
                <path d="M18 6L6 18M6 6l12 12" />
              </svg>
            </button>
          </div>

          <div class="lch-pop__body">
            <p v-if="loading" class="lch-empty">불러오는 중...</p>

            <template v-else>
              <!-- 동의 전이 이력 -->
              <h4 class="lch-sec">동의 상태 변경 이력</h4>
              <div class="tableWrap">
                <table class="tableList">
                  <thead>
                    <tr>
                      <th>일시</th>
                      <th>변경</th>
                      <th>약관 버전</th>
                      <th>경로</th>
                      <th>수행자</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="consentHistList.length === 0">
                      <td colspan="5" class="noData">변경 이력이 없습니다.</td>
                    </tr>
                    <tr v-for="h in consentHistList" :key="h.histId">
                      <td style="text-align: center">{{ h.actionDtime }}</td>
                      <td style="text-align: center">
                        {{ transitionLabel(h) }}
                      </td>
                      <td style="text-align: center">v{{ h.termsVersion }}</td>
                      <td style="text-align: center">
                        {{ sourceLabel(h.agrSource) }}
                      </td>
                      <td style="text-align: center">{{ h.actorUserCd }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- 파기 이력 -->
              <h4 class="lch-sec">위치정보 파기 이력</h4>
              <p class="lch-note">
                파기 이력에는 좌표가 저장되지 않습니다. 언제·무엇을·몇 건
                지웠는지만 남습니다.
              </p>
              <div class="tableWrap">
                <table class="tableList">
                  <thead>
                    <tr>
                      <th>일시</th>
                      <th>사유</th>
                      <th>출퇴근</th>
                      <th>TBM 입실</th>
                      <th>TBM 개설</th>
                      <th>대상 기간</th>
                      <th>수행자</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="purgeHistList.length === 0">
                      <td colspan="7" class="noData">파기 이력이 없습니다.</td>
                    </tr>
                    <tr v-for="p in purgeHistList" :key="p.purgeId">
                      <td style="text-align: center">{{ p.actionDtime }}</td>
                      <td style="text-align: center">
                        {{ reasonLabel(p.purgeReasonCd) }}
                      </td>
                      <td style="text-align: right">{{ p.attdGpsRows }}</td>
                      <td style="text-align: right">
                        {{ p.tbmAttendanceRows }}
                      </td>
                      <td style="text-align: right">{{ p.tbmSessionRows }}</td>
                      <td style="text-align: center">{{ periodLabel(p) }}</td>
                      <td style="text-align: center">{{ p.actorUserCd }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </template>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, onMounted, nextTick } from "vue";
import axios from "@/api/axios";

const props = defineProps({
  siteCd_p: { type: String, default: "" },
  userCd_p: { type: String, default: "" },
  userNm_p: { type: String, default: "" },
});

const emit = defineEmits(["close"]);

const consentHistList = ref([]);
const purgeHistList = ref([]);
const loading = ref(false);
const overlayRef = ref(null);

const onClose = () => emit("close");

const STATE_LABEL = {
  AGREED: "동의",
  SUSPENDED: "일시 중지",
  PENDING_REAGREE: "재동의 필요",
  WITHDRAWN: "동의 철회",
};

// 상태 컬럼 도입 이전 행은 BEFORE/AFTER_STATE 가 NULL 이다 —
//   그 경우 AGR_YN 으로 표시한다(이력을 소급 조작하지 않는다).
const stateOf = (state, agrYn) => {
  if (state) return STATE_LABEL[state] || state;
  if (agrYn === "Y") return "동의";
  if (agrYn === "N") return "미동의";
  return "-";
};
const transitionLabel = (h) =>
  `${stateOf(h.beforeState, h.beforeAgrYn)} → ${stateOf(h.afterState, h.afterAgrYn)}`;

const SOURCE_LABEL = {
  GATE: "로그인 게이트",
  MYPAGE: "마이페이지",
  JOIN: "가입",
  SYSTEM: "시스템(약관 개정)",
  ADMIN: "관리자",
};
const sourceLabel = (src) => SOURCE_LABEL[src] || src || "-";

const reasonLabel = (cd) =>
  cd === "RETENTION"
    ? "보존기간 경과"
    : cd === "WITHDRAW"
      ? "동의 철회"
      : cd || "-";

const fmtYmd = (ymd) =>
  ymd && ymd.length === 8
    ? `${ymd.slice(0, 4)}-${ymd.slice(4, 6)}-${ymd.slice(6, 8)}`
    : ymd || "-";
const periodLabel = (p) =>
  p.oldestCollected
    ? `${fmtYmd(p.oldestCollected)} ~ ${fmtYmd(p.latestCollected)}`
    : "-";

const fnLoad = async () => {
  if (!props.userCd_p) return;
  loading.value = true;
  try {
    const response = await axios.get("/webApi/location01/consent-histories", {
      params: { siteCd: props.siteCd_p, userCd: props.userCd_p },
    });
    if (response.status === 200) {
      consentHistList.value = response.data?.consentHistList ?? [];
      purgeHistList.value = response.data?.purgeHistList ?? [];
    }
  } catch (err) {
    console.error("[LocationConsentHistPop] 이력 조회 실패", err);
    consentHistList.value = [];
    purgeHistList.value = [];
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  await nextTick();
  overlayRef.value?.focus();
  fnLoad();
});
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}
.lch-pop {
  width: min(56rem, 92vw);
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  background: var(--color-surface, #fff);
  border-radius: 0.5rem;
  overflow: hidden;
}
.lch-pop__header {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
}
.lch-pop__title {
  font-weight: 600;
}
.lch-pop__sub {
  font-weight: 400;
  color: var(--color-text-secondary);
}
.lch-pop__close {
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--color-text-secondary);
}
.lch-pop__body {
  flex: 1 1 auto;
  overflow: auto;
  padding: 1rem;
}
.lch-sec {
  margin: 0 0 0.5rem;
  font-size: 0.9375rem;
}
.lch-sec + .tableWrap {
  margin-bottom: 1.25rem;
}
.lch-note {
  margin: -0.25rem 0 0.5rem;
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  word-break: keep-all;
}
.lch-empty {
  padding: 2rem 0;
  text-align: center;
  color: var(--color-text-secondary);
}
</style>
