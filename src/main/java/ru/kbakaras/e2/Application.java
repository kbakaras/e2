package ru.kbakaras.e2;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

@EnableWebMvc
@EnableSwagger2
@SpringBootApplication
//@SpringBootApplication(scanBasePackages = "ru.glance.agr.rest, ru.glance.agr.service, ru.kbakaras.e2.service")
//@Import(DbBeanConfig.class)
public class Application implements WebMvcConfigurer, ApplicationContextAware {
    public static BeanFactory factory;

    /**
     * Конвертор объявлен бином в явном виде для того, чтобы при создании RestTemplate он добавлялся
     * в список стандартных конверторов. Этот же бин добавляется в список конверторов контекста WebMVC.
     */
    @Bean
    public Element4jHttpMessageConverter element4jHttpMessageConverter() {
        return new Element4jHttpMessageConverter();
    }


    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(element4jHttpMessageConverter());
    }



    @Bean
    public Docket apiDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        factory = applicationContext;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/swagger/**")
                .addResourceLocations("classpath:/swagger/");
    }


    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        //application.addListeners((ApplicationListener<ApplicationStartedEvent>) event -> Application.factory = event.getApplicationContext());
        application.run(args);
	}
}