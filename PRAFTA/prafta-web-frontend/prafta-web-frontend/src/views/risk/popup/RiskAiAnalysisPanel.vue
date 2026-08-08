<template>
  <!-- ★슬라이드 Transition 은 부모(RiskAssessInfo)가 감싼다:
       v-if 언마운트가 부모에서 일어나므로 열림/닫힘 양방향 애니메이션이 재생된다.
       루트는 단일 엘리먼트(absolute inset:0) 유지 필수. -->
  <div class="ai-panel">
    <!-- ★AI 작업중 블러 오버레이 (초기 로드/채팅/확정/도출 공통) -->
    <div v-if="aiBusy" class="ai-panel__busy">
      <LoadingSpinner />
      <span class="ai-panel__busy-text">{{ busyText }}</span>
    </div>

    <!-- 헤더: 타이틀 + 초기화 + 단계 칩 내비게이션 + 닫기 -->
    <div class="ai-panel__header">
      <span class="ai-panel__title">AI 분석</span>
      <!-- 초기화(처음부터 다시): 서버 도출 행 DELETE 후 재진입(확인 다이얼로그 경유) -->
      <button
        type="button"
        class="btn btn-report ai-panel__reset"
        :disabled="aiBusy"
        @click="fnReset"
      >
        초기화
      </button>
      <!-- 단계 칩 내비게이션(v3.7 2단계): 클릭 시 해당 step 으로 로컬 전환(서버 상태 불변).
             칩1 = 사진 有 "이미지 확정" / 사진 無 "정보 확인", 칩2 = 결과(도출 데이터 있을 때만 클릭 가능) -->
      <ol class="ai-steps">
        <li
          class="ai-steps__item"
          :class="stepChipClass('imageConfirm')"
          @click="fnGoStep('imageConfirm')"
        >
          {{ props.hasSourceImage ? "이미지 확정" : "정보 확인" }}
        </li>
        <li
          class="ai-steps__item"
          :class="stepChipClass('result')"
          @click="fnGoStep('result')"
        >
          결과
        </li>
      </ol>
      <button class="icon-button" type="button" @click="fnClose">
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

    <!-- 본문 -->
    <div class="ai-panel__body">
      <!-- ── STEP 1: 이미지 확정(사진 有) / 정보 확인(사진 無) — v3.7 2단계 구조 ── -->
      <section v-if="step === 'imageConfirm'" class="ai-confirm">
        <!-- 좌측: 근로자 이미지(크기 고정) + 개선 전 내용 요약(구 도출 탭에서 이동 — 기존 스타일 재사용) -->
        <div class="ai-confirm__left">
          <div class="ai-confirm__photo">
            <img
              v-if="props.photoUrl"
              :src="props.photoUrl"
              alt="개선 전 사진"
              class="ai-confirm__photo-img"
            />
            <div v-else class="ai-confirm__photo-empty">이미지 없음</div>
          </div>
          <div class="ai-derive__summary">
            <div class="ai-derive__summary-title">
              개선 전 내용 요약 (읽기 전용)
            </div>
            <div class="ai-derive__row">
              <span class="ai-derive__row-label">작업명</span>
              <span class="ai-derive__row-value">{{
                props.summary.processNm || "-"
              }}</span>
            </div>
            <div class="ai-derive__row">
              <span class="ai-derive__row-label">위험성분류</span>
              <span class="ai-derive__row-value">{{
                props.summary.riskTypeNm || "-"
              }}</span>
            </div>
            <div class="ai-derive__row">
              <span class="ai-derive__row-label">유해요인명</span>
              <span class="ai-derive__row-value">{{
                props.summary.hazardNm || "-"
              }}</span>
            </div>
            <div class="ai-derive__row ai-derive__row--multiline">
              <span class="ai-derive__row-label">유해요인설명</span>
              <span class="ai-derive__row-value">{{
                props.summary.initDesc || "-"
              }}</span>
            </div>
          </div>
        </div>

        <!-- 우측(사진 有): 자유 대화형 채팅 — 예/아니오·보충입력 블록 제거(v3.7), 입력줄 상시 노출 -->
        <div v-if="props.hasSourceImage" class="ai-confirm__chat">
          <div class="ai-chat__log" ref="chatLogRef">
            <div
              v-for="(t, i) in visibleTurns"
              :key="'turn-' + i"
              class="ai-chat__row"
              :class="
                t.role === 'user'
                  ? 'ai-chat__row--user'
                  : 'ai-chat__row--assistant'
              "
            >
              <span class="ai-chat__bubble">{{ t.text }}</span>
            </div>
            <div v-if="visibleTurns.length === 0" class="ai-chat__empty">
              AI 가 사진을 판독하는 중입니다…
            </div>
          </div>

          <p v-if="chatErrorMsg" class="ai-panel__state ai-panel__state--error">
            {{ chatErrorMsg }}
          </p>
          <p
            v-if="deriveErrorMsg"
            class="ai-panel__state ai-panel__state--error"
          >
            {{ deriveErrorMsg }}
          </p>

          <!-- 채팅 입력줄(항상 노출): 설명/질문 + 이미지 첨부(캡·선검증 기존 유지) + 전송 -->
          <div class="ai-chat__input">
            <textarea
              v-model="chatInput"
              class="ai-chat__input-field"
              rows="2"
              placeholder="사진에 대한 설명이나 질문을 입력하세요. (예: sorter가 아니라 컨베이어 라인입니다.)"
              :disabled="aiBusy"
            ></textarea>
            <div class="ai-chat__input-actions">
              <input
                type="file"
                ref="imageInputRef"
                accept="image/jpeg,image/png,image/webp"
                multiple
                style="display: none"
                @change="fnPickImages"
              />
              <button
                type="button"
                class="btn btn-report"
                :disabled="aiBusy || adminImages.length >= 2"
                @click="imageInputRef?.click()"
              >
                이미지 첨부
              </button>
              <span class="ai-correct__attach-hint"
                >jpg/png/webp · 장당 3MB · 최대 2장</span
              >
              <button
                type="button"
                class="btn btn-save ai-chat__send"
                :disabled="
                  aiBusy || (!chatInput.trim() && adminImages.length === 0)
                "
                @click="fnSendChat"
              >
                전송
              </button>
            </div>
            <ul v-if="adminImages.length" class="ai-correct__chips">
              <li
                v-for="(img, i) in adminImages"
                :key="'img-' + i"
                class="ai-correct__chip"
              >
                <span class="ai-correct__chip-name">{{ img.name }}</span>
                <button
                  type="button"
                  class="btn-x"
                  :disabled="aiBusy"
                  title="첨부 제거"
                  @click="fnRemoveImage(i)"
                >
                  x
                </button>
              </li>
            </ul>
          </div>
        </div>

        <!-- 우측(사진 無): 관리자 의견 — BE chat-image 는 이미지 필수 게이트라 채팅 미제공 -->
        <div v-else class="ai-confirm__opinion">
          <label class="ai-confirm__opinion-label" for="aiOpinion"
            >관리자 의견 (선택)</label
          >
          <textarea
            id="aiOpinion"
            v-model="suppDesc"
            class="ai-confirm__opinion-field"
            rows="6"
            placeholder="AI 도출에 참고할 관리자 의견을 입력하세요. (예: 이 작업은 우천 시 미끄럼 위험이 큽니다.)"
            :disabled="aiBusy"
            @blur="fnSaveSupplement"
          ></textarea>
          <p class="ai-confirm__opinion-hint">
            입력한 의견은 AI 도출에 반영됩니다.
          </p>
          <p
            v-if="deriveErrorMsg"
            class="ai-panel__state ai-panel__state--error"
          >
            {{ deriveErrorMsg }}
          </p>
        </div>
      </section>

      <!-- ── STEP 2: 결과 (v3.8 재설계 — 좌 40% 컨텍스트 / 우 60% 아코디언) ── -->
      <section v-else class="ai-result">
        <!-- 좌측(40%): 이미지 + 개선 전 요약 + 확정 대화(사진 無면 관리자 의견) — 단계1 좌측 구성 재사용 -->
        <div class="ai-result__left">
          <div class="ai-confirm__photo">
            <img
              v-if="props.photoUrl"
              :src="props.photoUrl"
              alt="개선 전 사진"
              class="ai-confirm__photo-img"
            />
            <div v-else class="ai-confirm__photo-empty">이미지 없음</div>
          </div>
          <div class="ai-derive__summary">
            <div class="ai-derive__summary-title">
              개선 전 내용 요약 (읽기 전용)
            </div>
            <div class="ai-derive__row">
              <span class="ai-derive__row-label">작업명</span>
              <span class="ai-derive__row-value">{{
                props.summary.processNm || "-"
              }}</span>
            </div>
            <div class="ai-derive__row">
              <span class="ai-derive__row-label">위험성분류</span>
              <span class="ai-derive__row-value">{{
                props.summary.riskTypeNm || "-"
              }}</span>
            </div>
            <div class="ai-derive__row">
              <span class="ai-derive__row-label">유해요인명</span>
              <span class="ai-derive__row-value">{{
                props.summary.hazardNm || "-"
              }}</span>
            </div>
            <div class="ai-derive__row ai-derive__row--multiline">
              <span class="ai-derive__row-label">유해요인설명</span>
              <span class="ai-derive__row-value">{{
                props.summary.initDesc || "-"
              }}</span>
            </div>
          </div>
          <!-- 확정 대화 카드(사진 有): 기본=마지막 AI 발화(확정된 이미지 이해), 토글로 전체 대화 확장 -->
          <div v-if="props.hasSourceImage" class="ai-result__dialog">
            <div class="ai-result__dialog-head">
              <span class="ai-result__dialog-title">확정 대화</span>
              <button
                type="button"
                class="ai-result__dialog-toggle"
                @click="showFullDialog = !showFullDialog"
              >
                {{ showFullDialog ? "마지막 발화만" : "전체 대화 보기" }}
              </button>
            </div>
            <div v-if="!showFullDialog" class="ai-result__dialog-last">
              {{ lastAssistantText || "-" }}
            </div>
            <div v-else class="ai-result__dialog-full">
              <div
                v-for="(t, i) in visibleTurns"
                :key="'dlg-' + i"
                class="ai-result__dialog-line"
              >
                <span class="ai-result__dialog-role">{{
                  t.role === "user" ? "관리자" : "AI"
                }}</span>
                <span class="ai-result__dialog-text">{{ t.text }}</span>
              </div>
            </div>
          </div>
          <!-- 사진 無: 관리자 의견(읽기 전용 표시) -->
          <div v-else class="ai-result__dialog">
            <div class="ai-result__dialog-head">
              <span class="ai-result__dialog-title">관리자 의견</span>
            </div>
            <div class="ai-result__dialog-last">{{ suppDesc || "-" }}</div>
          </div>
        </div>

        <!-- 우측(60%): 유해요인 아코디언 — 행 클릭 시 아래로 개선안 확장(단일 확장) -->
        <div class="ai-result__right">
          <div class="ai-result__col-title">유해요인 및 개선안</div>
          <ul class="ai-acc">
            <!-- AI 도출 유해요인(키 'ai-N' — 기존 선택 키 체계 유지) -->
            <li
              v-for="(hz, i) in hazards"
              :key="'hz-' + i"
              class="ai-acc__item"
            >
              <div
                class="ai-acc__row"
                :class="{
                  'ai-acc__row--active': activeHazardKey === 'ai-' + i,
                }"
                @click="fnSelectHazard('ai-' + i)"
              >
                <input
                  type="checkbox"
                  class="ai-result__check"
                  :checked="selectedHazards.includes('ai-' + i)"
                  :disabled="aiBusy"
                  @click.stop
                  @change="fnToggleHazard('ai-' + i)"
                />
                <span class="ai-result__text">
                  {{ hz.text }}
                  <sup v-if="hz.markers && hz.markers.length" class="ai-cite">{{
                    hz.markers.join("")
                  }}</sup>
                </span>
                <span class="ai-acc__arrow">{{
                  activeHazardKey === "ai-" + i ? "▲" : "▼"
                }}</span>
              </div>
              <div v-if="activeHazardKey === 'ai-' + i" class="ai-acc__panel">
                <ul class="ai-acc__measures">
                  <li
                    v-for="(ms, j) in hz.measures || []"
                    :key="'ms-' + j"
                    class="ai-acc__measure"
                  >
                    <input
                      type="checkbox"
                      class="ai-result__check"
                      :checked="isMeasureSelected('ai-' + i, j)"
                      :disabled="aiBusy"
                      @change="fnToggleMeasure('ai-' + i, j)"
                    />
                    <span class="ai-result__text">
                      {{ ms.text }}
                      <sup
                        v-if="ms.markers && ms.markers.length"
                        class="ai-cite"
                        >{{ ms.markers.join("") }}</sup
                      >
                    </span>
                  </li>
                  <li
                    v-if="!(hz.measures && hz.measures.length)"
                    class="ai-result__empty"
                  >
                    관련 개선안이 없습니다. 다시 도출해 주세요.
                  </li>
                </ul>
              </div>
            </li>
            <!-- verbatim 원문 유해요인(키 'vb-N' — LLM 미경유 패스스루, 원문 그대로 표시) -->
            <li
              v-for="(vb, i) in verbatimHazardItems"
              :key="'vb-' + i"
              class="ai-acc__item"
            >
              <div
                class="ai-acc__row ai-acc__row--verbatim"
                :class="{
                  'ai-acc__row--active': activeHazardKey === 'vb-' + i,
                }"
                @click="fnSelectHazard('vb-' + i)"
              >
                <input
                  type="checkbox"
                  class="ai-result__check"
                  :checked="selectedHazards.includes('vb-' + i)"
                  :disabled="aiBusy"
                  @click.stop
                  @change="fnToggleHazard('vb-' + i)"
                />
                <span class="ai-acc__body">
                  <span class="ai-result__text">
                    <span class="ai-vb-badge">원문</span>
                    {{ vb.text }}
                  </span>
                  <span class="ai-result__vb-source">{{ vb.sourceName }}</span>
                </span>
                <span class="ai-acc__arrow">{{
                  activeHazardKey === "vb-" + i ? "▲" : "▼"
                }}</span>
              </div>
              <div v-if="activeHazardKey === 'vb-' + i" class="ai-acc__panel">
                <p class="ai-result__vb-note">
                  원문 항목은 출처와 함께 원문 그대로 표시됩니다.
                </p>
                <ul class="ai-acc__measures">
                  <li
                    v-for="(ms, j) in vb.measures"
                    :key="'vms-' + j"
                    class="ai-acc__measure"
                  >
                    <input
                      type="checkbox"
                      class="ai-result__check"
                      :checked="isMeasureSelected('vb-' + i, j)"
                      :disabled="aiBusy"
                      @change="fnToggleMeasure('vb-' + i, j)"
                    />
                    <span class="ai-result__text">{{ ms.text }}</span>
                  </li>
                  <li v-if="vb.measures.length === 0" class="ai-result__empty">
                    관련 개선안이 없습니다.
                  </li>
                </ul>
              </div>
            </li>
            <li
              v-if="hazards.length === 0 && verbatimHazardItems.length === 0"
              class="ai-result__empty"
            >
              도출된 유해요인이 없습니다.
            </li>
          </ul>

          <!-- 참고 원문(verbatim 패스스루 — LLM 미경유, 원문 무변경 표시. v3.8: 기관명/링크/라이선스 보강,
                 구 저장분은 필드 없음 → v-if 생략 방어) -->
          <div v-if="verbatimRefs.length" class="ai-verbatim">
            <button
              type="button"
              class="ai-verbatim__toggle"
              :aria-expanded="showVerbatim"
              @click="fnToggleVerbatim"
            >
              <span class="ai-verbatim__toggle-arrow">{{
                showVerbatim ? "▼" : "▶"
              }}</span>
              참고 원문 (출처 원문 그대로)
            </button>
            <div v-if="showVerbatim" class="ai-verbatim__body">
              <p class="ai-verbatim__notice">
                라이선스에 따라 원문 그대로 표시됩니다(변경 불가).
              </p>
              <ul class="ai-verbatim__list">
                <li
                  v-for="(vr, i) in verbatimRefs"
                  :key="'vr-' + i"
                  class="ai-verbatim__item"
                >
                  <div class="ai-verbatim__source">
                    <span v-if="vr.sourceOrg" class="ai-verbatim__org">{{
                      vr.sourceOrg
                    }}</span>
                    <a
                      v-if="vr.sourceUrl"
                      :href="vr.sourceUrl"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="ai-verbatim__source-name ai-src-link"
                      >{{ vr.sourceName }}</a
                    >
                    <span v-else class="ai-verbatim__source-name">{{
                      vr.sourceName
                    }}</span>
                    <span
                      v-if="vr.dataReliability"
                      class="ai-verbatim__badge"
                      >{{ vr.dataReliability }}</span
                    >
                    <span v-if="vr.licenseType" class="ai-verbatim__license">{{
                      vr.licenseType
                    }}</span>
                  </div>
                  <div v-if="vr.content" class="ai-verbatim__field">
                    <span class="ai-verbatim__field-label">재해개요</span>
                    <span class="ai-verbatim__field-value">{{
                      vr.content
                    }}</span>
                  </div>
                  <div v-if="vr.hazardText" class="ai-verbatim__field">
                    <span class="ai-verbatim__field-label">유해요인</span>
                    <span class="ai-verbatim__field-value">{{
                      vr.hazardText
                    }}</span>
                  </div>
                  <div v-if="vr.measureText" class="ai-verbatim__field">
                    <span class="ai-verbatim__field-label">감소대책</span>
                    <span class="ai-verbatim__field-value">{{
                      vr.measureText
                    }}</span>
                  </div>
                </li>
              </ul>
            </div>
          </div>

          <!-- 근거 출처(v3.8 보강: 마커 · 기관명 · 자료명(링크) · 신뢰등급 — 구 저장분은 v-if 생략) -->
          <div v-if="citations.length" class="ai-result__cites">
            <div class="ai-result__col-title">근거 출처</div>
            <ol class="ai-cite-list">
              <li
                v-for="(ct, i) in citations"
                :key="'ct-' + i"
                class="ai-cite-list__item"
              >
                <span class="ai-cite-list__marker">{{ ct.marker }}</span>
                <span v-if="ct.sourceOrg">{{ ct.sourceOrg }} ·</span>
                <a
                  v-if="ct.sourceUrl"
                  :href="ct.sourceUrl"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="ai-src-link"
                  >{{ ct.sourceName }}</a
                >
                <span v-else>{{ ct.sourceName }}</span>
                <span v-if="ct.dataReliability"
                  >· {{ ct.dataReliability }}</span
                >
              </li>
            </ol>
          </div>
          <p v-if="abstained" class="ai-panel__note">
            확정된 근거 기반 결과가 없어 일반 지식 기반 참고로 제시합니다(각주
            없음).
          </p>
          <p v-if="disclaimer" class="ai-panel__disclaimer">{{ disclaimer }}</p>
        </div>
      </section>
    </div>

    <!-- 푸터 (v3.7: 도출 단계 제거 — 도출 버튼을 푸터로 이동, 결과 단계에서는 재도출 역할) -->
    <div class="ai-panel__footer">
      <div class="ai-panel__footer-left"></div>
      <!-- F-10 규약: 왼쪽=진행/확정(도출·저장), 오른쪽=이탈(닫기) -->
      <div class="ai-panel__footer-right">
        <button
          type="button"
          class="btn btn-save"
          :disabled="aiBusy"
          @click="fnDerive"
        >
          AI 유해요인/개선안 도출
        </button>
        <!-- 저장: 이번 범위에서는 표시만(실제 데이터 세팅은 후속 요청) -->
        <button
          v-if="step === 'result'"
          type="button"
          class="btn btn-save"
          :disabled="aiBusy"
          @click="fnSaveSelection"
        >
          저장
        </button>
        <button
          type="button"
          class="btn btn-cancel"
          :disabled="aiBusy"
          @click="fnClose"
        >
          닫기
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
/* eslint-disable */
// PRAFTA-WEB_003 v3.7: 절차 기반 AI 분석 슬라이드 패널 (RiskAssessInfo 팝업 내부 오버레이).
// 단계 상태머신(2단계): imageConfirm(사진 有=이미지 확정 자유 채팅 / 사진 無=정보 확인) → result.
// 도출 단계는 제거 — 도출 버튼은 푸터 상시 노출. confirm-image/IMG_CONFIRMED 는 BE legacy 존치(FE 미사용).
import { ref, computed, nextTick, onMounted, getCurrentInstance } from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage, isAiQuotaExceeded } from "@/utils/apiError";
import { readFileAsBase64 } from "@/utils/fileUtil";
import LoadingSpinner from "@/components/common/LoadingSpinner.vue";

