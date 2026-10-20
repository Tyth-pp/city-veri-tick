package com.city.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.city.result.SystemResultCode;
import com.city.dto.CreateTicketTypeDTO;
import com.city.dto.UpdateTicketTypeDTO;
import com.city.entity.Activity;
import com.city.entity.ActivitySession;
import com.city.entity.TicketOrder;
import com.city.entity.TicketType;
import com.city.enums.ActivityStatus;
import com.city.enums.SessionStatus;
import com.city.exception.BusinessException;
import com.city.mapper.ActivityMapper;
import com.city.mapper.ActivitySessionMapper;
import com.city.mapper.TicketOrderMapper;
import com.city.mapper.TicketTypeMapper;
import com.city.service.OrganizerAccessService;
import com.city.service.OrganizerTicketTypeService;
import com.city.vo.TicketTypeViewVO;
import com.city.vo.TicketTypeInventoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class OrganizerTicketTypeServiceImpl implements OrganizerTicketTypeService {
    @Autowired
    private TicketTypeMapper ticketTypeMapper;
    @Autowired
    private TicketOrderMapper ticketOrderMapper;
    @Autowired
    private ActivitySessionMapper activitySessionMapper;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private OrganizerAccessService organizerAccessService;

    @Override
    public List<TicketTypeViewVO> list(Long userId, Long sessionId) {
        requireOwnedSession(userId, sessionId, false);
        return findBySession(sessionId).stream().map(TicketTypeViewVO::from).toList();
    }

    @Override
    public TicketTypeInventoryVO inventory(Long userId, Long ticketTypeId) {
        TicketType type = requireTicketType(ticketTypeId);
        requireOwnedSession(userId, type.getSessionId(), false);
        int stock = type.getStock() == null ? 0 : type.getStock();
        int locked = type.getLockedStock() == null ? 0 : type.getLockedStock();
        return new TicketTypeInventoryVO(type.getId(), stock, locked, stock + locked);
    }

    @Override
    @Transactional
    public TicketTypeViewVO create(Long userId, Long sessionId, CreateTicketTypeDTO dto) {
        ActivitySession session = requireOwnedSession(userId, sessionId, true);
        LocalDateTime saleStart = session.getSaleStartTime() != null ? session.getSaleStartTime() : dto.getSaleStartTime();
        LocalDateTime saleEnd = session.getSaleEndTime() != null ? session.getSaleEndTime() : dto.getSaleEndTime();
        validate(dto.getName(), dto.getPrice(), dto.getStock(), saleStart, saleEnd, session);
        List<TicketType> types = findBySession(sessionId);
        requireUniqueName(types, null, dto.getName());
        requireCapacity(types, null, dto.getStock(), session.getCapacity());
        TicketType type = new TicketType();
        type.setSessionId(sessionId);
        copy(dto.getName(), dto.getPrice(), dto.getStock(), saleStart, saleEnd, dto.getEnabled(), type);
        type.setLockedStock(0);
        ticketTypeMapper.insert(type);
        return TicketTypeViewVO.from(type);
    }

    @Override
    @Transactional
    public TicketTypeViewVO update(Long userId, Long ticketTypeId, UpdateTicketTypeDTO dto) {
        TicketType type = requireTicketType(ticketTypeId);
        ActivitySession session = requireOwnedSession(userId, type.getSessionId(), true);
        LocalDateTime saleStart = session.getSaleStartTime() != null ? session.getSaleStartTime() : dto.getSaleStartTime();
        LocalDateTime saleEnd = session.getSaleEndTime() != null ? session.getSaleEndTime() : dto.getSaleEndTime();
        validate(dto.getName(), dto.getPrice(), dto.getStock(), saleStart, saleEnd, session);
        List<TicketType> types = findBySession(session.getId());
        requireUniqueName(types, ticketTypeId, dto.getName());
        requireCapacity(types, ticketTypeId, dto.getStock(), session.getCapacity());
        copy(dto.getName(), dto.getPrice(), dto.getStock(), saleStart, saleEnd, dto.getEnabled(), type);
        ticketTypeMapper.updateById(type);
        return TicketTypeViewVO.from(type);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long ticketTypeId) {
        TicketType type = requireTicketType(ticketTypeId);
        requireOwnedSession(userId, type.getSessionId(), true);
        Long orderCount = ticketOrderMapper.selectCount(new QueryWrapper<TicketOrder>().eq("ticket_type_id", ticketTypeId));
        if (orderCount > 0) throw new BusinessException(SystemResultCode.TICKET_TYPE_HAS_ORDERS);
        ticketTypeMapper.deleteById(ticketTypeId);
    }

    private ActivitySession requireOwnedSession(Long userId, Long sessionId, boolean editable) {
        ActivitySession session = activitySessionMapper.selectById(sessionId);
        if (session == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        Activity activity = activityMapper.selectById(session.getActivityId());
        if (activity == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        Long organizerId = organizerAccessService.requireOrganizerId(userId);
        if (!organizerId.equals(activity.getOrganizerId())) throw new BusinessException(SystemResultCode.FORBIDDEN);
        if (editable && (session.getStatus() != SessionStatus.DRAFT
                || (activity.getStatus() != ActivityStatus.DRAFT && activity.getStatus() != ActivityStatus.REJECTED))) {
            throw new BusinessException(SystemResultCode.CONFLICT);
        }
        return session;
    }

    private TicketType requireTicketType(Long id) {
        TicketType type = ticketTypeMapper.selectById(id);
        if (type == null) throw new BusinessException(SystemResultCode.NOT_FOUND);
        return type;
    }

    private List<TicketType> findBySession(Long sessionId) {
        return ticketTypeMapper.selectList(new QueryWrapper<TicketType>().eq("session_id", sessionId).orderByAsc("id"));
    }

    private void validate(String name, BigDecimal price, Integer stock, LocalDateTime saleStart,
                          LocalDateTime saleEnd, ActivitySession session) {
        if (name == null || name.isBlank() || price == null || price.signum() < 0 || price.stripTrailingZeros().scale() > 2 || stock == null || stock < 0) {
            throw new BusinessException(SystemResultCode.TICKET_FIELDS_INVALID);
        }
        if (saleStart == null || saleEnd == null) throw new BusinessException(SystemResultCode.SALE_TIME_REQUIRED);
        if (!saleEnd.isAfter(saleStart)) throw new BusinessException(SystemResultCode.SALE_TIME_INVALID);
        if (saleEnd.isAfter(session.getStartTime())) throw new BusinessException(SystemResultCode.SALE_TIME_AFTER_SESSION);
    }

    private void requireUniqueName(List<TicketType> types, Long currentId, String name) {
        boolean duplicate = types.stream().anyMatch(type -> (currentId == null || !Objects.equals(type.getId(), currentId))
                && type.getName().equalsIgnoreCase(name.trim()));
        if (duplicate) throw new BusinessException(SystemResultCode.TICKET_TYPE_NAME_DUPLICATE);
    }

    private void requireCapacity(List<TicketType> types, Long currentId, int stock, int capacity) {
        int allocated = types.stream().filter(type -> currentId == null || !Objects.equals(type.getId(), currentId))
                .mapToInt(TicketType::getStock).sum();
        if (allocated + stock > capacity) throw new BusinessException(SystemResultCode.SESSION_CAPACITY_EXCEEDED);
    }

    private void copy(String name, BigDecimal price, Integer stock, LocalDateTime saleStart,
                      LocalDateTime saleEnd, Boolean enabled, TicketType type) {
        type.setName(name.trim());
        type.setPrice(price);
        type.setStock(stock);
        type.setSaleStartTime(saleStart);
        type.setSaleEndTime(saleEnd);
        type.setEnabled(enabled);
    }
}
