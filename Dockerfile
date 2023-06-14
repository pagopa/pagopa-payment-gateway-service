FROM openjdk:17-slim
WORKDIR /workspace/app

COPY . .

ADD  ./keys/clientCertificate.jks /opt/keys/

RUN apt-get update && \
	apt-get -y install dos2unix
RUN dos2unix mvnw
RUN chmod 777 mvnw
RUN ./mvnw package -Pspringboot-postgres -q

ENTRYPOINT ["java","-jar","./target/pagopa-payment-transactions-gateway-0.0.0.jar"]