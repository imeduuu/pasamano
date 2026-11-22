package cl.trato.identity.infrastructure.config;

import cl.trato.identity.application.ActivateSellerProfile;
import cl.trato.identity.application.ChangePassword;
import cl.trato.identity.application.GetProfile;
import cl.trato.identity.application.GetPublicSellerProfile;
import cl.trato.identity.application.Login;
import cl.trato.identity.application.Logout;
import cl.trato.identity.application.OneTimeTokenIssuer;
import cl.trato.identity.application.RefreshSession;
import cl.trato.identity.application.RegisterUser;
import cl.trato.identity.application.RequestPasswordReset;
import cl.trato.identity.application.ResendVerification;
import cl.trato.identity.application.ResetPassword;
import cl.trato.identity.application.SessionIssuer;
import cl.trato.identity.application.UpdateProfile;
import cl.trato.identity.application.VerifyEmail;
import cl.trato.identity.application.port.AccessTokenIssuer;
import cl.trato.identity.application.port.ActiveListingCount;
import cl.trato.identity.application.port.ConsentRepository;
import cl.trato.identity.application.port.OneTimeTokenRepository;
import cl.trato.identity.application.port.PasswordHasher;
import cl.trato.identity.application.port.RefreshTokenRepository;
import cl.trato.identity.application.port.SellerProfileRepository;
import cl.trato.identity.application.port.TokenGenerator;
import cl.trato.identity.application.port.UserRepository;
import cl.trato.identity.domain.LegalVersions;
import cl.trato.identity.domain.PasswordPolicy;
import cl.trato.shared.application.DomainEventPublisher;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Cableado de los casos de uso de identidad. Las clases de aplicación no conocen Spring (salvo
 * {@code @Transactional}); se registran aquí como beans.
 */
@Configuration
public class IdentityConfiguration {

  static final String COMMON_PASSWORDS = "security/common-passwords.txt";

  @Bean
  PasswordPolicy passwordPolicy() {
    return new PasswordPolicy(loadCommonPasswords(new ClassPathResource(COMMON_PASSWORDS)));
  }

  /** Lee la lista de contraseñas comunes ignorando líneas vacías y comentarios ({@code #}). */
  public static List<String> loadCommonPasswords(Resource resource) {
    try (InputStream in = resource.getInputStream();
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      return reader
          .lines()
          .map(String::strip)
          .filter(line -> !line.isEmpty() && !line.startsWith("#"))
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo leer " + COMMON_PASSWORDS, e);
    }
  }

  @Bean
  LegalVersions legalVersions(IdentityProperties properties) {
    return new LegalVersions(
        properties.legal().termsVersion(), properties.legal().privacyVersion());
  }

  @Bean
  OneTimeTokenIssuer oneTimeTokenIssuer(OneTimeTokenRepository tokens, TokenGenerator generator) {
    return new OneTimeTokenIssuer(tokens, generator);
  }

  @Bean
  SessionIssuer sessionIssuer(
      RefreshTokenRepository refreshTokens,
      TokenGenerator generator,
      AccessTokenIssuer accessTokens) {
    return new SessionIssuer(refreshTokens, generator, accessTokens);
  }

  @Bean
  RegisterUser registerUser(
      UserRepository users,
      ConsentRepository consents,
      PasswordPolicy policy,
      PasswordHasher hasher,
      OneTimeTokenIssuer tokenIssuer,
      DomainEventPublisher events,
      LegalVersions legalVersions,
      Clock clock) {
    return new RegisterUser(
        users, consents, policy, hasher, tokenIssuer, events, legalVersions, clock);
  }

  @Bean
  VerifyEmail verifyEmail(
      UserRepository users, OneTimeTokenRepository tokens, TokenGenerator generator, Clock clock) {
    return new VerifyEmail(users, tokens, generator, clock);
  }

  @Bean
  ResendVerification resendVerification(
      UserRepository users,
      OneTimeTokenIssuer tokenIssuer,
      DomainEventPublisher events,
      Clock clock) {
    return new ResendVerification(users, tokenIssuer, events, clock);
  }

  @Bean
  Login login(UserRepository users, PasswordHasher hasher, SessionIssuer sessions, Clock clock) {
    return new Login(users, hasher, sessions, clock);
  }

  @Bean
  RefreshSession refreshSession(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      TokenGenerator generator,
      AccessTokenIssuer accessTokens,
      Clock clock) {
    return new RefreshSession(users, refreshTokens, generator, accessTokens, clock);
  }

  @Bean
  Logout logout(RefreshTokenRepository refreshTokens, TokenGenerator generator, Clock clock) {
    return new Logout(refreshTokens, generator, clock);
  }

  @Bean
  RequestPasswordReset requestPasswordReset(
      UserRepository users,
      OneTimeTokenIssuer tokenIssuer,
      DomainEventPublisher events,
      Clock clock) {
    return new RequestPasswordReset(users, tokenIssuer, events, clock);
  }

  @Bean
  ResetPassword resetPassword(
      UserRepository users,
      OneTimeTokenRepository tokens,
      RefreshTokenRepository refreshTokens,
      PasswordPolicy policy,
      PasswordHasher hasher,
      TokenGenerator generator,
      Clock clock) {
    return new ResetPassword(users, tokens, refreshTokens, policy, hasher, generator, clock);
  }

  @Bean
  GetProfile getProfile(UserRepository users, SellerProfileRepository sellerProfiles) {
    return new GetProfile(users, sellerProfiles);
  }

  @Bean
  UpdateProfile updateProfile(
      UserRepository users, SellerProfileRepository sellerProfiles, Clock clock) {
    return new UpdateProfile(users, sellerProfiles, clock);
  }

  @Bean
  ActivateSellerProfile activateSellerProfile(
      UserRepository users,
      SellerProfileRepository sellerProfiles,
      IdentityProperties properties,
      Clock clock) {
    return new ActivateSellerProfile(
        users, sellerProfiles, clock, properties.legal().termsVersion());
  }

  @Bean
  GetPublicSellerProfile getPublicSellerProfile(
      SellerProfileRepository sellerProfiles, ActiveListingCount activeListingCount) {
    return new GetPublicSellerProfile(sellerProfiles, activeListingCount);
  }

  /** El catálogo aún no existe; este adaptador temporal informa cero publicaciones activas. */
  @Bean
  ActiveListingCount activeListingCount() {
    return sellerId -> 0;
  }

  @Bean
  ChangePassword changePassword(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      PasswordPolicy policy,
      PasswordHasher hasher,
      Clock clock) {
    return new ChangePassword(users, refreshTokens, policy, hasher, clock);
  }
}
