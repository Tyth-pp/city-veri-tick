-- City Veri Tick complete initialization script. MySQL 8.0+.
CREATE DATABASE IF NOT EXISTS `city_veri_tick` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `city_veri_tick`;

CREATE TABLE `city_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `username` VARCHAR(64) NOT NULL, `password_hash` VARCHAR(100) NOT NULL,
  `phone` VARCHAR(20) NULL, `nickname` VARCHAR(64) NULL, `real_name` VARCHAR(32) NULL, `id_card_hash` VARCHAR(100) NULL, `id_card_masked` VARCHAR(18) NULL, `avatar_file_id` BIGINT NULL, `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `must_change_password` TINYINT(1) NOT NULL DEFAULT 0,
  `staff_name` VARCHAR(32) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_user_username` (`username`), UNIQUE KEY `uk_user_phone` (`phone`)
) ENGINE=InnoDB COMMENT='Unified accounts';

CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `code` VARCHAR(32) NOT NULL, `name` VARCHAR(64) NOT NULL,
  `scope` ENUM('PLATFORM','ORGANIZER','ACTIVITY') NOT NULL, `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB;

CREATE TABLE `sys_user_role` (
  `user_id` BIGINT NOT NULL, `role_id` BIGINT NOT NULL, `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`,`role_id`),
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `city_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `organizer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `name` VARCHAR(128) NOT NULL, `unified_social_credit_code` VARCHAR(32) NULL,
  `contact_name` VARCHAR(64) NOT NULL, `contact_phone` VARCHAR(20) NOT NULL,
  `status` ENUM('PENDING','APPROVED','REJECTED','DISABLED') NOT NULL DEFAULT 'PENDING',
  `reviewed_by` BIGINT NULL, `reviewed_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_organizer_credit` (`unified_social_credit_code`), KEY `idx_organizer_status` (`status`),
  CONSTRAINT `fk_organizer_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `city_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE `organizer_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `organizer_id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL,
  `member_role` ENUM('OWNER','OPERATOR','FINANCE','VERIFIER_MANAGER','VERIFIER') NOT NULL DEFAULT 'OPERATOR', `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (`id`),
  UNIQUE KEY `uk_organizer_member` (`organizer_id`,`user_id`), KEY `idx_organizer_member_user` (`user_id`),
  CONSTRAINT `fk_om_organizer` FOREIGN KEY (`organizer_id`) REFERENCES `organizer` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_om_user` FOREIGN KEY (`user_id`) REFERENCES `city_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `organizer_id` BIGINT NOT NULL, `title` VARCHAR(200) NOT NULL,
  `category` VARCHAR(32) NULL, `description` TEXT NULL, `content` TEXT NULL, `highlights` TEXT NULL, `notices` TEXT NULL,
  `cover_file_id` BIGINT NULL, `venue_name` VARCHAR(200) NULL, `venue_address` VARCHAR(500) NULL,
  `status` ENUM('DRAFT','PENDING','PUBLISHED','REJECTED','OFFLINE','CANCELLED') NOT NULL DEFAULT 'DRAFT', `created_by` BIGINT NULL,
  `submitted_by` BIGINT NULL, `submitted_at` DATETIME NULL,
  `rejection_reason` VARCHAR(500) NULL, `reviewed_by` BIGINT NULL, `reviewed_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), KEY `idx_activity_org_status` (`organizer_id`,`status`),
  CONSTRAINT `fk_activity_organizer` FOREIGN KEY (`organizer_id`) REFERENCES `organizer` (`id`),
  CONSTRAINT `fk_activity_creator` FOREIGN KEY (`created_by`) REFERENCES `city_user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_activity_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `city_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE `activity_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `activity_id` BIGINT NOT NULL, `start_time` DATETIME NOT NULL, `end_time` DATETIME NOT NULL,
  `sale_start_time` DATETIME NULL, `sale_end_time` DATETIME NULL,
  `capacity` INT NOT NULL, `sold_count` INT NOT NULL DEFAULT 0, `status` ENUM('DRAFT','ON_SALE','CLOSED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), KEY `idx_session_activity_time` (`activity_id`,`start_time`),
  CONSTRAINT `fk_session_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_session_capacity` CHECK (`capacity` > 0), CONSTRAINT `chk_session_time` CHECK (`end_time` > `start_time`)
) ENGINE=InnoDB;

CREATE TABLE `ticket_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `session_id` BIGINT NOT NULL, `name` VARCHAR(100) NOT NULL,
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00, `stock` INT NOT NULL, `locked_stock` INT NOT NULL DEFAULT 0,
  `sale_start_time` DATETIME NULL, `sale_end_time` DATETIME NULL, `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), KEY `idx_ticket_type_session` (`session_id`),
  CONSTRAINT `fk_ticket_type_session` FOREIGN KEY (`session_id`) REFERENCES `activity_session` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_ticket_stock` CHECK (`stock` >= 0), CONSTRAINT `chk_ticket_price` CHECK (`price` >= 0)
) ENGINE=InnoDB;

