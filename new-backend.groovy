pipeline {
    agent any
    environment {
    DOCKER_REPO = "backend"
    DOCKER_USER = "shushankbittu"
    CLUSTER_NAME = "meesho"
    REGION = "ap-southeast-2"
}
    stages {
        stage('Code-checkout'){
            steps{
                git branch: 'main',
                    url: 'https://github.com/shushanknagdawane789-eng/Backend-.git'
            }
        }
        stage('Code-build'){
            steps{
                sh 'mvn clean package'
            }
        }
        stage ('Docker-build'){
            steps {
                sh 'docker build -t ${DOCKER_REPO}:${BUILD_NUMBER} . '
            }
        }
        stage ('Docker-login'){
            steps{
                withCredentials([
                            usernamePassword(
                                credentialsId: 'docker-hub-creds',
                                usernameVariable: 'DOCKER_USERNAME',
                                passwordVariable: 'DOCKER_PASSWORD'
                            )
                        ]) 
                        {    
                           sh 'docker login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD}'
                    }
            }
        }
         stage ('Docker-push'){
            steps{
                withCredentials([
                            usernamePassword(
                                credentialsId: 'docker-hub-creds',
                                usernameVariable: 'DOCKER_USERNAME',
                                passwordVariable: 'DOCKER_PASSWORD'
                            )
                        ]) 
                        {    
                           sh '''docker tag ${DOCKER_REPO}:${BUILD_NUMBER} ${DOCKER_USER}/${DOCKER_REPO}:${BUILD_NUMBER}
                           
                                 docker push ${DOCKER_USER}/${DOCKER_REPO}:${BUILD_NUMBER}
                                 docker rmi -f ${DOCKER_USER}/${DOCKER_REPO}:${BUILD_NUMBER}
                           '''
                    }
            }
        }
        stage('Image-Name-change'){
                steps {
          
                    sh '''
sed -i "s|shushankbittu/backend:latest|${DOCKER_USER}/${DOCKER_REPO}:${BUILD_NUMBER}|g" k8s/deployment.yaml
'''
                    sh 'cat k8s/deployment.yaml'
                }
            }
        stage('EKS-deploy'){
            steps{
                withCredentials([aws(accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'aws_creds', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY')]) {

                    sh ''' 
                             aws eks update-kubeconfig \
  --name meesho \
  --region ap-southeast-2
                            kubectl get nodes
                            kubectl apply -f k8s/deployment.yaml
                            kubectl apply -f k8s/service.yaml
                            kubectl get pods 
                            kubectl get deployment
                            kubectl get svc

                    '''

                }       
            }

        }


    }
}
