package ru.kbakaras.e2.testing;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import ru.kbakaras.e2.core.conversion.Conversion;
import ru.kbakaras.e2.core.conversion.Conversions;
import ru.kbakaras.e2.core.conversion.Converter4Payload;
import ru.kbakaras.e2.core.conversion.PayloadConversionBind;
import ru.kbakaras.e2.message.E2Entity;
import ru.kbakaras.e2.message.E2Update;
import ru.kbakaras.sugar.spring.PackageResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConversionTest {

    private static final Logger log = LoggerFactory.getLogger(ConversionTest.class);

    private Conversions conversions;
    private E2Update input;
    private String expected;

    private UidReaderWriter uidReaderWriter;

    private class UidReaderWriter {

        private Path path;
        private boolean read;

        private List<String> list;
        private int index = 0;


        UidReaderWriter(Path path) {

            this.path = path;

            try {

                list = Files.readAllLines(path);
                read = !list.isEmpty();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        public UUID get() {

            if (read) {

                if (index < list.size()) {
                    return UUID.fromString(list.get(index++));

                } else {
                    read = false;
                    return get();
                }

            } else {

                UUID uid = UUID.randomUUID();
                list.add(uid.toString());
                return uid;

            }

        }

        void close() {

            if (!read) {
                try {

                    Files.write(path, list);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }

    }


    public ConversionTest conversionPackage(String conversionPackage) {

        String[] folders = conversionPackage.split("\\.");

        File file = new File(System.getProperty("user.dir"));
        file = new File(file, "src/test/resources");
        file = new File(file, folders[folders.length - 1] + ".uids");

        if (!file.exists()) {

            log.warn("UID file ({}) not found. UIDs will be generated randomly.", file.getName());

        } else {

            uidReaderWriter = new UidReaderWriter(file.toPath());

        }


        Map<String, Class<? extends Conversion>> conversionMap = new HashMap<>();

        new PackageResolver().forEach(conversionPackage, PayloadConversionBind.class, (bindClass, props) -> {

            String sourceEntity = (String) props.get("sourceEntity");
            conversionMap.put(sourceEntity, bindClass);

        });

        this.conversions = new Conversions(
                conversionMap,
                uidReaderWriter != null ? uidReaderWriter::get : null
        );

        return this;

    }


    public ConversionTest inputSoapFrom1c(String fileName) {

        try (InputStream is = new ClassPathResource(fileName).getInputStream()) {

            this.input = new E2Update(new SAXReader().read(is)
                    .getRootElement()
                    .element("Body")
                    .element("elementResponse")
                    .element("return")
                    .element("systemResponse")
            );

            return this;

        } catch (IOException | DocumentException e) {
            throw new RuntimeException(e);
        }

    }

    public ConversionTest inputSoapFrom1ce2(String fileName) {

        try (InputStream is = new ClassPathResource(fileName).getInputStream()) {

            this.input = new E2Update(new SAXReader().read(is)
                    .getRootElement()
                    .element("Body")
                    .element("elementResponse")
                    .element("systemResponse")
            );

            return this;

        } catch (IOException | DocumentException e) {
            throw new RuntimeException(e);
        }

    }

    public ConversionTest inputRest(String fileName) {

        try (InputStream is = new ClassPathResource(fileName).getInputStream()) {

            this.input = new E2Update(new SAXReader().read(is).getRootElement());

            return this;

        } catch (IOException | DocumentException e) {
            throw new RuntimeException(e);
        }

    }


    public ConversionTest expected(String fileName) {

        try (InputStream is = new ClassPathResource(fileName).getInputStream()) {

            this.expected = IOUtils.toString(is, "UTF-8");

            return this;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    public void run() {

        E2Update output;

        Converter4Payload converter = new Converter4Payload(
                input,
                output = new E2Update(),
                conversions);

        if (uidReaderWriter != null) {
            converter.setUidSupplier(uidReaderWriter::get);
        }


        for (E2Entity entity: input.entities()) {
            entity.elementsChanged().forEach(converter::convertElement);
        }


        if (uidReaderWriter != null) {
            uidReaderWriter.close();
        }


        String outputString = ConversionTest.string4xml(output.xml().getDocument());

        Assertions.assertEquals(expected, outputString);

    }

    private static String string4xml(Document document) {
        try {
            OutputFormat outputFormat = new OutputFormat("    ", true);

            StringWriter sw = new StringWriter();
            new XMLWriter(sw, outputFormat).write(document);

            return sw.toString();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}