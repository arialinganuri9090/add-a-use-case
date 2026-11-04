package team.projectpulse.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import team.projectpulse.user.UserRepository;

/**
 * Refuses a bearer token whose account has been deactivated or no longer exists (FR-SEC-active-account).
 *
 * <p>A token is self-contained and lives for 2 hours, so the login-time check alone would let a student deactivated
 * mid-session keep her access until the token expired. This validator runs on every authenticated request, after the
 * default signature and expiry checks, and costs one primary-key lookup. A failure takes the standard
 * invalid-bearer-token path to {@code 401}, which the SPA turns into a return to login. It applies to every account,
 * instructors included, since the FR covers both.
 */
@Component
public class ActiveAccountJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INACTIVE_ACCOUNT = new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN, "The account is deactivated or no longer exists.", null);

    private final UserRepository userRepository;


    public ActiveAccountJwtValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Integer userId = readUserId(jwt.getClaim("userId"));
        if (userId != null && this.userRepository.existsByIdAndEnabledTrue(userId)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(INACTIVE_ACCOUNT);
    }

    private static Integer readUserId(Object claim) {
        if (claim instanceof Integer i) return i;
        if (claim instanceof Long l) return l.intValue();
        if (claim instanceof String s) {
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

}
