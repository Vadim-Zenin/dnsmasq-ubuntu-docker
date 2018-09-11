FROM ubuntu:18.04

ARG BUILD_VERSION

LABEL org.label-schema.maintainer="Vadim Zenin" \
  org.label-schema.docker.schema-version="1.0" \
  org.label-schema.name="dnsmasq_ubuntu_18.04" \
  org.label-schema.version=$BUILD_VERSION \
  org.label-schema.description=="dnsmasq on official Ubuntu 18.04" \
  org.label-schema.docker.cmd="docker run -d --rm --name dnsmasq -p 5353:53/tcp \
  -v <your_path>/opt/dnsmasq/etc:/etc/dnsmasq <imageId>" 

# maintainer="" \
# org.label-schema.vendor="" \
# org.label-schema.build-date=$BUILD_DATE \

# Set noninteractive
ENV DEBIAN_FRONTEND noninteractive

# Install packages and Clean up
RUN adduser --uid 307 --ingroup nogroup --disabled-password --gecos 'dnsmasq' --shell /bin/false dnsmasq \
  && apt-get -qq update \
  && apt-get install -y dnsmasq \
  && apt-get clean autoclean \
  && apt-get autoremove -y \
  && rm -rf /var/lib/apt/lists/* \
  && rm -rf /usr/share/doc/* \
  && rm -rf /usr/share/man/*

RUN mkdir -p -m 775 /etc/dnsmasq/ \
  && mkdir -p -m 775 /opt/deploy/ \
  && mv /etc/dnsmasq.conf /etc/dnsmasq/dnsmasq.conf.dist \
  && mkdir -p -m 775 /var/log/dnsmasq/

COPY opt/deploy /opt/deploy

RUN mv /etc/default/dnsmasq /opt/deploy/configs/dnsmasq.default.dist \
  && mv /etc/dnsmasq.d /opt/deploy/configs/ \
  && cp -r /opt/deploy/configs/* /etc/dnsmasq/ \
  && chown -R dnsmasq: /etc/dnsmasq/ \
  && ln -s /etc/dnsmasq/dnsmasq.default /etc/default/dnsmasq \
  && ln -s /etc/dnsmasq/dnsmasq.conf /etc/dnsmasq.conf \
  && chmod +x /opt/deploy/scripts/*.sh \
  && ls -la /etc/dnsmasq/* \
  && ls -la /opt/deploy/* 

EXPOSE 53/tcp 53/udp

ENTRYPOINT exec /opt/deploy/scripts/run.sh
