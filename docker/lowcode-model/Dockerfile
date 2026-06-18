FROM openjdk:8-jre-alpine
LABEL maintainer="lowcode-platform"
WORKDIR /app
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
COPY lowcode-model/target/lowcode-model.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","-Xms512m","-Xmx1024m","-Dspring.devtools.restart.enabled=true","app.jar"]
