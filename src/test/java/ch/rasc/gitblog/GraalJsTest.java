package ch.rasc.gitblog;

import static org.junit.jupiter.api.Assertions.*;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.junit.jupiter.api.Test;

class GraalJsTest {
    @Test void discoversEngineAndInvokesJavaScriptFunctions() throws Exception {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
        assertNotNull(engine, "Prism requires the GraalJS ScriptEngine provider");
        try {
            engine.eval("function highlight(code) { return '<code>' + code + '</code>'; }");
            assertEquals("<code>const x = 42;</code>", ((Invocable) engine).invokeFunction("highlight", "const x = 42;"));
        }
        finally {
            if (engine instanceof AutoCloseable closeable) { closeable.close(); }
        }
    }
}
