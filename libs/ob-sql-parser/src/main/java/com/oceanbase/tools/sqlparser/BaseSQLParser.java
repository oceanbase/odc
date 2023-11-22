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
package com.oceanbase.tools.sqlparser;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.sqlparser.adapter.StatementFactory;
import com.oceanbase.tools.sqlparser.statement.Statement;

/**
 * {@link BaseSQLParser}
 *
 * @author yh263208
 * @date 2022-12-13 23:12
 * @since ODC_release_4.1.0
 */
public abstract class BaseSQLParser<T extends Lexer, V extends Parser> implements SQLParser {

    protected abstract ParseTree doParse(V parser);

    protected abstract T getLexer(Reader statementReader) throws IOException;

    protected abstract V getParser(TokenStream tokens);

    public ParseTree buildAst(Reader statementReader) {
        if (statementReader == null) {
            throw new IllegalArgumentException("Input reader is null");
        }
        T lexer;
        try {
            lexer = getLexer(statementReader);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        if (lexer == null) {
            throw new IllegalArgumentException("Lexer can not be null");
        }
        lexer.removeErrorListeners();
        lexer.addErrorListener(new FastFailErrorListener());
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        V parser = getParser(tokens);
        if (parser == null) {
            throw new IllegalArgumentException("Parser can not be null");
        }
        parser.removeErrorListeners();
        parser.addErrorListener(new FastFailErrorListener());
        parser.setErrorHandler(new FastFailErrorStrategy());
        return doParse(parser);
    }

    @Override
    public Statement parse(Reader statementReader) throws SyntaxErrorException {
        return buildStatement(buildAst(statementReader));
    }

    public Statement buildStatement(ParseTree root) {
        String basePkg = getStatementFactoryBasePackage();
        if (StringUtils.isEmpty(basePkg)) {
            throw new IllegalStateException("Base package dir is empty, " + basePkg);
        }
        basePkg = basePkg.replace('.', '/');
        URL url = BaseSQLParser.class.getClassLoader().getResource(basePkg);
        if (url == null) {
            throw new IllegalStateException("Can not load package for path, " + basePkg);
        }
        URI uri;
        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        String scheme = uri.getScheme();
        Set<Class<? extends StatementFactory<? extends Statement>>> classes;
        if ("file".equals(scheme)) {
            classes = loadClassesFromLocal(uri);
        } else if ("jar".equals(scheme)) {
            classes = loadClassesFromJar(url);
        } else {
            throw new IllegalArgumentException("UnSupported scheme " + scheme);
        }
        Optional<Class<? extends StatementFactory<? extends Statement>>> optional = classes.stream().filter(clazz -> {
            return getSupportedParameters(clazz).stream().anyMatch(items -> {
                if (CollectionUtils.isEmpty(items)) {
                    return false;
                } else if (items.size() != 1) {
                    return false;
                }
                return items.get(0).equals(root.getClass());
            });
        }).findFirst();
        if (!optional.isPresent()) {
            return null;
        }
        Class<? extends StatementFactory<? extends Statement>> clazz = optional.get();
        try {
            Constructor<? extends StatementFactory<? extends Statement>> constructor =
                    clazz.getConstructor(root.getClass());
            return constructor.newInstance(root).generate();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            return null;
        }
    }

    private Set<Class<? extends StatementFactory<? extends Statement>>> loadClassesFromLocal(URI uri) {
        File[] files = new File(uri).listFiles();
        if (files == null) {
            return new HashSet<>();
        }
        String basePkg = getStatementFactoryBasePackage();
        return Arrays.stream(files).filter(file -> file.getName().endsWith(".class")).map(file -> {
            try {
                String classPath = basePkg + "." + file.getName();
                return Class.forName(classPath.substring(0, classPath.indexOf(".class")));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }).filter(StatementFactory.class::isAssignableFrom)
                .map(c -> (Class<? extends StatementFactory<? extends Statement>>) c).collect(Collectors.toSet());
    }

    private Set<Class<? extends StatementFactory<? extends Statement>>> loadClassesFromJar(URL url) {
        JarFile jarFile;
        String entryName;
        try {
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            entryName = connection.getEntryName() == null ? "" : connection.getEntryName();
            if (entryName.contains("!")) {
                entryName = entryName.replace("!", "");
            }
            jarFile = connection.getJarFile();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Enumeration<? extends ZipEntry> enumeration = jarFile.entries();
        List<String> targets = new LinkedList<>();
        while (enumeration.hasMoreElements()) {
            ZipEntry zipEntry = enumeration.nextElement();
            if (zipEntry.getName().startsWith(entryName) && !zipEntry.isDirectory()) {
                targets.add(zipEntry.getName().substring(entryName.length() + 1));
            }
        }
        String basePkg = getStatementFactoryBasePackage();
        return targets.stream().map(s -> {
            try {
                String classPath = basePkg + "." + s;
                return Class.forName(classPath.substring(0, classPath.indexOf(".class")));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }).filter(StatementFactory.class::isAssignableFrom)
                .map(c -> (Class<? extends StatementFactory<? extends Statement>>) c).collect(Collectors.toSet());
    }

    private List<List<Class<?>>> getSupportedParameters(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length == 0) {
            return Collections.emptyList();
        }
        List<List<Class<?>>> types = new ArrayList<>();
        for (Constructor<?> constructor : constructors) {
            types.add(Arrays.stream(constructor.getParameters()).map(Parameter::getType).collect(Collectors.toList()));
        }
        return types;
    }

    protected abstract String getStatementFactoryBasePackage();

}
