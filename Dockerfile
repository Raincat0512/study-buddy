# 构建阶段：用 Maven 编译打包
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

# 运行阶段：只带 JRE
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/study-buddy-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
