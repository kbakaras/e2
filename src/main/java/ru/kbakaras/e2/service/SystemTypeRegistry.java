package ru.kbakaras.e2.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import ru.kbakaras.e2.model.SystemType;
import ru.kbakaras.e2.repositories.SystemTypeRepository;

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.HashMap;
import java.util.Map;

@Service
public class SystemTypeRegistry implements InitializingBean {
    private final static Logger LOG = LoggerFactory.getLogger(SystemTypeRegistry.class);

    @PersistenceUnit
    private EntityManagerFactory emf;
    @Resource
    private SystemTypeRepository repository;

    private Map<Class<? extends SystemType>, SystemType> map = new HashMap<>();

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public void afterPropertiesSet() {
        /*emf.getMetamodel().getEntities().forEach(entityType -> {
            Class<?> clazz = entityType.getJavaType();
            if (SystemType.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                DiscriminatorValue dv = AnnotationUtils.findAnnotation(clazz, DiscriminatorValue.class);
                if (dv != null) {
                    UUID id = UUID.fromString(dv.value());

                    SystemType systemType = repository.findById(id).orElse(null);
                    if (systemType == null) {
                        try {
                            systemType = (SystemType) clazz.newInstance();
                            systemType.setId(id);
                            systemType.setName(clazz.getSimpleName());
                            systemType = repository.save(systemType);
                            LOG.info("Created SystemType database element: {}", systemType);
                        } catch (InstantiationException | IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    map.put((Class<SystemType>) clazz, systemType);
                } else {
                    LOG.warn("Discriminator value not found for SystemType: {}. SYSTEM TYPE IGNORED!", clazz.getName());
                }
            }
        });*/
    }

    @SuppressWarnings("unchecked")
    public <T extends SystemType> T get1(Class<T> clazz) {
        return (T) map.get(clazz);
    }
}