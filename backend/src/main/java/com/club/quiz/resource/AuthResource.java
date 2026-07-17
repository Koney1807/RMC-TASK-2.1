package com.club.quiz.resource;

import com.club.quiz.config.RateLimiter;
import com.club.quiz.dto.AuthResponse;
import com.club.quiz.dto.ForgotPasswordRequest;
import com.club.quiz.dto.LoginRequest;
import com.club.quiz.dto.ResetPasswordRequest;
import com.club.quiz.dto.SignupRequest;
import com.club.quiz.model.User;
import com.mongodb.MongoWriteException;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.smallrye.jwt.build.Jwt;
import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

// ---- Real account signup/login, replacing the old name-entry box. ----
// Anyone can sign up; the account's username becomes the name that shows up
// on the leaderboard, so scores can't be spoofed under someone else's name.
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    private static final int LOGIN_MAX_ATTEMPTS = 10;
    private static final int SIGNUP_MAX_ATTEMPTS = 20;
    private static final long WINDOW_MILLIS = 60_000;

    private static final int RESET_MAX_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Inject
    RateLimiter rateLimiter;

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "app.frontend-url")
    String frontendUrl;

    @ConfigProperty(name = "app.reset-token-minutes")
    int resetTokenMinutes;

    @Context
    HttpServerRequest httpRequest;

    private String clientIp() {
        return httpRequest != null && httpRequest.remoteAddress() != null
                ? httpRequest.remoteAddress().host()
                : "unknown";
    }

    private Response tooManyRequests() {
        return Response.status(429)
                .entity(Map.of("message", "Too many attempts. Please wait a minute and try again."))
                .build();
    }

    @POST
    @Path("/signup")
    public Response signup(@Valid SignupRequest request) {
        if (rateLimiter.tooManyAttempts("signup:" + clientIp(), SIGNUP_MAX_ATTEMPTS, WINDOW_MILLIS)) {
            return tooManyRequests();
        }

        String username = request.username.trim().toLowerCase();

        if (User.usernameTaken(username)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("message", "That username is already taken.")).build();
        }

        User user = new User();
        user.username = username;
        user.email = request.email.trim();
        user.passwordHash = BcryptUtil.bcryptHash(request.password);

        try {
            user.persist();
        } catch (MongoWriteException e) {
            // usernameTaken() above is a check-then-act: two signups for the
            // same username (or, here, the same leaderboard display name)
            // can race between the check and the write. The unique index
            // from MongoIndexInitializer is the real guarantee; translate a
            // duplicate-key error into the same 409 the pre-check returns.
            if (e.getError().getCode() == 11000) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(Map.of("message", "That username is already taken.")).build();
            }
            LOG.error("Unexpected error persisting new user", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("message", "Could not create your account. Please try again.")).build();
        }

        return Response.status(Response.Status.CREATED)
                .entity(new AuthResponse(issueToken(username), username)).build();
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest request) {
        String username = request.username.trim().toLowerCase();
        if (rateLimiter.tooManyAttempts("login:" + clientIp() + ":" + username, LOGIN_MAX_ATTEMPTS, WINDOW_MILLIS)) {
            return tooManyRequests();
        }

        User user = User.findByUsername(username);

        if (user == null || !BcryptUtil.matches(request.password, user.passwordHash)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Invalid username or password.")).build();
        }

        return Response.ok(new AuthResponse(issueToken(username), username)).build();
    }

    @POST
    @Path("/forgot-password")
    public Response forgotPassword(@Valid ForgotPasswordRequest request) {
        if (rateLimiter.tooManyAttempts("forgot-password:" + clientIp(), RESET_MAX_ATTEMPTS, WINDOW_MILLIS)) {
            return tooManyRequests();
        }

        // Always return the same generic message whether or not the email
        // matches an account - confirming/denying an email's existence here
        // would let anyone enumerate registered users.
        Response genericResponse = Response.ok(Map.of("message",
                "If an account with that email exists, we've sent a password reset link.")).build();

        User user = User.findByEmail(request.email.trim());
        if (user == null) {
            return genericResponse;
        }

        String rawToken = generateRawToken();
        user.resetTokenHash = hashToken(rawToken);
        user.resetTokenExpiry = Instant.now().plusSeconds(resetTokenMinutes * 60L);
        user.update();

        String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
        try {
            mailer.send(Mail.withText(user.email,
                    "Reset your password",
                    "We received a request to reset your password.\n\n"
                            + "Reset it here (valid for " + resetTokenMinutes + " minutes):\n"
                            + resetLink + "\n\n"
                            + "If you didn't request this, you can safely ignore this email."));
        } catch (Exception e) {
            // Don't leak mailer failures to the client - same generic
            // response either way - but do log it so it's visible to us.
            LOG.error("Failed to send password reset email", e);
        }

        return genericResponse;
    }

    @POST
    @Path("/reset-password")
    public Response resetPassword(@Valid ResetPasswordRequest request) {
        if (rateLimiter.tooManyAttempts("reset-password:" + clientIp(), RESET_MAX_ATTEMPTS, WINDOW_MILLIS)) {
            return tooManyRequests();
        }

        User user = User.findByResetTokenHash(hashToken(request.token));
        if (user == null || user.resetTokenExpiry == null || user.resetTokenExpiry.isBefore(Instant.now())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "This reset link is invalid or has expired. Please request a new one."))
                    .build();
        }

        user.passwordHash = BcryptUtil.bcryptHash(request.newPassword);
        // Single-use: clear the token so the same link can't be replayed.
        user.resetTokenHash = null;
        user.resetTokenExpiry = null;
        user.update();

        return Response.ok(Map.of("message", "Your password has been reset. You can now log in.")).build();
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on every JVM.
            throw new IllegalStateException(e);
        }
    }

    private String issueToken(String username) {
        return Jwt.issuer("quiz-leaderboard-app")
                .upn(username)
                .groups(Set.of("user"))
                .expiresIn(java.time.Duration.ofHours(12))
                .sign();
    }
}
