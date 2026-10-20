package com.city.config;

import com.city.exception.RefundException;
import com.city.mapper.AccountOperationMapper;
import com.city.mapper.UserMapper;
import com.city.service.AccountOperationService;
import com.city.service.ManagementMessageService;
import com.city.service.RefundService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.*;
import java.time.Clock;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real Spring wiring and JDK transaction proxies, with isolated external dependencies. */
class ServiceWiringTest {
    static final String[] SERVICES={"AccountOperationService","ManagementDashboardService","ManagementMessageService","RefundService","SmsCodeService","StaffAccountService","StaffProfileService","UserProfileService"};
    @Configuration @EnableTransactionManagement(proxyTargetClass=false) static class Transactions { }
    static class CountingTransactions extends AbstractPlatformTransactionManager {
        int begun,committed,rolledBack;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object tx,TransactionDefinition definition) { begun++; }
        @Override protected void doCommit(DefaultTransactionStatus status) { committed++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { rolledBack++; }
    }
    AnnotationConfigApplicationContext context(CountingTransactions transactions) throws Exception {
        var context=new AnnotationConfigApplicationContext();context.register(Transactions.class);
        context.registerBean("transactionManager",PlatformTransactionManager.class,()->transactions);
        Set<Class<?>> dependencies=new HashSet<>(),contracts=new HashSet<>();
        for(String name:SERVICES) {
            Class<?> implementation=Class.forName("com.city.service.impl."+name+"Impl");
            contracts.add(Class.forName("com.city.service."+name));context.register(implementation);
            for(var field:implementation.getDeclaredFields()) if(field.isAnnotationPresent(Autowired.class)) dependencies.add(field.getType());
            if(implementation.getDeclaredConstructors().length==1)
                dependencies.addAll(Arrays.asList(implementation.getDeclaredConstructors()[0].getParameterTypes()));
        }
        dependencies.removeAll(contracts);
        for(Class<?> type:dependencies) registerDependency(context,type);
        context.refresh();return context;
    }
    private <T> void registerDependency(AnnotationConfigApplicationContext context,Class<T> type) {
        context.getBeanFactory().registerSingleton(type.getName(),type==Clock.class?type.cast(Clock.systemUTC()):mock(type));
    }
    @Test void interfacesResolveAndTransactionalServicesRemainProxied() throws Exception {
        try(var context=context(new CountingTransactions())) {
            for(String name:SERVICES) assertThat(context.getBean(Class.forName("com.city.service."+name))).isNotNull();
            for(String name:new String[]{"AccountOperationService","ManagementMessageService","RefundService","StaffAccountService","StaffProfileService","UserProfileService"})
                assertThat(AopUtils.isJdkDynamicProxy(context.getBean(Class.forName("com.city.service."+name)))).as(name).isTrue();
        }
    }
    @Test void mandatoryAuditAndMessagesCannotWriteOutsideTransactions() throws Exception {
        var transactions=new CountingTransactions();
        try(var context=context(transactions)) {
            assertThatThrownBy(()->context.getBean(AccountOperationService.class).record(1L,2L,"DISABLE","test")).isInstanceOf(IllegalTransactionStateException.class);
            assertThatThrownBy(()->context.getBean(ManagementMessageService.class).publish("PLATFORM_ADMIN",null,"test","title","body","/")).isInstanceOf(IllegalTransactionStateException.class);
            verifyNoInteractions(context.getBean(UserMapper.class),context.getBean(AccountOperationMapper.class));
        }
    }
    @Test void requiredRefundTransactionRollsBackBusinessFailureThroughInterface() throws Exception {
        var transactions=new CountingTransactions();
        try(var context=context(transactions)) {
            assertThatThrownBy(()->context.getBean(RefundService.class).apply(1L,2L," ")).isInstanceOf(RefundException.class);
            assertThat(transactions.begun).isEqualTo(1);assertThat(transactions.rolledBack).isEqualTo(1);assertThat(transactions.committed).isZero();
        }
    }
}
