//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.language.java.runtime.compiler.shared;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

public class PureCompileAndExecuteJava
{
    private static final String PURE_COMP_RESULT = "meta::external::language::java::compiler::CompilationResult";
    private static final String PURE_EXEC_RESULT = "meta::external::language::java::compiler::ExecutionResult";
    private static final String PURE_COMP_AND_EXEC_RESULT = "meta::external::language::java::compiler::CompileAndExecuteResult";

    @SuppressWarnings("unused")
    public static CoreInstance compilePure(CoreInstance javaSource, CoreInstance config, ExecutionSupport executionSupport)
    {
        ImmutableList<CoreInstance> javaSources = (javaSource == null) ? Lists.immutable.empty() : Lists.immutable.with(javaSource);
        return compilePure(javaSources, config, executionSupport);
    }

    public static CoreInstance compilePure(Iterable<? extends CoreInstance> javaSources, CoreInstance config, ExecutionSupport executionSupport)
    {
        if (!(executionSupport instanceof CompiledExecutionSupport))
        {
            throw new IllegalArgumentException("Only CompiledExecutionSupport supported");
        }
        return compilePure(javaSources, config, ((CompiledExecutionSupport) executionSupport).getProcessorSupport());
    }

    public static CoreInstance compilePure(Iterable<? extends CoreInstance> javaSources, CoreInstance config, ProcessorSupport processorSupport)
    {
        CompilationResult javaCompResult = CompileAndExecuteJava.compile(toJavaFileObjects(javaSources), toCompileOptions(config));
        return toPureCompilationResult(javaCompResult, processorSupport);
    }

    @SuppressWarnings("unused")
    public static CoreInstance compileAndExecutePure(CoreInstance javaSource, CoreInstance compConfig, CoreInstance execConfig, ExecutionSupport executionSupport)
    {
        ImmutableList<CoreInstance> javaSources = (javaSource == null) ? Lists.immutable.empty() : Lists.immutable.with(javaSource);
        return compileAndExecutePure(javaSources, compConfig, execConfig, executionSupport);
    }

    public static CoreInstance compileAndExecutePure(Iterable<? extends CoreInstance> javaSources, CoreInstance compConfig, CoreInstance execConfig, ExecutionSupport executionSupport)
    {
        if (!(executionSupport instanceof CompiledExecutionSupport))
        {
            throw new IllegalArgumentException("Only CompiledExecutionSupport supported");
        }
        return compileAndExecutePure(javaSources, compConfig, execConfig, ((CompiledExecutionSupport) executionSupport).getProcessorSupport());
    }

    public static CoreInstance compileAndExecutePure(Iterable<? extends CoreInstance> javaSources, CoreInstance compConfig, CoreInstance execConfig, ProcessorSupport processorSupport)
    {
        CompilationResult javaCompResult = CompileAndExecuteJava.compile(toJavaFileObjects(javaSources), toCompileOptions(compConfig));
        ExecutionResult javaExecResult = javaCompResult.isSuccess() ? CompileAndExecuteJava.execute(javaCompResult, getExecClassName(execConfig), getExecMethodName(execConfig)) : null;
        return toPureCompileAndExecuteResult(javaCompResult, javaExecResult, processorSupport);
    }

    private static Collection<JavaFileObject> toJavaFileObjects(Iterable<? extends CoreInstance> javaSources)
    {
        return (javaSources == null) ?
                Lists.fixedSize.empty() :
                Iterate.collect(javaSources, PureCompileAndExecuteJava::toJavaFileObject);
    }

