#### (Random) Data Generator

Éste módulo proporciona un proceso que permite generar registros con datos casuales para las colecciones:

1. 'org.keedio.domain.Account'.
2. 'org.keedio.domain.AccountTransaction'.

El data generator genera un número indefinido de objetos **Account** y, para cada uno de ellos, un número configurable de **AccountTransacion**s. El throughput máximo de generación de objetos de tipo **AccountTransaction** es customizable por medio de una property, esto es indispensable para poder medir el throughput máximo de todo el sistema.

La configuración del endpoint de mongodb se encuentra en [./common/src/main/resources/application.conf](./common/src/main/resources/application.conf).

En [src/main/resources/application.conf](./src/main/resources/application.conf) se puede jugar con las siguientes propiedades para customizar el comportamiento del datagenerator:
- `num_txs_per_generated_account`: el número total de inserts/updates/deletes que hacen sobre objetos de tipo AccountTransacion para cada objeto Account generado
- `update_ratio`: ratio de updates a generar
- `delete_ratio`: ratio de deletes a generar
- `rate.limiter`: máximo número de inserts/updates/deletes que se generarán (por segundo) sobre la colección AccountTransaction.

##### Ejemplo
```
num_txs_per_generated_account=100
update_ratio=0.3
delete_ratio=0.05

rate.limiter=1000
```
En este escenario, sobre la colección AccountTransaction se generarán siempre 100 accesos, de los cuales:
- 30 serán updates (update_ratio=0.3)
- 5 serán deletes (delete_ratio=0.05)
- 65 serán inserts (100-30-5)
Además, no se perimitrán más de 1000 escrituras/segundo en la colección AccountTransaction.

##### Configurar el actor de salida
En esta distribuición se proporcionan escritores para syslog, fichero, kafka y mongo.
Para seleccionar el escritor a activar, es suficiente modificar la property active.actor en [./datagenerator/src/main/resources/application.conf](./datagenerator/src/main/resources/application.conf).

###### Configurar actor SyslogLoggerActor
Es necesario setear la property ``active.actor=sysloggerActor``.

Adicionalmente, es necesario configurar el appender de [./datagenerator/src/main/resources/logback.xml](logback) con el endpoint del servidor de syslog

##### Cómo compilar el datagenerator
Para compilar este proyecto es necesario tener instalado SBT (Simple Build Tool)
```
$ sbt assembly
```

##### Lanzar el datagenerator usando el assembly jar
```
$ java <options> -jar datagenerator-assembly-0.1.0-SNAPSHOT.jar
```

**Ejemplo 1**: lanzar el datagenerator con un rate limit de 100, usando el sysloggerActor cómo plugin de salida
```
$ java -Drate.limiter=100 -Dactive.actor=sysloggerActor -Dsyslog.host=localhost -Dsyslog.port=5140 -Dsyslog.facility=USER -jar datagenerator-assembly-0.1.0-SNAPSHOT.jar
```

**Ejemplo 2**: lanzar el datagenerator con un rate limit de 5, usando el kafkaWriterActor cómo plugin de salida
```
$ java -Dactive.actor=kafkaWriterActor -Drate.limiter=5 -Dkafka.brokers=localhost:9093 -Dkafka.topic=myTopicName  -jar datagenerator-assembly-0.1.0-SNAPSHOT.jar
```

**Ejemplo 3**: lanzar el datagenerator con un rate limit de 5, usando el fileWriterActor cómo plugin de salida
```
$ java -Dactive.actor=fileWriterActor -DfileAppender.output=/ruta/fichero/salida.log -Drate.limiter=5 -jar datagenerator-assembly-0.1.0-SNAPSHOT.jar
```

**Ejemplo 4**: lanzar el datagenerator con un rate limit de 5, usando el dbWriterActor cómo plugin de salida:

El dbWriterActor necesita las siguientes cuatro propiedades para funcionar correctamente:

1. 'dbWriter.driverClass', por defecto: com.mysql.jdbc.Driver
2. 'dbWriter.jdbcUrl', por defecto: jdbc:mysql://localhost:3306/logback
3. 'dbWriter.user', por defecto: 'logback'
4. 'dbWriter.password', por defecto: 'logback'

Se puede sobreescribir el valor de cada una de ellas añadiendo el parametro '-D' oportuno.

Adicionalmente, es necesario que en la BBDD se haya previamente creado el esquema usado por el dbappender.

Mas información: [http://logback.qos.ch/manual/appenders.html#DBAppender](http://logback.qos.ch/manual/appenders.html#DBAppender)

Tuneable datagenerator no proporciona ningun driver JDBC, es necesario añadir al classpath la ruta del jar que contiene el driver especifico para la bbdd que se va a usar.

```
$ java -cp ".:datagenerator-assembly-0.1.0-SNAPSHOT.jar:/path/to/db/specific/driver.jar" -D<additional_properties> -Drate.limiter=5 org.keedio.datagenerator.Main
```

**Ejemplo 5**: lanzar el datagenerator para que genere log a partir de un fichero existente:
```
$ java -DinputFileGenerator.sourceFile=/tmp/capped-ironport-mail.log -Drate.limiter=100 -DactiveActor=sysloggerActor -jar datagenerator-assembly-0.1.0.jar
```