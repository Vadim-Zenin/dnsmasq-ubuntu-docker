## dnsmasq_ubuntu_18.04

Lightweight and simple DNS, DHCP and TFTP server on official Ubuntu 18.04

dnsmasq version: 2.79

### Docker image: Ubuntu 18.04 (Bionic Beaver)

The image is intended to provide DNS, DHCP and TFTP service in small, middle and Development environments.

> **Important Note**:
>
> Please use an attached volume. Example:  `-v /opt/dnsmasq/etc:/etc/dnsmasq`
> The image will extract default images if an attached volume either directory empty or dnsmasq.conf deleted/renamed. 
> Please copy your modified files before you rename or delete dnsmasq.conf in the attached volume.
> If you do not use the attached volume all configurations will be reset to initial after docker restart.

### Build a docker image

This image is built automatically and can be obtained on [Docker Hub](https://hub.docker.com/r/vadimzenin/dnsmasq_ubuntu_18.04/).

Use the following instructions if you need to manually build the image:

- [Install Docker](https://docs.docker.com/engine/installation/).
- Clone this repo to local directory.
- `pushd ` into directory.
- Run 
```bash
docker build --no-cache=true --build-arg BUILD_VERSION="0.9.1.$(date -u +'+%Y%m%d%H%M')" --compress -t dnsmasq_ubuntu_18.04 .
```

### Tag and Pull the image

```bash
docker login mysite.mydomain.com:8888
docker tag dnsmasq_ubuntu_18.04:0.9.2.201809102309 mysite.mydomain.com:8888/dnsmasq_ubuntu_18.04:0.9.2.201809102309
docker pull mysite.mydomain.com:8888/dnsmasq_ubuntu_18.04:0.9.2.201809102309
```

### Run the docker image

- Please check if port 53 used by different application
```bash
netstat -lntp | grep 53
```
- Stop the application
```bash
systemctl status systemd-resolved
systemctl stop systemd-resolved
```
- Create required folders for dnsmasq configuration files
```bash
mkdir -p -m 775 /opt/dnsmasq/etc/
```
- Run the docker image with ports for DNS and TFTPD
```bash
docker run -it --rm --name test -p 53:53/tcp -p 53:53/udp -p 69:69/udp -v /opt/dnsmasq/etc:/etc/dnsmasq vadimzenin/dnsmasq_ubuntu_18.04:latest
```

If port(s) are used system return message:

Error starting userland proxy: listen tcp 0.0.0.0:53: bind: address already in use.

### Run the docker image without standard DNS ports. DNS would not work properly.

```bash
mkdir -p -m 775 /opt/dnsmasq/etc/
docker run -it --rm --name test -p 2253:53/tcp -p 2253:53/udp -p 2269:69/udp -v /opt/dnsmasq/etc:/etc/dnsmasq vadimzenin/dnsmasq_ubuntu_18.04:latest
docker run -it --rm --name test -p 2253:53/tcp -p 2253:53/udp -p 2269:69/udp -v /opt/dnsmasq/etc:/etc/dnsmasq <imageId>
```

### Source

Source files location is [GitHub](https://github.com/Vadim-Zenin/dnsmasq-ubuntu-docker)

### Continuous integration

TeamCity Kotlin script located in .teamcity/


### Docker registry

Docker registry [link](https://hub.docker.com/r/vadimzenin/dnsmasq_ubuntu_18.04/)

### License

Licensed under the GNU GENERAL PUBLIC LICENSE Version 3 License.  See the LICENSE file for details.

### Author Information

Created by [Vadim Zenin](https://github.com/Vadim-Zenin/).
