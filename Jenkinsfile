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
    }
    post {
        always {
            warnings canComputeNew: false, canResolveRelativePaths: false, categoriesPattern: '', consoleParsers: [[parserName: 'Java Compiler (javac)']], defaultEncoding: '', excludePattern: '', healthy: '', includePattern: '', messagesPattern: '', unHealthy: ''
            build job: 'i-actuator', wait: false
        }
    }
}