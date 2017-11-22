package dk.dbc.z3950IllProxy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.yaz4j.ConnectionExtended;
import org.yaz4j.Package;
import org.yaz4j.exception.Bib1Exception;
import org.yaz4j.exception.ZoomException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Main rest endpoint for the z3950 ill proxy service.
 * <p>
 * Available endpoints:<br>
 * /howru/                  - Health check for service<br>
 * /ill/                    - Post an ILL order via z39.50.<br>
 * /doholdings/             - z39.50 holdings lookup using yaz4j client without timeout given on input, defaults to 60 seconds.<br>
 * /doholdings/{timeout}/   - z39.50 holdings lookup using yaz4j client with timeout given on input.
 */
@Stateless
@Path("")
public class Z3950Endpoint {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(Z3950Endpoint.class);

    private ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @EJB
    private Z3950Handler z3950Handler;

    /**
     * Returns application status.
     *
     * @return Response object with application status
     */
    @GET
    @Path("howru")
    public Response getStatus() {
        JsonObjectBuilder jsonResultObj = Json.createObjectBuilder();
        jsonResultObj.add("ok", true);
        JsonObject jsonObject = jsonResultObj.build();
        return Response.ok().entity(jsonObject.toString()).build();
    }

    /**
     * Post an ILL order via z39.50.
     *
     * @param inputData String representation of a JSON object that can be unmarshalled into an {@link SendIllRequest}
     * @return Response object with HTTP 200 for ok or HTTP 502 for error
     */
    @POST
    @Path("ill")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendIll(String inputData) {
        LOGGER.entry("inputData removed");
        StopWatch stopWatch = new Log4JStopWatch("Z3950Endpoint.sendIll");
        try {
            SendIllRequest sendIllRequest = objectMapper.readValue(inputData, SendIllRequest.class);
            String targetRef = "\"[target ref missing]\"";
            String returnDoc = "\"[return doc missing]\"";

            if (!validSendIllRequest(sendIllRequest)) {
                return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity("{errorMessage: \"invalid ill request\"}").build();
            }

            String targetHost = buildTargetHost(sendIllRequest);
            LOGGER.info("targetHost: " + targetHost);

            try (ConnectionExtended connection = new ConnectionExtended(targetHost, 0)) {
                connection.option("implementationName", Z3950Handler.IMPEMENTATION_APP);
                if (StringUtils.isNotEmpty(sendIllRequest.getUser())
                        && StringUtils.isNotEmpty(sendIllRequest.getGroup())
                        && StringUtils.isNotEmpty(sendIllRequest.getPassword())) {
                    connection.option("user", sendIllRequest.getUser());
                    connection.option("group", sendIllRequest.getGroup());
                    connection.option("password", sendIllRequest.getPassword());
                } else {
                    LOGGER.info("Not all of user, group and password is present, not setting any of them in request");
                }
                connection.connect();

                Package ill = connection.getPackage("itemorder");
                ill.option("doc", sendIllRequest.getData());
                ill.send();
            } catch (Bib1Exception e) {
                LOGGER.catching(XLogger.Level.ERROR, e);
                return Response.status(HttpURLConnection.HTTP_BAD_GATEWAY).type(MediaType.APPLICATION_JSON).entity("{errorMessage: " + e.getMessage() + "}").build();
            } catch (ZoomException e) {
                LOGGER.catching(XLogger.Level.ERROR, e);
                return Response.status(HttpURLConnection.HTTP_BAD_GATEWAY).type(MediaType.APPLICATION_JSON).entity("{errorMessage: " + e.getMessage()
                        + ", targetRef: " + targetRef
                        + ", returnDoc: " + returnDoc
                        + "}").build();
            }
            return Response.ok().entity("{inputData: " + inputData
                    + ", targetRef: " + targetRef
                    + ", returnDoc: " + returnDoc
                    + "}").type(MediaType.APPLICATION_JSON).build();
        } catch (IOException e) {
            LOGGER.catching(XLogger.Level.ERROR, e);
            return Response.status(HttpURLConnection.HTTP_BAD_GATEWAY).type(MediaType.APPLICATION_JSON).entity("{errorMessage: " + e.getMessage() + "}").build();
        } finally {
            stopWatch.stop();
            LOGGER.exit();
        }
    }

