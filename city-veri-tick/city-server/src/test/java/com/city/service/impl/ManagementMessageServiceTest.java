package com.city.service.impl;

import com.city.mapper.ManagementMessageMapper;
import com.city.security.LoginPrincipal;
import com.city.service.OrganizerAccessService;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ManagementMessageServiceTest {
    @Test void organizerInboxAlwaysUsesAuthenticatedOrganization() {
        var mapper=mock(ManagementMessageMapper.class);var access=mock(OrganizerAccessService.class);
        when(access.requireOrganizerId(7L)).thenReturn(9L);
        var service=new ManagementMessageServiceImpl(mapper,access,Clock.systemUTC());
        service.inbox(new LoginPrincipal(7L,"ORGANIZER_ADMIN","j",Instant.now(),Instant.now()),1);
        verify(mapper).list("ORGANIZER_ADMIN",9L,7L,0);
    }
    @Test void userCannotReadManagementMessages() {
        var mapper=mock(ManagementMessageMapper.class);
        var service=new ManagementMessageServiceImpl(mapper,mock(OrganizerAccessService.class),Clock.systemUTC());
        assertThatThrownBy(()->service.inbox(new LoginPrincipal(7L,"USER","j",Instant.now(),Instant.now()),1)).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(mapper);
    }
    @Test void markingOtherOrganizationsMessageReadIsRejected() {
        var mapper=mock(ManagementMessageMapper.class);var access=mock(OrganizerAccessService.class);
        when(access.requireOrganizerId(7L)).thenReturn(9L);
        var service=new ManagementMessageServiceImpl(mapper,access,Clock.systemUTC());
        assertThatThrownBy(()->service.read(new LoginPrincipal(7L,"ORGANIZER_ADMIN","j",Instant.now(),Instant.now()),88L)).isInstanceOf(RuntimeException.class);
        verify(mapper,never()).markRead(anyLong(),anyLong());
    }
}
