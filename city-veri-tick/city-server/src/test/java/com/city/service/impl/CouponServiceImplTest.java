package com.city.service.impl;
import com.city.cache.CouponClaimGate;
import com.city.exception.BusinessException;
import com.city.vo.UserCouponViewVO;
import com.city.entity.*;
import com.city.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.*;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CouponServiceImplTest {
    CouponMapper coupons=mock(CouponMapper.class);
    UserCouponMapper userCoupons=mock(UserCouponMapper.class);
    CouponCategoryScopeMapper scopes=mock(CouponCategoryScopeMapper.class);
    CouponServiceImpl service=new CouponServiceImpl();
    TicketOrderMapper orders=mock(TicketOrderMapper.class);
    Coupon c;
    @BeforeEach void setup(){
        ReflectionTestUtils.setField(service,"claimGate",org.mockito.Mockito.mock(CouponClaimGate.class)); ReflectionTestUtils.setField(service,"coupons",coupons); ReflectionTestUtils.setField(service,"scopes",scopes);
        ReflectionTestUtils.setField(service,"userCoupons",userCoupons); ReflectionTestUtils.setField(service,"orders",orders);
        var manager=mock(org.springframework.transaction.PlatformTransactionManager.class);when(manager.getTransaction(any())).thenAnswer(i->new org.springframework.transaction.support.SimpleTransactionStatus());ReflectionTestUtils.setField(service,"transactionManager",manager);
        ReflectionTestUtils.setField(service,"clock",Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"),ZoneOffset.UTC));
        c=new Coupon(); c.setId(9L); c.setStatus("ACTIVE"); c.setCouponType("FULL_REDUCTION");
        c.setDiscountAmount(new BigDecimal("20")); c.setMinOrderAmount(new BigDecimal("100"));
        c.setPerUserLimit(1); c.setStartsAt(LocalDateTime.of(2029,1,1,0,0)); c.setEndsAt(LocalDateTime.of(2031,1,1,0,0));
        when(coupons.selectForUpdate(9L)).thenReturn(c); when(coupons.selectById(9L)).thenReturn(c);
    }
    UserCoupon owned(String state){
        UserCoupon uc=new UserCoupon(); uc.setId(11L); uc.setCouponId(9L); uc.setStatus(state);
        when(userCoupons.find(7L,9L)).thenReturn(uc); when(userCoupons.selectForUpdate(11L,7L)).thenReturn(uc);
        when(userCoupons.findForOrder(7L,9L,101L)).thenReturn(uc); return uc;
    }
    @Test void claimAtomicallyIncrementsCouponAndCreatesUserCoupon(){
        when(coupons.claimOne(eq(9L),any())).thenReturn(1);
        assertThat(service.claim(7L,9L).getStatus()).isEqualTo("AVAILABLE"); verify(userCoupons).insert(any(UserCoupon.class));
        var sequence=inOrder(coupons,userCoupons); sequence.verify(coupons).selectForUpdate(9L); sequence.verify(userCoupons).countClaims(7L,9L);
    }
    @Test void duplicateClaimRejectedBeforeIncrement(){
        when(userCoupons.countClaims(7L,9L)).thenReturn(1);
        assertThatThrownBy(()->service.claim(7L,9L)).hasMessageContaining("已领取"); verify(coupons,never()).claimOne(anyLong(),any());
    }
    @Test void configuredLimitAllowsSecondClaimButRejectsThird(){
        c.setPerUserLimit(2); when(userCoupons.countClaims(7L,9L)).thenReturn(1,2); when(coupons.claimOne(eq(9L),any())).thenReturn(1);
        service.claim(7L,9L); assertThatThrownBy(()->service.claim(7L,9L)).hasMessageContaining("已领取");
        verify(coupons,times(1)).claimOne(eq(9L),any());
    }
    @Test void lockChecksMinimumAndReturnsDiscount(){
        owned("AVAILABLE"); when(userCoupons.change(eq(11L),eq("AVAILABLE"),eq("LOCKED"),eq(101L),any())).thenReturn(1);
        assertThat(service.lockForOrder(7L,9L,101L,new BigDecimal("120"),"CONCERT",1L)).isEqualByComparingTo("20");
        assertThatThrownBy(()->service.lockForOrder(7L,9L,102L,new BigDecimal("80"),"CONCERT",1L)).hasMessageContaining("门槛");
    }
    @Test void onlySpecifiedActivityCanUseCoupon(){
        owned("AVAILABLE"); c.setScopeType("ACTIVITY"); c.setActivityId(1L);
        assertThatThrownBy(()->service.lockForOrder(7L,9L,101L,new BigDecimal("120"),"CONCERT",2L)).hasMessageContaining("指定活动");
        verify(userCoupons,never()).change(anyLong(),anyString(),anyString(),anyLong(),any());
    }
    @Test void couponCanBeUsedAfterClaimWindowEnds(){
        owned("AVAILABLE"); c.setEndsAt(LocalDateTime.of(2029,12,1,0,0)); c.setUseStartsAt(c.getStartsAt()); c.setUseEndsAt(LocalDateTime.of(2031,1,1,0,0));
        when(userCoupons.change(eq(11L),anyString(),anyString(),eq(101L),any())).thenReturn(1);
        assertThat(service.lockForOrder(7L,9L,101L,new BigDecimal("120"),"CONCERT",1L)).isEqualByComparingTo("20");
    }
    @Test void useReleaseAndRefundRestoreTargetTheOrder(){
        UserCoupon uc=owned("LOCKED"); when(userCoupons.markUsed(eq(11L),eq(101L),any())).thenReturn(1);
        service.use(7L,9L,101L); verify(userCoupons).markUsed(eq(11L),eq(101L),any());
        service.release(7L,9L,101L); verify(userCoupons).release(11L,101L);
        uc.setStatus("USED"); service.restoreAfterRefund(7L,9L,101L); verify(userCoupons).restoreAfterRefund(11L,101L);
    }
    @Test void noReturnKeepsCouponUsedAndExpiredReturnIsForfeited(){
        owned("USED"); c.setRefundPolicy("NO_RETURN"); service.restoreAfterRefund(7L,9L,101L); verify(userCoupons,never()).forfeit(anyLong(),anyLong());
        c.setRefundPolicy("RETURN");c.setUseEndsAt(LocalDateTime.of(2029,12,31,0,0));service.restoreAfterRefund(7L,9L,101L);verify(userCoupons).forfeit(11L,101L);
        verify(userCoupons,never()).restoreAfterRefund(anyLong(),anyLong());
    }

    @Test void walletSeparatesUsableFutureExpiredAndLockedInstances() {
        Coupon future=new Coupon();org.springframework.beans.BeanUtils.copyProperties(c,future);future.setId(10L);future.setUseStartsAt(LocalDateTime.of(2030,2,1,0,0));
        Coupon expired=new Coupon();org.springframework.beans.BeanUtils.copyProperties(c,expired);expired.setId(12L);expired.setUseEndsAt(LocalDateTime.of(2030,1,1,0,0));
        when(coupons.selectById(10L)).thenReturn(future);when(coupons.selectById(12L)).thenReturn(expired);
        UserCoupon available=row(1L,9L,"AVAILABLE"),notStarted=row(2L,10L,"AVAILABLE"),ended=row(3L,12L,"AVAILABLE"),locked=row(4L,9L,"LOCKED");locked.setOrderId(101L);
        TicketOrder pending=new TicketOrder();pending.setId(101L);pending.setUserId(7L);pending.setStatus("PENDING_PAYMENT");pending.setExpiredAt(LocalDateTime.of(2030,1,1,0,15));when(orders.selectById(101L)).thenReturn(pending);
        when(userCoupons.selectList(any())).thenReturn(java.util.List.of(available,notStarted,ended,locked));
        var wallet=service.mine(7L);
        assertThat(wallet.getUsableCount()).isEqualTo(1);assertThat(wallet.getUnusedCount()).isEqualTo(3);assertThat(wallet.getInvalidCount()).isEqualTo(1);
        assertThat(wallet.getItems()).extracting(UserCouponViewVO::getState).containsExactly("AVAILABLE","NOT_STARTED","EXPIRED","LOCKED");
        assertThat(wallet.getItems().get(3).isCanPay()).isTrue();
    }
    @Test void walletDoesNotExposeOtherAccountsOrderOrPretendAnExpiredLockIsAvailable() {
        UserCoupon locked=row(4L,9L,"LOCKED");locked.setOrderId(101L);
        TicketOrder other=new TicketOrder();other.setId(101L);other.setUserId(8L);other.setStatus("PENDING_PAYMENT");when(orders.selectById(101L)).thenReturn(other);
        when(userCoupons.selectList(any())).thenReturn(java.util.List.of(locked));
        var item=service.mine(7L).getItems().get(0);assertThat(item.getOrderId()).isNull();assertThat(item.isCanPay()).isFalse();
        other.setUserId(7L);other.setExpiredAt(LocalDateTime.of(2030,1,1,0,0));
        var wallet=service.mine(7L);assertThat(wallet.getUsableCount()).isZero();assertThat(wallet.getItems().get(0).getState()).isEqualTo("LOCKED");assertThat(wallet.getItems().get(0).isCanPay()).isFalse();
    }
    @Test void usedCouponKeepsItsOrderAndReturnedCouponBecomesUsableAgain() {
        UserCoupon used=row(1L,9L,"USED");used.setOrderId(101L);
        TicketOrder paid=new TicketOrder();paid.setId(101L);paid.setUserId(7L);paid.setStatus("PAID");paid.setOrderNo("ORDER101");when(orders.selectById(101L)).thenReturn(paid);
        when(userCoupons.selectList(any())).thenReturn(java.util.List.of(used));
        assertThat(service.mine(7L).getUsedCount()).isEqualTo(1);assertThat(service.mine(7L).getItems().get(0).getOrderNo()).isEqualTo("ORDER101");
        used.setStatus("AVAILABLE");used.setOrderId(null);
        assertThat(service.mine(7L).getUsableCount()).isEqualTo(1);
        c.setStatus("INACTIVE");assertThat(service.mine(7L).getInvalidCount()).isEqualTo(1);
    }
    private UserCoupon row(Long id,Long couponId,String state) {
        UserCoupon u=new UserCoupon();u.setId(id);u.setUserId(7L);u.setCouponId(couponId);u.setStatus(state);return u;
    }
    @Test void redisReservationIsFinalizedOnlyByTransactionOutcome(){
        var gate=mock(CouponClaimGate.class);ReflectionTestUtils.setField(service,"claimGate",gate);
        var ticket=new CouponClaimGate.Reservation(9L,7L,"request");when(gate.reserve(9L,7L)).thenReturn(ticket);when(coupons.claimOne(eq(9L),any())).thenReturn(1);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try{service.claim(7L,9L);verify(gate,never()).finish(any(),anyBoolean());
            var callback=org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().get(0);
            callback.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED);verify(gate).finish(ticket,true);
        }finally{org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();}
    }
    @Test void rejectedDatabaseClaimCompensatesOnlyAfterRollback(){
        var gate=mock(CouponClaimGate.class);ReflectionTestUtils.setField(service,"claimGate",gate);
        var ticket=new CouponClaimGate.Reservation(9L,7L,"request");when(gate.reserve(9L,7L)).thenReturn(ticket);when(userCoupons.countClaims(7L,9L)).thenReturn(1);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try{assertThatThrownBy(()->service.claim(7L,9L)).isInstanceOf(BusinessException.class);verify(gate,never()).finish(any(),anyBoolean());
            org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().get(0).afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);verify(gate).finish(ticket,false);
        }finally{org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();}
    }

    @Test void transactionStartFailureReturnsTheReservation(){
        var gate=mock(CouponClaimGate.class);ReflectionTestUtils.setField(service,"claimGate",gate);
        var ticket=new CouponClaimGate.Reservation(9L,7L,"request");when(gate.reserve(9L,7L)).thenReturn(ticket);
        var manager=mock(org.springframework.transaction.PlatformTransactionManager.class);when(manager.getTransaction(any())).thenThrow(new org.springframework.transaction.CannotCreateTransactionException("pool unavailable"));ReflectionTestUtils.setField(service,"transactionManager",manager);
        assertThatThrownBy(()->service.claim(7L,9L)).isInstanceOf(org.springframework.transaction.CannotCreateTransactionException.class);verify(gate).finish(ticket,false);verify(coupons,never()).claimOne(anyLong(),any());
    }
    @Test void specialCouponsRemainVisibleBeforeOpeningAndWhenSoldOut(){
        c.setClaimMode("FLASH_SALE");c.setTotalQuantity(10);c.setClaimedQuantity(0);c.setStartsAt(LocalDateTime.of(2030,1,2,0,0));when(coupons.selectList(any())).thenReturn(java.util.List.of(c));when(userCoupons.selectList(any())).thenReturn(java.util.List.of());
        assertThat(service.available(7L)).singleElement().satisfies(v->assertThat(v.getClaimState()).isEqualTo("UPCOMING"));c.setStartsAt(LocalDateTime.of(2029,1,1,0,0));c.setClaimedQuantity(10);assertThat(service.available(7L)).singleElement().satisfies(v->assertThat(v.getClaimState()).isEqualTo("SOLD_OUT"));
    }

}
