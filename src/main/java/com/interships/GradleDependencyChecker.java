package com.interships;

import org.objectweb.asm.*;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class GradleDependencyChecker {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java GradleDependencyChecker <MainClassName> <path-to-jar1> [<path-to-jar2> ...]");
            return;
        }

        String mainClassName = args[0];
        String[] jarPaths = new String[args.length - 1];
        System.arraycopy(args, 1, jarPaths, 0, jarPaths.length);

        try {
            boolean result = canExecuteMainClass(mainClassName, jarPaths);
            System.out.println(result ? "true: All dependencies available" : "false: Missing dependencies");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean canExecuteMainClass(String mainClassName, String[] jarPaths) throws IOException {
        Set<String> availableClasses = new HashSet<>();

        // Load classes from provided JAR files
        for (String jarPath : jarPaths) {
            File jarFile = new File(jarPath);
            if (!jarFile.exists()) {
                System.out.println("JAR file not found: " + jarPath);
                return false;
            }
            try (JarFile jar = new JarFile(jarFile)) {
                jar.stream()
                        .filter(e -> e.getName().endsWith(".class"))
                        .forEach(e -> availableClasses.add(classNameFromEntry(e)));
            }
        }

        // Check if the main class is available
        if (!availableClasses.contains(mainClassName)) {
            System.out.println("Main class not found in provided JARs.");
            return false;
        }

        // Analyze dependencies of the main class
        Set<String> requiredClasses = analyzeDependencies(mainClassName, jarPaths);

        System.out.println("Available classes: " + availableClasses);
        System.out.println("Required classes for " + mainClassName + ": " + requiredClasses);

        // Verify all required classes are available
        for (String requiredClass : requiredClasses) {
            if (!availableClasses.contains(requiredClass)) {
                System.out.println("Missing dependency: " + requiredClass);
                return false;
            }
        }

        return true;
    }

    private static String classNameFromEntry(ZipEntry entry) {
        return entry.getName().replace('/', '.').replace(".class", "");
    }

    private static Set<String> analyzeDependencies(String className, String[] jarPaths) throws IOException {
        Set<String> dependencies = new HashSet<>();
        Set<String> visitedClasses = new HashSet<>(); // Track already-processed classes to avoid infinite recursion
        collectDependenciesRecursively(className, jarPaths, dependencies, visitedClasses);

        // Remove array types and collection-related class names
        return cleanDependencyList(dependencies);
    }

    private static Set<String> cleanDependencyList(Set<String> dependencies) {
        Set<String> cleanedDependencies = new HashSet<>();
        for (String dependency : dependencies) {
            // Remove array syntax and collection wrappers if the base type is available
            String baseClass = dependency.replace("[]", "");  // Handle arrays
            if (!isJavaStandardClass(baseClass)) {
                cleanedDependencies.add(baseClass);
            }
        }
        return cleanedDependencies;
    }

    private static void collectDependenciesRecursively(String className, String[] jarPaths, Set<String> dependencies, Set<String> visitedClasses) throws IOException {
        if (visitedClasses.contains(className)) {
            return; // Already processed this class
        }
        visitedClasses.add(className);

        for (String jarPath : jarPaths) {
            try (JarFile jarFile = new JarFile(jarPath)) {
                ZipEntry entry = jarFile.getEntry(className.replace('.', '/') + ".class");
                if (entry != null) {
                    ClassReader classReader = new ClassReader(jarFile.getInputStream(entry));
                    classReader.accept(new DependencyClassVisitor(dependencies), 0);

                    // Recursively collect dependencies of the dependencies
                    for (String dependency : new HashSet<>(dependencies)) {
                        collectDependenciesRecursively(dependency, jarPaths, dependencies, visitedClasses);
                    }
                }
            }
        }
    }

    private static class DependencyClassVisitor extends ClassVisitor {
        private final Set<String> dependencies;

        public DependencyClassVisitor(Set<String> dependencies) {
            super(Opcodes.ASM9);
            this.dependencies = dependencies;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            if (superName != null && !isJavaStandardClass(superName)) {
                dependencies.add(Type.getObjectType(superName).getClassName());
            }
            for (String iface : interfaces) {
                if (!isJavaStandardClass(iface)) {
                    dependencies.add(Type.getObjectType(iface).getClassName());
                }
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                    if (!isJavaStandardClass(owner)) {
                        dependencies.add(Type.getObjectType(owner).getClassName());
                    }
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    if (!isJavaStandardClass(owner)) {
                        dependencies.add(Type.getObjectType(owner).getClassName());
                    }
                }

                @Override
                public void visitTypeInsn(int opcode, String type) {
                    if (!isJavaStandardClass(type)) {
                        dependencies.add(Type.getObjectType(type).getClassName());
                    }
                }
            };
        }
    }

    private static boolean isJavaStandardClass(String className) {
        return className.startsWith("java/") || className.startsWith("javax/") || className.startsWith("jdk/") || isPrimitiveType(className);
    }

    private static boolean isPrimitiveType(String className) {
        return Set.of("boolean", "char", "byte", "short", "int", "long", "float", "double", "void",
                "java.lang.String", "java.lang.Integer", "java.lang.Double", "java.lang.Float",
                "java.lang.Boolean", "java.lang.Character", "java.lang.Byte", "java.lang.Short",
                "java.lang.Long").contains(className);
    }
}
