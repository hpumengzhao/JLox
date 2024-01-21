# Java Interpreter for Lox

## Lox is a simple oop programming language.

https://www.craftinginterpreters.com/

## Examples

1. Inherit from a class.
```
class Demo {
    init(a, b, c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
    cook() {
        print "Fry until golden brown.";
    }
}
class Sub < Demo{
    init(a, b, c) {
        super.init(a,b,c);
    }
}
var subclass = Sub(1,2,3);
print subclass.a;
print subclass.b;
print subclass.c;
```

2. closure function

```
fun makeCounter() {
    var i = 0;
    fun count() {
        i = i + 1;
        print i;
    }
    return count;
}
var counter = makeCounter();
counter(); // "1".
counter(); // "2".
```
3. recursive function
```
fun fib(n) {
    if (n <= 1) return n;
    return fib(n - 2) + fib(n - 1);
}
for (var i = 0; i < 20; i = i + 1) {
    print fib(i);
}
```