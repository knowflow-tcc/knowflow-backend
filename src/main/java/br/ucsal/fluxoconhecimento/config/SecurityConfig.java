package br.ucsal.fluxoconhecimento.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.ucsal.fluxoconhecimento.dto.ErroDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Segurança da aplicação (RF01, RF12, RNF01, RNF10, RN01)
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMINISTRADOR = "ADMINISTRADOR";
    private static final String GESTOR = "GESTOR";
    private static final String COLABORADOR = "COLABORADOR";

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Público: tela de login, arquivos estáticos e páginas de erro
                .requestMatchers("/login.html", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error", "/error/**")
                    .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                // RF14 - recuperação de senha (a pessoa ainda não está logada)
                .requestMatchers("/recuperar-senha.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/recuperar-senha", "/api/auth/redefinir-senha").permitAll()
                // Documentação da API (Swagger); as chamadas feitas por ela continuam exigindo login
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // Administrador: usuários, departamentos, permissões e histórico geral
                .requestMatchers("/usuarios.html", "/departamentos.html", "/historico.html").hasRole(ADMINISTRADOR)
                .requestMatchers("/api/usuarios/**", "/api/historico/**").hasRole(ADMINISTRADOR)
                .requestMatchers(HttpMethod.POST, "/api/departamentos/**").hasRole(ADMINISTRADOR)
                .requestMatchers(HttpMethod.PUT, "/api/departamentos/**").hasRole(ADMINISTRADOR)

                // Gestor: organiza as categorias
                .requestMatchers("/categorias.html").hasRole(GESTOR)
                .requestMatchers(HttpMethod.POST, "/api/categorias/**").hasRole(GESTOR)
                .requestMatchers(HttpMethod.PUT, "/api/categorias/**").hasRole(GESTOR)

                // Gestor e administrador: painel de indicadores, relatórios e validação de soluções
                .requestMatchers("/painel.html", "/api/painel/**", "/api/relatorios/**").hasAnyRole(GESTOR, ADMINISTRADOR)
                .requestMatchers(HttpMethod.PATCH, "/api/solucoes/*/validar").hasAnyRole(GESTOR, ADMINISTRADOR)

                // Todos os perfis: favoritos (RF24) e as próprias notificações (RF21)
                .requestMatchers("/api/problemas/*/favorito", "/api/notificacoes/**").authenticated()

                // Colaborador e gestor: registro do conhecimento, comentários, avaliações e anexos
                .requestMatchers("/problema-form.html").hasAnyRole(COLABORADOR, GESTOR)
                .requestMatchers(HttpMethod.POST, "/api/problemas/**", "/api/solucoes/**").hasAnyRole(COLABORADOR, GESTOR)
                .requestMatchers(HttpMethod.PUT, "/api/problemas/**", "/api/causas/**", "/api/solucoes/**")
                    .hasAnyRole(COLABORADOR, GESTOR)
                .requestMatchers(HttpMethod.PATCH, "/api/problemas/**", "/api/causas/**", "/api/solucoes/*/arquivar",
                        "/api/comentarios/**", "/api/anexos/**")
                    .hasAnyRole(COLABORADOR, GESTOR)

                // Todo o resto (busca, detalhes, listas, download de anexos): qualquer usuário logado
                .anyRequest().authenticated())
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .exceptionHandling(e -> e
                // API: responde JSON 401; telas: redireciona para o login
                .defaultAuthenticationEntryPointFor(naoAutenticadoApi(), new AntPathRequestMatcher("/api/**"))
                .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login.html"),
                        AnyRequestMatcher.INSTANCE)
                .accessDeniedHandler(acessoNegado()))
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.NO_CONTENT.value()))
                .deleteCookies("JSESSIONID"))
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable());
        return http.build();
    }

    // RNF01 - senhas guardadas apenas como hash BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provedor = new DaoAuthenticationProvider();
        provedor.setUserDetailsService(userDetailsService);
        provedor.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provedor);
    }

    // Onde o login guarda o usuário autenticado: na sessão HTTP
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    private AuthenticationEntryPoint naoAutenticadoApi() {
        return (request, response, ex) -> escreverErro(response, HttpStatus.UNAUTHORIZED,
                "Sua sessão expirou ou você não está logado. Entre novamente.");
    }

    private AccessDeniedHandler acessoNegado() {
        return (request, response, ex) -> {
            if (!ehApi(request)) {
                response.sendError(HttpStatus.FORBIDDEN.value());
                return;
            }
            String mensagem = ex instanceof CsrfException
                    ? "A proteção de segurança da página expirou. Recarregue a página e tente de novo."
                    : "O seu perfil não tem acesso a esta funcionalidade.";
            escreverErro(response, HttpStatus.FORBIDDEN, mensagem);
        };
    }

    private static boolean ehApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    private void escreverErro(HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), new ErroDTO(status.value(), mensagem));
    }
}
