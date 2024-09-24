package Fridge_Chef.team.config.local;

import Fridge_Chef.team.security.CustomJwtAuthenticationConverter;
import Fridge_Chef.team.security.service.CustomOAuth2UserService;
import Fridge_Chef.team.user.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;

@Profile("local")
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityLocalConfig {
    private static final KeyPair keyPair = generateKeyPair();
    private static final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(this::configureHeaders)
                .authorizeHttpRequests(this::configureAuthorization)
                .logout(logoutConfigurer -> logoutConfigurer.logoutSuccessUrl("/"))
                .oauth2ResourceServer(this::configureJwt)
                .oauth2Login(this::configureOAuth2Login)
                .build();
    }

    private void configureOAuth2Login(OAuth2LoginConfigurer<HttpSecurity> oauth2LoginConfigurer) {
        oauth2LoginConfigurer.userInfoEndpoint(endpointCustomizer -> endpointCustomizer.userService(customOAuth2UserService));
    }
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    private void configureAuthorization(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                .requestMatchers(HttpMethod.GET, "/", "/css/**", "/img/**", "/js/**", "/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/user").hasRole(Role.USER.name())
                .requestMatchers(
                        "/docs.html", "/favicon.ico", "/api/auth/**", "/api/cert/email/**",
                        "/api/email/**", "/api/user/signup", "/api/user/login",
                        "/api/ingredients/**", "/api/fridge/ingredients", "/api/recipes/", "/api/recipes/{id}",
                        "/api/categorys", "/api/categorys/boards/**", "/api/recipes/{recipe_id}/comments",
                        "/api/categorys/{category_id}/boards/{board_id}/comments"

                ).permitAll()
                .requestMatchers(
                        "/api/user", "/api/user/account", "/api/user/password",
                        "/api/recipes/book", "/api/categorys/{category_id}/board", "/api/recipes/{recipe_id}/comment",
                        "/api/categorys/{category_id}/boards/{board_id}/comment"
                )
                .hasAnyAuthority(Role.USER.getAuthority(), Role.ADMIN.getAuthority())
                .requestMatchers("/api/manager/busines/ingredient")
                .hasAnyAuthority(Role.ADMIN.getAuthority())
                .requestMatchers(PathRequest.toH2Console()).permitAll()
                .anyRequest().authenticated();
    }

    private void configureJwt(OAuth2ResourceServerConfigurer<HttpSecurity> configurer) {
        configurer.jwt(jwt -> {
            jwt.decoder(jwtDecoder());
            jwt.jwtAuthenticationConverter(new CustomJwtAuthenticationConverter());
        });
    }

    private void configureHeaders(HeadersConfigurer<HttpSecurity> headers) {
        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key pair", e);
        }
    }

}