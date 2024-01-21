package org.craftinginterpreter.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign   : Token name, Expr value",
                "Binary   : Expr left, Token operator, Expr right",
                "Call     : Expr callee, Token paren, List<Expr> arguments",
                "Get      : Expr object, Token name",
                "Set      : Expr object, Token name, Expr value",
                "Super    : Token keyword, Token method",
                "This     : Token keyword",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Logical  : Expr left, Token operator, Expr right",
                "Unary    : Token operator, Expr right",
                "Var      : Token name"
        ));

        defineAst(outputDir, "Stmt", Arrays.asList(
                "Block      : List<Stmt> statements",
                "Class      : Token name, Expr.Var superclass, List<Stmt.Function> method",
                "Expression : Expr expression",
                "Function   : Token name, List<Token> params, List<Stmt> body",
                "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "Return     : Token keyword, Expr value",
                "Print      : Expr expression",
                "Var        : Token name, Expr initializer",
                "While      : Expr condition, Stmt body"
        ));
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter printWriter = new PrintWriter(path, "UTF-8");

        printWriter.println("package org.craftinginterpreter.jlox.ast;");
        printWriter.println();
        printWriter.println("import java.util.List;");
        printWriter.println();
        printWriter.println("import org.craftinginterpreter.jlox.scanner.Token;");
        printWriter.println();
        printWriter.println("public abstract class " + baseName + " {");

        defineVisitor(printWriter, baseName, types);

        for (String type: types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(printWriter, baseName, className, fields);
        }

        printWriter.println();
        printWriter.println("  public abstract <R> R accept(Visitor<R> visitor);");
        printWriter.println();

        printWriter.println("}");
        printWriter.close();
    }

    private static void defineType(PrintWriter printWriter, String baseName, String className, String fields) {
        printWriter.println("  public static class " + className + " extends " + baseName + " {");
        printWriter.println("    public " + className + "(" + fields + ") {");
        String[] fieldList = fields.split(", ");
        for (String field: fieldList) {
            String name = field.split(" ")[1];
            printWriter.println("      this." + name + " = " + name + ";");
        }
        printWriter.println("    }");
        printWriter.println();

        printWriter.println("    @Override");
        printWriter.println("    public <R> R accept(Visitor<R> visitor) {");
        printWriter.println("        return visitor.visit" + className + baseName + "(this);");
        printWriter.println("    }");

        for (String field: fieldList) {
            printWriter.println("    public final " +  field + ";");
        }
        printWriter.println("  }");
    }

    private static void defineVisitor(PrintWriter printWriter, String baseName, List<String> types) {
        printWriter.println("  public interface Visitor<R> {");
        for (String type: types) {
            String typeName = type.split(":")[0].trim();
            printWriter.println("    public R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }
        printWriter.println("  }");
    }
}
