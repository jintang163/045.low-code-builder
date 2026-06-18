# 低代码开发平台 (Low-Code Platform)

一款企业级低代码开发平台，支持可视化数据模型设计、页面拖拽搭建、业务逻辑编排与工作流设计，一键生成可独立部署的应用。

## ✨ 核心特性

### 🎯 核心功能
- **数据模型可视化设计**：拖拽创建数据表，支持多种字段类型，ER图展示，自动生成SQL迁移脚本
- **页面可视化搭建**：拖拽式页面设计器，30+组件，支持PC/移动端双端适配，实时预览
- **业务逻辑编排**：可视化拖拽编排业务逻辑，支持触发器、条件分支、数据操作、变量赋值等
- **工作流设计**：集成Flowable引擎，支持BPMN 2.0规范，可视化审批流程设计
- **一键应用生成**：根据设计自动生成完整的前后端代码，支持Docker一键部署

### 🛠 技术栈

**后端技术栈**
- 微服务框架：Spring Cloud 2021.0.8 + Spring Boot 2.7.18
- 服务注册：Nacos 2.2.3
- ORM框架：MyBatis Plus 3.5.5
- 工作流引擎：Flowable 6.8.0
- 数据库：MySQL 8.0（兼容达梦、PostgreSQL）
- 缓存：Redis 7.2 + Redisson 3.25.0
- 对象存储：MinIO（兼容阿里云OSS、腾讯云COS）

**前端技术栈**
- 构建工具：Vite 5.0
- UI框架：React 18 + Ant Design 5.12
- 拖拽库：React DnD 16
- 表单引擎：Formily 2.3
- 图表库：ECharts 5.4
- 状态管理：Zustand 4.4.7
- 路由：React Router v6

## 📁 项目结构

```
lowcode-platform/
├── lowcode-common/              # 公共模块（工具类、异常、通用实体）
├── lowcode-gateway/             # 网关服务（路由、鉴权、限流）
├── lowcode-auth/                # 认证服务（登录、权限、JWT）
├── lowcode-model/               # 数据模型模块（表设计、ER图、SQL生成）
├── lowcode-page/                # 页面模块（页面设计、组件库）
├── lowcode-flow/                # 流程模块（业务逻辑、工作流）
├── lowcode-generator/           # 代码生成模块（代码生成、应用发布）
├── lowcode-oss/                 # 对象存储服务（文件上传下载）
├── lowcode-frontend/            # 前端项目（React + Ant Design）
├── sql/                         # 数据库脚本
├── docker/                      # Docker配置
└── pom.xml                      # 父POM
```

## 🚀 快速开始

### 环境要求
- JDK 1.8+
- Node.js 16+
- Maven 3.6+
- Docker 20.10+
- Docker Compose 2.0+

### 方式一：Docker Compose 一键部署

```bash
# 1. 克隆项目
git clone <repository-url>
cd lowcode-platform

# 2. 执行构建脚本
cd docker
./build.sh  # Linux/Mac
# 或
build.bat   # Windows

# 3. 启动服务
docker-compose up -d
```

### 方式二：本地开发运行

**1. 启动基础服务（MySQL、Redis、Nacos、MinIO）**
```bash
cd docker
docker-compose up -d mysql redis nacos minio
```

**2. 初始化数据库**
```bash
# 执行SQL脚本
mysql -h 127.0.0.1 -uroot -plowcode@2024 < sql/init.sql
```

**3. 启动后端服务**
```bash
# 依次启动各服务
# 1. lowcode-gateway (端口: 8080)
# 2. lowcode-auth (端口: 8081)
# 3. lowcode-model (端口: 8082)
# 4. lowcode-page (端口: 8083)
# 5. lowcode-flow (端口: 8084)
# 6. lowcode-generator (端口: 8085)
# 7. lowcode-oss (端口: 8086)
```

**4. 启动前端**
```bash
cd lowcode-frontend
npm install
npm run dev
```

## 🌐 访问地址

| 服务 | 地址 | 账号/密码 |
|------|------|----------|
| 前端平台 | http://localhost | admin / 123456 |
| 后端网关 | http://localhost:8080 | - |
| Nacos控制台 | http://localhost:8848/nacos | nacos / nacos |
| MinIO控制台 | http://localhost:9001 | admin / lowcode@2024 |
| API文档 | http://localhost:8080/doc.html | - |

## 📚 功能模块

### 1. 数据模型设计
- ✅ 可视化表设计，支持拖拽调整字段顺序
- ✅ 支持多种字段类型：文本、数字、日期、枚举、关联等
- ✅ 主键、索引、唯一约束配置
- ✅ ER图可视化展示表关系
- ✅ 逆向工程导入已有数据库
- ✅ 模型变更自动生成SQL迁移脚本
- ✅ 多数据源支持（MySQL、PostgreSQL、达梦）

### 2. 页面可视化搭建
- ✅ 拖拽式页面设计器
- ✅ 30+组件库（基础、表单、布局、数据、图表、高级）
- ✅ 组件属性、样式、事件、数据绑定配置
- ✅ PC/移动端双端实时预览
- ✅ 响应式布局设计
- ✅ 页面代码生成与下载
- ✅ Formily动态表单

### 3. 业务逻辑编排
- ✅ 可视化拖拽编排节点
- ✅ 30+节点类型（触发器、控制流、数据操作、变量、通知等）
- ✅ SVG连线可视化
- ✅ 节点属性动态配置
- ✅ 代码自动生成与热部署
- ✅ 调试模式，单步执行，变量快照

### 4. 工作流设计
- ✅ BPMN 2.0规范支持
- ✅ Flowable引擎集成
- ✅ 可视化流程设计器
- ✅ 多种节点类型（事件、任务、网关、流程）
- ✅ 会签、监听器、服务任务等高级特性
- ✅ 流程部署与实例管理

### 5. 应用生成与发布
- ✅ 一键生成完整应用代码包
- ✅ 后端代码（Spring Boot + MyBatis Plus）
- ✅ 前端代码（React + Ant Design）
- ✅ Docker配置生成
- ✅ README文档自动生成

## 🔧 系统架构

### 后端微服务架构
```
                    ┌─────────────────┐
                    │   前端应用/客户端  │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  API Gateway    │  路由、鉴权、限流
                    └────────┬────────┘
                             │
           ┌─────────────────┼─────────────────┐
           │                 │                 │
      ┌────▼───┐        ┌────▼───┐        ┌────▼───┐
      │  Auth  │        │  Model │        │  Page  │
      └────────┘        └────────┘        └────────┘
           │                 │                 │
      ┌────▼───┐        ┌────▼───┐        ┌────▼───┐
      │  Flow  │        │  Gen   │        │  OSS   │
      └────────┘        └────────┘        └────────┘
```

### 核心数据流向
1. 用户请求 → 网关鉴权 → 对应微服务处理
2. 设计器操作 → 保存设计数据 → 代码生成器 → 应用代码包
3. 流程执行 → Flowable引擎 → 业务逻辑执行 → 数据持久化

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 开源协议

本项目采用 MIT 协议，详见 [LICENSE](LICENSE) 文件。

## 📞 联系方式

- 项目主页：[GitHub Repository]
- 问题反馈：[Issues]

## 🙏 致谢

感谢以下开源项目：
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [Ant Design](https://ant.design/)
- [React DnD](https://react-dnd.github.io/)
- [Formily](https://formilyjs.org/)
- [Flowable](https://www.flowable.com/)
- [MyBatis Plus](https://baomidou.com/)
