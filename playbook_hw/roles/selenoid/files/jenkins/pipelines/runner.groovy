timeout("300"){
    node("python") {

        withBuildUser {
            def user = env.BUILD_USER
            currentBuild.description = "Running as ${user}"
        }

        def yamlConfig = readYaml text: params.YAML_CONFIG
        def testTypes = yamlConfig['TEST_TYPES']

        def testsRunning = [:]

        testTypes.each { type ->
            testsRunning[type] = {
                node('maven') {
                    stage("running test $type") {
                        return build(job:"${type}", propagate: false, wait: true)
                    }
                }
            }
        }

        // Параллельное выполнение возвращает Map с результатами
        def jobResults = parallel testsRunning

        stage("publish allure results") {
            sh "mkdir -p allure-results"

            echo "Found ${jobResults.size()} completed jobs"

            jobResults.each { testType, job ->
                def jobName = job.getProjectName()
                def jobNumber = job.getNumber()

                echo "Processing job: ${jobName} #${jobNumber}"

                copyArtifacts(
                        filter: "allure-results/**",
                        projectName: jobName,
                        selector: specific("${jobNumber}"),
                        target: "results-${jobName}"
                )
                sh "cp -r results-${jobName}/allure-results/* allure-results/"
                sh "rm -rf results-${jobName}"

                echo "Completed copying artifacts from ${jobName}"
            }

            // Проверяем что файлы скопировались
            sh "ls -la allure-results/"

            allure([
                    includeProperties: false,
                    jdk              : '',
                    properties       : [],
                    reportBuildPolicy: 'ALWAYS',
                    results          : [[path: 'allure-results']]
            ])
        }
    }
}