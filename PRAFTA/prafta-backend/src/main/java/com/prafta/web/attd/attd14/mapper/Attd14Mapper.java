package com.prafta.web.attd.attd14.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.attd14.result.AdminRequestHistoryRowResult;

/**
 * 관리자 발신 연차 변경 요청 이력(attd14) 전용 읽기 Mapper (prafta-com-016-H).
 *
 * <p>출처 = {@code TB_LEAVE_CHANGE_REQUEST}(INITIATOR_TYPE='ADMIN' 서버 고정). attd13 의
 * {@code selectChangeRequests} 를 복제하되 발의주체 고정·발의자/확인자 이름 조인·기간/유형 필터·페이징을 추가한다.
 * attd13 Mapper 는 무수정(회귀 0). 모든 쿼리는 CMPNY_CD 스코프 격리, DEL_YN='N'.
 */
@Mapper
public interface Attd14Mapper {

    /**
     * 관리자 발신 요청 이력 목록(페이징). 역할 기반 스코프(attd13 계승):
     * <ul>
     *   <li>{@code siteWide='Y'}(master/hr): 회사 전사. siteCd 지정 시 해당 사업장만, 미지정 시 전체.</li>
     *   <li>{@code siteWide='N'}(노드 관리자): siteCd + nodeCd(+하위) 강제(호출부가 권한검증 통과 nodeCd 만 전달).</li>
     * </ul>
     * INITIATOR_TYPE='ADMIN' 고정. 기간(INSERT_DATE between fromDate~toDate)·요청유형·상태·대상사용자명 필터.
     */
    List<AdminRequestHistoryRowResult> selectAdminRequestHistory(@Param("cmpnyCd") String cmpnyCd,
                                                                 @Param("siteWide") String siteWide,
                                                                 @Param("siteCd") String siteCd,
                                                                 @Param("nodeCd") String nodeCd,
                                                                 @Param("incSubNodeYn") String incSubNodeYn,
                                                                 @Param("userNm") String userNm,
                                                                 @Param("reqType") String reqType,
                                                                 @Param("reqStatus") String reqStatus,
                                                                 @Param("fromDate") String fromDate,
                                                                 @Param("toDate") String toDate,
                                                                 @Param("size") int size,
                                                                 @Param("offset") int offset);

    /** 위 목록과 동일 필터의 총 건수(페이징 totalCnt). LIMIT/OFFSET 제외. */
    int selectAdminRequestHistoryCount(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("siteWide") String siteWide,
                                       @Param("siteCd") String siteCd,
                                       @Param("nodeCd") String nodeCd,
                                       @Param("incSubNodeYn") String incSubNodeYn,
                                       @Param("userNm") String userNm,
                                       @Param("reqType") String reqType,
                                       @Param("reqStatus") String reqStatus,
                                       @Param("fromDate") String fromDate,
                                       @Param("toDate") String toDate);

    /**
     * 관리자 발신 요청 이력 단건 상세(회사 스코프 + INITIATOR_TYPE='ADMIN'). 없거나 스코프 밖/삭제면 null.
     * IDOR: 역할 스코프 재검증은 호출부(서비스)가 siteCd/targetUserCd 로 수행.
     */
    AdminRequestHistoryRowResult selectAdminRequestHistoryDetail(@Param("cmpnyCd") String cmpnyCd,
                                                                 @Param("changeReqId") String changeReqId);
}
