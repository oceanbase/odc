#!/usr/bin/env bash
# start odc in background

script_source=$(readlink -f $0)
script_directory=$(dirname $script_source)
install_directory=$(dirname $script_directory)

nohup ${script_directory}/start-odc.sh >/dev/null 2>&1 &
ret=$?
pid=$!
echo "start odc-server done, ret=${ret}, pid=${pid}"

sleep 1

echo "check process status:"
ps -p ${pid}
ret=$?
if [ $ret -ne 0 ]; then
    echo "process start failed!"
    echo "please try '${script_directory}/start-odc.sh' for more information!"
else
    echo "process start success!"
    echo "you may check log by 'tailf ${install_directory}/log/odc.log'"
fi
exit $ret
