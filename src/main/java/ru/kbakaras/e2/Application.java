package ru.kbakaras.e2;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableWebMvc
@SpringBootApplication
//@SpringBootApplication(scanBasePackages = "ru.glance.agr.rest, ru.glance.agr.service, ru.kbakaras.e2.service")
//@Import(DbBeanConfig.class)
public class Application implements WebMvcConfigurer, ApplicationContextAware {
    public static BeanFactory factory;

    /**
     * Конвертор объявлен бином в явном виде для того, чтобы при создании RestTemplate он добавлялся
     * в список стандартный конверторов. Этот же бин добавляется в список конверторов контекста WebMVC.
     */
    /*@Bean
    public Element4jHttpMessageConverter element4jHttpMessageConverter() {
        return new Element4jHttpMessageConverter();
    }

    @Bean
    public ConversionRegistry conversionRegistry() {
        return new ConversionRegistry("ru.glance.agr.conversion");
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(element4jHttpMessageConverter());
    }*/

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        //application.addListeners((ApplicationListener<ApplicationStartedEvent>) event -> Application.factory = event.getApplicationContext());
        application.run(args);
	}

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        factory = applicationContext;
    }
}