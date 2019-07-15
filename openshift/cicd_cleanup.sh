#!/usr/bin/env bash

source cicd_config.sh
oc project ${DEV_PROJECT_NAME}
oc delete all --selector app=${APPLICATION_NAME}
oc project ${STAGE_PROJECT_NAME}
oc delete all --selector app=${APPLICATION_NAME}
