FROM ubuntu:focal

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

ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8
ENV CC /usr/bin/gcc
ENTRYPOINT []
