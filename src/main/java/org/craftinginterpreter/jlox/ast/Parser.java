package org.craftinginterpreter.jlox.ast;

import org.craftinginterpreter.jlox.Lox;
import org.craftinginterpreter.jlox.ast.Expr;
import org.craftinginterpreter.jlox.ast.Stmt;
import org.craftinginterpreter.jlox.scanner.Token;
import org.craftinginterpreter.jlox.scanner.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {

    private static class ParseError extends RuntimeException { }
    private final List<Token> tokens;
    private int curr = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // program → declarations* EOF
    public List<Stmt> parse() {
        try {
            List<Stmt> declarations = new ArrayList<>();
            while (!isAtEnd()) {
                declarations.add(declaration());
            }
            return declarations;
        } catch (ParseError parseError) {
            return null;
        }
    }

    // declarations → varDeclaration | statement | functionDeclaration
    private Stmt declaration() {
        try {
            if (match(TokenType.CLASS)) {
                return classDeclaration();
            }
            if (match(TokenType.FUN)) {
                return functionDeclaration("function");
            }
            if (match(TokenType.VAR)) {
                return varDeclaration();
            }
            return statement();
        } catch (ParseError error) {
            synchronize();
        }
        return null;
    }

    private Stmt classDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect class name.");

        Expr.Var superclass = null;
        if (match(TokenType.LESS)) {
            consume(TokenType.IDENTIFIER, "Expect superclass name");
            superclass = new Expr.Var(previous());
        }

        consume(TokenType.LEFT_BRACE, "Expect '{' brefore after class name.");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            methods.add(functionDeclaration("function"));
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, superclass, methods);
    }

    private Stmt.Function functionDeclaration(String kind) {
        Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");

        consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    // varDeclaration -> VAR name (= ininitializer)?;
    private Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name");

        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    // statement -> PRINT printStmt | { block } | expressionStmt | ifStmt | whileStmt | forStmt
    private Stmt statement() {
        if (match(TokenType.PRINT)) {
            return printStmt();
        }
        if (match(TokenType.LEFT_BRACE)) {
            return new Stmt.Block(block());
        }
        if (match(TokenType.IF)) {
            return ifStmt();
        }
        if (match(TokenType.WHILE)) {
            return whileStmt();
        }
        if (match(TokenType.FOR)) {
            return forStmt();
        }
        if (match(TokenType.RETURN)) {
            return returnStmt();
        }
        return expressionStmt();
    }

    private Stmt returnStmt() {
        Token keyword = previous();

        Expr value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    // desugar the for loop to while...
    private Stmt forStmt() {

        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;

        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStmt();
        }

        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition");

        Expr incremental = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            incremental = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for claused");

        Stmt body = statement();

        if (incremental != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(incremental)));
        }

        if (condition ==  null) {
            condition = new Expr.Literal(true);
        }
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    // whileStmt
    private Stmt whileStmt() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after while condition");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    // ifStmt-> if(expression) stmt else stmt?
    private Stmt ifStmt() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    // block -> declarations*
    private List<Stmt> block() {
        List<Stmt> stmts = new ArrayList<>();
        while (!isAtEnd() && !check(TokenType.RIGHT_BRACE)) {
            stmts.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block");
        return stmts;
    }

    // printStmt -> expression;
    private Stmt printStmt() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    //expressionStmt -> expression;
    private Stmt expressionStmt() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Except ';' after expression");
        return new Stmt.Expression(value);
    }

    // expression → assignment ;
    private Expr expression() {
        return assignment();
    }


    // assignment → IDENTIFIER "=" assignment | logic_or;
    private Expr assignment() {
        Expr expr = or();

        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Var) {
                Token name = ((Expr.Var)expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
            }
            error(equals, "Invalid assignment target");
        }
        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    // equality → comparison ( ( "!=" | "==" ) comparison )* ;
    private Expr equality() {
        Expr expr = comparison();

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    private Expr comparison() {
        Expr expr = term();

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // term → factor ( ( "-" | "+" ) factor )* ;
    private Expr term() {
        Expr expr = factor();

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // factor → unary ( ( "/" | "*" ) unary )* ;
    private Expr factor() {
        Expr expr = unary();

        while (match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;

    }

    // unary → ( "!" | "-" ) unary | primary
    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
//        return primary();
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(TokenType.DOT)) {
                Token name = consume(TokenType.IDENTIFIER, "Expected property name after '.'");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while(match(TokenType.COMMA));
        }

        Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments");

        return new Expr.Call(callee, paren, arguments);
    }

    //primary → NUMBER | STRING | "true" | "false" | "nil"
    //        | "(" expression ")" ;
    private Expr primary() {
        if (match(TokenType.FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TokenType.TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(TokenType.NIL)) {
            return new Expr.Literal(null);
        }

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().getLiteral());
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Var(previous());
        }

        if (match(TokenType.THIS)) {
            return new Expr.This(previous());
        }

        if (match(TokenType.SUPER)) {
            Token keyword = previous();
            consume(TokenType.DOT, "Expect '.' after 'super'");
            Token method = consume(TokenType.IDENTIFIER, "Expect super class method name.");
            return new Expr.Super(keyword, method);
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Except ')' after expression");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Except expression.");
    }

    private Token consume(TokenType tokenType, String message) {
        if (check(tokenType)) return advance();
        // enter panic mode, reset the state
        throw error(peek(), message);
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            // discards tokens until it thinks it found a statement boundary.
            if (previous().getTokenType() == TokenType.SEMICOLON) {
                return ;
            }

            switch (peek().getTokenType()) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return ;
            }

            advance();
        }
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private boolean match(TokenType ... tokenTypes) {
        for (TokenType tokenType: tokenTypes) {
            if (check(tokenType)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType tokenType) {
        if (isAtEnd()) {
            return false;
        }
        return peek().getTokenType() == tokenType;
    }

    private Token advance() {
        if (!isAtEnd()) {
            curr++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().getTokenType() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(curr);
    }

    private Token previous() {
        return tokens.get(curr - 1);
    }
}
