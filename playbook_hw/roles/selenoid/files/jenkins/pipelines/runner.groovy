timeout("300"){
    node("python") {

        withBuildUser {
            def user = env.BUILD_USER
            currentBuild.description = "Running as ${user}"
        }

        def yamlConfig = readYaml text: params.YAML_CONFIG
        def testTypes = yamlConfig['TEST_TYPES']

        def jobs = []
        def testsRunning = [:]
        testTypes.each { type ->
            testsRunning[type] = {
                node('maven') {
                    stage("running test $type") {
                        jobs += build(job:"${type}", propagate: false, wait: true)
                    }
                }
            }
        }

        parallel testsRunning

        stage("publish allure results") {
            jobs.each {job ->
                copyArtifacts(
                        filter: "allure-results/**",
                        projectName: job.getProjectName(),
                        selector: specific("${job.getNumber()}"),
                        target: 'allure-results',
                        flatten: true
                )            }

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