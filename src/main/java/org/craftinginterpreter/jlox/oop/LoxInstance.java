package org.craftinginterpreter.jlox.oop;

import org.craftinginterpreter.jlox.executor.RuntimeError;
import org.craftinginterpreter.jlox.function.LoxFunction;
import org.craftinginterpreter.jlox.scanner.Token;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    private LoxClass kclass;

    private final Map<String, Object> fields = new HashMap<>();
    LoxInstance(LoxClass kclass) {
        this.kclass = kclass;
    }

    @Override
    public String toString() {
        return kclass.name + " instance";
    }

    public Object get(Token name) {
        if (fields.containsKey(name.getLexeme())) {
            return fields.get(name.getLexeme());
        }

        LoxFunction method = kclass.findMethod(name.getLexeme());
        if (method != null) {
            return method.bind(this);
        }

        throw new RuntimeError(name, "undefined property '" + name.getLexeme() + "'." );
    }

    public void set(Token name, Object value) {
        fields.put(name.getLexeme(), value);
    }
}
