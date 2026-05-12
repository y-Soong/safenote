package com.prafta.web.attd.attd08.service.impl;

import org.springframework.stereotype.Service;

import com.prafta.web.attd.attd08.mapper.Attd08Mapper;
import com.prafta.web.attd.attd08.service.Attd08Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Attd08ServiceImpl implements Attd08Service {

    private final Attd08Mapper attd08Mapper;
}
