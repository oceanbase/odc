# -*- coding: utf-8 -*
# @Author  : gaoda.xy
# @Time    : 2022/7/21

import os
import glob
import argparse
import pandas as pd
import xml.etree.ElementTree as elementTree
from functools import cmp_to_key


# Struct of unit test results for single test method
class TestMethod(object):
    def __init__(self, _name=None, _classname=None, _time=0.0, _error=False):
        self.name = _name
        self.classname = _classname
        self.time = _time
        self.error = _error


# Struct of unit test results for single test class
class TestClass(object):
    def __init__(self, _name=None, _time=0.0, _tests=0, _errors=0, _skipped=0, _failures=0, _methods=None):
        self.name = _name
        self.time = _time
        self.tests = _tests
        self.errors = _errors
        self.skipped = _skipped
        self.failures = _failures
        self.methods = _methods


# Struct of unit test results for single full test
class UnitTestResultsPerCycle(object):
    def __init__(self, _tests=0, _errors=0, _failures=0, _skipped=0, _success_rate=0.0, _time=0.0, _classes=None):
        self.tests = _tests
        self.errors = _errors
        self.failures = _failures
        self.skipped = _skipped
        self.success_rate = _success_rate
        self.time = _time
        if _classes is None:
            self.classes = []
        else:
            self.classes = classes


# Struct of unit test results for all full test
class AllTestResults(object):
    def __init__(self, _tests=0, _errors=0, _failures=0, _skipped=0, _success_rate=0.0, _time=0.0, _success_times=0,
                 _results=None):
        self.tests = _tests
        self.errors = _errors
        self.failures = _failures
        self.skipped = _skipped
        self.success_rate = _success_rate
        self.time = _time
        self.success_times = _success_times
        if _results is None:
            self.results = []
        else:
            self.results = results


def get_surefire_reports(path):
    """
    Get all surefire reports in path
    :param path: Root directory of maven-surefire-plugin reports
    :return: Absolute filepath of reports files
    """
    reports_paths = []
    if os.path.exists(path):
        for file in os.listdir(path):
            if os.path.splitext(file)[-1] == '.xml':
                reports_paths.append(os.path.join(path, file))
    else:
        print("[ERROR] Could not find directory: " + str(path))
        exit(-1)
    if len(reports_paths) == 0:
        print("[ERROR] Could not find any surefire report file")
        exit(-1)
    return reports_paths


def cmp_class(x, y):
    """
    Compare classes by error count, failure count, timecost
    :param x: items1: {'packageName.className': {TestClass}}
    :param y: items2: {'packageName.className': {TestClass}}
    :return: -1, 0, 1
    """
    x_error_counter = 0
    y_error_counter = 0
    x_failure_counter = 0
    y_failure_counter = 0
    x_time = 0.0
    y_time = 0.0
    for i in range(len(x[1])):
        x_error_counter += x[1][i].errors
        x_failure_counter += x[1][i].failures
        x_time += x[1][i].time
    x_time = x_time / len(x[1])
    for j in range(len(y[1])):
        y_error_counter += y[1][j].errors
        y_failure_counter += y[1][j].failures
        y_time += y[1][j].time
    y_time = y_time / len(y[1])
    if x_error_counter > y_error_counter:
        return -1
    elif x_error_counter < y_error_counter:
        return 1
    elif x_failure_counter > y_failure_counter:
        return -1
    elif x_failure_counter < y_failure_counter:
        return 1
    else:
        return 1 if x_time < y_time else -1 if x_time > y_time else 0


def cmp_method(x, y):
    """
    Compare methods by error count, timecost
    :param x: items1: {'packageName.className.methodName': {TestMethod}}
    :param y: items2: {'packageName.className.methodName': {TestMethod}}
    :return: -1, 0, 1
    """
    x_error_counter = 0
    y_error_counter = 0
    x_time = 0.0
    y_time = 0.0
    for i in range(len(x[1])):
        x_error_counter += 1 if x[1][i].error else 0
        x_time += x[1][i].time
    x_time = x_time / len(x[1])
    for j in range(len(y[1])):
        y_error_counter += 1 if y[1][j].error else 0
        y_time += y[1][j].time
    y_time = y_time / len(y[1])
    if x_error_counter > y_error_counter:
        return -1
    elif x_error_counter < y_error_counter:
        return 1
    else:
        return 1 if x_time < y_time else -1 if x_time > y_time else 0


