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
timeout=60
"""
                echo "конифг файл job.ini"
                writeFile(file: 'playbook_hw/roles/selenoid/files/jenkins/job.ini', text: iniConfig, encoding: "UTF-8")
            }
        }

        stage("Upload jobs") {
            dir('playbook_hw/roles/selenoid/files/jenkins/') {
                sh "jenkins-jobs --conf ./job.ini update ./jobs/"
                }
            }
    }
}