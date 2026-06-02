package com.prafta.web.attd.attd05.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.attd05.application.param.LeaveTypeListParam;
import com.prafta.web.attd.attd05.application.param.SchTypeDeleParam;
import com.prafta.web.attd.attd05.application.param.SchTypeListParam;
import com.prafta.web.attd.attd05.application.param.SchTypeParam;
import com.prafta.web.attd.attd05.application.param.UserWorkPlansParam;
import com.prafta.web.attd.attd05.application.param.WorkPlanCellDeleParam;
import com.prafta.web.attd.attd05.dto.request.SchTypeDeleRequst;
import com.prafta.web.attd.attd05.dto.request.SchTypeListRequst;
import com.prafta.web.attd.attd05.dto.request.SchTypeRequst;
import com.prafta.web.attd.attd05.dto.request.UserWorkPlansRequest;
import com.prafta.web.attd.attd05.dto.request.WorkPlanCellDeleRequst;
import com.prafta.web.attd.attd05.dto.response.LeaveTypeResponse;
import com.prafta.web.attd.attd05.dto.response.SaveUserWorkPlansResponse;
import com.prafta.web.attd.attd05.dto.response.SchTypeListResponse;
import com.prafta.web.attd.attd05.dto.response.UserWorkPlansResponse;
import com.prafta.web.attd.attd05.service.Attd05Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/attd05")
@RequiredArgsConstructor
public class Attd05Controller {

    private final Attd05Service attd05Service;
    private final JwtUtil jwtUtil;
    
    @GetMapping("/user-work-plans")
    public ResponseEntity<?> getUserWorkPlan(@ModelAttribute UserWorkPlansRequest request, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	UserWorkPlansResponse response = attd05Service.getUserWorkPlan(UserWorkPlansParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/sch-type-lists")
    public ResponseEntity<?> getSchTypeList(@ModelAttribute SchTypeListRequst request, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	SchTypeListResponse response = attd05Service.getSchTypeList(SchTypeListParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    @GetMapping("/leave-type-lists")
    public ResponseEntity<?> getLeaveTypeList(@RequestHeader(value = "Authorization", required = false) String authorization) {

    	LeaveTypeResponse response = attd05Service.getLeaveTypeList(LeaveTypeListParam.from(jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    @PostMapping("/save-user-work-plans")
    public ResponseEntity<?> saveUserWorkPlans(@RequestBody List<SchTypeRequst> request, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	SaveUserWorkPlansResponse response = attd05Service.saveUserWorkPlans(SchTypeParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    @PostMapping("/delete-user-work-plans")
    public ResponseEntity<?> deleteUserWorkPlans(@RequestBody List<SchTypeDeleRequst> request, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	attd05Service.deleteUserWorkPlans(SchTypeDeleParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * PRAFTA-041 - 근무계획 셀(사용자+근무일) 단위 삭제(셀 비우기).
     * 비우는 셀이 법정 연차였으면 직접 연차 사용기록 취소(차감 복원)를 함께 수행한다.
     * 기존 월 단위 {@code /delete-user-work-plans}(fnDelete 전용)와 별개의 경로다.
     */
    @PostMapping("/delete-user-work-plan-cells")
    public ResponseEntity<?> deleteUserWorkPlanCells(@RequestBody List<WorkPlanCellDeleRequst> request, @RequestHeader(value = "Authorization", required = false) String authorization) {

    	attd05Service.deleteUserWorkPlanCells(WorkPlanCellDeleParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