def get_owner_by_domain(_dir, _type, _domain):
    """
    Get git owner by domain ("package.class" for test class or "package.class.method" for test method).
    :param _dir: Root directory of maven project
    :param _type: "class" or "method"
    :param _domain: Domain name of test method or class
    :return: git owner's name
    """
    dir_prefix = os.path.join(_dir, 'app')
    dir_suffix = ''
    if _type == "method":
        dir_suffix = os.path.join("src/test/java", os.path.dirname(_domain.replace('.', '/')) + ".java")
    elif _type == "class":
        dir_suffix = os.path.join("src/test/java", _domain.replace('.', '/') + ".java")
    else:
        print("[ERROR] Unsupported type of testcase")
        exit(-1)
    dir_find = glob.glob(dir_prefix + "/*/" + dir_suffix)
    if len(dir_find) == 0:
        owner = '获取失败'
    elif len(dir_find) > 1:
        owner = '类名冲突'
    else:
        owner = get_owner_by_file(dir_find[0], _type, _domain.split('.')[-1])
        # Convert deprecated git user to new one (specially for ODC projects)
        name_mapper = {"mogao.zj": "漠高", "yh263208": "山露", "wenniu.ly": "闻牛"}
        if owner in name_mapper:
            owner = name_mapper.get(owner)
    return owner


def get_owner_by_file(_file, _type, _keywords):
    """
    Get git owner by java code file and keywords
    :param _file: Java code file
    :param _type: "class" or "method"
    :param _keywords: name of test method or class
    :return: git owner's name of input test method or class
    """
    if _type == "method":
        declaration = 'public void ' + _keywords + '()'
    elif _type == "class":
        declaration = 'public class ' + _keywords
    else:
        return "获取失败"
    query_row_cmd = "grep -n " + '"' + declaration + '"' + " " + _file + " | awk -F: '{print $1}'"
    cmd_row_res = os.popen(query_row_cmd)
    try:
        row_number = int(cmd_row_res.read().replace("\n", ""))
    except BaseException:
        return "获取失败"
    else:
        query_git_owner_cmd = "git blame -p -L " + str(row_number) + "," + str(row_number) + " " + _file
        cmd_owner_res = os.popen(query_git_owner_cmd)
        try:
            owner = cmd_owner_res.readlines()[1].replace("\n", "").split(" ")[-1]
        except BaseException:
            return "获取失败"
        else:
            return owner
        finally:
            cmd_owner_res.close()
    finally:
        cmd_row_res.close()


def str2boolean(parameter):
    """
    Convert input parameter in CLI from string to boolean
    :param parameter: String
    :return: True, False
    """
    if parameter.lower() in ('yes', 'true', 't', 'y', '1'):
        return True
    elif parameter.lower() in ('no', 'false', 'f', 'n', '0'):
        return False
    else:
        print("[ERROR] Unsupported value encountered: " + str(parameter))
        exit(-1)


def str2directory(parameter):
    """
    Convert input parameter in CLI from string to absolute directory
    (If the directory does not exist, it will be created automatically)
    :param parameter: String: any directory
    :return: absolute directory
    """
    if parameter[0] == '~':
        path = os.path.expanduser(parameter)
    else:
        path = os.path.abspath(parameter)
    if not os.path.exists(path):
        os.makedirs(path)
    return path


