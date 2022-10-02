FROM maven:3.3-jdk-8
COPY . /tmp
WORKDIR /tmp
EXPOSE 8080/tcp
ENV PORT 8080
ENV GUEST_PASSWORD pass123
ENV GUEST_USERNAME guest123
ENTRYPOINT ["mvn", "spring-boot:run", "-Dspring-boot.run.profiles=prod"]
