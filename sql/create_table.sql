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

alter table user
    add column email varchar(256) null comment '邮箱';

-- 图片表
create table if not exists picture
(
    id           bigint auto_increment comment 'id' primary key,
    url          varchar(512)                       not null comment '图片 url',
    name         varchar(128)                       not null comment '图片名称',
    introduction varchar(512)                       null comment '简介',
    category     varchar(64)                        null comment '分类',
    tags         varchar(512)                       null comment '标签（JSON 数组）',
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

alter table picture
    add column spaceId bigint null comment '空间 id';
create index idx_spaceId on picture (spaceId);

alter table picture
    add column thumbnailUrl varchar(512) null comment '缩略图 url';


ALTER TABLE picture
    -- 添加新列
    ADD COLUMN reviewStatus  INT DEFAULT 0 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    ADD COLUMN reviewMessage VARCHAR(512)  NULL COMMENT '审核信息',
    ADD COLUMN reviewerId    BIGINT        NULL COMMENT '审核人 ID',
    ADD COLUMN reviewTime    DATETIME      NULL COMMENT '审核时间';

-- 创建基于 reviewStatus 列的索引
CREATE INDEX idx_reviewStatus ON picture (reviewStatus);

ALTER TABLE picture
    ADD COLUMN picColor varchar(16) null comment '图片主色调';

ALTER TABLE picture
    ADD COLUMN commentCount bigint default 0 null comment '评论数',
    ADD COLUMN likeCount    bigint default 0 null comment '点赞数',
    ADD COLUMN shareCount   bigint default 0 null comment '分享数',
    ADD COLUMN viewCount    bigint default 0 null comment '浏览数';
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


-- 评论表
create table comment
(
    id           bigint                                     not null comment '主键id'
        primary key,
    targetId     bigint           default 0                 not null comment '目标id',
    targetType   tinyint(1)       default 0                 not null comment '目标的类型 0-图片 1-帖子',
    userId       bigint           default 0                 not null comment '用户id',
    content      varchar(500)                               null comment '评论内容',
    parentId     bigint           default -1                null comment '父级评论id',
    likeCount    int              default 0                 null comment '点赞数量',
    fromId       bigint                                     null comment '回复记录id',
    targetUserId BIGINT                                     null comment '目标用户id',
    isRead       tinyint(1)       default 0                 null comment '是否已读',
    createTime   datetime         default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime         default CURRENT_TIMESTAMP not null comment '更新时间',
    isDeleted    tinyint unsigned default '0'               not null comment '逻辑删除 1（true）已删除， 0（false）未删除'
)
    comment '评论' collate = utf8mb4_general_ci
                   row_format = DYNAMIC;

create index idx_targetId
    on comment (targetId);

create index idx_userId
    on comment (userId);

-- 点赞表
create table user_like
(
    id         bigint auto_increment comment 'id' primary key,
    targetType tinyint(1) default 0                 not null comment '目标的类型 0-图片 1-帖子',
    userId     bigint                               not null comment '用户 id',
    userName   varchar(50)                          null comment '用户昵称',
    userAvatar varchar(255)                         null comment '用户头像',
    likePic    text                                 null comment '我点赞的图片id的json数组',
    likePost   text                                 null comment '我点赞的帖子id的json数组',
    createTime datetime   default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '用户点赞表' collate = utf8mb4_unicode_ci;
create index idx_userId on user_like (userId);

alter table user_like
    add column likeShare tinyint(1) default 0 null comment '是否点赞分享 0-点赞 1-分享';

-- 空间表
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

-- 论坛表（帖子表）
create table forum
(
    id            bigint auto_increment comment 'id' primary key,
    title         varchar(128)                       not null comment '标题',
    content       text                               not null comment '内容',
    userId        bigint                             not null comment '创建人',
    category      varchar(64)                        not null comment '分类',
    likeCount     int      default 0                 null comment '点赞数',
    viewCount     int      default 0                 null comment '浏览数',
    shareCount    int      default 0                 null comment '分享数',
    commentCount  int      default 0                 null comment '评论数',
    reviewStatus  int      default 0                 not null comment '审核状态 0-待审核，1-通过，2-拒绝',
    reviewMessage varchar(512)                       null comment '审核信息',
    reviewerId    bigint                             null comment '审核人',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除'
) comment '论坛表' collate = utf8mb4_unicode_ci;

create index idx_title on forum (title);
create index idx_category on forum (category);
create index idx_userId on forum (userId);

-- 论坛图片表
create table forum_file
(
    id         bigint auto_increment comment 'id' primary key,
    forumId    bigint                             not null comment '帖子 id',
    picUrl     varchar(512)                       null comment 'url',
    type       tinyint  default 0                 null comment '图片类型 0-封面，1-文件',
    size       bigint                             null comment '图片大小',
    position   int                                null comment '图片位置',
    sort       int                                null comment '图片顺序',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 null comment '是否删除'
) comment '论坛图片表' collate = utf8mb4_unicode_ci;
create index idx_forumId on forum_file (forumId);
create index idx_position on forum_file (position);

-- 点赞/分享 消息提示
create table like_message
(
    id         bigint auto_increment comment '主键id'
        primary key,
    receiverId bigint                                     not null comment '消息接收者id',
    sendId     bigint                                     not null comment '消息发送者id',
    targetType tinyint(1)       default 0                 null comment '目标的类型 0-图片 1-帖子',
    actionType tinyint(1)       default 0                 null comment '0-点赞，1-分享',
    targetId   bigint                                     not null comment '目标的id',
    isRead     tinyint(1)       default 0                 null comment '是否已读， 0-未读，1-已读',
    createTime datetime         default CURRENT_TIMESTAMP not null comment '创建时间',
    isDeleted  tinyint unsigned default '0'               not null comment '逻辑删除 1（true）已删除， 0（false）未删除'
)
    comment '点赞/分享消息' collate = utf8mb4_general_ci
                            row_format = DYNAMIC;

create index idx_receiverId on like_message (receiverId);
create index idx_sendId on like_message (sendId);
create index idx_targetId on like_message (targetId);


-- 关注表
create table follow
(
    id          bigint auto_increment comment '主键id'
        primary key,
    userId      bigint                                     not null comment '被关注的人的id',
    followerId  bigint                                     not null comment '粉丝id',
    followState tinyint(1)       default 0                 null comment '关注状态， 0-已关注，1-已取消关注',
    isMutual    tinyint(1)       default 0                 null comment '是否双向关注， 0-否，1-是',
    createTime  datetime         default CURRENT_TIMESTAMP not null comment '创建时间',
    isDeleted   tinyint unsigned default '0'               not null comment '逻辑删除 1（true）已删除， 0（false）未删除'
)
    comment '关注表' collate = utf8mb4_general_ci
                     row_format = DYNAMIC;
create index idx_followerId on follow (followerId);
create index idx_userId on follow (userId);


-- 消息的内容
create table chat_message
(
    id           bigint auto_increment comment 'id'
        primary key,
    sessionId    varchar(128)                         null comment '会话id，链接为user1_user2用于区别会话的,id按大小排列',
    replayId     bigint                               null comment '回复消息的id',
    sendId       bigint                               not null comment '聊天发送者的id',
    receiveId    bigint                               not null comment '聊天接收者的id',
    content      text                                 null comment '消息内容',
    messageType  tinyint    default '0'               null comment '消息类型 0-图片，1-文件',
    targetId     bigint                               null comment '目标的id',
    isRead       tinyint(1) default 0                 null comment '是否已读， 0-未读，1-已读',
    chatPromptId bigint                               not null comment '消息提示的id',
    createTime   datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    isDelete     tinyint(1) default 0                 not null comment '是否删除'
)
    comment '用户聊天表' collate = utf8mb4_unicode_ci;
create index idx_sendId on chat_message (sendId);
create index idx_receiveId on chat_message (receiveId);
create index idx_sessionId on chat_message (sessionId);
create index idx_replayId on chat_message (replayId);
create index idx_targetId on chat_message (targetId);
create index idx_chatPromptId on chat_message (chatPromptId);

alter table chat_message
    add isRecalled tinyint(1) default 0 not null comment '是否撤回， 0-未撤回，1-已撤回';



-- 消息的提示
create table chat_prompt
(
    id              bigint auto_increment comment 'id' primary key,
    userId          bigint                               not null comment '用户id',
    targetId        bigint                               not null comment '目标id',
    title           varchar(128)                         null comment '聊天记录的名称',
    receiveTitle    varchar(128)                         null comment '对方定义的聊天名称',
    chatType        tinyint(1) default '0'               null comment '聊天类型 0-私信 ,1-好友，2-群聊',
    unreadCount     int                                  null comment '未读消息数量',
    lastMessage     text                                 null comment '最后一条消息内容',
    lastMessageTime datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '最后交流的时间',
    createTime      datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint(1) default 0                 not null comment '是否删除'
) comment '消息提示表' collate = utf8mb4_unicode_ci
                       row_format = DYNAMIC;
create index idx_userId on chat_prompt (userId);
create index idx_targetId on chat_prompt (targetId);
create index idx_title on chat_prompt (title);
create index idx_receiveTitle on chat_prompt (receiveTitle);


-- 视频文件表
create table audio_file
(
    id           bigint auto_increment comment 'id' primary key,
    userId       bigint                               not null comment '用户id',
    fileUrl      varchar(512)                         null comment '存放地址',
    fileType     tinyint(1) default '0'               null comment '文件类型 0-图片，1-视频，2-音频',
    title        varchar(128)                         null comment '标题',
    size         bigint                               null comment '大小',
    introduction varchar(128)                         null comment '简介',
    createTime   datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint(1) default 0                 not null comment '是否删除'
) comment '视频文件表' collate = utf8mb4_unicode_ci
                       row_format = DYNAMIC;
create index idx_userId on audio_file (userId);