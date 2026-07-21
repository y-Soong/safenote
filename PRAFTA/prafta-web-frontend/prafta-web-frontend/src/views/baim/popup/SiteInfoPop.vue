<template>
  <Transition name="fade">
    <div v-show="true" class="modal-overlay prafta-modal-popup">
      <div
        class="modal-content-wide"
        :style="{ top: position.y + 'px', left: position.x + 'px' }"
        ref="modalRef"
      >
        <!-- 🔹 Title  v-if="visible" -->
        <div class="modal-header" @mousedown="startDrag">
          <span>사업장정보</span>
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
            <!-- PRAFTA-SUBCON-T2-09: 미러(연동) 사업장 안내 — 강제는 서버(T2-04)가 담당. -->
            <p v-if="isMirror" class="mirror-guide">
              연동(읽기전용) 사업장입니다. 담당자 지정만 변경할 수 있습니다.
            </p>
            <div class="form-row-max">
              <label>사업장</label>
              <input
                id="siteNo"
                v-model="siteNo"
                placeholder="사업장코드"
                :disabled="isMirror"
                @blur="focusKill"
              />
              <div class="editable-form">
                <input
                  style="width: 28rem"
                  v-model="siteNm"
                  ref="siteNmFcs"
                  placeholder="사업장명"
                  :disabled="isMirror"
                />
              </div>
            </div>
            <!-- 주소찾기 -->
            <div class="form-row-max">
              <label>주소</label>
              <div class="form-row editable-form">
                <input
                  style="width: 10.5rem"
                  v-model="zipCode"
                  placeholder="우편번호"
                  disabled
                />
              </div>
              <button
                class="btn btn-primary"
                ref="addrFcs"
                style="margin-left: 1px"
                :disabled="isMirror"
                @click="onClickAddressSearch"
              >
                주소찾기
              </button>
              <div class="editable-form">
                <input
                  style="width: 28rem"
                  v-model="addr1"
                  placeholder="기본주소"
                  disabled
                />
              </div>
            </div>

            <div class="form-row-max editable-form">
              <label>상세주소</label>
              <input
                v-model="addr2"
                placeholder="상세주소"
                :disabled="isMirror"
              />
            </div>

            <div class="form-row-max">
              <label>사업개시일</label>
              <CalendarSrch
                style="width: 10rem; height: 2rem"
                v-model="strDate"
                :readonly="readonly"
              />
              <label>사업종료일</label>
              <CalendarSrch
                style="width: 10rem; height: 2rem"
                v-model="endDate"
                :min-date="endDateMinDate"
                :readonly="isMirror"
              />
            </div>

            <div class="form-row-max">
              <label>사용여부</label>
              <div style="width: 10rem">
                <BaseSelect id="useYn" v-model="useYn" :disabled="isMirror">
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

              <label>관리자</label>
              <div>
                <input
                  id="siteAdminNm"
                  style="margin-left: 2px; width: 10rem"
                  v-model="siteAdminNm"
                  disabled
                />
              </div>
              <button
                class="btn btn-primary"
                ref="siteAdminSrchBtnFcs"
                style="margin-left: 1px"
                @click="fnUserSearchPopOpen"
              >
                계정찾기
              </button>
            </div>

            <div class="form-row-max">
              <label>사업장전화번호</label>
              <div>
                <input
                  id="telNo"
                  ref="telNoFcs"
                  style="width: 10rem"
                  v-model="telNo"
                  :disabled="isMirror"
                  @blur="focusKill"
                />
              </div>
              <label>GPS 반경</label>
              <div class="gps-range-field">
                <input
                  id="gpsRange"
                  style="width: 5rem"
                  v-model="gpsRange"
                  maxlength="4"
                  :disabled="isMirror"
                  @input="handleGpsRangeInput"
                  @blur="focusKill"
                />
                <span class="gps-range-unit">(m)</span>
              </div>
            </div>

            <div class="form-row-max">
              <label>사업장비고</label>
              <textarea
                id="siteDesc"
                ref="siteDescFcs"
                style="width: 100%"
                v-model="siteDesc"
                :disabled="isMirror"
              />
            </div>
          </div>

          <!-- 🔹 Map -->
          <div class="map-container">
            <div id="kakao-map" ref="mapContainer"></div>
          </div>
        </div>

        <!-- 🔹 Footer: 초기화/저장 (가이드 구분선 적용) -->
        <div class="modal-footer">
          <div class="btn-group">
            <button class="btn btn-primary" @click="props.reset">초기화</button>
            <!-- 좌표 산출 중에는 저장 차단 — 이전 주소 좌표가 저장되는 것을 막는다. -->
            <button
              class="btn btn-primary"
              :disabled="geocoding"
              @click="fnSiteSave"
            >
              {{ geocoding ? "좌표 확인 중..." : "저장" }}
            </button>
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
  computed,
  defineProps,
  defineEmits,
  onMounted,
  onBeforeUnmount,
  watch,
  getCurrentInstance,
  nextTick,
} from "vue";
import axios from "@/api/axios";
import { resolveApiErrorMessage } from "@/utils/apiError";
import { fnSearchAddress } from "@/utils/addrUtil";
import { useModal } from "@/utils/useModal";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import CalendarSrch from "@/components/common/CalendarSrch.vue";
import UserSearchPop from "@/components/popup/UserSearchPop.vue";
import BaseSelect from "@/components/common/BaseSelect.vue";

