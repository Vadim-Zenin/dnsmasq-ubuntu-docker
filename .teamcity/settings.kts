import jetbrains.buildServer.configs.kotlin.v2018_1.*
import jetbrains.buildServer.configs.kotlin.v2018_1.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.v2018_1.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2018_1.projectFeatures.dockerRegistry
import jetbrains.buildServer.configs.kotlin.v2018_1.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2018_1.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2018.1"

project {

    vcsRoot(SshGitMysiteNydomainCom)

    buildType(GeneralPipeline)

    params {
        param("docker_repository_address", "mysite.mydomain.com:8888")
        param("build.timestamp.format", "YYYYmmddHHMM")
        param("docker_container_name", "test_%build.counter%")
        param("docker_image_name", "dnsmasq_ubuntu_18.04")
    }

    features {
        dockerRegistry {
            id = "PROJECT_EXT_3"
            name = "docker-temp"
            url = "https://mysite.mydomain.com:8888"
            userName = "dnsmasq-ubuntu-docker"
            password = ""
        }
    }
}

object GeneralPipeline : BuildType({
    name = "General Pipeline"

    buildNumberPattern = "0.2.%build.counter%.%build.formatted.timestamp%"

    params {
        param("test_dns_record_name", "my-example-host02.my-example-localdomain.localz")
        param("app_name", "dnsmasq")
        param("build.timestamp.format", "YYYYMMddHHMM")
        param("my_temp1", "null")
        param("docker_container_name", "%docker_image_name%_%build.counter%")
        param("test_dns_record_value", "192.168.32.8")
    }

    vcs {
        root(DslContext.settingsRoot)
        root(SshGitMysiteNydomainCom)

        checkoutMode = CheckoutMode.ON_SERVER
        cleanCheckout = true
    }

    steps {
        script {
            name = "Prepare_Environment"
            scriptContent = """
                if [ ! -d "/opt/%app_name%/etc" ]; then
                    rm -fr /opt/%app_name%/etc/*
                fi
                mkdir -p -m 775 /opt/%app_name%/etc
            """.trimIndent()
        }
        dockerCommand {
            name = "Docker_build"
            commandType = build {
                source = path {
                    path = "Dockerfile"
                }
                namesAndTags = "%docker_image_name%:%build.number%"
                commandArgs = "--no-cache=true --build-arg BUILD_VERSION=%build.number% --compress"
            }
        }
        script {
            name = "Docker_run_schell"
            scriptContent = """
                echo "docker run -d --rm --name %docker_container_name% --hostname %docker_container_name% -p 5353:53/tcp -p 5353:53/udp -v /opt/dnsmasq/etc:/etc/dnsmasq" %docker_image_name%:%build.number%
                docker run -d --rm --name %docker_container_name% --hostname %docker_container_name% -p 5353:53/tcp -p 5353:53/udp -v /opt/dnsmasq/etc:/etc/dnsmasq %docker_image_name%:%build.number%
                sleep 10
            """.trimIndent()
        }
        script {
            name = "Docker_running_check"
            scriptContent = """
                docker ps -a | grep "%docker_container_name%"
                if [ ${'$'}(docker inspect -f '{{.State.Running}}' "%docker_container_name%") = "true" ] 
                  then echo "INFO: Docker container %docker_container_name% is running"
                  return 0
                  else echo "ERROR: Docker container %docker_container_name% is not running"
                  return 1
                fi
            """.trimIndent()
        }
        script {
            name = "Application_test"
            scriptContent = """
                echo "testing %app_name%"
                
                echo "${'$'}(dig +short %test_dns_record_name% @${'$'}(docker ps -a | grep "%docker_container_name%" | awk '{print ${'$'}1}' | xargs docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'))" == "%test_dns_record_value%"
                
                my_temp1=${'$'}(docker ps -a | grep "%docker_container_name%" | awk '{print ${'$'}1}' | xargs docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}')
                echo "INFO: Docker container IP address: ${'$'}my_temp1"
                
                my_temp1="${'$'}(dig +short %test_dns_record_name% ${'$'}(echo "@${'$'}my_temp1"))"
                echo "INFO: Current IP of requested DNS record: ${'$'}my_temp1"
                
                python -c "assert('${'$'}my_temp1' == '%test_dns_record_value%')"
                if [ ${'$'}? -ne 0 ]; then
                    echo "ERROR: test failed"
                    return 1
                else 
                    echo "INFO: test successful"
                    return 0
                fi
            """.trimIndent()
        }
        script {
            name = "Docker_stop"
            executionMode = BuildStep.ExecutionMode.RUN_ON_FAILURE
            scriptContent = """docker ps -a | grep "%docker_container_name%" | awk 'BEGIN{FS=" "} {print ${'$'}1}' | xargs docker stop"""
        }
        script {
            name = "Docker_login"
            scriptContent = "docker login -u %docker_klv_user_name% -p %buildsvc_credentials% %docker_repository_address%"
        }
        script {
            name = "Docker_tag"
            scriptContent = """
                echo "docker tag %docker_image_name%:%build.number% %docker_repository_address%/%docker_image_name%:%build.number%"
                docker tag %docker_image_name%:%build.number% %docker_repository_address%/%docker_image_name%:%build.number%
            """.trimIndent()
        }
        script {
            name = "Docker_push"
            scriptContent = """
                # Please login to docker registry first time from teamcity console
                # docker login <nexus-hostname>:<repository-port>
                echo "docker push %docker_repository_address%/%docker_image_name%:%build.number%"
                docker push %docker_repository_address%/%docker_image_name%:%build.number%
            """.trimIndent()
        }
        script {
            name = "Docker_image_delete"
            executionMode = BuildStep.ExecutionMode.RUN_ON_SUCCESS
            scriptContent = """
                docker ps -a | grep "%docker_container_name%" | awk 'BEGIN{FS=" "} {print ${'$'}1}' | xargs docker stop
                docker ps -a | grep "%docker_container_name%" | awk 'BEGIN{FS=" "} {print ${'$'}1}' | xargs docker rm
                docker images --format '{{.Repository}};{{.Tag}};{{.ID}}' | grep "%docker_image_name%" | grep "build.number%"| awk 'BEGIN{FS=";"} {print ${'$'}3}' | xargs docker rmi -f
                return 0
            """.trimIndent()
            formatStderrAsError = true
        }
    }

    triggers {
        vcs {
        }
    }

    failureConditions {
        executionTimeoutMin = 10
    }
})

object SshGitMysiteNydomainCom : GitVcsRoot({
    name = "ssh://git@mysite.mydomain.com:/dnsmasq-ubuntu-docker.git#develop"
    url = "ssh://git@mysite.mydomain.com:/dnsmasq-ubuntu-docker.git"
    branchSpec = "+:develop*"
    agentCleanPolicy = GitVcsRoot.AgentCleanPolicy.ALWAYS
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "dnsmasq-ubuntu-docker.priv.key"
    }
})
