FROM eclipse-temurin:17-jdk-jammy

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

COPY i-poten-1fdd0-firebase-adminsdk-fbsvc-573e118da8.json .

ADD https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh /
RUN chmod +x /wait-for-it.sh



ENTRYPOINT ["java", "-jar", "/app.jar"]
