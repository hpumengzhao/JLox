package org.craftingintepreter.jlox;

import org.craftinginterpreter.jlox.scanner.Scanner;
import org.craftinginterpreter.jlox.scanner.TokenType;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ScannerTest {
    @Test
    public void testScanToken() throws IOException {
        String source = new String(Files.readAllBytes(Paths.get("src/test/resources/source1.lox")));
        Scanner scanner = new Scanner(source);
        Assert.assertEquals(scanner.scanTokens().get(8).getLiteral(), "ssss");
        Assert.assertEquals(scanner.scanTokens().get(10).getTokenType(), TokenType.WHILE);
    }
}