const readonly = ref(true);
const modalRef = ref(null);
const emit = defineEmits(["close"]);
const systCodeArr = ref({});

const { open: openPop } = useModal();

const props = defineProps({
  // visible: Boolean,
  cmpnyCd_p: String,
  siteCd_p: String,
  onSelect: Function,
  reset: Function,
});

// 공통 훅으로 화면 중앙(살짝 위쪽)에 배치 + 드래그 가능
const { position, startDrag } = useCenteredDraggable(modalRef, {
  horizontalRatio: 2,
  verticalRatio: 3.5,
});

/* 회원가입정보 */
const cmpnyCd = ref("");
const siteCd = ref("");
const siteNo = ref("");
const siteNm = ref("");
const siteNmFcs = ref("");
const addr1 = ref("");
const addr2 = ref("");
const addrFcs = ref("");
const zipCode = ref("");
const strDate = ref("");
const endDate = ref("");
const useYn = ref("");
const siteAdminCd = ref("");
const siteAdminNm = ref("");
const siteAdminSrchBtnFcs = ref("");
const telNo = ref("");
const telNoFcs = ref("");
const gpsRange = ref("");
const siteDesc = ref("");
const siteDescFcs = ref("");

// PRAFTA-SUBCON-T2-09: 미러(연동) 사업장 여부 — 잠금 필드 입력 비활성 근거(서버 T2-04 가 최종 강제).
const isMirror = ref(false);

// 지오코딩 산출 좌표 (BE 계약 필드명 lat/lon, 문자열 전송)
const lat = ref("");
const lon = ref("");

// 지오코딩 진행 상태 — addressSearch 는 비동기 콜백이라, 주소 변경 직후 곧바로 저장하면
//   lat/lon 이 아직 '이전 주소'의 좌표인 채로 전송될 수 있다(주소-좌표 불일치가 무경고로 저장됨).
//   저장 버튼 비활성 + 저장 시 in-flight 결과 대기, 2중으로 막는다.
const geocoding = ref(false);
let geocodePromise = null;

// 콜백이 끝내 오지 않는 경우(네트워크 단절 등) 저장이 영구 대기하지 않도록 하는 상한.
const GEOCODE_TIMEOUT_MS = 5000;

const mapContainer = ref(null);
let map = null;
let marker = null;
let circle = null; // 원 객체
let geocoder = null;

// 반경 설정 (미터 단위) - 기본값 100미터
const mapRadius = ref(100);

const { proxy } = getCurrentInstance();

