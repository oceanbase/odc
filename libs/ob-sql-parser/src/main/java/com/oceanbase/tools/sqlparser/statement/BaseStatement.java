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
package com.oceanbase.tools.sqlparser.statement;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * {@link BaseStatement}
 *
 * @author yh263208
 * @date 2022-11-24 21:46
 * @since ODC_release_4.1.0
 * @see Statement
 */
public abstract class BaseStatement implements Statement {

    private final ParserRuleContext ruleNode;
    private final TerminalNode terminalNode;

    protected BaseStatement() {
        this(null, null);
    }

    protected BaseStatement(TerminalNode terminalNode) {
        this(null, terminalNode);
    }

    protected BaseStatement(ParserRuleContext ruleNode) {
        this(ruleNode, null);
    }

    protected BaseStatement(ParserRuleContext ruleNode, TerminalNode terminalNode) {
        this.ruleNode = ruleNode;
        this.terminalNode = terminalNode;
    }

    @Override
    public String getText() {
        if (this.ruleNode != null) {
            Token start = this.ruleNode.getStart();
            Token offset = this.ruleNode.getStop();
            if (start == null || offset == null) {
                return null;
            }
            CharStream charStream = start.getTokenSource().getInputStream();
            if (charStream == null) {
                return null;
            }
            return charStream.getText(Interval.of(start.getStartIndex(), offset.getStopIndex()));
        } else if (this.terminalNode != null) {
            return this.terminalNode.getText();
        }
        return null;
    }

    @Override
    public int getStart() {
        if (this.ruleNode != null) {
            return this.ruleNode.getStart().getStartIndex();
        } else if (this.terminalNode != null) {
            return this.terminalNode.getSymbol().getStartIndex();
        }
        return -1;
    }

    @Override
    public int getStop() {
        if (this.ruleNode != null) {
            return this.ruleNode.getStop().getStopIndex();
        } else if (this.terminalNode != null) {
            return this.terminalNode.getSymbol().getStopIndex();
        }
        return -1;
    }

    @Override
    public int getLine() {
        if (this.ruleNode != null) {
            Token offset = this.ruleNode.getStart();
            if (offset == null) {
                return -1;
            }
            return offset.getLine();
        } else if (this.terminalNode != null) {
            return this.terminalNode.getSymbol().getLine();
        }
        return -1;
    }

    @Override
    public int getCharPositionInLine() {
        if (this.ruleNode != null) {
            Token offset = this.ruleNode.getStart();
            if (offset == null) {
                return -1;
            }
            return offset.getCharPositionInLine();
        } else if (this.terminalNode != null) {
            return this.terminalNode.getSymbol().getCharPositionInLine();
        }
        return -1;
    }

}
