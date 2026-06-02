package com.prafta.web.user.user01.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.common.security.JwtUtil;
import com.prafta.common.util.ClientIpExtractor;
import com.prafta.web.user.user01.application.param.HireDateHistoryParam;
import com.prafta.web.user.user01.application.param.HireDateImpactParam;
import com.prafta.web.user.user01.application.param.LeaveInfoParam;
import com.prafta.web.user.user01.application.param.MyPasswdParam;
import com.prafta.web.user.user01.application.param.MyProfileParam;
import com.prafta.web.user.user01.application.param.ScheduleWithdrawalParam;
import com.prafta.web.user.user01.application.param.SiteNodeAdminCandidateListParam;
import com.prafta.web.user.user01.application.param.UserCreateParam;
import com.prafta.web.user.user01.application.param.UserCreditParam;
import com.prafta.web.user.user01.application.param.UserHireDateParam;
import com.prafta.web.user.user01.application.param.UserInfoListParam;
import com.prafta.web.user.user01.application.param.UserInfoParam;
import com.prafta.web.user.user01.application.param.UserPasswdParam;
import com.prafta.web.user.user01.application.param.WithdrawMyAccountParam;
import com.prafta.web.user.user01.application.param.WithdrawalCancelParam;
import com.prafta.web.user.user01.dto.UserBatchUpdateResponse;
import com.prafta.web.user.user01.dto.request.MyPasswdRequest;
import com.prafta.web.user.user01.dto.request.ScheduleWithdrawalRequest;
import com.prafta.web.user.user01.dto.request.SiteNodeAdminCandidateListRequest;
import com.prafta.web.user.user01.dto.request.UserCreateRequest;
import com.prafta.web.user.user01.dto.request.UserCreditRequest;
import com.prafta.web.user.user01.dto.request.UserHireDateRequest;
import com.prafta.web.user.user01.dto.request.UserInfoListRequest;
import com.prafta.web.user.user01.dto.request.UserInfoRequest;
import com.prafta.web.user.user01.dto.request.UserPasswdRequest;
import com.prafta.web.user.user01.dto.request.WithdrawalCancelRequest;
import com.prafta.web.user.user01.dto.response.HireDateHistoryResponse;
import com.prafta.web.user.user01.dto.response.HireDateImpactResponse;
import com.prafta.web.user.user01.dto.response.LeaveInfoResponse;
import com.prafta.web.user.user01.dto.response.MyProfileResponse;
import com.prafta.web.user.user01.dto.response.SiteNodeAdminCandidateListResponse;
import com.prafta.web.user.user01.dto.response.UserInfoListResponse;
import com.prafta.web.user.user01.service.User01BatchService;
import com.prafta.web.user.user01.service.User01Service;
import com.prafta.web.user.user01.upload.dto.response.UserUploadJobStartResponse;
import com.prafta.web.user.user01.upload.dto.response.UserUploadJobStatusResponse;
import com.prafta.web.user.user01.upload.service.UploadJobService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/user01")
@RequiredArgsConstructor
public class User01Controller { 	
	
	private final User01Service user01Service;
	private final User01BatchService user01BatchService;
	// PRAFTA-037-F6: 비동기 업로드 잡 서비스
	private final UploadJobService uploadJobService;
	private final JwtUtil jwtUtil;
    
