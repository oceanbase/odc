#! /bin/bash
# For stop odc-server process


echo "check odc-server by ps:"
ps aux | grep odc-server | grep -v grep

if [[ "$1" == "--force" ]]; then
    echo "force mode, will use kill -9"
    ps aux | grep odc-server | grep -v grep | awk '{print $2}' | xargs --no-run-if-empty kill -9
else
    ps aux | grep odc-server | grep -v grep | awk '{print $2}' | xargs --no-run-if-empty kill
fi

