pipeline {
    triggers {
        pollSCM('H/3 * * * *')
    }
    agent {
        kubernetes {
            yaml """
apiVersion: v1
kind: Pod
spec:
  serviceAccountName: jenkins-admin 
  containers:
  - name: docker
    image: docker:24.0.5-dind
    securityContext:
      privileged: true
    volumeMounts:
    - name: dind-storage
      mountPath: /var/lib/docker
  - name: kubectl
    image: alpine/k8s:1.27.3
    command: ["cat"]
    tty: true
  volumes:
  - name: dind-storage
    emptyDir: {}
"""
        }
    }

    environment {
        IMAGE_NAME = "kwonjh0406/chatbot-be"
    }

    stages {
        stage('1. 코드 가져오기') {
            steps {
                checkout scm
            }
        }

        stage('2. 환경설정 파일 주입') {
            steps {
                container('docker') {
                    withCredentials([file(credentialsId: 'chatbot-be-application-yaml', variable: 'APP_YAML')]) {
                        sh '''
                            mkdir -p src/main/resources
                            cp $APP_YAML src/main/resources/application.yaml
                        '''
                    }
                }
            }
        }

        stage('3. 도커 이미지 빌드 및 푸시') {
            steps {
                container('docker') {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'docker-auth',
                            usernameVariable: 'DOCKER_USERNAME',
                            passwordVariable: 'DOCKER_PASSWORD'
                        )
                    ]) {
                        sh '''
                          docker build -t $IMAGE_NAME:${BUILD_NUMBER} .
                          echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
                          docker push $IMAGE_NAME:${BUILD_NUMBER}
                        '''
                    }
                }
            }
        }

        stage('4. K8s 배포 업데이트') {
            steps {
                container('kubectl') {
                    sh '''
                      sed -i "s/IMAGE_TAG/${BUILD_NUMBER}/g" k8s-deployment.yaml
                      kubectl apply -f k8s-deployment.yaml -n chatbot
                    '''
                }
            }
        }
    }
}
