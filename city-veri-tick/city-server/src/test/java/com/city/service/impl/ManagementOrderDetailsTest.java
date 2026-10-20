package com.city.service.impl;
import com.city.service.RefundService;
import com.city.service.support.ManagementOrderSupport;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.city.enums.TicketStatus;
import java.time.LocalDateTime;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.city.entity.*;
import com.city.mapper.*;
import com.city.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import java.util.List;
class ManagementOrderDetailsTest {
 @Test void managementTicketsResolveActualCheckInOperatorAndTime(){
  TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"test"),CheckInRecord.class);
  var records=mock(CheckInRecordMapper.class);var users=mock(UserMapper.class);var ticket=new Ticket();ticket.setId(5L);ticket.setStatus(TicketStatus.CHECKED_IN);
  var record=new CheckInRecord();record.setVerifierId(8L);record.setCheckedInAt(LocalDateTime.of(2026,10,15,10,0));when(records.selectOne(any())).thenReturn(record);
  var operator=new User();operator.setStaffName("核销员张");operator.setUsername("verifier_zhang");when(users.selectById(8L)).thenReturn(operator);
  var result=ManagementOrderSupport.ticket(ticket,records,users);assertThat(result.getVerifierName()).isEqualTo("核销员张");assertThat(result.getCheckedInAt()).isEqualTo(record.getCheckedInAt());
 }
 @Test void platformDetailUsesOrderIdentitySnapshotInsteadOfCurrentUserProfile(){
  var service=new PlatformOrderServiceImpl();ReflectionTestUtils.setField(service,"refundService",mock(RefundService.class));var orders=mock(TicketOrderMapper.class);var types=mock(TicketTypeMapper.class);var sessions=mock(ActivitySessionMapper.class);var activities=mock(ActivityMapper.class);var tickets=mock(TicketMapper.class);var files=mock(FileStorageService.class);
  ReflectionTestUtils.setField(service,"orderMapper",orders);ReflectionTestUtils.setField(service,"ticketTypeMapper",types);ReflectionTestUtils.setField(service,"sessionMapper",sessions);ReflectionTestUtils.setField(service,"activityMapper",activities);ReflectionTestUtils.setField(service,"ticketMapper",tickets);ReflectionTestUtils.setField(service,"fileStorageService",files);
  var order=new TicketOrder();order.setId(1L);order.setTicketTypeId(2L);order.setRealName("购票时姓名");order.setIdCardMasked("110***********1234");when(orders.selectById(1L)).thenReturn(order);
  var type=new TicketType();type.setId(2L);type.setSessionId(3L);when(types.selectById(2L)).thenReturn(type);
  var session=new ActivitySession();session.setId(3L);session.setActivityId(4L);when(sessions.selectById(3L)).thenReturn(session);
  var activity=new Activity();activity.setId(4L);activity.setVenueName("城市剧场");when(activities.selectById(4L)).thenReturn(activity);when(tickets.selectList(any())).thenReturn(List.of());
  var result=service.detail(1L);assertThat(result.getRealName()).isEqualTo("购票时姓名");assertThat(result.getIdCardMasked()).isEqualTo("110***********1234");assertThat(result.getVenueName()).isEqualTo("城市剧场");
 }
}
