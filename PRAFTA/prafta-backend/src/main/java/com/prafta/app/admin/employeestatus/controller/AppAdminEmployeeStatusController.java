package com.prafta.app.admin.employeestatus.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.app.admin.common.scope.application.query.ScopedNodeQuery;
import com.prafta.app.admin.common.scope.mapper.AdminScopeMapper;
import com.prafta.app.admin.common.scope.result.AdminSeedNodeResult;
import com.prafta.app.admin.employeestatus.application.param.EmployeeGpsTrailParam;
import com.prafta.app.admin.employeestatus.application.param.EmployeeStatusDailyParam;
import com.prafta.app.admin.employeestatus.dto.response.EmployeeGpsTrailResponse;
import com.prafta.app.admin.employeestatus.dto.response.EmployeeStatusDailyResponse;
import com.prafta.app.admin.employeestatus.dto.response.EmployeeStatusScopeNodesResponse;
import com.prafta.app.admin.employeestatus.service.AppAdminEmployeeStatusService;
import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PRAFTA-001~003: 모바일 앱 관리자 모드 "직원관리"(실시간 근태 현황 + 외근 위치) 컨트롤러.
 *
 * <p>최종 URL(ApiPrefixConfig 가 {@code com.prafta.app.*} 에 {@code /prafta/appApi} 자동 부여):
 * <ul>
 *   <li>GET /prafta/appApi/admin/employee-status/daily        (일자 직원 현황 — PRAFTA-002)</li>
 *   <li>GET /prafta/appApi/admin/employee-status/scope-nodes  (부서 필터 칩 소스 — PRAFTA-002)</li>
 *   <li>GET /prafta/appApi/admin/employee-status/gps-trail    (외근 GPS 궤적 — PRAFTA-003)</li>
 * </ul>
 *
 * <p><b>daily 인가</b> — 사업장 인가 + 부서 스코프 2단 게이트는 {@code AppAdminEmployeeStatusServiceImpl}
 * 안에 있다(컨트롤러는 게이트를 중복 구현하지 않는다 — 신규 조회화면 노드 스코프 게이트 누락 재발 방지).
 *
 * <p><b>scope-nodes 예외</b> — 본 EP 는 위 서비스를 타지 않는 신규 조회라, {@code AppAdminSelfJoinController}
 * 의 선례(scope-nodes 는 컨트롤러가 {@code assertSiteAccess} 를 직접 건다)를 그대로 따른다(타 사업장 조직도
 * 정찰 방지). PII 없음(부서 코드/명칭만).
 *
 * <p><b>권한</b>([권한매트릭스 §3]) EMPLOYEE_STATUS = master ∥ hr ∥ nodeAdmin (safe 단독 ⛔, ATTD_DETAIL 축
 * 재사용 — PRAFTA-001). PII(휴대폰/이메일) 미노출 — 이름·노드명만. 조회 전용(쓰기 부수효과 없음).
 * GPS 좌표 평문/복호화값은 어떤 로그에도 남기지 않는다.
 */
@Slf4j
@RestController
@RequestMapping("/admin/employee-status")
@RequiredArgsConstructor
public class AppAdminEmployeeStatusController {

    private final AppAdminEmployeeStatusService employeeStatusService;
    /** scope-nodes 전용 사업장 인가(다른 EP 는 서비스 내부에서 수행). */
    private final SiteAccessService siteAccessService;
    /** 부서 필터 칩 소스(내가 정/부 관리자인 seed 노드). */
    private final AdminScopeMapper adminScopeMapper;
    private final JwtUtil jwtUtil;

    /** PRAFTA-002: 일자 직원 현황(workYmd 필수, siteCd/nodeCd/keyword/page/pageSize 선택). */
    @GetMapping("/daily")
    public ResponseEntity<?> getDaily(
            @RequestParam("workYmd") String workYmd,
            @RequestParam(value = "siteCd", required = false) String siteCd,
            @RequestParam(value = "nodeCd", required = false) String nodeCd,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        EmployeeStatusDailyResponse response = employeeStatusService.selectDaily(
                EmployeeStatusDailyParam.of(workYmd, siteCd, nodeCd, keyword, page, pageSize, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * PRAFTA-002: 부서 필터 칩 소스 — 요청자가 정/부 관리자인 노드(seed) 목록.
     *
     * <p>{@code companyWide} 판정은 {@code AuthRoleUtils.isManager} 를 쓴다(AppAdminSelfJoinController 와
     * 동일 원칙 — 서버 게이트 축과 화면 축이 어긋나지 않게).
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

        // 본 EP 는 employeeStatusService 를 타지 않으므로 사업장 인가를 여기서 건다(타 사업장 조직도 정찰 방지).
        siteAccessService.assertSiteAccess(
                tokenInfo.gv_cmpnyCd(), tokenInfo.gv_userCd(), tokenInfo.gv_authCd(),
                tokenInfo.gv_siteCd(), targetSiteCd);

        boolean companyWide = AuthRoleUtils.isManager(tokenInfo.gv_authCd());

        List<EmployeeStatusScopeNodesResponse.Node> nodes = new ArrayList<>();
        if (!companyWide) {
            // 전사 역할은 부서를 고를 필요가 없으므로 조회 자체를 생략한다.
            List<AdminSeedNodeResult> rows = adminScopeMapper.selectAdminSeedNodes(
                    ScopedNodeQuery.of(tokenInfo.gv_cmpnyCd(), targetSiteCd, tokenInfo.gv_userCd()));
            if (rows != null) {
                for (AdminSeedNodeResult row : rows) {
                    nodes.add(EmployeeStatusScopeNodesResponse.Node.builder()
                            .nodeCd(row.nodeCd())
                            .nodeNm(row.nodeNm())
                            .build());
                }
            }
        }

        log.info("앱 관리자 직원 현황 부서 스코프 조회 - siteCd={}, 전사={}, seed={}건",
                targetSiteCd, companyWide, nodes.size());

        return ResponseEntity.status(HttpStatus.OK).body(EmployeeStatusScopeNodesResponse.builder()
                .companyWide(companyWide)
                .nodes(nodes)
                .build());
    }

    /** PRAFTA-003: 외근 GPS 궤적(attdId 단건). 여러 attdId(2구간)는 프론트가 순차 호출해 병합한다. */
    @GetMapping("/gps-trail")
    public ResponseEntity<?> getGpsTrail(
            @RequestParam("attdId") String attdId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        TokenInfo token = jwtUtil.getAllClaimsAsMap(authorization);
        EmployeeGpsTrailResponse response = employeeStatusService.selectGpsTrail(
                EmployeeGpsTrailParam.of(attdId, token));

        return ResponseEntity.status(HttpStatus.OK).body(response);
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
}
