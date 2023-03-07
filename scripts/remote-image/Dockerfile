# Our images must be for Linux x86_64
FROM --platform=linux/amd64 ubuntu:focal

ENV DEBIAN_FRONTEND=noninteractive

# Required for a base build of java targets
RUN apt-get -qqy update && \
    apt-get install -qy --no-install-recommends \
      build-essential \
      libcrypt-dev \
      libnet1 \
      libzip-dev \
      linux-libc-dev \
      python3.9 \
      python-is-python3 \
      zip \
      zlib1g && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/*

ADD https://github.com/bazelbuild/bazelisk/releases/download/v1.16.0/bazelisk-linux-amd64 /usr/bin/bazel
RUN chmod +x /usr/bin/bazel

RUN apt-get -qqy update && \
    apt-get install -qy --no-install-recommends \
      ca-certificates && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/*

RUN apt-get update -qqy && \
    apt-get -qy install firefox && \
    dpkg -r --force-depends firefox && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/*

# Ruby requirements
RUN apt-get -qqy update && \
    apt-get install -qy --no-install-recommends \
      libssl-dev openssl && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/*

# Ruby requirements
RUN apt-get -qqy update && \
    apt-get install -qy --no-install-recommends \
      curl && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/*

RUN apt-get update -qqy && apt-get install -y wget curl gnupg2
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - && echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list
RUN apt-get update -qqy && \
    apt-get -qy install google-chrome-stable && \
    dpkg -r --force-depends google-chrome-stable && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/*


ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8
ENV CC /usr/bin/gcc
ENTRYPOINT []
