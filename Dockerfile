FROM openjdk:17-jdk-slim
WORKDIR /app
COPY ./src/ /app/
RUN javac detector/monitor/*.java detector/simulador/*.java