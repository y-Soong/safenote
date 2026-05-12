package com.prafta.web.attd.attd08.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prafta.common.security.JwtUtil;
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
}
