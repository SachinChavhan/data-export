# Based on openjdk-8-jdk-headless 
FROM registry.comprehend.com/java8:176f2753-2020-03-31-063722

ENV WORKING_DIR /home/comprehend
ENV HOME /root
ENV SSH_KEY_DIR $HOME/.ssh

# Install Apt packages
RUN apt-get update -q -q \
  && apt-get upgrade --yes --force-yes \
  && apt-get install -y \
    curl \
    git \
    postgresql-client \
	python2.7 \
    python3.4 \
    python3-pip \
    python-docopt \
    python-numpy \
    python-pip \
    ssh \
    sshpass \
    stunnel \
    unzip \
    vim \
    wget \
  && rm -rf /var/lib/apt/lists/*
  
# Install zookeepercli
RUN curl -OL https://github.com/outbrain/zookeepercli/releases/download/v1.0.10/zookeepercli_1.0.10_amd64.deb \
  && dpkg -i zookeepercli_1.0.10_amd64.deb \
  && rm -f zookeepercli_1.0.10_amd64.deb  

# Add known SSH keys (Github & sftp.comprehend.com)
ADD sftp.comprehend.com /tmp/
RUN mkdir ${SSH_KEY_DIR} \
  && ssh-keyscan github.com >> ${SSH_KEY_DIR}/known_hosts \
  && cat /tmp/sftp.comprehend.com >> ${SSH_KEY_DIR}/known_hosts \
  && rm  /tmp/sftp.comprehend.com

ADD id_rsa.pub ${SSH_KEY_DIR}/id_rsa.pub
ADD id_rsa ${SSH_KEY_DIR}/id_rsa

RUN chmod 700 ${SSH_KEY_DIR}/id_rsa
RUN chmod 700 ${SSH_KEY_DIR}/id_rsa.pub

RUN echo "Host github.com\n\tStrictHostKeyChecking no\n" >> /root/.ssh/config
RUN mkdir -p ${WORKING_DIR}/vault
RUN cat ${SSH_KEY_DIR}/id_rsa >> ${WORKING_DIR}/vault/ccdm_deploy_private_key
RUN cat ${SSH_KEY_DIR}/id_rsa >> ${WORKING_DIR}/vault/data-export_deploy_private_key
RUN cat ${SSH_KEY_DIR}/id_rsa >> ${WORKING_DIR}/vault/din-customer_deploy_private_key

RUN chmod 700 ${WORKING_DIR}/vault/ccdm_deploy_private_key
RUN chmod 700 ${WORKING_DIR}/vault/data-export_deploy_private_key
RUN chmod 700 ${WORKING_DIR}/vault/din-customer_deploy_private_key

# Add SSH configuration
ADD ssh_config ${SSH_KEY_DIR}/config

RUN apt-get install python3.4

ADD export.sh export.sh
CMD sh export.sh