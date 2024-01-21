package org.craftinginterpreter.jlox.semanticanalysis;

import org.craftinginterpreter.jlox.function.FunctionType;
import org.craftinginterpreter.jlox.executor.Interpreter;
import org.craftinginterpreter.jlox.Lox;
import org.craftinginterpreter.jlox.scanner.Token;
import org.craftinginterpreter.jlox.ast.Expr;
import org.craftinginterpreter.jlox.ast.Stmt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private final Interpreter interpreter;

    private final Stack<Map<String, Boolean>> scopes = new Stack<>();

    private enum ClassType {
        NONE,
        CLASS,
        SUBCLASS
    }

    private ClassType currentClassType = ClassType.NONE;
    private FunctionType currentFunctionType = FunctionType.NONE;

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        if (currentClassType == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'super' outside of a class");
        }
        if (currentClassType != ClassType.SUBCLASS) {
            Lox.error(expr.keyword, "Can't use 'super' in a class without super class");
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        if (currentClassType == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside a class");
        }

        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Object visitVarExpr(Expr.Var expr) {

        if (!scopes.empty() && scopes.peek().get(expr.name.getLexeme()) == Boolean.FALSE) {
            // var a = a;
            Lox.error(expr.name, "Can't read local variable in its own initializer.");
        }

        // var a = b; resolveLocal(b, b)
        resolveLocal(expr, expr.name);
        return null;
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; --i) {
            if (scopes.get(i).containsKey(name.getLexeme())) {
                interpreter.resolve(expr, scopes.size() - i - 1);
                return ;
            }
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingclass = currentClassType;
        currentClassType = ClassType.CLASS;
        declare(stmt.name);
        define(stmt.name);

        if (stmt.superclass != null && stmt.name.getLexeme().equals(stmt.superclass.name.getLexeme())) {
            Lox.error(stmt.name, "A class cannot inherit.lox from itself.");
        }

        if (stmt.superclass != null) {
            currentClassType = ClassType.SUBCLASS;
            resolve(stmt.superclass);
        }

        if (stmt.superclass != null) {
            beginScope();
            scopes.peek().put("super", true);
        }

        beginScope();
        scopes.peek().put("this", true);
        for (Stmt.Function function: stmt.method) {
            FunctionType functionType = FunctionType.METHOD;
            if (function.name.getLexeme().equals("init")) {
                functionType = FunctionType.INITIALIZER;
            }
            resolveFunction(function, functionType);
        }
        endScope();
        currentClassType = enclosingclass;
        if (stmt.superclass != null) {
            endScope();
        }
        return null;
    }

    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    private void endScope() {
        scopes.pop();
    }

    public void resolve(List<Stmt> stmtList) {
        for (Stmt stmt : stmtList) {
            resolve(stmt);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosingType = currentFunctionType;
        currentFunctionType = type;
        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        currentFunctionType = enclosingType;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunctionType == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }
        if (stmt.value != null) {
            if (currentFunctionType == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Can't return a value from an initializer");
            }
            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) {
            return ;
        }
        Map<String, Boolean> scope = scopes.peek();
        if (scope.containsKey(name.getLexeme())) {
            Lox.error(name, "Already variable with this name in this scope.");
        }
        scope.put(name.getLexeme(), false); // not ready
    }

    private void define(Token name) {
        if (scopes.isEmpty()) {
            return ;
        }
        scopes.peek().put(name.getLexeme(), true); // alive
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }
}
