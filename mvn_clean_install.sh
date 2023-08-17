#!/usr/bin/env bash
# maven build, clean install without tests
./mvnw clean install -DskipTests=true -Dmaven.test.skip=true
