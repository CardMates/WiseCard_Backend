package com.example.demo.common.controller;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sample")
public class sampleController {
    @RequestMapping("/sample")
    public String sampleEndpoint(
            @RequestBody List<Integer> requestBody
            ){
        return "Sample endpoint response";
    }
}
