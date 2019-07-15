
//oc new-project test-dev
//oc create serviceaccount jenkins -n test-dev
//oc policy add-role-to-user edit system:serviceaccount:test:jenkins -n test-dev 
//oc new-project test-stage
//oc create serviceaccount jenkins -n test-stage
//oc policy add-role-to-user edit system:serviceaccount:test:jenkins -n test-stage
//oc project test-dev; oc delete all --selector app=example-python-bokeh-apps
//oc project test-stage; oc delete all --selector app=example-python-bokeh-apps

pipeline {
    agent {
        label 'agent'
    }

    environment {
        APPLICATION_NAME="example-python-bokeh-apps"
        APPLICATION_VERSION="1.0"
        GIT_REPO="https://github.com/coryk2049/example-python-bokeh-apps.git"
        GIT_BRANCH="master"
        DEV_PROJECT="test-dev"
        STAGE_PROJECT="test-stage"
        STAGE_TAG="promoteToQA"
        DEV_BOKEH_ALLOW_WS_ORIGIN="example-python-bokeh-apps-test-dev.128.60.8.73.nip.io"
        STAGE_BOKEH_ALLOW_WS_ORIGIN="example-python-bokeh-apps-test-stage.128.60.8.73.nip.io"
        SONAR_LOGIN="admin"
        SONAR_PASS="admin"
        SONAR_ENDPOINT="http://sonarqube:9000" 
        SONAR_SRC="./apps/"
    }

    stages {
        stage('Check Out Latest') {
            steps {
                git branch: "${GIT_BRANCH}", url: "${GIT_REPO}"
                sh "pwd; tree"
            }
        }
        stage('Run Unit Test(s)') {
            parallel {
                stage('Unit Test 1') {
                    steps {
                        sleep 1
                    }
                }
                stage('Unit Test 2') {
                    steps {
                        sleep 1
                    }
                }
                stage('Unit Test N') {
                    steps {
                        sleep 1
                    }
                }
            }      
        }    
        stage('Code Analysis') {
            steps {
                script {
                    sh '''
                    SONARQUBE_SCANNER_VERSION=4.0.0.1744
                    wget https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-$SONARQUBE_SCANNER_VERSION-linux.zip
                    unzip sonar-scanner-cli-$SONARQUBE_SCANNER_VERSION-linux.zip 
                    rm sonar-scanner-cli-$SONARQUBE_SCANNER_VERSION-linux.zip 
                    mv sonar-scanner-$SONARQUBE_SCANNER_VERSION-linux sonarqube-scanner
                    sed -i 's/use_embedded_jre=true/use_embedded_jre=false/g' sonarqube-scanner/bin/sonar-scanner
                    ./sonarqube-scanner/bin/sonar-scanner -X -Dsonar.login=${SONAR_LOGIN} -Dsonar.password=${SONAR_PASS} -Dsonar.projectBaseDir=. -Dsonar.projectKey=MyProjectKey -Dsonar.projectName="${APPLICATION_NAME}" -Dsonar.projectVersion="${APPLICATION_VERSION}" -Dsonar.sources=${SONAR_SRC} -Dsonar.host.url=${SONAR_ENDPOINT}
                    '''
                }
            }
        }
        stage('Archive App') {
            steps {
                sleep 1
            }
        }
        stage('Create Image Builder') {
            when {
                expression {
                    openshift.withCluster() {
                        openshift.withProject("${DEV_PROJECT}") {
                            return !openshift.selector("bc", "${APPLICATION_NAME}").exists();
                        }
                    }
                }
            }
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject("${DEV_PROJECT}") {
                            sleep 1
                            //openshift.newBuild("--name=cory-bokeh", "--image-stream=openshift/wildfly:13.0", "--binary=true")
                        }
                    }
                }
            }
        }
        stage('Build Image') {
            when {
                expression {
                    openshift.withCluster() {
                        openshift.withProject("${DEV_PROJECT}") {
                            return !openshift.selector("bc", "${APPLICATION_NAME}").exists();
                        }
                    }
                }
            }
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject("${DEV_PROJECT}") {
                            sleep 1
                            //openshift.selector("bc", "${APPLICATION_NAME}").startBuild("--from-dir=oc-build", "--wait=true")
                        }
                    }
                }
            }
        }
        stage('Create DEV') {
            when {
                expression {
                    openshift.withCluster() {
                        openshift.withProject("${DEV_PROJECT}") {
                            return !openshift.selector("bc", "${APPLICATION_NAME}").exists();
                        }
                    }
                }
            }
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject("${DEV_PROJECT}") {
                            def app = openshift.newApp("${GIT_REPO}")
                            def dc = openshift.selector("dc", "${APPLICATION_NAME}")
                            //openshift.set.env("dc/${APPLICATION_NAME}", "BOKEH_ALLOW_WS_ORIGIN=${DEV_BOKEH_ALLOW_WS_ORIGIN}")

                            sh "oc project"
                            sh "oc project ${DEV_PROJECT}"
                            sh "oc set env dc/${APPLICATION_NAME} BOKEH_ALLOW_WS_ORIGIN=${DEV_BOKEH_ALLOW_WS_ORIGIN}"

                            while (dc.object().spec.replicas != dc.object().status.availableReplicas) {
                                sleep 5
                            }
                            openshift.set("triggers", "dc/${APPLICATION_NAME}", "--manual")
                            app.narrow("svc").expose("--hostname=${DEV_BOKEH_ALLOW_WS_ORIGIN}");
                        }
                    }
                }
            }            
        }
        stage('Deploy DEV') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject("${DEV_PROJECT}") {
                            openshift.selector("dc", "${APPLICATION_NAME}").rollout().latest();
                        }
                    }
                }
            }
        }
        stage('Promote to STAGE?') {
            steps {
                timeout(time:15, unit:'MINUTES') {
                    input message: "Promote to STAGE?", ok: "Promote"
                }
                script {
                    openshift.withCluster() {
                        openshift.tag("${DEV_PROJECT}/${APPLICATION_NAME}:latest", "${STAGE_PROJECT}/${APPLICATION_NAME}:${STAGE_TAG}")
                    }
                }
            }
        }
        stage('Deploy STAGE') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject("${STAGE_PROJECT}") {
                            if (openshift.selector('dc', '${APPLICATION_NAME}').exists()) {
                                openshift.selector('dc', '${APPLICATION_NAME}').delete()
                                openshift.selector('svc', '${APPLICATION_NAME}').delete()
                                openshift.selector('route', '${APPLICATION_NAME}').delete()
                            }
                            def app = openshift.newApp("${APPLICATION_NAME}:${STAGE_TAG}")
                            sh "oc project"
                            sh "oc project ${STAGE_PROJECT}"
                            sh "oc set env dc/${APPLICATION_NAME} BOKEH_ALLOW_WS_ORIGIN=${STAGE_BOKEH_ALLOW_WS_ORIGIN}"
                            app.narrow("svc").expose("--hostname=${STAGE_BOKEH_ALLOW_WS_ORIGIN}");
                        }
                    }
                }
            }
        }
    }
}