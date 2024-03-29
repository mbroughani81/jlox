package craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap();

    public Interpreter() {
        globals.define("clock", new LoxCallable() {
            public int arity() { return 0; }
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }
            public String toString() { return "<native fn>"; }
        });
    }

    void interpret(List<Stmt> statements) { 
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    // eval expression 
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    public Object visitAssignExpr(Assign expr) {
        Object value = evaluate(expr.value);
        
        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        
        return value;
    }
    
    public Object visitLogicalExpr(Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);
    
        if (!(object instanceof LoxInstance)) { 
          throw new RuntimeError(expr.name,
                                 "Only instances have fields.");
        }
    
        Object value = evaluate(expr.value);
        ((LoxInstance)object).set(expr.name, value);
        return value;
      }

    public Object visitBinaryExpr(Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (Double)left > (Double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);    
                return (Double)left >= (Double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (Double)left < (Double)right;
            case LESS_EQUAL:
                    checkNumberOperands(expr.operator, left, right);
                return (Double)left <= (Double)right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (Double)left - (Double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);    
                return (Double)left / (Double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (Double)left + (Double)right;
                } 
        
                if (left instanceof String && right instanceof String) {
                  return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator,"Operands must be two numbers or two strings.");
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (Double)left * (Double)right;
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
        }

        return null;
    }

    public Object visitCallExpr(Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<Object>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }
        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren,
                "Can only call functions and classes.");
        }
        LoxCallable function = (LoxCallable)callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                function.arity() + " arguments but got " +
                arguments.size() + ".");
        }
        return function.call(this, arguments);
    }

    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
          return ((LoxInstance)object).get(expr.name);
        }
    
        throw new RuntimeError(expr.name,
            "Only instances have properties.");
    }

    public Object visitGroupingExpr(Grouping expr) {
        return evaluate(expr.expression);
    }

    public Object visitLiteralExpr(Literal expr) {
        return expr.value;
    }

    public Object visitUnaryExpr(Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(Double)right;
        }

        return null;
    }

    public Object visitVariableExpr(Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }
    
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (Boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
    
        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";
    
        if (object instanceof Double) {
          String text = object.toString();
          if (text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
          }
          return text;
        }
    
        return object.toString();
    }

    // execute statements
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    public Void visitIfStmt(If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    public Void visitExpressionStmt(Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    public Void visitFunctionStmt(Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    public Void visitPrintStmt(Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    

    public Void visitReturnStmt(Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new craftinginterpreters.lox.Return(value);
    }

    public Void visitWhileStmt(While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    public Void visitVarStmt(Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    public Void visitBlockStmt(Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);
        
        Map<String, LoxFunction> methods = new HashMap();
        for (Stmt.Function method : stmt.methods) {
            LoxFunction function = new LoxFunction(method, environment, method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }

        LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
        environment.assign(stmt.name, klass);
        return null;
    }
}
