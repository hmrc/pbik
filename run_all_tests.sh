#!/usr/bin/env bash

sbt clean scalafmtAll compile coverage Test/test coverageOff dependencyUpdates coverageReport
