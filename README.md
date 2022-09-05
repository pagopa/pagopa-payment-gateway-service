
# pagopa-payment-gateway-service
Payment transactions gateway  for the Payment Manager

# How to start the environment

### Profile jboss-oracle
- ```mvn clean package``` or ```mvn clean package -Pjboss-oracle```
- ```docker-compose -f docker-compose-oracle.yaml up```
- `deploy it on Jboss`
#### Stack
* JAVA 8
* JBOSS
* ORACLE DB

### Profile springboot-postgres
- ```mvn clean package -Pspringboot-postgres```
- ```docker-compose -f docker-compose-springboot-postgres.yaml up```
#### Stack
* JAVA 11
* Spring Boot
* POSTGRES DB

## Test endpoints

TODO
