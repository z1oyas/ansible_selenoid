def GLOBAL = []

timeout(1200) {
    node("maven") {
        stage('checkout') {
            echo "код из репозитория"
            checkout scm
        }

        stage('create ini config') {
            withCredentials([usernamePassword(credentialsId: "jenkins", passwordVariable: 'userPassword', usernameVariable: 'userName')]) {
                def iniConfig = """
[job_builder]
recursive=True
keep_descriptors=False

[jenkins]
url=http://localhost/jenkins/
username=${userName}
password=${userPassword}
"""
                echo "конифг файл job.ini"
                sh "echo '${iniConfig}' > job.ini"
                writeFile(file: 'job.ini', text: iniConfig, encoding: "UTF-8")
            }
        }

        stage("Upload jobs") {
            sh "jenkins-jobs --conf ./job.ini update ./../jobs/"
        }
    }
}