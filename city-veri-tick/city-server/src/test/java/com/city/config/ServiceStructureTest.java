package com.city.config;
import com.city.service.AccountOperationService;
import com.city.service.ManagementDashboardService;
import com.city.service.ManagementMessageService;
import com.city.service.RefundService;
import com.city.service.SmsCodeService;
import com.city.service.StaffAccountService;
import com.city.service.StaffProfileService;
import com.city.service.UserProfileService;
import com.city.cache.PublicActivityCache;
import com.city.service.support.ManagementOrderSupport;
import com.city.integration.sms.DevelopmentSmsCodeSender;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;
class ServiceStructureTest {
 @Test void businessServicesHaveInterfacesAndImplementations() throws Exception {
  for(String name:new String[]{"AccountOperationService","ManagementDashboardService","ManagementMessageService","RefundService","SmsCodeService","StaffAccountService","StaffProfileService","UserProfileService"}) {
   Class<?> api=Class.forName("com.city.service."+name);
   Class<?> impl=Class.forName("com.city.service.impl."+name+"Impl");
   assertThat(api.isInterface()).as(name).isTrue();assertThat(api.isAssignableFrom(impl)).as(name).isTrue();
  }
 }
 @Test void controllersAndHandlersDoNotDependOnImplementations() throws Exception {
  for(String dir:new String[]{"controller","handler"}) try(var files=Files.walk(Path.of("src/main/java/com/city",dir))) {
   for(Path file:files.filter(p->p.toString().endsWith(".java")).toList())
    assertThat(Files.readString(file)).as(file.toString()).doesNotContain("com.city.service.impl.");
  }
 }
 @Test void componentPackagesExpressTheirResponsibilities() throws Exception {
  assertThat(Class.forName("com.city.cache.PublicActivityCache")).isNotNull();
  assertThat(Class.forName("com.city.service.support.ManagementOrderSupport")).isNotNull();
  assertThat(Class.forName("com.city.integration.sms.DevelopmentSmsCodeSender")).isNotNull();
  try(var files=Files.list(Path.of("src/main/java/com/city/service/impl"))) {
   assertThat(files.filter(p->p.toString().endsWith(".java")).map(p->p.getFileName().toString()).toList()).allMatch(name->name.endsWith("ServiceImpl.java"));
  }
 }
}
