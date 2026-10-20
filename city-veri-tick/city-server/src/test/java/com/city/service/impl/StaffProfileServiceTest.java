package com.city.service.impl;
import com.city.service.StaffProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

import com.city.dto.StaffProfileDTO;
import com.city.entity.User;
import com.city.entity.OrganizerMember;
import com.city.mapper.*;
import com.city.service.OrganizerAccessService;
import com.city.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class StaffProfileServiceTest {
    private final StaffProfileService service=new StaffProfileServiceImpl();
    private final UserMapper users=mock(UserMapper.class);
    private final OrganizerMemberMapper members=mock(OrganizerMemberMapper.class);
    private final OrganizerAccessService access=mock(OrganizerAccessService.class);
    StaffProfileServiceTest(){ReflectionTestUtils.setField(service,"users",users);ReflectionTestUtils.setField(service,"members",members);ReflectionTestUtils.setField(service,"access",access);}
    private StaffProfileDTO dto(){var d=new StaffProfileDTO();d.setName(" 欧阳明 ");d.setPhone("13800138000");return d;}
    @Test void selfProfileUpdatesOnlyStaffNameAndPhonePreservingConsumerIdentity(){
        User u=new User();u.setId(7L);u.setEnabled(true);u.setRealName("观众实名");u.setNickname("昵称");
        when(users.selectByIdForUpdate(7L)).thenReturn(u);when(users.countRole(7L,"ORGANIZER_ADMIN")).thenReturn(1L);when(users.updateById(any(User.class))).thenReturn(1);
        service.updateSelf(7L,dto());
        verify(users).updateById(argThat((User patch)->"欧阳明".equals(patch.getStaffName())&&"13800138000".equals(patch.getPhone())&&patch.getRealName()==null&&patch.getNickname()==null&&patch.getPasswordHash()==null));
    }
    @Test void platformCannotEditAnotherPlatformAdministrator(){
        when(users.countRole(8L,"PLATFORM_ADMIN")).thenReturn(1L);
        assertThatThrownBy(()->service.updateManaged(1L,8L,dto())).isInstanceOf(BusinessException.class);
        verify(users,never()).updateById(any(User.class));
    }
    @Test void organizerCannotEditForeignVerifier(){
        when(access.requireOrganizerId(3L)).thenReturn(10L);when(users.countRole(8L,"VERIFIER")).thenReturn(1L);
        assertThatThrownBy(()->service.updateVerifier(3L,8L,dto())).isInstanceOf(BusinessException.class);
        verify(users,never()).updateById(any(User.class));
    }
    @Test void duplicatePhoneRejectsWithoutWriting(){
        User u=new User();u.setId(7L);u.setEnabled(true);when(users.selectByIdForUpdate(7L)).thenReturn(u);when(users.countRole(7L,"VERIFIER")).thenReturn(1L);
        User other=new User();other.setId(8L);when(users.selectByPhone("13800138000")).thenReturn(other);
        assertThatThrownBy(()->service.updateSelf(7L,dto())).isInstanceOf(BusinessException.class);verify(users,never()).updateById(any(User.class));
    }
    @Test void ordinaryConsumerCannotEditStaffProfile(){
        assertThatThrownBy(()->service.updateSelf(7L,dto())).isInstanceOf(BusinessException.class);verify(users,never()).updateById(any(User.class));
    }
    @Test void profileDoesNotInferNameFromNicknameOrConsumerIdentityOrExposeSecrets() throws Exception {
        User u=new User();u.setId(7L);u.setUsername("staff_one");u.setNickname("演示昵称");u.setRealName("购票实名");u.setPasswordHash("secret-hash");u.setEnabled(true);
        when(users.countRole(7L,"VERIFIER")).thenReturn(1L);when(users.selectById(7L)).thenReturn(u);when(users.selectOrganizations(7L)).thenReturn(List.of());
        var view=service.self(7L);assertThat(view.getName()).isNull();assertThat(view.getUsername()).isEqualTo("staff_one");
        String json=new ObjectMapper().writeValueAsString(view);
        assertThat(json).doesNotContain("secret-hash","购票实名","演示昵称","passwordHash","idCardHash");
    }
    @Test void organizerCanEditItsOwnVerifier(){
        when(access.requireOrganizerId(3L)).thenReturn(10L);when(users.countRole(8L,"VERIFIER")).thenReturn(1L);
        when(members.selectOne(any())).thenReturn(new OrganizerMember());User u=new User();u.setId(8L);when(users.selectByIdForUpdate(8L)).thenReturn(u);when(users.updateById(any(User.class))).thenReturn(1);
        service.updateVerifier(3L,8L,dto());verify(users).updateById(argThat((User p)->p.getId().equals(8L)&&"欧阳明".equals(p.getStaffName())));
    }
    @Test void platformCanMaintainOrganizerButOrganizerCannotEditAdministrator(){
        when(users.countRole(8L,"ORGANIZER_ADMIN")).thenReturn(1L);User u=new User();u.setId(8L);when(users.selectByIdForUpdate(8L)).thenReturn(u);when(users.updateById(any(User.class))).thenReturn(1);
        service.updateManaged(1L,8L,dto());verify(users).updateById(any(User.class));
        when(access.requireOrganizerId(3L)).thenReturn(10L);
        assertThatThrownBy(()->service.updateVerifier(3L,8L,dto())).isInstanceOf(BusinessException.class);
    }
}
