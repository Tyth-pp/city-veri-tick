package com.city.service;

import com.city.dto.CreateTicketTypeDTO;
import com.city.dto.UpdateTicketTypeDTO;
import com.city.vo.TicketTypeViewVO;
import com.city.vo.TicketTypeInventoryVO;

import java.util.List;

public interface OrganizerTicketTypeService {
    /**
     * @param userId    当前登录用户ID
     * @param sessionId 场次ID
     * @return 票种列表
     * @apiNote 查询场次票种
     */
    List<TicketTypeViewVO> list(Long userId, Long sessionId);

    TicketTypeInventoryVO inventory(Long userId, Long ticketTypeId);

    /**
     * @param userId    当前登录用户ID
     * @param sessionId 场次ID
     * @param dto       创建参数
     * @return 创建的票种
     * @apiNote 创建票种
     */
    TicketTypeViewVO create(Long userId, Long sessionId, CreateTicketTypeDTO dto);

    /**
     * @param userId       当前登录用户ID
     * @param ticketTypeId 票种ID
     * @param dto          修改参数
     * @return 修改后的票种
     * @apiNote 修改票种
     */
    TicketTypeViewVO update(Long userId, Long ticketTypeId, UpdateTicketTypeDTO dto);

    /**
     * @param userId       当前登录用户ID
     * @param ticketTypeId 票种ID
     * @return 无返回值
     * @apiNote 删除票种
     */
    void delete(Long userId, Long ticketTypeId);
}
