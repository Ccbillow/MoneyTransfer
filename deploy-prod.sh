#!/bin/bash

mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=prod