// 카카오 지도 API 로드
const loadKakaoMapScript = () => {
  return new Promise((resolve, reject) => {
    // 이미 로드되어 있는지 확인
    if (window.kakao && window.kakao.maps) {
      resolve();
      return;
    }

    // 이미 로딩 중인 스크립트가 있는지 확인
    const existingScript = document.querySelector(
      'script[src*="dapi.kakao.com"]'
    );
    if (existingScript) {
      // 기존 스크립트가 로드될 때까지 대기
      const checkInterval = setInterval(() => {
        if (window.kakao && window.kakao.maps) {
          clearInterval(checkInterval);
          resolve();
        }
      }, 100);

      // 타임아웃 설정 (10초)
      setTimeout(() => {
        clearInterval(checkInterval);
        if (!window.kakao || !window.kakao.maps) {
          reject(new Error("카카오 지도 API 로드 타임아웃"));
        }
      }, 10000);

      return;
    }

    // Vite: VITE_ 접두사 사용
    const kakaoKey = import.meta.env.VITE_PUBLIC_KAKAO_APP_JS_KEY;

    if (!kakaoKey) {
      reject(new Error("카카오 지도 API 키가 없습니다."));
      return;
    }

    const scriptUrl = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${kakaoKey}&libraries=services&autoload=false`;

    // fetch HEAD는 CORS로 차단되므로 스크립트 태그로 직접 로드 (script 태그는 CORS 제한 없음)
    const script = document.createElement("script");
    script.src = scriptUrl;
    script.async = true;

    script.onload = () => {
      if (window.kakao && window.kakao.maps) {
        window.kakao.maps.load(() => {
          resolve();
        });
      } else {
        reject(new Error("카카오 지도 API 객체를 찾을 수 없습니다."));
      }
    };

    script.onerror = () => {
      reject(
        new Error(
          "카카오 지도 API 로드 실패. 카카오 개발자 콘솔에서 (1) JavaScript 키 사용 여부, (2) 사이트 도메인에 http://localhost:8081 등록 여부를 확인하세요."
        )
      );
    };

    document.head.appendChild(script);
  });
};

// 지도 초기화
const initMap = async () => {
  if (!mapContainer.value) return;

  try {
    await loadKakaoMapScript();

    const defaultPosition = new window.kakao.maps.LatLng(37.5665, 126.978); // 서울시청 기본 위치

    const mapOption = {
      center: defaultPosition,
      level: 3,
    };

    map = new window.kakao.maps.Map(mapContainer.value, mapOption);
    geocoder = new window.kakao.maps.services.Geocoder();

    // 기존 주소가 있으면 지도에 표시
    if (addr1.value) {
      updateMapLocation(addr1.value);
    }
  } catch (error) {
    console.error("지도 초기화 실패:", error);
  }
};

// 주소로 지도 위치 업데이트 (지오코딩 완료 시점을 저장 로직이 기다릴 수 있도록 Promise 반환)
const updateMapLocation = (address) => {
  if (!geocoder || !map || !address) return Promise.resolve();

  geocoding.value = true;
  geocodePromise = new Promise((resolve) => {
    let settled = false;
    const done = () => {
      if (settled) return;
      settled = true;
      geocoding.value = false;
      resolve();
    };
    // 콜백 미도착 대비 상한 — 시간 초과 시 좌표 없음으로 확정(저장 시 D4 경고 트리거)
    const timer = setTimeout(() => {
      if (settled) return;
      lat.value = "";
      lon.value = "";
      console.warn("주소 검색 시간 초과:", address);
      done();
    }, GEOCODE_TIMEOUT_MS);

    geocoder.addressSearch(address, (result, status) => {
      clearTimeout(timer);
      if (settled) return;
      try {
        applyGeocodeResult(result, status);
      } finally {
        done();
      }
    });
  });
  return geocodePromise;
};

// 지오코딩 콜백 본문 — 성공 시 좌표/마커/반경원 갱신, 실패 시 좌표 초기화.
const applyGeocodeResult = (result, status) => {
  {
    if (status === window.kakao.maps.services.Status.OK) {
      // 산출 좌표 보관 (위도=y, 경도=x). 저장 payload 및 지오펜스 판정에 사용
      lat.value = String(result[0].y);
      lon.value = String(result[0].x);

      const coords = new window.kakao.maps.LatLng(result[0].y, result[0].x);

      // 기존 마커 제거
      if (marker) {
        marker.setMap(null);
      }

      // 기존 원 제거
      if (circle) {
        circle.setMap(null);
      }

      // 새 마커 생성
      marker = new window.kakao.maps.Marker({
        map: map,
        position: coords,
      });

      // 반경 원 생성 (gpsRange.value를 숫자로 변환, 없으면 기본값 100)
      const radiusValue = Number(gpsRange.value) || 100;

      circle = new window.kakao.maps.Circle({
        center: coords,
        radius: radiusValue, // 미터 단위
        strokeWeight: 2, // 선 두께
        strokeColor: "#75B8FA", // 선 색상
        strokeOpacity: 0.8, // 선 투명도
        strokeStyle: "solid", // 선 스타일
        fillColor: "#75B8FA", // 채우기 색상
        fillOpacity: 0.2, // 채우기 투명도
      });

      // 원을 지도에 표시
      circle.setMap(map);

      // 지도 중심 이동 및 적절한 줌 레벨 설정
      map.setCenter(coords);

      // 반경에 맞게 지도 레벨 조정 (반경이 클수록 더 넓게)
      const radius = radiusValue;
      let level = 3; // 기본 레벨

      map.setLevel(level);
    } else {
      // 좌표 산출 실패 시 보관 좌표 초기화 (저장 시 D4 경고 트리거)
      lat.value = "";
      lon.value = "";
      console.warn("주소 검색 실패:", status);
    }
  }
};

// PRAFTA-COM-001-T2-5: 날짜를 YYYY-MM-DD 로 정규화(DB값 YYYYMMDD / 신규 YYYY-MM-DD 혼재 대응).
//   CalendarSrch(flatpickr) 의 minDate 는 YYYY-MM-DD 형식을 기대한다.
function toYmdDash(val) {
  if (proxy.$util.isEmpty(val)) return "";
  const compact = String(val).replace(/-/g, "");
  if (compact.length !== 8 || /[^0-9]/.test(compact)) return "";
  return `${compact.slice(0, 4)}-${compact.slice(4, 6)}-${compact.slice(6, 8)}`;
}

// 사업종료일 선택 하한 = 사업개시일 당일(5.2.1: 개시일 이후 모든 날짜, 개시일 당일 허용).
const endDateMinDate = computed(() => toYmdDash(strDate.value));

// PRAFTA-COM-001-T2-5: 사업개시일 변경 시 종료일이 그보다 앞서면 종료일 초기화(UI 정합).
watch(
  () => strDate.value,
  () => {
    const startYmd = toYmdDash(strDate.value).replace(/-/g, "");
    const endYmd = toYmdDash(endDate.value).replace(/-/g, "");
    if (startYmd && endYmd && endYmd < startYmd) {
      endDate.value = "";
    }
  }
);

// 주소 변경 감시
watch(
  () => addr1.value,
  (newAddr) => {
    if (newAddr && map) {
      // 지도 초기화 이후의 주소 변경 = 사용자가 주소찾기로 새 주소를 고른 것.
      //   이전 주소의 좌표는 즉시 무효화한다 — 지오코딩 결과가 오기 전에 저장되더라도
      //   낡은 좌표가 새 주소와 함께 실리지 않게(최악의 경우 좌표 없음 confirm 으로 귀결).
      //   ※ 최초 로드(fnGetSiteInfo)는 map 이 아직 null 이라 여기 걸리지 않으므로
      //     DB 복원 좌표는 보존된다.
      lat.value = "";
      lon.value = "";
      nextTick(() => {
        updateMapLocation(newAddr);
      });
    } else if (!newAddr) {
      // 주소가 비면 보관 좌표 초기화
      lat.value = "";
      lon.value = "";
    }
  }
);

onMounted(async () => {
  await fnGetSystinfoList();
  cmpnyCd.value = props.cmpnyCd_p;

  if (props.siteCd_p) {
    siteCd.value = props.siteCd_p;
    await fnGetSiteInfo(siteCd.value);
  } else {
    useYn.value = "Y";
    strDate.value = proxy.$util.getToday();
  }

  // 지도 초기화
  await nextTick();
  initMap();
});

onBeforeUnmount(() => {
  if (marker) {
    marker.setMap(null);
    marker = null;
  }
  if (circle) {
    circle.setMap(null);
    circle = null;
  }
  map = null;
  geocoder = null;
});

// GPS 반경 입력 제한 (4자리 숫자만)
const handleGpsRangeInput = (e) => {
  let value = e.target.value;
  // 숫자가 아닌 문자 제거
  value = value.replace(/[^0-9]/g, "");
  // 4자리 제한
  if (value.length > 4) {
    value = value.slice(0, 4);
  }
  gpsRange.value = value;
  e.target.value = value;
};

// focusKill 이벤트
function focusKill(e) {
  if (e.target.id == "siteNo") {
    if (/[^a-zA-Z0-9]/.test(siteNo.value)) {
      const alertMsg =
        "사업장 코드는 숫자, 영문자, 숫자+영문자 구성으로만 생성할 수 있습니다.";
      fnAlertMsg(alertMsg, () => {
        siteNo.value = siteNo.value.replace(/[^a-zA-Z0-9]/g, "");
      });
    }
  } else if (e.target.id == "telNo") {
    if (proxy.$util.isNotEmpty(telNo.value)) {
      telNoFocusKill();
    }
  }
}

// API 호출
const fnGetSystinfoList = async () => {
  try {
    const response = await axios.get("/comApi/baseinfo/syst-info-lists", {
      params: {
        systCodeList: ["SYS003"],
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
    fnAlertMsg(resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다."));
  }
};

const fnGetSiteInfo = async (siteCd) => {
  try {
    const response = await axios.get("/webApi/baim01/site-info-lists", {
      params: {
        cmpnyCd: cmpnyCd.value,
        siteCd: siteCd,
      },
    });
    if (response.status === 200) {
      if (response.data?.siteInfoList?.length == 1) {
        siteNo.value = response.data?.siteInfoList[0].siteNo;
        siteNm.value = response.data?.siteInfoList[0].siteNm;
        zipCode.value = response.data?.siteInfoList[0]?.zipCode;
        addr1.value = response.data?.siteInfoList[0]?.addr1;
        addr2.value = response.data?.siteInfoList[0]?.addr2;
        // PRAFTA-COM-001-T2: DB 는 YYYYMMDD(대시 없음) 저장. flatpickr(dateFormat Y-m-d)
        //   는 대시 구분자로 파싱하므로 무대시 값은 오파싱된다 → 로드 즉시 YYYY-MM-DD 로 정규화.
        strDate.value = toYmdDash(response.data?.siteInfoList[0].strDate);
        endDate.value = toYmdDash(response.data?.siteInfoList[0].endDate);
        useYn.value = response.data?.siteInfoList[0].useYn;
        telNo.value = response.data?.siteInfoList[0].telNo;
        gpsRange.value = response.data?.siteInfoList[0].gpsRange;
        siteDesc.value = response.data?.siteInfoList[0].siteDesc;
        siteAdminCd.value = response.data?.siteInfoList[0].siteAdminCd;
        siteAdminNm.value = response.data?.siteInfoList[0].siteAdminNm;
        // PRAFTA-SUBCON-T2-09: 미러 여부(linkSrcCmpnyCd NOT NULL) — 잠금 필드 비활성 판정.
        isMirror.value = !!response.data?.siteInfoList[0].linkSrcCmpnyCd;
        // 저장된 좌표 복원 (지도/저장 정확도 유지). 이후 initMap 지오코딩으로 갱신될 수 있음
        lat.value = proxy.$util.isEmpty(response.data?.siteInfoList[0].lat)
          ? ""
          : String(response.data?.siteInfoList[0].lat);
        lon.value = proxy.$util.isEmpty(response.data?.siteInfoList[0].lon)
          ? ""
          : String(response.data?.siteInfoList[0].lon);
      }
    }
  } catch (err) {
    fnAlertMsg(resolveApiErrorMessage(err, "조회 중 오류가 발생했습니다."));
  }
};

const fnSiteSave = async () => {
  fnConfirmMsg("저장하시겠습니까 ?", async () => {
    if (!fnSiteInfoValidationChk()) {
      return;
    }

    // PRAFTA-COM-001-T2-5: 과거 종료일 + 사용여부 'Y' 자동 제안.
    //   BE resolveEndDateBoundary(5.2.4 재개방): 종료일이 과거인데 사용여부가 'Y' 이면
    //   "종료 취소(재개방)"로 간주해 종료일을 NULL 처리한다 → 사용자는 "저장됐는데 종료일이
    //   안 들어간다"고 느낀다. 종료를 의도했을 수 있으므로 '미사용' 전환을 제안한다.
    //   (당일은 BE 가 useYn 무관하게 종료 처리하므로 제외 — 엄격 과거만 대상)
    const todayYmd = proxy.$util.getToday().replace(/-/g, "");
    const endYmd = toYmdDash(endDate.value).replace(/-/g, "");
    if (endYmd && endYmd < todayYmd && useYn.value === "Y") {
      const ok = await proxy.$confirm(
        "사업종료일이 오늘보다 과거입니다.\n" +
          "사용여부를 '미사용'으로 변경하여 사업장을 종료 처리하시겠습니까?\n\n" +
          "[확인] 종료(미사용)로 저장 · [취소] 저장 취소"
      );
      if (!ok) {
        // 취소: 사용여부 'Y' 유지 시 종료일이 저장되지 않으므로 저장을 중단한다(잘못된 성공 표시 방지).
        return;
      }
      useYn.value = "N";
    }

    // 지오코딩 in-flight 대기 — 주소를 바꾼 직후 저장을 누르면 이전 주소의 좌표가 실릴 수 있다.
    //   버튼 비활성(:disabled="geocoding")과 별개로, 대기 중 결과를 반드시 반영하고 진행한다.
    if (geocoding.value && geocodePromise) {
      await geocodePromise;
    }

    // 좌표 미확보 시 저장 여부를 사용자가 선택한다.
    //   좌표(LAT/LON)가 NULL 이면 서버 지오펜스 판정이 '항상 사업장 안'으로 폴백되어
    //   GPS 반경 체크가 무력화된다 → 경고만 띄우고 통과시키지 않고 명시적 동의를 받는다.
    if (proxy.$util.isEmpty(lat.value) || proxy.$util.isEmpty(lon.value)) {
      const ok = await proxy.$confirm(
        "주소로부터 좌표를 가져오지 못했습니다.\n" +
          "좌표 없이 저장하면 이 사업장의 출퇴근 유효범위(GPS 반경) 체크가 동작하지 않습니다.\n\n" +
          "[확인] 좌표 없이 저장 · [취소] 저장 취소 후 주소 다시 선택"
      );
      if (!ok) {
        return;
      }
    }

    try {
      const response = await axios.post("/webApi/baim01/save-site-infos", [
        {
          siteCd: proxy.$util.isEmpty(siteCd.value) ? null : siteCd.value,
          siteNo: siteNo.value,
          siteNm: siteNm.value,
          cmpnyCd: cmpnyCd.value,
          addr1: addr1.value,
          addr2: addr2.value,
          zipCode: zipCode.value,
          strDate: strDate.value,
          endDate: endDate.value,
          useYn: useYn.value,
          siteAdminCd: siteAdminCd.value,
          telNo: telNo.value,
          gpsRange: gpsRange.value,
          siteDesc: siteDesc.value,
          lat: proxy.$util.isEmpty(lat.value) ? null : lat.value,
          lon: proxy.$util.isEmpty(lon.value) ? null : lon.value,
        },
      ]);
      if (response.status === 200) {
        const alertMsg = "처리됐습니다.";
        fnAlertMsg(alertMsg, () => {
          emit("close");
          props.onSelect();
        });
      }
    } catch (err) {
      // 백엔드 메시지(예: 권한 없음 BAIM_403_002) 우선 노출, 없으면 기본 문구
      const alertMsg = resolveApiErrorMessage(
        err,
        "요청처리에 실패했습니다.\n관리자에게 문의해주세요."
      );
      fnAlertMsg(alertMsg);
    }
  });
};

/* user function */
function telNoFocusKill() {
  if (proxy.$util.validatePhoneNumber(telNo.value)) {
    telNo.value = proxy.$util.formatPhoneNumber(telNo.value);
    siteDescFcs.value.focus();
  } else {
    const alertMsg = "사업장전화번호를 확인해주세요.";
    fnAlertMsg(alertMsg, () => {
      telNo.value = "";
      telNoFcs.value.focus();
    });
  }
}

function onUserSelected(userCdVal, userNmVal) {
  siteAdminCd.value = userCdVal;
  siteAdminNm.value = userNmVal;
}

function fnUserSearchPopOpen() {
  openPop(UserSearchPop, {
    cmpnyCd_p: cmpnyCd.value,
    onSelect: onUserSelected,
  });
}

function onClickAddressSearch() {
  fnSearchAddress(zipCode, addr1, addr2);
  // 주소 선택 후 지도 업데이트는 watch에서 처리됨
}

function fnSiteInfoValidationChk() {
  let alertMsg = "";
  let retVal = true;

  // PRAFTA-COM-001-T2-1: 사업장번호 기본값 세팅 제거 → 필수 입력 검증(미입력 시 NOT NULL 위반 차단).
  if (proxy.$util.isEmpty(siteNo.value)) {
    alertMsg = "사업장번호를 입력해주세요.";

    fnAlertMsg(alertMsg, () => {
      const el = document.getElementById("siteNo");
      if (el) el.focus();
    });
    retVal = false;
  } else if (proxy.$util.isEmpty(siteNm.value)) {
    alertMsg = "사업장명을 입력해주세요.";

    fnAlertMsg(alertMsg, () => {
      siteNmFcs.value.focus();
    });
    retVal = false;
  } else if (
    proxy.$util.isEmpty(zipCode.value) ||
    proxy.$util.isEmpty(addr1.value)
  ) {
    alertMsg = "주소를 입력해주세요.";

    fnAlertMsg(alertMsg, () => {
      addrFcs.value.focus();
    });
    retVal = false;
  } else if (proxy.$util.isEmpty(siteAdminCd.value)) {
    alertMsg = "관리자 계정을 지정해주세요.";

    fnAlertMsg(alertMsg, () => {
      siteAdminSrchBtnFcs.value.focus();
    });
    retVal = false;
  } else {
    // PRAFTA-COM-001-T2-5: 종료일이 개시일보다 과거이면 차단(FE 1차 검증, BE T2-2 가 최종 검증).
    //   개시일 당일(strDate == endDate)은 허용(5.2.1).
    const startYmd = toYmdDash(strDate.value).replace(/-/g, "");
    const endYmd = toYmdDash(endDate.value).replace(/-/g, "");
    if (startYmd && endYmd && endYmd < startYmd) {
      alertMsg = "사업종료일은 사업개시일 이후로 지정해주세요.";
      fnAlertMsg(alertMsg);
      retVal = false;
    }
  }

  return retVal;
}

async function fnAlertMsg(message, afterConfirmCallback) {
  await proxy.$alert(message);
  if (afterConfirmCallback) {
    afterConfirmCallback();
  }
}

async function fnConfirmMsg(message, afterConfirmCallback) {
  const result = await proxy.$confirm(message);
  if (result && afterConfirmCallback) {
    afterConfirmCallback(); // ✅ 확인 눌렀을 때만 실행
  }
}
</script>

<style scoped>
/* PRAFTA-SUBCON-T2-09: 미러(연동) 사업장 안내 문구 */
.mirror-guide {
  padding: 0.35rem 0.6rem;
  border-radius: var(--btn-radius, 8px);
  background: var(--color-primary-bg, #dcfce7);
  color: var(--color-primary, #16a34a);
  font-size: var(--btn-font-sm, 12px);
}

/* GPS 반경 입력 우측 단위 표기 */
.gps-range-field {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}
.gps-range-unit {
  color: var(--color-text-muted, #6b7280);
}

/* 폼·지도 같은 행 높이 → 지도 하단이 저장 버튼(폼 하단)과 맞음 */
.content-wrapper {
  display: grid;
  grid-template-columns: minmax(0, 850px) 1fr;
  grid-template-rows: 1fr;
  gap: 1rem;
  padding: 1.2rem;
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  min-width: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding-right: 0.5rem;
}

/* 지도 영역: 저장 버튼 끝단과 높이 맞춤 (폼 높이 = 행 높이) */
.map-container {
  min-width: 100px;
  min-height: 0;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
  align-self: stretch;
}

#kakao-map {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
</style>
