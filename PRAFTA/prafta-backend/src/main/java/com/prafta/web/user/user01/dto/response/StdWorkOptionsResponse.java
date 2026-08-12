package com.prafta.web.user.user01.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 소정-03/08 — 계정 생성 폼(UserInfoPop 'C' 모드)의 소정근로시간 입력 옵션 응답.
 *
 * <p>화면이 "풀타임(주 40시간)" 라벨과 사유 셀렉트를 <b>하드코딩 없이</b> 그릴 수 있도록,
 * 회사 통상 기준값과 선택 가능한 사유코드를 함께 내려준다(지시서 B-1: 기준값은 서버 소유).
 */
@Getter
@Builder
public class StdWorkOptionsResponse {

    /** 회사 통상근로자 주 소정근로 분 (행 부재 시 시스템 폴백 2400 = 주 40시간). */
    private final int cmpnyWeekStdMinutes;

    /**
     * 단시간 선택 시 고를 수 있는 사유코드 목록 [SYS083].
     *
     * <p>제외 대상
     * <ul>
     *   <li>{@code NORMAL} — 풀타임 라디오가 담당하므로 목록에서 뺀다.</li>
     *   <li>단축 사유(육아기·임신기·가족돌봄) — 적용 종료일이 필수인데 생성 폼에는 기간
     *       입력이 없다(등록해도 서버 검증에서 차단됨). 소정근로시간 관리 화면에서 기간과
     *       함께 등록하는 것이 정상 경로다.</li>
     * </ul>
     */
    private final List<ReasonOption> reasonOptions;

    /** 사유 셀렉트 1건 (코드/명칭만 — 차감·부여 규칙은 화면에 노출하지 않는다). */
    @Getter
    @Builder
    public static class ReasonOption {

        /** 사유코드 [SYS083] */
        private final String reasonCd;

        /** 사유 명칭 */
        private final String reasonNm;
    }
}