const props = defineProps({
  // 평가건 스코프 3축 { siteCd, processCd, assessmentCd } — CMPNY_CD 는 서버가 JWT 로만 도출
  scope: { type: Object, required: true },
  // 개선 전 요약(읽기 전용) { processNm, riskTypeNm, hazardNm, initDesc }
  summary: { type: Object, default: () => ({}) },
  // 개선 전 사진 URL(없으면 "")
  photoUrl: { type: String, default: "" },
  // 근로자 사진 첨부 여부(INIT_FILE_MGMT_CD 존재) — 이미지 확정 단계 분기
  hasSourceImage: { type: Boolean, default: false },
});

const emit = defineEmits(["close", "save", "derived", "touched"]);
const { proxy } = getCurrentInstance();

// 관리자 추가 이미지 FE 선검증 기준(서버 AI_400_004 와 동일)
const ADMIN_IMAGE_MAX_COUNT = 2;
const ADMIN_IMAGE_MAX_BYTES = 3 * 1024 * 1024;
const ADMIN_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];

// 채팅 전송 시 텍스트 공백 + 이미지만 첨부한 경우 대신 보내는 기본 문구
const CHAT_DEFAULT_MESSAGE = "추가 설명/이미지를 참고하여 다시 판단해 주세요.";

// ── 단계 상태머신(v3.7 2단계) ──────────────────────────────────
// 'imageConfirm'(단계1 — 사진 有=이미지 확정 채팅, 사진 無=정보 확인. 값은 기존 재사용) | 'result'
const step = ref("imageConfirm");

