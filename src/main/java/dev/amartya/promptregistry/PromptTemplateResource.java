package dev.amartya.promptregistry;

import dev.amartya.promptregistry.model.CreateVersionRequest;
import dev.amartya.promptregistry.model.PromptTemplate;
import dev.amartya.promptregistry.model.RenderPromptResponse;
import dev.amartya.promptregistry.model.RenderRequest;
import dev.amartya.promptregistry.model.TemplateVersionDetail;
import dev.amartya.promptregistry.model.TemplateVersionSummary;
import dev.amartya.promptregistry.model.VersionDiff;
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

    @POST
    @Path("{id}/versions")
    public Response createVersion(@PathParam("id") String id, @Valid CreateVersionRequest body) {
        return Response.status(Response.Status.CREATED).entity(service.createVersion(id, body)).build();
    }

    @GET
    @Path("{id}/versions")
    public List<TemplateVersionSummary> listVersions(@PathParam("id") String id) {
        return service.listVersions(id);
    }

    @GET
    @Path("{id}/versions/{version}")
    public TemplateVersionDetail getVersion(@PathParam("id") String id, @PathParam("version") int version) {
        return service.getVersion(id, version);
    }

    @POST
    @Path("{id}/versions/{version}/render")
    public RenderPromptResponse renderVersion(
            @PathParam("id") String id,
            @PathParam("version") int version,
            RenderRequest body) {
        return service.renderVersion(id, version, body == null ? new RenderRequest() : body);
    }

    @GET
    @Path("{id}/versions/{fromVersion}/diff/{toVersion}")
    public VersionDiff diff(
            @PathParam("id") String id,
            @PathParam("fromVersion") int fromVersion,
            @PathParam("toVersion") int toVersion) {
        return service.diff(id, fromVersion, toVersion);
    }
}
