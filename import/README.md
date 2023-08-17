### Oblcient依赖包

obclient-x86.tar.gz 由obclient-1.2.8_odc-20220505160422.el7.alios7.x86_64.rpm安装，删除掉无用lib，再次打包而成，用于集成命令行工具供白屏使用

obclient-aarch64.tar.gz 由obclient-1.2.8_odc-20220505160422.el7.alios7.aarch64.rpm安装，删除掉无用lib，再次打包而成，用于集成命令行工具供白屏使用

以上rpm包为特殊版本，屏蔽掉有可能产生系统安全问题或者无用命令后重新打包而成，以下是命令列表

Whitelist:
?         (\?) Synonym for `help'.  
delimiter (\d) Set statement delimiter.  
help      (\h) Display this help.  
source    (\.) Execute an SQL script file. Takes a file name as an argument.  
status    (\s) Get status information from the server.  
use       (\u) Use another database. Takes database name as argument.  
warnings  (\W) Show warnings after every statement.  
nowarning (\w) Don't show warnings after every statement.  
charset   (\C) Switch to another charset. Might be needed for processing  
clear     (\c) Clear the current input statement.  
ego       (\G) Send command to mysql server, display result vertically.  
exit      (\q) Exit mysql. Same as quit.  
go        (\g) Send command to mysql server.
quit      (\q) Quit mysql.  

Blacklist:  
connect   (\r) Reconnect to the server. Optional arguments are db and host.  
conn      (\) Reconnect to the server. Optional arguments are db and host.  
edit      (\e) Edit command with $EDITOR.
nopager   (\n) Disable pager, print to stdout.  
notee     (\t) Don't write into outfile.  
pager     (\P) Set PAGER [to_pager]. Print the query results via PAGER.  
print     (\p) Print current command.  
prompt    (\R) Change your mysql prompt.  
rehash    (\#) Rebuild completion hash.  
system    (\!) Execute a system shell command.  
tee       (\T) Set outfile [to_outfile]. Append everything into given outfile.  
binlog with multi-byte charsets.  
resetconnection(\x) Clean session context.  