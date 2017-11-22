package dk.dbc.z3950IllProxy;

import org.apache.commons.lang3.StringUtils;
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

/**
 * Bean that implements actual z39.50 holdings lookup functionality. Also provides functionality for formatting the
 * answer when done.
 */
@Stateless
public class Z3950Handler {
    public static final String IMPEMENTATION_APP = "DBC z3950 ill proxy";

    private static final XLogger LOGGER = XLoggerFactory.getXLogger(Z3950Handler.class);
    private static final String Z3950_SEARCH_RPN = "@attr 4=103 @attr BIB1 1=12 ";

    /**
     * Method for asynchronously making a holdings lookup over z39.50
     * <p>
     * For example see: {@link Z3950Endpoint#doHoldings(String)}
     *
     * @param z3950HoldingsRequest A json object matching {@link Z3950HoldingsRequest}
     * @return A json object containing the response
     * @throws ZoomException YAZ4J client exception
     */
    @Asynchronous
    public Future<String> z3950SearchImpl(Z3950HoldingsRequest z3950HoldingsRequest) throws ZoomException {
        LOGGER.entry(z3950HoldingsRequest);
        StopWatch stopWatch = new Log4JStopWatch("Z3950Handler.z3950SearchImpl");
        try {
            String targetHost = buildTargetHost(z3950HoldingsRequest);
            LOGGER.info("targetHost: " + targetHost);
            try (ConnectionExtended connection = new ConnectionExtended(targetHost, 0)) {
                connection.option("implementationName", IMPEMENTATION_APP);
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
                String content = "";
                if (resultSet.getHitCount() > 1) {
                    LOGGER.warn("Search for id = {} returned more than one result ({})", z3950HoldingsRequest.getId(), resultSet.getHitCount());
                } else if (resultSet.getHitCount() == 1) {
                    byte[] byteContent = resultSet.getRecord(0).getContent();
                    content = new String(byteContent);
                }
                return new AsyncResult<>(content);
            }
        } finally {
            stopWatch.stop();
            LOGGER.exit();
        }
    }

    /**
     * Method for mapping id and response from all z39.50 holdings lookup into combined structure.
     *
     * @param z3950HoldingResponseMap Map containing id and response object
     * @param errorResults            List of error results that must be merged with existing z39.50 results
     * @return JSON object with all z39.50 holdings responses
     */
    public String mapZ3950HoldingsResponse(Map<String, Future<String>> z3950HoldingResponseMap, JsonObjectBuilder errorResults) {
        LOGGER.entry();
        StopWatch stopWatch = new Log4JStopWatch("Z3950Handler.mapZ3950HoldingsResponse");
        String res = null;
        JsonObjectBuilder jsonResultObj = errorResults;
        try {
            for (HashMap.Entry<String, Future<String>> entry : z3950HoldingResponseMap.entrySet()) {
                JsonObjectBuilder z3950Result;
                if (entry.getValue().isDone()) {
                    try {
                        if (StringUtils.isEmpty(entry.getValue().get())) {
                            z3950Result = buildZ3950Result("No result found", false, "");
                        } else {
                            z3950Result = buildZ3950Result("", true, entry.getValue().get());
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        z3950Result = buildZ3950Result(e.getMessage(), false, "");
                    }
                } else {
                    entry.getValue().cancel(true);
                    z3950Result = buildZ3950Result("no results found within timeout period", false, "");
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

    // Utility method for building the target host url
    private String buildTargetHost(Z3950HoldingsRequest z3950HoldingsRequest) {
        LOGGER.entry(z3950HoldingsRequest);
        String targetHost = null;
        try {
            targetHost = z3950HoldingsRequest.getServer();
            if (z3950HoldingsRequest.getPort() != null && z3950HoldingsRequest.getPort() > 0) {
                targetHost += ":" + z3950HoldingsRequest.getPort();
            }
            if (StringUtils.isNotEmpty(z3950HoldingsRequest.getBase())) {
                targetHost += "/" + z3950HoldingsRequest.getBase();
            }
            return targetHost;
        } finally {
            LOGGER.exit(targetHost);
        }
    }

    /**
     * Utility method for creating a z39.50 holdings lookup result object.
     *
     * @param message Message of result
     * @param good    If the result is valid or not
     * @param result  The actual result of the lookup
     * @return JsonObjectBuilder object
     */
    public JsonObjectBuilder buildZ3950Result(String message, boolean good, String result) {
        LOGGER.entry(message, good, result);
        JsonObjectBuilder jsonObjectBuilder = null;
        try {
            jsonObjectBuilder = Json.createObjectBuilder();
            jsonObjectBuilder.add("message", message);
            jsonObjectBuilder.add("good", good);
            jsonObjectBuilder.add("result", result);
            return jsonObjectBuilder;
        } finally {
            LOGGER.exit(jsonObjectBuilder);
        }
    }

    /**
     * Utility method function for creating a z39.50 holdings lookup error result object.
     *
     * @param errorMessage Error message
     * @return JsonObjectBuilder object
     */
    public JsonObjectBuilder buildZ3950ErrorResult(String errorMessage) {
        LOGGER.entry(errorMessage);
        JsonObjectBuilder jsonObjectBuilder = null;
        try {
            jsonObjectBuilder = Json.createObjectBuilder();
            jsonObjectBuilder.add("message", errorMessage);
            jsonObjectBuilder.add("good", false);
            jsonObjectBuilder.add("result", "");
            return jsonObjectBuilder;
        } finally {
            LOGGER.exit(jsonObjectBuilder);
        }
    }
}
