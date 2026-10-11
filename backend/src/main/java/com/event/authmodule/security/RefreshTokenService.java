package com.event.authmodule.security;

import com.event.authmodule.entitys.RefreshToken;
import com.event.authmodule.repository.RefreshTokenRepository;
import com.event.authmodule.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final UserInfoRepository userInfoRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken createRefreshToken(String username){
        RefreshToken refreshToken = RefreshToken.builder()
                .userInfo(userInfoRepository.findByUsername(username).orElseThrow())
                .token(UUID.randomUUID().toString())
                .expiryTime(Instant.now().plusSeconds(60*60*24*7))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public void verifyExpiration(RefreshToken refreshToken) {
        if (refreshToken.getExpiryTime().isBefore(Instant.now())) {
            refreshTokenRepository.deleteByTokenValue(refreshToken.getToken());
            throw new BadCredentialsException("Refresh token expired. Login again.");
        }
    }


}
