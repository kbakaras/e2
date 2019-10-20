package ru.kbakaras.e2.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.File;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.Callable;

import static ru.kbakaras.e2.cli.E2Command.ERROR;
import static ru.kbakaras.e2.cli.E2Command.RESULT;
import static ru.kbakaras.e2.cli.E2Command.RESULT_ERROR;
import static ru.kbakaras.e2.cli.E2Command.RESULT_INFO;
import static ru.kbakaras.e2.cli.E2Command.RESULT_SKIPPED;
import static ru.kbakaras.e2.cli.E2Command.RESULT_SUCCESS;

@CommandLine.Command(
        mixinStandardHelpOptions = true,
        name = "config",
        header = "Загрузка jar-файла конфигурации на сервер"
)
public class ConfigCommand implements Callable<Void> {

    @ParentCommand
    private E2Command parent;

    @Option(names = {"-f", "--file"}, description = "Jar-файл конфигурации")
    private File file;

    private ObjectMapper mapper = new ObjectMapper();


    @Override
    public Void call() throws Exception {

        ObjectNode request = mapper.createObjectNode();


        if (file != null) {

            if (!file.exists()) {
                System.err.println(MessageFormat.format("File {0} not exist!", file.getName()));
                return null;
            }

            request.put("fileName", file.getName());
            request.put("data", IOUtils.toByteArray(file.toURI()));

        }


        Map<String, Object> response = parent.createConnector().sendPost(
                "Queue/config",
                mapper.writeValueAsString(request),
                null);


        JsonNode tree = mapper.readTree((String) response.get("body"));

        String result = tree.get(RESULT).textValue();

        if (RESULT_SUCCESS.equals(result)) {

            System.out.println(MessageFormat.format(
                    "Configuration successfully updated. Current configuration:\n   {0}",
                    tree.get("configuration").textValue()
            ));

        } else if (RESULT_SKIPPED.equals(result)) {

            System.out.println(MessageFormat.format(
                    "Configuration update rejected. Possibly it is same as current. Current configuration:\n    {0}",
                    tree.get("configuration").textValue()
            ));

        } else if (RESULT_INFO.equals(result)) {

            System.out.println(MessageFormat.format(
                    "Current configuration:\n    {0}",
                    tree.get("configuration").textValue()
            ));

        } else if (RESULT_ERROR.equals(result)) {

            System.err.println(MessageFormat.format(
                    "Configuration update error:\n{0}",
                    tree.get(ERROR).textValue())
            );
        }

        return null;
    }
}
// config -f /home/kbakaras/projects/idea/glance/glance-e2/build/libs/glance-e2-1.0-SNAPSHOT.jar