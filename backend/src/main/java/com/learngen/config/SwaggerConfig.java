package com.learngen.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / knife4j 配置。
 *
 * <p>对应 CLAUDE.md §21 评分项「界面美观、卡片化展示」与 §6 配套文档。
 *
 * <p>访问地址：{@code http://localhost:8080/doc.html}
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI learngenOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("学习多智能体系统 API")
                        .description("基于大模型的个性化资源生成与学习多智能体系统 - 后端接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("learngen-team")
                                .email("team@example.com")));
    }
}