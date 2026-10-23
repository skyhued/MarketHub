# 不加 # syntax 指令：BuildKit 已内置 dockerfile 前端，加了反而会强制联网拉镜像。
# 构建阶段不含 ARG，八个服务共用一份 Maven 缓存。
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY docker/maven-settings.xml /root/.m2/settings.xml
COPY . .
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -q -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms128m -Xmx384m"
COPY --from=build /src/${MODULE}/target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
