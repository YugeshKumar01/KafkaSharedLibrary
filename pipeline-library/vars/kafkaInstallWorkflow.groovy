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
                            credentialsId: CONFIG.repository.credentialsId,
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
                        def extraVars = CONFIG.ansible.extraVars.collect { k, v -> "${k}=${v}" }.join(' ')

                        echo "Executing Ansible playbook: ${playbook} with inventory: ${inventory} and extra vars: ${extraVars}"

                        sh """
                        ansible-playbook -i ${inventory} ${playbook} --extra-vars "${extraVars}"
                        """
                    }
                }
            }

            stage('Notification') {
                steps {
                    script {
                        def notificationType = CONFIG.notification.type
                        def recipients = CONFIG.notification.recipients.join(',')
                        def message = "Kafka installation on ${CONFIG.ansible.inventory} completed successfully."

                        if (notificationType == 'email') {
                            echo "Sending email notification to ${recipients}..."
                            emailext subject: "Kafka Installation Status",
                                     body: message,
                                     to: recipients
                        } else if (notificationType == 'slack') {
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
