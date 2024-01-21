package org.craftinginterpreter.jlox.helper;

import org.craftinginterpreter.jlox.executor.Interpreter;

import java.util.List;

public interface LoxCallable {

    int arity(); // the number of parameters...
    Object call(Interpreter interpreter, List<Object> arguments);
}
