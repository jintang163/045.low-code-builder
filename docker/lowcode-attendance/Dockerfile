FROM openjdk:8-jdk-alpine

LABEL maintainer="lowcode-platform"

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app

COPY lowcode-attendance/target/lowcode-attendance.jar app.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "app.jar", "-Xms256m", "-Xmx512m"]