CREATE TABLE `ticket_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `order_no` VARCHAR(32) NOT NULL, `user_id` BIGINT NOT NULL, `idempotency_key` VARCHAR(128) NULL, `coupon_id` BIGINT NULL, `real_name` VARCHAR(32) NULL, `id_card_hash` VARCHAR(100) NULL, `id_card_masked` VARCHAR(18) NULL, `ticket_type_id` BIGINT NOT NULL,
  `quantity` INT NOT NULL, `amount` DECIMAL(10,2) NOT NULL, `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00, `pay_amount` DECIMAL(10,2) NOT NULL,
  `status` ENUM('PENDING_PAYMENT','PAID','CANCELLED','REFUNDING','REFUNDED','EXPIRED') NOT NULL DEFAULT 'PENDING_PAYMENT',
  `expired_at` DATETIME NULL, `paid_at` DATETIME NULL, `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_order_no` (`order_no`), UNIQUE KEY `uk_order_user_idempotency` (`user_id`,`idempotency_key`), KEY `idx_order_user_time` (`user_id`,`created_at`), KEY `idx_order_status_expire` (`status`,`expired_at`), KEY `idx_order_paid_time` (`paid_at`),
  KEY `idx_order_coupon` (`coupon_id`), CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `city_user` (`id`), CONSTRAINT `fk_order_type` FOREIGN KEY (`ticket_type_id`) REFERENCES `ticket_type` (`id`),
  CONSTRAINT `chk_order_quantity` CHECK (`quantity` > 0)
) ENGINE=InnoDB;

CREATE TABLE `ticket` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `ticket_no` VARCHAR(32) NOT NULL, `order_id` BIGINT NOT NULL, `ticket_type_id` BIGINT NOT NULL,
  `verification_code` VARCHAR(64) NOT NULL, `status` ENUM('UNUSED','CHECKED_IN','REFUNDED') NOT NULL DEFAULT 'UNUSED',
  `issued_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_ticket_no` (`ticket_no`), UNIQUE KEY `uk_ticket_code` (`verification_code`), KEY `idx_ticket_order` (`order_id`), KEY `idx_ticket_issued_time` (`issued_at`),
  CONSTRAINT `fk_ticket_order` FOREIGN KEY (`order_id`) REFERENCES `ticket_order` (`id`), CONSTRAINT `fk_ticket_type` FOREIGN KEY (`ticket_type_id`) REFERENCES `ticket_type` (`id`)
) ENGINE=InnoDB;

