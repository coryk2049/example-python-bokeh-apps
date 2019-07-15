#!/usr/bin/env bash

source cicd_config.sh

oc new-project ${DEV_PROJECT_NAME}
oc create serviceaccount ${JENKINS_SA_NAME} -n ${DEV_PROJECT_NAME}

oc policy add-role-to-user edit system:serviceaccount:${CICD_PROJET_NAME}:${JENKINS_SA_NAME} -n ${DEV_PROJECT_NAME}

oc new-project ${STAGE_PROJECT_NAME}
oc create serviceaccount jenkins -n ${STAGE_PROJECT_NAME}

oc policy add-role-to-user edit system:serviceaccount:${CICD_PROJET_NAME}:${JENKINS_SA_NAME} -n ${STAGE_PROJECT_NAME}

oc new-project ${PROD_PROJECT_NAME}
oc create serviceaccount jenkins -n ${PROD_PROJECT_NAME}

oc policy add-role-to-user edit system:serviceaccount:${CICD_PROJET_NAME}:${JENKINS_SA_NAME} -n ${PROD_PROJECT_NAME}