// ── 서버 상태 ─────────────────────────────────────────────────
const chatTurns = ref([]); // [{ role, text, hidden }]
const suppDesc = ref("");
const hazards = ref([]); // [{ text, markers[], measures:[{text, markers[]}] }]
const citations = ref([]); // [{ marker, sourceName, dataReliability }]
// verbatim 참고 원문(LLM 미경유 서버 패스스루 — 원문 무변경 표시, 그라운딩 개선 C)
const verbatimRefs = ref([]); // [{ sourceName, dataReliability, content, hazardText, measureText }]
const abstained = ref(false);
const disclaimer = ref("");

// ── UI 상태 ───────────────────────────────────────────────────
const initialLoading = ref(false);
const chatSending = ref(false);
const deriving = ref(false);
const chatErrorMsg = ref("");
const deriveErrorMsg = ref("");
const showVerbatim = ref(false); // 참고 원문 접이식 섹션(기본 접힘)
const showFullDialog = ref(false); // 결과 탭 확정 대화 카드: 전체 대화 보기 토글(기본=마지막 AI 발화만)
const chatInput = ref(""); // 채팅 입력줄(v3.7 — 구 correctionText 흡수)
const adminImages = ref([]); // [{ base64, mediaType, name }] — 패널 세션 동안 유지·전송마다 전체 재전송
const chatLogRef = ref(null);
const imageInputRef = ref(null);