    private static JavaFileObject toJavaFileObject(CoreInstance javaSource)
    {
        String packageName = PrimitiveUtilities.getStringValue(javaSource.getValueForMetaPropertyToOne("package"));
        String className = PrimitiveUtilities.getStringValue(javaSource.getValueForMetaPropertyToOne("name"));
        String content = PrimitiveUtilities.getStringValue(javaSource.getValueForMetaPropertyToOne("content"));
        return new SimpleJavaFileObject(URI.create("string:///" + packageName.replace('.', '/') + "/" + className + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE)
        {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors)
            {
                return content;
            }

            @Override
            public InputStream openInputStream()
            {
                return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    private static Iterable<String> toCompileOptions(CoreInstance compilationConfig)
    {
        if (compilationConfig == null)
        {
            return Lists.immutable.empty();
        }

        ListIterable<? extends CoreInstance> classpath = compilationConfig.getValueForMetaPropertyToMany("classpath");
        MutableList<String> options = Lists.mutable.empty();
        if (classpath.notEmpty())
        {
            options.add("-classpath");
            if (File.separatorChar == '\\')
            {
                String escaped = "\\" + File.pathSeparator;
                options.add(classpath.asLazy().collect(e -> PrimitiveUtilities.getStringValue(e).replace(File.pathSeparator, escaped)).makeString(File.pathSeparator));
            }
            else
            {
                options.add(classpath.asLazy().collect(PrimitiveUtilities::getStringValue).makeString(File.pathSeparator));
            }
        }
        return options;
    }

    private static String getExecClassName(CoreInstance execConfig)
    {
        return PrimitiveUtilities.getStringValue(execConfig.getValueForMetaPropertyToOne("class"));
    }

    private static String getExecMethodName(CoreInstance execConfig)
    {
        return PrimitiveUtilities.getStringValue(execConfig.getValueForMetaPropertyToOne("method"));
    }

    private static CoreInstance toPureCompilationResult(CompilationResult javaCompResult, ProcessorSupport processorSupport)
    {
        CoreInstance pureCompResult = processorSupport.newCoreInstance(null, PURE_COMP_RESULT, null);

        CoreInstance success = processorSupport.newCoreInstance(Boolean.toString(javaCompResult.isSuccess()), M3Paths.Boolean, null);
        pureCompResult.setKeyValues(toPropertyPath(PURE_COMP_RESULT, "successful"), Lists.immutable.with(success));

        if (!javaCompResult.isSuccess())
        {
            ListIterable<CoreInstance> errorMessages = javaCompResult.getErrorMessages().collect(e -> processorSupport.newCoreInstance(e, M3Paths.String, null));
            pureCompResult.setKeyValues(toPropertyPath(PURE_COMP_RESULT, "errors"), errorMessages);
        }

        return pureCompResult;
    }

    private static CoreInstance toPureExecutionResult(ExecutionResult javaExecResult, ProcessorSupport processorSupport)
    {
        CoreInstance pureExecResult = processorSupport.newCoreInstance(null, PURE_EXEC_RESULT, null);

        CoreInstance success = processorSupport.newCoreInstance(Boolean.toString(javaExecResult.isSuccess()), M3Paths.Boolean, null);
        pureExecResult.setKeyValues(toPropertyPath(PURE_EXEC_RESULT, "successful"), Lists.immutable.with(success));

        if (!javaExecResult.isSuccess())
        {
            Throwable error = javaExecResult.getError();
            if (error != null)
            {
                StringWriter stringWriter = new StringWriter();
                try (PrintWriter printWriter = new PrintWriter(stringWriter))
                {
                    error.printStackTrace(printWriter);
                }
                CoreInstance errorMessage = processorSupport.newCoreInstance(stringWriter.toString(), M3Paths.String, null);
                pureExecResult.setKeyValues(toPropertyPath(PURE_EXEC_RESULT, "error"), Lists.immutable.with(errorMessage));
            }
        }

        return pureExecResult;
    }

    private static CoreInstance toPureCompileAndExecuteResult(CompilationResult javaCompResult, ExecutionResult javaExecResult, ProcessorSupport processorSupport)
    {
        CoreInstance pureCompAndExecResult = processorSupport.newCoreInstance(null, PURE_COMP_AND_EXEC_RESULT, null);

        CoreInstance pureCompResult = toPureCompilationResult(javaCompResult, processorSupport);
        pureCompAndExecResult.setKeyValues(toPropertyPath(PURE_COMP_AND_EXEC_RESULT, "compilationResult"), Lists.immutable.with(pureCompResult));

        if (javaExecResult != null)
        {
            CoreInstance pureExecResult = toPureExecutionResult(javaExecResult, processorSupport);
            pureCompAndExecResult.setKeyValues(toPropertyPath(PURE_COMP_AND_EXEC_RESULT, "executionResult"), Lists.immutable.with(pureExecResult));
        }
        return pureCompAndExecResult;
    }

    private static ListIterable<String> toPropertyPath(String classPath, String propertyName)
    {
        return _Package.convertM3PathToM4(classPath).with(M3Properties.properties).with(propertyName);
    }
}
