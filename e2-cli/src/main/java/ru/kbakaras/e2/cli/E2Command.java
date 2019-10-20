package ru.kbakaras.e2.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.kbakaras.e2.cli.support.ConsoleTable;
import ru.kbakaras.e2.cli.support.E2Queue;
import ru.kbakaras.e2.cli.support.ServerConnector;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(
        name = "e2",
        header = "Без указания подкомманд отображает состояние всех очередей",
        mixinStandardHelpOptions = true,
        subcommands = {
                ResumeCommand.class,
                StopCommand.class,
                StartCommand.class,
                RevertCommand.class,
                RepeatCommand.class,
                ProcessCommand.class,
                ListCommand.class,
                ReadCommand.class,
                ReconvertCommand.class,
                ConfigCommand.class
})
public class E2Command implements Callable<Void> {

    @Option(names = { "-s", "--server" })
    private String server;

    @Option(names = { "-q", "--queue"}, description = "Запрашиваемая очередь")
    private E2Queue queue;

    private ObjectMapper mapper = new ObjectMapper();


    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new E2Command()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Void call() throws Exception {

        ObjectNode request = mapper.createObjectNode();
        if (queue != null && queue == E2Queue.delivery) {
            request.put("queue", queue.name());
        }

        Map<String, Object> result = createConnector().sendPost(
                "Queue/stats",
                mapper.writeValueAsString(request),
                null);

        JsonNode tree = mapper.readTree((String) result.get("body"));
        ConsoleTable table = new ConsoleTable();

        if (queue == E2Queue.delivery) {

            table.setHeaders(new String[] {"system", "|", "unprocessed", "stuck", "|", "processed", "undelivered"});
            table.addValue("----------");
            table.addValue("-");
            table.addValue("----------");
            table.addValue("----------");
            table.addValue("-");
            table.addValue("----------");
            table.addValue("----------");

            JsonNode array = tree.get("stats");
            array.forEach(node -> {
                table.addValue(node.get("destination").get("name").textValue());
                table.addValue("|");
                table.addValue(MessageFormat.format("{0}", node.get("unprocessed").longValue()));
                table.addValue(MessageFormat.format("{0}", node.get("stuck").longValue()));
                table.addValue("|");
                table.addValue(MessageFormat.format("{0}", node.get("processed").longValue()));
                table.addValue(MessageFormat.format("{0}", node.get("undelivered").longValue()));

            });


        } else {

            table.setHeaders(FIELDS);

            tree.fieldNames().forEachRemaining(queueName -> {
                table.addValue(queueName);
                JsonNode queueJson = tree.get(queueName);
                for (String field: FIELDS_Values) {
                    if (field.equals("|")) {
                        table.addValue("|");
                    } else {
                        table.addValue(queueJson.get(field).asText());
                    }
                }
                table.addValue(queueJson.get("stopped").asBoolean() ? "***" : null);
            });

        }


        System.out.print(table.toString());

        return null;
    }

    public String server() {
        if (server == null) {
            String env = System.getenv("e2.server");
            server = env != null ? env : "localhost";
        }
        return server;
    }

    public String serverAddress() {
        return "http://" + server() + ":10100/manage/";
    }

    public ServerConnector createConnector() {
        return new ServerConnector(serverAddress());
    }

    private static final String[] FIELDS = new String[] {
            "queue", "count", "stuck", "|", "delivered", "processed", "stopped"
    };
    private static final String[] FIELDS_Values =
            Arrays.copyOfRange(FIELDS, 1, FIELDS.length - 1);

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_INFO    = "INFO";
    public static final String RESULT_SKIPPED = "SKIPPED";
    public static final String RESULT_ERROR   = "ERROR";

    public static final String RESULT = "result";
    public static final String ERROR  = "error";
}