// 결과 선택 상태 (선택만 — 저장 동작은 후속 요청)
// ★키 기반: AI 도출 항목은 'ai-N', verbatim 원문 항목은 'vb-N' — 두 리스트의 index 충돌 방지
const activeHazardKey = ref(null); // 'ai-N' | 'vb-N' | null
const selectedHazards = ref([]); // 선택된 hazard 키 배열('ai-N'|'vb-N')
const selectedMeasures = ref({}); // { hazardKey: [measureIdx, ...] }

// ── computed ─────────────────────────────────────────────────
const aiBusy = computed(
  () => initialLoading.value || chatSending.value || deriving.value
);
const busyText = computed(() =>
  initialLoading.value ? "불러오는 중…" : "AI 분석중…"
);
// hidden 턴(kickoff 지시문)은 렌더 제외
const visibleTurns = computed(() =>
  chatTurns.value.filter((t) => t && !t.hidden)
);
// verbatim 원문 유해요인 → 선택 항목 변환.
// ★LLM 미경유 패스스루 — 변경금지 라이선스: 텍스트 가공 금지({{ }} 보간만).
//   measureText 의 개행(\n) 분리는 줄 단위 "레이아웃"일 뿐 각 줄 텍스트는 무변경
//   (trim 후 빈 줄만 제외, "▶" 등 기호 포함 원문 유지). hazardText 없는 항목은 제외.
const verbatimHazardItems = computed(() =>
  verbatimRefs.value
    .filter((vr) => vr && vr.hazardText)
    .map((vr) => ({
      text: vr.hazardText,
      sourceName: vr.sourceName,
      sourceOrg: vr.sourceOrg,
      dataReliability: vr.dataReliability,
      measures: (vr.measureText || "")
        .split("\n")
        .map((line) => line.trim())
        .filter((line) => line.length > 0)
        .map((line) => ({ text: line })),
    }))
);
// 확정 대화 카드 기본 표시분: 마지막 AI 발화(=확정된 이미지 이해)
const lastAssistantText = computed(() => {
  const v = visibleTurns.value;
  for (let i = v.length - 1; i >= 0; i--) {
    if (v[i].role === "assistant" && v[i].text) return v[i].text;
  }
  return "";
});

// ── 공통 헬퍼 ─────────────────────────────────────────────────
const scopeKeys = () => ({
  siteCd: props.scope.siteCd,
  processCd: props.scope.processCd,
  assessmentCd: props.scope.assessmentCd,
});

const scrollChatToBottom = () => {
  nextTick(() => {
    const el = chatLogRef.value;
    if (el) el.scrollTop = el.scrollHeight;
  });
};

// 서버 전체 상태 응답 반영 (derivation / chat-image / derive 공통.
//  imgConfirmed 는 BE legacy 필드 — v3.7 부터 FE 미사용이라 반영하지 않는다)
const applyResponse = (data) => {
  if (!data) return;
  chatTurns.value = data.chatTurns || [];
  suppDesc.value = data.suppDesc || "";
  hazards.value = data.hazards || [];
  citations.value = data.citations || [];
  verbatimRefs.value = data.verbatimRefs || [];
  abstained.value = !!data.abstained;
  disclaimer.value = data.disclaimer || "";
};

// 이력에 assistant 확인 질의가 1건 이상 존재하는지(kickoff 필요 여부 판단)
const hasAssistantTurn = () =>
  chatTurns.value.some((t) => t && t.role === "assistant");

// 복원 규칙(v3.7 2단계): 도출 데이터(hazards 또는 verbatimRefs)가 있으면 'result', 없으면 단계1.
//   IMG_CONFIRMED 기반 분기는 제거(confirm 단계 개념 소멸 — BE 컬럼은 legacy 존치).
const resolveStep = () => {
  step.value =
    hazards.value.length || verbatimRefs.value.length
      ? "result"
      : "imageConfirm";
};

// ── API 함수 ─────────────────────────────────────────────────
// 마운트 시 전체 상태 로드 → step 결정 → imageConfirm & assistant 발화 0 이면 kickoff 자동 실행
const fnLoad = async () => {
  initialLoading.value = true;
  let loaded = false;
  try {
    const response = await axios.get("/webApi/riskai01/derivation", {
      params: scopeKeys(),
    });
    if (response.status === 200) {
      applyResponse(response.data);
      resolveStep();
      scrollChatToBottom();
      loaded = true;
    }
  } catch (err) {
    // 초기 로드 실패: 기본 단계 유지 + 우측 컬럼 종류별 에러 표기(재시도는 패널 재오픈)
    const msg = resolveApiErrorMessage(
      err,
      "AI 분석 정보를 불러오지 못했습니다."
    );
    if (props.hasSourceImage) chatErrorMsg.value = msg;
    else deriveErrorMsg.value = msg;
  } finally {
    initialLoading.value = false;
  }
  // 자동 1차 판독(kickoff): 로드 성공 + 사진 有 단계1 + assistant 발화 0
  if (
    loaded &&
    props.hasSourceImage &&
    step.value === "imageConfirm" &&
    !hasAssistantTurn()
  ) {
    await fnKickoff();
  }
};

