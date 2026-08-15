package ipn.escom.defensoria.catalogo_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI catalogoServiceOpenAPI() {
        final String esquemaJwt = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Catálogo Service — Defensoría de los Derechos Politécnicos")
                        .description("Catálogos institucionales (dependencias del IPN y futuros catálogos "
                                + "compartidos) para la Plataforma de Quejas.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(esquemaJwt))
                .components(new Components()
                        .addSecuritySchemes(esquemaJwt, new SecurityScheme()
                                .name(esquemaJwt)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
