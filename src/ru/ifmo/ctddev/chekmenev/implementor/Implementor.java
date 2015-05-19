package ru.ifmo.ctddev.chekmenev.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * This class create class you provide
 * This class can create only non-static and non-private classes
 * with non-private constructors
 *
 * @author Alexander Chekmenev, alexchekmenev@mail.ru
 * @see info.kgeorgiy.java.advanced.implementor.Impler
 * @see info.kgeorgiy.java.advanced.implementor.JarImpler
 */
public class Implementor implements Impler, JarImpler {

    /**
     * Takes array of parameters and returns their as string with modifiers, type and name
     * (for example, as "final int arg0, String arg2")
     *
     * @param params parameters
     * @return parameters as string
     * @see java.lang.reflect.Parameter
     * @see java.lang.reflect.Modifier
     */
    private String getParametersAsString(Parameter[] params) {
        String asString = "";
        for (Parameter param : params) {
            String modifiers = Modifier.toString(param.getModifiers());
            if (modifiers.length() != 0) {
                modifiers += " ";
            }
            asString += (modifiers + param.getType().getCanonicalName() + " " + param.getName());
            if (param != params[params.length - 1]) {
                asString += ", ";
            }
        }
        return asString;
    }

    /**
     * Takes {@link java.lang.reflect.Executable} ({@link java.lang.reflect.Constructor} or {@link java.lang.reflect.Method})
     * and returns their exceptions as string (for example, as "throws E1, E2, E3")
     *
     * @param executable {@link java.lang.reflect.Constructor} or {@link java.lang.reflect.Method}
     * @return throws as string
     * @see java.lang.reflect.Executable
     */
    private String getThrowsAsString(Executable executable) {
        String asString = "";
        Class[] exceptions = executable.getExceptionTypes();
        if (exceptions.length > 0) {
            asString = " throws ";
            for (Class exception : exceptions) {
                asString += exception.getCanonicalName();
                if (exception != exceptions[exceptions.length - 1]) {
                    asString += ", ";
                }
            }
        }
        return asString;
    }

    /**
     * Create new class with suffix Impl and directories using its package
     *
     * @param clazz class, new class extended from
     * @param file root of first package directory
     * @return file of new class placed in package directory
     * @throws IOException if problems with creating file will occur
     */
    private File makePackageDirs(Class clazz, File file) throws IOException {
        String packagePath = "/" + clazz.getPackage().getName().replace('.', '/');
        String classPath = file.getAbsoluteFile() + packagePath + "/" + clazz.getSimpleName() + "Impl.java";
        File newClassFile = new File(classPath);
        newClassFile.getParentFile().mkdirs();
        return newClassFile;
    }

    /**
     * Recursive walker that look for all declared methods of class,
     * implemented interfaces and its superclass
     *
     * @param clazz class for looking for methods
     * @param visitor {@link MethodVisitor} for processing methods
     * @see java.lang.Class
     * @see java.lang.reflect.Method
     * @see ru.ifmo.ctddev.chekmenev.implementor.Implementor.MethodVisitor
     */
    private void walkThroughMethods(Class clazz, MethodVisitor visitor) {
        if (clazz == null) {
            return;
        }
        for (Method method : clazz.getDeclaredMethods()) {
            visitor.onVisit(method);
        }
        for (Class iface : clazz.getInterfaces()) {
            walkThroughMethods(iface, visitor);
        }
        walkThroughMethods(clazz.getSuperclass(), visitor);
    }

