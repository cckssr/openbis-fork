package ch.systemsx.cisd.openbis.generic.server.dataaccess.db;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@FunctionalInterface
public interface Pred {
    Predicate build(
            Root<?> root,
            CriteriaBuilder cb);
}
