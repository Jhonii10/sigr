pipeline {
  agent any
  options { timestamps() }
  stages {
    stage('Checkout') { steps { checkout scm } }
    stage('Backend') {
      steps { dir('backend') { sh 'mvn -B clean verify' } }
      post { always { junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml' } }
    }
    stage('Frontend') {
      steps { dir('frontend') { sh 'npm ci && npm run lint && npm test && npm run build' } }
    }
  }
}
