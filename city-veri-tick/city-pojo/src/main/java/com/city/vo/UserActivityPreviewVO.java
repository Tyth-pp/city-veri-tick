package com.city.vo;

/**
 * Read-only metadata for saved activities, independent of the current sales window.
 */
public record UserActivityPreviewVO(UserActivitySummaryVO activity, String availability) {
}
