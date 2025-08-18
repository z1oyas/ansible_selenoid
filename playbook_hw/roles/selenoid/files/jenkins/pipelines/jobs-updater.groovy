def GLOBAL = []

timeout(1200) {
    node("python") {
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
                writeFile(file: 'playbook_hw/roles/selenoid/files/jenkins/job.ini', text: iniConfig, encoding: "UTF-8")
            }
        }

        stage("Upload jobs") {
            echo "обновление джобов"
            dir('playbook_hw/roles/selenoid/files/jenkins') {
                sh '''
                    echo "=== Проверяем что установлено ==="
                    which python3 || echo "python3 not found"
                    which jenkins-jobs || echo "jenkins-jobs not found"
                    python3 -c "import jenkins_jobs" && echo "jenkins_jobs module found" || echo "jenkins_jobs module not found"
                    find /usr -name "*jenkins-jobs*" 2>/dev/null || echo "jenkins-jobs executable not found"
                    
                    echo "=== Пробуем разные варианты запуска ==="
                    python3 -m jenkins_jobs --conf ./job.ini update ./jobs/ || echo "python3 -m jenkins_jobs failed"
                '''
            }
        }

        stage("Verify") {
            echo "Проверка обновления джобов"
            dir('playbook_hw/roles/selenoid/files/jenkins') {
                sh "jenkins-jobs --conf ./job.ini list"
            }
        }
    }
}