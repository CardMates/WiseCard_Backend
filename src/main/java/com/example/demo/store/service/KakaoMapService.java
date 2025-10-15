package com.example.demo.store.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.demo.config.ApiKeyConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoMapService {

    private final RestTemplate restTemplate;
    private final ApiKeyConfig apiKeyConfig;

    private static final String KAKAO_API_BASE_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

    public List<Map<String, Object>> searchPlaces(String categoryCode, Double latitude, Double longitude) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(KAKAO_API_BASE_URL)
                    .queryParam("query", "매장")
                    .queryParam("category_group_code", categoryCode)
                    .queryParam("x", longitude)
                    .queryParam("y", latitude)
                    .queryParam("radius", 2000)
                    .queryParam("page", 1)
                    .queryParam("size", 15)
                    .build()
                    .encode()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + apiKeyConfig.getKakaoApiKey());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("카카오 API 호출 - URL: {}", uri);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, Map.class
            ).getBody();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");

            log.info("카카오 API 응답 - 총 {}개 장소 검색됨", documents != null ? documents.size() : 0);

            return documents != null ? documents : new ArrayList<>();

        } catch (Exception e) {
            log.error("카카오 API 호출 실패", e);
            return new ArrayList<>();
        }
    }

    /**
     * 카테고리별 온라인 매장 검색 (위도/경도 없이)
     */
    public List<Map<String, Object>> searchPlacesByCategory(String category) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(KAKAO_API_BASE_URL)
                    .queryParam("query", category)
                    .queryParam("page", 1)
                    .queryParam("size", 15)
                    .build()
                    .encode()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + apiKeyConfig.getKakaoApiKey());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("카카오 API 호출 (온라인 매장) - URL: {}", uri);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, Map.class
            ).getBody();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");

            log.info("카카오 API 응답 (온라인 매장) - 총 {}개 장소 검색됨", documents != null ? documents.size() : 0);

            return documents != null ? documents : new ArrayList<>();

        } catch (Exception e) {
            log.error("카카오 API 호출 실패 (온라인 매장)", e);
            return new ArrayList<>();
        }
    }
}