package ru.kbakaras.e2.service;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.kbakaras.e2.repository.Queue4DeliveryRepository;

public class Manager4Delivery implements InitializingBean, DisposableBean {

    private Queue4DeliveryRepository queue4DeliveryRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    @Override
    public void destroy() throws Exception {

    }

}