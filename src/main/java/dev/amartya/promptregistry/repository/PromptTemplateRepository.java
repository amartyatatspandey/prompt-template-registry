package dev.amartya.promptregistry.repository;

import dev.amartya.promptregistry.entity.PromptTemplateEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PromptTemplateRepository implements PanacheRepositoryBase<PromptTemplateEntity, String> {
    // Repository pattern (not PanacheEntity active record): persistence stays injectable
    // and JPA types stay off the REST boundary.
}
