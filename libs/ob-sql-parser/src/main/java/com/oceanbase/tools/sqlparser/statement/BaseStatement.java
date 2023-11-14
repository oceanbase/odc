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
    private final ParserRuleContext beginRule;
    private final TerminalNode beginNode;
    private final ParserRuleContext endRule;
    private final TerminalNode endNode;

    protected BaseStatement() {
        this(null, null, null, null, null, null);
    }

    protected BaseStatement(TerminalNode terminalNode) {
        this(null, terminalNode, null, null, null, null);
    }

    protected BaseStatement(ParserRuleContext ruleNode) {
        this(ruleNode, null, null, null, null, null);
    }

    protected BaseStatement(ParserRuleContext beginRule, ParserRuleContext endRule) {
        this(null, null, beginRule, endRule, null, null);
    }

    protected BaseStatement(ParserRuleContext beginRule, TerminalNode endNode) {
        this(null, null, beginRule, null, null, endNode);
    }

    protected BaseStatement(TerminalNode beginNode, TerminalNode endNode) {
        this(null, null, null, null, beginNode, endNode);
    }

    protected BaseStatement(TerminalNode beginNode, ParserRuleContext endRule) {
        this(null, null, null, endRule, beginNode, null);
    }

    private BaseStatement(ParserRuleContext ruleNode, TerminalNode terminalNode,
            ParserRuleContext beginRule, ParserRuleContext endRule,
            TerminalNode beginNode, TerminalNode endNode) {
        this.ruleNode = ruleNode;
        this.terminalNode = terminalNode;
        this.endNode = endNode;
        this.endRule = endRule;
        this.beginNode = beginNode;
        this.beginRule = beginRule;
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
        CharStream charStream = null;
        if (this.beginNode != null) {
            charStream = this.beginNode.getSymbol().getTokenSource().getInputStream();
        } else if (this.beginRule != null) {
            Token start = this.beginRule.getStart();
            if (start != null) {
                charStream = start.getTokenSource().getInputStream();
            }
        }
        int startIndex = getStart();
        int endIndex = getStop();
        if (startIndex == -1 || endIndex == -1 || charStream == null) {
            return null;
        }
        return charStream.getText(Interval.of(startIndex, endIndex));
    }

    @Override
    public int getStart() {
        if (this.ruleNode != null) {
            return this.ruleNode.getStart().getStartIndex();
        } else if (this.terminalNode != null) {
            return this.terminalNode.getSymbol().getStartIndex();
        } else if (this.beginNode != null) {
            return this.beginNode.getSymbol().getStartIndex();
        } else if (this.beginRule != null) {
            Token start = this.beginRule.getStart();
            if (start != null) {
                return start.getStartIndex();
            }
        }
        return -1;
    }

    @Override
    public int getStop() {
        if (this.ruleNode != null) {
            return this.ruleNode.getStop().getStopIndex();
        } else if (this.terminalNode != null) {
            return this.terminalNode.getSymbol().getStopIndex();
        } else if (this.endNode != null) {
            return this.endNode.getSymbol().getStopIndex();
        } else if (this.endRule != null) {
            Token end = this.endRule.getStop();
            if (end != null) {
                return end.getStopIndex();
            }
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
        } else if (this.beginNode != null) {
            return this.beginNode.getSymbol().getLine();
        } else if (this.beginRule != null) {
            Token start = this.beginRule.getStart();
            if (start != null) {
                return start.getLine();
            }
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
        } else if (this.beginNode != null) {
            return this.beginNode.getSymbol().getCharPositionInLine();
        } else if (this.beginRule != null) {
            Token start = this.beginRule.getStart();
            if (start != null) {
                return start.getCharPositionInLine();
            }
        }
        return -1;
    }

}
