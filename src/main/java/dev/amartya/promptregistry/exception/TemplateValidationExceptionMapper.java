package dev.amartya.promptregistry.exception;

import dev.amartya.promptregistry.model.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TemplateValidationExceptionMapper implements ExceptionMapper<TemplateValidationException> {

    @Override
    public Response toResponse(TemplateValidationException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(exception.getMessage()))
                .build();
    }
}
