package com.city.vo;
import com.city.entity.Coupon;
import lombok.Data;
import java.math.BigDecimal;
/**
 * 优惠券选择结果值对象(VO)
 * 用于封装优惠券选择后的相关信息，包括优惠券详情、优惠金额、是否可用、不可用原因以及用户拥有数量等
 */
@Data  // Lombok注解，自动生成getter、setter、toString等方法
public class CouponChoiceVO {
    private Coupon coupon;         // 优惠券详细信息对象
    private BigDecimal discountAmount;  // 优惠金额，使用BigDecimal确保精度
    private boolean usable;        // 该优惠券是否可用
    private String reason;         // 不可用时的原因描述，如果可用则为空
    private long ownedQuantity;    // 用户拥有的该优惠券数量
}
