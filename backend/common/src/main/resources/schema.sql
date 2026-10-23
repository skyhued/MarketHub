-- ============================================
-- MarketHub 二手交易平台 数据库初始化脚本
-- 版本: v2.0 (微服务架构)
-- 适配: user-service / product-service / order-service / cart-service / review-service / recommend-service
-- ============================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS market_hub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE market_hub;

-- 应用使用的数据库账号由 MySQL 容器根据 MYSQL_USER / MYSQL_PASSWORD 环境变量创建，这里不写密码

-- ========== 1. RBAC权限体系（用户服务） ==========

-- 角色表
CREATE TABLE IF NOT EXISTS role (
    id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 权限表（细粒度权限控制）
CREATE TABLE IF NOT EXISTS permission (
    id INT AUTO_INCREMENT PRIMARY KEY,
    perm_name VARCHAR(50) NOT NULL COMMENT '权限名称',
    perm_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限标识, 如 product:audit',
    perm_type VARCHAR(20) DEFAULT 'BUTTON' COMMENT '权限类型: MENU/BUTTON/API',
    parent_id INT DEFAULT NULL COMMENT '父权限ID',
    path VARCHAR(255) DEFAULT NULL COMMENT '前端路由路径',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS role_permission (
    id INT AUTO_INCREMENT PRIMARY KEY,
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    role_id INT DEFAULT 2 COMMENT '关联role表',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-正常',
    token_version INT NOT NULL DEFAULT 0 COMMENT '登出/改密码/禁用时加1，使已签发的 token 失效',
    avatar_url VARCHAR(255),
    shop_name VARCHAR(20) DEFAULT NULL UNIQUE COMMENT '店铺名，开店审核通过时写入；收回商家资格后仍保留，别人不能占用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 开店申请（用户服务）
-- 一份申请的生命周期：0 待审核 → 1 已通过 → 3 已收回，或 0 待审核 → 2 已驳回。历史申请全部保留，供管理员审核时参考。
CREATE TABLE IF NOT EXISTS merchant_application (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    shop_name VARCHAR(20) NOT NULL,
    category_id INT NOT NULL COMMENT '主营类目',
    contact_phone VARCHAR(20) NOT NULL,
    description VARCHAR(500) NOT NULL COMMENT '申请说明',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-待审核 1-已通过 2-已驳回 3-已收回',
    reject_reason VARCHAR(255) DEFAULT NULL,
    reviewer_id INT DEFAULT NULL,
    review_time DATETIME DEFAULT NULL,
    revoke_reason VARCHAR(255) DEFAULT NULL,
    revoker_id INT DEFAULT NULL,
    revoke_time DATETIME DEFAULT NULL,
    result_seen TINYINT NOT NULL DEFAULT 0 COMMENT '申请人是否已看过"审核通过"的提示',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    -- 只在待审核时有值、其余为 NULL（唯一键允许多个 NULL）：
    -- 同一个用户同一时间只能有一份待审核申请，连点两下提交也挡得住
    pending_user_id INT AS (IF(status = 0, user_id, NULL)) STORED,
    UNIQUE KEY uk_pending_user (pending_user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 2. 分类表（商品服务） ==========
CREATE TABLE IF NOT EXISTS category (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    parent_id INT DEFAULT NULL,
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 3. 商品表（商品服务） ==========
CREATE TABLE IF NOT EXISTS product (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    image_url VARCHAR(255),
    category_id INT,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-下架 1-上架',
    audit_status TINYINT NOT NULL DEFAULT 0 COMMENT '0-待审 1-通过 2-驳回',
    sales_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_audit (status, audit_status),
    INDEX idx_create_time (create_time),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 4. 订单表（订单服务） ==========
CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE COMMENT '分布式订单号（雪花算法）',
    user_id INT NOT NULL,
    seller_id INT DEFAULT NULL COMMENT '卖家ID',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    actual_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    freight DECIMAL(10,2) DEFAULT 0.00,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-待付 1-已付待发 2-已发 3-完成 4-取消',
    payment_type TINYINT DEFAULT NULL,
    payment_time DATETIME,
    delivery_time DATETIME,
    receive_time DATETIME,
    receiver_name VARCHAR(50),
    receiver_phone VARCHAR(20),
    receiver_address VARCHAR(255),
    remark VARCHAR(500),
    cancel_time DATETIME,
    cancel_reason VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 5. 订单项表（订单服务） ==========
CREATE TABLE IF NOT EXISTS order_item (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    product_name VARCHAR(100),
    product_image VARCHAR(255),
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 6. 购物车表（购物车服务） ==========
CREATE TABLE IF NOT EXISTS cart (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    checked TINYINT NOT NULL DEFAULT 1 COMMENT '0-未勾选 1-已勾选',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 7. 收藏表 ==========
CREATE TABLE IF NOT EXISTS favorite (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 8. 评价表（评价服务） ==========
CREATE TABLE IF NOT EXISTS review (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    order_id INT NOT NULL,
    rating TINYINT NOT NULL COMMENT '1-5星',
    content TEXT,
    reply TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_order_product (user_id, order_id, product_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 9. 用户行为表（推荐服务） ==========
CREATE TABLE IF NOT EXISTS user_behavior (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    behavior_type TINYINT NOT NULL COMMENT '1-浏览 2-收藏 3-加购 4-购买',
    weight DECIMAL(3,1) NOT NULL COMMENT '行为权重',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_product (user_id, product_id, behavior_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 10. 推荐缓存表（推荐服务） ==========
CREATE TABLE IF NOT EXISTS recommendation_cache (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '0-热门 -1-相似',
    product_id INT NOT NULL,
    score DECIMAL(10,4) DEFAULT NULL,
    reason VARCHAR(100) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 11. 推荐离线评估日志（推荐服务） ==========
-- 每跑完一次 train.py 追加一行，用来纵向对比"改了算法之后指标是涨还是跌"。
-- 指标是离线留出集上的值，不是线上效果；status=SKIPPED 表示行为数据不足、只写了热门兜底。
CREATE TABLE IF NOT EXISTS recommend_eval_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(20) NOT NULL COMMENT 'SUCCESS-SKIPPED-FAILED',
    note VARCHAR(255) DEFAULT NULL,
    user_count INT DEFAULT NULL,
    item_count INT DEFAULT NULL,
    behavior_count INT DEFAULT NULL,
    test_behavior_count INT DEFAULT NULL,
    k_value INT DEFAULT NULL,
    precision_at_k DECIMAL(8,4) DEFAULT NULL,
    recall_at_k DECIMAL(8,4) DEFAULT NULL,
    ndcg_at_k DECIMAL(8,4) DEFAULT NULL,
    catalog_coverage DECIMAL(8,4) DEFAULT NULL,
    intra_list_diversity DECIMAL(8,4) DEFAULT NULL,
    elapsed_seconds DECIMAL(8,2) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 12. 库存/销量变更账本（商品服务） ==========
-- 服务间调用没有事务，而"本地回滚 + 重试"会把已经提交的远端副作用重复施加一遍
-- （多件订单里前几件归还成功、后一件超时抛异常 → 本地回滚 → 重试 → 前几件又被归还一次 → 库存虚增）。
-- 每次变更先在这里插一行，靠唯一键把重复的挡掉，使这些操作幂等。
-- 用订单号而不是订单 id 作幂等键：下单补偿时订单还没落库、没有 id，但订单号在扣库存之前就生成好了。
CREATE TABLE IF NOT EXISTS stock_ledger (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL COMMENT '分布式订单号，作为幂等键',
    product_id INT NOT NULL,
    op VARCHAR(20) NOT NULL COMMENT 'DEDUCT-扣库存 RESTORE-还库存 SALES_INC-加销量 SALES_DEC-扣销量',
    quantity INT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_product_op (order_no, product_id, op)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 13. 秒杀活动与抢购账本（商品服务） ==========
-- stock 是秒杀专属库存，独立于 product.stock：只限制"能抢到名额的人数"，
-- 抢到名额之后走的还是普通下单流程，会照常再扣一次 product 的真实库存。
CREATE TABLE IF NOT EXISTS seckill_activity (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    seckill_price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-下线 1-上线',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    INDEX idx_time_range (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 一个用户在一场活动里只有一次成功抢购机会，唯一键顺带把"重复抢"和"MQ 消息重投"都挡住。
-- order_no 抢到名额时是 NULL，异步建单成功后才补上；建单失败可以安全重试（confirm 阶段幂等）。
CREATE TABLE IF NOT EXISTS seckill_grab_ledger (
    id INT AUTO_INCREMENT PRIMARY KEY,
    activity_id INT NOT NULL,
    user_id INT NOT NULL,
    order_no VARCHAR(50) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_activity_user (activity_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 14. AI 知识库片段（AI 服务） ==========
-- 助手回答时"依据"的那份资料：仓库里的 Markdown 按标题分层、按段落切块后，每块算一个向量存一行。
-- 向量存成逗号分隔的浮点文本，是为了在 Navicat 里能直接看懂，也省掉一个二进制解析器。
-- kind 区分语料来源：HELP 是 docs/help 下写给用户看的帮助文档，DEV 是其余开发文档；检索时 HELP 有小幅加权。
-- 检索时整表读进内存在 Java 里算余弦——语料只有几百块，这个量级不需要向量库。
CREATE TABLE IF NOT EXISTS ai_doc_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source VARCHAR(255) NOT NULL COMMENT '语料文件相对仓库根目录的路径',
    kind VARCHAR(16) NOT NULL DEFAULT 'DEV' COMMENT 'HELP 用户帮助文档 / DEV 开发文档',
    heading VARCHAR(500) DEFAULT '' COMMENT '所属标题路径，用 > 连接',
    chunk_index INT NOT NULL DEFAULT 0,
    content TEXT NOT NULL,
    embedding MEDIUMTEXT NOT NULL COMMENT '逗号分隔的浮点数向量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_source (source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 15. AI 助手问答日志（AI 服务） ==========
-- 每问一次追加一行。sources 记下这次命中了哪些片段和相似度，
-- 复盘"模型答错/幻觉"时能分清是检索没找对资料，还是模型看了资料仍然答错——两种原因的改法完全不同。
-- latency_ms 是回答耗时的原始数据，P95 这类指标直接从这里算。
CREATE TABLE IF NOT EXISTS ai_chat_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    question VARCHAR(500) NOT NULL,
    answer TEXT,
    sources VARCHAR(1000) DEFAULT '' COMMENT '命中的片段与相似度',
    latency_ms INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user (user_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 初始化数据 ==========

-- 插入角色数据
INSERT INTO role (id, role_name, role_code, description, status) VALUES
(1, '管理员', 'ADMIN', '系统管理员，拥有所有权限', 1),
(2, '普通用户', 'USER', '买家：浏览、下单、付款、确认收货、评价', 1),
(3, '商家', 'MERCHANT', '商家：买家全部权限，外加发布/修改/删除商品和发货', 1);

-- 插入权限数据
INSERT INTO permission (perm_name, perm_code, perm_type, sort_order) VALUES
('用户列表', 'user:list', 'API', 1),
('创建用户', 'user:create', 'API', 2),
('更新用户', 'user:update', 'API', 3),
('删除用户', 'user:delete', 'API', 4),
('用户状态', 'user:status', 'API', 5),
('用户角色', 'user:role', 'API', 6),
('商品列表', 'product:list', 'API', 7),
('创建商品', 'product:create', 'API', 8),
('更新商品', 'product:update', 'API', 9),
('删除商品', 'product:delete', 'API', 10),
('商品审核', 'product:audit', 'API', 11),
('订单列表', 'order:list', 'API', 12),
('创建订单', 'order:create', 'API', 13),
('支付订单', 'order:pay', 'API', 14),
('订单发货', 'order:ship', 'API', 15),
('确认收货', 'order:confirm', 'API', 16),
('取消订单', 'order:cancel', 'API', 17),
('购物车列表', 'cart:list', 'API', 18),
('购物车操作', 'cart:operate', 'API', 19),
('创建评价', 'review:create', 'API', 20),
('评价列表', 'review:list', 'API', 21),
('管理后台', 'admin:dashboard', 'API', 22),
('数据统计', 'admin:stats', 'API', 23),
('申请开店', 'merchant:apply', 'API', 24),
('入驻审核', 'merchant:audit', 'API', 25),
('AI 助手问答', 'ai:chat', 'API', 26);

-- 给ADMIN角色分配所有权限
INSERT INTO role_permission (role_id, permission_id)
SELECT 1, id FROM permission;

-- 给USER（买家）分配购买相关权限，外加申请开店
INSERT INTO role_permission (role_id, permission_id)
SELECT 2, id FROM permission WHERE perm_code IN (
    'product:list',
    'order:list', 'order:create', 'order:pay', 'order:confirm', 'order:cancel',
    'cart:list', 'cart:operate',
    'review:create', 'review:list',
    'merchant:apply',
    'ai:chat'
);

-- 给MERCHANT分配买家权限 + 商品管理 + 发货
INSERT INTO role_permission (role_id, permission_id)
SELECT 3, id FROM permission WHERE perm_code IN (
    'product:list', 'product:create', 'product:update', 'product:delete',
    'order:list', 'order:create', 'order:pay', 'order:ship', 'order:confirm', 'order:cancel',
    'cart:list', 'cart:operate',
    'review:create', 'review:list',
    'ai:chat'
);

-- 插入默认分类
INSERT INTO category (name, parent_id, sort_order) VALUES
('电子产品', NULL, 1),
('服装鞋帽', NULL, 2),
('图书音像', NULL, 3),
('生活日用', NULL, 4),
('运动户外', NULL, 5),
('其他', NULL, 6);

-- 测试账号
-- admin / admin123
-- merchant1 / merchant123
-- merchant2 / merchant123
-- buyer1 / buyer123
-- buyer2 / buyer123
INSERT INTO user (id, username, password, nickname, role_id, status, shop_name) VALUES
(1, 'admin', '$2a$10$LvjHBtc5PjIkrfs2PjsDa.7V9sDTbnmEq1vvD5FdN1dyV4Skc.Hbe', '平台管理员', 1, 1, NULL),
(2, 'merchant1', '$2a$10$TZql2LUHNb6d.TykE5Tdve5e0cE/xjMNyzyp34kSLZLhn5x.pNnsy', '数码商家', 3, 1, '数码闲置铺'),
(3, 'merchant2', '$2a$10$TZql2LUHNb6d.TykE5Tdve5e0cE/xjMNyzyp34kSLZLhn5x.pNnsy', '生活用品商家', 3, 1, '生活杂货铺'),
(4, 'buyer1', '$2a$10$YR4UQS7v7qhdVGAOV.2N0uoAYgJlpCqevm9xaVjH4vEhTiB8.JR9.', '买家一号', 2, 1, NULL),
(5, 'buyer2', '$2a$10$YR4UQS7v7qhdVGAOV.2N0uoAYgJlpCqevm9xaVjH4vEhTiB8.JR9.', '买家二号', 2, 1, NULL),
(6, 'buyer3', '$2a$10$YR4UQS7v7qhdVGAOV.2N0uoAYgJlpCqevm9xaVjH4vEhTiB8.JR9.', '买家三号', 2, 1, NULL),
(7, 'buyer4', '$2a$10$YR4UQS7v7qhdVGAOV.2N0uoAYgJlpCqevm9xaVjH4vEhTiB8.JR9.', '买家四号', 2, 1, NULL)
ON DUPLICATE KEY UPDATE
password=VALUES(password), nickname=VALUES(nickname), role_id=VALUES(role_id), status=VALUES(status), shop_name=VALUES(shop_name);

-- 两个演示商家的开店记录，这样收回他们的商家资格时也有申请记录可以标记
INSERT IGNORE INTO merchant_application (id, user_id, shop_name, category_id, contact_phone, description, status, reviewer_id, review_time, result_seen) VALUES
(1, 2, '数码闲置铺', 1, '13800000002', '出自用的数码产品，都能验机。', 1, 1, NOW(), 1),
(2, 3, '生活杂货铺', 4, '13800000003', '搬家清理出来的生活用品和书。', 1, 1, NOW(), 1);

INSERT INTO product (id, user_id, name, description, price, stock, image_url, category_id, status, audit_status, sales_count, view_count) VALUES
(1001, 2, '九成新机械键盘', '红轴机械键盘，支持热插拔，正常使用三个月。', 199.00, 1, 'products/demo-keyboard.jpg', 1, 1, 1, 5, 120),
(1002, 2, '平板电脑 128G', '自用平板，屏幕无划痕，附保护壳。', 1299.00, 1, 'products/demo-tablet.jpg', 1, 1, 1, 3, 260),
(1003, 3, '考研数学复习书', '完整套装，有少量笔记。', 68.00, 1, 'products/demo-book.jpg', 3, 1, 1, 8, 180),
(1004, 3, '露营折叠椅', '轻便折叠椅，承重稳定，使用两次。', 89.00, 1, 'products/demo-chair.jpg', 5, 1, 1, 4, 95),
(1005, 2, '蓝牙降噪耳机', '功能正常，待平台审核。', 399.00, 1, 'products/demo-headphone.jpg', 1, 0, 0, 0, 12),
(1006, 3, '旧款游戏主机', '已审核驳回，仅用于演示驳回状态。', 899.00, 1, 'products/demo-console.jpg', 1, 0, 2, 0, 5),
(1007, 2, '笔记本机械硬盘 1TB', '拆机件，通电时间短，慢扫无坏道。', 159.00, 1, 'products/demo-hdd.jpg', 1, 1, 1, 2, 88),
(1008, 2, '24 寸显示器', '办公用显示器，无亮点无坏点，带原装支架。', 459.00, 1, 'products/demo-monitor.jpg', 1, 1, 1, 1, 143),
(1009, 2, '无线鼠标', '静音无线鼠标，接收器齐全。', 45.00, 1, 'products/demo-mouse.jpg', 1, 1, 1, 6, 210),
(1010, 3, '保温杯 500ml', '不锈钢保温杯，只用过几次，保温正常。', 39.00, 1, 'products/demo-cup.jpg', 4, 1, 1, 7, 176),
(1011, 3, '护眼台灯', '可调色温台灯，学生党自用。', 59.00, 1, 'products/demo-lamp.jpg', 4, 1, 1, 3, 121),
(1012, 3, '跑步鞋 42 码', '只跑过几次，鞋底磨损轻微。', 129.00, 1, 'products/demo-shoes.jpg', 5, 1, 1, 5, 199),
(1013, 3, '登山包 40L', '轻量登山包，带防雨罩。', 189.00, 1, 'products/demo-backpack.jpg', 5, 1, 1, 2, 154),
(1014, 3, '英语词汇书', '考研词汇书，前几十页有笔记。', 29.00, 1, 'products/demo-vocab.jpg', 3, 1, 1, 9, 231)
ON DUPLICATE KEY UPDATE
name=VALUES(name), price=VALUES(price), stock=VALUES(stock), status=VALUES(status), audit_status=VALUES(audit_status);

INSERT IGNORE INTO favorite (user_id, product_id, create_time) VALUES
(4, 1001, NOW()), (4, 1003, NOW()), (5, 1002, NOW());

-- 秒杀演示活动：平板电脑限量 2 件秒杀价，product.stock 留够 20 件承接抢到名额之后的真实下单
UPDATE product SET stock = 20 WHERE id = 1002;
INSERT INTO seckill_activity (id, product_id, seckill_price, stock, start_time, end_time, status) VALUES
(1, 1002, 899.00, 2, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1)
ON DUPLICATE KEY UPDATE seckill_price=VALUES(seckill_price), stock=VALUES(stock),
    start_time=VALUES(start_time), end_time=VALUES(end_time), status=VALUES(status);

-- 行为时间必须显式写死，不能用默认的 NOW()：train.py 按 create_time 切训练集/测试集，
-- 全部同一时刻会让切分退化、测试集为空。这里每人 8 条落在 30~5 天前、2 条落在 3~1 天前，
-- 于是每人最近的那 2 条刚好构成留出集，离线指标才有意义。
INSERT IGNORE INTO user_behavior (user_id, product_id, behavior_type, weight, create_time) VALUES
(4, 1001, 1, 0.5, DATE_SUB(NOW(), INTERVAL 30 DAY)), (4, 1001, 2, 1.0, DATE_SUB(NOW(), INTERVAL 28 DAY)),
(4, 1002, 1, 0.5, DATE_SUB(NOW(), INTERVAL 26 DAY)), (4, 1002, 2, 1.0, DATE_SUB(NOW(), INTERVAL 25 DAY)),
(4, 1007, 1, 0.5, DATE_SUB(NOW(), INTERVAL 24 DAY)), (4, 1007, 2, 1.0, DATE_SUB(NOW(), INTERVAL 24 DAY)),
(4, 1003, 1, 0.5, DATE_SUB(NOW(), INTERVAL 22 DAY)), (4, 1014, 1, 0.5, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(4, 1010, 1, 0.5, DATE_SUB(NOW(), INTERVAL 18 DAY)), (4, 1011, 1, 0.5, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(4, 1012, 2, 1.0, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(4, 1008, 1, 0.5, DATE_SUB(NOW(), INTERVAL 3 DAY)), (4, 1009, 2, 1.0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 1010, 1, 0.5, DATE_SUB(NOW(), INTERVAL 30 DAY)), (5, 1010, 2, 1.0, DATE_SUB(NOW(), INTERVAL 29 DAY)),
(5, 1011, 1, 0.5, DATE_SUB(NOW(), INTERVAL 27 DAY)), (5, 1004, 3, 0.8, DATE_SUB(NOW(), INTERVAL 25 DAY)),
(5, 1012, 1, 0.5, DATE_SUB(NOW(), INTERVAL 23 DAY)), (5, 1012, 4, 2.0, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 1013, 1, 0.5, DATE_SUB(NOW(), INTERVAL 21 DAY)), (5, 1001, 1, 0.5, DATE_SUB(NOW(), INTERVAL 19 DAY)),
(5, 1002, 1, 0.5, DATE_SUB(NOW(), INTERVAL 17 DAY)), (5, 1003, 2, 1.0, DATE_SUB(NOW(), INTERVAL 15 DAY)),
(5, 1014, 1, 0.5, DATE_SUB(NOW(), INTERVAL 3 DAY)), (5, 1009, 1, 0.5, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(6, 1001, 2, 1.0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (6, 1002, 1, 0.5, DATE_SUB(NOW(), INTERVAL 28 DAY)),
(6, 1007, 3, 0.8, DATE_SUB(NOW(), INTERVAL 26 DAY)), (6, 1008, 1, 0.5, DATE_SUB(NOW(), INTERVAL 24 DAY)),
(6, 1003, 4, 2.0, DATE_SUB(NOW(), INTERVAL 22 DAY)), (6, 1014, 1, 0.5, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(6, 1004, 1, 0.5, DATE_SUB(NOW(), INTERVAL 18 DAY)), (6, 1011, 2, 1.0, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(6, 1009, 1, 0.5, DATE_SUB(NOW(), INTERVAL 3 DAY)), (6, 1012, 1, 0.5, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(7, 1010, 2, 1.0, DATE_SUB(NOW(), INTERVAL 30 DAY)), (7, 1011, 1, 0.5, DATE_SUB(NOW(), INTERVAL 28 DAY)),
(7, 1012, 3, 0.8, DATE_SUB(NOW(), INTERVAL 26 DAY)), (7, 1013, 2, 1.0, DATE_SUB(NOW(), INTERVAL 24 DAY)),
(7, 1004, 1, 0.5, DATE_SUB(NOW(), INTERVAL 22 DAY)), (7, 1003, 1, 0.5, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(7, 1014, 1, 0.5, DATE_SUB(NOW(), INTERVAL 18 DAY)), (7, 1002, 1, 0.5, DATE_SUB(NOW(), INTERVAL 16 DAY)),
(7, 1007, 1, 0.5, DATE_SUB(NOW(), INTERVAL 3 DAY)), (7, 1009, 1, 0.5, DATE_SUB(NOW(), INTERVAL 1 DAY));
