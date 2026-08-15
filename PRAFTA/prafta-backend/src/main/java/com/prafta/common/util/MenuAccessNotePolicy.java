package com.prafta.common.util;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

/**
 * 화면권한(User_02)에서 "권한 부여만으로는 이용할 수 없는 화면"의 추가 조건 안내 문구 SSOT.
 *
 * <p><b>왜 필요한가</b><br>
 * 화면권한을 부여해도 서버가 별도 조건으로 접근을 막는 화면이 있다. 관리자는 체크만 하고
 * "왜 안 되지?" 를 겪게 된다(2026-08-15 통합테스트에서 Attd_05 로 실제 발생).
 * 부여하는 그 자리에서 조건을 보여주어 헛수고를 막는다.
 *
 * <p><b>왜 체크박스를 막지 않는가</b><br>
 * 조사 결과 "권한을 줘봐야 아무 의미 없는 화면"은 <b>실재하지 않았다</b>. 전부 어떤 조건에서는
 * 의미가 있다. 예를 들어 Attd_10 연차 상신 탭은 결재선에 지정된 사용자면 일반사용자도 반드시
 * 이용해야 하므로, 메뉴를 잠그면 결재 흐름이 끊긴다. 그래서 <b>차단이 아니라 안내</b>다.
 *
 * <p>이 클래스는 표시 전용이다. <b>실제 인가는 각 화면의 서버 게이트가 담당하며 여기서 아무것도
 * 강제하지 않는다.</b> 문구가 낡아도 보안에 영향은 없지만, 게이트를 바꾸면 여기도 함께 갱신할 것.
 *
 * <p>키는 {@code MENU_D_ID}(=화면 식별자)이며, DB 콜레이션이 대소문자를 무시하므로
 * <b>소문자로 보관</b>하고 조회 시 정규화한다({@link MenuLockPolicy} 와 동일 규약).
 */
public final class MenuAccessNotePolicy {

    /**
     * 화면 추가 조건 안내 1건.
     *
     * @param badge  표에 붙는 짧은 배지 문구(공간이 좁으므로 10자 내외)
     * @param detail 마우스오버로 보여줄 전문 — 왜 막히는지, 무엇을 해야 하는지까지
     */
    public record AccessNote(String badge, String detail) {
    }

    /**
     * 조사로 확인된 화면만 넣는다(2026-08-15 기준 3건).
     * 추측으로 늘리지 말 것 — 틀린 안내는 없느니만 못하다.
     */
    private static final Map<String, AccessNote> NOTES = Map.of(
            // Attd05ServiceImpl.getUserWorkPlan → canManageNode
            //   (master/hr/safe 전사 통과 OR 해당·상위 부서의 담당 정/부)
            "attd_05", new AccessNote(
                    "부서 담당자 필요",
                    "마스터·HR·산업안전관리자이거나, 해당 부서의 담당 정/부로 지정된 사용자만 이용할 수 있습니다."
                            + " 화면권한만 부여해서는 열리지 않습니다. 담당자 지정은 조직관리(Baim_06)에서 합니다."),

            // 탭마다 게이트가 다르다.
            //   연차 상신 : leaveflow/my-approvals — 역할 게이트 없음(결재선에 지정된 본인 기준)
            //   그 외 3탭 : reqinbox/pending, attd13 — AuthRoleUtils.isManager(master/hr)
            "attd_10", new AccessNote(
                    "탭별 제한",
                    "연차 상신 탭은 결재선에 결재자로 지정된 사용자면 이용할 수 있습니다."
                            + " 근태 보정·초과근무 상신·스케줄 수정 탭은 마스터·HR만 이용할 수 있습니다."),

            // Baim07ServiceImpl: getActivePolicy 는 게이트 없음, createPolicy 만 역할 검사.
            "baim_07", new AccessNote(
                    "저장 제한",
                    "조회는 화면권한이 있으면 가능하지만, 연차부여정책 저장은 마스터·HR만 할 수 있습니다."));

    private MenuAccessNotePolicy() {
        // 유틸리티 클래스 - 인스턴스 생성 금지
    }

    /** 전체 안내 맵(불변). 키는 소문자 MENU_D_ID — 화면은 자기 menuDId 를 소문자로 바꿔 조회한다. */
    public static Map<String, AccessNote> all() {
        return Collections.unmodifiableMap(NOTES);
    }

    /** 단건 조회. 안내 대상이 아니면 null. */
    public static AccessNote noteOf(String menuDId) {
        if (menuDId == null || menuDId.isEmpty()) {
            return null;
        }
        return NOTES.get(menuDId.toLowerCase(Locale.ROOT));
    }
}