// 자동 첫 질의(kickoff). 서버가 숨김 user 턴 영속 후 VLM 확인 질의를 생성(멱등)
const fnKickoff = async () => {
  chatErrorMsg.value = "";
  chatSending.value = true;
  try {
    // ★타임아웃 상향: vision(VLM) 응답 지연 대비 — 전역 axios 10초로는 부족할 수 있음.
    const response = await axios.post(
      "/webApi/riskai01/chat-image",
      { ...scopeKeys(), kickoff: true },
      { headers: { "Content-Type": "application/json" }, timeout: 60000 }
    );
    if (response.status === 200) {
      applyResponse(response.data);
      scrollChatToBottom();
      // 서버에 미확정 작업분이 생성됨(commit-on-save 가드 대상) — 부모에 dirty 알림
      emit("touched");
    }
  } catch (err) {
    chatErrorMsg.value = resolveApiErrorMessage(
      err,
      "이미지 확인 질의 중 오류가 발생했습니다."
    );
    // 회사 월간 AI 토큰 쿼터 소진(AI_429_001) → Alert 모달 우선 표출(inline 병기 — §2-5)
    if (isAiQuotaExceeded(err)) {
      await proxy.$alert(chatErrorMsg.value);
    }
  } finally {
    chatSending.value = false;
  }
};

// 채팅 전송(v3.7 — 구 fnReask 통합): 설명/질문 + 첨부 이미지(전체 재전송 — 확정 결정 8) → AI 응답
const fnSendChat = async () => {
  chatErrorMsg.value = "";
  chatSending.value = true;
  try {
    // 텍스트 공백 + 이미지만 첨부한 경우 기본 문구로 대체
    const message = chatInput.value.trim() || CHAT_DEFAULT_MESSAGE;
    // ★타임아웃 상향: vision(VLM) 응답 지연 대비 — 전역 axios 10초로는 부족할 수 있음.
    const response = await axios.post(
      "/webApi/riskai01/chat-image",
      {
        ...scopeKeys(),
        userMessage: message,
        adminImages: adminImages.value.map((i) => ({
          base64: i.base64,
          mediaType: i.mediaType,
        })),
        suppDesc: suppDesc.value,
      },
      { headers: { "Content-Type": "application/json" }, timeout: 60000 }
    );
    if (response.status === 200) {
      applyResponse(response.data);
      // ★입력줄만 비움. 첨부 목록(adminImages)은 유지 — 후속 전송에 재전송
      chatInput.value = "";
      scrollChatToBottom();
      // 서버에 미확정 작업분이 생성됨(commit-on-save 가드 대상) — 부모에 dirty 알림
      emit("touched");
    }
  } catch (err) {
    chatErrorMsg.value = resolveApiErrorMessage(
      err,
      "이미지 분석 대화 중 오류가 발생했습니다."
    );
    // 회사 월간 AI 토큰 쿼터 소진(AI_429_001) → Alert 모달 우선 표출(inline 병기 — §2-5)
    if (isAiQuotaExceeded(err)) {
      await proxy.$alert(chatErrorMsg.value);
    }
  } finally {
    chatSending.value = false;
  }
};

// 파일 선택 → base64 변환. FE 선검증(서버 AI_400_004 와 동일 기준): 최대 2장/장당 3MB/jpg·png·webp
const fnPickImages = async (e) => {
  const input = e?.target;
  const files = Array.from(input?.files || []);
  try {
    for (const file of files) {
      if (adminImages.value.length >= ADMIN_IMAGE_MAX_COUNT) {
        await proxy.$alert("이미지는 최대 2장까지 첨부할 수 있습니다.");
        break;
      }
      if (!ADMIN_IMAGE_TYPES.includes(file.type)) {
        await proxy.$alert(
          "jpg/png/webp 형식의 이미지만 첨부할 수 있습니다.\n(" +
            file.name +
            ")"
        );
        continue;
      }
      if (file.size > ADMIN_IMAGE_MAX_BYTES) {
        await proxy.$alert(
          "장당 3MB 이하의 이미지만 첨부할 수 있습니다.\n(" + file.name + ")"
        );
        continue;
      }
      const base64 = await readFileAsBase64(file);
      adminImages.value.push({ base64, mediaType: file.type, name: file.name });
    }
  } catch (err) {
    await proxy.$alert("이미지를 읽는 중 오류가 발생했습니다.");
  } finally {
    // 같은 파일 재선택 허용
    if (input) input.value = "";
  }
};

const fnRemoveImage = (idx) => {
  adminImages.value.splice(idx, 1);
};

// 관리자 의견 blur 자동저장(best-effort — 실패는 콘솔 경고만, v2.1 supplement 재활용)
const fnSaveSupplement = async () => {
  try {
    await axios.post(
      "/webApi/riskai01/supplement",
      { ...scopeKeys(), suppDesc: suppDesc.value },
      { headers: { "Content-Type": "application/json" } }
    );
    // 서버에 미확정 작업분(관리자 의견)이 저장됨(commit-on-save 가드 대상) — 부모에 dirty 알림
    emit("touched");
  } catch (err) {
    console.warn("관리자 의견 저장 실패", err);
  }
};

// AI 유해요인/개선안 도출(RAG + LLM, 그룹 결과) → 결과 단계로
const fnDerive = async () => {
  deriveErrorMsg.value = "";
  deriving.value = true;
  try {
    // ★타임아웃 상향: derive 는 BE 에서 최대 3회 LLM 호출 체인(그라운딩 실패→재시도→자유생성 폴백)이
    //   발생할 수 있어 전역 axios 10초로는 부족(실측 10.5초 — BE 200 정상인데 FE 만 오류 표시).
    const response = await axios.post(
      "/webApi/riskai01/derive",
      { ...scopeKeys(), suppDesc: suppDesc.value },
      { headers: { "Content-Type": "application/json" }, timeout: 90000 }
    );
    if (response.status === 200) {
      applyResponse(response.data);
      // 선택 상태 초기화(새 도출 결과 기준). 참고 원문/전체 대화 토글도 기본 접힘으로 복귀
      activeHazardKey.value = null;
      selectedHazards.value = [];
      selectedMeasures.value = {};
      showVerbatim.value = false;
      showFullDialog.value = false;
      step.value = "result";
      // 부모(RiskAssessInfo) dirty 추적: 이번 세션에 도출을 수행했음을 알림(미저장 닫기 가드용)
      emit("derived");
    }
  } catch (err) {
    deriveErrorMsg.value = resolveApiErrorMessage(
      err,
      "AI 도출 중 오류가 발생했습니다."
    );
    // 회사 월간 AI 토큰 쿼터 소진(AI_429_001) → Alert 모달 우선 표출(inline 병기 — §2-5)
    if (isAiQuotaExceeded(err)) {
      await proxy.$alert(deriveErrorMsg.value);
    }
  } finally {
    deriving.value = false;
  }
};

// ── 결과 선택 (UI 토글만 — 저장 동작은 후속. 키='ai-N'|'vb-N') ───────────────────
// 아코디언 단일 확장(v3.8): 같은 행 재클릭 시 접힘, 다른 행 클릭 시 이전 행 접히고 그 행만 확장
const fnSelectHazard = (key) => {
  activeHazardKey.value = activeHazardKey.value === key ? null : key;
};
const fnToggleHazard = (key) => {
  const pos = selectedHazards.value.indexOf(key);
  if (pos >= 0) selectedHazards.value.splice(pos, 1);
  else selectedHazards.value.push(key);
};
const isMeasureSelected = (hzKey, msIdx) =>
  (selectedMeasures.value[hzKey] || []).includes(msIdx);
