#!/bin/bash

cd $WORKSPACE/pbik
sbt -jvm-debug 5005 -DOverridePbikUrl=http://localhost:9352/nps-hod-service/services/nps "run 9351"
