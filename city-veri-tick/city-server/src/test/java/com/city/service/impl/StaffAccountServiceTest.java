package com.city.service.impl;
import com.city.service.StaffAccountService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.city.dto.ChangePasswordDTO;
import com.city.dto.CreateOrganizerDTO;
import com.city.security.RevocationReason;

import com.city.dto.StaffAccountDTO;
import com.city.dto.VerifierActivitiesDTO;
import com.city.entity.*;
import com.city.mapper.*;
import com.city.exception.BusinessException;
import com.city.service.OrganizerAccessService;
import com.city.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StaffAccountServiceTest {
    final StaffAccountService service = new StaffAccountServiceImpl();
    final UserMapper users=mock(UserMapper.class);
    final OrganizerMapper organizers=mock(OrganizerMapper.class);
    final OrganizerMemberMapper members=mock(OrganizerMemberMapper.class);
    final ActivityMapper activities=mock(ActivityMapper.class);
    final ActivityVerifierMapper assignments=mock(ActivityVerifierMapper.class);
    final OrganizerAccessService access=mock(OrganizerAccessService.class);
    final PasswordEncoder encoder=mock(PasswordEncoder.class);
    final TokenBlacklistService tokens=mock(TokenBlacklistService.class);
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service,"users",users); ReflectionTestUtils.setField(service,"organizers",organizers);
        ReflectionTestUtils.setField(service,"members",members); ReflectionTestUtils.setField(service,"activities",activities);
        ReflectionTestUtils.setField(service,"assignments",assignments); ReflectionTestUtils.setField(service,"access",access);
        ReflectionTestUtils.setField(service,"encoder",encoder); ReflectionTestUtils.setField(service,"tokens",tokens);
        when(access.requireOrganizerId(3L)).thenReturn(10L);
        Organizer org=new Organizer();org.setId(10L);org.setStatus("APPROVED");when(organizers.selectById(10L)).thenReturn(org);
        when(users.selectRoleId("VERIFIER")).thenReturn(4L);
        when(encoder.encode("Initial123!")).thenReturn("bcrypt-hash");
        when(users.insert(any(User.class))).thenAnswer(i->{i.<User>getArgument(0).setId(8L);return 1;});
    }
    StaffAccountDTO dto() {var d=new StaffAccountDTO();d.setUsername("verify_one");d.setName("检票员");d.setInitialPassword("Initial123!");return d;}
    @Test void createsScopedVerifierWithHashedPasswordAndMandatoryChange() {
        assertThat(service.createVerifier(3L,dto())).isEqualTo(8L);
        verify(users).insert(argThat((User u)->u.getMustChangePassword() && "bcrypt-hash".equals(u.getPasswordHash())));
        verify(users).insertRole(8L,4L);
        verify(members).insert(argThat((OrganizerMember m)->m.getOrganizerId().equals(10L) && m.getUserId().equals(8L)));
    }
    @Test void duplicateUsernameDoesNotWriteAccountOrMembership() {
        when(users.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(()->service.createVerifier(3L,dto())).isInstanceOf(BusinessException.class);
        verify(users,never()).insert(any(User.class));verifyNoInteractions(members);
    }
    @Test void foreignVerifierCannotBeDisabled() {
        when(members.selectOne(any())).thenReturn(null);
        assertThatThrownBy(()->service.setVerifierEnabled(3L,8L,false)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(tokens);verify(users,never()).updateById(any(User.class));
    }
    @Test void foreignActivityCannotReplaceExistingPermissions() {
        OrganizerMember member=new OrganizerMember();member.setOrganizerId(10L);member.setUserId(8L);
        when(members.selectOne(any())).thenReturn(member);when(users.countRole(8L,"VERIFIER")).thenReturn(1L);
        Activity activity=new Activity();activity.setId(20L);activity.setOrganizerId(11L);when(activities.selectById(20L)).thenReturn(activity);
        VerifierActivitiesDTO d=new VerifierActivitiesDTO();d.setActivityIds(List.of(20L));
        assertThatThrownBy(()->service.setVerifierActivities(3L,8L,d)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(assignments);
    }
    @Test void selectedOwnActivitiesReplacePermissionsWithoutDeletingCheckinHistory() {
        OrganizerMember m=new OrganizerMember();m.setOrganizerId(10L);m.setUserId(8L);
        when(members.selectOne(any())).thenReturn(m);when(users.countRole(8L,"VERIFIER")).thenReturn(1L);
        Activity a=new Activity();a.setId(20L);a.setOrganizerId(10L);
        when(activities.selectById(20L)).thenReturn(a);when(activities.selectList(any())).thenReturn(List.of(a));
        var d=new VerifierActivitiesDTO();d.setActivityIds(List.of(20L,20L));
        service.setVerifierActivities(3L,8L,d);
        verify(assignments).update(isNull(),any(Wrapper.class));
        verify(assignments,times(1)).insert(argThat((ActivityVerifier v)->v.getUserId().equals(8L)&&v.getActivityId().equals(20L)&&v.getEnabled()));
        verify(assignments,never()).delete(any(Wrapper.class));
    }
    @Test void platformProvisioningCreatesOrganizationAndAdministratorTogether() {
        when(organizers.insert(any(Organizer.class))).thenAnswer(i->{i.<Organizer>getArgument(0).setId(10L);return 1;});
        when(users.selectRoleId("ORGANIZER_ADMIN")).thenReturn(2L);
        var d=new CreateOrganizerDTO();d.setName("城市文旅");d.setContactName("负责人");d.setContactPhone("13800138000");d.setAdministrator(dto());
        assertThat(service.createOrganizer(1L,d)).isEqualTo(10L);
        verify(organizers).insert(argThat((Organizer o)->"APPROVED".equals(o.getStatus())&&o.getReviewedBy().equals(1L)));
        verify(users).insertRole(8L,2L);
        verify(members).insert(argThat((OrganizerMember m)->m.getOrganizerId().equals(10L)&&"OWNER".equals(m.getMemberRole())));
    }
    @Test void duplicatePhoneDoesNotWriteAccount() {
        var d=dto();d.setPhone("13800138000");
        when(users.selectByPhone(d.getPhone())).thenReturn(new User());
        assertThatThrownBy(()->service.createVerifier(3L,d)).isInstanceOf(BusinessException.class);
        verify(users,never()).insert(any(User.class));
    }
    @Test void correctPasswordRevokesSessionsAndStoresNewHash() {
        var d=new ChangePasswordDTO();d.setCurrentPassword("Initial123!");d.setNewPassword("Personal123!");
        User u=new User();u.setId(8L);u.setEnabled(true);u.setPasswordHash("old-hash");when(users.selectByIdForUpdate(8L)).thenReturn(u);
        when(encoder.matches(d.getCurrentPassword(),"old-hash")).thenReturn(true);
        when(encoder.encode(d.getNewPassword())).thenReturn("new-hash");when(users.updateById(any(User.class))).thenReturn(1);
        service.changePassword(8L,d);
        verify(tokens).revokeAll(8L,RevocationReason.LOGOUT);
        verify(users).updateById(argThat((User p)->"new-hash".equals(p.getPasswordHash()) && !p.getMustChangePassword()));
    }
    @Test void wrongCurrentPasswordCannotChangePasswordOrRevokeSessions() {
        var d=new ChangePasswordDTO();d.setCurrentPassword("wrong");d.setNewPassword("Personal123!");
        User u=new User();u.setEnabled(true);u.setPasswordHash("old-hash");when(users.selectByIdForUpdate(8L)).thenReturn(u);
        assertThatThrownBy(()->service.changePassword(8L,d)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(tokens);verify(users,never()).updateById(any(User.class));
    }
}
