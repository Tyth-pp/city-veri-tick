package com.city.service.impl;
import com.city.service.support.AccountOperationPolicy;
import com.city.entity.*;
import com.city.mapper.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
class AccountOperationServiceTest {
 @Test void persistsActorSnapshotAndReasonWithoutPrivateAccountFields(){
  var service=new AccountOperationServiceImpl();var users=mock(UserMapper.class);var operations=mock(AccountOperationMapper.class);
  ReflectionTestUtils.setField(service,"users",users);ReflectionTestUtils.setField(service,"operations",operations);
  var actor=new User();actor.setStaffName("管理员李");actor.setUsername("admin_li");actor.setPasswordHash("private");when(users.selectById(1L)).thenReturn(actor);when(operations.insert(any(AccountOperation.class))).thenReturn(1);
  service.record(1L,7L,"DISABLE","  人员离职  ");
  verify(operations).insert(argThat((AccountOperation row)->row.getOperatorId().equals(1L)&&row.getTargetUserId().equals(7L)&&row.getOperatorName().equals("管理员李")&&row.getReason().equals("人员离职")&&row.getCreatedAt()!=null));
 }
 @Test void reasonsAreValidatedWithoutInventingDefaults(){assertThatThrownBy(()->AccountOperationPolicy.validate("  ",true)).hasMessage("请填写停用原因");assertThat(AccountOperationPolicy.validate(null,false)).isEmpty();assertThatThrownBy(()->AccountOperationPolicy.validate("a".repeat(501),false)).isInstanceOf(IllegalArgumentException.class);}
}
