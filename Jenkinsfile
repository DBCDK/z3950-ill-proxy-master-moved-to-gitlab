#!groovy
pipeline {
    agent { label 'devel8'}
    stages {
        stage("checkout and build ") {
            steps {
                checkout scm
                sh "mvn verify "
                junit 'target/surefire-reports/*.xml'
            }
        }
        stage("docker image") {
            steps {
                sh "cp target/z3950-ill-proxy-1.0-SNAPSHOT.war src/main/docker/"
                def imageName="z3950-ill-proxy"
                def imageLabel=${BUILD_NUMBER}

                dir("src/main/docker/") {
                    def app = docker.build("$imageName:${imageLabel}".toLowerCase(), '--pull --no-cache .')
                    if (currentBuild.resultIsBetterOrEqualTo('SUCCESS')) {
                        docker.withRegistry('https://docker-i.dbc.dk', 'docker') {
                            app.push()
                            app.push("latest")
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            warnings canComputeNew: false, canResolveRelativePaths: false, categoriesPattern: '', consoleParsers: [[parserName: 'Java Compiler (javac)']], defaultEncoding: '', excludePattern: '', healthy: '', includePattern: '', messagesPattern: '', unHealthy: ''
            build job: 'i-actuator', wait: false
        }
    }
}