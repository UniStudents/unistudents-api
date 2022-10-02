FROM maven:3.3-jdk-8
COPY . /tmp
WORKDIR /tmp
EXPOSE 8080/tcp
ENV PORT 8080
ENV GUEST_PASSWORD pass123
ENV GUEST_USERNAME guest123
<<<<<<< HEAD
RUN echo '\033[0;32m--------------------\n\n\033[0;33mPLEASE WAIT A FEW MINUTES FOR THE IMAGE TO INSTALL ITS DEPENDANCIES AND BOOT! (~1-2mins)\n\033[0;32m~The DevOps Engineer of your heart, donfn~\n\n--------------------\033[0m'
=======
>>>>>>> lib-implementation
ENTRYPOINT ["mvn", "spring-boot:run", "-Dspring-boot.run.profiles=prod"]