    // Utility method for validating that an z39.50 order contains the bare minimum of information
    private boolean validSendIllRequest(SendIllRequest sendIllRequest) {
        LOGGER.entry(sendIllRequest);
        boolean res = false;
        try {
            res = StringUtils.isNotEmpty(sendIllRequest.getServer()) && StringUtils.isNotEmpty(sendIllRequest.getData());
            return res;
        } finally {
            LOGGER.exit(res);
        }
    }

    // Utility method for building the target host url
    private String buildTargetHost(SendIllRequest sendIllRequest) {
        LOGGER.entry(sendIllRequest);
        String targetHost = null;
        try {
            targetHost = sendIllRequest.getServer();
            if (StringUtils.isNotEmpty(sendIllRequest.getPort())) {
                targetHost += ":" + sendIllRequest.getPort();
            }
            return targetHost;
        } finally {
            LOGGER.exit(targetHost);
        }
    }

    /**
     * z39.50 holdings lookup using yaz4j client without timeout given on input, defaults to 60 seconds.
     * Example of input:
     * <pre>
     * {@code
     * [{
     *    "url": "webservice.statsbiblioteket.dk:9054/standard",
     *    "server": "webservice.statsbiblioteket.dk",
     *    "port": 9054,
     *    "base": "standard",
     *    "id": "4874555",
     *    "user": "",
     *    "group": "",
     *    "password": "",
     *    "format": "XML",
     *    "esn": "B3",
     *    "schema": "1.2.840.10003.13.7.4",
     *    "responder": "820010"
     * }]
     * }
     * </pre>
     *
     * @param inputData A list of json objects matching {@link Z3950HoldingsRequest}
     * @return A json object containing the combined response
     * Example of output:
     * <pre>
     * {@code
     * {
     *    "820010:::4874555": {
     *       "message": "",
     *       "good": "true",
     *       "result": "<?xml version = \"1.0\" encoding = \"UTF-8\"?>\n  <n:holdingsStructure xmlns:n=\"http://www.loc.gov/z3950/agency/defns/HoldingsSchema8\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/z3950/agency/defns/HoldingsSchema8\nhttp://www.loc.gov/z3950/agency/defns/HoldingsSchema8.xsd\">\n    <bibItemInfo-1 targetItemId-3=\"004874555\"></bibItemInfo-1>\n    <holdingsStatement-4 numberOfCopies-14=\"000000002\">\n      <holdingsSiteLocation-6 targetLocationId-26=\"004874555\"></holdingsSiteLocation-6>\n      <localHoldings-10>\n        <bibView-11 targetBibPartId-40=\"004874555\" numberOfPieces-56=\"000000002\">\n          <bibPartLendingInfo-116 servicePolicy-109=\"1\" expectedDispatchDate-111=\"2017-11-20T00:00:00\">\n          </bibPartLendingInfo-116>\n        </bibView-11>\n      </localHoldings-10>\n    </holdingsStatement-4>\n  </n:holdingsStructure>\n"
     *    }
     * }
     * }
     * </pre>
     */
    @POST
    @Path("doholdings")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doHoldings(String inputData) {
        LOGGER.entry("inputData removed");
        StopWatch stopWatch = new Log4JStopWatch("Z3950Endpoint.doHoldings");
        try {
            return doHoldingsWithTimeout(60L, inputData);
        } finally {
            stopWatch.stop();
            LOGGER.exit();
        }
    }

