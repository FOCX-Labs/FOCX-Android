# FOCX 去中心化电商平台 - Android应用详细设计

## 项目概述
FOCX是一个基于区块链的去中心化电商平台Android应用，采用Kotlin + Jetpack Compose全面构建，提供完整的购买、销售、收益和治理功能。界面设计遵循Material Design 3设计规范，支持深色模式，具有优秀的用户体验。

## 整体架构设计

### 主要布局结构
- **MainActivity**: 主Activity，包含Compose UI的底部导航栏
- **Composable架构**: 使用Composable函数管理各个功能模块
- **深色模式支持**: 基于系统设置的明暗主题切换，使用Material3主题
- **Material Design**: 遵循Material Design 3设计规范，使用Material3 Compose组件

### 底部导航栏 (NavigationBar)
- **五个主要标签页**: 购买(Buy)、销售(Sell)、收益(Earn)、治理(Governance)、个人资料(Profile)
- **图标设计**: 每个标签配有对应的Material Icons Compose
- **激活状态**: 当前页面标签高亮显示，使用Material3的选中状态
- **导航功能**: 使用Navigation Compose实现Composable间导航

## 页面详细设计

### 1. 购买页面 (Buy/Index)

#### 搜索头部 (SearchBar Composable)
- **搜索输入框**: 使用Material3的SearchBar Composable，支持实时搜索
- **筛选按钮**: 右侧筛选图标，点击打开筛选ModalBottomSheet
- **自适应设计**: 使用Compose的响应式布局适配不同屏幕

#### 筛选面板 (FilterBottomSheet Composable)
- **价格范围**: 使用Slider Composable选择价格区间
- **操作按钮**: Button Composable的应用筛选和重置筛选按钮

#### 产品展示 (ProductCard Composable)
- **网格视图**: 使用LazyVerticalGrid分两列展示产品
- **产品信息**: 包含图片、名称、商家名称、价格、销量
- **交互效果**: 使用clickable修饰符和涟漪效果、导航到详情页面

#### 产品详情页 (ProductDetailScreen Composable)
- **产品图片**: 使用HorizontalPager Composable实现图片轮播
- **基本信息**: 产品名称、价格、销量、商家名称、发货地、销售范围存、库存、物流方式
- **详细描述**: 使用LazyColumn展示产品详细说明和规格参数
- **购买操作**: Button Composable立即购买

### 2. 销售页面 (Sell)

#### 卖家仪表板 (SellerDashboardScreen Composable)
- **统计卡片**: 使用Card Composable展示总销售额、订单数量、产品数量、等关键指标
- **我的产品**: LazyColumn展示产品列表，支持点击查看详情
  - 产品卡片包含图片、名称、价格、销量
  - 使用clickable修饰符和涟漪效果导航到产品详情页面
  - 使用DropdownMenu提供编辑和删除操作
- **最近订单**: LazyColumn展示订单列表，支持点击查看详情
  - 订单信息包含订单号、产品、金额、状态
  - 点击导航到已售产品详情页面
  - 使用AssistChip显示状态标识和时间
- **添加产品**: FloatingActionButton Composable导航到产品发布页面，包含图片选择、商品名称、价格、货币类型、发货地、销售范围存、库存、物流方式、描述
- **编辑产品**：复用添加产品的UI

#### 已售产品详情页 (SoldOrderDetailScreen Composable)
- **订单状态**: 使用LinearProgressIndicator和自定义Composable显示状态指示器
- **产品信息**: LazyColumn展示详细的产品信息
- **客户信息**: Card Composable展示买家联系方式和配送地址
- **支付信息**: 展示支付方式和交易详情
- **操作按钮**: 根据订单状态动态显示相应的Button Composable

### 3. 收益页面 (Earn)

#### 保险基金池
- **统计面板**: 使用Card Composable展示总保险价值、当前APY、总用户数、我的存款
- **趋势指示**: 使用自定义Canvas Composable或第三方图表库显示数据变化趋势
- **质押操作**: OutlinedTextField Composable和Button Composable确认按钮
- **提取功能**: ModalBottomSheet展示提取质押资产的操作界面

#### 金库信息
- **费用分配**: 使用Canvas Composable绘制饼图或环形图展示平台团队和保险持有者的分配比例
- **风险警告**: 使用AlertDialog Composable或ModalBottomSheet展示质押风险、平台风险、监管风险提示
- **收益统计**: Card Composable展示个人总收益和LazyColumn展示收益历史

#### 最近活动
- **活动列表**: LazyColumn展示质押和提取的历史记录
- **图标标识**: 使用Icon Composable区分不同操作类型
- **时间排序**: 按时间倒序显示活动记录

### 4. 治理页面 (Governance)

