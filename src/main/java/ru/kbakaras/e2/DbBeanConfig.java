package ru.kbakaras.e2;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import ru.kbakaras.jpa.repository.SugarRepositoryExe;

@Configuration
@EnableJpaRepositories(
        value = "ru.kbakaras.e2.repository",
        repositoryBaseClass = SugarRepositoryExe.class
)
@EntityScan(basePackages = {
        "ru.kbakaras.e2.model",
        "ru.glance.agr.model"
})
class DbBeanConfig {}