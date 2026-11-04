package team.projectpulse.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import team.projectpulse.user.UserRepository;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// FR-SEC-active-account: a bearer token is accepted only while its account exists and is active
@ExtendWith(MockitoExtension.class)
class ActiveAccountJwtValidatorTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ActiveAccountJwtValidator validator;

    private static Jwt tokenFor(Object userId) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .subject("someone@abc.edu");
        if (userId != null) {
            builder.claim("userId", userId);
        }
        return builder.build();
    }

    @Test
    void testActiveAccountPasses() {
        given(this.userRepository.existsByIdAndEnabledTrue(4)).willReturn(true);

        OAuth2TokenValidatorResult result = this.validator.validate(tokenFor(4));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void testDeactivatedOrMissingAccountFails() {
        given(this.userRepository.existsByIdAndEnabledTrue(4)).willReturn(false);

        OAuth2TokenValidatorResult result = this.validator.validate(tokenFor(4));

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).allMatch(error -> error.getErrorCode().equals(OAuth2ErrorCodes.INVALID_TOKEN));
    }

    @Test
    void testTokenWithoutUserIdFails() {
        OAuth2TokenValidatorResult result = this.validator.validate(tokenFor(null));

        assertThat(result.hasErrors()).isTrue();
        verify(this.userRepository, never()).existsByIdAndEnabledTrue(any());
    }

    @Test
    void testLongUserIdClaimIsRead() {
        given(this.userRepository.existsByIdAndEnabledTrue(4)).willReturn(true);

        OAuth2TokenValidatorResult result = this.validator.validate(tokenFor(4L));

        assertThat(result.hasErrors()).isFalse();
    }

}
