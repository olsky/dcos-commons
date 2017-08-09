FROM ubuntu:16.04
RUN apt-get update && apt-get install -y \
    python3 \
    git \
    curl \
    jq \
    default-jdk \
    python-pip \
    python3-dev \
    python3-pip \
    software-properties-common \
    python-software-properties \
    libssl-dev \
    wget \
    zip
# install go 1.7
RUN add-apt-repository -y ppa:longsleep/golang-backports && apt-get update && apt-get install -y golang-go
# AWS CLI for uploading build artifacts
RUN pip install awscli
# Install python test dependencies
RUN pip3 install dcoscli==0.5.3 dcos==0.5.3 dcos-shakedown==1.4.5 teamcity-messages git+https://github.com/dcos/dcos-test-utils@2588a9e79f7eb1b81f37689298b8c83c1f924946
# Install dcos-launch to create clusters for integration testing
RUN apt-get install -y python3-venv
RUN wget https://downloads.dcos.io/dcos-launch/bin/linux/dcos-launch -O /usr/bin/dcos-launch
RUN chmod +x /usr/bin/dcos-launch
# shakedown and dcos-cli require this to output cleanly
ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8
# use an arbitrary path for temporary build artifacts
ENV GOPATH=/go-tmp
# Add the default SSH key
COPY aws_default_ssh_key /root/.ssh/id_rsa
RUN chmod 600 /root/.ssh/id_rsa
