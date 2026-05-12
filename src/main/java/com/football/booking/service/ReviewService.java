package com.football.booking.service;

import com.football.booking.dto.request.ReviewRequest;
import com.football.booking.dto.response.ReviewResponse;
import com.football.booking.entity.Field;
import com.football.booking.entity.Review;
import com.football.booking.entity.User;
import com.football.booking.exception.AccessDeniedException;
import com.football.booking.exception.ResourceNotFoundException;
import com.football.booking.repository.FieldRepository;
import com.football.booking.repository.ReviewRepository;
import com.football.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final FieldRepository fieldRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse createOrUpdateReview(ReviewRequest request, Authentication authentication) {
        User user = getUser(authentication);
        Field field = fieldRepository.findById(request.getFieldId())
                .orElseThrow(() -> new ResourceNotFoundException("Площадка не найдена с ID: " + request.getFieldId()));

        // Один отзыв на площадку — обновляем если уже есть
        Review review = reviewRepository.findByUserIdAndFieldId(user.getId(), field.getId())
                .orElse(Review.builder().user(user).field(field).build());

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return mapToResponse(reviewRepository.save(review));
    }

    public Page<ReviewResponse> getFieldReviews(Long fieldId, Pageable pageable) {
        if (!fieldRepository.existsById(fieldId)) {
            throw new ResourceNotFoundException("Площадка не найдена с ID: " + fieldId);
        }
        return reviewRepository.findByFieldId(fieldId, pageable)
                .map(this::mapToResponse);
    }

    public Page<ReviewResponse> getMyReviews(Authentication authentication, Pageable pageable) {
        User user = getUser(authentication);
        return reviewRepository.findByUserId(user.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public void deleteReview(Long reviewId, Authentication authentication) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Отзыв не найден с ID: " + reviewId));

        User user = getUser(authentication);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!review.getUser().getId().equals(user.getId()) && !isAdmin) {
            throw new AccessDeniedException("У вас нет прав для удаления этого отзыва");
        }

        reviewRepository.delete(review);
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .fieldId(review.getField().getId())
                .fieldName(review.getField().getName())
                .username(review.getUser().getUsername())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
