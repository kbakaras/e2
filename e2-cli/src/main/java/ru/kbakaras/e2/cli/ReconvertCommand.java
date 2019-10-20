package ru.kbakaras.e2.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import ru.kbakaras.e2.cli.support.E2Queue;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

@CommandLine.Command(
        mixinStandardHelpOptions = true,
        name = "reconvert",
        header = "Повторная конвертация сообщений в очереди на доставку\n",
        description =
                "\nВозможна повторная конвертация конкретного указанного " +
                "сообщения, либо переконвертация всей очереди. При вызове " +
                "данной команды очередь должна находиться в остановленном виде.\n"
)
public class ReconvertCommand implements Callable<Void> {
    @ParentCommand
    private E2Command parent;

    @Option(names = { "-i", "--id" }, description = "Идентификатор сообщения (UUID)")
    private UUID id;

    @Option(names = { "--full-reconvert" }, description = "Флаг переконвертации всей очереди")
    private boolean fullReconvert = false;

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public Void call() throws Exception {

        if (!fullReconvert) {
            if (id == null) {
                throw new IllegalArgumentException("Необходимо указать либо ID сообщения, либо флаг полной переконвертации!");
            }

            JsonNode tree = reconvertSingle(id);
            String result = tree.get("result").textValue();

            if (E2Command.RESULT_SUCCESS.equals(result)) {
                System.out.println(tree.get("newMessage").textValue());

            } else {
                System.err.println("Reconversion ERROR:");
                System.err.println(tree.get("error").textValue());
            }

        } else {
            ObjectNode request = mapper.createObjectNode();
            request.put("queue", E2Queue.delivery.name());
            request.put("stuck", false);
            request.put("processed", false);
            request.put("limit", 1000000);


            Map<String, Object> response = parent.createConnector().sendPost(
                    "Queue/list",
                    mapper.writeValueAsString(request),
                    null);

            JsonNode tree = mapper.readTree((String) response.get("body"));

            int count = tree.get("list").size();
            int index = 0;

            for (JsonNode node: tree.get("list")) {
                index++;

                UUID id = UUID.fromString(node.get("id").textValue());
                tree = reconvertSingle(id);

                String result = tree.get("result").textValue();

                if (E2Command.RESULT_SUCCESS.equals(result)) {
                    System.out.println(String.format("OK %d/%d %s", index, count, id.toString()));

                } else if (E2Command.RESULT_SKIPPED.equals(result)) {
                    System.err.println(String.format("SKIPPED %d/%d %s", index, count, id.toString()));
                    System.err.println(tree.get("error").textValue());

                } else if (E2Command.RESULT_ERROR.equals(result)) {
                    System.err.println(String.format("ERROR %d/%d %s", index, count, id.toString()));
                    System.err.println(tree.get("error").textValue());
                    break;
                }
            }

        }

        return null;
    }

    private JsonNode reconvertSingle(UUID id) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("id", mapper.writeValueAsString(id));

        Map<String, Object> response = parent.createConnector().sendPost(
                "Queue/reconvert",
                mapper.writeValueAsString(request),
                null);

        return mapper.readTree((String) response.get("body"));
    }
}