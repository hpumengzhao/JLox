package org.craftinginterpreter.jlox.scanner;

import org.craftinginterpreter.jlox.Lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start= 0;
    private int curr = 0;
    private int line = 1;

    private static final Map<String, TokenType> keyWords;

    static {
        keyWords = new HashMap<>();
        keyWords.put("and", TokenType.AND);
        keyWords.put("class", TokenType.CLASS);
        keyWords.put("else", TokenType.ELSE);
        keyWords.put("false", TokenType.FALSE);
        keyWords.put("for", TokenType.FOR);
        keyWords.put("fun", TokenType.FUN);
        keyWords.put("if", TokenType.IF);
        keyWords.put("nil", TokenType.NIL);
        keyWords.put("or", TokenType.OR);
        keyWords.put("print", TokenType.PRINT);
        keyWords.put("return", TokenType.RETURN);
        keyWords.put("super", TokenType.SUPER);
        keyWords.put("this", TokenType.THIS);
        keyWords.put("true", TokenType.TRUE);
        keyWords.put("var", TokenType.VAR);
        keyWords.put("while", TokenType.WHILE);
    }

    public Scanner(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = curr;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return curr >= source.length();
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case '-': addToken(TokenType.MINUS); break;
            case '+': addToken(TokenType.PLUS); break;
            case '*': addToken(TokenType.STAR); break;
            case ';': addToken(TokenType.SEMICOLON); break;

            case '!':
                addToken(matchNext('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '=':
                addToken(matchNext('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;
            case '<':
                addToken(matchNext('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '>':
                addToken(matchNext('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;
            case '/':
                if (matchNext('/')) {
                    // consume comments
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(TokenType.SLASH);
                }
                break;
            // ignore the useless chars
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;

            case '"':
                consumeStringLiteral();
                break;
            default:
                if (isDigit(c)) {
                    consumeNumberLiteral();
                } else if (isAlpha(c)) {
                    consumeIdentifier();
                } else {
                    Lox.error(line, "Unexpected character");
                }
        }
    }

    // look ahead
    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(curr);
    }

    private void consumeStringLiteral() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++; // support multi-line string
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
        }

        advance(); //eat "
        String value = source.substring(start + 1, curr - 1);
        addToken(TokenType.STRING, value);
    }

    private void consumeNumberLiteral() {
        while (isDigit(peek())) advance();
        if ('.' == peek() && isDigit(peekNext())){
            advance();
            while (isDigit(peek())) {
                advance();
            }
        }
        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, curr)));
    }

    private void consumeIdentifier() {
        while (isAlphaNumber(peek())) advance();

        String identifier = source.substring(start, curr);

        if (keyWords.containsKey(identifier)) {
            addToken(keyWords.get(identifier));
            return ;
        }

        addToken(TokenType.IDENTIFIER);
    }

    private boolean matchNext(char expected) {
        if (isAtEnd()) {
            return false;
        }

        if (source.charAt(curr) != expected) {
            return false;
        }

        curr++;
        return true;
    }

    private char peekNext() {
        if (curr + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(curr + 1);
    }

    // consume chars
    private char advance() {
        curr++;
        return source.charAt(curr - 1);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'A' && c <= 'Z') ||
                (c >= 'a' && c <= 'z') ||
                (c == '_');
    }

    private boolean isAlphaNumber(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, curr);
        tokens.add(new Token(type, text, literal, line));
    }

}
