#!/usr/bin/env bash
# for generate self certificated ssl certificate files

echo try create directory '/etc/pki/nginx' if not exists...
sudo mkdir -p /etc/pki/nginx
cd /etc/pki/nginx || exit

#read configurations
read -rp "Enter your domain [www.example.com]: " DOMAIN
read -rp "Enter your province [Zhejiang]: " PROVINCE
read -rp "Enter your city [Hangzhou]: " CITY
read -rp "Enter your company name [OceanBase]: " COMPANY
read -rp "Enter your company unit name [DBA]: " UNIT

echo generate certificate...
SUBJ="/C=CN/ST=${PROVINCE}/L=${CITY}/O=${COMPANY}/OU=${UNIT}/CN=${DOMAIN}"
sudo openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
    -keyout /etc/pki/nginx/odcserver.key \
    -out /etc/pki/nginx/odcserver.crt \
    -subj "${SUBJ}"

echo "check generated files (use ls -l odcserver.*):"
ls -l odcserver.*
