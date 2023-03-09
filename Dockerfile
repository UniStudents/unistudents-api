FROM maven:3.8.6-openjdk-11
COPY . /tmp
WORKDIR /tmp
EXPOSE 8080/tcp
ENV PORT 8080
ENV GUEST_PASSWORD pass123
ENV GUEST_USERNAME guest123

RUN ["mvn", "install"]
ENTRYPOINT ["mvn", "spring-boot:run", "-Dspring-boot.run.profiles=prod"]
