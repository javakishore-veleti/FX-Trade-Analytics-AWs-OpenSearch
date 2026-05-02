package com.jk.fx.trade_mgmt.masterdata.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI masterDataOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FX Master Data Service API")
                        .description("Master data CRUD for currencies, currency pairs, and trade books.")
                        .version("v1")
                        .contact(new Contact().name("FX Trade Analytics"))
                        .license(new License().name("Apache 2.0")));
    }
}
