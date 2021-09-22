#!/usr/bin/env groovy

@Library('apm@current') _

pipeline {
  agent any
  environment {
    REPO = 'ecs-logging-java'
    BASE_DIR = "src/github.com/elastic/${env.REPO}"
    NOTIFY_TO = credentials('notify-to')
    JOB_GCS_BUCKET = credentials('gcs-bucket')
    DOCKERHUB_SECRET = 'secret/apm-team/ci/elastic-observability-dockerhub'
    MAVEN_CONFIG = '-Dmaven.repo.local=.m2'
    JAVA_HOME = "${env.HUDSON_HOME}/.java/java11"
    HOME = "${env.WORKSPACE}"
    PATH = "${env.HOME}/bin:${env.JAVA_HOME}/bin:${env.PATH}"
  }
  options {
    timeout(time: 1, unit: 'HOURS')
    buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '20', daysToKeepStr: '30'))
    timestamps()
    ansiColor('xterm')
    disableResume()
    durabilityHint('PERFORMANCE_OPTIMIZED')
    rateLimitBuilds(throttle: [count: 60, durationName: 'hour', userBoost: true])
    quietPeriod(10)
  }
  triggers {
    issueCommentTrigger("${obltGitHubComments()}")
  }
  parameters {
    string(name: 'MAVEN_CONFIG', defaultValue: '-B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn', description: 'Additional maven options.')
    booleanParam(name: 'Run_As_Master_Branch', defaultValue: false, description: 'Allow to run any steps on a PR, some steps normally only run on master branch.')
    booleanParam(name: 'test_ci', defaultValue: true, description: 'Enable test')
    booleanParam(name: 'doc_ci', defaultValue: true, description: 'Enable doc validation')
  }

  stages {
    stage('Initializing'){
      agent { label 'linux && immutable' }
      options { skipDefaultCheckout() }
      environment {
        MAVEN_CONFIG = "${params.MAVEN_CONFIG} ${env.MAVEN_CONFIG}"
      }
      stages {
        /**
         Checkout the code and stash it, to use it on other stages.
        */
        stage('Checkout') {
          steps {
            deleteDir()
            gitCheckout(basedir: env.BASE_DIR, githubNotifyFirstTimeContributor: true)
            stash allowEmpty: true, name: 'source', useDefaultExcludes: false
          }
        }
        /**
        Build on a linux environment.
        */
        stage('Build') {
          options { skipDefaultCheckout() }
          steps {
            withGithubNotify(context: 'Build', tab: 'artifacts') {
              deleteDir()
              unstash 'source'
              dir(BASE_DIR){
                sh """#!/bin/bash
                set -euxo pipefail
                ./mvnw clean install -DskipTests=true -Dmaven.javadoc.skip=true
                ./mvnw license:aggregate-third-party-report -Dlicense.excludedGroups=^co\\.elastic\\.
                """
                archiveArtifacts allowEmptyArchive: true, onlyIfSuccessful: true,
                                 artifacts: 'target/site/aggregate-third-party-report.html'
              }
              stash allowEmpty: true, name: 'build', useDefaultExcludes: false
            }
          }
        }
      }
    }
    stage('Tests') {
      environment {
        MAVEN_CONFIG = "${params.MAVEN_CONFIG} ${env.MAVEN_CONFIG}"
      }
      parallel {
        /**
          Run only unit test.
        */
        stage('Unit Tests') {
          agent { label 'linux && immutable' }
          options { skipDefaultCheckout() }
          when {
            beforeAgent true
            expression { return params.test_ci }
          }
          steps {
            withGithubNotify(context: 'Unit Tests', tab: 'tests') {
              deleteDir()
              unstash 'build'
              dir(BASE_DIR){
                sh './mvnw test'
              }
            }
          }
          post {
            always {
              junit(allowEmptyResults: true, keepLongStdio: true,
                    testResults: "${BASE_DIR}/**/junit-*.xml,${BASE_DIR}/**/TEST-*.xml")
            }
          }
        }
        /**
          Build javadoc files.
        */
        stage('Javadoc') {
          agent { label 'linux && immutable' }
          options { skipDefaultCheckout() }
          when {
            beforeAgent true
            expression { return params.doc_ci }
          }
          steps {
            withGithubNotify(context: 'Javadoc') {
              deleteDir()
              unstash 'build'
              dir(BASE_DIR){
                sh './mvnw compile javadoc:javadoc'
              }
            }
          }
        }
        /**
          Build javadoc files.
        */
        stage('Sanity checks') {
          agent { label 'linux && immutable' }
          options { skipDefaultCheckout() }
          steps {
            withGithubNotify(context: 'Sanity checks', tab: 'tests') {
              deleteDir()
              unstash 'source'
              dir(BASE_DIR){
                preCommit(commit: "${GIT_BASE_COMMIT}", junit: true)
              }
            }
          }
        }
      }
    }
  }
  post {
    cleanup {
      notifyBuildResult()
    }
  }
}