const fnToggleMeasure = (hzKey, msIdx) => {
  const cur = selectedMeasures.value[hzKey] || [];
  const pos = cur.indexOf(msIdx);
  if (pos >= 0) {
    cur.splice(pos, 1);
  } else {
    cur.push(msIdx);
    // 개선안을 선택하면 그 유해요인도 자동 선택(개선안만 수용되는 모순 방지).
    //   개선안 해제 시 유해요인 선택은 유지(관리자가 직접 해제).
    if (!selectedHazards.value.includes(hzKey)) {
      selectedHazards.value.push(hzKey);
    }
  }
  selectedMeasures.value = { ...selectedMeasures.value, [hzKey]: cur };
};

// 참고 원문(verbatim) 접이식 섹션 펼침/접힘 토글
const fnToggleVerbatim = () => {
  showVerbatim.value = !showVerbatim.value;
};

// ── 단계 칩 내비게이션(v3.7 2단계 — 로컬 step 전환만, 서버 상태 불변, 과거 값 열람 목적) ──
//   칩1(imageConfirm)=항상 이동 가능 / 칩2(결과)=도출 데이터(hazards 또는 verbatimRefs)가
//   있을 때만 이동 가능. aiBusy 중엔 전체 무시.
const resultAvailable = computed(
  () => hazards.value.length > 0 || verbatimRefs.value.length > 0
);
const canGoStep = (target) => {
  if (aiBusy.value) return false;
  if (target === "result") return resultAvailable.value;
  return true;
};
const stepChipClass = (target) => ({
  "ai-steps__item--active": step.value === target,
  "ai-steps__item--clickable": step.value !== target && canGoStep(target),
  "ai-steps__item--disabled": !canGoStep(target),
});
const fnGoStep = (target) => {
  if (target === step.value || !canGoStep(target)) return;
  step.value = target;
  if (target === "imageConfirm") scrollChatToBottom();
};

// ── 초기화(처음부터 다시): 서버 도출 행 DELETE(대화이력/도출결과/보완설명 전체) 후
//    로컬 상태 전부 초기화 → fnLoad 와 동일한 재진입 흐름(step 재결정 + 사진 있으면 kickoff 자동 실행) ──
const fnReset = async () => {
  const ok = await proxy.$confirm(
    "AI 분석 과정을 처음부터 다시 시작할까요? 대화 이력과 도출 결과가 모두 삭제됩니다."
  );
  if (!ok) return;
  initialLoading.value = true;
  try {
    await axios.post(
      "/webApi/riskai01/reset",
      { ...scopeKeys() },
      { headers: { "Content-Type": "application/json" } }
    );
    // 로컬 상태 전부 초기화(서버 행 DELETE 라 SUPP_DESC 포함 — suppDesc 도 비움)
    chatTurns.value = [];
    suppDesc.value = "";
    hazards.value = [];
    citations.value = [];
    verbatimRefs.value = [];
    abstained.value = false;
    chatErrorMsg.value = "";
    deriveErrorMsg.value = "";
    showVerbatim.value = false;
    showFullDialog.value = false;
    chatInput.value = "";
    adminImages.value = [];
    activeHazardKey.value = null;
    selectedHazards.value = [];
    selectedMeasures.value = {};
    initialLoading.value = false;
    // 재진입: fnLoad 재사용(step 재결정 + imageConfirm & assistant 발화 0 이면 kickoff)
    await fnLoad();
  } catch (err) {
    initialLoading.value = false;
    await proxy.$alert(
      resolveApiErrorMessage(err, "초기화 중 오류가 발생했습니다.")
    );
  }
};

// 원문(verbatim) 항목 출처표기 접미: 제3유형 변경금지 라이선스의 출처표시 의무.
//   원문 텍스트 자체는 한 글자도 변형하지 않고 뒤에 표기만 덧붙인다.
const buildVerbatimSourceSuffix = (vb) => {
  const org = (vb.sourceOrg || "").trim();
  const name = (vb.sourceName || "").trim();
  // 기관명·자료명 둘 다 비어 있으면 " (출처: )" 같은 빈 접미를 만들지 않고 생략
  if (!org && !name) return "";
  if (org && name) return ` (출처: ${org} · ${name})`;
  return ` (출처: ${org || name})`;
};

// 저장: 표시 순서(=배열 index 순서, ai 항목 먼저 → vb 항목)대로 선택 항목을 수집해 부모로 emit.
//   실제 필드 반영과 패널 닫기는 부모(RiskAssessInfo)가 수행한다(여기서 close emit 안 함).
const fnSaveSelection = async () => {
  const hazardLines = [];
  const measureLines = [];

  // AI 도출 항목('ai-N')
  hazards.value.forEach((hz, i) => {
    const key = "ai-" + i;
    if (selectedHazards.value.includes(key)) {
      hazardLines.push(hz.text);
    }
    // 체크된 개선안은 유해요인 선택 해제 여부와 무관하게 포함(인덱스 오름차순)
    const checkedIdxs = (selectedMeasures.value[key] || [])
      .slice()
      .sort((a, b) => a - b);
    checkedIdxs.forEach((j) => {
      const ms = (hz.measures || [])[j];
      if (ms) measureLines.push(ms.text);
    });
  });

  // verbatim 원문 항목('vb-N') — 출처표기 접미 부착
  verbatimHazardItems.value.forEach((vb, i) => {
    const key = "vb-" + i;
    const suffix = buildVerbatimSourceSuffix(vb);
    if (selectedHazards.value.includes(key)) {
      hazardLines.push(vb.text + suffix);
    }
    const checkedIdxs = (selectedMeasures.value[key] || [])
      .slice()
      .sort((a, b) => a - b);
    checkedIdxs.forEach((j) => {
      const ms = vb.measures[j];
      if (ms) measureLines.push(ms.text + suffix);
    });
  });

  if (hazardLines.length === 0 && measureLines.length === 0) {
    await proxy.$alert("반영할 유해요인 또는 개선안을 선택해 주세요.");
    return;
  }
  emit("save", { hazardLines, measureLines });
};

const fnClose = () => {
  emit("close");
};

onMounted(() => {
  fnLoad();
});
</script>

