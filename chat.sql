-- 建议：先建库
-- CREATE DATABASE xechat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- USE xechat;

-- 1) 用户
CREATE TABLE `user` (
  `user_id`        VARCHAR(64)  NOT NULL,
  `username`       VARCHAR(32)  NOT NULL,
  `address`        VARCHAR(128) NULL,
  `status`         TINYINT      NOT NULL DEFAULT 1,  -- 0离线 1在线
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_status` (`status`),
  KEY `idx_user_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2) 会话(消息主体里你列的 session 字段，更适合单独做会话表)
CREATE TABLE `session` (
  `session_id`       VARCHAR(64)  NOT NULL,
  `session_type`     TINYINT      NOT NULL,          -- 1私聊 2频道/群聊
  `session_name`     VARCHAR(64)  NULL,
  `creator_user_id`  VARCHAR(64)  NULL,
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`session_id`),
  KEY `idx_session_type` (`session_type`),
  KEY `idx_session_creator` (`creator_user_id`),
  KEY `idx_session_created_at` (`created_at`),
  CONSTRAINT `fk_session_creator_user`
    FOREIGN KEY (`creator_user_id`) REFERENCES `user` (`user_id`)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) 消息
CREATE TABLE `message` (
  `message_id`      VARCHAR(64)  NOT NULL,
  `session_id`      VARCHAR(64)  NOT NULL,
  `sender_user_id`  VARCHAR(64)  NOT NULL,
  `message_type`    TINYINT      NOT NULL,           -- 1文本 2图片 3文件 4系统 5撤回等(自行定义)
  `content`         TEXT         NULL,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`message_id`),
  KEY `idx_message_session_time` (`session_id`, `created_at`),
  KEY `idx_message_sender_time` (`sender_user_id`, `created_at`),
  CONSTRAINT `fk_message_session`
    FOREIGN KEY (`session_id`) REFERENCES `session` (`session_id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_message_sender_user`
    FOREIGN KEY (`sender_user_id`) REFERENCES `user` (`user_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4) 会话成员关系
CREATE TABLE `session_member` (
  `session_id`    VARCHAR(64) NOT NULL,
  `user_id`       VARCHAR(64) NOT NULL,
  `join_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `muted`         TINYINT     NOT NULL DEFAULT 0,     -- 0否 1是
  `top_pinned`    TINYINT     NOT NULL DEFAULT 0,     -- 0否 1是
  PRIMARY KEY (`session_id`, `user_id`),
  KEY `idx_member_user` (`user_id`),
  KEY `idx_member_join_time` (`join_time`),
  CONSTRAINT `fk_member_session`
    FOREIGN KEY (`session_id`) REFERENCES `session` (`session_id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_member_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5) 消息撤回记录表
CREATE TABLE `message_revoke` (
  `revoke_id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `message_id`        VARCHAR(64)  NOT NULL,
  `session_id`        VARCHAR(64)  NOT NULL,
  `message_type`      TINYINT      NOT NULL,
  `operator_user_id`  VARCHAR(64)  NOT NULL,
  `target_user_id`    VARCHAR(64)  NULL,
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`revoke_id`),
  UNIQUE KEY `uk_revoke_message` (`message_id`),
  KEY `idx_revoke_session_time` (`session_id`, `created_at`),
  KEY `idx_revoke_operator` (`operator_user_id`),
  CONSTRAINT `fk_revoke_message`
    FOREIGN KEY (`message_id`) REFERENCES `message` (`message_id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_revoke_session`
    FOREIGN KEY (`session_id`) REFERENCES `session` (`session_id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_revoke_operator_user`
    FOREIGN KEY (`operator_user_id`) REFERENCES `user` (`user_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_revoke_target_user`
    FOREIGN KEY (`target_user_id`) REFERENCES `user` (`user_id`)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6) 上传文件
CREATE TABLE `upload_file` (
  `file_id`          VARCHAR(64)   NOT NULL,
  `origin_name`      VARCHAR(255)  NOT NULL,
  `uploader_user_id` VARCHAR(64)   NOT NULL,
  `file_path`        VARCHAR(512)  NOT NULL,
  `file_size`        BIGINT        NOT NULL DEFAULT 0,
  `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`file_id`),
  KEY `idx_file_uploader_time` (`uploader_user_id`, `created_at`),
  CONSTRAINT `fk_file_uploader_user`
    FOREIGN KEY (`uploader_user_id`) REFERENCES `user` (`user_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7) 敏感词
CREATE TABLE `sensitive_word` (
  `word`        VARCHAR(64) NOT NULL,
  `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `level`       TINYINT     NOT NULL DEFAULT 1,
  `enabled`     TINYINT     NOT NULL DEFAULT 1,
  `remark`      VARCHAR(255) NULL,
  PRIMARY KEY (`word`),
  KEY `idx_sensitive_enabled` (`enabled`),
  KEY `idx_sensitive_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8) 聊天记录（你这里偏“落盘索引表”，记录 md 文件与消息的关联）
CREATE TABLE `chat_record` (
  `record_id`       BIGINT      NOT NULL AUTO_INCREMENT,
  `message_id`      VARCHAR(64)  NOT NULL,
  `record_file_id`  VARCHAR(64)  NOT NULL,
  `sender_user_id`  VARCHAR(64)  NOT NULL,
  `file_path`       VARCHAR(512) NOT NULL,
  `record_date`     DATE         NOT NULL,
  `sender_name`     VARCHAR(32)  NOT NULL,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_record_message` (`message_id`),
  KEY `idx_record_date` (`record_date`),
  KEY `idx_record_sender` (`sender_user_id`),
  KEY `idx_record_file` (`record_file_id`),
  CONSTRAINT `fk_record_message`
    FOREIGN KEY (`message_id`) REFERENCES `message` (`message_id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_record_sender_user`
    FOREIGN KEY (`sender_user_id`) REFERENCES `user` (`user_id`)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;