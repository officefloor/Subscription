pipeline {
	agent any
    
    environment {
        RESULTS_EMAIL = credentials('results-email')
        REPLY_TO_EMAIL = credentials('reply-to-email')
    }
    
    triggers {
        parameterizedCron('''
H 2 * * * 
''')
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
		disableConcurrentBuilds()
		timeout(time: 8, unit: 'HOURS')
    }

	tools {
	    maven 'maven-3.6.0'
	    jdk 'jdk11'
	}
	
	stages {
	
		stage('Build') {
	        steps {
	        	sh 'mvn -version'
	        	echo "JAVA_HOME = ${env.JAVA_HOME}"
	        	sh 'mvn clean install site'
	        }
		    post {
			    always {
					junit allowEmptyResults: true, testResults: 'officefloor/**/target/surefire-reports/TEST-*.xml'
					junit allowEmptyResults: true, testResults: 'officefloor/**/target/failsafe-reports/TEST-*.xml'
			    }
		    }
		}

	}
	
    post {
   		always {
            script {
   				if (currentBuild.result != 'ABORTED') {
	    			emailext to: "${RESULTS_EMAIL}", replyTo: "${REPLY_TO_EMAIL}", subject: 'OoFunImpRct ${BUILD_STATUS}! (${BRANCH_NAME} ${BUILD_NUMBER})', body: '''
${PROJECT_NAME} - ${BUILD_NUMBER} - ${BUILD_STATUS}

Tests:
Passed: ${TEST_COUNTS,var="pass"}
Failed: ${TEST_COUNTS,var="fail"}
Skipped: ${TEST_COUNTS,var="skip"}
Total: ${TEST_COUNTS,var="total"}

${FAILED_TESTS}


Changes (since last successful build):
${CHANGES_SINCE_LAST_SUCCESS}


Log (last lines):
...
${BUILD_LOG}
'''
				}
			}
		}
	}

}