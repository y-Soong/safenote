package com.prafta.app.selfjoin.admin.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.admin.common.scope.application.query.ScopedNodeQuery;
import com.prafta.app.admin.common.scope.mapper.AdminScopeMapper;
import com.prafta.app.admin.common.scope.result.AdminSeedNodeResult;
import com.prafta.app.selfjoin.admin.dto.request.AppSelfJoinApproveRequest;
import com.prafta.app.selfjoin.admin.dto.request.AppSelfJoinHistoryRequest;
import com.prafta.app.selfjoin.admin.dto.request.AppSelfJoinListRequest;
import com.prafta.app.selfjoin.admin.dto.request.AppSelfJoinRejectRequest;
import com.prafta.app.selfjoin.admin.dto.response.AppSelfJoinApproveOptionsResponse;
import com.prafta.app.selfjoin.admin.dto.response.AppSelfJoinScopeNodesResponse;
import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.common.cmm.baseinfo.application.param.BaseInfoListParam;
import com.prafta.common.cmm.baseinfo.dto.response.BaseInfoListResponse;
import com.prafta.common.cmm.baseinfo.result.BaseInfoResult;
import com.prafta.common.cmm.baseinfo.service.BaseinfoService;
import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.common.util.ClientIpExtractor;
import com.prafta.web.user.user01.dto.response.StdWorkOptionsResponse;
import com.prafta.web.user.user01.service.User01Service;
import com.prafta.web.user.user09.application.param.SelfJoinApproveParam;
import com.prafta.web.user.user09.application.param.SelfJoinHistoryListParam;
import com.prafta.web.user.user09.application.param.SelfJoinListParam;
import com.prafta.web.user.user09.application.param.SelfJoinRejectParam;
import com.prafta.web.user.user09.dto.request.SelfJoinApproveRequest;
import com.prafta.web.user.user09.dto.request.SelfJoinHistoryListRequest;
import com.prafta.web.user.user09.dto.request.SelfJoinListRequest;
import com.prafta.web.user.user09.dto.request.SelfJoinRejectRequest;
import com.prafta.web.user.user09.dto.response.SelfJoinHistoryListResponse;
import com.prafta.web.user.user09.dto.response.SelfJoinListResponse;
import com.prafta.web.user.user09.service.User09Service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 앱 관리자 모드 — 셀프가입(회원가입) 승인/거부 컨트롤러.
 *
 * <p><b>본 컨트롤러는 위임만 한다. 비즈니스 판정은 {@link User09Service} 단일 출처다.</b>
 * 승인/거부/조회 로직·권한 게이트·PII 마스킹을 앱 쪽에 복제하지 않는다.
 *
 * <p>최종 URL(ApiPrefixConfig 가 {@code com.prafta.app.*} 에 {@code /prafta/appApi} 자동 부여):
 * <ul>
 *   <li>GET  /prafta/appApi/admin/self-join/pending          — 승인 대기 목록('06' 서버 고정)</li>
 *   <li>GET  /prafta/appApi/admin/self-join/history          — 처리 이력(서버 페이징)</li>
 *   <li>POST /prafta/appApi/admin/self-join/approve          — 승인</li>
 *   <li>POST /prafta/appApi/admin/self-join/reject           — 거부</li>
 *   <li>GET  /prafta/appApi/admin/self-join/approve-options  — 승인 시트 옵션(소정근로 + 직급)</li>
 *   <li>GET  /prafta/appApi/admin/self-join/scope-nodes      — 부서 필터 칩 소스</li>
 * </ul>
 *
 * <p><b>권한</b> — 조회/승인/거부의 2단 게이트(사업장 인가 {@code assertSiteAccess} + 부서 관리
 * 권한 {@code canManageNodeExcludeSafe})는 {@code User09ServiceImpl} 안에 있다. 컨트롤러는
 * 게이트를 중복 구현하지 않으며, <b>게이트를 우회하는 경로도 만들지 않는다</b>(매퍼 직접 호출 금지).
 * 예외는 {@code scope-nodes} 1건인데, 이는 User09Service 를 타지 않는 신규 조회라 컨트롤러가
 * {@code assertSiteAccess} 를 직접 건다(타 사업장 조직도 정찰 방지).
 *
 * <p><b>Param 생성</b> — 웹 {@code SelfJoin*Param.from(웹Request, TokenInfo)} 팩토리를 그대로
 * 호출한다. record 정식 생성자로 우회하면 {@code pageSize} 상한 100 · {@code actionType}
 * 화이트리스트 · 날짜 형식 검증이 통째로 사라진다.
 *
 * <p><b>로깅</b> — 휴대폰/이메일 평문, 소정근로 사유코드, 거부 사유 본문을 남기지 않는다.
 */
