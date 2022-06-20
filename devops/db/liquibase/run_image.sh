/home/oracle/liquibase/liquibase --changeLogFile=/home/oracle/changelogs/payment-transaction-gateway/master-changelog.xml --url="jdbc:oracle:thin:@host.docker.internal:1521:ORCLCDB" --username=PPTEST --password=BW7P8U --log-level=INFO --driver=oracle.jdbc.driver.OracleDriver --classpath=/home/oracle/liquibase/com.oracle.ojdbc8-12.2.0.1.jar update

while [ $? -ne 0 ]
do
	sleep 60s
	/home/oracle/liquibase/liquibase --changeLogFile=/home/oracle/changelogs/payment-transaction-gateway/master-changelog.xml --url="jdbc:oracle:thin:@host.docker.internal:1521:ORCLCDB" --username=PPTEST --password=BW7P8U --log-level=INFO --driver=oracle.jdbc.driver.OracleDriver --classpath=/home/oracle/liquibase/com.oracle.ojdbc8-12.2.0.1.jar update
done