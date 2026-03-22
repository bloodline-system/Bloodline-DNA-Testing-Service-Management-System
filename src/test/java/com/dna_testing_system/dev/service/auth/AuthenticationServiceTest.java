package com.dna_testing_system.dev.service.auth;

import com.dna_testing_system.dev.dto.request.auth.AuthenticationRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.RefreshTokenRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.RegisterRequestDTO;
import com.dna_testing_system.dev.dto.request.auth.VerificationOptRequestDTO;
import com.dna_testing_system.dev.dto.response.auth.AuthTokensResponseDTO;
import com.dna_testing_system.dev.dto.response.auth.RegisterResponseDTO;
import com.dna_testing_system.dev.entity.SignUp;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.SignUpStatus;
import com.dna_testing_system.dev.exception.BlacklistedTokenException;
import com.dna_testing_system.dev.exception.InvalidTokenException;
import com.dna_testing_system.dev.exception.LoginNotValidException;
import com.dna_testing_system.dev.exception.OptFailException;
import com.dna_testing_system.dev.exception.SignUpNotValidException;
import com.dna_testing_system.dev.mapper.AuthMapper;
import com.dna_testing_system.dev.properties.JwtProperties;
import com.dna_testing_system.dev.repository.SignUpRepository;
import com.dna_testing_system.dev.service.auth.impl.AuthenticationServiceImpl;
import com.dna_testing_system.dev.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthMapper authMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SignUpRepository signUpRepository;
    @Mock
    private UserService userService;
    @Mock
    private JwtKeyService jwtKeyService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Test
    void loginReturnsTokensWhenCredentialsAreValid() {
        //give
        AuthenticationRequestDTO request = AuthenticationRequestDTO.builder()
                .username("alice")
                .password("password123")
                .build();
        User user = User.builder().id("u1").username("alice").passwordHash("hashed").build();
        AuthTokensResponseDTO mapped = AuthTokensResponseDTO.builder().userId("u1").build();

        when(userService.getUserByUserName("alice")).thenReturn(user);
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(authMapper.toAuthTokensResponseDTO(user)).thenReturn(mapped);
        when(jwtProperties.getTokenExp()).thenReturn(Duration.ofMinutes(15));
        when(jwtProperties.getRefreshTokenExp()).thenReturn(Duration.ofDays(1));
        when(jwtKeyService.generateToken(eq(user), any(Duration.class))).thenReturn("access-token");
        when(jwtKeyService.generateRefreshToken(eq(user), any(Duration.class))).thenReturn("refresh-token");

        //when
        AuthTokensResponseDTO result = authenticationService.login(request);

        //then
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals("u1", result.getUserId());
        assertNotNull(result.getExpiresIn());
    }

    @Test
    void loginThrowsWhenPasswordDoesNotMatch() {
        AuthenticationRequestDTO request = AuthenticationRequestDTO.builder()
                .username("alice")
                .password("wrong")
                .build();
        User user = User.builder().username("alice").passwordHash("hashed").build();

        when(userService.getUserByUserName("alice")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(LoginNotValidException.class, () -> authenticationService.login(request));
    }

    @Test
    void logoutThrowsWhenAccessTokenIsBlank() {
        assertThrows(InvalidTokenException.class, () -> authenticationService.logout(" ", "refresh"));
    }

    @Test
    void logoutThrowsWhenAccessTokenIsNull() {
        assertThrows(InvalidTokenException.class, () -> authenticationService.logout(null, "refresh"));
    }

    @Test
    void logoutThrowsWhenRefreshTokenIsBlank() {
        assertThrows(InvalidTokenException.class, () -> authenticationService.logout("access", " "));
    }

    @Test
    void logoutThrowsWhenRefreshTokenIsNull() {
        assertThrows(InvalidTokenException.class, () -> authenticationService.logout("access", null));
    }

    @Test
    void logoutBlacklistsBothTokens() {
        authenticationService.logout("access", "refresh");

        verify(tokenBlacklistService).blacklistAccessToken("access");
        verify(tokenBlacklistService).blacklistRefreshToken("refresh");
    }

    @Test
    void refreshTokenThrowsWhenTokenMissing() {
        assertThrows(InvalidTokenException.class,
                () -> authenticationService.refreshToken(new RefreshTokenRequestDTO(" ")));
    }

    @Test
    void refreshTokenThrowsWhenTokenIsNull() {
        assertThrows(InvalidTokenException.class,
                () -> authenticationService.refreshToken(new RefreshTokenRequestDTO(null)));
    }

    @Test
    void refreshTokenThrowsWhenTokenIsInvalid() {
        when(jwtKeyService.validateToken("bad-token")).thenReturn(false);

        assertThrows(InvalidTokenException.class,
                () -> authenticationService.refreshToken(new RefreshTokenRequestDTO("bad-token")));
    }

    @Test
    void refreshTokenThrowsWhenTokenIsBlacklisted() {
        when(jwtKeyService.validateToken("refresh")).thenReturn(true);
        when(tokenBlacklistService.isRefreshTokenBlacklisted("refresh")).thenReturn(true);

        assertThrows(BlacklistedTokenException.class,
                () -> authenticationService.refreshToken(new RefreshTokenRequestDTO("refresh")));
    }

    @Test
    void refreshTokenRotatesRefreshTokenOnSuccess() {
        User user = User.builder().id("u1").username("alice").build();
        AuthTokensResponseDTO mapped = AuthTokensResponseDTO.builder().userId("u1").build();

        when(jwtKeyService.validateToken("refresh-old")).thenReturn(true);
        when(tokenBlacklistService.isRefreshTokenBlacklisted("refresh-old")).thenReturn(false);
        when(jwtKeyService.extractUsername("refresh-old")).thenReturn("alice");
        when(userService.getUserByUserName("alice")).thenReturn(user);
        when(authMapper.toAuthTokensResponseDTO(user)).thenReturn(mapped);
        when(jwtProperties.getTokenExp()).thenReturn(Duration.ofMinutes(15));
        when(jwtProperties.getRefreshTokenExp()).thenReturn(Duration.ofDays(1));
        when(jwtKeyService.generateToken(eq(user), any(Duration.class))).thenReturn("new-access");
        when(jwtKeyService.generateRefreshToken(eq(user), any(Duration.class))).thenReturn("new-refresh");

        AuthTokensResponseDTO result = authenticationService.refreshToken(new RefreshTokenRequestDTO("refresh-old"));

        verify(tokenBlacklistService).blacklistRefreshToken("refresh-old");
        assertEquals("new-access", result.getAccessToken());
        assertEquals("new-refresh", result.getRefreshToken());
    }

    @Test
    void refreshTokenThrowsWhenUserResolvedFromTokenIsNull() {
        when(jwtKeyService.validateToken("refresh-old")).thenReturn(true);
        when(tokenBlacklistService.isRefreshTokenBlacklisted("refresh-old")).thenReturn(false);
        when(jwtKeyService.extractUsername("refresh-old")).thenReturn("ghost");
        when(userService.getUserByUserName("ghost")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> authenticationService.refreshToken(new RefreshTokenRequestDTO("refresh-old")));
    }

    @Test
    void registerThrowsWhenUsernameAlreadyExists() {
        RegisterRequestDTO request = registerRequest();
        when(signUpRepository.existsByUsernameAndStatusIn(eq(request.getUsername()), anyList())).thenReturn(true);

        assertThrows(SignUpNotValidException.class, () -> authenticationService.register(request));
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        RegisterRequestDTO request = registerRequest();
        when(signUpRepository.existsByUsernameAndStatusIn(eq(request.getUsername()), anyList())).thenReturn(false);
        when(signUpRepository.existsByEmailAndStatusIn(eq(request.getEmail()), anyList())).thenReturn(true);

        assertThrows(SignUpNotValidException.class, () -> authenticationService.register(request));
    }

    @Test
    void registerCreatesPendingSignupWithEncodedPasswordAndOtp() {
        RegisterRequestDTO request = registerRequest();
        SignUp mappedSignUp = SignUp.builder().username("alice").email("alice@example.com").build();
        RegisterResponseDTO responseDTO = RegisterResponseDTO.builder().signUpId("signup-1").build();

        when(signUpRepository.existsByUsernameAndStatusIn(eq(request.getUsername()), anyList())).thenReturn(false);
        when(signUpRepository.existsByEmailAndStatusIn(eq(request.getEmail()), anyList())).thenReturn(false);
        when(authMapper.toSignUp(request)).thenReturn(mappedSignUp);
        when(Objects.requireNonNull(passwordEncoder.encode("password123"))).thenReturn("encoded-password");
        when(signUpRepository.save(mappedSignUp)).thenReturn(mappedSignUp);
        when(authMapper.toRegisterResponseDTO(mappedSignUp)).thenReturn(responseDTO);

        RegisterResponseDTO result = authenticationService.register(request);

        assertEquals(SignUpStatus.PENDING, mappedSignUp.getStatus());
        assertEquals("encoded-password", mappedSignUp.getPassword());
        assertNotNull(mappedSignUp.getCurrentVerificationToken());
        assertNotNull(mappedSignUp.getExpiredVerificationTokenDate());
        assertNotNull(mappedSignUp.getLastVerificationTokenSentAt());
        assertEquals(0, mappedSignUp.getCountAttemptVerificationToken());
        assertEquals("signup-1", result.signUpId());
    }

    @Test
    void verifyUserRegistrationThrowsWhenSignupNotFound() {
        when(signUpRepository.findByIdAndStatus("missing", SignUpStatus.PENDING)).thenReturn(Optional.empty());

        assertThrows(OptFailException.class,
                () -> authenticationService.verifyUserRegistration(new VerificationOptRequestDTO("missing", "123456")));
    }

    @Test
    void verifyUserRegistrationThrowsWhenOtpMismatches() {
        SignUp signUp = pendingSignup("s1", "654321", LocalDateTime.now().plusMinutes(2));
        when(signUpRepository.findByIdAndStatus("s1", SignUpStatus.PENDING)).thenReturn(Optional.of(signUp));

        assertThrows(OptFailException.class,
                () -> authenticationService.verifyUserRegistration(new VerificationOptRequestDTO("s1", "111111")));
    }

    @Test
    void verifyUserRegistrationThrowsWhenOtpExpired() {
        SignUp signUp = pendingSignup("s1", "123456", LocalDateTime.now().minusSeconds(1));
        when(signUpRepository.findByIdAndStatus("s1", SignUpStatus.PENDING)).thenReturn(Optional.of(signUp));

        assertThrows(OptFailException.class,
                () -> authenticationService.verifyUserRegistration(new VerificationOptRequestDTO("s1", "123456")));
    }

    @Test
    void verifyUserRegistrationMarksUserAsVerifiedAndCreatesUser() {
        SignUp signUp = pendingSignup("s1", "123456", LocalDateTime.now().plusMinutes(2));
        when(signUpRepository.findByIdAndStatus("s1", SignUpStatus.PENDING)).thenReturn(Optional.of(signUp));

        authenticationService.verifyUserRegistration(new VerificationOptRequestDTO("s1", "123456"));

        assertEquals(SignUpStatus.VERIFIED, signUp.getStatus());
        verify(signUpRepository).save(signUp);
        verify(userService).createUser(signUp);
    }

    @Test
    void resendUserVerificationOtpThrowsWhenSignupNotFound() {
        when(signUpRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(OptFailException.class, () -> authenticationService.resendUserVerificationOTP("missing"));
    }

    @Test
    void resendUserVerificationOtpThrowsWhenStatusIsNotPending() {
        SignUp signUp = SignUp.builder().id("s1").status(SignUpStatus.VERIFIED).build();
        when(signUpRepository.findById("s1")).thenReturn(Optional.of(signUp));

        assertThrows(OptFailException.class, () -> authenticationService.resendUserVerificationOTP("s1"));
    }

    @Test
    void resendUserVerificationOtpThrowsWhenResendIntervalNotElapsed() {
        SignUp signUp = SignUp.builder()
                .id("s1")
                .username("alice")
                .status(SignUpStatus.PENDING)
                .countAttemptVerificationToken(0)
                .lastVerificationTokenSentAt(LocalDateTime.now())
                .build();
        when(signUpRepository.findById("s1")).thenReturn(Optional.of(signUp));

        assertThrows(OptFailException.class, () -> authenticationService.resendUserVerificationOTP("s1"));
    }

    @Test
    void resendUserVerificationOtpThrowsWhenAttemptLimitReached() {
        SignUp signUp = SignUp.builder()
                .id("s1")
                .username("alice")
                .status(SignUpStatus.PENDING)
                .countAttemptVerificationToken(5)
                .lastVerificationTokenSentAt(LocalDateTime.now().minusMinutes(2))
                .build();
        when(signUpRepository.findById("s1")).thenReturn(Optional.of(signUp));

        assertThrows(OptFailException.class, () -> authenticationService.resendUserVerificationOTP("s1"));
    }

    @Test
    void resendUserVerificationOtpAssignsNewOtpAndIncrementsAttempt() {
        SignUp signUp = SignUp.builder()
                .id("s1")
                .username("alice")
                .status(SignUpStatus.PENDING)
                .countAttemptVerificationToken(0)
                .lastVerificationTokenSentAt(LocalDateTime.now().minusMinutes(2))
                .build();
        when(signUpRepository.findById("s1")).thenReturn(Optional.of(signUp));

        authenticationService.resendUserVerificationOTP("s1");

        assertEquals(1, signUp.getCountAttemptVerificationToken());
        assertNotNull(signUp.getCurrentVerificationToken());
        assertNotNull(signUp.getExpiredVerificationTokenDate());
        assertNotNull(signUp.getLastVerificationTokenSentAt());
        verify(signUpRepository).save(signUp);
    }

    @Test
    void getOtpForDebuggingThrowsWhenSignupNotFound() {
        when(signUpRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(OptFailException.class, () -> authenticationService.getOtpForDebugging("missing"));
    }

    @Test
    void getOtpForDebuggingThrowsWhenSignupNotPending() {
        SignUp signUp = SignUp.builder().id("s1").status(SignUpStatus.VERIFIED).build();
        when(signUpRepository.findById("s1")).thenReturn(Optional.of(signUp));

        assertThrows(OptFailException.class, () -> authenticationService.getOtpForDebugging("s1"));
    }

    @Test
    void getOtpForDebuggingReturnsCurrentOtpForPendingSignup() {
        SignUp signUp = SignUp.builder()
                .id("s1")
                .status(SignUpStatus.PENDING)
                .currentVerificationToken("456789")
                .build();
        when(signUpRepository.findById("s1")).thenReturn(Optional.of(signUp));

        assertEquals("456789", authenticationService.getOtpForDebugging("s1").otpCode());
    }

    private RegisterRequestDTO registerRequest() {
        return RegisterRequestDTO.builder()
                .username("alice")
                .password("password123")
                .email("alice@example.com")
                .firstName("Alice")
                .lastName("Walker")
                .build();
    }

    private SignUp pendingSignup(String signUpId, String otp, LocalDateTime expiration) {
        return SignUp.builder()
                .id(signUpId)
                .status(SignUpStatus.PENDING)
                .currentVerificationToken(otp)
                .expiredVerificationTokenDate(expiration)
                .build();
    }
}