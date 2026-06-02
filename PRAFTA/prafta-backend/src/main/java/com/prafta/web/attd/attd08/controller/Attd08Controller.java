package com.prafta.web.attd.attd08.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
import com.prafta.web.attd.attd08.application.param.AttdGpsTrailParam;
import com.prafta.web.attd.attd08.application.param.AttdListsParam;
import com.prafta.web.attd.attd08.dto.request.AttdGpsTrailRequest;
import com.prafta.web.attd.attd08.dto.request.AttdListsRequest;
import com.prafta.web.attd.attd08.dto.response.AttdGpsTrailResponse;
import com.prafta.web.attd.attd08.dto.response.AttdListsResponse;
import com.prafta.web.attd.attd08.service.Attd08Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/attd08")
@RequiredArgsConstructor
public class Attd08Controller {

    private final Attd08Service attd08Service;
    private final JwtUtil jwtUtil;

    @GetMapping("/attd-lists")
    public ResponseEntity<?> getAttdLists(
            @ModelAttribute AttdListsRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        AttdListsResponse response = attd08Service.getAttdLists(
                AttdListsParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/attd-gps-trail")
    public ResponseEntity<?> getAttdGpsTrail(
            @ModelAttribute AttdGpsTrailRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        AttdGpsTrailResponse response = attd08Service.getAttdGpsTrail(
                AttdGpsTrailParam.from(request, jwtUtil.getAllClaimsAsMap(authorization)));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
