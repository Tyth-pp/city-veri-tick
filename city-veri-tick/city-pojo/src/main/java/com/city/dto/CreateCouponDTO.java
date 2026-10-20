package com.city.dto;

import com.city.constant.CouponMessageConstant;
import jakarta.validation.constraints.*;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "创建优惠券请求，优惠类型与发放方式独立配置")
public class CreateCouponDTO {
    @Schema(description = "优惠券名称，最多64字", example = "城市演出九折券")
    @NotBlank(message = CouponMessageConstant.NAME_REQUIRED)
    @Size(max = 64)
    private String name;
    @Schema(description = "发放方式：普通领取、限量领取或定时秒杀", example = "FLASH_SALE", allowableValues = {"NORMAL", "LIMITED", "FLASH_SALE"})
    @NotBlank
    private String claimMode = "NORMAL";
    @Schema(description = "优惠类型：满减、折扣或无门槛", example = "DISCOUNT", allowableValues = {"FULL_REDUCTION", "DISCOUNT", "NO_THRESHOLD"})
    @NotBlank
    private String couponType = "FULL_REDUCTION";
    @Schema(description = "优惠券说明，最多255字", example = "适用于指定演出")
    @Size(max = 255)
    private String description;
    @Schema(description = "满减或无门槛券的固定抵扣金额；折扣券不使用此字段", example = "20.00")
    @DecimalMin("0.01")
    private BigDecimal discountAmount;
    @Schema(description = "折扣券实付比例，9折传0.9，8.5折传0.85；大于等于0.01且小于1", example = "0.9")
    @DecimalMin("0.01")
    @DecimalMax(value = "1", inclusive = false)
    private BigDecimal discountRate;
    @Schema(description = "折扣券最多减免金额；null表示不封顶，非空时必须大于0且最多两位小数", example = "100.00", nullable = true)
    @DecimalMin("0.01")
    private BigDecimal maxDiscountAmount;
    @Schema(description = "订单金额门槛；折扣券可填0，无门槛券保存为0", example = "500.00")
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal minOrderAmount = BigDecimal.ZERO;
    @Schema(description = "优惠后最低实付金额，至少0.01元", example = "0.01")
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal minPayAmount = new BigDecimal("0.01");
    @Schema(description = "总发放数量，正整数；个人不限次数仍受此库存约束", example = "1000")
    @NotNull
    @Min(1)
    private Integer totalQuantity;
    @Schema(description = "当前账号累计领取上限；0表示不限次数，正整数不能超过总发放量，遗漏默认1", example = "0")
    @NotNull
    @Min(0)
    private Integer perUserLimit = 1; // 0: no per-account claim limit
    @Schema(description = "适用范围：ALL为全平台（可配分类），ACTIVITY为指定活动", example = "ALL", allowableValues = {"ALL", "ACTIVITY"})
    @NotBlank
    private String scopeType = "ALL";
    @Schema(description = "指定活动时必填的活动ID", example = "1")
    private Long activityId;
    @Schema(description = "ALL范围下可选活动分类，多个分类按任一匹配；空列表表示全部分类", example = "[\"CONCERT\",\"PERFORMANCE\"]")
    private List<String> categoryCodes;
    @Schema(description = "退款后处理：RETURN在仍有效时返券，NO_RETURN不返券", example = "RETURN", allowableValues = {"RETURN", "NO_RETURN"})
    @NotBlank
    private String refundPolicy = "RETURN";
    @Schema(description = "领取开始时间，包含此时刻，使用北京时间", example = "2030-01-01T10:00:00")
    @NotNull
    private LocalDateTime startsAt;
    @Schema(description = "领取结束时间，不包含此时刻，必须晚于开始时间", example = "2030-01-02T10:00:00")
    @NotNull
    private LocalDateTime endsAt;
    @Schema(description = "使用开始时间，与领券窗口独立配置", example = "2030-01-01T10:00:00")
    @NotNull
    private LocalDateTime useStartsAt;
    @Schema(description = "使用结束时间，必须晚于使用开始且不早于领券结束", example = "2030-01-03T10:00:00")
    @NotNull
    private LocalDateTime useEndsAt;
}
