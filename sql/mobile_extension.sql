-- =====================================================
-- 移动端扩展 SQL
-- 描述：低代码平台移动端功能扩展
-- 编码：UTF-8
-- =====================================================

USE lowcode_platform;

-- =====================================================
-- 1. 为 sys_page 表扩展 layout_type 字段，支持移动端布局
-- =====================================================
-- 注意：sys_page 表已存在 layout_type 字段，这里扩展其注释和新增索引
ALTER TABLE sys_page MODIFY COLUMN layout_type VARCHAR(50) DEFAULT 'FLEX' COMMENT '布局类型 FLEX GRID ABSOLUTE MOBILE_FLEX MOBILE_GRID MOBILE_FLOW';
ALTER TABLE sys_page ADD COLUMN IF NOT EXISTS mobile_config TEXT COMMENT '移动端专属配置JSON' AFTER page_config;
ALTER TABLE sys_page ADD KEY IF NOT EXISTS idx_layout_type (layout_type);

-- =====================================================
-- 2. 为 sys_component_library 表增加 support_platform 字段支持多平台
-- =====================================================
ALTER TABLE sys_component_library ADD COLUMN IF NOT EXISTS support_platform VARCHAR(100) DEFAULT 'PC' COMMENT '支持平台 PC MOBILE MINI_APP ALL' AFTER component_category;
ALTER TABLE sys_component_library ADD COLUMN IF NOT EXISTS touch_events TEXT COMMENT '触屏事件配置JSON' AFTER event_config;
ALTER TABLE sys_component_library ADD COLUMN IF NOT EXISTS gesture_support TEXT COMMENT '手势支持配置JSON' AFTER touch_events;
ALTER TABLE sys_component_library ADD KEY IF NOT EXISTS idx_support_platform (support_platform);