    /**
     * z39.50 holdings lookup using yaz4j client with timeout given on input.
     * <p>
     * For example see: {@link Z3950Endpoint#doHoldings(String)}
     *
     * @param timeout   Time in seconds to wait for answer
     * @param inputData A list of json objects matching {@link Z3950HoldingsRequest}
     * @return A json object containing the combined response
     */
    @POST
    @Path("doholdings/{timeout}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doHoldingsWithTimeout(@PathParam("timeout") Long timeout, String inputData) {
        LOGGER.entry(timeout, "inputData removed");
        StopWatch stopWatch = new Log4JStopWatch("Z3950Endpoint.doHoldingsWithTimeout");
        String res = null;
        List<Z3950HoldingsRequest> z3950HoldingsRequests;
        try {
            if (timeout == 0 || timeout > 600) {
                throw new IllegalArgumentException("Invalid timeout given, must be > 0 or =< 600");
            }

            JsonObjectBuilder errorResults = Json.createObjectBuilder();
            z3950HoldingsRequests = objectMapper.readValue(inputData, new TypeReference<List<Z3950HoldingsRequest>>() {
            });
            Map<String, Future<String>> z3950HoldingResponseMap = new HashMap<>();
            String key;
            Future<String> value;
            JsonObjectBuilder errorResult;
            for (Z3950HoldingsRequest z3950HoldingsRequest : z3950HoldingsRequests) {
                if (validZ3950Request(z3950HoldingsRequest)) {
                    key = z3950HoldingsRequest.getResponder() + ":::" + z3950HoldingsRequest.getId();
                    try {
                        value = z3950Handler.z3950SearchImpl(z3950HoldingsRequest);
                        z3950HoldingResponseMap.put(key, value);
                    } catch (ZoomException e) {
                        LOGGER.catching(XLogger.Level.ERROR, e);
                        errorResult = z3950Handler.buildZ3950ErrorResult(e.getMessage());
                        errorResults.add(key, errorResult);
                    }
                } else {
                    if (StringUtils.isNotEmpty(z3950HoldingsRequest.getId()) && StringUtils.isNotEmpty(z3950HoldingsRequest.getResponder())) {
                        errorResult = z3950Handler.buildZ3950ErrorResult("Server url missing from request");
                        key = z3950HoldingsRequest.getResponder() + ":::" + z3950HoldingsRequest.getId();
                        errorResults.add(key, errorResult);
                    } else {
                        LOGGER.warn("Request does not contain enough information for a z39.50 holdings lookup: " + z3950HoldingsRequest);
                    }
                }
            }

            boolean keepRunning = true;
            LocalDateTime start = LocalDateTime.now();
            while (keepRunning) {
                Thread.sleep(1000);
                for (HashMap.Entry<String, Future<String>> entry : z3950HoldingResponseMap.entrySet()) {
                    keepRunning = !(keepRunning && entry.getValue().isDone());
                }
                if (keepRunning && start.plusSeconds(timeout).isAfter(LocalDateTime.now())) {
                    keepRunning = false;
                }
            }
            res = z3950Handler.mapZ3950HoldingsResponse(z3950HoldingResponseMap, errorResults);
            return Response.ok().entity(res).type(MediaType.APPLICATION_JSON).build();
        } catch (InterruptedException e) {
            LOGGER.catching(XLogger.Level.ERROR, e);
            return Response.status(HttpURLConnection.HTTP_BAD_GATEWAY).type(MediaType.APPLICATION_JSON).entity("{errorMessage: " + e.getMessage() + "}").build();
        } catch (IOException e) {
            LOGGER.catching(XLogger.Level.ERROR, e);
            return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity("{errorMessage: " + e.getMessage() + "}").build();
        } finally {
            stopWatch.stop();
            LOGGER.exit(res);
        }
    }

    // Utility method for validating that a z39.50 holdings request contains the bare minimum of information
    private boolean validZ3950Request(Z3950HoldingsRequest z3950HoldingsRequest) {
        LOGGER.entry(z3950HoldingsRequest);
        boolean res = false;
        try {
            res = StringUtils.isNotEmpty(z3950HoldingsRequest.getServer())
                    && StringUtils.isNotEmpty(z3950HoldingsRequest.getResponder())
                    && StringUtils.isNotEmpty(z3950HoldingsRequest.getId());
            return res;
        } finally {
            LOGGER.exit(res);
        }
    }
}
