package com.security.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties.Data;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.security.configurations.JwtConfig;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.util.*;
import java.util.function.Function;

import javax.crypto.SecretKey;

@Service
public class JwtService {
    @Autowired
    private JwtConfig jwtConfig;

    // build token
    private String buildToken(Map<String, Object> claims, UserDetails userDetails, Long expiration){
        return Jwts
            .builder()
            .claims(claims)
            .subject(userDetails.getUsername())
            .id(UUID.randomUUID().toString())
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSecretKey())
            .compact();
    }

    // lấy secret key từ config
    private SecretKey getSecretKey(){
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.secret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // tạo access token với claims
    public String generateAccessToken(Map<String, Object> claims, UserDetails userDetails){
        return buildToken(claims, userDetails, jwtConfig.expiration());
    }

    // tạo access token với claims mặc định
    public String generateAccessToken(UserDetails userDetails){
        return generateAccessToken(new HashMap<>(), userDetails);
    }

    // tạo refresh token với claims
    public String generateRefreshToken(Map<String, Object> claims, UserDetails userDetails){
        return buildToken(claims, userDetails, jwtConfig.refreshExpiration());
    }

    // tạo refresh token với claims mặc định
    public String generateRefreshToken(UserDetails userDetails){
        return generateRefreshToken(new HashMap<>(), userDetails);
    }

    // tạo token xác thực với claims
    public String generateVeruficationToken(Integer userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "verification");
        claims.put("userId", userId);

        return Jwts
            .builder()
            .claims(claims)
            .subject(String.valueOf(userId))
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + 86400000L))
            .signWith(getSecretKey())
            .compact();
    }

    // lấy tất cả claims từ token
    public Claims extractAllClaims(String token) {
        return Jwts
            .parser()
            .verifyWith(getSecretKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    // lấy claims từ token
    public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // lấy username từ token
    public String extractUsername(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    // lấy thời gian hết hạn từ token
    public Date extractExpiration(String token) {
        return extractClaims(token, Claims::getExpiration);
    }

    // lấy thời gian tạo token từ token
    public Date extractIssuedAt(String token) {
        return extractClaims(token, Claims::getIssuedAt);
    }

    // lấy id từ token
    public String extractJTI(String token) {
        return extractClaims(token, Claims::getId);
    }

    // lấy role từ token
    public String extractRole(String token) {
        return extractClaims(token, Claims -> Claims.get("role", String.class));
    }

    // lấy userId từ token xác thực
    public Integer extractUserIdFromVerificationToken(String token) {
        Claims claims = extractAllClaims(token);

        String type = claims.get("type", String.class);
        if(!"verification".equals(type)){
            throw new JwtException("Invalid token type");
        }
        return claims.get("userId", Integer.class);
    }

    // kiểm tra token có hết hạn chưa
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // kiểm tra token phải hợp lệ và không hết hạn
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

}
