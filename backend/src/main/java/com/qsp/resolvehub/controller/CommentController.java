
package com.qsp.resolvehub.controller;

import com.qsp.resolvehub.dto.request.CommentRequest;
import com.qsp.resolvehub.dto.response.CommentResponse;
import com.qsp.resolvehub.entity.Comment;
import com.qsp.resolvehub.service.CommentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CommentController {

    private final CommentService commentService;

    // Add a comment to an accessible ticket.
    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Integer ticketId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {

        Comment comment = commentService.addComment(
                ticketId,
                authentication.getName(),
                request.content()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommentResponse.from(comment));
    }

    // Get paginated comments for an accessible ticket.
    @GetMapping
    public ResponseEntity<Page<CommentResponse>> getComments(
            @PathVariable Integer ticketId,
            Authentication authentication,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable) {

        Page<CommentResponse> comments =
                commentService.getTicketComments(
                        ticketId,
                        authentication.getName(),
                        pageable
                ).map(CommentResponse::from);

        return ResponseEntity.ok(comments);
    }

    // Only the comment author can edit their comment.
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Integer ticketId,
            @PathVariable Integer commentId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {

        Comment comment = commentService.updateComment(
                ticketId,
                commentId,
                authentication.getName(),
                request.content()
        );

        return ResponseEntity.ok(CommentResponse.from(comment));
    }

    // The comment author or an admin can delete a comment.
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Integer ticketId,
            @PathVariable Integer commentId,
            Authentication authentication) {

        commentService.deleteComment(
                ticketId,
                commentId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}
