package com.city.controller.admin.organizer;

import com.city.dto.CreateTicketTypeDTO;
import com.city.dto.UpdateTicketTypeDTO;
import com.city.result.Result;
import com.city.security.LoginPrincipal;
import com.city.service.OrganizerTicketTypeService;
import com.city.vo.TicketTypeViewVO;
import com.city.vo.TicketTypeInventoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "主办方活动管理接口")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/controller/admin/organizer")
public class OrganizerTicketTypeController {
    @Autowired
    private OrganizerTicketTypeService organizerTicketTypeService;

    /**
     * @param user      当前登录用户
     * @param sessionId 场次ID
     * @return 场次票种列表
     * @apiNote 查询场次票种
     */
    @Operation(summary = "查询场次票种")
    @GetMapping("/sessions/{sessionId}/ticket-types")
    public Result<List<TicketTypeViewVO>> list(@AuthenticationPrincipal LoginPrincipal user,
                                               @PathVariable("sessionId") Long sessionId) {
        return Result.success(organizerTicketTypeService.list(user.userId(), sessionId));
    }

    /**
     * @param user         当前登录用户
     * @param ticketTypeId 票种ID
     * @return 票种库存信息
     * @apiNote 查询票种可售库存和待支付锁定库存
     */
    @Operation(summary = "查询票种库存")
    @GetMapping("/ticket-types/{ticketTypeId}/inventory")
    public Result<TicketTypeInventoryVO> inventory(@AuthenticationPrincipal LoginPrincipal user,
                                                   @PathVariable("ticketTypeId") Long ticketTypeId) {
        return Result.success(organizerTicketTypeService.inventory(user.userId(), ticketTypeId));
    }

    /**
     * @param user      当前登录用户
     * @param sessionId 场次ID
     * @param dto       创建票种请求参数
     * @return 创建的票种
     * @apiNote 创建场次票种
     */
    @Operation(summary = "创建场次票种")
    @PostMapping("/sessions/{sessionId}/ticket-types")
    public Result<TicketTypeViewVO> create(@AuthenticationPrincipal LoginPrincipal user,
                                           @PathVariable("sessionId") Long sessionId,
                                           @Valid @RequestBody CreateTicketTypeDTO dto) {
        return Result.success(organizerTicketTypeService.create(user.userId(), sessionId, dto));
    }

    /**
     * @param user         当前登录用户
     * @param ticketTypeId 票种ID
     * @param dto          修改票种请求参数
     * @return 修改后的票种
     * @apiNote 修改票种
     */
    @Operation(summary = "修改票种")
    @PutMapping("/ticket-types/{ticketTypeId}")
    public Result<TicketTypeViewVO> update(@AuthenticationPrincipal LoginPrincipal user,
                                           @PathVariable("ticketTypeId") Long ticketTypeId,
                                           @Valid @RequestBody UpdateTicketTypeDTO dto) {
        return Result.success(organizerTicketTypeService.update(user.userId(), ticketTypeId, dto));
    }

    /**
     * @param user         当前登录用户
     * @param ticketTypeId 票种ID
     * @return 操作结果
     * @apiNote 删除票种
     */
    @Operation(summary = "删除票种")
    @DeleteMapping("/ticket-types/{ticketTypeId}")
    public Result<Void> delete(@AuthenticationPrincipal LoginPrincipal user,
                               @PathVariable("ticketTypeId") Long ticketTypeId) {
        organizerTicketTypeService.delete(user.userId(), ticketTypeId);
        return Result.success();
    }
}
