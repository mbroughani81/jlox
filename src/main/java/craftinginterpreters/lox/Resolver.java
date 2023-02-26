package craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import craftinginterpreters.lox.Expr.Assign;
import craftinginterpreters.lox.Expr.Binary;
import craftinginterpreters.lox.Expr.Call;
import craftinginterpreters.lox.Expr.Grouping;
import craftinginterpreters.lox.Expr.Literal;
import craftinginterpreters.lox.Expr.Logical;
import craftinginterpreters.lox.Expr.Unary;
import craftinginterpreters.lox.Expr.Variable;
import craftinginterpreters.lox.Stmt.Block;
import craftinginterpreters.lox.Stmt.Expression;
import craftinginterpreters.lox.Stmt.Function;
import craftinginterpreters.lox.Stmt.If;
import craftinginterpreters.lox.Stmt.Print;
import craftinginterpreters.lox.Stmt.Return;
import craftinginterpreters.lox.Stmt.Var;
import craftinginterpreters.lox.Stmt.While;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack();
    private FunctionType currentFunction = FunctionType.NONE;

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION
    }    

    public Void visitAssignExpr(Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    public Void visitBinaryExpr(Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    public Void visitCallExpr(Call expr) {
        resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    public Void visitGroupingExpr(Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    public Void visitLiteralExpr(Literal expr) {
        return null;
    }

    public Void visitLogicalExpr(Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    public Void visitUnaryExpr(Unary expr) {
        resolve(expr.right);
        return null;
    }

    public Void visitVariableExpr(Variable expr) {
        if (!scopes.isEmpty() &&
            scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Lox.error(expr.name,"Can't read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    public Void visitBlockStmt(Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    public Void visitExpressionStmt(Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    public Void visitFunctionStmt(Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    private void resolveFunction(Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();

        currentFunction = enclosingFunction;
    }

    public Void visitIfStmt(If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    public Void visitPrintStmt(Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    public Void visitReturnStmt(Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }
        
        if (stmt.value != null) {
            resolve(stmt.value);
        }

        return null;
    }

    public Void visitVarStmt(Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    public Void visitWhileStmt(While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    void beginScope() {
        scopes.push(new HashMap());
    }

    void endScope() {
        scopes.pop();
    }

    void declare(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, Boolean> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name,"Already a variable with this name in this scope.");
        }
        scope.put(name.lexeme, false);
    }

    void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    void resolve(Stmt stmt) {
        stmt.accept(this);
    }
    
    void resolve(Expr expr) {
        expr.accept(this);
    }

}