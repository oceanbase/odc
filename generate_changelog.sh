#! /bin/bash
# For generate changelog.rst
# based on configuration file `.gitchangelog.rc`
gitchangelog > CHANGELOG.rst
echo "Change log update to file 'CHANGELOG.rst'"
echo "Please modify file 'CHANGELOG.md' !"
