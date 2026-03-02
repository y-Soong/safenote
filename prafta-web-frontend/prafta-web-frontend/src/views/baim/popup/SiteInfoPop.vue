<template>
  <Transition name="fade">
    <div class="modal-overlay prafta-modal-popup">
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
            <div class="form-row-max">
              <label>사업장</label>
              <input
                id="siteNo"
                v-model="siteNo"
                placeholder="사업장코드"
                @blur="focusKill"
              />
              <div class="editable-form">
                <input
                  style="width: 28rem"
                  v-model="siteNm"
                  ref="siteNmFcs"
                  placeholder="사업장명"
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
              <input v-model="addr2" placeholder="상세주소" />
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
                :readonly="readonly"
              />
            </div>

            <div class="form-row-max">
              <label>사용여부</label>
              <div style="width: 10rem">
                <BaseSelect id="useYn" v-model="useYn">
                  <option
                    v-for="opt in systCodeArr['SYS003'] || []"
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
                  @blur="focusKill"
                />
              </div>
              <label>GPS 반경</label>
              <div>
                <input
                  id="gpsRange"
                  style="width: 5rem"
                  v-model="gpsRange"
                  maxlength="4"
                  @input="handleGpsRangeInput"
                  @blur="focusKill"
                />
              </div>
            </div>

            <div class="form-row-max">
              <label>사업장비고</label>
              <textarea
                id="siteDesc"
                ref="siteDescFcs"
                style="width: 100%"
                v-model="siteDesc"
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
            <button class="btn btn-primary" @click="fnSiteSave">저장</button>
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
  defineProps,
  defineEmits,
  onMounted,
  onBeforeUnmount,
  watch,
  getCurrentInstance,
  nextTick,
} from "vue";
import axios from "@/api/axios";
import { fnSearchAddress } from "@/utils/addrUtil";
import { useModal } from "@/utils/useModal";
import { useCenteredDraggable } from "@/composables/useCenteredDraggable";
import CalendarSrch from "@/components/common/CalendarSrch";
import UserSearchPop from "@/components/popup/UserSearchPop";
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
  onSearch: Function,
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
const siteAdminId = ref("");
const siteAdminNm = ref("");
const siteAdminSrchBtnFcs = ref("");
const telNo = ref("");
const telNoFcs = ref("");
const gpsRange = ref("");
const siteDesc = ref("");
const siteDescFcs = ref("");

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

    // Vue CLI: VUE_APP_ 접두사 필요
    // 환경변수는 빌드 시점에 주입되므로 process.env 사용
    // .env.development 파일에 VUE_APP_PUBLIC_KAKAO_APP_JS_KEY=your_key 형태로 설정 필요
    const env = typeof process !== "undefined" ? process.env : {};
    const kakaoKey = env.VUE_APP_PUBLIC_KAKAO_APP_JS_KEY;

    if (!kakaoKey) {
      reject(new Error("카카오 지도 API 키가 없습니다."));
      return;
    }

    const scriptUrl = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${kakaoKey}&libraries=services&autoload=false`;

    // 먼저 fetch로 스크립트가 접근 가능한지 확인
    fetch(scriptUrl, { method: "HEAD" })
      .then((response) => {
        if (!response.ok) {
          reject(
            new Error(
              `카카오 지도 API 접근 실패: HTTP ${response.status}. 브라우저 개발자 도구의 네트워크 탭을 확인하세요.`
            )
          );
          return;
        }

        // 스크립트 로드
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

        script.onerror = (error) => {
          reject(
            new Error(
              `카카오 지도 API 로드 실패. 네트워크 탭에서 HTTP 상태 코드를 확인하세요.`
            )
          );
        };

        document.head.appendChild(script);
      })
      .catch((fetchError) => {
        // fetch 실패해도 스크립트 로드는 시도 (CORS 정책 때문일 수 있음)
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

        script.onerror = (error) => {
          reject(
            new Error(
              `카카오 지도 API 로드 실패. 카카오 개발자 콘솔 설정을 확인하세요.`
            )
          );
        };

        document.head.appendChild(script);
      });
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

// 주소로 지도 위치 업데이트
const updateMapLocation = (address) => {
  if (!geocoder || !map || !address) return;

  geocoder.addressSearch(address, (result, status) => {
    if (status === window.kakao.maps.services.Status.OK) {
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
      
      // if (radius <= 50) {
      //   level = 4; // 50m 이하: 더 가까이
      // } else if (radius <= 100) {
      //   level = 3; // 100m: 기본
      // } else if (radius <= 200) {
      //   level = 2; // 200m: 조금 넓게
      // } else if (radius <= 500) {
      //   level = 1; // 500m: 넓게
      // } else {
      //   level = 0; // 500m 이상: 매우 넓게
      // }
      
      map.setLevel(level);
    } else {
      console.warn("주소 검색 실패:", status);
    }
  });
};

// 주소 변경 감시
watch(
  () => addr1.value,
  (newAddr) => {
    if (newAddr && map) {
      nextTick(() => {
        updateMapLocation(newAddr);
      });
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
    const response = await axios.get("/comApi/baseinfo/syst-info-list", {
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
    alert(err.response.data.message);
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
        strDate.value = response.data?.siteInfoList[0].strDate;
        endDate.value = response.data?.siteInfoList[0].endDate;
        useYn.value = response.data?.siteInfoList[0].useYn;
        telNo.value = response.data?.siteInfoList[0].telNo;
        gpsRange.value = response.data?.siteInfoList[0].gpsRange;
        siteDesc.value = response.data?.siteInfoList[0].siteDesc;
        siteAdminId.value = response.data?.siteInfoList[0].siteAdminId;
        siteAdminNm.value = response.data?.siteInfoList[0].siteAdminNm;
      }
    }
  } catch (err) {
    alert(err.response.data.message);
  }
};

const fnSiteSave = async () => {
  fnConfirmMsg("저장하시겠습니까 ?", async () => {
    if (!fnSiteInfoValidationChk()) {
      return;
    }

    try {
      // const response = await axios.post("/webApi/baim01/updateSiteInfo", [
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
          siteAdminId: siteAdminId.value,
          telNo: telNo.value,
          gpsRange: gpsRange.value,
          siteDesc: siteDesc.value,
        },
      ]);
      if (response.status === 200) {
        const alertMsg = "처리됐습니다.";
        fnAlertMsg(alertMsg, () => {
          emit("close");
          props.onSearch();
        });
      }
    } catch (err) {
      const alertMsg = "요청처리에 실패했습니다.\n관리자에게 문의해주세요.";
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

function onUserSelected(userIdVal, userNmVal) {
  siteAdminId.value = userIdVal;
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

  if (proxy.$util.isEmpty(siteNm.value)) {
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
  } else if (proxy.$util.isEmpty(siteAdminId.value)) {
    alertMsg = "관리자 계정을 지정해주세요.";

    fnAlertMsg(alertMsg, () => {
      siteAdminSrchBtnFcs.value.focus();
    });
    retVal = false;
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
