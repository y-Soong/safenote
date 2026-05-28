package com.prafta.web.user.user01.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.user.user01.application.param.HireDateHistoryParam;
import com.prafta.web.user.user01.application.param.HireDateImpactParam;
import com.prafta.web.user.user01.application.param.LeaveInfoParam;
import com.prafta.web.user.user01.application.param.MyPasswdParam;
import com.prafta.web.user.user01.application.param.MyProfileParam;
import com.prafta.web.user.user01.application.param.ScheduleWithdrawalParam;
import com.prafta.web.user.user01.application.param.SiteNodeAdminCandidateListParam;
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

}
