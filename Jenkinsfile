#!groovy
pipeline {
    agent { label 'devel8'}
    stages {
        stage("checkout and build ") {
            steps {
                checkout scm
                sh "mvn verify "
                junit 'reports/**/*Test.xml'
            }
        }
    }
    post {
        always {
            build job: 'i-actuator', wait: false
        }
    }
}