@Slf4j
@RestController
@RequestMapping("/admin/self-join")
@RequiredArgsConstructor
public class AppAdminSelfJoinController {

    private final User09Service user09Service;
    /** 승인 시트 소정근로 옵션(회사 통상 기준값 + 사유코드) — 계정 생성 폼과 같은 소스. */
    private final User01Service user01Service;
    /** 직급[COM007] 코드표 조회 — 기존 기초코드 서비스 재사용(신규 쿼리 금지). */
    private final BaseinfoService baseinfoService;
    /** scope-nodes 전용 사업장 인가(다른 EP 는 User09Service 내부에서 수행). */
    private final SiteAccessService siteAccessService;
    /** 부서 필터 칩 소스(내가 정/부 관리자인 seed 노드). */
    private final AdminScopeMapper adminScopeMapper;
    private final JwtUtil jwtUtil;

    /** 직급 코드 그룹 [COM007]. */
    private static final String BASE_CODE_RANK = "COM007";

    /**
     * 고용형태 [SYS041] — 앱 승인 시트는 입력란이 없어 서버가 정규직 고정으로 채운다
     * (2026-08-13 사용자 확정). 값 유효성은 {@code User09ServiceImpl} 이 재검증한다.
     */
    private static final String EMPLOYMENT_TYPE_REGULAR = "REGULAR";

    // ====================================================================
    // 조회
    // ====================================================================

