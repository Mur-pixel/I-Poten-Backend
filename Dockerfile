FROM --platform=linux/amd64 amazoncorretto:17

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} /app.jar

ADD https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]

