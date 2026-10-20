export function inventorySaleStatus({activity,session,ticket,inventory},now=Date.now()) {
 if(!ticket.enabled)return '已停用'
 if(['OFFLINE','CANCELLED'].includes(activity.status))return '活动已下架或取消'
 if(activity.status!=='PUBLISHED')return '待活动发布'
 if(session.status==='CLOSED'||session.status==='CANCELLED')return '场次已关闭'
 if(session.startTime&&now>=Date.parse(session.startTime))return '已停售'
 if(session.status!=='ON_SALE')return '场次未开放'
 const start=Date.parse(ticket.saleStartTime),end=Date.parse(ticket.saleEndTime)
 if(!Number.isFinite(start)||!Number.isFinite(end)||end<=start)return '售票时间未配置'
 if(now<start)return '未到开售时间'
 if(now>=end)return '已停售'
 if(Number(inventory.stock)<=0)return '已售罄'
 return '销售中'
}