-- =====================================================
-- 3. 创建 sys_mobile_preview 表用于预览服务
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_mobile_preview (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    page_id BIGINT NOT NULL COMMENT '页面ID',
    preview_code VARCHAR(50) NOT NULL COMMENT '预览码',
    preview_url VARCHAR(500) COMMENT '预览地址',
    qrcode_url VARCHAR(500) COMMENT '二维码地址',
    device_type VARCHAR(20) DEFAULT 'MOBILE' COMMENT '设备类型 MOBILE TABLET PAD',
    screen_width INT DEFAULT 375 COMMENT '屏幕宽度',
    screen_height INT DEFAULT 667 COMMENT '屏幕高度',
    preview_config TEXT COMMENT '预览配置JSON',
    expire_time DATETIME COMMENT '过期时间',
    view_count INT DEFAULT 0 COMMENT '访问次数',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_preview_code (preview_code),
    KEY idx_app_id (app_id),
    KEY idx_page_id (page_id),
    KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='移动端预览表';

-- =====================================================
-- 4. 创建 sys_app_version 表用于应用版本管理
-- =====================================================
CREATE TABLE IF NOT EXISTS sys_app_version (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    version_name VARCHAR(50) NOT NULL COMMENT '版本名称',
    version_code VARCHAR(20) NOT NULL COMMENT '版本号',
    platform VARCHAR(20) DEFAULT 'ALL' COMMENT '发布平台 ANDROID IOS H5 MINI_APP ALL',
    description VARCHAR(500) COMMENT '版本描述',
    change_log TEXT COMMENT '更新日志',
    file_size BIGINT COMMENT '安装包大小(字节)',
    download_url VARCHAR(500) COMMENT '下载地址',
    force_update TINYINT DEFAULT 0 COMMENT '是否强制更新 0否 1是',
    min_support_version VARCHAR(20) COMMENT '最低支持版本',
    publish_status TINYINT DEFAULT 0 COMMENT '发布状态 0草稿 1审核中 2已发布 3已下架',
    publish_time DATETIME COMMENT '发布时间',
    gray_scale_config TEXT COMMENT '灰度发布配置JSON',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_version (app_id, version_code, platform),
    KEY idx_app_id (app_id),
    KEY idx_platform (platform),
    KEY idx_publish_status (publish_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用版本管理表';

-- =====================================================
-- 5. 组件分类新增 "移动端布局" 分类
-- 6. 插入移动端专用组件数据
-- =====================================================

-- 移动端布局组件
INSERT INTO sys_component_library (component_type, component_name, component_category, support_platform, icon, description, default_props, default_style, prop_schema, touch_events, gesture_support) VALUES
-- 栅格布局
('MobileGrid', '栅格布局', '移动端布局', 'MOBILE', 'LayoutOutlined', '移动端栅格布局系统，支持响应式布局',
 '{"columns":12,"gutter":8,"justify":"flex-start","align":"stretch"}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true,"onTouchCancel":true}',
 '{"swipe":false,"longPress":false,"pinch":false,"rotate":false}'),

-- 栅格列
('MobileGridItem', '栅格项', '移动端布局', 'MOBILE', 'LayoutOutlined', '栅格布局子项',
 '{"span":12,"offset":0,"pull":0,"push":0}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 折叠面板
('MobileCollapse', '折叠面板', '移动端布局', 'MOBILE', 'DownCircleOutlined', '可折叠的内容面板，支持手风琴模式',
 '{"accordion":true,"defaultActiveKey":[],"bordered":true}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 折叠面板项
('MobileCollapseItem', '折叠面板项', '移动端布局', 'MOBILE', 'RightCircleOutlined', '折叠面板子项',
 '{"title":"标题","name":"","disabled":false}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 底部导航
('MobileTabBar', '底部导航', '移动端布局', 'MOBILE', 'MenuOutlined', '移动端底部标签导航栏',
 '{"items":[{"key":"home","title":"首页","icon":"HomeOutlined"},{"key":"category","title":"分类","icon":"AppstoreOutlined"},{"key":"cart","title":"购物车","icon":"ShoppingCartOutlined"},{"key":"mine","title":"我的","icon":"UserOutlined"}],"defaultActiveKey":"home","safeAreaInsetBottom":true}',
 '{"position":"fixed","bottom":0,"left":0,"right":0,"zIndex":1000}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 轮播图
('MobileSwiper', '轮播图', '移动端布局', 'MOBILE', 'PictureOutlined', '移动端轮播组件，支持自动播放和手势滑动',
 '{"autoplay":true,"interval":3000,"loop":true,"indicatorDots":true,"indicatorColor":"rgba(0,0,0,.3)","indicatorActiveColor":"#fff","duration":500}',
 '{"width":"100%","height":180}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true,"onTouchCancel":true}',
 '{"swipe":true,"longPress":false,"pinch":true}'),

-- 轮播图项
('MobileSwiperItem', '轮播图项', '移动端布局', 'MOBILE', 'PictureOutlined', '轮播图子项',
 '{}',
 '{"width":"100%","height":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 标签栏
('MobileTabs', '标签栏', '移动端布局', 'MOBILE', 'TabletOutlined', '移动端标签页切换',
 '{"items":[{"key":"1","title":"标签1"},{"key":"2","title":"标签2"}],"defaultActiveKey":"1","type":"line","swipeable":true,"animated":true}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 标签页内容
('MobileTabPane', '标签页内容', '移动端布局', 'MOBILE', 'FileTextOutlined', '标签页内容区域',
 '{"key":"","title":""}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 搜索栏
('MobileSearchBar', '搜索栏', '移动端布局', 'MOBILE', 'SearchOutlined', '移动端搜索框组件',
 '{"placeholder":"请输入搜索内容","showCancel":true,"cancelText":"取消","background":"#f5f5f5","maxLength":50}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 下拉刷新
('MobilePullRefresh', '下拉刷新', '移动端布局', 'MOBILE', 'ReloadOutlined', '下拉刷新组件，支持自定义刷新动画',
 '{"refreshing":false,"pullDistance":50,"headHeight":50,"loadingText":"加载中...","releaseText":"释放刷新","pullText":"下拉刷新","finishText":"刷新成功"}',
 '{"width":"100%","minHeight":200}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true,"onTouchCancel":true}',
 '{"swipe":true,"longPress":false,"pull":true}'),

-- 滑动单元格
('MobileSwipeCell', '滑动单元格', '移动端布局', 'MOBILE', 'DoubleLeftOutlined', '可滑动的单元格，支持左右滑动显示操作按钮',
 '{"leftWidth":0,"rightWidth":100,"asyncClose":false,"disabled":false}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true,"onTouchCancel":true}',
 '{"swipe":true,"longPress":false}'),

-- 列表
('MobileList', '列表', '移动端布局', 'MOBILE', 'UnorderedListOutlined', '移动端列表组件',
 '{"bordered":true,"gutter":0}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 列表项
('MobileListItem', '列表项', '移动端布局', 'MOBILE', 'BarsOutlined', '列表子项',
 '{"title":"标题","description":"","prefix":"","suffix":"","arrow":true,"disabled":false}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":true}'),

-- 宫格
('MobileGrid', '宫格', '移动端布局', 'MOBILE', 'AppstoreOutlined', '宫格布局，用于展示功能入口',
 '{"columnNum":4,"border":true,"gutter":0,"square":true,"clickable":true}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":true}'),

-- 宫格项
('MobileGridItem', '宫格项', '移动端布局', 'MOBILE', 'AppstoreOutlined', '宫格子项',
 '{"icon":"","text":"","dot":false,"badge":""}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 侧边栏
('MobileSidebar', '侧边栏', '移动端布局', 'MOBILE', 'MenuUnfoldOutlined', '侧边导航栏',
 '{"defaultActiveKey":"","width":80}',
 '{"height":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 侧边栏项
('MobileSidebarItem', '侧边栏项', '移动端布局', 'MOBILE', 'MenuOutlined', '侧边栏子项',
 '{"title":"","key":"","disabled":false}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 瀑布流
('MobileWaterfall', '瀑布流', '移动端布局', 'MOBILE', 'PictureOutlined', '瀑布流布局',
 '{"columnCount":2,"columnGap":8,"rowGap":8}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 瀑布流项
('MobileWaterfallItem', '瀑布流项', '移动端布局', 'MOBILE', 'FileImageOutlined', '瀑布流子项',
 '{}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 安全区域
('MobileSafeArea', '安全区域', '移动端布局', 'MOBILE', 'SafetyOutlined', '安全区域适配组件',
 '{"top":true,"bottom":true,"left":false,"right":false}',
 '{}',
 '{}',
 '{}',
 '{}'),

-- 吸顶容器
('MobileSticky', '吸顶容器', '移动端布局', 'MOBILE', 'PushpinOutlined', '滚动时自动吸顶的容器',
 '{"offsetTop":0,"zIndex":100}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 浮动按钮
('MobileFloatingButton', '浮动按钮', '移动端布局', 'MOBILE', 'PlusCircleOutlined', '浮动操作按钮',
 '{"type":"primary","shape":"circle","icon":"PlusOutlined","text":"","trigger":"click","iconPrefix":"antd"}',
 '{"position":"fixed","right":16,"bottom":16,"zIndex":1000}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":true}'),

-- 浮动操作菜单
('MobileFloatingMenu', '浮动菜单', '移动端布局', 'MOBILE', 'MenuFoldOutlined', '浮动操作菜单，点击展开多个选项',
 '{"items":[],"direction":"up","trigger":"click","overlay":true}',
 '{"position":"fixed","right":16,"bottom":16,"zIndex":1000}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 空状态
('MobileEmpty', '空状态', '移动端布局', 'MOBILE', 'InboxOutlined', '空状态提示组件',
 '{"image":"","imageStyle":{},"description":"暂无数据","buttonText":""}',
 '{"padding":"40px 20px"}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 结果页
('MobileResult', '结果页', '移动端布局', 'MOBILE', 'CheckCircleOutlined', '结果展示页组件',
 '{"status":"success","title":"操作成功","description":"","buttonText":"返回"}',
 '{"padding":"40px 20px"}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 步骤条
('MobileSteps', '步骤条', '移动端布局', 'MOBILE', 'OrderedListOutlined', '步骤进度指示器',
 '{"current":0,"direction":"horizontal","status":"process","items":[]}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 步骤项
('MobileStep', '步骤项', '移动端布局', 'MOBILE', 'UnorderedListOutlined', '步骤子项',
 '{"title":"","description":"","icon":""}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 索引栏
('MobileIndexBar', '索引栏', '移动端布局', 'MOBILE', 'FilterOutlined', '字母索引栏，用于快速定位',
 '{"indexList":["A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"],"sticky":true,"stickyOffsetTop":0}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 索引栏锚点
('MobileIndexAnchor', '索引锚点', '移动端布局', 'MOBILE', 'FlagOutlined', '索引栏锚点',
 '{"index":"","children":""}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 分隔符
('MobileDivider', '分隔符', '移动端布局', 'MOBILE', 'ColumnHeightOutlined', '内容分隔符',
 '{"contentPosition":"center","dashed":false,"hairline":true}',
 '{"margin":"12px 0"}',
 '{}',
 '{}',
 '{}'),

-- 弹窗
('MobilePopup', '弹窗', '移动端布局', 'MOBILE', 'PlaySquareOutlined', '弹出层容器',
 '{"visible":false,"position":"bottom","overlay":true,"closeable":false,"closeIcon":"Cross","closeIconPosition":"top-right","destroyOnClose":false,"safeAreaInsetBottom":true,"round":false}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 对话框
('MobileDialog', '对话框', '移动端布局', 'MOBILE', 'ExclamationCircleOutlined', '对话框组件',
 '{"visible":false,"title":"","message":"","showConfirmButton":true,"showCancelButton":true,"confirmButtonText":"确认","cancelButtonText":"取消","overlay":true,"closeOnClickOverlay":true}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 动作面板
('MobileActionSheet', '动作面板', '移动端布局', 'MOBILE', 'EllipsisOutlined', '底部弹出的动作选择面板',
 '{"visible":false,"title":"","description":"","options":[],"cancelText":"取消","overlay":true,"closeOnClickOverlay":true,"safeAreaInsetBottom":true}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 抽屉
('MobileDrawer', '抽屉', '移动端布局', 'MOBILE', 'MenuUnfoldOutlined', '侧边抽屉组件',
 '{"visible":false,"position":"right","width":"80%","overlay":true,"closeable":true,"closeIconPosition":"top-left"}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 加载中
('MobileLoading', '加载中', '移动端布局', 'MOBILE', 'LoadingOutlined', '加载提示组件',
 '{"visible":false,"type":"spinner","text":"加载中...","delay":0,"overlay":true}',
 '{}',
 '{}',
 '{}',
 '{}'),

-- 加载更多
('MobileLoadMore', '加载更多', '移动端布局', 'MOBILE', 'DownOutlined', '列表加载更多组件',
 '{"status":"loading","loadingText":"加载中...","finishedText":"没有更多了","errorText":"加载失败，点击重试","immediateCheck":true}',
 '{"padding":"20px 0"}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 无限滚动
('MobileInfiniteScroll', '无限滚动', '移动端布局', 'MOBILE', 'DownCircleOutlined', '无限滚动加载组件',
 '{"loadMoreText":"加载更多","loadingText":"加载中...","finishedText":"没有更多了","errorText":"加载失败","threshold":100,"immediateCheck":true}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 数字键盘
('MobileNumberKeyboard', '数字键盘', '移动端布局', 'MOBILE', 'KeyboardOutlined', '数字输入键盘',
 '{"visible":false,"title":"安全键盘","maxlength":6,"value":"","closeable":true,"closeButtonText":"完成","deleteText":"删除","extraKey":"","randomKeyOrder":false}',
 '{"position":"fixed","bottom":0,"left":0,"right":0,"zIndex":1000}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 密码输入框
('MobilePasswordInput', '密码输入框', '移动端布局', 'MOBILE', 'LockOutlined', '密码输入框，配合数字键盘使用',
 '{"value":"","maxlength":6,"mask":true,"gutter":12,"focused":false}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 倒计时
('MobileCountDown', '倒计时', '移动端布局', 'MOBILE', 'ClockCircleOutlined', '倒计时组件',
 '{"value":0,"format":"HH:mm:ss","autoStart":true,"millisecond":false}',
 '{}',
 '{}',
 '{}',
 '{}'),

-- 进度条
('MobileProgress', '进度条', '移动端布局', 'MOBILE', 'LineChartOutlined', '进度条组件',
 '{"value":0,"type":"line","strokeWidth":8,"color":"#1677ff","trackColor":"#f5f5f5","pivotText":"{percentage}%","showPivot":true}',
 '{"width":"100%"}',
 '{}',
 '{}',
 '{}'),

-- 环形进度
('MobileCircle', '环形进度', '移动端布局', 'MOBILE', 'RedoOutlined', '环形进度条组件',
 '{"value":0,"size":100,"strokeWidth":4,"color":"#1677ff","trackColor":"#f5f5f5","fill":"none","pivotText":"{percentage}%","showPivot":true}',
 '{}',
 '{}',
 '{}',
 '{}'),

-- 标签
('MobileTag', '标签', '移动端布局', 'MOBILE', 'TagsOutlined', '标签组件',
 '{"type":"primary","round":false,"mark":false,"color":"","textColor":"","closeable":false}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 徽标
('MobileBadge', '徽标', '移动端布局', 'MOBILE', 'NotificationOutlined', '徽标提示组件',
 '{"value":"","max":99,"dot":false,"color":"#ee0a24","offset":[0,0]}',
 '{}',
 '{}',
 '{}',
 '{}'),

-- 图标
('MobileIcon', '图标', '移动端布局', 'MOBILE', 'StarOutlined', '图标组件',
 '{"name":"","size":"20px","color":"","dot":false,"badge":"","tag":"i","classPrefix":"van-icon"}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 单元格
('MobileCell', '单元格', '移动端布局', 'MOBILE', 'TableOutlined', '单元格组件',
 '{"title":"","value":"","label":"","icon":"","rightIcon":"Arrow","isLink":false,"clickable":false,"required":false,"center":false,"arrowDirection":"right"}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":true}'),

-- 单元格组
('MobileCellGroup', '单元格组', '移动端布局', 'MOBILE', 'TableOutlined', '单元格组',
 '{"title":"","inset":false,"border":true}',
 '{"width":"100%"}',
 '{}',
 '{}',
 '{}'),

-- 导航栏
('MobileNavBar', '导航栏', '移动端布局', 'MOBILE', 'ArrowLeftOutlined', '顶部导航栏',
 '{"title":"","leftText":"","rightText":"","leftArrow":true,"fixed":true,"placeholder":true,"border":true,"safeAreaInsetTop":true,"zIndex":100}',
 '{"width":"100%","height":44}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 通知栏
('MobileNoticeBar', '通知栏', '移动端布局', 'MOBILE', 'SoundOutlined', '通知公告栏',
 '{"text":"","mode":"","leftIcon":"VolumeOutlined","color":"#ed6a0c","background":"#fffbe8","scrollable":true,"delay":1000,"speed":50,"closeable":false}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 开关
('MobileSwitch', '开关', '移动端布局', 'MOBILE', 'SwitcherOutlined', '移动端开关组件',
 '{"checked":false,"loading":false,"disabled":false,"size":"24px","activeColor":"#1677ff","inactiveColor":"rgba(0,0,0,0.25)"}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 复选框
('MobileCheckbox', '复选框', '移动端布局', 'MOBILE', 'CheckSquareOutlined', '移动端复选框',
 '{"checked":false,"disabled":false,"labelDisabled":false,"labelPosition":"right","iconSize":"20px","checkedColor":"#1677ff"}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 复选框组
('MobileCheckboxGroup', '复选框组', '移动端布局', 'MOBILE', 'CheckSquareOutlined', '复选框组',
 '{"value":[],"disabled":false,"direction":"vertical","iconSize":"20px","checkedColor":"#1677ff"}',
 '{}',
 '{}',
 '{}',
 '{}'),

-- 单选框
('MobileRadio', '单选框', '移动端布局', 'MOBILE', 'CheckCircleOutlined', '移动端单选框',
 '{"checked":false,"disabled":false,"labelDisabled":false,"labelPosition":"right","iconSize":"20px","checkedColor":"#1677ff"}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 单选框组
('MobileRadioGroup', '单选框组', '移动端布局', 'MOBILE', 'CheckCircleOutlined', '单选框组',
 '{"value":"","disabled":false,"direction":"vertical","iconSize":"20px","checkedColor":"#1677ff"}',
 '{}',
 '{}',
 '{}',
 '{}'),

-- 步进器
('MobileStepper', '步进器', '移动端布局', 'MOBILE', 'PlusOutlined', '数字增减器',
 '{"value":1,"min":1,"max":9999,"step":1,"inputWidth":"32px","buttonSize":"28px","decimalLength":0,"allowEmpty":false,"disableInput":false,"disablePlus":false,"disableMinus":false,"longPress":true}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":true}'),

-- 滑动条
('MobileSlider', '滑动条', '移动端布局', 'MOBILE', 'ColumnOutlined', '滑动选择器',
 '{"value":0,"min":0,"max":100,"step":1,"barHeight":"2px","buttonSize":"24px","activeColor":"#1677ff","inactiveColor":"#e5e5e5","disabled":false,"readonly":false}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 双滑块
('MobileRange', '双滑块', '移动端布局', 'MOBILE', 'ColumnOutlined', '双滑块滑动选择器',
 '{"value":[0,100],"min":0,"max":100,"step":1,"barHeight":"2px","buttonSize":"24px","activeColor":"#1677ff","inactiveColor":"#e5e5e5","disabled":false,"readonly":false}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 选择器
('MobilePicker', '选择器', '移动端布局', 'MOBILE', 'UnorderedListOutlined', '底部弹出选择器',
 '{"visible":false,"title":"","columns":[],"value":"","defaultValue":"","itemHeight":44,"visibleItemCount":6,"toolbarPosition":"top","confirmButtonText":"确认","cancelButtonText":"取消","allowHtml":false}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 选择器列
('MobilePickerColumn', '选择器列', '移动端布局', 'MOBILE', 'UnorderedListOutlined', '选择器列组件',
 '{"value":"","options":[],"itemHeight":44,"visibleItemCount":6,"defaultIndex":0,"allowHtml":false}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 日期选择器
('MobileDatePicker', '日期选择器', '移动端布局', 'MOBILE', 'CalendarOutlined', '日期选择器',
 '{"visible":false,"type":"datetime","title":"选择日期时间","value":"","minDate":"2000-01-01","maxDate":"2030-12-31","itemHeight":44,"visibleItemCount":6,"confirmButtonText":"确认","cancelButtonText":"取消"}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 时间选择器
('MobileTimePicker', '时间选择器', '移动端布局', 'MOBILE', 'ClockCircleOutlined', '时间选择器',
 '{"visible":false,"title":"选择时间","value":"","minTime":"00:00","maxTime":"23:59","itemHeight":44,"visibleItemCount":6,"confirmButtonText":"确认","cancelButtonText":"取消","use12Hours":false}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 地区选择器
('MobileArea', '地区选择', '移动端布局', 'MOBILE', 'EnvironmentOutlined', '省市区选择器',
 '{"visible":false,"title":"选择地区","value":"","areaList":{},"columnsNum":3,"itemHeight":44,"visibleItemCount":6,"confirmButtonText":"确认","cancelButtonText":"取消"}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 级联选择
('MobileCascader', '级联选择', '移动端布局', 'MOBILE', 'ApartmentOutlined', '多级级联选择器',
 '{"visible":false,"title":"请选择","options":[],"value":[],"columnsNum":3,"itemHeight":44,"visibleItemCount":6,"confirmButtonText":"确认","cancelButtonText":"取消","closeable":true}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 输入框
('MobileField', '输入框', '移动端布局', 'MOBILE', 'EditOutlined', '移动端文本输入框',
 '{"value":"","label":"","type":"text","placeholder":"请输入","maxlength":"","clearable":true,"clearTrigger":"focus","disabled":false,"readonly":false,"error":false,"errorMessage":"","labelWidth":"6em","labelAlign":"left","inputAlign":"left","autosize":false,"showWordLimit":false,"autocomplete":"off"}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 输入框组
('MobileForm', '表单', '移动端布局', 'MOBILE', 'FormOutlined', '移动端表单组件',
 '{"disabled":false,"readonly":false,"showError":true,"showErrorMessage":true,"labelWidth":"6em","labelAlign":"left","inputAlign":"left","scrollToError":true,"validateFirst":false}',
 '{"width":"100%"}',
 '{}',
 '{}',
 '{}'),

-- 表单项
('MobileFormItem', '表单项', '移动端布局', 'MOBILE', 'FormOutlined', '表单项组件',
 '{"label":"","name":"","rules":[],"required":false,"disabled":false,"readonly":false,"errorMessage":"","labelWidth":"","labelAlign":"","inputAlign":"","leftIcon":"","rightIcon":"","asterisk":true}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 文本域
('MobileTextarea', '文本域', '移动端布局', 'MOBILE', 'FileTextOutlined', '移动端文本域',
 '{"value":"","placeholder":"请输入","maxlength":"","rows":3,"autosize":false,"maxHeight":"","showWordLimit":false,"clearable":true,"clearTrigger":"focus","disabled":false,"readonly":false,"autocomplete":"off"}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 搜索
('MobileSearch', '搜索', '移动端布局', 'MOBILE', 'SearchOutlined', '移动端搜索组件',
 '{"value":"","placeholder":"请输入搜索关键词","maxlength":"","clearable":true,"showAction":true,"actionText":"搜索","disabled":false,"readonly":false,"autocomplete":"off","shape":"square","background":"#f7f8fa"}',
 '{"width":"100%"}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 卡片
('MobileCard', '卡片', '移动端布局', 'MOBILE', 'IdcardOutlined', '移动端卡片组件',
 '{"title":"","desc":"","thumb":"","thumbSize":"80px","rounded":true,"bordered":true,"isLink":false,"lazyLoad":false,"price":"","originPrice":"","priceTop":"","priceBottom":"","num":"","desc":"","tag":"","tags":"","footer":"","footerDesc":"","footerExtra":"","cover":""}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 商品卡片
('MobileGoodsAction', '商品操作栏', '移动端布局', 'MOBILE', 'ShoppingCartOutlined', '商品页面底部操作栏',
 '{"safeAreaInsetBottom":true}',
 '{"position":"fixed","bottom":0,"left":0,"right":0,"zIndex":1000}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 按钮
('MobileButton', '按钮', '移动端布局', 'MOBILE', 'PlayCircleOutlined', '移动端按钮组件',
 '{"text":"按钮","type":"default","size":"normal","icon":"","iconPosition":"left","nativeType":"button","block":false,"plain":false,"square":false,"round":false,"disabled":false,"loading":false,"loadingText":"","hairline":false,"color":"","gradient":"","iconPrefix":"antd"}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":true}'),

-- 按钮组
('MobileButtonGroup', '按钮组', '移动端布局', 'MOBILE', 'PlayCircleOutlined', '按钮组组件',
 '{"type":"default","size":"normal","square":false,"round":false,"disabled":false,"mode":"default"}',
 '{}',
 '{}',
 '{}',
 '{}'),

-- 图片
('MobileImage', '图片', '移动端布局', 'MOBILE', 'PictureOutlined', '图片组件',
 '{"src":"","alt":"","width":"","height":"","fit":"fill","position":"center","round":false,"radius":"","lazyLoad":false,"showError":true,"showLoading":true,"errorIcon":"PhotoFail","loadingIcon":"Photo"}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false,"pinch":true}'),

-- 图片预览
('MobileImagePreview', '图片预览', '移动端布局', 'MOBILE', 'PictureOutlined', '图片预览组件',
 '{"visible":false,"images":[],"startPosition":0,"showIndex":true,"showIndicators":false,"closeable":false,"closeIcon":"Clear","closeIconPosition":"top-right","closeOnClickImage":true,"closeOnClickOverlay":true,"doubleScale":true,"minZoom":1/3,"maxZoom":3,"swipeDuration":300,"longPress":[]}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":true,"pinch":true,"doubleTap":true}'),

-- 上传
('MobileUploader', '上传', '移动端布局', 'MOBILE', 'UploadOutlined', '文件上传组件',
 '{"value":[],"accept":"image/*","uploadText":"上传","previewSize":"80px","previewImage":true,"previewFullImage":true,"maxCount":8,"maxSize":"10MB","multiple":true,"disabled":false,"readonly":false,"deletable":true,"showUpload":true,"beforeRead":"","afterRead":"","beforeDelete":"","imageFit":"cover","resultType":"dataUrl"}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":true}'),

-- 评分
('MobileRate', '评分', '移动端布局', 'MOBILE', 'StarOutlined', '评分组件',
 '{"value":0,"count":5,"size":"20px","gutter":"4px","color":"#ffd21e","voidColor":"#dcdee0","icon":"StarFilled","voidIcon":"StarOutlined","allowHalf":false,"readonly":false,"disabled":false,"touchable":true}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 分享面板
('MobileShareSheet', '分享面板', '移动端布局', 'MOBILE', 'ShareAltOutlined', '底部弹出分享面板',
 '{"visible":false,"title":"立即分享给好友","cancelText":"取消","description":"","options":[],"closeOnClickOverlay":true,"safeAreaInsetBottom":true}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 联系人卡片
('MobileContactCard', '联系人卡片', '移动端布局', 'MOBILE', 'UserOutlined', '联系人信息卡片',
 '{"type":"add","name":"","tel":"","addText":"添加联系人","editText":"编辑联系人","isEditable":true}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 地址编辑
('MobileAddressEdit', '地址编辑', '移动端布局', 'MOBILE', 'EnvironmentOutlined', '收货地址编辑组件',
 '{"addressInfo":{},"areaList":{},"areaColumnsNum":3,"isDefault":false,"showDelete":false,"telValidator":"","searchResult":[],"showSetDefault":true}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 地址列表
('MobileAddressList', '地址列表', '移动端布局', 'MOBILE', 'EnvironmentOutlined', '收货地址列表',
 '{"list":[],"modelValue":"","switchable":true,"showBottom":true,"defaultTagText":"默认","addButtonText":"新增收货地址","disabledText":"当前选择","rightIcon":"Arrow"}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 签名
('MobileSignature', '签名', '移动端布局', 'MOBILE', 'EditOutlined', '手写签名组件',
 '{"type":"png","lineWidth":3,"strokeColor":"#000","backgroundColor":"#fff","confirmButtonText":"确认","clearButtonText":"重写"}',
 '{"width":"100%","height":200}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 日历
('MobileCalendar', '日历', '移动端布局', 'MOBILE', 'CalendarOutlined', '日历选择组件',
 '{"visible":false,"type":"single","title":"日期选择","color":"#1677ff","minDate":"","maxDate":"","defaultDate":"","rowHeight":"64px","formatter":"","poppable":true,"showMark":true,"showTitle":true,"showSubtitle":true,"confirmText":"确定","rangePrompt":"","startText":"开始","endText":"结束","showRangePrompt":true,"readonly":false}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchMove":true,"onTouchEnd":true}',
 '{"swipe":true,"longPress":false}'),

-- 优惠券
('MobileCoupon', '优惠券', '移动端布局', 'MOBILE', 'CouponOutlined', '优惠券组件',
 '{"id":"","name":"","condition":"","expireAt":"","value":0,"tag":"","tagColor":"","description":"","discount":0,"unitDesc":"元","currency":"¥","startAt":"","endAt":"","used":false,"disabled":false,"reason":"","description":"","leftIcon":"","rightIcon":"","centered":false,"hairline":true}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 优惠券列表
('MobileCouponList', '优惠券列表', '移动端布局', 'MOBILE', 'CouponOutlined', '优惠券列表组件',
 '{"modelValue":"","list":[],"chosenCoupon":"","disabledList":[],"showCount":true,"enabledTitle":"可使用优惠券","disabledTitle":"不可使用优惠券","showCloseButton":true,"closeButtonText":"不使用优惠券","showEmpty":"default","emptyImage":"","emptyDescription":"暂无可用优惠券"}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 优惠券选择
('MobileCouponCell', '优惠券选择', '移动端布局', 'MOBILE', 'CouponOutlined', '优惠券选择单元格',
 '{"title":"优惠券","value":"","border":true,"editable":true,"coupon":{},"showCount":true}',
 '{}',
 '{}',
 '{"onTouchStart":true,"onTouchEnd":true}',
 '{"swipe":false,"longPress":false}'),

-- 付款码
('MobileBarcode', '付款码', '移动端布局', 'MOBILE', 'QrcodeOutlined', '条形码/二维码组件',
 '{"value":"","type":"barcode","width":"","height":"","color":"#000","background":"#fff","margin":"10px 20px"}',
 '{}',
 '{}',
 '{}',
 '{}');

-- =====================================================
-- 7. 更新已有组件的 support_platform 字段
-- =====================================================
UPDATE sys_component_library SET support_platform = 'ALL' WHERE support_platform = 'PC' AND is_custom = 0;

-- =====================================================
-- 8. 新增移动端布局分类到组件分类枚举
-- =====================================================
-- 注意：如果有枚举表或字典表，需要在此处插入分类数据
-- 此处通过注释说明，实际使用时根据项目字典表结构进行调整

-- =====================================================
-- 移动端扩展 SQL 结束
-- =====================================================
