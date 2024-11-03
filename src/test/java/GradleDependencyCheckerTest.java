import com.interships.GradleDependencyChecker;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GradleDependencyCheckerTest {

    private final String baseDir = "src/test/build/libs/";

    private static String[] getJarPaths(String... jarNames) {
        String[] jarPaths = new String[jarNames.length];
        for (int i = 0; i < jarNames.length; i++) {
            File jarFile = new File("src/test/build/libs/" + jarNames[i]);
            if (!jarFile.exists() || !jarFile.canRead()) {
                throw new RuntimeException("Required JAR file not found or unreadable: " + jarFile.getAbsolutePath());
            }
            jarPaths[i] = jarFile.getAbsolutePath();
        }
        return jarPaths;
    }

    @Test
    void testClassBWithOnlyModuleB() throws IOException {
        String[] jars = getJarPaths("ModuleB-1.0.jar");
        assertFalse(GradleDependencyChecker.canExecuteMainClass("com.jetbrains.internship2024.ClassB", jars),
                "Expected false as dependencies are missing");
    }

    @Test
    void testClassBWithModuleAAndModuleB() throws IOException {
        String[] jars = getJarPaths("ModuleA-1.0.jar", "ModuleB-1.0.jar");
        assertTrue(GradleDependencyChecker.canExecuteMainClass("com.jetbrains.internship2024.ClassB", jars),
                "Expected true as all dependencies are available");
    }

    @Test
    void testClassAWithOnlyModuleA() throws IOException {
        String[] jars = getJarPaths("ModuleA-1.0.jar");
        assertTrue(GradleDependencyChecker.canExecuteMainClass("com.jetbrains.internship2024.ClassA", jars),
                "Expected true as all dependencies are available");
    }

    @Test
    void testSomeAnotherClassWithOnlyModuleA() throws IOException {
        String[] jars = getJarPaths("ModuleA-1.0.jar");
        assertFalse(GradleDependencyChecker.canExecuteMainClass("com.jetbrains.internship2024.SomeAnotherClass", jars),
                "Expected false as dependencies are missing");
    }

    @Test
    void testSomeAnotherClassWithModuleAAndCommonsIo() throws IOException {
        String[] jars = getJarPaths("ModuleA-1.0.jar", "commons-io-2.16.1.jar");
        assertTrue(GradleDependencyChecker.canExecuteMainClass("com.jetbrains.internship2024.SomeAnotherClass", jars),
                "Expected true as all dependencies are available");
    }

    @Test
    void testClassB1WithOnlyModuleB() throws IOException {
        String[] jars = getJarPaths("ModuleB-1.0.jar");
        assertTrue(GradleDependencyChecker.canExecuteMainClass("com.jetbrains.internship2024.ClassB1", jars),
                "Expected true as all dependencies are available");
    }
}
