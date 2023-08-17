## Introduction
`unit_test_evaluation_tool.py` 是用于评估单元测试性能状况的工具，功能包括：
- 分析单测的平均耗时、通过率、错误次数、失败次数（可分析全量单测或指定任意测试用例）
- 按错误次数、失败次数和耗时排序，自动获取代码 owner，将测试结果输出到 `csv` 或 `xlsx` 文件

## Usages
#### 使用帮助
执行`python unit_test_evaluation_tool.py --help`查看帮助：
```
usage: python unit_test_evaluation_tool.py [-h] [-r] [-n] [-t] [-i] [-s] [-p] [-d] [-o] [-e]

Tool for evaluating unit test performance
(Requirement: pandas, openpyxl)

optional arguments:
  -h, --help                 show this help message and exit
  -r , --repeat-times        number of times to repeat unit test, default=1
  -n , --number-of-shown     number of top high priority testcase shown below, default=100
  -t , --testcase            specify testcase, the same usages as 'mvn -Dtest=xxx', default=all, meaning run all testcase
  -i , --ignore-failure      determine whether to continue when some testcase fell fail, default=true
  -s , --skip-test           determine whether to skip executing 'mvn test', default=false
  -p , --project-directory   root directory of maven project, default=..
  -d , --reports-directory   root directory where saving or reading surefire reports, default=~/unit-test-results
  -o , --results-directory   root directory where saving evaluation results, default=~/unit-test-results
  -e , --results-type        specify the output results file type, support 'csv' and 'xlsx', default=csv
```
#### 典型样例
##### 样例 1
需要分析`EmptyEncryptorTest`和`AesEncryptorTest`两个测试类的耗时和通过率，可以使用脚本执行 10 次单测，查看统计结果：
```
[/home/localuser/Workspace/ob-odc]
$cd script-test/

[/home/localuser/Workspace/ob-odc/script-test]
$python unit_test_evaluation_tool.py -r 10 -t EmptyEncryptorTest,AesEncryptorTest
```
脚本执行，打印相关信息 log：
```
<-------------------- Summary -------------------->
Test times: 10, Success times: 10, Failures times: 0
Tests: 110
Failures: 0
Errors: 0
Skipped: 0
Success rate: 100.00000%
Time elapsed: 6.580 sec
<-------------------------------------------------------------------------------- unit test result of methods -------------------------------------------------------------------------------->
   Average timecost (sec)  Error times                                                                                    Method Owner
1                   0.072            0                                com.oceanbase.odc.common.crypto.AesEncryptorTest.emptySalt    诣舟
2                   0.017            0  com.oceanbase.odc.common.crypto.AesEncryptorTest.passwordSize64_EncryptedSizeLessThan256    诣舟
3                   0.012            0               com.oceanbase.odc.common.crypto.AesEncryptorTest.encryptDecrypt_long_string  获取失败
4                   0.011            0                   com.oceanbase.odc.common.crypto.AesEncryptorTest.differentKey_Different    诣舟
5                   0.009            0                  com.oceanbase.odc.common.crypto.AesEncryptorTest.differentSalt_Different    诣舟
6                   0.008            0                  com.oceanbase.odc.common.crypto.AesEncryptorTest.differentSalt_Exception    诣舟
7                   0.007            0                   com.oceanbase.odc.common.crypto.AesEncryptorTest.sameKeyTwice_Different    诣舟
8                   0.005            0                   com.oceanbase.odc.common.crypto.AesEncryptorTest.keyLength192_emptySalt    诣舟
9                   0.002            0                           com.oceanbase.odc.common.crypto.AesEncryptorTest.encryptDecrypt  获取失败
10                  0.002            0                          com.oceanbase.odc.common.crypto.EmptyEncryptorTest.empty_Decrypt    诣舟
11                  0.000            0                          com.oceanbase.odc.common.crypto.EmptyEncryptorTest.empty_Encrypt    诣舟

<--------------------------------------------------------------------------------unit test result of classes-------------------------------------------------------------------------------->
  Average timecost (sec)                                               Class  Error times  Failure times Owner
1                  0.515  com.oceanbase.odc.common.crypto.EmptyEncryptorTest            0              0    诣舟
2                  0.143    com.oceanbase.odc.common.crypto.AesEncryptorTest            0              0    诣舟

[INFO] The evaluation results have been saved in /home/gaoda.xy/unit-test-results
```
可在指定的文件夹下（在本例中为 /home/gaoda.xy/unit-test-results ）查看报告文件
##### 样例 2
后台执行 100 次全量单测并将结果输出到表格：
```
[/home/localuser/Workspace/ob-odc/script-test]
$nohup python -u unit_test_evaluation_tool.py -r 100 -e xlsx > ~/unit-test-results/unit_test_log.log 2>&1 &
```

## Builds
#### Dependencies
- Python 2.7 or Python 3
    - pandas 1.4
    - openpyxl 3.0 （不需要导出 xlsx 格式报告时，可以不安装）
- Maven 3.6.1
- Git 2.31.1
#### 安装 pandas, openpyxl
`pip install pandas & pip install openpyxl`

## Notes
1. 依赖项的版本不作强制要求，建议使用较新版本
2. 脚本所有选项都有默认值，在 ob-odc 工程上基本适用
3. 脚本底层依赖执行 `mvn test`，请务必安装 Maven 并使用 `-p` 选项指定工程根目录
4. 获取测试用例的 owner 依赖 `git blame`，没有 Git 环境会获取失败。脚本根据测试用例所在类的包名定位源代码，因此当测试方法是继承自父类的时候会匹配失败
