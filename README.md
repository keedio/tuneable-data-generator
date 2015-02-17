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

##### Cómo lanzar el datagenerator
desde la carpeta principal del proyecto, y no desde la carpeta del módulo datagenerator:
```
$ sbt datagenerator/run
```

para arrancar la generacion a fichero, usar el siguiente comando:
```
$ sbt datagenerator/run -DfileAppender.output=/ruta/fichero/salida.log
```

##### Lanzar el datagenerator usando el assembly jar
```
$ java <options> -jar datagenerator-assembly-0.1.0-SNAPSHOT.jar
```

Ejemplo, lanzar el datagenerator con un rate limit de 100, usando el sysloggerActor cómo plugin de salida
```
$ java -Drate.limiter=100 -Dactive.actor=sysloggerActor -jar datagenerator-assembly-0.1.0-SNAPSHOT.jar
```