#### 统计概览
- **四个关键指标**: 活跃提案、总提案、总投票、通过率
- **网格布局**: 使用LazyVerticalGrid展示统计数据
- **数据可视化**: 使用Card Composable清晰展示数字和描述

#### 标签页导航
- **三个标签**: 使用TabRow Composable展示提案(Proposals)、平台规则(Platform Rules)、投票(Votes)
- **激活状态**: 当前标签高亮显示，使用Material3指示器
- **内容切换**: 结合HorizontalPager Composable实现标签内容切换

#### 提案管理
- **创建提案**: FloatingActionButton Composable引导用户创建新提案
- **提案列表**: LazyColumn展示活跃提案的详细信息
  - Card Composable包含提案标题、描述、投票进度
  - LinearProgressIndicator显示支持和反对票数统计
  - Button Composable提供投票操作
  - 展示提案者信息和保证金（Security Deposit）
- **投票功能**: 使用Button Composable实现支持和反对的投票

### 5. 个人资料页面 (Profile)

#### 用户信息
- **钱包连接**: Text Composable显示连接的钱包地址，长按复制到剪贴板
- **连接状态**: 使用自定义Composable或Icon显示绿色指示器
- **用户头像**: AsyncImage Composable展示个人头像和基本信息

#### 余额展示
- **钱包余额**: 使用Card Composable分别显示USDC和ETH余额
- **质押余额**: Card Composable展示质押金额、APY、奖励等信息
- **总计显示**: 使用突出的Text Composable或Card Composable显示总余额

#### 功能菜单
- **我的地址**: 导航到地址管理页面，支持添加、编辑、删除
- **我的订单**: 导航到订单页面查看状态和历史记录
- **卡片设计**: 每个功能项使用Card Composable布局
- **图标标识**: 使用Icon Composable为每个功能配置图标
- **导航箭头**: 右侧使用Icon Composable显示箭头指示可点击

### 6. 订单管理系统

#### 我的订单页面 (MyOrdersScreen Composable)
- **筛选标签**: 使用TabRow Composable展示全部、待付款、处理中、已发货、已送达、已取消
- **订单卡片**: LazyColumn + Card Composable包含产品信息、订单状态、价格、日期
- **状态标识**: 使用AssistChip Composable显示不同颜色的状态徽章
- **操作按钮**: Button Composable提供查看详情、评价、立即支付等操作

#### 订单详情页 (OrderDetailScreen Composable)
- **状态跟踪**: 使用自定义Composable展示订单状态时间线和进度
- **产品信息**: LazyColumn + Card Composable展示详细的产品信息和图片
- **配送信息**: Card Composable展示收货地址和联系方式
- **支付信息**: Card Composable展示支付方式和交易详情
- **物流跟踪**: Text Composable展示快递单号和配送状态

## 交互设计特点

### 视觉设计
- **科技美学**: 深色背景配合霓虹色彩，营造未来科技氛围
- **Material Design 3**: 在Material规范基础上融入科技元素
- **发光UI**: 按钮、卡片等关键元素添加微光效果和边框发光
- **渐变色彩**: 使用科技蓝到紫色的渐变，增强视觉冲击力
- **几何图形**: 运用六边形、菱形等几何元素增强科技感
- **色彩系统**: 使用Material Color System，主色调为科技蓝、霓虹绿，辅以红色(价格)、绿色(成功)、黄色(警告)
- **圆角设计**: 遵循Material Design的圆角规范，结合科技感的直角元素
- **阴影效果**: 使用elevation属性和发光效果实现立体层次感

### 动画效果
- **科技涟漪**: 自定义Ripple Effect，使用霓虹色彩和发光效果的触摸反馈
- **全息转换**: 页面转换使用科技感的滑动、淡入淡出和缩放组合动画
- **粒子效果**: 在关键操作时添加粒子动画，增强科技氛围
- **数据流动画**: 模拟数据传输的流动效果，体现区块链特色
- **脉冲动画**: 重要按钮和状态指示器使用脉冲发光动画
- **全息投影**: Shared Element转换模拟全息投影效果
- **量子加载**: 使用量子风格的加载动画替代传统进度条
- **Matrix效果**: 背景添加矩阵雨效果，增强黑客/科技氛围
- **Lottie科技动画**: 使用科技主题的Lottie动画增强视觉效果

### 自适应设计
- **多屏幕支持**: 支持手机、平板等不同屏幕尺寸
- **自适应布局**: 使用Compose的BoxWithConstraints和WindowSizeClass适配不同屏幕
- **触摸友好**: 遵循Material Design的触摸目标尺寸规范
- **可访问性**: 支持Compose的semantics、大字体和高对比度模式

### 用户体验
- **直观导航**: 使用Navigation Compose实现清晰的导航结构
- **即时反馈**: 用户操作的即时响应和SnackbarHost状态提示
- **错误处理**: 使用AlertDialog Composable和友好的错误提示界面
- **数据持久化**: 使用Room数据库和DataStore保存用户数据和状态

