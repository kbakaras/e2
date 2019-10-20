package ru.kbakaras.e2.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import ru.kbakaras.e2.cli.support.E2QueueField;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "read",
        mixinStandardHelpOptions = true,
        header = "Чтение информации о заданном сообщении"
)
public class ReadCommand implements Callable<Void> {
    @ParentCommand
    private E2Command parent;

    @Option(names = { "-i", "--id" }, required = true, description = "Идентификатор сообщения (UUID)")
    private UUID id;

    @Option(names = { "-f", "--field" }, description = "Поле очереди, значение которого требуется получить")
    private E2QueueField field;

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public Void call() throws Exception {
        ObjectNode request = mapper.createObjectNode();
        request.put("id",    mapper.writeValueAsString(id));
        if (field != null) {
            request.put("field", field.name());
        }

        Map<String, Object> result = parent.createConnector().sendPost(
                "Queue/read",
                mapper.writeValueAsString(request),
                null);

        JsonNode tree = mapper.readTree((String) result.get("body"));

        System.out.println(tree.get("fieldValue").textValue());
        return null;
    }
}