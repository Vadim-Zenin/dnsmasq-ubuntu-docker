#!/bin/bash
#*******************************************************************************
# Author: Vadim Zenin https://github.com/Vadim-Zenin/
# Date:   2018-09-11
#
# Usage: ENTRYPOINT exec /opt/deploy/scripts/run.sh
#
# Tested platform:
# Ubuntu 18.04
#
# This code is made available as is, without warranty of any kind. The entire
# risk of the use or the results from the use of this code remains with the user.
#*******************************************************************************

if [[ ! -f /etc/dnsmasq/dnsmasq.conf ]]; then
  cp -r /opt/deploy/configs/* /etc/dnsmasq/
  echo "initial configs files copy `date +%Y-%m-%d-%H:%M`" > /etc/dnsmasq/configs_files_initial_copy.txt
  echo "initial configs files copied to /etc/dnsmasq/"
  chown -R dnsmasq: /etc/dnsmasq/
fi

echo "`date +%Y-%m-%d-%H:%M` INFO: run.sh executed" >> /var/log/dnsmasq/dnsmasq.log

# top

/usr/sbin/dnsmasq --no-daemon --conf-file=/etc/dnsmasq/dnsmasq.conf --log-facility=/var/log/dnsmasq/dnsmasq.log
