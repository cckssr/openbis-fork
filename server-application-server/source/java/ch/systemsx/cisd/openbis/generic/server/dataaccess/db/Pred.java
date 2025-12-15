package ch.systemsx.cisd.openbis.generic.server.dataaccess.db;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@FunctionalInterface
public interface Pred {
    Predicate build(
            Root<?> root,
            CriteriaBuilder cb);
}
