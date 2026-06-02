package com.prafta.common.cmm.dailyjoin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.annotation.NoAuth;
import com.prafta.common.cmm.dailyjoin.application.param.InsertDailyUserParam;
import com.prafta.common.cmm.dailyjoin.application.param.SiteInfoParam;
import com.prafta.common.cmm.dailyjoin.application.param.UserIdDupleCheckParam;
import com.prafta.common.cmm.dailyjoin.dto.request.InsertDailyUserRequest;
import com.prafta.common.cmm.dailyjoin.dto.request.SiteInfoRequest;
import com.prafta.common.cmm.dailyjoin.dto.request.UserIdDupleCheckRequest;
import com.prafta.common.cmm.dailyjoin.dto.response.InsertDailyUserResponse;
import com.prafta.common.cmm.dailyjoin.dto.response.SiteInfoResponse;
import com.prafta.common.cmm.dailyjoin.dto.response.UserIdDupleCheckResponse;
import com.prafta.common.cmm.dailyjoin.service.DailyJoinService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 일일사용자 회원가입(비로그인 외부 화면) 컨트롤러.
 * com.prafta.common 패키지이므로 ApiPrefixConfig 에 의해 실제 경로는 /prafta/comApi/dailyJoin/... 로 매핑된다.
 *
 * <p>AuthAspect 는 메서드 레벨 @NoAuth 만 JWT 면제 대상으로 인식하므로,
 * 클래스 레벨과 함께 각 엔드포인트(메서드)에도 @NoAuth 를 부착하여 비로그인 접근을 보장한다.</p>
 */
@Slf4j
@NoAuth
@RestController
@RequestMapping("/dailyJoin")
@RequiredArgsConstructor
public class DailyJoinController {

    private final DailyJoinService dailyJoinService;

    /** joinCd 기반 회사/사업장 정보 조회. */
    @NoAuth
    @GetMapping("/site-infos")
    public ResponseEntity<?> getSiteInfo(@ModelAttribute SiteInfoRequest request) {

        SiteInfoResponse response = dailyJoinService.selectSiteInfo(SiteInfoParam.from(request));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 사용자ID 중복체크. */
    @NoAuth
    @GetMapping("/user-id-duple-checks")
    public ResponseEntity<?> getUserIdDupleCheck(@ModelAttribute UserIdDupleCheckRequest request) {

        UserIdDupleCheckResponse response = dailyJoinService.checkUserIdDuple(UserIdDupleCheckParam.from(request));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** 일일사용자 회원가입. */
    @NoAuth
    @PostMapping("/insert-daily-user")
    public ResponseEntity<?> insertDailyUser(@RequestBody InsertDailyUserRequest request) {

        InsertDailyUserResponse response = dailyJoinService.insertDailyUser(InsertDailyUserParam.from(request));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
