package dk.dbc.z3950IllProxy;

import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.yaz4j.ConnectionExtended;
import org.yaz4j.PrefixQuery;
import org.yaz4j.Query;
import org.yaz4j.ResultSet;
import org.yaz4j.exception.ZoomException;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Stateless
public class Z3950Handler {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(Z3950Endpoint.class);
    private static final String Z3950_SEARCH_RPN = "@attr 4=103 @attr BIB1 1=12 ";

    @Asynchronous
    public Future<String> z3950SearchImpl(Z3950HoldingsRequest z3950HoldingsRequest) throws ZoomException {
        LOGGER.entry(z3950HoldingsRequest);
        StopWatch stopWatch = new Log4JStopWatch("Z3950Handler.z3950SearchImpl");
        try {
            String targetHost = z3950HoldingsRequest.getServer() + ":" + z3950HoldingsRequest.getPort() + "/" + z3950HoldingsRequest.getBase();
            LOGGER.info("targetHost: " + targetHost);
            try (ConnectionExtended connection = new ConnectionExtended(targetHost, 0)) {
                connection.option("implementationName", "DBC z3950 ill proxy");
                if (z3950HoldingsRequest.getUser() != null && z3950HoldingsRequest.getGroup() != null && z3950HoldingsRequest.getPassword() != null) {
                    connection.option("user", z3950HoldingsRequest.getUser());
                    connection.option("group", z3950HoldingsRequest.getGroup());
                    connection.option("password", z3950HoldingsRequest.getPassword());
                }

                if (z3950HoldingsRequest.getSchema() != null) {
                    connection.option("schema", z3950HoldingsRequest.getSchema());
                } else {
                    connection.option("schema", "1.2.840.10003.13.7.4");
                }

                if (z3950HoldingsRequest.getFormat() != null) {
                    connection.option("preferredRecordSyntax", z3950HoldingsRequest.getFormat());
                } else {
                    connection.option("preferredRecordSyntax", "1.2.840.10003.5.109.10");
                }

                if (z3950HoldingsRequest.getEsn() != null) {
                    connection.option("elementSetName", z3950HoldingsRequest.getEsn());
                } else {
                    connection.option("elementSetName", "B3");
                }
                connection.option("smallSetUpperBound", "0");
                connection.option("largeSetLowerBound", "1");
                connection.option("mediumSetPresentNumber", "0");
                connection.connect();

                Query query = new PrefixQuery(Z3950_SEARCH_RPN + z3950HoldingsRequest.getId());
                ResultSet resultSet = connection.search(query);
                // TODO: THL: check hitcount og hent korrekt antal poster
                byte[] byteContent = resultSet.getRecord(0).getContent();
                return new AsyncResult<>(new String(byteContent));
            }
        } finally {
            stopWatch.stop();
            LOGGER.exit();
        }
    }

    public String mapZ3950HoldingsResponse(Map<String, Future<String>> z3950HoldingResponseMap) {
        LOGGER.entry();
        StopWatch stopWatch = new Log4JStopWatch("Z3950Handler.mapZ3950HoldingsResponse");
        String res = null;
        JsonObjectBuilder jsonResultObj = Json.createObjectBuilder();
        try {
            for (HashMap.Entry<String, Future<String>> entry : z3950HoldingResponseMap.entrySet()) {
                JsonObjectBuilder z3950Result = Json.createObjectBuilder();
                if (entry.getValue().isDone()) {
                    try {
                        z3950Result.add("message", "");
                        z3950Result.add("good", "true");
                        z3950Result.add("result", entry.getValue().get());
                    } catch (InterruptedException | ExecutionException e) {
                        z3950Result = Json.createObjectBuilder();
                        z3950Result.add("message", e.getMessage());
                        z3950Result.add("good", "false");
                        z3950Result.add("result", "");
                    }
                } else {
                    entry.getValue().cancel(true);
                    z3950Result.add("message", "no results found within timeout period");
                    z3950Result.add("good", "false");
                    z3950Result.add("result", "");
                }
                jsonResultObj.add(entry.getKey(), z3950Result);
            }
            JsonObject jsonObject = jsonResultObj.build();
            res = jsonObject.toString();
            return res;
        } finally {
            stopWatch.stop();
            LOGGER.exit(res);
        }
    }


}
