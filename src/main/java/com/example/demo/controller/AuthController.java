package com.example.demo.controller;

import com.example.demo.auth.dto.AccessTokenRequest;
import com.example.demo.auth.dto.RefreshTokenRequest;
import com.example.demo.auth.dto.TokenResponse;
import com.example.demo.auth.service.KakaoLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final KakaoLoginService kakaoLoginService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody AccessTokenRequest request) {
        TokenResponse token = kakaoLoginService.login(request);
        return ResponseEntity.ok(token);
    }

    @PutMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestBody RefreshTokenRequest request) {
        TokenResponse token = kakaoLoginService.reissue(request);
        return ResponseEntity.ok(token);
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw() {
        kakaoLoginService.withdraw();
        return ResponseEntity.ok().build();
    }
}
