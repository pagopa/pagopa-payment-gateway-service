FROM openjdk:17-slim@sha256:aaa3b3cb27e3e520b8f116863d0580c438ed55ecfa0bc126b41f68c3f62f9774
WORKDIR /workspace/app

COPY . .

ADD  ./keys/clientCertificate.jks /opt/keys/

RUN apt-get update && \
	apt-get -y install dos2unix
RUN dos2unix mvnw
RUN chmod 777 mvnw
RUN ./mvnw package -Pspringboot-postgres -q

ENTRYPOINT ["java","-jar","./target/pagopa-payment-transactions-gateway-0.0.0.jar"]