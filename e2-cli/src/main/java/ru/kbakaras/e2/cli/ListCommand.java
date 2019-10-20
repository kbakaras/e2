package ru.kbakaras.e2.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import ru.kbakaras.e2.cli.support.ConsoleTable;
import ru.kbakaras.e2.cli.support.E2Queue;

import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "list",
        mixinStandardHelpOptions = true,
        header = "Получение списка сообщений очереди в порядке обработки"
)
public class ListCommand implements Callable<Void> {
    @ParentCommand
    private E2Command parent;

    @Option(names = { "-q", "--queue"}, required = true, description = "Запрашиваемая очередь")
    private E2Queue queue;

    @Option(names = { "-t", "--stuck" }, description = "Флаг запроса только застрявших сообщений")
    private boolean stuck = false;

    @Option(names = { "-p", "--processed" }, description = "Флаг запроса также и обработанных сообщений")
    private boolean processed = false;

    @Option(names = { "-l", "--limit" }, description = "Ограничение количества возвращаемых сообщений (default: ${DEFAULT-VALUE})")
    private int limit = 10;

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public Void call() throws Exception {
        ObjectNode request = mapper.createObjectNode();
        request.put("queue", queue.name());
        request.put("stuck", stuck);
        request.put("processed", processed);
        request.put("limit", limit);


        Map<String, Object> result = parent.createConnector().sendPost(
                "Queue/list",
                mapper.writeValueAsString(request),
                null);

        JsonNode tree = mapper.readTree((String) result.get("body"));

        ConsoleTable table = new ConsoleTable();
        table.setHeaders(FIELDS);

        tree.get("list").forEach(node -> {
            for (String field: FIELDS) {
                table.addValue(node.get(field).asText());
            }
        });

        System.out.print(table.toString());

        return null;
    }

    private static final String[] FIELDS = new String[] {
            "id", "timestamp", "size", "attempt", "stuck", "processed", "delivered", "destination"
    };
}