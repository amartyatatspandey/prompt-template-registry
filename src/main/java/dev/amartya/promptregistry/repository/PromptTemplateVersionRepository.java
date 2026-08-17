package dev.amartya.promptregistry.repository;

import dev.amartya.promptregistry.entity.PromptTemplateVersionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PromptTemplateVersionRepository implements PanacheRepositoryBase<PromptTemplateVersionEntity, Long> {
    // Repository pattern (not PanacheEntity active record): persistence stays injectable
    // and JPA types stay off the REST boundary.

    public int maxVersionNumber(String templateId) {
        Integer max = getEntityManager()
                .createQuery(
                        "select coalesce(max(v.versionNumber), 0) from PromptTemplateVersionEntity v where v.template.id = :id",
                        Integer.class)
                .setParameter("id", templateId)
                .getSingleResult();
        return max == null ? 0 : max;
    }

    public Optional<PromptTemplateVersionEntity> findByTemplateIdAndVersion(String templateId, int versionNumber) {
        return find("template.id = ?1 and versionNumber = ?2", templateId, versionNumber).firstResultOptional();
    }

    public List<PromptTemplateVersionEntity> listByTemplateIdOrdered(String templateId) {
        return list("template.id = ?1 order by versionNumber", templateId);
    }
}
