
package com.qsp.resolvehub.specification;

import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketStatus;
import com.qsp.resolvehub.entity.TicketPriority;

import org.springframework.data.jpa.domain.Specification;

public final class TicketSpecification {

    private TicketSpecification() {}

    public static Specification<Ticket> search(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }

            String pattern = "%" + search.trim().toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("ticketNumber")), pattern)
            );
        };
    }

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(TicketPriority priority) {
        return (root, query, cb) ->
                priority == null ? cb.conjunction()
                        : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> hasCategory(Integer categoryId) {
        return (root, query, cb) ->
                categoryId == null ? cb.conjunction()
                        : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Ticket> assignedTo(Integer agentId) {
        return (root, query, cb) ->
                agentId == null ? cb.conjunction()
                        : cb.equal(root.get("assignedTo").get("id"), agentId);
    }
}
