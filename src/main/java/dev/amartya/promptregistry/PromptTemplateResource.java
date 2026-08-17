package dev.amartya.promptregistry;

import dev.amartya.promptregistry.model.PromptTemplate;
import dev.amartya.promptregistry.model.RenderPromptResponse;
import dev.amartya.promptregistry.model.RenderRequest;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PromptTemplateResource {

    @Inject
    PromptTemplateService service;

    @POST
    public Response create(@Valid PromptTemplate body) {
        return Response.status(Response.Status.CREATED).entity(service.create(body)).build();
    }

    @GET
    public List<PromptTemplate> list() {
        return service.list();
    }

    @GET
    @Path("{id}")
    public PromptTemplate get(@PathParam("id") String id) {
        return service.get(id);
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") String id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("{id}/render")
    public RenderPromptResponse render(@PathParam("id") String id, RenderRequest body) {
        return service.render(id, body == null ? new RenderRequest() : body);
    }
}