if __name__ == "__main__":
    # # Get input config parameters in CLI
    parser = argparse.ArgumentParser(
        prog='python unit_test_evaluation_tool.py',
        description="Tool for evaluating unit test performance\r\n(Requirement: pandas, openpyxl)",
        formatter_class=lambda prog: argparse.RawTextHelpFormatter(prog, max_help_position=100)
    )
    parser.add_argument(
        '-r', '--repeat-times',
        metavar='',
        dest='repeat_times',
        help="number of times to repeat unit test, default=1",
        type=int,
        default=1
    )
    parser.add_argument(
        '-n', '--number-of-shown',
        metavar='',
        dest='number_of_shown',
        help="number of top high priority testcase shown below, default=100",
        type=int,
        default=100
    )
    parser.add_argument(
        '-t', '--testcase',
        metavar='',
        dest='maven_testcase',
        help="specify testcase, the same usages as 'mvn -Dtest=xxx', default=all, meaning run all testcase",
        type=str,
        default='all'
    )
    parser.add_argument(
        '-i', '--ignore-failure',
        metavar='',
        dest='ignore_failure',
        help="determine whether to continue when some testcase fell fail, default=true",
        type=str2boolean,
        default=True
    )
    parser.add_argument(
        '-s', '--skip-test',
        metavar='',
        dest='skip_test',
        help="determine whether to skip executing 'mvn test', default=false",
        type=str2boolean,
        default=False
    )
    parser.add_argument(
        '-p', '--project-directory',
        metavar='',
        dest='maven_project_directory',
        help="root directory of maven project, default=..",
        type=str2directory,
        default='..'
    )
    parser.add_argument(
        '-d', '--reports-directory',
        metavar='',
        dest='reports_directory',
        help="root directory where saving or reading surefire reports, default=~/unit-test-results",
        type=str2directory,
        default='~/unit-test-results'
    )
    parser.add_argument(
        '-o', '--results-directory',
        metavar='',
        dest='output_results_directory',
        help="root directory where saving evaluation results, default=~/unit-test-results",
        type=str2directory,
        default='~/unit-test-results'
    )
    parser.add_argument(
        '-e', '--results-type',
        metavar='',
        dest='output_results_type',
        help="specify the output results file type, support 'csv' and 'xlsx', default=csv",
        type=str,
        default='csv'
    )
    args = parser.parse_args()
    # repeat times
    repeat_times = args.repeat_times
    if repeat_times <= 0:
        print("[ERROR] Unsupported value encountered: " + str(repeat_times))
        exit(-1)
    # number of shown methods
    number_of_shown = args.number_of_shown
    if number_of_shown <= 0:
        print("[ERROR] Unsupported value encountered: " + str(number_of_shown))
        exit(-1)
    # specify testcase
    maven_testcase = args.maven_testcase
    # test failure ignore
    ignore_failure = args.ignore_failure
    # skip test flag
    skip_test = args.skip_test
    # root directory of maven project
    dir_maven_project = args.maven_project_directory
    # root directory of surefire reports
    dir_read_reports = args.reports_directory
    # root directory for save surefire reports
    dir_save_reports = args.reports_directory
    # root directory for save evaluating results
    dir_save_results = args.output_results_directory
    # output file type
    output_results_type = args.output_results_type
    if output_results_type not in ('csv', 'xlsx'):
        print("[ERROR] Unsupported value encountered: " + output_results_type)
        exit(-1)

    # # execute mvn test
    if not skip_test:
        for i in range(repeat_times):
            cmd = "cd " + dir_maven_project \
                  + " && mvn -DsurefireReportsDirectory=" \
                  + os.path.join(dir_save_reports, "surefire-reports-" + str(i + 1))
            if ignore_failure:
                cmd = cmd + " -Dmaven.test.failure.ignore=true"
            if maven_testcase != 'all':
                cmd = cmd + " -Dtest=" + maven_testcase
                cmd = cmd + " -DfailIfNoTests=false"
            cmd = cmd + " test"
            os.system(cmd)
            print("-" * 50)
            print("[INFO] The" + str(i + 1) + " time unit test complete!")
            print("-" * 50)

    # # analysis unit test results
    print("-" * 100)
    print("[INFO] Start analysis ...")

    # save all results
    all_results = AllTestResults()

    # define unit test results for single method
    # key: 'packageName.className.methodName'
    # value: {TestMethod}
    methods_ut_results = {}

    # define unit test results for single test class
    # key: 'packageName.className'
    # value: {TestClass}
    classes_ut_results = {}

    for i in range(repeat_times):
        reports = get_surefire_reports(os.path.join(dir_read_reports, 'surefire-reports-' + str(i + 1)) + '/')
        results = UnitTestResultsPerCycle()

        for report in reports:
            # analysis for single .xml report
            tree = elementTree.parse(report)
            root = tree.getroot()
            # single test class
            test_class = TestClass(
                root.attrib['name'],
                float(root.attrib['time'].replace(",", "")),
                int(root.attrib['tests'].replace(",", "")),
                int(root.attrib['errors'].replace(",", "")),
                int(root.attrib['skipped'].replace(",", "")),
                int(root.attrib['failures'].replace(",", "")),
                None
            )

            # methods in test class
            test_methods = []
            for testcase in root.iter('testcase'):
                test_method = TestMethod(
                    testcase.attrib['name'],
                    testcase.attrib['classname'],
                    float(testcase.attrib['time'].replace(",", "")),
                    False
                )
                for error in testcase.iter('error'):
                    test_method.error = True
                test_methods.append(test_method)
                # add to method dict
                if test_method.classname + '.' + test_method.name in methods_ut_results:
                    methods_ut_results[test_method.classname + '.' + test_method.name].append(test_method)
                else:
                    methods_ut_results[test_method.classname + '.' + test_method.name] = [test_method]

            test_class.methods = test_methods
            # add to class dict
            if test_class.name in classes_ut_results:
                classes_ut_results[test_class.name].append(test_class)
            else:
                classes_ut_results[test_class.name] = [test_class]

            # add result
            results.tests += test_class.tests
            results.errors += test_class.errors
            results.failures += test_class.failures
            results.skipped += test_class.skipped
            results.time += test_class.time
            results.classes.append(test_class)

        # all surefire reports are done.
        if results.tests > 0:
            results.success_rate = float(results.tests - results.failures - results.errors) / float(results.tests)
        print("The " + str(i + 1) + " times unit test complete!")
        print("Tests: " + str(results.tests) + ", Failures: " + str(results.failures)
              + ", Errors: " + str(results.errors) + ", Skipped: " + str(results.skipped)
              + ", Success Rate: %.2f%%" % (results.success_rate * 100)
              + ", Time elapsed: %.3f sec" % results.time)

        all_results.tests += results.tests
        all_results.errors += results.errors
        all_results.failures += results.failures
        all_results.skipped += results.skipped
        all_results.time += results.time
        if results.errors == 0 and results.failures == 0:
            all_results.success_times += 1
        all_results.results.append(results)

    # get final average results
    if all_results.tests > 0:
        all_results.success_rate = float(all_results.tests - all_results.failures - all_results.errors) / float(
            all_results.tests)
    print("<" + "-" * 20 + " Summary " + "-" * 20 + ">")
    print("Test times: " + str(repeat_times) + ", Success times: " + str(
        all_results.success_times) + ", Failures times: " + str(repeat_times - all_results.success_times))
    print("Tests: " + str(all_results.tests))
    print("Failures: " + str(all_results.failures))
    print("Errors: " + str(all_results.errors))
    print("Skipped: " + str(all_results.skipped))
    print("Success rate: %.5f%%" % (all_results.success_rate * 100))
    print("Time elapsed: %.3f sec" % all_results.time)

    # Sort by error, failure, time-consuming descending
    sorted_methods_ut_results = sorted(methods_ut_results.items(), key=cmp_to_key(cmp_method))
    sorted_class_ut_results = sorted(classes_ut_results.items(), key=cmp_to_key(cmp_class))

    # print methods with error or long time elapsed
    methods = []
    timecost = []
    error_times = []
    methods_num = len(sorted_methods_ut_results)
    for i in range(methods_num if methods_num < number_of_shown else number_of_shown):
        methods.append(sorted_methods_ut_results[i][0])
        method_info = sorted_methods_ut_results[i][1]
        time = 0.0
        counter = 0
        error_counter = 0
        is_error = False
        for u in method_info:
            if u.error:
                error_counter += 1
            time += u.time
            counter += 1
        timecost.append("%.3f" % float(time / counter))
        error_times.append(error_counter)
    owners = []
    for method in methods:
        owners.append(get_owner_by_domain(dir_maven_project, "method", method))
    df_methods = pd.DataFrame({
        'Method': methods,
        'Error times': error_times,
        'Average timecost (sec)': timecost,
        'Owner': owners
    })
    df_methods.index = range(1, methods_num + 1 if methods_num < number_of_shown else number_of_shown + 1)
    pd.set_option('display.max_rows', None)
    pd.set_option('display.max_columns', None)
    pd.set_option('max_colwidth', 200)
    pd.set_option('display.width', 1000)
    print("<" + "-" * 80 + " unit test result of methods " + "-" * 80 + ">")
    print(df_methods)

    # print classes with error, failures or long time elapsed
    classes = []
    timecost = []
    error_times = []
    failure_times = []
    classes_num = len(sorted_class_ut_results)
    for i in range(classes_num if classes_num < number_of_shown else number_of_shown):
        classes.append(sorted_class_ut_results[i][0])
        class_info = sorted_class_ut_results[i][1]
        time = 0.0
        counter = 0
        error_counter = 0
        failure_counter = 0
        is_error = False
        for c in class_info:
            error_counter += c.errors
            failure_counter += c.failures
            time += c.time
            counter += 1
        timecost.append("%.3f" % float(time / counter))
        error_times.append(error_counter)
        failure_times.append(failure_counter)
    owners = []
    for test_class in classes:
        owners.append(get_owner_by_domain(dir_maven_project, "class", test_class))
    df_classes = pd.DataFrame({
        "Class": classes,
        'Error times': error_times,
        'Failure times': failure_times,
        'Average timecost (sec)': timecost,
        'Owner': owners
    })
    df_classes.index = range(1, classes_num + 1 if classes_num < number_of_shown else number_of_shown + 1)
    pd.set_option('display.max_rows', None)
    pd.set_option('display.max_columns', None)
    pd.set_option('max_colwidth', 200)
    pd.set_option('display.width', 1000)
    print("")
    print("<" + "-" * 80 + "unit test result of classes" + "-" * 80 + ">")
    print(df_classes)
    print("")

    # # Save results (Existing files will be overwritten!)
    if output_results_type == 'csv':
        df_methods.to_csv(os.path.join(dir_save_results, "test_methods_results.csv"), encoding='utf-8-sig',
                          index_label="序号")
        df_classes.to_csv(os.path.join(dir_save_results, "test_classes_results.csv"), encoding='utf-8-sig',
                          index_label="序号")
    else:
        df_methods.to_excel(os.path.join(dir_save_results, "test_results.xlsx"), sheet_name='methods',
                            encoding='utf-8-sig', index_label="序号")
        df_classes.to_excel(os.path.join(dir_save_results, "test_results.xlsx"), sheet_name='classes',
                            encoding='utf-8-sig', index_label="序号")
    print("[INFO] The evaluation results have been saved in " + str(dir_save_results))
