timeout(1200){
    node("maven") {
        try {
            sh "mkdir -p envs"

            def yamlConfig = readYaml text: $YAML_CONFIG

            stage("Create environment variables") {
                dir("envs") {
                    sh "BROWSER=${yamlConfig['BROWSER']}" > ".env"
                    sh "BROWSER_VERSION=${yamlConfig['BROWSER_VERSION']}" >> ".env"
                }
            }
            stage("Running UI Automation") {
                def status = sh(
                        script: "dcoker run --rm --name=ui_tests --env-file envs/.env --network=host -v $pwd/allure:/root/ui_tests/allure-results lockalhost:5005/ui_tests:1.0.0",
                        returnStatus: true
                )
                if (status > 0) {
                    currentBuild.result = 'UNSTABLE'
                }
            }

            stage("Allure report publisher") {
                allure([
                        includeProperties: false,
                        jdk              : '',
                        properties       : [],
                        reportBuildPolicy: 'ALWAYS',
                        results          : [[path: 'allure']]
                ])
            }

            stage("Telegram notification") {
                sh "curl"
            }
        }
        finally {
            stage("Cleanup") {
                sh "docker rm -f ui_tests"
            }
        }
    }
}