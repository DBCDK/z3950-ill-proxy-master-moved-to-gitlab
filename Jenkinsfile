#!groovy
pipeline {
    agent { label 'devel8' }
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
                script {

                    def imageName="z3950-ill-proxy"
                    def imageLabel=BUILD_NUMBER

                    dir("src/main/docker/") {
                        sh 'cp  ../../../target/*.war .'
                        //def app = docker.build("$imageName:${imageLabel}".toLowerCase(), '--pull --no-cache .')
                        // Work around bug https://issues.jenkins-ci.org/browse/JENKINS-44609 , https://issues.jenkins-ci.org/browse/JENKINS-44789
                        sh "docker build -t $imageName:${imageLabel} --pull --no-cache ."
                        if (currentBuild.resultIsBetterOrEqualTo('SUCCESS')) {
                            docker.withRegistry('https://docker-fbiscrum.artifacts.dbccloud.dk', 'docker') {
                                app = docker.image("$imageName:${imageLabel}")  // Load image by name:tag bug JENKINS-44609 and JENKINS-44789
                                app.push()
                                app.push("latest")
                            }
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