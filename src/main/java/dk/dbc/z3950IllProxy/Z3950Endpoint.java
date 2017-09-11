package dk.dbc.z3950IllProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaz4j.ConnectionExtended;
import org.yaz4j.Package;
import org.yaz4j.exception.Bib1Exception;
import org.yaz4j.exception.ZoomException;

import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Stateless
@Path("")
public class Z3950Endpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(Z3950Endpoint.class);

    @GET
    @Path("status")
    public Response getStatus() {
        return Response.ok().entity("{}").build();
    }

    @POST
    @Path("ill/{bibliotek}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendIll( @PathParam("bibliotek") String bibliotek,
                             String jobInputStreamData) throws ZoomException {

        String targetRef="[target ref missing]";
        String returnDoc="[return doc missing]";
        
        try(  ConnectionExtended connection= new ConnectionExtended("z3950.dbc.dk:2106/danbib", 0)) {

            connection.option("implementationName","DBC z3950 ill");
            connection.option("user","user");
            connection.option("group","group");
            connection.option("password","ItIsSecret");
            connection.connect();

            Package ill = connection.getPackage("itemorder");
            ill.option("doc", jobInputStreamData);
            ill.send();


        } catch( Bib1Exception e) {
            return Response.ok().entity("{\n  ERROR1: \n  "+ bibliotek                     
                            + "\n  " + e.getMessage()
                            + "\n}\n").build();

        } catch( ZoomException e) {
            LOGGER.error( e.getMessage() , e);

            return Response.ok().entity("{\n  ERROR2: \n  "+ bibliotek                     
                            + "\n  " + targetRef
                            + "\n  " + returnDoc
                            + "\n}\n").build();
        }

        return Response.ok().entity("{\n  "+ bibliotek
                + "\n  " + jobInputStreamData
                + "\n  " + targetRef                  
                + "\n  " + returnDoc
                + "\n}\n").build();
    }
}
