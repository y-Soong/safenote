package com.prafta.app.notiset.notiset01.application;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 푸시 알림 토글키 ↔ NOTI_TYPE[SYS045] 매핑 단일 출처 (PRAFTA-APP-021-1).
 *
 * <p>plan §3 의 매핑표를 코드화한다. 1 토글 = 1개 이상 NOTI_TYPE 이며, 설정 저장 시 토글의 USE_YN 을
 * 매핑된 모든 NOTI_TYPE 행에 동일 적용한다. 매핑이 바뀌면 본 클래스만 수정한다.
 *
 * <p>토글 그룹 구분:
 * <ul>
 *   <li>근로자 토글(W*) — 모든 사용자에게 노출/저장 허용.</li>
 *   <li>관리자 토글(M*) — isAdmin(노드 main/sub 관리자 또는 master/hr/safe)에게만 노출/저장 허용.
 *       비관리자가 관리자 토글을 PUT 으로 보내면 서버 화이트리스트로 무시한다(권한 가드).</li>
 *   <li>읽기전용 항목(R*) — 매핑은 존재하나 사용자가 끌 수 없는 항목(plan §8-R 7). FE 는 ON 고정·disabled,
 *       PUT 페이로드에서 제외해야 하며 서버도 저장을 거부한다(아래 isSavableToggle=false).</li>
 * </ul>
 *
 * <p>마스터 스위치는 토글이 아닌 특수 NOTI_TYPE 키 {@link #MASTER_NOTI_TYPE}('__MASTER__') 1행으로 저장한다.
 */
public final class PushNotiTypeConst {

    private PushNotiTypeConst() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /** 마스터 스위치 1행을 표현하는 특수 NOTI_TYPE 키. */
    public static final String MASTER_NOTI_TYPE = "__MASTER__";

    // ── 근로자 토글키(W*) — 전 사용자 노출/저장 ──
    public static final String W1_LEAVE_RECALL = "W1_LEAVE_RECALL";
    public static final String W2_REQUEST_RESULT = "W2_REQUEST_RESULT";
    public static final String W3_TBM = "W3_TBM";
    public static final String W4_CHECKIN_REMIND = "W4_CHECKIN_REMIND";
    public static final String W5_CHECKOUT_REMIND = "W5_CHECKOUT_REMIND";
    public static final String W6_SHIFT_SCH_CHANGE = "W6_SHIFT_SCH_CHANGE";

    // ── 관리자 토글키(M*) — isAdmin 만 노출/저장 ──
    public static final String M1_LATE_EARLY = "M1_LATE_EARLY";
    public static final String M2_APPROVAL = "M2_APPROVAL";
    public static final String M3_REFUSAL_CHECKIN = "M3_REFUSAL_CHECKIN";
    public static final String M4_NEAR_MISS = "M4_NEAR_MISS";
    public static final String M5_RISK_REQUEST = "M5_RISK_REQUEST";

    // ── 읽기전용 항목키(R*) — 노출하되 저장 불가(ON 고정), plan §8-R 7 ──
    public static final String R1_REFUSAL_NOTICE = "R1_REFUSAL_NOTICE";
    public static final String R2_LEAVE_CHANGE_CONFIRMED = "R2_LEAVE_CHANGE_CONFIRMED";
    public static final String R3_LEAVE_CHANGE_REJECTED = "R3_LEAVE_CHANGE_REJECTED";

    /** 토글 그룹 분류. */
    public enum ToggleGroup {
        WORKER, ADMIN, READONLY
    }

    /** 토글 1개의 정의(키 + 그룹 + 매핑 NOTI_TYPE 목록). */
    public static final class ToggleDef {
        private final String toggleKey;
        private final ToggleGroup group;
        private final List<String> notiTypes;

        private ToggleDef(String toggleKey, ToggleGroup group, List<String> notiTypes) {
            this.toggleKey = toggleKey;
            this.group = group;
            this.notiTypes = Collections.unmodifiableList(notiTypes);
        }

        public String getToggleKey() {
            return toggleKey;
        }

        public ToggleGroup getGroup() {
            return group;
        }

        public List<String> getNotiTypes() {
            return notiTypes;
        }
    }

    // 순서 보존(LinkedHashMap) — 응답 항목 순서가 UI 노출 순서와 일치.
    private static final Map<String, ToggleDef> TOGGLES = new LinkedHashMap<>();

    static {
        // 근로자 토글
        //   prafta-com-016-C-2: 관리자 연차/월차 직접 등록 통보(LEAVE_DIRECT_SET)도 연차 관련 근로자 알림이므로
        //   W1(연차) 토글 하위에 매핑한다(기본 ON, opt-out). 토글 OFF 시 회수·직접등록 통보가 함께 꺼진다.
        put(W1_LEAVE_RECALL, ToggleGroup.WORKER, "LEAVE_GRANT_RECALLED", "LEAVE_DIRECT_SET");
        put(W2_REQUEST_RESULT, ToggleGroup.WORKER,
                "LEAVE_RESULT_APPROVED", "LEAVE_RESULT_REJECTED",
                "ATTD_RESULT_APPROVED", "ATTD_RESULT_REJECTED");
        put(W3_TBM, ToggleGroup.WORKER, "TBM_STARTED", "TBM_COMPLETED");
        put(W4_CHECKIN_REMIND, ToggleGroup.WORKER, "ATTD_CHECKIN_REMINDER");
        put(W5_CHECKOUT_REMIND, ToggleGroup.WORKER, "ATTD_CHECKOUT_REMINDER");
        // prafta-com-016-D-2: 교대근무 팀 스케줄 변경 통보(근로자 대상). 기본 ON(opt-out).
        put(W6_SHIFT_SCH_CHANGE, ToggleGroup.WORKER, "SHIFT_SCH_CHANGED");

        // 관리자 토글
        put(M1_LATE_EARLY, ToggleGroup.ADMIN, "ATTD_LATE_EARLY_DETECTED");
        put(M2_APPROVAL, ToggleGroup.ADMIN,
                "ATTD_APPROVAL_TURN", "ATTD_APPROVAL_REQUEST",
                "LEAVE_APPROVAL_TURN", "LEAVE_USED_NO_APRV",
                "LEAVE_CHANGE_REQUEST", "LEAVE_CHANGE_RESPONSE");
        put(M3_REFUSAL_CHECKIN, ToggleGroup.ADMIN, "LEAVE_REFUSAL_CHECKIN_ALERT");
        put(M4_NEAR_MISS, ToggleGroup.ADMIN, "NEAR_MISS_REPORTED");
        put(M5_RISK_REQUEST, ToggleGroup.ADMIN, "RISK_ASSESS_REQUESTED");

        // 읽기전용 항목(저장 불가 — ON 고정)
        put(R1_REFUSAL_NOTICE, ToggleGroup.READONLY, "LEAVE_REFUSAL_NOTICE");
        put(R2_LEAVE_CHANGE_CONFIRMED, ToggleGroup.READONLY, "LEAVE_CHANGE_CONFIRMED");
        put(R3_LEAVE_CHANGE_REJECTED, ToggleGroup.READONLY, "LEAVE_CHANGE_REJECTED");
    }

    private static void put(String toggleKey, ToggleGroup group, String... notiTypes) {
        TOGGLES.put(toggleKey, new ToggleDef(toggleKey, group, List.of(notiTypes)));
    }

    /** 전체 토글 정의(순서 보존, 불변). */
    public static List<ToggleDef> allToggles() {
        return List.copyOf(TOGGLES.values());
    }

    /** 토글키로 정의 조회(없으면 null). */
    public static ToggleDef findByKey(String toggleKey) {
        if (toggleKey == null) {
            return null;
        }
        return TOGGLES.get(toggleKey);
    }

    /**
     * 해당 사용자가 저장(PUT)할 수 있는 토글인지 판정.
     * <ul>
     *   <li>READONLY 그룹 → 항상 false(끌 수 없는 항목).</li>
     *   <li>ADMIN 그룹 → isAdmin 일 때만 true(비관리자가 보내면 무시).</li>
     *   <li>WORKER 그룹 → 항상 true.</li>
     * </ul>
     */
    public static boolean isSavableToggle(String toggleKey, boolean isAdmin) {
        ToggleDef def = findByKey(toggleKey);
        if (def == null) {
            return false;
        }
        return switch (def.getGroup()) {
            case READONLY -> false;
            case ADMIN -> isAdmin;
            case WORKER -> true;
        };
    }

    /**
     * 한 토글에 매핑된 NOTI_TYPE 들이 모두 OFF(USE_YN='N') 인지에 대응하는 NOTI_TYPE 목록 반환.
     * (저장 시 토글 USE_YN 을 매핑된 모든 NOTI_TYPE 행에 전개.)
     */
    public static List<String> notiTypesOf(String toggleKey) {
        ToggleDef def = findByKey(toggleKey);
        return def == null ? List.of() : def.getNotiTypes();
    }

    /**
     * NOTI_TYPE → 소속 토글키 역매핑. 조회 응답 시 USE_YN='N' 행을 토글 on=false 로 환원하는 데 사용.
     * 한 NOTI_TYPE 은 정확히 한 토글에 속한다(중복 매핑 없음 — 정의상 보장).
     */
    public static Map<String, String> notiTypeToToggleKey() {
        Map<String, String> map = new LinkedHashMap<>();
        for (ToggleDef def : TOGGLES.values()) {
            for (String notiType : def.getNotiTypes()) {
                map.put(notiType, def.getToggleKey());
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /** 전체 토글키 집합(불변). */
    public static Set<String> allToggleKeys() {
        return TOGGLES.values().stream().map(ToggleDef::getToggleKey)
                .collect(Collectors.toUnmodifiableSet());
    }
}