    /**
     * Write all methods from {@code clazz} using {@code writer}
     *
     * @param clazz methods of this class will be written
     * @param writer methods of {@code clazz} will be written using writer
     * @throws IOException if problems with writing will occur
     * @see java.lang.reflect.Method
     */
    private void writeMethodsToWriter(Class clazz, Writer writer) throws IOException {
        Map<String, Method> methods = new HashMap<>();

        // get all abstract methods ..
        walkThroughMethods(clazz, method -> {
            if (Modifier.isAbstract(method.getModifiers())) {
                String signature = method.getReturnType().getCanonicalName()
                        + method.getName()
                        + getParametersAsString(method.getParameters());
                if (!methods.containsKey(signature)) {
                    methods.put(signature, method);
                }
            }
        });

        // .. excluding methods with realisation
        walkThroughMethods(clazz, method -> {
            if (!Modifier.isAbstract(method.getModifiers())) {
                String signature = method.getReturnType().getCanonicalName()
                        + method.getName()
                        + getParametersAsString(method.getParameters());
                if (methods.containsKey(signature)) {
                    methods.remove(signature);
                }
            }
        });

        for (Method method : methods.values()) {
            String modifier = "";
            if (Modifier.isPublic(method.getModifiers())) {
                modifier = "public";
            } else if (Modifier.isProtected(method.getModifiers())) {
                modifier = "protected";
            }
            String signature = "";
            signature += modifier + " " + method.getReturnType().getCanonicalName() + " " + method.getName();
            signature += "(" + getParametersAsString(method.getParameters()) + ")" + getThrowsAsString(method);
            writer.write("    " + signature);
            writer.write(" {\n");
            writer.write("        return");

            if (method.getReturnType().isPrimitive()) {
                String returnType = method.getReturnType().getSimpleName();

                if (!returnType.equals("void")) {
                    writer.write(" ");
                }
                if (returnType.equals("short") || returnType.equals("int") || returnType.equals("byte")) {
                    writer.write("0");
                } else if (returnType.equals("long")) {
                    writer.write("0L");
                } else if (returnType.equals("float")) {
                    writer.write("0.0f");
                } else if (returnType.equals("double")) {
                    writer.write("0.0d");
                } else if (returnType.equals("char")) {
                    writer.write("0");
                } else if (returnType.equals("boolean")) {
                    writer.write("false");
                }
            } else {
                writer.write(" null");
            }
            writer.write(";\n");
            writer.write("    }\n\n");
        }

    }

    /**
     * Write public and protected constructors of {@code clazz} using {@code writer}
     *
     * @param clazz constructors of this class will be written
     * @param writer constructors of {@code clazz} will be written using writer
     * @throws IOException if problems with writing will occur
     * @throws ImplerException if class doesn't contain public or protected constructors
     * @see java.lang.reflect.Constructor
     */
    private void writeConstructorsToWriter(Class clazz, Writer writer) throws IOException, ImplerException {
        if (clazz.isInterface()) {
            return;
        }
        String name = clazz.getSimpleName() + "Impl";
        boolean hasConstructors = false;
        for (Constructor constructor : clazz.getDeclaredConstructors()) {
            if (Modifier.isPublic(constructor.getModifiers()) || Modifier.isProtected(constructor.getModifiers())) {
                hasConstructors = true;
                Parameter[] params = constructor.getParameters();
                String paramValuesAsString = "";
                for (Parameter parameter : params) {
                    paramValuesAsString += parameter.getName();
                    if (parameter != params[params.length - 1]) {
                        paramValuesAsString += ", ";
                    }
                }

                writer.write("    " + Modifier.toString(constructor.getModifiers() & Modifier.constructorModifiers()) + " " + name);
                writer.write("(" + getParametersAsString(params) + ")" + getThrowsAsString(constructor));
                writer.write(" {\n");
                writer.write("        super(" + paramValuesAsString + ");\n");
                writer.write("    }\n");
            }
        }
        if (!hasConstructors) {
            throw new ImplerException("No public or protected constructors found");
        }
    }

    /**
     * Create new class extended or implemented {@code clazz}
     * Function create .java file of new class with suffix Impl, that contains
     * all constructors and methods that should be implemented
     *
     * @param clazz class we extend
     * @param writer is used for writing new class
     * @throws IOException if problems with writing will occur
     * @throws ImplerException if problems with implementing will occur
     */
    private void writeClassToWriter(Class clazz, Writer writer) throws IOException, ImplerException {
        if (clazz.getPackage() != null) {
            writer.write("package " + clazz.getPackage().getName() + ";\n\n");
        }

        String extOrImpl = clazz.isInterface() ? " implements " : " extends ";

        writer.write("public class " + clazz.getSimpleName() + "Impl" + extOrImpl + clazz.getSimpleName());
        writer.write(" {\n");
        writeConstructorsToWriter(clazz, writer);
        writeMethodsToWriter(clazz, writer);
        writer.write("}\n");

    }

