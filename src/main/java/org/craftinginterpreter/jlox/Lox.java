package org.craftinginterpreter.jlox;

import org.craftinginterpreter.jlox.ast.Parser;
import org.craftinginterpreter.jlox.ast.Stmt;
import org.craftinginterpreter.jlox.executor.Interpreter;
import org.craftinginterpreter.jlox.executor.RuntimeError;
import org.craftinginterpreter.jlox.scanner.Scanner;
import org.craftinginterpreter.jlox.scanner.Token;
import org.craftinginterpreter.jlox.scanner.TokenType;
import org.craftinginterpreter.jlox.semanticanalysis.Resolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    private static boolean hadError = false;
    private static final Interpreter interpreter = new Interpreter();
    private static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    //run the input file
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
         run(new String(bytes, Charset.defaultCharset()));
         if (hadError) {
             System.exit(65);
         }
         if (hadRuntimeError) {
             System.exit(70);
         }
    }

    //run with command line
    private static void runPrompt() throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(System.in);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        for (;;) {
            System.out.print(">");
            hadError = false;
            hadRuntimeError = false;
            String line = bufferedReader.readLine();
            if (line == null) {
                break;
            }
            run(line);
        }
    }

    private static void run(String sourceCode) {
        //scan the sourceCode and obtain a list of tokens
        Scanner scanner = new Scanner(sourceCode);
        List<Token> tokenList = scanner.scanTokens();

        Parser parser = new Parser(tokenList);
        List<Stmt> statements = parser.parse();
        if (hadError) {
            return;
        }

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);
        if (hadError) {
            return;
        }

        interpreter.interpret(statements);

    }

    //error handling
    public static void error(int line, String message) {
        report(line, "", message);
    }

    public static void runtimeError(RuntimeError runtimeError) {
        System.out.println(runtimeError.getMessage() + " at [line " + runtimeError.getToken().getLine() + "]");
    }

    public static void error(Token token, String message) {
        hadError = true;
        if (token.getTokenType() == TokenType.EOF) {
            report(token.getLine(), " at end", message);
        } else {
            report(token.getLine(), " at '" + token.getLexeme() + "'", message);
        }
    }

    private static void report(int line, String where, String message) {
        System.out.println("[line " + line + "] Error" + where + ": " + message);
    }
}
