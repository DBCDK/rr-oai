def dockerRepository = 'https://docker-os.dbc.dk'

properties([
    disableConcurrentBuilds()
])
if (env.BRANCH_NAME == 'master') {
    properties([
        pipelineTriggers([
            triggers: [
                [
                    $class: 'jenkins.triggers.ReverseBuildTrigger',
                    upstreamProjects: "Docker-payara5-bump-trigger", threshold: hudson.model.Result.SUCCESS
                ]
            ]
        ])
    ])
}
pipeline {
    agent { label "devel8" }
    tools {
        maven "maven 3.5"
    }
    environment {
        MAVEN_OPTS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
    }
    triggers {
        pollSCM("H/3 * * * *")
    }
    options {
        buildDiscarder(logRotator(artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "30", numToKeepStr: "30"))
        timestamps()
    }
    stages {
        stage("build") {
            steps {
                script {
                    def status = sh returnStatus: true, script:  """
                        rm -rf \$WORKSPACE/.repo
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo dependency:resolve dependency:resolve-plugins >/dev/null 2>&1 || true
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo clean
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo --fail-at-end org.jacoco:jacoco-maven-plugin:prepare-agent install -Dsurefire.useFile=false
                    """

                    // We want code-coverage and pmd/findbugs even if unittests fails
                    status += sh returnStatus: true, script:  """
                        mvn -B -Dmaven.repo.local=\$WORKSPACE/.repo pmd:pmd pmd:cpd findbugs:findbugs javadoc:aggregate
                    """

                    junit testResults: '**/target/*-reports/TEST-*.xml'

                    def java = scanForIssues tool: [$class: 'Java']
                    def javadoc = scanForIssues tool: [$class: 'JavaDoc']
                    publishIssues issues:[java, javadoc], unstableTotalAll:1

                    def pmd = scanForIssues tool: [$class: 'Pmd'], pattern: '**/target/pmd.xml'
                    publishIssues issues:[pmd], unstableTotalAll:1

                    def cpd = scanForIssues tool: [$class: 'Cpd'], pattern: '**/target/cpd.xml'
                    publishIssues issues:[cpd]

                    def findbugs = scanForIssues tool: [$class: 'FindBugs'], pattern: '**/target/findbugsXml.xml'
                    publishIssues issues:[findbugs], unstableTotalAll:1

                    step([$class: 'JacocoPublisher',
                          execPattern: 'target/*.exec,**/target/*.exec',
                          classPattern: 'target/classes,**/target/classes',
                          sourcePattern: 'src/main/java,**/src/main/java',
                          exclusionPattern: 'src/test*,**/src/test*,**/*?Request.*,**/*?Response.*,**/*?Request$*,**/*?Response$*,**/*?DTO.*,**/*?DTO$*'
                    ])

                    warnings consoleParsers: [
                         [parserName: "Java Compiler (javac)"],
                         [parserName: "JavaDoc Tool"]],
                         unstableTotalAll: "0",
                         failedTotalAll: "0"

                    if ( status != 0 ) {
                        currentBuild.result = Result.FAILURE
                    }
                }
            }
        }

        stage("docker") {
            steps {
                script {
                    def allDockerFiles = findFiles glob: '**/Dockerfile'
                    def dockerFiles = allDockerFiles.findAll { f -> f.path.endsWith("target/docker/Dockerfile") }
                    def version = readMavenPom().version

                    for (def f : dockerFiles) {
                        def dirName = f.path.take(f.path.length() - "target/docker/Dockerfile".length())
                        if ( dirName == '' )
                            dirName = '.'
                        dir(dirName) {
                            modulePom = readMavenPom file: 'pom.xml'
                            def projectArtifactId = modulePom.getArtifactId()
                            def imageName = "${projectArtifactId}-${version}".toLowerCase()
                            if (! env.CHANGE_BRANCH) {
                                imageLabel = env.BRANCH_NAME.toLowerCase()
                            } else {
                                imageLabel = env.CHANGE_BRANCH.toLowerCase()
                            }
                            if ( ! (imageLabel ==~ /master|trunk/) ) {
                                println("Using branch_name ${imageLabel}")
                                imageLabel = imageLabel.split(/\//)[-1]
                            } else {
                                println(" Using Master branch ${BRANCH_NAME}")
                                imageLabel = env.BUILD_NUMBER
                            }

                            println("In ${dirName} build ${projectArtifactId} as ${imageName}:$imageLabel")

                            def app = docker.build("$imageName:${imageLabel}", '--pull --no-cache --file target/docker/Dockerfile .')

                            if (currentBuild.resultIsBetterOrEqualTo('SUCCESS')) {
                                docker.withRegistry(dockerRepository, 'docker') {
                                    app.push()
                                    if (env.BRANCH_NAME ==~ /master|trunk/) {
                                        app.push "latest"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    post {
        success {
            step([$class: 'JavadocArchiver', javadocDir: 'target/site/apidocs', keepAll: false])
            archiveArtifacts artifacts: '**/target/*-jar-with-dependencies.jar', fingerprint: true
        }
    }
}
