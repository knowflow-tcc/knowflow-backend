package br.ucsal.fluxoconhecimento.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

// Documentação da API REST (Swagger UI em /swagger-ui.html)
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI documentacaoApi() {
        return new OpenAPI().info(new Info()
                .title("KnowFlow - Fluxo do Conhecimento")
                .version("1.0.0")
                .description("API REST da plataforma de gestão do conhecimento empresarial (TCC - ADS UCSAL). "
                        + "Para testar: entre no sistema em /login.html e volte para esta página; "
                        + "a sessão e o token CSRF são reaproveitados automaticamente.")
                .contact(new Contact().name("Danilo, Ícaro, João Breno e Lucas")));
    }
}
