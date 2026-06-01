# ==================== Stage 1: 构建前端 ====================
FROM node:18-alpine AS frontend-build

WORKDIR /app/frontend
# 先复制依赖文件，利用 Docker 缓存层
COPY frontend/package.json frontend/package-lock.json ./
RUN npm install
# 再复制源码并构建
COPY frontend/ ./
RUN npm run build

# ==================== Stage 2: 构建后端 ====================
FROM maven:3.8-openjdk-8 AS backend-build

WORKDIR /app
# 先复制 Maven 配置，利用缓存层下载依赖
COPY pom.xml ./
RUN mvn dependency:go-offline -B
# 复制后端源码
COPY src ./src
# 将前端构建产物复制到后端静态资源目录
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static
# 构建 WAR 包（跳过测试）
RUN mvn package -DskipTests -B

# ==================== Stage 3: 运行镜像 ====================
FROM openjdk:8-jre-slim

WORKDIR /app
# Jasypt 主密钥（生产环境通过环境变量覆盖）
ENV JASYPT_ENCRYPTOR_PASSWORD=SportPlan@Secret2024
# 时区设置
ENV TZ=Asia/Shanghai

# 安装字体库（验证码等场景可能需要）
RUN apt-get update && apt-get install -y fontconfig && rm -rf /var/lib/apt/lists/*

# 从构建阶段复制 WAR 包
COPY --from=backend-build /app/target/sportplan-0.0.1-SNAPSHOT.war ./app.war

# 创建上传目录
RUN mkdir -p /app/uploads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.war"]
