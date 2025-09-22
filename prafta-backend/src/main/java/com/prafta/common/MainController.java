package com.prafta.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // JSON 형태로 응답
@RequestMapping("/prafta") // 공통 URI prefix
public class MainController {

    @GetMapping("/")
    public String hello() {
        return "Hello, PRAFTA!";
    }
}
