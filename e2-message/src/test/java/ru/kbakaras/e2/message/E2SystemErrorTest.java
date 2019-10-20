package ru.kbakaras.e2.message;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

class E2SystemErrorTest {

    @Test
    void createTest() {

        E2Response response = new E2Response(E2Request.REQUEST_TYPE_ELEMENT);
        response.addSystemResponse(
                "4f7ef2ff-5f7b-48f0-89e3-f91d1169a467",
                "Test"
        ).addSystemError().setText("Test Error");


        try (InputStream is = this.getClass().getResourceAsStream("/errorResponse.xml")) {

            Document target = new SAXReader().read(is);

            Assertions.assertEquals(response.xml().asXML(), target.getRootElement().asXML());

        } catch (IOException | DocumentException e) {
            throw new RuntimeException(e);
        }

    }

}