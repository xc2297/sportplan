# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本仓库中工作时提供指导。

## 构建与运行命令

### 后端（Spring Boot + Maven）
- **构建：** `mvn clean package`
- **运行：** `mvn spring-boot:run`
- **测试：** `mvn test`
- **单测：** `mvn test -Dtest=SportplanApplicationTests`
- **WAR包：** `mvn package`（输出在 `target/`）

### 前端（Vue 3 + Vite）
- **安装依赖：** `cd frontend && npm install`
- **开发：** `cd frontend && npm run dev`（端口5173，代理到后端8080）
- **构建：** `cd frontend && npm run build`（输出在 `frontend/dist/`）

## 项目概述

运动积分奖惩系统，基于 Spring Boot 4.0.6（Java 17）后端 + Vue 3 前端 + MySQL 数据库。

## 后端架构

- **打包方式：** WAR
- **基础包名：** `org.example.sportplan`
- **分层结构：** `controller → service → repository → entity`
- **API前缀：** `/api`（通过 `server.servlet.context-path` 配置）
- **统一响应：** `ApiResponse<T>` 包装所有接口返回
- **异常处理：** `GlobalExceptionHandler` + `BusinessException`
- **积分计算：** `PointCalculator` 组件，根据性别和运动类型查系数表
- **数据库：** MySQL，JPA ddl-auto=update 自动建表

### 核心业务规则
- 4种运动（跑步/走路/俯卧撑/仰卧起坐），性别差异化积分系数
- 单项每日上限10分，每日总上限40分
- 每人每日一条记录（UNIQUE约束），重复提交视为更新
- 4个奖励等级：5分/10分/20分/30分

## 前端架构

- **技术栈：** Vue 3 + Vite + Vue Router + Pinia + Axios
- **页面路由：** `/`仪表盘、`/record`运动记录、`/rewards`兑换中心、`/team`小组管理、`/profile`个人中心
- **状态管理：** Pinia（useUserStore 管理当前用户）
- **API封装：** `src/api/` 目录，axios实例统一拦截响应
- **开发代理：** Vite将 `/api` 请求代理到 `http://localhost:8080`

## 源码结构

```
src/main/java/org/example/sportplan/
  ├── config/          — CORS等配置
  ├── controller/      — REST控制器
  ├── dto/request/     — 请求DTO
  ├── dto/response/    — 响应DTO（含ApiResponse）
  ├── entity/          — JPA实体类
  ├── exception/       — 全局异常处理
  ├── repository/      — Spring Data JPA仓库
  └── service/         — 业务逻辑层

frontend/src/
  ├── api/             — 后端API调用封装
  ├── components/      — 公共组件（NavBar、UserSelector）
  ├── router/          — 路由配置
  ├── stores/          — Pinia状态管理
  └── views/           — 页面组件
```
