package com.qsp.resolvehub.specification;

import com.qsp.resolvehub.entity.Ticket;
import com.qsp.resolvehub.entity.TicketStatus;

import jakarta.annotation.Priority;
import org.springframework.data.jpa.domain.Specification;

public final class TicketSpecification {

    private TicketSpecification() {}

    public static Specification<Ticket> hasStatus(
            TicketStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(
            Priority priority) {
        return (root, query, cb) ->
                priority == null ? cb.conjunction()
                        : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> titleContains(
            String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            return cb.like(
                    cb.lower(root.get("title")),
                    "%" + keyword.trim().toLowerCase() + "%"
            );
        };
    }

    public static Specification<Ticket> descriptionContains(
            String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            return cb.like(
                    cb.lower(root.get("description")),
                    "%" + keyword.trim().toLowerCase() + "%"
            );
        };
    }
}
