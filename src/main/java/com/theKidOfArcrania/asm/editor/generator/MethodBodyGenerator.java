package com.theKidOfArcrania.asm.editor.generator;

import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Scanner;

import static java.lang.System.out;

/**
 * Generates the MethodBodyGenerator class.
 * @author Henry Wang
 */
public class MethodBodyGenerator
{
    public static void main(String[] args) throws IOException
    {
        Scanner in = new Scanner(ClassLoader.getSystemResourceAsStream(
                "com/theKidOfArcrania/asm/editor/generator/MethodBody.gen"));
        while (in.hasNextLine())
        {
            String line = in.nextLine();
            if (line.contains("<GENCODE>"))
                gencode();
            else
                out.println(line);
        }
    }


    private static void gencode()
    {
        Class<MethodVisitor> mthVisitor = MethodVisitor.class;
        ArrayList<Method> voidMths = new ArrayList<>();
        for (Method mth : mthVisitor.getMethods())
        {
            if (mth.getReturnType() == void.class && mth.getName().startsWith("visit"))
                voidMths.add(mth);
        }

        code(1, "public void accept(MethodVisitor visitor) {");
        code(2, "for (Action action : actions) {");
        code(3, "switch (action.method) {");
        for (int i = 0; i < voidMths.size(); i++)
        {
            Method mth = voidMths.get(i);
            code(4, "case " + i + ":");

            Class<?> args[] = mth.getParameterTypes();
            StringBuilder sb = new StringBuilder("visitor.");
            sb.append(mth.getName()).append("(");
            for (int j = 0; j < args.length; j++)
            {
                if (j > 0)
                    sb.append(", ");
                sb.append("(").append(args[j].getCanonicalName()).append(") action.args[").append(j).append("]");
            }
            sb.append(");");

            code(5, sb.toString());
            code(5, "break;");
        }
        code(3, "}");
        code(2, "}");
        code(1, "}");

        for (int i = 0; i < voidMths.size(); i++)
        {
            Method mth = voidMths.get(i);

            Parameter params[] = mth.getParameters();
            StringBuilder sb = new StringBuilder("public void ");
            sb.append(mth.getName()).append("(");
            for (int j = 0; j < params.length; j++)
            {
                if (j > 0)
                    sb.append(", ");
                sb.append(params[j].getType().getCanonicalName()).append(" ").append(params[j].getName());
            }
            sb.append(") {");
            code(1, sb.toString());

            code(2, "if (fixed) {");
            code (3, "throw new IllegalStateException(\"Cannot modify this method body.\");");
            code (2, "}");

            if (mth.getName().equals("visitEnd"))
                code(2, "fixed = true;");

            sb = new StringBuilder("actions.add(new Action(");
            sb.append(i);
            sb.append(", new Object[] {");
            for (int j = 0; j < params.length; j++)
            {
                if (j > 0)
                    sb.append(", ");
                sb.append(params[j].getName());
            }
            sb.append("}));");
            code(2, sb.toString());

            code(1, "}");
        }
    }

    private static void code(int indent, String line)
    {
        for (int i = 0; i < indent * 4; i++)
            System.out.print(' ');
        System.out.println(line);
    }
}
