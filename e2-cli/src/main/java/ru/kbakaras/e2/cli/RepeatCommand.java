package ru.kbakaras.e2.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

@CommandLine.Command(
        mixinStandardHelpOptions = true,
        name = "repeat",
        header = "Отправка повторного запроса за содержащимися в сообщении объектами к системе отправителю\n",
        description =
                "\nНеобходимо указать id сообщения."
)
public class RepeatCommand implements Callable<Void> {
    @ParentCommand
    private E2Command parent;

    @Option(names = { "-i", "--id" }, description = "Идентификатор сообщения (UUID)", required = true)
    private UUID id;

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public Void call() throws Exception {

        JsonNode tree = repeat(id);
        String result = tree.get("result").textValue();

        if (E2Command.RESULT_SUCCESS.equals(result)) {
            System.out.println("Message with id=" + id + " successfully sent for repeat.");

        } else {
            System.err.println("Repeat ERROR:");
            System.err.println(tree.get("error").textValue());
        }

        return null;
    }

    private JsonNode repeat(UUID id) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("id", mapper.writeValueAsString(id));

        Map<String, Object> response = parent.createConnector().sendPost(
                "Queue/repeat",
                mapper.writeValueAsString(request),
                null);

        return mapper.readTree((String) response.get("body"));
    }
}