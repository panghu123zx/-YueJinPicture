create database if not exists ph_picture;

use ph_picture;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;


-- 图片表
create table if not exists picture
(
    id           bigint auto_increment comment 'id' primary key,
    url          varchar(512)                       not null comment '图片 url',
    name         varchar(128)                       not null comment '图片名称',
    introduction varchar(512)                       null comment '简介',
    category     varchar(64)                        null comment '分类',
    tags         varchar(512)                      null comment '标签（JSON 数组）',
    picSize      bigint                             null comment '图片体积',
    picWidth     int                                null comment '图片宽度',
    picHeight    int                                null comment '图片高度',
    picScale     double                             null comment '图片宽高比例',
    picFormat    varchar(32)                        null comment '图片格式',
    userId       bigint                             not null comment '创建用户 id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_category (category),         -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_userId (userId)              -- 提升基于用户 ID 的查询性能
) comment '图片' collate = utf8mb4_unicode_ci;

alter table picture add column spaceId bigint null comment '空间 id';
create index idx_spaceId on picture (spaceId);

alter table picture add column thumbnailUrl varchar(512) null comment '缩略图 url';


ALTER TABLE picture
    -- 添加新列
    ADD COLUMN reviewStatus INT DEFAULT 0 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    ADD COLUMN reviewMessage VARCHAR(512) NULL COMMENT '审核信息',
    ADD COLUMN reviewerId BIGINT NULL COMMENT '审核人 ID',
    ADD COLUMN reviewTime DATETIME NULL COMMENT '审核时间';

-- 创建基于 reviewStatus 列的索引
CREATE INDEX idx_reviewStatus ON picture (reviewStatus);

ALTER TABLE picture
    ADD COLUMN picColor varchar(16) null comment '图片主色调';

ALTER TABLE picture
    ADD COLUMN commentCount bigint default 0 null comment '评论数',
    ADD COLUMN likeCount bigint default 0 null comment '点赞数',
    ADD COLUMN shareCount bigint default 0 null comment '分享数',
    ADD COLUMN viewCount bigint default 0 null comment '浏览数';
CREATE INDEX idx_viewCount
    ON picture (viewCount);



-- 空间表
create table if not exists space
(
    id         bigint auto_increment comment 'id' primary key,
    spaceName  varchar(128)                       null comment '空间名称',
    spaceLevel int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   default 0                 null comment '空间图片的最大总大小',
    maxCount   bigint   default 0                 null comment '空间图片的最大数量',
    totalSize  bigint   default 0                 null comment '当前空间下图片的总大小',
    totalCount bigint   default 0                 null comment '当前空间下的图片数量',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    -- 索引设计
    index idx_userId (userId),        -- 提升基于用户的查询效率
    index idx_spaceName (spaceName),  -- 提升基于空间名称的查询效率
    index idx_spaceLevel (spaceLevel) -- 提升按空间级别查询的效率
) comment '空间' collate = utf8mb4_unicode_ci;



ALTER TABLE space
    ADD COLUMN spaceType int default 0 not null comment '空间类型：0-私有 1-团队';

CREATE INDEX idx_spaceType ON space (spaceType);



create table comment
(
    id         bigint                                     not null comment '主键id'
        primary key,
    targetId bigint           default 0                 not null comment '目标id',
    targetType tinyint(1)   default 0 not null comment '目标的类型 0-图片 1-帖子',
    userId     bigint           default 0                 not null comment '用户id',
    userName   varchar(50)                                null comment '用户昵称',
    userAvatar varchar(255)                               null comment '用户头像',
    content    varchar(500)                               null comment '评论内容',
    parentId   bigint           default -1                null comment '父级评论id',
    likeCount  int              default 0                 null comment '点赞数量',
    fromId     bigint                                     null comment '回复记录id',
    fromName   varchar(255) collate utf8mb4_bin           null comment '回复人名称',
    createTime datetime         default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime         default CURRENT_TIMESTAMP not null comment '更新时间',
    isDeleted  tinyint unsigned default '0'               not null comment '逻辑删除 1（true）已删除， 0（false）未删除'
)
    comment '评论' collate = utf8mb4_general_ci
                   row_format = DYNAMIC;

create index idx_targetId
    on comment (targetId);

create index idx_userId
    on comment (userId);

create table user_like
(
    id         bigint auto_increment comment 'id' primary key,
    targetType tinyint(1)   default 0 not null comment '目标的类型 0-图片 1-帖子',
    userId     bigint                                 not null comment '用户 id',
    userName   varchar(50)                                null comment '用户昵称',
    userAvatar varchar(255)                               null comment '用户头像',
    likePic text  null comment '我点赞的图片id的json数组',
    likePost text null comment '我点赞的帖子id的json数组',
    createTime datetime     default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '用户点赞表' collate = utf8mb4_unicode_ci;
create index idx_userId on  user_like (userId);

alter table user_like
    add column likeShare tinyint(1) default 0 null comment '是否点赞分享 0-点赞 1-分享';


create table space_user
(
    id         bigint auto_increment comment 'id'
        primary key,
    spaceId    bigint                                 not null comment '空间 id',
    userId     bigint                                 not null comment '用户 id',
    spaceRole  varchar(128) default 'viewer'          null comment '空间角色：viewer/editor/admin',
    createTime datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_spaceId_userId
        unique (spaceId, userId)
)
    comment '空间用户关联' collate = utf8mb4_unicode_ci;

create index idx_spaceId
    on space_user (spaceId);

create index idx_userId
    on space_user (userId);