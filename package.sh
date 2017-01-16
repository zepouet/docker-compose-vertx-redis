#!/usr/bin/env bash

docker run -d --name redis redis:3.0.7
mvn clean package -DskipTests

