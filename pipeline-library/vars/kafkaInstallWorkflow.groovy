def call() {
    pipeline {
        agent any
        environment {
            CONFIG = readYaml file: 'configs/kafka-ansible-config.yaml' // Reads configuration from the predefined path
        }
        stages {
            stage('Clone Ansible Repository') {
                steps {
                    script {
                        echo "Cloning Ansible repository from ${CONFIG.repository.url}..."
                        git branch: CONFIG.repository.branch,
                            url: CONFIG.repository.url
                    }
                }
            }

            stage('User Approval') {
                steps {
                    script {
                        input message: "Do you approve the installation of Kafka on the target environment: ${CONFIG.ansible.inventory}?",
                              ok: "Proceed"
                    }
                }
            }

            stage('Execute Ansible Playbook') {
                steps {
                    script {
                        def inventory = CONFIG.ansible.inventory
                        def playbook = CONFIG.ansible.playbook

                        echo "Executing Ansible playbook: ${playbook} with inventory: ${inventory}"

                        sh """
                        ansible-playbook -i ${inventory} ${playbook}"
                        """
                    }
                }
            }

            stage('Notification') {
                steps {
                    script {
                        def notificationType = CONFIG.notification.type
                        def message = "Kafka installation on ${CONFIG.ansible.inventory} completed successfully."

                        if (notificationType == 'slack') {
                            echo "Sending Slack notification to channel ${CONFIG.notification.channel}..."
                            slackSend channel: CONFIG.notification.channel,
                                      message: message
                        }
                    }
                }
            }
        }

        post {
            success {
                echo "Kafka installation pipeline completed successfully."
            }
            failure {
                echo "Kafka installation pipeline failed. Please check the logs for more details."
            }
        }
    }
}