    /** 승인 대기 목록. 상태는 서버가 '06' 으로 고정한다(클라이언트가 '07' 을 실을 수 없다). */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingList(
            @ModelAttribute AppSelfJoinListRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        SelfJoinListRequest webRequest = new SelfJoinListRequest();
        webRequest.setSiteCd(resolveSiteCd(request.getSiteCd(), tokenInfo));
        webRequest.setNodeCd(trimToNull(request.getNodeCd()));
        webRequest.setIncSubNodeYn(request.getIncSubNodeYn());
        webRequest.setUserKeyword(trimToNull(request.getUserKeyword()));
        // ★상태는 서버 상수. 앱 Request 에 필드 자체가 없어 클라이언트 값이 흘러들 여지가 없다.
        webRequest.setAccountStatus(SelfJoinListParam.STATUS_PENDING);

        SelfJoinListResponse response = user09Service.selectSelfJoinList(
                SelfJoinListParam.from(webRequest, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 처리 이력 목록(무한 스크롤). 조회는 감사 대상이 아니므로 감사 컨텍스트를 만들지 않는다. */
    @GetMapping("/history")
    public ResponseEntity<?> getHistoryList(
            @ModelAttribute AppSelfJoinHistoryRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);

        SelfJoinHistoryListRequest webRequest = new SelfJoinHistoryListRequest();
        webRequest.setSiteCd(resolveSiteCd(request.getSiteCd(), tokenInfo));
        webRequest.setNodeCd(trimToNull(request.getNodeCd()));
        webRequest.setIncSubNodeYn(request.getIncSubNodeYn());
        webRequest.setUserKeyword(trimToNull(request.getUserKeyword()));
        webRequest.setActionType(trimToNull(request.getActionType()));
        webRequest.setStartDate(trimToNull(request.getStartDate()));
        webRequest.setEndDate(trimToNull(request.getEndDate()));
        webRequest.setPage(request.getPage());
        webRequest.setPageSize(request.getPageSize());

        SelfJoinHistoryListResponse response = user09Service.selectSelfJoinHistoryList(
                SelfJoinHistoryListParam.from(webRequest, tokenInfo));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ====================================================================
    // 승인 / 거부
    // ====================================================================

    @PostMapping("/approve")
    public ResponseEntity<?> approve(
            @RequestBody AppSelfJoinApproveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpRequest) {

        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        SelfJoinApproveRequest webRequest = new SelfJoinApproveRequest();
        webRequest.setUserCd(request.getUserCd());
        webRequest.setHireDate(request.getHireDate());
        webRequest.setRankCd(request.getRankCd());
        webRequest.setStdWorkType(request.getStdWorkType());
        webRequest.setStdWorkWeekMinutes(request.getStdWorkWeekMinutes());
        webRequest.setStdWorkReasonCd(request.getStdWorkReasonCd());
        // 앱 시트에는 고용형태 입력란이 없다 — 서버가 정규직으로 고정한다.
        webRequest.setEmploymentType(EMPLOYMENT_TYPE_REGULAR);

        user09Service.approveSelfJoin(
                SelfJoinApproveParam.from(webRequest, jwtUtil.getAllClaimsAsMap(authorization)),
                buildAuditContext(httpRequest));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/reject")
    public ResponseEntity<?> reject(
            @RequestBody AppSelfJoinRejectRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpRequest) {

        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        SelfJoinRejectRequest webRequest = new SelfJoinRejectRequest();
        webRequest.setUserCd(request.getUserCd());
        webRequest.setRejectReason(request.getRejectReason());

        user09Service.rejectSelfJoin(
                SelfJoinRejectParam.from(webRequest, jwtUtil.getAllClaimsAsMap(authorization)),
                buildAuditContext(httpRequest));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    // ====================================================================
    // 옵션 / 스코프
    // ====================================================================

    /**
     * 승인 시트 입력 옵션(소정근로 + 직급).
     *
     * <p><b>권한 게이트 미부여(의도적)</b> — 웹 형제 EP({@code User01Controller#getStdWorkOptions})와
     * 동일 수준이다. 회사 단위 정책 상수와 코드표만 나가고 PII·타 테넌트 데이터가 없으며, 실제 쓰기
     * 경로({@code /approve})가 fail-closed 로 막는다.
     */
    @GetMapping("/approve-options")
    public ResponseEntity<?> getApproveOptions(
            @RequestParam(value = "siteCd", required = false) String siteCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        String cmpnyCd = (tokenInfo == null) ? null : tokenInfo.gv_cmpnyCd();

        StdWorkOptionsResponse stdWork = user01Service.getStdWorkOptions(cmpnyCd, resolveSiteCd(siteCd, tokenInfo));

        List<AppSelfJoinApproveOptionsResponse.ReasonOption> reasonOptions = new ArrayList<>();
        if (stdWork != null && stdWork.getReasonOptions() != null) {
            for (StdWorkOptionsResponse.ReasonOption option : stdWork.getReasonOptions()) {
                reasonOptions.add(AppSelfJoinApproveOptionsResponse.ReasonOption.builder()
                        .reasonCd(option.getReasonCd())
                        .reasonNm(option.getReasonNm())
                        .build());
            }
        }

        return ResponseEntity.status(HttpStatus.OK).body(AppSelfJoinApproveOptionsResponse.builder()
                .cmpnyWeekStdMinutes(stdWork == null ? 0 : stdWork.getCmpnyWeekStdMinutes())
                .reasonOptions(reasonOptions)
                .rankOptions(selectRankOptions(cmpnyCd))
                .build());
    }

    /**
     * 부서 필터 칩 소스 — 요청자가 정/부 관리자인 노드(seed) 목록.
     *
     * <p>{@code companyWide} 판정은 {@code AuthRoleUtils.isManager} 를 쓴다 —
     * {@code canManageNodeExcludeSafe} 의 전사 통과 조건과 <b>같은 함수</b>여야 화면 축과 서버 축이
     * 어긋나지 않는다.
     */
    @GetMapping("/scope-nodes")
    public ResponseEntity<?> getScopeNodes(
            @RequestParam(value = "siteCd", required = false) String siteCd,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo tokenInfo = jwtUtil.getAllClaimsAsMap(authorization);
        if (tokenInfo == null || tokenInfo.gv_cmpnyCd() == null || tokenInfo.gv_cmpnyCd().isBlank()) {
            throw new ApiException(CommonErrorCode.COMMON_400_003);
        }
        String targetSiteCd = resolveSiteCd(siteCd, tokenInfo);

        // 본 EP 는 User09Service 를 타지 않으므로 사업장 인가를 여기서 건다(타 사업장 조직도 정찰 방지).
        siteAccessService.assertSiteAccess(
                tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd(), tokenInfo.gv_authCd(),
                tokenInfo.gv_siteCd(), targetSiteCd);

        boolean companyWide = AuthRoleUtils.isManager(tokenInfo.gv_authCd());

        List<AppSelfJoinScopeNodesResponse.Node> nodes = new ArrayList<>();
        if (!companyWide) {
            // 전사 역할은 부서를 고를 필요가 없으므로 조회 자체를 생략한다.
            List<AdminSeedNodeResult> rows = adminScopeMapper.selectAdminSeedNodes(
                    ScopedNodeQuery.of(tokenInfo.gv_cmpnyCd(), targetSiteCd, tokenInfo.gv_userCd()));
            if (rows != null) {
                for (AdminSeedNodeResult row : rows) {
                    nodes.add(AppSelfJoinScopeNodesResponse.Node.builder()
                            .nodeCd(row.nodeCd())
                            .nodeNm(row.nodeNm())
                            .build());
                }
            }
        }

        log.info("앱 셀프가입 부서 스코프 조회 - siteCd={}, 전사={}, seed={}건",
                targetSiteCd, companyWide, nodes.size());

        return ResponseEntity.status(HttpStatus.OK).body(AppSelfJoinScopeNodesResponse.builder()
                .companyWide(companyWide)
                .nodes(nodes)
                .build());
    }

    // ====================================================================
    // 내부
    // ====================================================================

    /**
     * 직급[COM007] 옵션 조회.
     *
     * <p>기초코드 목록은 그룹 전체를 뜻하는 합성 행(BAIM_VAL_D_CD 가 NULL, 명칭 '전체')을 함께
     * 내려주므로 제외한다 — 그대로 두면 화면에 "전체" 라는 직급이 생긴다.
     */
    private List<AppSelfJoinApproveOptionsResponse.RankOption> selectRankOptions(String cmpnyCd) {

        List<AppSelfJoinApproveOptionsResponse.RankOption> rankOptions = new ArrayList<>();
        BaseInfoListResponse response =
                baseinfoService.selectBaseinfoList(new BaseInfoListParam(List.of(BASE_CODE_RANK), cmpnyCd));
        if (response == null || response.getBaseInfoList() == null) {
            return rankOptions;
        }
        for (BaseInfoResult row : response.getBaseInfoList()) {
            if (row == null || row.getBaimValDCd() == null || row.getBaimValDCd().isBlank()) {
                continue;
            }
            rankOptions.add(AppSelfJoinApproveOptionsResponse.RankOption.builder()
                    .rankCd(row.getBaimValDCd())
                    .rankNm(row.getBaimValDNm())
                    .build());
        }
        return rankOptions;
    }

    /**
     * 조회 사업장 해석 — 요청 파라미터 우선, 공백이면 토큰 사업장으로 폴백.
     *
     * <p>앱 관리자 모드는 현장 전환 시 토큰을 재발급하지 않아 파라미터가 필요하다. 값이 무엇이든
     * 최종 판정은 서버 {@code assertSiteAccess} 가 한다(클라 값 신뢰가 아니다).
     */
    private String resolveSiteCd(String requestSiteCd, TokenInfo tokenInfo) {

        if (requestSiteCd != null && !requestSiteCd.isBlank()) {
            return requestSiteCd.trim();
        }
        return (tokenInfo == null) ? null : tokenInfo.gv_siteCd();
    }

    private static String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /** 감사 컨텍스트(IP/UA) 추출 — 웹 {@code User09Controller} 와 동일 방식. */
    private AuditContext buildAuditContext(HttpServletRequest httpRequest) {
        return new AuditContext(
                ClientIpExtractor.extract(httpRequest),
                httpRequest != null ? httpRequest.getHeader("User-Agent") : null);
    }
}
