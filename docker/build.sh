#!/bin/bash

echo "=========================================="
echo "  低代码平台 - 一键构建部署脚本"
echo "=========================================="

cd ..

echo ""
echo "[1/8] 编译 lowcode-common..."
mvn clean install -pl lowcode-common -am -DskipTests

echo ""
echo "[2/8] 编译 lowcode-gateway..."
mvn clean package -pl lowcode-gateway -DskipTests

echo ""
echo "[3/8] 编译 lowcode-auth..."
mvn clean package -pl lowcode-auth -DskipTests

echo ""
echo "[4/8] 编译 lowcode-model..."
mvn clean package -pl lowcode-model -DskipTests

echo ""
echo "[5/8] 编译 lowcode-page..."
mvn clean package -pl lowcode-page -DskipTests

echo ""
echo "[6/8] 编译 lowcode-flow..."
mvn clean package -pl lowcode-flow -DskipTests

echo ""
echo "[7/8] 编译 lowcode-generator..."
mvn clean package -pl lowcode-generator -DskipTests

echo ""
echo "[8/8] 编译 lowcode-oss..."
mvn clean package -pl lowcode-oss -DskipTests

echo ""
echo "后端服务编译完成！"

echo ""
echo "构建前端项目..."
cd lowcode-frontend
npm install
npm run build
cd ..

echo ""
echo "构建 Docker 镜像..."
cd docker
docker-compose build

echo ""
echo "=========================================="
echo "  构建完成！"
echo "  启动命令: docker-compose up -d"
echo "  前端地址: http://localhost"
echo "  后端网关: http://localhost:8080"
echo "  Nacos控制台: http://localhost:8848/nacos (nacos/nacos)"
echo "  MinIO控制台: http://localhost:9001 (admin/lowcode@2024)"
echo "  默认账号: admin/123456"
echo "=========================================="
