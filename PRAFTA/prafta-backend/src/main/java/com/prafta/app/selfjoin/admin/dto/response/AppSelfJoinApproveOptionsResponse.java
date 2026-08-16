package com.prafta.app.selfjoin.admin.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 앱 관리자 모드 — 셀프가입 승인 시트 입력 옵션(소정근로 + 직급) 합본 응답.
 *
 * <p>앱은 왕복 1회가 유리하고 두 옵션이 같은 시트에서만 쓰이므로, 웹이 2개 EP 로 받는 값을
 * 하나로 합쳐 내려준다. 값의 출처는 웹과 동일하다(신규 쿼리 없음).
 * <ul>
 *   <li>소정근로 — {@code User01Service.getStdWorkOptions(cmpnyCd, siteCd)}</li>
 *   <li>직급 — {@code BaseinfoService.selectBaseinfoList} (COM007, USE_YN='Y')</li>
 * </ul>
 *
 * <p>PII 는 담지 않는다(회사 단위 정책 상수 + 코드표뿐).
 */
@Getter
@Builder
public class AppSelfJoinApproveOptionsResponse {

    /** 회사(사업장 오버라이드 반영) 통상 주 소정근로 분. 화면 "풀타임(주 N시간)" 라벨 소스. */
    private final int cmpnyWeekStdMinutes;

    /** 단시간(DIRECT) 선택 시 고를 수 있는 사유코드 목록 [SYS083]. */
    private final List<ReasonOption> reasonOptions;

    /** 직급 목록 [COM007] — 사용중(USE_YN='Y') 코드만. */
    private final List<RankOption> rankOptions;

    /** 소정근로 사유 1건. */
    @Getter
    @Builder
    public static class ReasonOption {

        /** 사유코드 [SYS083] */
        private final String reasonCd;

        /** 사유 명칭 */
        private final String reasonNm;
    }

    /** 직급 1건. */
    @Getter
    @Builder
    public static class RankOption {

        /** 직급코드 [COM007] */
        private final String rankCd;

        /** 직급 명칭 */
        private final String rankNm;
    }
}
