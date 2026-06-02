package com.prafta.web.attd.attd06.controller;

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
import com.prafta.web.attd.attd06.application.param.DeleteShiftTeamParam;
import com.prafta.web.attd.attd06.application.param.DeleteShiftTeamUserParam;
import com.prafta.web.attd.attd06.application.param.InsertShiftTeamUsersParam;
import com.prafta.web.attd.attd06.application.param.ShiftSchInfosParam;
import com.prafta.web.attd.attd06.application.param.ShiftTeamUserInfosParam;
import com.prafta.web.attd.attd06.application.param.ShiftTypeDetailListsParam;
import com.prafta.web.attd.attd06.application.param.ShiftTypeListsParam;
import com.prafta.web.attd.attd06.application.param.ShiftUserSchInfosParam;
import com.prafta.web.attd.attd06.application.param.UpdateShiftTeamLeadersParam;
import com.prafta.web.attd.attd06.application.param.UpdateShiftTeamNmParam;
import com.prafta.web.attd.attd06.application.param.UpdateShiftTeamPeriodParam;
import com.prafta.web.attd.attd06.application.param.UserListsParam;
import com.prafta.web.attd.attd06.dto.request.DeleteShiftTeamRequest;
import com.prafta.web.attd.attd06.dto.request.DeleteShiftTeamUserRequest;
import com.prafta.web.attd.attd06.dto.request.InsertShiftTeamUsersRequest;
import com.prafta.web.attd.attd06.dto.request.ShiftSchInfosRequest;
import com.prafta.web.attd.attd06.dto.request.ShiftTeamUserInfosRequest;
import com.prafta.web.attd.attd06.dto.request.ShiftTypeDetailListsRequest;
import com.prafta.web.attd.attd06.dto.request.ShiftTypeListsRequest;
import com.prafta.web.attd.attd06.dto.request.ShiftUserSchInfosRequest;
import com.prafta.web.attd.attd06.dto.request.UpdateShiftTeamLeadersRequest;
import com.prafta.web.attd.attd06.dto.request.UpdateShiftTeamNmRequest;
import com.prafta.web.attd.attd06.dto.request.UpdateShiftTeamPeriodRequest;
import com.prafta.web.attd.attd06.dto.request.UserListsRequest;
import com.prafta.web.attd.attd06.dto.response.ShiftTeamUserInfosResponse;
import com.prafta.web.attd.attd06.dto.response.ShiftTypeDetailListsResponse;
import com.prafta.web.attd.attd06.dto.response.ShiftTypeListsResponse;
import com.prafta.web.attd.attd06.dto.response.UserListsResponse;
import com.prafta.web.attd.attd06.service.Attd06Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/attd06")
@RequiredArgsConstructor
public class Attd06Controller {

    private final Attd06Service attd06Service;
    private final JwtUtil jwtUtil;

    @GetMapping("/shift-type-lists")
    public ResponseEntity<?> getShiftTypeLists(
            @ModelAttribute ShiftTypeListsRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ShiftTypeListsResponse response = attd06Service.getShiftTypeLists(
                ShiftTypeListsParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/user-lists")
    public ResponseEntity<?> getUserLists(
            @ModelAttribute UserListsRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        UserListsResponse response = attd06Service.getUserLists(
                UserListsParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/shift-type-detail-lists")
    public ResponseEntity<?> getShiftTypeDetailLists(
            @ModelAttribute ShiftTypeDetailListsRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ShiftTypeDetailListsResponse response = attd06Service.getShiftTypeDetailLists(
                ShiftTypeDetailListsParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/shift-team-user-infos")
    public ResponseEntity<?> getShiftTeamUserInfos(
            @ModelAttribute ShiftTeamUserInfosRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        ShiftTeamUserInfosResponse response = attd06Service.getShiftTeamUserInfos(
                ShiftTeamUserInfosParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/insert-shift-sch-infos")
    public ResponseEntity<?> insertShiftSchInfos(
            @RequestBody ShiftSchInfosRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        attd06Service.insertShiftSchInfos(
                ShiftSchInfosParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
    
    @PostMapping("/update-shift-user-sch-infos")
    public ResponseEntity<?> updateShiftUserSchInfos(
            @RequestBody ShiftUserSchInfosRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        attd06Service.updateShiftUserSchInfos(
        		ShiftUserSchInfosParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/update-shift-team-nms")
    public ResponseEntity<?> updateShiftTeamNm(
            @Valid @RequestBody UpdateShiftTeamNmRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        attd06Service.updateShiftTeamNm(
                UpdateShiftTeamNmParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/delete-shift-team-users")
    public ResponseEntity<?> deleteShiftTeamUser(
            @Valid @RequestBody DeleteShiftTeamUserRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        attd06Service.deleteShiftTeamUser(
                DeleteShiftTeamUserParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/insert-shift-team-users")
    public ResponseEntity<?> insertShiftTeamUsers(
            @RequestBody List<InsertShiftTeamUsersRequest> request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        attd06Service.insertShiftTeamUsers(
                InsertShiftTeamUsersParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/update-shift-team-leaders")
    public ResponseEntity<?> updateShiftTeamLeaders(
            @Valid @RequestBody UpdateShiftTeamLeadersRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        attd06Service.updateShiftTeamLeaders(
                UpdateShiftTeamLeadersParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/update-shift-team-periods")
    public ResponseEntity<?> updateShiftTeamPeriod(
            @Valid @RequestBody UpdateShiftTeamPeriodRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        attd06Service.updateShiftTeamPeriod(
                UpdateShiftTeamPeriodParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/delete-shift-teams")
    public ResponseEntity<?> deleteShiftTeam(
            @Valid @RequestBody DeleteShiftTeamRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        attd06Service.deleteShiftTeam(
                DeleteShiftTeamParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