## 技术实现特点

### 组件化设计
- **可复用组件**: 自定义Composable、Material3 Composable等基础组件
- **复合组件**: 产品卡片、订单项等业务相关的复合Composable
- **屏幕组件**: Screen Composable等结构组件

### 状态管理
- **ViewModel**: 使用Android Architecture Components的ViewModel管理UI状态
- **导航管理**: Navigation Compose实现Composable间导航
- **状态提升**: 使用Compose的State Hoisting模式管理状态
- **副作用处理**: 使用LaunchedEffect、DisposableEffect等处理副作用

### 样式系统
- **Material Design 3**: 使用Material3 Compose组件库
- **深色科技主题**: 默认使用深色主题，配合科技蓝、霓虹绿等强调色营造未来科技感
- **渐变背景**: 使用深色渐变背景，从深灰到纯黑的层次感
- **发光效果**: 关键UI元素添加微妙的发光边框和阴影效果
- **动态主题**: 支持明暗主题切换，深色模式下强化科技视觉元素
- **主题系统**: 使用Compose Theme和MaterialTheme管理应用样式
- **设计令牌**: 使用Material Design Tokens定义颜色、字体、形状等，自定义科技风格令牌
- **动态颜色**: 支持Material You动态颜色，结合科技色彩方案
- **透明度层次**: 使用不同透明度的表面创建深度感和层次感

## 开发架构

### 技术栈
- **开发语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构模式**: MVI + Clean Architecture (Model-View-Intent + 清洁架构)
- **依赖注入**: Hilt
- **数据层抽象**: Repository模式 + DataSource接口
- **Mock数据**: 使用本地Mock数据进行开发
- **Solana Mobile SDK**: 通过标识控制是否使用 solana mobile SDK 获取链上数据
- **网络请求**: Retrofit + OkHttp
- **数据库**: Room
- **图片加载**: Coil (Compose优化)
- **异步处理**: Kotlin Coroutines + Flow
- **导航**: Navigation Compose
- **状态管理**: StateFlow + SharedFlow (MVI状态管理)

### 项目结构 (Clean Architecture + MVI)
```
app/
├── data/           # 数据层 (Frameworks & Drivers)
│   ├── datasource/ # 数据源抽象
│   │   ├── mock/   # Mock数据源实现
│   │   ├── solana/ # Solana Mobile数据源实现
│   │   └── api/    # API数据源实现
│   ├── local/      # 本地数据源(Room)
│   ├── remote/     # 远程数据源
│   ├── repository/ # 数据仓库实现
│   └── mapper/     # 数据映射器
├── domain/         # 业务逻辑层 (Use Cases & Entities)
│   ├── entity/     # 业务实体
│   ├── repository/ # 仓库接口
│   ├── usecase/    # 用例
│   └── common/     # 通用业务逻辑
├── presentation/   # 表现层 (Interface Adapters)
│   ├── ui/         # UI组件 (View)
│   │   ├── screen/ # 页面Composable
│   │   ├── component/ # 可复用组件
│   │   └── theme/  # 主题样式
│   ├── viewmodel/  # ViewModel (Intent处理器)
│   ├── state/      # UI状态定义
│   ├── intent/     # 用户意图定义
│   └── navigation/ # 导航
├── di/             # 依赖注入
│   ├── DataModule  # 数据层依赖注入
│   ├── DomainModule # 业务层依赖注入
│   └── MockModule  # Mock数据依赖注入
└── utils/          # 工具类
```

### MVI架构设计

#### MVI模式核心概念
- **Model**: 表示应用状态的不可变数据结构
- **View**: 渲染UI状态并发送用户意图的Composable函数
- **Intent**: 表示用户意图或系统事件的密封类
- **单向数据流**: Intent → ViewModel → State → View
- **状态管理**: 使用StateFlow管理UI状态，SharedFlow处理一次性事件

#### Clean Architecture分层
- **Presentation Layer**: UI组件、ViewModel、State、Intent
- **Domain Layer**: Use Cases、Entities、Repository接口
- **Data Layer**: Repository实现、DataSource、网络/数据库访问

### 数据层抽象设计

#### 分阶段实现策略
- **第一阶段**: 使用Mock数据源，快速搭建UI和业务逻辑
- **第二阶段**: 集成Solana Mobile SDK，获取真实链上数据
- **第三阶段**: 优化数据缓存和离线支持

这个Android应用设计充分体现了现代移动应用的设计理念，采用Jetpack Compose构建现代化UI，遵循Material Design 3规范，注重用户体验和视觉美观，同时采用了Android和Compose的最佳实践，保持了良好的可维护性和扩展性。