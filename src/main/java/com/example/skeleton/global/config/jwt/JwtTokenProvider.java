package com.example.skeleton.global.config.jwt;

import com.example.skeleton.domain.client.entity.Client;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Key;
import java.util.*;

@Slf4j
@Component
public class JwtTokenProvider implements InitializingBean {

    // 사용자 권한 정보
    public static final String AUTH = "auth";

    // JWT 서명용 비밀키
    private final String SECRET;

    // 액세스 토큰 유효시간
    private final Long ACCESS_TOKEN_DURATION_MS;

    // 리프레시 토큰 유효시간
    private final Long REFRESH_TOKEN_DURATION_MS;

    // JWT 서명용 키
    private Key key;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("86400") Long accessTokenDuration,
            @Value("604800") Long refreshTokenDuration
    ) {
        this.SECRET = secret;
        this.ACCESS_TOKEN_DURATION_MS = accessTokenDuration * 1000;
        this.REFRESH_TOKEN_DURATION_MS = refreshTokenDuration * 1000;
    }

    /**
     * JWT 서명용 키 설정 (입력된 SECRET을 디코딩 및 암호화하여 Key 타입의 서명용 키로 설정)
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        byte[] decodedKeyBytes = Decoders.BASE64.decode(SECRET);
        this.key = Keys.hmacShaKeyFor(decodedKeyBytes);
    }

    /**
     * 토큰 생성 및 반환 : Client와 생성할 토큰 타입을 변수로 받음
     * @param client Client 객체
     * @param tokenType String : "access" or "refresh"
     */
    public String issueToken(Client client, String tokenType) {
        Date now = new Date();
        Date expiry = tokenType.equals("access")
                ? new Date(now.getTime() + ACCESS_TOKEN_DURATION_MS)
                : new Date(now.getTime() + REFRESH_TOKEN_DURATION_MS);

        return createToken(client, expiry);
    }

    /**
     * 주어진 Client 및 만료 시간(expiry)을 기반으로 액세스 토큰을 생성
     * @param client
     * @param expiry
     */
    private String createToken(Client client, Date expiry) {
        String token = Jwts.builder()
                .setSubject(client.getClientId())
                .setExpiration(expiry)
                .setIssuedAt(new Date())
                // Claim 지정(clientId 저장)
                .addClaims(Map.of("clientId", client.getClientId()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return token;
    }

    /**
     * 토큰이 유효한 지 확인
     * @param token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            log.info("토큰 : {}", token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("JWT토큰 사용기한이 만료되었습니다 : {}", token);
            return false;
        } catch (Exception e) {
            log.info("JWT토큰이 유효하지 않습니다 : {} / {}", token, e);
            return false;
        }
    }

    /**
     * 토큰에서 사용자 정보와 권한을 추출하여 Spring Security의 Authentication 객체를 생성하고 반환
     * @param token
     */
    public Authentication getAuthentication(String token) {

        Claims claims = getClaims(token);

        if (claims != null) {
            String authValue = claims.get(AUTH, String.class);

            if (authValue != null && !authValue.isEmpty()) {
                Set<SimpleGrantedAuthority> authority = Collections.singleton(
                        new SimpleGrantedAuthority(authValue)
                );

                return new UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User(claims.getSubject(), "", authority),
                        token,
                        authority
                );
            } else {
                log.info("JWT토큰이 권한 정보를 포함하고 있지 않습니다.");
                return null;
            }
        } else {
            log.info("JWT토큰의 클레임이 존재하지 않습니다(null).");
            return null;
        }
    }

    /**
     * 토큰에서 Claim 추출
     * @param token
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 액세스 토큰의 claim에서 clientId를 가져오기
     */
    public String getClientIdFromToken() {

        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String authorizationHeader = request.getHeader("Authorization");

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7); // "Bearer " 프리픽스 제거
                Claims claims = getClaims(token);
                return claims.get("clientId", String.class);
            } else {
                log.info("사용자 인증에 필요한 JWT토큰이 없거나 제대로 된 형태가 아닙니다.");
                return null;
            }
        } catch (Exception e) {
            log.info("사용자 인증을 위한 토큰에 문제가 있습니다.");
            return null;
        }

    }

}
