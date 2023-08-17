/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.tools.dbbrowser.env;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MySQLClientArgsParser {
    /**
     * 密码参数允许不设置参数值，需特殊处理
     */
    private static final String PASSWORD_SHORT_OPTION = "-p";
    private static final String PASSWORD_LONG_OPTION = "--password";

    private static final Options options = new Options();
    private static final CommandLineParser parser = new DefaultParser();

    static {
        options.addOption("h", "host", true, "Connect to host.");
        options.addOption("P", "port", true, "Port number to use for connection.");
        options.addOption("u", "user", true, "User for login if not current user.");
        options.addOption("p", "password", true, "Password to use when connecting to server.");
        options.addOption("D", "database", true, "Database to use.");
        options.addOption("A", "no-auto-rehash", false, "No automatic rehashing.");
        options.addOption(null, "default-character-set", true, "Set the default character set.");
        options.addOption(null, "delimiter", true, "Delimiter to be used.");
        options.addOption(null, "connect-timeout", true, "Number of seconds before connection timeout.");
        options.addOption(null, "max-allowed-packet", true,
                "The maximum packet length to send to or receive from server.");
        options.addOption(null, "net-buffer-length", true, "The buffer size for TCP/IP and socket communication.");

        options.addOption("B", "batch", false, "Don't use history file.");
        options.addOption(null, "binary-as-hex", false, "Print binary data as hex");
        options.addOption("c", "comments", false, "Preserve comments.");
        options.addOption("C", "compress", false, "Use compression in server/client protocol.");
        options.addOption("E", "vertical", false, "Print the output of a query (rows) vertically.");
        options.addOption("f", "force", false, "Continue even if we get an SQL error.");
        options.addOption("i", "ignore-spaces", false, "Ignore space after function names.");
        options.addOption("b", "no-beep", false, "Turn off beep on error.");
        options.addOption(null, "line-numbers", false, "Write line numbers for errors.");
        options.addOption("L", "skip-line-numbers", false, "Don't write line number for errors.");
        options.addOption("n", "unbuffered", false, "Flush buffer after each query.");
        options.addOption("w", "wait", false, "Wait and retry if connection is down.");
        options.addOption("v", "verbose", false, "Output version information and exit.");
        options.addOption(null, "column-names", false, "Write column names in results.");
        options.addOption("N", "skip-column-names", false, "Don't write column names in results.");
    }

    public static ConnectionParseResult parse(String commandLineStr) {
        Validate.notEmpty(commandLineStr, "commandLineStr");
        String[] arguments = commandLineStr.trim().split("\\s+");
        arguments = removePasswordIfNoValue(arguments);

        CommandLine commandLine;
        try {
            commandLine = parser.parse(options, arguments);
        } catch (ParseException e) {
            log.warn("parse command line failed, reason={}", ExceptionUtils.getRootCauseMessage(e));
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        ConnectionParseResult parseResult = new ConnectionParseResult();
        if (commandLine.hasOption("host")) {
            parseResult.setHost(getOptionValue(commandLine, "host"));
        }
        if (commandLine.hasOption("port")) {
            String portStr = getOptionValue(commandLine, "port");
            parseResult.setPort(StringUtils.isBlank(portStr) ? null : Integer.parseInt(portStr));
        }
        if (commandLine.hasOption("user")) {
            String user = getOptionValue(commandLine, "user");
            String[] userAndCluster = user.split("#");
            if (userAndCluster.length == 0 || userAndCluster.length > 2) {
                log.warn("invalid user, user={}", user);
                throw new IllegalArgumentException(String.format("invalid user '%s'", user));
            }
            if (userAndCluster.length == 2) {
                parseResult.setCluster(userAndCluster[1]);
            }
            String[] userAndTenant = userAndCluster[0].split("@");
            if (userAndTenant.length == 0 || userAndTenant.length > 2) {
                log.warn("invalid user, user={}", user);
                throw new IllegalArgumentException(String.format("invalid user '%s'", user));
            }
            if (userAndTenant.length == 2) {
                parseResult.setTenant(userAndTenant[1]);
            }
            parseResult.setUsername(userAndTenant[0]);
        }
        if (commandLine.hasOption("password")) {
            parseResult.setPassword(getOptionValue(commandLine, "password"));
        }
        if (commandLine.hasOption("database")) {
            parseResult.setDefaultDBName(getOptionValue(commandLine, "database"));
        }
        return parseResult;
    }

    private static String[] removePasswordIfNoValue(String[] arguments) {
        List<String> filledArguments = new ArrayList<>();
        int size = arguments.length;
        for (int i = 0; i < size; i++) {
            String argument = arguments[i];
            String nextArgument = i == (size - 1) ? null : arguments[i + 1];
            if (StringUtils.startsWith(nextArgument, "-")) {
                nextArgument = null;
            }
            boolean currentArgumentIsPasswordWithoutValue = StringUtils.equals(argument, PASSWORD_SHORT_OPTION)
                    || StringUtils.equals(argument, PASSWORD_LONG_OPTION);
            if (currentArgumentIsPasswordWithoutValue && nextArgument == null) {
                continue;
            }
            filledArguments.add(argument);
        }
        return filledArguments.toArray(new String[0]);
    }

    private static String getOptionValue(CommandLine commandLine, String opt) {
        String origin = commandLine.getOptionValue(opt);
        return unwrapValue(origin);
    }

    private static String unwrapValue(String value) {
        String unwrap = StringUtils.unwrap(value, '\"');
        if (StringUtils.equals(value, unwrap)) {
            unwrap = StringUtils.unwrap(value, '\'');
        }
        return unwrap;
    }

}
