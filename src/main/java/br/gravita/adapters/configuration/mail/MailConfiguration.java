package br.gravita.adapters.configuration.mail;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.IOException;

@Configuration
public class MailConfiguration {
    @Primary
    @Bean
    public FreeMarkerConfigurer freemarkerConfigurer() throws IOException {
        final var configurer = new FreeMarkerConfigurer();
        configurer.setTemplateLoaderPath("classpath:/mails/templates/");
        return configurer;
    }

    @Bean
    public freemarker.template.Configuration freemarkerConfiguration(final FreeMarkerConfigurer freemarkerConfigurer) {
        return freemarkerConfigurer.getConfiguration();
    }
}
