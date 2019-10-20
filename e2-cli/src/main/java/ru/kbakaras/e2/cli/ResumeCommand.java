package ru.kbakaras.e2.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import picocli.CommandLine;
import picocli.CommandLine.ParentCommand;
import ru.kbakaras.e2.cli.support.E2Queue;

import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        mixinStandardHelpOptions = true,
        name = "resume",
        header = "Возобновление работы остановленной очереди"
)
public class ResumeCommand implements Callable<Void> {
    @ParentCommand
    private E2Command parent;

    @CommandLine.Option(names = { "-q", "--queue"}, required = true, description = "Запрашиваемая очередь")
    private E2Queue queue;

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public Void call() throws Exception {
        ObjectNode request = mapper.createObjectNode();
        request.put("queue", queue.name());

        Map<String, Object> result = parent.createConnector().sendPost(
                "Queue/resume",
                mapper.writeValueAsString(request),
                null);

        JsonNode tree = mapper.readTree((String) result.get("body"));

        return null;
    }
}
