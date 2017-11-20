package dk.dbc.z3950IllProxy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Stateless
@Path("")
public class Z3950Endpoint {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(Z3950Endpoint.class);

    private ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @EJB
    private Z3950Handler z3950Handler;

    // TODO: JavaDoc
    @GET
    @Path("howru")
    public Response getStatus() {
        return Response.ok().entity("{}").build();
    }

    // TODO: JavaDoc
    @POST
    @Path("ill")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendIll(String inputData) {
        LOGGER.entry(inputData);
        StopWatch stopWatch = new Log4JStopWatch("Z3950Endpoint.sendIll");
        try {
            // TODO: implement validation of required fields
            SendIllRequest jsonQuery = objectMapper.readValue(inputData, SendIllRequest.class);
            String targetRef = "[target ref missing]";
            String returnDoc = "[return doc missing]";

            String targetHost = jsonQuery.getServer() + ":" + jsonQuery.getPort();
            LOGGER.info("targetHost: " + targetHost);

            try (ConnectionExtended connection = new ConnectionExtended(targetHost, 0)) {
                connection.option("implementationName", "DBC z3950 ill proxy");
                connection.option("user", jsonQuery.getUser());
                connection.option("group", jsonQuery.getGroup());
                connection.option("password", jsonQuery.getPassword());
                connection.connect();

                Package ill = connection.getPackage("itemorder");
                ill.option("doc", jsonQuery.getData());
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
            return Response.status(HttpURLConnection.HTTP_BAD_GATEWAY).type(MediaType.APPLICATION_JSON).entity("{error: " + e.getMessage() + "}").build();
        } finally {
            stopWatch.stop();
            LOGGER.exit();
        }
    }

    // TODO: JavaDoc
    @POST
    @Path("doholdings")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doHoldings(String inputData) {
        LOGGER.entry(inputData);
        StopWatch stopWatch = new Log4JStopWatch("Z3950Endpoint.doHoldings");
        try {
            return doHolingsWithTimeout(60L, inputData);
        } finally {
            stopWatch.stop();
            LOGGER.exit();
        }
    }

    // TODO: JavaDoc
    @POST
    @Path("doholdings/{timeout}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doHolingsWithTimeout(@PathParam("timeout") Long timeout, String inputData) {
        LOGGER.entry(inputData);
        assert timeout > 0 && timeout < 600; // Min 1 sec and max 10 min
        StopWatch stopWatch = new Log4JStopWatch("Z3950Endpoint.doHoldingsWithTimeout");
        String res = null;
        try {
            List<Z3950HoldingsRequest> z3950HoldingsRequests = objectMapper.readValue(inputData, new TypeReference<List<Z3950HoldingsRequest>>() {
            });
            Map<String, Future<String>> z3950HoldingResponseMap = new HashMap<>();
            try {
                for (Z3950HoldingsRequest z3950HoldingsRequest : z3950HoldingsRequests) {
                    String key = z3950HoldingsRequest.getResponder() + ":::" + z3950HoldingsRequest.getId();
                    Future<String> value = z3950Handler.z3950SearchImpl(z3950HoldingsRequest);
                    z3950HoldingResponseMap.put(key, value);
                }
            } catch (ZoomException e) {
                return Response.status(HttpURLConnection.HTTP_BAD_GATEWAY).type(MediaType.APPLICATION_JSON).entity("{errorMessage: " + e.getMessage() + "}").build();
            }

            Boolean keepRunning = true;
            LocalDateTime start = LocalDateTime.now();
            LOGGER.info("THL42 #1 - keepRunning: " + keepRunning);
            while (keepRunning) {
                Thread.sleep(1000);
                for (HashMap.Entry<String, Future<String>> entry : z3950HoldingResponseMap.entrySet()) {
                    LOGGER.info("THL42 #2 - keepRunning: " + keepRunning);
                    LOGGER.info("THL42 #3 - entry.getValue().isDone(): " + entry.getValue().isDone());
                    keepRunning = !(keepRunning && entry.getValue().isDone());
                    LOGGER.info("THL42 #4 - keepRunning: " + keepRunning);
                }
                LOGGER.info("THL42 #5 - keepRunning: " + keepRunning);
                LOGGER.info("THL42 #6 - start.plusSeconds(timeout).isAfter(LocalDateTime.now()): " + start.plusSeconds(timeout).isAfter(LocalDateTime.now()));
                if (keepRunning && start.plusSeconds(timeout).isAfter(LocalDateTime.now())) {
                    keepRunning = false;
                }
            }
            res = z3950Handler.mapZ3950HoldingsResponse(z3950HoldingResponseMap);
            return Response.ok().entity(res).type(MediaType.APPLICATION_JSON).build();
        } catch (IOException | InterruptedException e) {
            return Response.status(HttpURLConnection.HTTP_BAD_GATEWAY).type(MediaType.APPLICATION_JSON).entity("{errorMessage: " + e.getMessage() + "}").build();
        } finally {
            stopWatch.stop();
            LOGGER.exit(res);
        }
    }
}
