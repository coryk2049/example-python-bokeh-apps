#!/usr/bin/env bash
source cicd_config.sh
oc delete project ${DEV_PROJECT_NAME}
oc delete project ${STAGE_PROJECT_NAME}
#oc delete project ${PROD_PROJECT_NAME}