    /**
     * <p>Implement new class using {@code aClass} in {@code file}
     * with name same as aClass and with suffix Impl
     * <p>Function creates directories for package and file
     * (aClassName + "Impl").java, where aClassName is name of {@code aClass}
     * <p>New class placed in the same package like {@code aClass}
     * <p>New class contains of non-private constructors, all public or protected methods,
     * that wasn't implemented in superclasses.
     * <p>Every method and constructor has the same list of throwable like {@code aClass}
     *
     * @param aClass base class of of new class
     * @param file directory for new class
     * @throws ImplerException if problems with implementation will occur:
     *      <p>aClass == null
     *      <p>aClass is primitive
     *      <p>aClass is final
     *      <p>aClass doesn't contain non-private constructors
     */
    @Override
    public void implement(Class<?> aClass, File file) throws ImplerException {
        if (aClass == null || aClass.isPrimitive() || Modifier.isFinal(aClass.getModifiers())) {
            throw new ImplerException();
        }

        File newClassFile;
        try {
            newClassFile = makePackageDirs(aClass, file);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ImplerException("Can't create new class file");
        }

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newClassFile)));) {
            writeClassToWriter(aClass, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Interface is used for recursive walk
     *
     * @see #walkThroughMethods
     */
    private interface MethodVisitor {
        public void onVisit(Method method);
    }

    /**
     * Create jarFile for {@code aClass}
     * <p>Function creates temporary directory for {@code aClass},
     * using #implement and create jarFile using implemented class
     *
     * @param aClass implementing class
     * @param file jarFile
     * @throws ImplerException if problems with implementation will occur:
     * @see #implement
     */
    @Override
    public void implementJar(Class<?> aClass, File file) throws ImplerException {
        if (aClass== null) {
            throw new ImplerException("Implementing class is null");
        }
        if (file == null) {
            throw new ImplerException("JarFile is null");
        }
        File root = new File(".");
        try {
            root = Files.createTempDirectory("ImplTempRoot").toFile();
        } catch (IOException e) {
            System.err.println("Couldn't create temp directory");
        }
        implement(aClass, root);
        String name = (aClass.getPackage() != null ? aClass.getPackage().getName().replace(".", File.separator) : "")
                + File.separator + aClass.getSimpleName() + "Impl";
        int exitCode = compile(root, root.getAbsolutePath() + File.separator + name + ".java");
        if (exitCode != 0) {
            throw new ImplerException("Compilation error, exitCode = " + exitCode + ", name = " + name);
        }
        createJar(root.getAbsolutePath(), file.getAbsolutePath(), name + ".class");
    }

    /**
     * Takes class name as first argument, jarFile to generate to as second.
     * Generates a java class like {@link #implement implement}
     *
     * @param args arguments from the command line
     * @see #implementJar
     */
    public static void main(String[] args) {
        if (args == null || args.length < 2 || args[0] == null) {
            System.err.println("Too few arguments.");
            return;
        }
        if (args[0].equals("-jar") && args.length >= 3) {
            try {
                Class c = Class.forName(args[1]);
                new Implementor().implementJar(c, new File(args[2]));
            } catch (ClassNotFoundException e) {
                System.err.println("Class " + args[1] + " not found!");
            } catch (ImplerException e) {
                System.err.println("Class wasn't implemented:\n" + e.getMessage());
            }
        } else if (!args.equals("-jar")) {
            try {
                Class c = Class.forName(args[0]);
                new Implementor().implement(c, new File(args[1]));
            } catch (ClassNotFoundException e) {
                System.err.println("Class " + args[1] + " not found!");
            } catch (ImplerException e) {
                System.err.println("Class wasn't implemented:\n" + e.getMessage());
            }
        }
    }

    /**
     * Creates {@code JarFile} with the name {@code jarName} and {@code Manifest}
     * in the given directory {@code root} with {@code Class name}
     *
     * @param root    directory to create {@code JarFile} with the given {@code jarName}
     * @param jarName {@code file} to write to
     * @param name    {@code String} representing name of the {@code Class}
     * @see java.util.jar.Manifest
     */
    private static void createJar(String root, String jarName, String name) {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (FileInputStream fileInputStream = new FileInputStream(root + File.separator + name);
             JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarName), manifest)) {
            jarOutputStream.putNextEntry(new ZipEntry(name));
            byte[] b = new byte[1024];
            int c = 0;
            while ((c = fileInputStream.read(b)) >= 0) {
                jarOutputStream.write(b, 0, c);
            }
            jarOutputStream.closeEntry();
        } catch (IOException e) {
            System.err.println("== CreateJar, name = " + name + ", message = " + e.getMessage());
        }
    }

    /**
     * Returns result of the compiling
     *
     * @param root directory where the file locates
     * @param file name of the file to compile
     * @return resulting {@code int} of the compiling
     * @see javax.tools.JavaCompiler
     * @see javax.tools.ToolProvider
     */
    private static int compile(final File root, final String file) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final List<String> args = new ArrayList<>();
        args.add(file);
        args.add("-cp");
        args.add(root.getPath() + File.pathSeparator + System.getProperty("java.class.path"));
        return compiler.run(null, null, null, args.toArray(new String[args.size()]));
    }
}