-- Run once per database; safe to run again. No historical refund records are fabricated.
CREATE TABLE IF NOT EXISTS refund_request (
 id BIGINT NOT NULL AUTO_INCREMENT,
 order_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 amount DECIMAL(10,2) NOT NULL,
 status VARCHAR(16) NOT NULL,
 reason VARCHAR(500) NOT NULL,
 review_reason VARCHAR(500) NULL,
 reviewer_id BIGINT NULL,
 reviewer_name VARCHAR(32) NULL,
 reviewer_username VARCHAR(64) NULL,
 payment_mode VARCHAR(16) NOT NULL DEFAULT 'SIMULATED',
 refund_no VARCHAR(40) NULL,
 requested_at DATETIME NOT NULL,
 processed_at DATETIME NULL,
 PRIMARY KEY(id),
 UNIQUE KEY uk_refund_no(refund_no),
 KEY idx_refund_order_time(order_id,requested_at),
 KEY idx_refund_status_time(status,requested_at),
 KEY idx_refund_processed_time(status,processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE `activity_verifier` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `activity_id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL, `assigned_by` BIGINT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1, `valid_from` DATETIME NULL, `valid_until` DATETIME NULL, `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_activity_verifier` (`activity_id`,`user_id`),
  CONSTRAINT `fk_av_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_av_user` FOREIGN KEY (`user_id`) REFERENCES `city_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_av_assigner` FOREIGN KEY (`assigned_by`) REFERENCES `city_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE `check_in_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `ticket_id` BIGINT NOT NULL, `verifier_id` BIGINT NOT NULL, `device_code` VARCHAR(64) NULL,
  `checked_in_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (`id`), UNIQUE KEY `uk_checkin_ticket` (`ticket_id`), KEY `idx_checkin_verifier_time` (`verifier_id`,`checked_in_at`), KEY `idx_checkin_time` (`checked_in_at`),
  CONSTRAINT `fk_checkin_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `ticket` (`id`), CONSTRAINT `fk_checkin_verifier` FOREIGN KEY (`verifier_id`) REFERENCES `city_user` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `file_object` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `storage_provider` VARCHAR(32) NOT NULL DEFAULT 'OSS', `bucket_name` VARCHAR(128) NOT NULL, `object_key` VARCHAR(512) NOT NULL,
  `original_name` VARCHAR(255) NOT NULL, `content_type` VARCHAR(100) NULL, `size_bytes` BIGINT NOT NULL, `uploaded_by` BIGINT NULL, `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_file_object` (`bucket_name`,`object_key`), CONSTRAINT `fk_file_uploader` FOREIGN KEY (`uploaded_by`) REFERENCES `city_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE `outbox_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `event_id` CHAR(36) NOT NULL, `aggregate_type` VARCHAR(64) NOT NULL, `aggregate_id` BIGINT NOT NULL,
  `event_type` VARCHAR(64) NOT NULL, `payload` JSON NOT NULL, `status` ENUM('PENDING','SENT','FAILED') NOT NULL DEFAULT 'PENDING', `retry_count` INT NOT NULL DEFAULT 0,
  `next_retry_at` DATETIME NULL, `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `sent_at` DATETIME NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_outbox_event` (`event_id`), KEY `idx_outbox_dispatch` (`status`,`next_retry_at`)
) ENGINE=InnoDB;

CREATE TABLE `scheduled_job_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `job_name` VARCHAR(100) NOT NULL, `triggered_at` DATETIME NOT NULL, `finished_at` DATETIME NULL,
  `status` ENUM('RUNNING','SUCCESS','FAILED') NOT NULL, `processed_count` INT NOT NULL DEFAULT 0, `error_message` VARCHAR(1000) NULL,
  PRIMARY KEY (`id`), KEY `idx_job_log_time` (`job_name`,`triggered_at`)
) ENGINE=InnoDB;

CREATE TABLE account_operation (
 id BIGINT NOT NULL AUTO_INCREMENT,
 operator_id BIGINT NOT NULL,
 operator_name VARCHAR(100) NULL,
 operator_username VARCHAR(100) NULL,
 target_user_id BIGINT NOT NULL,
 operation VARCHAR(32) NOT NULL,
 reason VARCHAR(500) NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id),KEY idx_account_operation_target_time(target_user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT, `operator_id` BIGINT NULL, `operation` VARCHAR(100) NOT NULL, `target_type` VARCHAR(50) NULL, `target_id` BIGINT NULL,
  `request_ip` VARCHAR(45) NULL, `detail` JSON NULL, `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`), KEY `idx_log_operator_time` (`operator_id`,`created_at`), CONSTRAINT `fk_log_operator` FOREIGN KEY (`operator_id`) REFERENCES `city_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB;

INSERT INTO `sys_role` (`code`,`name`,`scope`) VALUES
 ('PLATFORM_ADMIN','Platform administrator','PLATFORM'), ('ORGANIZER_ADMIN','Organizer administrator','ORGANIZER'),
 ('VERIFIER','Ticket verifier','ACTIVITY'), ('USER','Customer','PLATFORM');

CREATE TABLE IF NOT EXISTS management_message (
 id BIGINT NOT NULL AUTO_INCREMENT,
 audience VARCHAR(32) NOT NULL,
 organizer_id BIGINT NULL,
 event_type VARCHAR(40) NOT NULL,
 title VARCHAR(100) NOT NULL,
 content VARCHAR(1000) NOT NULL,
 target_path VARCHAR(255) NOT NULL,
 created_at DATETIME(3) NOT NULL,
 PRIMARY KEY(id), KEY idx_message_audience_time(audience,organizer_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS management_message_read (
 message_id BIGINT NOT NULL,user_id BIGINT NOT NULL,read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(message_id,user_id), KEY idx_message_reader(user_id,message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS coupon (id BIGINT NOT NULL AUTO_INCREMENT,name VARCHAR(64) NOT NULL,description VARCHAR(255) NULL,discount_amount DECIMAL(10,2) NOT NULL,min_order_amount DECIMAL(10,2) NOT NULL DEFAULT 0,total_quantity INT NOT NULL,claimed_quantity INT NOT NULL DEFAULT 0,status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',starts_at DATETIME NOT NULL,ends_at DATETIME NOT NULL,created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,PRIMARY KEY(id),KEY idx_coupon_status_time(status,starts_at,ends_at),CHECK(discount_amount>=0),CHECK(min_order_amount>=0),CHECK(total_quantity>=0),CHECK(claimed_quantity>=0 AND claimed_quantity<=total_quantity)) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS user_coupon (id BIGINT NOT NULL AUTO_INCREMENT,user_id BIGINT NOT NULL,coupon_id BIGINT NOT NULL,status VARCHAR(16) NOT NULL,order_id BIGINT NULL,locked_at DATETIME NULL,used_at DATETIME NULL,created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,PRIMARY KEY(id),UNIQUE KEY uk_user_coupon(user_id,coupon_id),KEY idx_user_coupon_status(user_id,status),CONSTRAINT fk_user_coupon_user FOREIGN KEY(user_id) REFERENCES city_user(id),CONSTRAINT fk_user_coupon_coupon FOREIGN KEY(coupon_id) REFERENCES coupon(id),CONSTRAINT fk_user_coupon_order FOREIGN KEY(order_id) REFERENCES ticket_order(id)) ENGINE=InnoDB;

SET @has_coupon_type = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='coupon' AND COLUMN_NAME='coupon_type');
SET @add_coupon_type_sql = IF(@has_coupon_type = 0, 'ALTER TABLE coupon ADD COLUMN coupon_type VARCHAR(24) NOT NULL DEFAULT ''FULL_REDUCTION'' AFTER name, ADD COLUMN discount_rate DECIMAL(5,4) NULL AFTER discount_amount', 'SELECT 1');
PREPARE add_coupon_type_stmt FROM @add_coupon_type_sql; EXECUTE add_coupon_type_stmt; DEALLOCATE PREPARE add_coupon_type_stmt;
CREATE TABLE IF NOT EXISTS coupon_category_scope (id BIGINT NOT NULL AUTO_INCREMENT,coupon_id BIGINT NOT NULL,category_code VARCHAR(32) NOT NULL,PRIMARY KEY(id),UNIQUE KEY uk_coupon_category(coupon_id,category_code),CONSTRAINT fk_coupon_scope_coupon FOREIGN KEY(coupon_id) REFERENCES coupon(id) ON DELETE CASCADE) ENGINE=InnoDB;

-- Coupon rules upgrade. Idempotent; existing claim windows become use windows.
SET @coupon_legacy_cap = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='coupon' AND COLUMN_NAME='max_discount_amount')=0,1,0);
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='coupon' AND COLUMN_NAME='max_discount_amount')=0, 'ALTER TABLE coupon ADD COLUMN max_discount_amount DECIMAL(10,2) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='coupon' AND COLUMN_NAME='min_pay_amount')=0, 'ALTER TABLE coupon ADD COLUMN min_pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0.01', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='coupon' AND COLUMN_NAME='per_user_limit')=0, 'ALTER TABLE coupon ADD COLUMN per_user_limit INT NOT NULL DEFAULT 1', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='coupon' AND COLUMN_NAME='scope_type')=0, 'ALTER TABLE coupon ADD COLUMN scope_type VARCHAR(16) NOT NULL DEFAULT ''ALL''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='coupon' AND COLUMN_NAME='activity_id')=0, 'ALTER TABLE coupon ADD COLUMN activity_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='coupon' AND COLUMN_NAME='refund_policy')=0, 'ALTER TABLE coupon ADD COLUMN refund_policy VARCHAR(16) NOT NULL DEFAULT ''RETURN''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='coupon' AND COLUMN_NAME='use_starts_at')=0, 'ALTER TABLE coupon ADD COLUMN use_starts_at DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='coupon' AND COLUMN_NAME='use_ends_at')=0, 'ALTER TABLE coupon ADD COLUMN use_ends_at DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE coupon SET use_starts_at=starts_at WHERE use_starts_at IS NULL;
UPDATE coupon SET use_ends_at=ends_at WHERE use_ends_at IS NULL;
UPDATE coupon SET coupon_type=IF(min_order_amount>0,'FULL_REDUCTION','NO_THRESHOLD') WHERE coupon_type='NEW_USER';
UPDATE coupon SET coupon_type='NO_THRESHOLD' WHERE coupon_type='FULL_REDUCTION' AND min_order_amount=0;
UPDATE coupon SET coupon_type='FULL_REDUCTION' WHERE coupon_type='NO_THRESHOLD' AND min_order_amount>0;
UPDATE coupon SET max_discount_amount=discount_amount WHERE coupon_type='DISCOUNT' AND max_discount_amount IS NULL AND discount_amount>0 AND @coupon_legacy_cap=1;
UPDATE coupon SET status='INACTIVE' WHERE coupon_type='DISCOUNT' AND (discount_rate IS NULL OR (max_discount_amount IS NULL AND @coupon_legacy_cap=1));
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='user_coupon' AND INDEX_NAME='idx_user_coupon_lookup')=0, 'CREATE INDEX idx_user_coupon_lookup ON user_coupon(user_id,coupon_id,status)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='user_coupon' AND INDEX_NAME='uk_user_coupon_order')=0, 'CREATE UNIQUE INDEX uk_user_coupon_order ON user_coupon(order_id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='user_coupon' AND INDEX_NAME='uk_user_coupon')>0, 'ALTER TABLE user_coupon DROP INDEX uk_user_coupon', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Stage 8. Old coupons retain the ordinary database-backed claim flow.
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='coupon' AND COLUMN_NAME='claim_mode')=0, 'ALTER TABLE coupon ADD COLUMN claim_mode VARCHAR(16) NOT NULL DEFAULT ''NORMAL'' AFTER coupon_type', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- New coupon definitions require explicit activation; existing statuses are preserved.
ALTER TABLE coupon ALTER COLUMN status SET DEFAULT 'DRAFT';
