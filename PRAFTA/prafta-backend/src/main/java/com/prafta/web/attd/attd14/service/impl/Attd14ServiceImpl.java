package com.prafta.web.attd.attd14.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.attd.attd14.application.param.AdminRequestHistoryListParam;
import com.prafta.web.attd.attd14.dto.response.AdminRequestHistoryListResponse;
import com.prafta.web.attd.attd14.mapper.Attd14Mapper;
import com.prafta.web.attd.attd14.result.AdminRequestHistoryRowResult;
import com.prafta.web.attd.attd14.service.Attd14Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link Attd14Service} 구현 (prafta-com-016-H, 읽기 전용 이력 조회).
 *
 * <p>역할 기반 스코프/IDOR 는 attd13 {@code Attd13ServiceImpl.getChangeRequests}/{@code getChangeRequestDetail}
 * 정책을 계승한다(safe 제외 = canManageNodeExcludeSafe / canManageUserExcludeSafe).
 * 본 모듈은 쓰기 동작이 전혀 없다(목록/상세 조회만).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Attd14ServiceImpl implements Attd14Service {

    private static final String WHOLE_SITE = "*"; // 전체 부서 스코프(노드 관리자는 사용 불가)

    private final Attd14Mapper attd14Mapper;
    private final AttdCloseService attdCloseService;
    /** 사업장 접근 인가(공용 cmm 빈) — 토큰 사업장 등식 대신 User_03 원장(TB_USER_SITE_AUTH) 기반 인가. */
    private final com.prafta.common.cmm.siteauth.service.SiteAccessService siteAccessService;

    @Override
    public AdminRequestHistoryListResponse getAdminRequestHistory(AdminRequestHistoryListParam param) {
        // 역할 기반 스코프(attd13 D1+D3 계승):
        //   - master/hr: 회사 전사(사업장 미지정=전체, 지정 시 해당 사업장). 부서 미지정 허용.
        //   - 노드 정·부 관리자: 본인 담당 노드(+하위) 강제. 부서 미지정 진입은 안내성 BadRequest.
        boolean siteWide = AuthRoleUtils.isManager(param.gvAuthCd());
        if (!siteWide) {
            // 사업장 접근 인가(구 토큰 사업장 등식 가드 대체 — User_03 원장 기반).
            siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());
            if (param.nodeCd() == null || param.nodeCd().isBlank() || WHOLE_SITE.equals(param.nodeCd())) {
                throw new ApiException(AttdErrorCode.ATTD_400_130);
            }
            // 지정 노드에 대한 관리 권한(해당/상위 부서 정·부 관리자) 강제(safe 제외)
            if (!attdCloseService.canManageNodeExcludeSafe(
                    param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd(), param.nodeCd())) {
                log.warn("연차 변경 이력 스코프 권한 없음 - userCd={}, authCd={}, nodeCd={}",
                        param.gvUserCd(), param.gvAuthCd(), param.nodeCd());
                throw new ApiException(AttdErrorCode.ATTD_403_120);
            }
        }

        String siteWideFlag = siteWide ? "Y" : "N";
        int totalCnt = attd14Mapper.selectAdminRequestHistoryCount(
                param.gvCmpnyCd(), siteWideFlag, param.siteCd(), param.nodeCd(), param.incSubNodeYn(),
                param.userNm(), param.reqType(), param.reqStatus(), param.fromDate(), param.toDate());

        List<AdminRequestHistoryRowResult> list;
        if (totalCnt == 0) {
            list = List.of();
        } else {
            list = attd14Mapper.selectAdminRequestHistory(
                    param.gvCmpnyCd(), siteWideFlag, param.siteCd(), param.nodeCd(), param.incSubNodeYn(),
                    param.userNm(), param.reqType(), param.reqStatus(), param.fromDate(), param.toDate(),
                    param.size(), param.offset());
        }

        log.info("관리자 발신 연차 변경 요청 이력 조회. cmpnyCd={}, siteWide={}, page={}, size={}, total={}",
                param.gvCmpnyCd(), siteWideFlag, param.page(), param.size(), totalCnt);

        return AdminRequestHistoryListResponse.builder().list(list).totalCnt(totalCnt).build();
    }

    @Override
    public AdminRequestHistoryRowResult getAdminRequestHistoryDetail(String cmpnyCd, String authCd, String userCd, String changeReqId) {
        AdminRequestHistoryRowResult detail = attd14Mapper.selectAdminRequestHistoryDetail(cmpnyCd, changeReqId);
        // 존재하지 않거나 ADMIN 발신이 아니면 매퍼가 null → 404
        if (detail == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_121);
        }
        // IDOR: 대상 근로자 관리 권한(master/hr 전사 또는 대상자 소속/상위 부서 정·부 관리자, safe 제외).
        //   스코프 밖이면 정보 누출 방지를 위해 403 이 아닌 404 로 응답한다(prafta-com-016-H §3.3).
        if (!attdCloseService.canManageUserExcludeSafe(authCd, userCd, cmpnyCd, detail.siteCd(), detail.targetUserCd())) {
            log.warn("연차 변경 이력 상세 스코프 밖 접근(404 처리) - requester={}, authCd={}, target={}, changeReqId={}",
                    userCd, authCd, detail.targetUserCd(), changeReqId);
            throw new ApiException(AttdErrorCode.ATTD_404_121);
        }
        return detail;
    }
}
