package com.city.service.impl;

import com.city.constant.CouponMessageConstant;
import com.city.constant.OrderMessageConstant;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.result.SystemResultCode;
import com.city.cache.CouponClaimGate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.city.dto.CreateCouponDTO;
import com.city.entity.*;
import com.city.exception.BusinessException;
import com.city.mapper.*;
import com.city.service.CouponService;
import com.city.service.support.CouponRulePolicy;
import com.city.vo.CouponChoiceVO;
import com.city.vo.UserCouponViewVO;
import com.city.vo.CouponWalletVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CouponServiceImpl implements CouponService {
    @Autowired
    CouponMapper coupons;
    @Autowired
    CouponClaimGate claimGate;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    CouponCategoryScopeMapper scopes;
    @Autowired
    UserCouponMapper userCoupons;
    @Autowired
    ActivityMapper activities;
    @Autowired
    ActivitySessionMapper sessions;
    @Autowired
    TicketTypeMapper types;
    @Autowired
    Clock clock;
    @Autowired
    TicketOrderMapper orders;

    @Transactional
    public Coupon create(CreateCouponDTO dto) {
        CouponRulePolicy.validate(dto);
        Activity activity = null;
        if ("ACTIVITY".equals(dto.getScopeType())) {
            activity = activities.selectById(dto.getActivityId());
            if (activity == null) throw new IllegalArgumentException(CouponMessageConstant.ACTIVITY_NOT_FOUND);
        }
        Coupon c = new Coupon();
        c.setClaimMode(dto.getClaimMode());
        c.setName(dto.getName().trim());
        c.setCouponType(dto.getCouponType());
        c.setDescription(dto.getDescription());
        c.setDiscountAmount("DISCOUNT".equals(dto.getCouponType()) ? BigDecimal.ZERO : dto.getDiscountAmount());
        c.setDiscountRate("DISCOUNT".equals(dto.getCouponType()) ? dto.getDiscountRate() : null);
        c.setMaxDiscountAmount("DISCOUNT".equals(dto.getCouponType()) ? dto.getMaxDiscountAmount() : null);
        c.setMinOrderAmount("NO_THRESHOLD".equals(dto.getCouponType()) ? BigDecimal.ZERO : dto.getMinOrderAmount());
        c.setMinPayAmount(dto.getMinPayAmount());
        c.setTotalQuantity(dto.getTotalQuantity());
        c.setPerUserLimit(dto.getPerUserLimit());
        c.setClaimedQuantity(0);
        c.setStatus("DRAFT");
        c.setStartsAt(dto.getStartsAt());
        c.setEndsAt(dto.getEndsAt());
        c.setUseStartsAt(dto.getUseStartsAt());
        c.setUseEndsAt(dto.getUseEndsAt());
        c.setScopeType(dto.getScopeType());
        c.setActivityId(activity == null ? null : activity.getId());
        c.setRefundPolicy(dto.getRefundPolicy());
        coupons.insert(c);
        if (activity == null && dto.getCategoryCodes() != null)
            dto.getCategoryCodes().stream().distinct().forEach(code -> {
                CouponCategoryScope scope = new CouponCategoryScope();
                scope.setCouponId(c.getId());
                scope.setCategoryCode(code);
                scopes.insert(scope);
            });
        return enrich(c);
    }

    public List<Coupon> adminList() {
        return coupons.selectList(Wrappers.<Coupon>lambdaQuery().orderByDesc(Coupon::getCreatedAt)).stream().map(this::enrich).toList();
    }

    @Transactional
    public void setStatus(Long id, String status) {
        if (!List.of("ACTIVE", "INACTIVE").contains(status)) throw new IllegalArgumentException(CouponMessageConstant.STATUS_INVALID);
        Coupon c = coupons.selectForUpdate(id);
        if (c == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        c.setStatus(status);
        coupons.updateById(c);
        if (CouponClaimGate.special(c)) claimGate.invalidateAfterCommit(id);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserCoupon claim(Long userId, Long couponId) {
        if (userId == null || userId <= 0 || couponId == null || couponId <= 0)
            throw new IllegalArgumentException(CouponMessageConstant.IDENTITY_INVALID);
        CouponClaimGate.Reservation reservation = claimGate.reserve(couponId, userId);
        // Acquire no main-transaction connection until Redis admission/bootstrap has completed.
        try {
            return new TransactionTemplate(transactionManager).execute(status -> persistClaim(userId, couponId, reservation));
        } catch (CannotCreateTransactionException ex) {
            // The transaction never started, so no completion callback can undo the reservation.
            claimGate.finish(reservation, false);
            throw ex;
        }
    }

    private UserCoupon persistClaim(Long userId, Long couponId, CouponClaimGate.Reservation reservation) {
        if (reservation != null)
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_COMMITTED) claimGate.finish(reservation, true);
                    else if (status == STATUS_ROLLED_BACK) claimGate.finish(reservation, false);
                    // Unknown commit results are not safe to compensate. Fixed Redis TTL reconciles them.
                }
            });
        // Database checks remain authoritative after provisional Redis admission.
        Coupon c = coupons.selectForUpdate(couponId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (c == null || !"ACTIVE".equals(c.getStatus()) || now.isBefore(c.getStartsAt()) || !now.isBefore(c.getEndsAt()))
            throw new BusinessException(SystemResultCode.COUPON_UNAVAILABLE);
        if (limit(c) > 0 && userCoupons.countClaims(userId, couponId) >= limit(c))
            throw new BusinessException(SystemResultCode.COUPON_ALREADY_CLAIMED);
        if (coupons.claimOne(couponId, now) != 1) throw new BusinessException(SystemResultCode.COUPON_UNAVAILABLE);
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus("AVAILABLE");
        userCoupons.insert(uc);
        return uc;
    }

    private int limit(Coupon c) {
        return c.getPerUserLimit() == null ? 1 : c.getPerUserLimit();
    }

    public List<Coupon> available(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        Map<Long, Long> claimed = userCoupons.selectList(Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getUserId, userId))
                .stream().collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        return coupons.selectList(Wrappers.<Coupon>lambdaQuery().eq(Coupon::getStatus, "ACTIVE").gt(Coupon::getEndsAt, now))
                .stream().filter(c -> CouponClaimGate.special(c) || (!now.isBefore(c.getStartsAt()) && c.getClaimedQuantity() < c.getTotalQuantity() && (limit(c) == 0 || claimed.getOrDefault(c.getId(), 0L) < limit(c))))
                .map(c -> {
                    int count = claimed.getOrDefault(c.getId(), 0L).intValue();
                    c.setUserClaimedQuantity(count);
                    c.setServerTime(clock.instant());
                    c.setClaimState(limit(c) > 0 && count >= limit(c) ? "LIMIT_REACHED" : now.isBefore(c.getStartsAt()) ? "UPCOMING" : c.getClaimedQuantity() >= c.getTotalQuantity() ? "SOLD_OUT" : "AVAILABLE");
                    return enrich(c);
                }).toList();
    }

    public List<UserCoupon> list(Long userId) {
        return userCoupons.selectList(Wrappers.<UserCoupon>lambdaQuery().eq(UserCoupon::getUserId, userId).orderByDesc(UserCoupon::getCreatedAt));
    }

    public CouponWalletVO mine(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        Map<Long, Coupon> definitions = new HashMap<>();
        Map<Long, TicketOrder> relatedOrders = new HashMap<>();
        List<UserCouponViewVO> items = list(userId).stream().map(uc -> {
            Coupon c = definitions.computeIfAbsent(uc.getCouponId(), id -> {
                Coupon found = coupons.selectById(id);
                return found == null ? null : enrich(found);
            });
            TicketOrder order = uc.getOrderId() == null ? null : relatedOrders.computeIfAbsent(uc.getOrderId(), orders::selectById);
            // 关联数据也校验账号，避免异常历史关联暴露他人订单。
            if (order != null && !userId.equals(order.getUserId())) order = null;
            return walletItem(uc, c, order, now);
        }).toList();
        CouponWalletVO result = new CouponWalletVO();
        result.setItems(items);
        result.setUsableCount(items.stream().filter(v -> "AVAILABLE".equals(v.getState())).count());
        result.setUnusedCount(items.stream().filter(v -> "UNUSED".equals(v.getGroup())).count());
        result.setUsedCount(items.stream().filter(v -> "USED".equals(v.getGroup())).count());
        result.setInvalidCount(items.stream().filter(v -> "INVALID".equals(v.getGroup())).count());
        return result;
    }

    private UserCouponViewVO walletItem(UserCoupon uc, Coupon c, TicketOrder order, LocalDateTime now) {
        UserCouponViewVO v = new UserCouponViewVO();
        v.setUserCouponId(uc.getId());
        v.setCoupon(c);
        v.setClaimedAt(uc.getCreatedAt());
        v.setUsedAt(uc.getUsedAt());
        if (order != null) {
            v.setOrderId(order.getId());
            v.setOrderNo(order.getOrderNo());
        }
        if (c == null) return state(v, "INVALID", "INVALID", CouponMessageConstant.LABEL_INVALID, CouponMessageConstant.INFORMATION_INVALID);
        // 订单已锁券后沿用下单时优惠，不因券刚好过期而将占用券误判成可用。
        if ("LOCKED".equals(uc.getStatus())) {
            if (order == null || !"PENDING_PAYMENT".equals(order.getStatus()))
                return state(v, "INVALID", "INVALID", CouponMessageConstant.LABEL_PENDING, CouponMessageConstant.ORDER_STATE_UPDATING);
            boolean canPay = order.getExpiredAt() != null && now.isBefore(order.getExpiredAt());
            v.setCanPay(canPay);
            return state(v, "UNUSED", "LOCKED", CouponMessageConstant.LABEL_LOCKED, canPay ? CouponMessageConstant.LOCKED_BY_UNPAID_ORDER : CouponMessageConstant.WAITING_FOR_RELEASE);
        }
        if ("USED".equals(uc.getStatus()))
            return state(v, "USED", "USED", CouponMessageConstant.LABEL_USED, order != null && "REFUNDED".equals(order.getStatus()) ? CouponMessageConstant.NOT_RETURNED_AFTER_REFUND : CouponMessageConstant.APPLIED_TO_ORDER);
        if ("EXPIRED".equals(uc.getStatus())) return state(v, "INVALID", "EXPIRED", CouponMessageConstant.LABEL_EXPIRED, CouponMessageConstant.USE_PERIOD_ENDED);
        if (!"AVAILABLE".equals(uc.getStatus())) return state(v, "INVALID", "INVALID", CouponMessageConstant.LABEL_INVALID, CouponMessageConstant.NOT_USABLE);
        if (!now.isBefore(CouponRulePolicy.useEnd(c)))
            return state(v, "INVALID", "EXPIRED", CouponMessageConstant.LABEL_EXPIRED, CouponMessageConstant.USE_PERIOD_ENDED);
        if (!"ACTIVE".equals(c.getStatus())) return state(v, "INVALID", "DISABLED", CouponMessageConstant.LABEL_DISABLED, CouponMessageConstant.DISABLED_BY_PLATFORM);
        if (now.isBefore(CouponRulePolicy.useStart(c)))
            return state(v, "UNUSED", "NOT_STARTED", CouponMessageConstant.LABEL_NOT_STARTED, CouponMessageConstant.USE_NOT_STARTED);
        return state(v, "UNUSED", "AVAILABLE", CouponMessageConstant.LABEL_AVAILABLE, CouponMessageConstant.SELECTION_HINT);
    }

    private UserCouponViewVO state(UserCouponViewVO v, String group, String state, String label, String reason) {
        v.setGroup(group);
        v.setState(state);
        v.setStateLabel(label);
        v.setReason(reason);
        return v;
    }

    private Coupon enrich(Coupon c) {
        c.setCategoryCodes(scopes.categories(c.getId()));
        if (c.getActivityId() != null) {
            Activity a = activities.selectById(c.getActivityId());
            c.setActivityTitle(a == null ? CouponMessageConstant.ACTIVITY_REMOVED : a.getTitle());
        }
        return c;
    }

    private String unusable(Coupon c, BigDecimal amount, String category, Long activityId) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (c == null || !"ACTIVE".equals(c.getStatus())) return CouponMessageConstant.DISABLED;
        if (now.isBefore(CouponRulePolicy.useStart(c))) return CouponMessageConstant.USE_NOT_STARTED;
        if (!now.isBefore(CouponRulePolicy.useEnd(c))) return CouponMessageConstant.EXPIRED;
        if ("ACTIVITY".equals(c.getScopeType())) {
            if (!Objects.equals(c.getActivityId(), activityId)) return CouponMessageConstant.ACTIVITY_SCOPE_MISMATCH;
        } else if (scopes.count(c.getId()) > 0 && !scopes.categories(c.getId()).contains(category))
            return CouponMessageConstant.CATEGORY_SCOPE_MISMATCH;
        if (!"NO_THRESHOLD".equals(c.getCouponType()) && amount.compareTo(c.getMinOrderAmount()) < 0)
            return CouponMessageConstant.THRESHOLD_NOT_MET;
        if (CouponRulePolicy.discount(c, amount).signum() <= 0) return CouponMessageConstant.MINIMUM_PAYMENT_NOT_MET;
        return null;
    }

    public List<CouponChoiceVO> choices(Long userId, Long ticketTypeId, int quantity) {
        if (quantity < 1 || quantity > 100) throw new IllegalArgumentException(OrderMessageConstant.QUANTITY_INVALID);
        TicketType type = types.selectById(ticketTypeId);
        ActivitySession session = type == null ? null : sessions.selectById(type.getSessionId());
        Activity activity = session == null ? null : activities.selectById(session.getActivityId());
        if (activity == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        BigDecimal amount = type.getPrice().multiply(BigDecimal.valueOf(quantity));
        Map<Long, Long> owned = userCoupons.selectList(Wrappers.<UserCoupon>lambdaQuery()
                        .eq(UserCoupon::getUserId, userId).eq(UserCoupon::getStatus, "AVAILABLE"))
                .stream().collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        List<CouponChoiceVO> result = new ArrayList<>();
        owned.forEach((id, count) -> {
            Coupon c = coupons.selectById(id);
            if (c == null) return;
            String reason = unusable(c, amount, activity.getCategory() == null ? null : activity.getCategory().name(), activity.getId());
            CouponChoiceVO option = new CouponChoiceVO();
            option.setCoupon(enrich(c));
            option.setOwnedQuantity(count);
            option.setUsable(reason == null);
            option.setReason(reason);
            option.setDiscountAmount(reason == null ? CouponRulePolicy.discount(c, amount) : BigDecimal.ZERO);
            result.add(option);
        });
        result.sort(Comparator.comparing(CouponChoiceVO::getDiscountAmount).reversed());
        return result;
    }

    @Transactional
    public BigDecimal lockForOrder(Long userId, Long couponId, Long orderId, BigDecimal amount, String category, Long activityId) {
        if (couponId == null) return null;
        UserCoupon found = userCoupons.find(userId, couponId);
        UserCoupon uc = found == null ? null : userCoupons.selectForUpdate(found.getId(), userId);
        if (uc == null || !"AVAILABLE".equals(uc.getStatus()))
            throw new BusinessException(SystemResultCode.COUPON_UNAVAILABLE);
        Coupon c = coupons.selectForUpdate(uc.getCouponId());
        String reason = unusable(c, amount, category, activityId);
        if (reason != null) throw new IllegalArgumentException(reason);
        if (userCoupons.change(uc.getId(), "AVAILABLE", "LOCKED", orderId, LocalDateTime.now(clock)) != 1)
            throw new BusinessException(SystemResultCode.COUPON_UNAVAILABLE);
        return CouponRulePolicy.discount(c, amount);
    }

    public void use(Long userId, Long couponId, Long orderId) {
        if (couponId == null) return;
        UserCoupon uc = userCoupons.findForOrder(userId, couponId, orderId);
        if (uc == null || userCoupons.markUsed(uc.getId(), orderId, LocalDateTime.now(clock)) != 1)
            throw new BusinessException(SystemResultCode.COUPON_UNAVAILABLE);
    }

    public void release(Long userId, Long couponId, Long orderId) {
        if (couponId == null) return;
        UserCoupon uc = userCoupons.findForOrder(userId, couponId, orderId);
        if (uc != null) userCoupons.release(uc.getId(), orderId);
    }

    public void restoreAfterRefund(Long userId, Long couponId, Long orderId) {
        if (couponId == null) return;
        UserCoupon uc = userCoupons.findForOrder(userId, couponId, orderId);
        if (uc == null || !"USED".equals(uc.getStatus())) return;
        Coupon c = coupons.selectById(couponId);
        if (c == null || "NO_RETURN".equals(c.getRefundPolicy())) return; // 保持已使用，不重新发券。
        if (!LocalDateTime.now(clock).isBefore(CouponRulePolicy.useEnd(c))) userCoupons.forfeit(uc.getId(), orderId);
        else if ("ACTIVE".equals(c.getStatus())) userCoupons.restoreAfterRefund(uc.getId(), orderId);
    }
}
