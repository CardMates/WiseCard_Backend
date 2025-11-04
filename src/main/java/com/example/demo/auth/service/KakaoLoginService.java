package com.example.demo.auth.service;

import com.example.demo.auth.client.KakaoOAuthClient;
import com.example.demo.auth.client.KakaoUserInfo;
import com.example.demo.auth.dto.AccessTokenRequest;
import com.example.demo.auth.dto.RefreshTokenRequest;
import com.example.demo.auth.dto.TokenResponse;
import com.example.demo.auth.entity.Member;
import com.example.demo.auth.jwt.JwtTokenProvider;
import com.example.demo.auth.repository.MemberRepository;
import com.example.demo.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.demo.auth.util.AuthUtils.getMemberId;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoLoginService {
    private final MemberRepository memberRepository;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public TokenResponse login(AccessTokenRequest request) {
        KakaoUserInfo kakaoUserInfo = kakaoOAuthClient.retrieveUserInfo(request.accessToken());
        Member member = memberRepository.findBySocialId(kakaoUserInfo.getId()).orElseGet(()-> {
            Member newMember = Member.builder()
                    .socialId(kakaoUserInfo.getId())
                    .name(kakaoUserInfo.getNickName())
                    .email(kakaoUserInfo.getEmail())
                    .build();
            return memberRepository.save(newMember);
        });

        String accessToken = jwtTokenProvider.createAccessToken(member.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        refreshTokenService.saveOrUpdateToken(member.getId(), refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse reissue(RefreshTokenRequest request) {
        return jwtTokenProvider.reissueToken(request.refreshToken());
    }


    @Transactional
    public void withdraw() {
        Long memberId = getMemberId();
        refreshTokenRepository.deleteByMemberId(memberId);
        memberRepository.deleteById(memberId);
    }
}
