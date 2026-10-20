package com.city.service.impl;

import com.city.result.SystemResultCode;
import com.city.entity.OrganizerMember;
import com.city.exception.BusinessException;
import com.city.mapper.OrganizerMemberMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrganizerAccessServiceImplTest {
    private final OrganizerMemberMapper mapper = mock(OrganizerMemberMapper.class);
    private final OrganizerAccessServiceImpl service = new OrganizerAccessServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "organizerMemberMapper", mapper);
    }

    @Test
    void returnsOrganizerIdForEnabledMembership() {
        OrganizerMember member = new OrganizerMember();
        member.setOrganizerId(20L);
        when(mapper.selectOne(any())).thenReturn(member);

        assertThat(service.requireOrganizerId(7L)).isEqualTo(20L);
    }

    @Test
    void rejectsUserWithoutEnabledMembership() {
        when(mapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.requireOrganizerId(7L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.resultCode()).isEqualTo(SystemResultCode.FORBIDDEN));
    }
}
