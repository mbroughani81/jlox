package craftinginterpreters.lox;

import craftinginterpreters.lox.Expr.Assign;
import craftinginterpreters.lox.Expr.Binary;
import craftinginterpreters.lox.Expr.Call;
import craftinginterpreters.lox.Expr.Grouping;
import craftinginterpreters.lox.Expr.Literal;
import craftinginterpreters.lox.Expr.Logical;
import craftinginterpreters.lox.Expr.Unary;
import craftinginterpreters.lox.Expr.Variable;

public class AstPrinter implements Expr.Visitor<String> {

    String print(Expr expr) {
        return expr.accept(this);
    }

    

    public String visitCallExpr(Call expr) {
        // TODO Auto-generated method stub
        return null;
    }



    public String visitLogicalExpr(Logical expr) {
        // TODO Auto-generated method stub
        return null;
    }



    public String visitAssignExpr(Assign expr) {
        return parenthesize("set " + expr.name.lexeme, expr.value);
    }

    public String visitBinaryExpr(Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    public String visitGroupingExpr(Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    public String visitLiteralExpr(Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    public String visitUnaryExpr(Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    public String visitVariableExpr(Variable expr) {
        return expr.name.lexeme;
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
    
        builder.append("(").append(name);
        for (Expr expr : exprs) {
          builder.append(" ");
          builder.append(expr.accept(this));
        }
        builder.append(")");
    
        return builder.toString();
    }
    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
            new Expr.Unary(
                new Token(TokenType.MINUS, "-", null, 1),
                new Expr.Literal(123)),
            new Token(TokenType.STAR, "*", null, 1),
            new Expr.Grouping(
                new Expr.Literal(45.67)));
    
        System.out.println(new AstPrinter().print(expression));
    }
}
