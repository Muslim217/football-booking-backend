package com.football.booking.controller;

import com.football.booking.dto.request.ReviewRequest;
import com.football.booking.dto.response.ReviewResponse;
import com.football.booking.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Отзывы", description = "Отзывы и рейтинги площадок")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/field/{fieldId}")
    @Operation(summary = "Отзывы по площадке (публичный)")
    public ResponseEntity<Page<ReviewResponse>> getFieldReviews(
            @PathVariable Long fieldId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(reviewService.getFieldReviews(fieldId, pageable));
    }

    @GetMapping("/my")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Мои отзывы")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            Authentication authentication,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(reviewService.getMyReviews(authentication, pageable));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Оставить / обновить отзыв")
    public ResponseEntity<ReviewResponse> createOrUpdateReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createOrUpdateReview(request, authentication));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Удалить отзыв (автор или ADMIN)")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            Authentication authentication) {
        reviewService.deleteReview(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