<style scoped>
/* ── 패널 루트: 팝업 전체(risk-popup-shell)를 덮는 오버레이 ── */
.ai-panel {
  position: absolute;
  inset: 0;
  z-index: 60;
  display: flex;
  flex-direction: column;
  background: var(--color-bg, #ffffff);
  overflow: hidden;
}

/* ── 상→하 슬라이드 트랜지션: 부모(RiskAssessInfo) scoped 에 정의(Transition 이 부모 소유) ── */

/* ── 블러 오버레이 ── */
.ai-panel__busy {
  position: absolute;
  inset: 0;
  z-index: 70;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  color: var(--color-text, #374151);
}
.ai-panel__busy-text {
  font-weight: 600;
}

/* ── 헤더 ── */
.ai-panel__header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.25rem;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
  background: var(--modal-header-bg, rgba(22, 163, 74, 0.04));
}
.ai-panel__title {
  font-weight: 700;
  color: var(--color-text-strong, #111827);
}
.ai-panel__reset {
  flex-shrink: 0;
  font-size: 0.85rem;
  padding: 0.25rem 0.7rem;
}
.ai-steps {
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  gap: 0.4rem;
  list-style: none;
  margin: 0;
  padding: 0;
}
.ai-steps__item {
  color: var(--color-text-muted, #6b7280);
  font-size: 0.85rem;
  padding: 0.2rem 0.6rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--btn-radius, 6px);
  background: var(--color-surface, #f9fafb);
}
.ai-steps__item + .ai-steps__item::before {
  content: none;
}
.ai-steps__item--active {
  color: #fff;
  background: var(--color-primary, #16a34a);
  border-color: var(--color-primary, #16a34a);
  font-weight: 600;
}
/* 칩 내비게이션: 이동 가능 칩은 pointer+hover 강조, 불가 칩은 muted */
.ai-steps__item--clickable {
  cursor: pointer;
}
.ai-steps__item--clickable:hover {
  border-color: var(--color-primary, #16a34a);
  color: var(--color-primary, #16a34a);
  background: var(--color-primary-soft, rgba(22, 163, 74, 0.06));
}
.ai-steps__item--disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.icon-button {
  flex-shrink: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  width: 1.5rem;
  height: 1.5rem;
  color: var(--color-text, #374151);
  padding: 0;
}

/* ── 버튼 변형(프로젝트 규약 — 전역 .btn 베이스 + 컴포넌트 scoped 변형, RiskAssessInfo 팔레트 동일) ── */
.btn-save {
  background: var(--color-primary, #16a34a);
  color: #ffffff;
  border: none;
}
.btn-save:hover:not(:disabled) {
  background: var(--color-primary-strong, #15803d);
}
.btn-cancel {
  background: var(--color-bg, #ffffff);
  color: var(--color-text, #374151);
  border: 1px solid var(--color-border, #e5e7eb);
}
.btn-cancel:hover:not(:disabled) {
  background: var(--color-surface, #f9fafb);
}
.btn-report {
  background: var(--color-bg, #ffffff);
  color: var(--color-primary, #16a34a);
  border: 1px solid var(--color-primary, #16a34a);
}
.btn-report:hover:not(:disabled) {
  background: var(--color-primary-soft, rgba(22, 163, 74, 0.06));
}
.btn-report:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ── 본문 공통 ── */
.ai-panel__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 1.25rem 1.5rem;
}
.ai-panel__state {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--color-text-muted, #6b7280);
  margin: 0.5rem 0 0;
}
.ai-panel__state--error {
  color: var(--color-danger, #b91c1c);
}
.ai-panel__note {
  color: var(--color-warning-text, #92400e);
  background: var(--color-warning-bg, #fef3c7);
  border-radius: var(--btn-radius, 6px);
  padding: 0.4rem 0.6rem;
  margin: 0.75rem 0 0;
  font-size: 0.85rem;
}
.ai-panel__disclaimer {
  color: var(--color-text-muted, #6b7280);
  font-size: 0.78rem;
  margin: 0.5rem 0 0;
}

/* ── STEP 1: 이미지 확정(사진 有) / 정보 확인(사진 無) — v3.7 ── */
.ai-confirm {
  display: flex;
  gap: 1.25rem;
  height: 100%;
  min-height: 0;
}
/* 좌측 컬럼(고정폭): 상단 이미지(크기 고정) + 하단 개선 전 내용 요약 */
.ai-confirm__left {
  flex: 0 0 34%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.ai-confirm__photo {
  flex: 0 0 auto;
  height: 260px;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 4px);
  background: var(--color-surface, #f9fafb);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.ai-confirm__photo-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
.ai-confirm__photo-empty {
  align-self: center;
  color: var(--color-text-muted, #6b7280);
  font-size: 0.85rem;
}
.ai-confirm__chat {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
/* 우측(사진 無): 관리자 의견 */
.ai-confirm__opinion {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.ai-confirm__opinion-label {
  font-weight: 600;
  color: var(--color-text, #374151);
  font-size: 0.9rem;
}
.ai-confirm__opinion-field {
  resize: vertical;
  min-height: 2.4rem;
  padding: 0.5rem 0.6rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 4px);
  background: var(--color-bg, #ffffff);
  color: var(--color-text, #374151);
  font-size: 0.88rem;
  font-family: inherit;
}
.ai-confirm__opinion-field:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.ai-confirm__opinion-hint {
  margin: 0;
  color: var(--color-text-muted, #6b7280);
  font-size: 0.78rem;
}
.ai-chat__log {
  flex: 1 1 auto;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  overflow-y: auto;
  padding: 0.6rem;
  background: var(--color-surface, #f9fafb);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 4px);
}
.ai-chat__row {
  display: flex;
}
.ai-chat__row--user {
  justify-content: flex-end;
}
.ai-chat__row--assistant {
  justify-content: flex-start;
}
.ai-chat__bubble {
  max-width: 78%;
  padding: 0.45rem 0.7rem;
  border-radius: var(--input-radius, 4px);
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 0.88rem;
  line-height: 1.4;
}
.ai-chat__row--user .ai-chat__bubble {
  background: var(--color-primary, #16a34a);
  color: #fff;
}
.ai-chat__row--assistant .ai-chat__bubble {
  background: var(--color-bg, #ffffff);
  color: var(--color-text, #374151);
  border: 1px solid var(--color-border, #e5e7eb);
}
.ai-chat__empty {
  color: var(--color-text-muted, #6b7280);
  font-size: 0.85rem;
  padding: 0.4rem 0;
  text-align: center;
}
/* 채팅 입력줄(v3.7 — 상시 노출: textarea + 이미지 첨부 + 전송) */
.ai-chat__input {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.ai-chat__input-field {
  resize: vertical;
  min-height: 2.4rem;
  padding: 0.5rem 0.6rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 4px);
  background: var(--color-bg, #ffffff);
  color: var(--color-text, #374151);
  font-size: 0.88rem;
  font-family: inherit;
}
.ai-chat__input-field:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.ai-chat__input-actions {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}
.ai-chat__send {
  margin-left: auto;
}
.ai-chat__send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.ai-correct__attach-hint {
  color: var(--color-text-muted, #6b7280);
  font-size: 0.78rem;
}
.ai-correct__chips {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}
.ai-correct__chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.2rem 0.5rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--btn-radius, 6px);
  background: var(--color-bg, #ffffff);
  font-size: 0.8rem;
  color: var(--color-text, #374151);
}
.ai-correct__chip-name {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
/* ── 개선 전 내용 요약 카드(v3.7 — 단계1 좌측 하단으로 이동, 클래스명은 기존 유지) ── */
.ai-derive__summary {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 4px);
  background: var(--color-surface, #f9fafb);
  padding: 0.9rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.ai-derive__summary-title {
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  font-size: 0.9rem;
}
.ai-derive__row {
  display: flex;
  gap: 0.75rem;
  font-size: 0.88rem;
}
.ai-derive__row--multiline {
  align-items: flex-start;
}
.ai-derive__row-label {
  flex: 0 0 100px;
  color: var(--color-text-muted, #6b7280);
  font-weight: 500;
}
.ai-derive__row-value {
  flex: 1 1 auto;
  color: var(--color-text, #374151);
  white-space: pre-wrap;
  word-break: break-word;
}

/* ── STEP 2: 결과 (v3.8 — 좌 40% 컨텍스트 / 우 60% 아코디언, 양측 세로 스크롤) ── */
.ai-result {
  display: flex;
  gap: 1.25rem;
  height: 100%;
  min-height: 0;
}
.ai-result__left {
  flex: 0 0 40%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  overflow-y: auto;
  min-height: 0;
}
.ai-result__right {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  overflow-y: auto;
  min-height: 0;
}
.ai-result__col-title {
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  font-size: 0.9rem;
}
.ai-result__check {
  flex: 0 0 auto;
  margin-top: 0.2rem;
  cursor: pointer;
}
.ai-result__text {
  flex: 1 1 auto;
  word-break: break-word;
  font-size: 0.88rem;
  line-height: 1.4;
}
.ai-result__empty {
  color: var(--color-text-muted, #6b7280);
  font-size: 0.85rem;
  padding: 0.4rem 0;
}

/* 확정 대화 카드(좌측 하단 — 기본 마지막 AI 발화, 토글로 전체 대화) */
.ai-result__dialog {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 4px);
  background: var(--color-surface, #f9fafb);
  padding: 0.75rem 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.ai-result__dialog-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}
.ai-result__dialog-title {
  font-weight: 600;
  color: var(--color-text-strong, #111827);
  font-size: 0.9rem;
}
.ai-result__dialog-toggle {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
  font-size: 0.78rem;
  color: var(--color-primary, #16a34a);
  font-family: inherit;
}
.ai-result__dialog-last {
  color: var(--color-text, #374151);
  font-size: 0.85rem;
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-word;
}
.ai-result__dialog-full {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  max-height: 260px;
  overflow-y: auto;
}
.ai-result__dialog-line {
  display: flex;
  gap: 0.5rem;
  font-size: 0.85rem;
  align-items: flex-start;
}
.ai-result__dialog-role {
  flex: 0 0 44px;
  color: var(--color-text-muted, #6b7280);
  font-weight: 600;
}
.ai-result__dialog-text {
  flex: 1 1 auto;
  color: var(--color-text, #374151);
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.45;
}

/* 유해요인 아코디언(v3.8 — 행 클릭 시 아래로 개선안 확장, 단일 확장) */
.ai-acc {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.ai-acc__item {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--btn-radius, 6px);
  background: var(--color-surface, #f9fafb);
  overflow: hidden;
}
.ai-acc__row {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.5rem 0.65rem;
  color: var(--color-text, #374151);
  cursor: pointer;
}
.ai-acc__row--active {
  background: var(--color-primary-soft, rgba(22, 163, 74, 0.08));
}
/* verbatim 원문 행(AI 도출과 시각 구분 — warning 계열 좌측 보더) */
.ai-acc__row--verbatim {
  border-left: 3px solid var(--color-warning-text, #92400e);
}
.ai-acc__arrow {
  flex: 0 0 auto;
  margin-top: 0.2rem;
  font-size: 0.7rem;
  color: var(--color-text-muted, #6b7280);
}
.ai-acc__body {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}
.ai-acc__panel {
  border-top: 1px solid var(--color-border, #e5e7eb);
  background: var(--color-bg, #ffffff);
  padding: 0.5rem 0.65rem 0.6rem 2rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.ai-acc__measures {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.ai-acc__measure {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
}

/* 출처 링크(근거 출처/참고 원문 공통 — 새 창) */
.ai-src-link {
  color: var(--color-primary, #16a34a);
  text-decoration: underline;
  word-break: break-all;
}
.ai-vb-badge {
  display: inline-block;
  margin-right: 0.3rem;
  padding: 0.05rem 0.35rem;
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--color-warning-text, #92400e);
  background: var(--color-warning-bg, #fef3c7);
  border-radius: var(--btn-radius, 6px);
  vertical-align: baseline;
}
.ai-result__vb-source {
  color: var(--color-text-muted, #6b7280);
  font-size: 0.75rem;
}
.ai-result__vb-note {
  margin: 0;
  color: var(--color-text-muted, #6b7280);
  font-size: 0.78rem;
}
.ai-cite {
  color: var(--color-primary, #16a34a);
  font-size: 0.7rem;
}
/* ── 참고 원문(verbatim 패스스루 — 접이식, 기본 접힘) ── */
.ai-verbatim {
  margin-top: 0.5rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--btn-radius, 6px);
  background: var(--color-surface, #f9fafb);
}
.ai-verbatim__toggle {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  width: 100%;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0.5rem 0.65rem;
  font-weight: 600;
  font-size: 0.88rem;
  color: var(--color-text-strong, #111827);
  text-align: left;
  font-family: inherit;
}
.ai-verbatim__toggle-arrow {
  flex-shrink: 0;
  font-size: 0.7rem;
  color: var(--color-text-muted, #6b7280);
}
.ai-verbatim__body {
  padding: 0 0.65rem 0.65rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.ai-verbatim__notice {
  margin: 0;
  color: var(--color-text-muted, #6b7280);
  font-size: 0.78rem;
}
.ai-verbatim__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.ai-verbatim__item {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  padding: 0.5rem 0.65rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--input-radius, 4px);
  background: var(--color-bg, #ffffff);
}
.ai-verbatim__source {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.ai-verbatim__source-name {
  font-weight: 600;
  font-size: 0.85rem;
  color: var(--color-text-strong, #111827);
}
/* v3.8 출처 보강: 기관명/라이선스 표기 */
.ai-verbatim__org {
  font-size: 0.82rem;
  color: var(--color-text, #374151);
}
.ai-verbatim__license {
  flex-shrink: 0;
  font-size: 0.72rem;
  padding: 0.1rem 0.45rem;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--btn-radius, 6px);
  color: var(--color-text-muted, #6b7280);
  background: var(--color-bg, #ffffff);
}
.ai-verbatim__badge {
  flex-shrink: 0;
  font-size: 0.72rem;
  padding: 0.1rem 0.45rem;
  border: 1px solid var(--color-primary, #16a34a);
  border-radius: var(--btn-radius, 6px);
  color: var(--color-primary, #16a34a);
  background: var(--color-primary-soft, rgba(22, 163, 74, 0.06));
}
.ai-verbatim__field {
  display: flex;
  gap: 0.5rem;
  font-size: 0.82rem;
  align-items: flex-start;
}
.ai-verbatim__field-label {
  flex: 0 0 60px;
  color: var(--color-text-muted, #6b7280);
  font-weight: 500;
}
.ai-verbatim__field-value {
  flex: 1 1 auto;
  color: var(--color-text, #374151);
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.4;
}

.ai-result__cites {
  margin-top: 0.5rem;
}
.ai-cite-list {
  margin: 0.35rem 0 0;
  padding-left: 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.ai-cite-list__item {
  color: var(--color-text-muted, #6b7280);
  font-size: 0.8rem;
  display: flex;
  gap: 0.4rem;
}
.ai-cite-list__marker {
  color: var(--color-primary, #16a34a);
}

/* ── 푸터 ── */
.ai-panel__footer {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border-top: 1px solid var(--color-border, #e5e7eb);
  background: var(--modal-footer-bg, #f9fafb);
}
.ai-panel__footer-left,
.ai-panel__footer-right {
  display: flex;
  gap: 0.5rem;
}
.ai-panel__footer .btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
