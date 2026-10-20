package com.city.mapper;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.*;

@Mapper
public interface ManagementDashboardMapper {
    @SelectProvider(type = Sql.class, method = "activity")
    Map<String, Object> activityMetrics(@Param("organizerId") Long organizerId);

    @SelectProvider(type = Sql.class, method = "payment")
    Map<String, Object> paymentMetrics(@Param("organizerId") Long organizerId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @SelectProvider(type = Sql.class, method = "refund")
    Map<String, Object> refundMetrics(@Param("organizerId") Long organizerId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @SelectProvider(type = Sql.class, method = "ticket")
    Map<String, Object> ticketMetrics(@Param("organizerId") Long organizerId);

    @SelectProvider(type = Sql.class, method = "trend")
    List<Map<String, Object>> trend(@Param("organizerId") Long organizerId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @SelectProvider(type = Sql.class, method = "categories")
    List<Map<String, Object>> categories(@Param("organizerId") Long organizerId);

    @SelectProvider(type = Sql.class, method = "tasks")
    List<Map<String, Object>> tasks(@Param("organizerId") Long organizerId);

    @SelectProvider(type = Sql.class, method = "sessions")
    List<Map<String, Object>> sessions(@Param("organizerId") Long organizerId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @SelectProvider(type = Sql.class, method = "ranking")
    List<Map<String, Object>> ranking(@Param("organizerId") Long organizerId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Select("SELECT CONCAT('account-',x.id) id,CASE x.operation WHEN 'DISABLE' THEN '停用账号' WHEN 'ENABLE' THEN '启用账号' WHEN 'FORCE_LOGOUT' THEN '强制下线' ELSE x.operation END title,COALESCE(x.operator_name,x.operator_username) operatorName,DATE_FORMAT(x.created_at,'%Y-%m-%dT%H:%i:%s') time,CONCAT('/management/platform/users?keyword=',x.target_user_id) targetPath FROM account_operation x ORDER BY x.created_at DESC LIMIT 8")
    List<Map<String, Object>> recentAccountOperations();

    @Select("SELECT CONCAT('review-',a.id) id,CONCAT(CASE a.status WHEN 'PUBLISHED' THEN '审核通过：' WHEN 'REJECTED' THEN '审核驳回：' ELSE '活动下架：' END,a.title) title,COALESCE(NULLIF(u.staff_name,''),u.username) operatorName,DATE_FORMAT(a.reviewed_at,'%Y-%m-%dT%H:%i:%s') time,CONCAT('/management/platform/activities/',a.id) targetPath FROM activity a LEFT JOIN city_user u ON u.id=a.reviewed_by WHERE a.reviewed_at IS NOT NULL AND a.status IN ('PUBLISHED','REJECTED','OFFLINE') ORDER BY a.reviewed_at DESC LIMIT 8")
    List<Map<String, Object>> recentActivityReviews();

    class Sql {
        private static final String ORDER = " FROM ticket_order o JOIN ticket_type tt ON tt.id=o.ticket_type_id JOIN activity_session s ON s.id=tt.session_id JOIN activity a ON a.id=s.activity_id WHERE 1=1 ";
        private static final String TICKET = " FROM ticket t JOIN ticket_order o ON o.id=t.order_id JOIN ticket_type tt ON tt.id=t.ticket_type_id JOIN activity_session s ON s.id=tt.session_id JOIN activity a ON a.id=s.activity_id WHERE 1=1 ";
        private static final String REFUND = " FROM refund_request r JOIN ticket_order o ON o.id=r.order_id JOIN ticket_type tt ON tt.id=o.ticket_type_id JOIN activity_session s ON s.id=tt.session_id JOIN activity a ON a.id=s.activity_id WHERE 1=1 ";

        private static String scope(Map<String, Object> p) {
            return p.get("organizerId") == null ? "" : " AND a.organizer_id=#{organizerId} ";
        }

        public static String activity(Map<String, Object> p) {
            return "SELECT COUNT(*) totalActivities,COALESCE(SUM(a.status='PENDING'),0) pendingActivities,COALESCE(SUM(a.status='DRAFT'),0) draftActivities,COALESCE(SUM(a.status='REJECTED'),0) rejectedActivities,COALESCE(SUM(a.status='PUBLISHED'),0) publishedActivities,"
                    + "COALESCE(SUM(a.status='PUBLISHED' AND EXISTS (SELECT 1 FROM activity_session s JOIN ticket_type tt ON tt.session_id=s.id WHERE s.activity_id=a.id AND s.status='ON_SALE' AND s.start_time>DATE_ADD(UTC_TIMESTAMP(),INTERVAL 8 HOUR) AND tt.enabled=1 AND tt.stock>0 AND tt.sale_start_time IS NOT NULL AND tt.sale_end_time>tt.sale_start_time AND tt.sale_start_time<=DATE_ADD(UTC_TIMESTAMP(),INTERVAL 8 HOUR) AND tt.sale_end_time>DATE_ADD(UTC_TIMESTAMP(),INTERVAL 8 HOUR))),0) sellingActivities FROM activity a WHERE 1=1 " + scope(p);
        }

        public static String payment(Map<String, Object> p) {
            return "SELECT COUNT(*) paidOrders,COALESCE(SUM(o.pay_amount),0) paidRevenue" + ORDER + scope(p) + " AND o.paid_at>=#{from} AND o.paid_at<#{to}";
        }

        public static String refund(Map<String, Object> p) {
            return "SELECT COALESCE(SUM(CASE WHEN r.status='REFUNDED' AND r.processed_at>=#{from} AND r.processed_at<#{to} THEN r.amount ELSE 0 END),0) refundAmount,COALESCE(SUM(r.status='PENDING'),0) pendingRefunds" + REFUND + scope(p);
        }

        public static String ticket(Map<String, Object> p) {
            return "SELECT COUNT(*) totalTickets,COALESCE(SUM(t.status='CHECKED_IN'),0) checkedInTickets,COALESCE(SUM(t.status='REFUNDED'),0) refundedTickets,COALESCE(SUM(t.status='UNUSED' AND o.status='PAID' AND s.end_time>DATE_ADD(UTC_TIMESTAMP(),INTERVAL 8 HOUR)),0) pendingUseTickets,COALESCE(SUM(t.status='UNUSED' AND s.end_time<=DATE_ADD(UTC_TIMESTAMP(),INTERVAL 8 HOUR)),0) endedUnusedTickets,COALESCE(SUM(t.status='UNUSED' AND o.status='REFUNDING'),0) frozenTickets,COALESCE(SUM(t.status IN ('UNUSED','CHECKED_IN') AND o.status IN ('PAID','REFUNDING','COMPLETED')),0) validTickets" + TICKET + scope(p);
        }

        public static String trend(Map<String, Object> p) {
            String sc = scope(p);
            return "SELECT day,SUM(payment) payment,SUM(refund) refund,SUM(issued) issued,SUM(checked) checked FROM ("
                    + "SELECT DATE(o.paid_at) day,SUM(o.pay_amount) payment,0 refund,0 issued,0 checked" + ORDER + sc + " AND o.paid_at>=#{from} AND o.paid_at<#{to} GROUP BY DATE(o.paid_at) UNION ALL "
                    + "SELECT DATE(r.processed_at),0,SUM(r.amount),0,0" + REFUND + sc + " AND r.status='REFUNDED' AND r.processed_at>=#{from} AND r.processed_at<#{to} GROUP BY DATE(r.processed_at) UNION ALL "
                    + "SELECT DATE(t.issued_at),0,0,COUNT(*),0" + TICKET + sc + " AND t.issued_at>=#{from} AND t.issued_at<#{to} GROUP BY DATE(t.issued_at) UNION ALL "
                    + "SELECT DATE(c.checked_in_at),0,0,0,COUNT(*) FROM check_in_record c JOIN ticket t ON t.id=c.ticket_id JOIN ticket_order o ON o.id=t.order_id JOIN ticket_type tt ON tt.id=t.ticket_type_id JOIN activity_session s ON s.id=tt.session_id JOIN activity a ON a.id=s.activity_id WHERE c.checked_in_at>=#{from} AND c.checked_in_at<#{to} " + sc + " GROUP BY DATE(c.checked_in_at)) x GROUP BY day ORDER BY day";
        }

        public static String categories(Map<String, Object> p) {
            return "SELECT COALESCE(NULLIF(a.category,''),'未分类') name,COUNT(*) count FROM activity a WHERE 1=1 " + scope(p) + " GROUP BY a.category ORDER BY count DESC";
        }

        public static String ranking(Map<String, Object> p) {
            return "SELECT activityId,title,SUM(payment) payment,SUM(checked) checked FROM (SELECT a.id activityId,a.title,SUM(o.pay_amount) payment,0 checked" + ORDER + scope(p) + " AND o.paid_at>=#{from} AND o.paid_at<#{to} GROUP BY a.id,a.title UNION ALL SELECT a.id,a.title,0,COUNT(*) FROM check_in_record c JOIN ticket t ON t.id=c.ticket_id JOIN ticket_type tt ON tt.id=t.ticket_type_id JOIN activity_session s ON s.id=tt.session_id JOIN activity a ON a.id=s.activity_id WHERE c.checked_in_at>=#{from} AND c.checked_in_at<#{to} " + scope(p) + " GROUP BY a.id,a.title) ranking GROUP BY activityId,title ORDER BY payment DESC,checked DESC LIMIT 5";
        }

        public static String tasks(Map<String, Object> p) {
            return "SELECT a.id,a.title,a.status,a.submitted_at submittedAt,a.rejection_reason rejectionReason,g.name organizerName,COALESCE(u.staff_name,u.nickname,u.username) submitterName FROM activity a LEFT JOIN organizer g ON g.id=a.organizer_id LEFT JOIN city_user u ON u.id=a.submitted_by WHERE a.status IN " + (p.get("organizerId") == null ? "('PENDING')" : "('DRAFT','REJECTED','PENDING')") + scope(p) + " ORDER BY CASE a.status WHEN 'REJECTED' THEN 0 WHEN 'PENDING' THEN 1 ELSE 2 END,a.submitted_at ASC,a.created_at DESC LIMIT 8";
        }

        public static String sessions(Map<String, Object> p) {
            return "SELECT s.id sessionId,a.id activityId,a.title,s.start_time startTime,s.end_time endTime,(SELECT COUNT(*) FROM activity_verifier av JOIN city_user u ON u.id=av.user_id WHERE av.activity_id=a.id AND av.enabled=1 AND u.enabled=1 AND (av.valid_from IS NULL OR av.valid_from<=s.start_time) AND (av.valid_until IS NULL OR av.valid_until>=s.start_time)) verifierCount FROM activity_session s JOIN activity a ON a.id=s.activity_id WHERE s.start_time>=#{from} AND s.start_time<#{to} AND a.status='PUBLISHED' " + scope(p) + " ORDER BY s.start_time LIMIT 20";
        }
    }
}
