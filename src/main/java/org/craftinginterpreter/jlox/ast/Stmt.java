package org.craftinginterpreter.jlox.ast;

import java.util.List;

import org.craftinginterpreter.jlox.scanner.Token;

public abstract class Stmt {
  public interface Visitor<R> {
    public R visitBlockStmt(Block stmt);
    public R visitClassStmt(Class stmt);
    public R visitExpressionStmt(Expression stmt);
    public R visitFunctionStmt(Function stmt);
    public R visitIfStmt(If stmt);
    public R visitReturnStmt(Return stmt);
    public R visitPrintStmt(Print stmt);
    public R visitVarStmt(Var stmt);
    public R visitWhileStmt(While stmt);
  }
  public static class Block extends Stmt {
    public Block(List<Stmt> statements) {
      this.statements = statements;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitBlockStmt(this);
    }
    public final List<Stmt> statements;
  }
  public static class Class extends Stmt {
    public Class(Token name, Expr.Var superclass, List<Stmt.Function> method) {
      this.name = name;
      this.superclass = superclass;
      this.method = method;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitClassStmt(this);
    }
    public final Token name;
    public final Expr.Var superclass;
    public final List<Stmt.Function> method;
  }
  public static class Expression extends Stmt {
    public Expression(Expr expression) {
      this.expression = expression;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitExpressionStmt(this);
    }
    public final Expr expression;
  }
  public static class Function extends Stmt {
    public Function(Token name, List<Token> params, List<Stmt> body) {
      this.name = name;
      this.params = params;
      this.body = body;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitFunctionStmt(this);
    }
    public final Token name;
    public final List<Token> params;
    public final List<Stmt> body;
  }
  public static class If extends Stmt {
    public If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitIfStmt(this);
    }
    public final Expr condition;
    public final Stmt thenBranch;
    public final Stmt elseBranch;
  }
  public static class Return extends Stmt {
    public Return(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitReturnStmt(this);
    }
    public final Token keyword;
    public final Expr value;
  }
  public static class Print extends Stmt {
    public Print(Expr expression) {
      this.expression = expression;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitPrintStmt(this);
    }
    public final Expr expression;
  }
  public static class Var extends Stmt {
    public Var(Token name, Expr initializer) {
      this.name = name;
      this.initializer = initializer;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitVarStmt(this);
    }
    public final Token name;
    public final Expr initializer;
  }
  public static class While extends Stmt {
    public While(Expr condition, Stmt body) {
      this.condition = condition;
      this.body = body;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
        return visitor.visitWhileStmt(this);
    }
    public final Expr condition;
    public final Stmt body;
  }

  public abstract <R> R accept(Visitor<R> visitor);

}
