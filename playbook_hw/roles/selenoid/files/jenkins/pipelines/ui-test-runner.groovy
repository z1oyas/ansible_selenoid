import groovy.json.JsonSlurperClassic

def testsStatistics = [:]

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
                        script: "docker run --rm --name=ui_tests --env-file envs/.env --network=host -v $pwd/allure:/root/ui_tests/allure-results lockalhost:5005/ui_tests:1.0.0",
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

            stage("Gets statistics from allure artifacts") {
                def jsonLines = readFile ".allure-report/widget/summary.json"
                def slurped = new JsonSlurperClassic().parseText(jsonLines)

                slurped.each{k, v ->
                    testsStatistics[k] =v
                }

            }


            stage("Telegram notification") {
                def message ="""=============UI TESTS RESULT ================
                browser name: $BROWSER
                browser version: $BROWSER_VERSION
                """
//-4970638051 chat id
//bot token 8269057546:AAFNVRjY9nx4CbK1Zdd2Xg8NQC4uoL6SD4U

                testsStatistics.each{k,v ->
                    message += "\t\t$k: $v\n"
                }
                withCredentials([string(credentialsId: 'chat_id', variable: 'chatId'), string(credentialsId: 'bot_token',vaiable: 'botToken')]){
                    sh"curl -X POST -H 'Content-Type: application/json' -d '{\"chat_id\": \"$chatId\", \"text\": \"$message\"'" +
                            " \"https://api.telegram.org/bot$botToken/sendMessage\""
                }
            }
        }
        finally {
            stage("Cleanup") {
                sh "docker rm -f ui_tests"
            }
        }
    }
}