    @GetMapping("/user-info-lists")
    public ResponseEntity<?> getUserInfoList(@ModelAttribute UserInfoListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	UserInfoListResponse response = user01Service.selectUserInfoList(UserInfoListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));
    	
    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    @PostMapping("/update-user-passwd")
    public ResponseEntity<?> updateUserPw(@RequestBody UserPasswdRequest request, @RequestHeader(value = "Authorization", required = true) String authorization) {

    	user01Service.updateUserPw(UserPasswdParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/update-my-passwd")
    public ResponseEntity<?> updateMyPw(@RequestBody MyPasswdRequest request, @RequestHeader(value = "Authorization", required = true) String authorization) {

    	user01Service.updateMyPw(MyPasswdParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).build();
    }
    
    @PostMapping("/update-user-infos")
    public ResponseEntity<?> updateUserInfo(@Valid @RequestBody List<UserInfoRequest> reqeust, @RequestHeader(value = "Authorization", required = false) String authorization) {
    	
    	UserBatchUpdateResponse result = user01BatchService.updateUserInfoBatch(UserInfoParam.from(reqeust, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).body(result);
    }
    
    @PostMapping("/withdraw-my-account")
    public ResponseEntity<?> withdrawMyAccount(@RequestHeader(value = "Authorization", required = true) String authorization) {

        // 탈퇴 대상은 토큰으로만 결정한다 (IDOR 방지). request body의 식별자는 사용하지 않는다.
    	user01Service.withdrawMyAccount(WithdrawMyAccountParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/schedule-withdrawal")
    public ResponseEntity<?> scheduleWithdrawal(@RequestBody ScheduleWithdrawalRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	user01Service.scheduleWithdrawal(ScheduleWithdrawalParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).build();
    }
    
    @PostMapping("/cancel-withdrawal")
    public ResponseEntity<?> cancelWithdrawal(@RequestBody WithdrawalCancelRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	user01Service.cancelWithdrawal(WithdrawalCancelParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).build();
    }
    
    

    @GetMapping("/my-profile")
    public ResponseEntity<?> getMyProfile(@RequestHeader("Authorization") String authorization) {

        // 조회 대상은 토큰에서만 결정한다 (IDOR 방지). 토큰 미존재/무효 시 param.from()에서 거부.
        MyProfileResponse response = user01Service.selectMyProfile(MyProfileParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/site-node-admin-candidate-lists")
    public ResponseEntity<?> getSiteNodeAdminCandidateLists(@ModelAttribute SiteNodeAdminCandidateListRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	SiteNodeAdminCandidateListResponse response = user01Service.selectSiteNodeAdminCandidateLists(SiteNodeAdminCandidateListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ===== PRAFTA-017-4 - 근태/연차 정보 (master/hr 전용, 정책서 §8.5.6~§8.5.8) =====

    @GetMapping("/{userCd}/leave-info")
    public ResponseEntity<?> getLeaveInfo(@PathVariable("userCd") String userCd, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	LeaveInfoResponse response = user01Service.selectLeaveInfo(LeaveInfoParam.from(userCd, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/update-user-credit")
    public ResponseEntity<?> updateUserCredit(@Valid @RequestBody UserCreditRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	user01Service.updateUserCredit(UserCreditParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/{userCd}/hire-date-impact")
    public ResponseEntity<?> getHireDateImpact(@PathVariable("userCd") String userCd, @RequestParam("newDate") String newDate, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	HireDateImpactResponse response = user01Service.analyzeHireDateImpact(HireDateImpactParam.from(userCd, newDate, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/update-user-hire-date")
    public ResponseEntity<?> updateUserHireDate(@Valid @RequestBody UserHireDateRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	user01Service.updateUserHireDate(UserHireDateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/{userCd}/hire-date-history")
    public ResponseEntity<?> getHireDateHistory(@PathVariable("userCd") String userCd, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	HireDateHistoryResponse response = user01Service.selectHireDateHistory(HireDateHistoryParam.from(userCd, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // ===== PRAFTA-036 - 관리자 단건 사용자 생성 (UserInfoPop callmethod_p='C' 모드) =====

    @PostMapping("/insert-user-info")
    public ResponseEntity<?> insertUserInfo(@Valid @RequestBody UserCreateRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	user01Service.insertUserOne(UserCreateParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

    	return ResponseEntity.status(HttpStatus.OK).build();
    }

    // ===== PRAFTA-036 - 엑셀 양식 다운로드 / 일괄 업로드 =====

    @GetMapping("/user-create-template")
    public ResponseEntity<byte[]> getUserCreateTemplate(
    		@RequestHeader(value = "Authorization", required = false) String authorization,
    		HttpServletRequest httpRequest) {

    	// PRAFTA-037-F5: 감사 컨텍스트(IP/UA) 추출. Service 계층이 HttpServletRequest 에 직접 의존하지 않도록 Controller 에서 추출.
    	AuditContext auditContext = new AuditContext(
    			ClientIpExtractor.extract(httpRequest),
    			httpRequest != null ? httpRequest.getHeader("User-Agent") : null
    	);

    	byte[] xlsx = user01Service.buildUserCreateTemplate(jwtUtil.getAllClaimsAsMap(authorization), auditContext);

    	String filename = "사용자생성양식.xlsx";
    	String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

    	HttpHeaders headers = new HttpHeaders();
    	headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    	headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);
    	headers.setContentLength(xlsx.length);

    	return ResponseEntity.status(HttpStatus.OK).headers(headers).body(xlsx);
    }

    @PostMapping(value = "/upload-user-creates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadUserCreates(@RequestPart("file") MultipartFile file, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	UserBatchUpdateResponse result = user01BatchService.uploadUserCreates(file, jwtUtil.getAllClaimsAsMap(authorization));

    	return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    // ===== PRAFTA-037-F6 - 엑셀 비동기 업로드 + 진행률 폴링 =====

    @PostMapping(value = "/upload-user-creates-async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadUserCreatesAsync(@RequestPart("file") MultipartFile file, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	UserUploadJobStartResponse start = uploadJobService.startUpload(file, jwtUtil.getAllClaimsAsMap(authorization));

    	return ResponseEntity.status(HttpStatus.OK).body(start);
    }

    @GetMapping("/upload-job/{jobId}")
    public ResponseEntity<?> getUploadJob(@PathVariable("jobId") String jobId, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	UserUploadJobStatusResponse status = uploadJobService.getStatus(jobId, jwtUtil.getAllClaimsAsMap(authorization));

    	return ResponseEntity.status(HttpStatus.OK).body(status);
    }

}
