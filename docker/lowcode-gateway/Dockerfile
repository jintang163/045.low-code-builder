FROM openjdk:8-jre-alpine
LABEL maintainer="lowcode-platform"
WORKDIR /app
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
COPY lowcode-gateway/target/lowcode-gateway.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","-Xms512m","-Xmx1024m","app.jar"]
