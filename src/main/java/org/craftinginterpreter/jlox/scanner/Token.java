package org.craftinginterpreter.jlox.scanner;

public class Token {
    private final TokenType tokenType;
    private final String lexeme; //source code string

    private final Object literal; // the reaL value of a variable...

    private final int line;

    public Token(TokenType tokenType, String lexeme, Object literal, int line) {
        this.tokenType = tokenType;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public String getLexeme() {
        return lexeme;
    }

    public Object getLiteral() {
        return literal;
    }

    public int getLine() {
        return line;
    }

    public String toString() {
        return tokenType + " " + lexeme + " " + literal;
    }
}
