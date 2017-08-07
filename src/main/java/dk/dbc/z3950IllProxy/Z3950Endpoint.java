package dk.dbc.z3950IllProxy;

import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Stateless
@Path("")
public class Z3950Endpoint {
    @GET
    @Path("status")
    public Response getStatus() {
        return Response.ok().entity("{}").build();
    }
}
