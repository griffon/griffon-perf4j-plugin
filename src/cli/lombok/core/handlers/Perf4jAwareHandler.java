/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lombok.core.handlers;

import lombok.ast.Expression;
import lombok.ast.IMethod;
import lombok.ast.IType;

import static lombok.ast.AST.*;

/**
 * @author Andres Almiray
 */
public abstract class Perf4jAwareHandler<TYPE_TYPE extends IType<? extends IMethod<?, ?, ?, ?>, ?, ?, ?, ?, ?>> extends AbstractHandler<TYPE_TYPE> implements Perf4jAwareConstants {
    private Expression<?> defaultPerf4jProviderInstance() {
        return Call(Name(DEFAULT_PERF4j_PROVIDER_TYPE), "getInstance");
    }

    public void addPerf4jProviderField(final TYPE_TYPE type) {
        addField(type, PERF4j_PROVIDER_TYPE, PERF4j_PROVIDER_FIELD_NAME, defaultPerf4jProviderInstance());
    }

    public void addPerf4jProviderAccessors(final TYPE_TYPE type) {
        type.editor().injectMethod(
            MethodDecl(Type(VOID), METHOD_SET_PERF4j_PROVIDER)
                .makePublic()
                .withArgument(Arg(Type(PERF4j_PROVIDER_TYPE), PROVIDER))
                .withStatement(
                    If(Equal(Name(PROVIDER), Null()))
                        .Then(Block()
                            .withStatement(Assign(Field(PERF4j_PROVIDER_FIELD_NAME), defaultPerf4jProviderInstance())))
                        .Else(Block()
                            .withStatement(Assign(Field(PERF4j_PROVIDER_FIELD_NAME), Name(PROVIDER)))))
        );

        type.editor().injectMethod(
            MethodDecl(Type(PERF4j_PROVIDER_TYPE), METHOD_GET_PERF4j_PROVIDER)
                .makePublic()
                .withStatement(Return(Field(PERF4j_PROVIDER_FIELD_NAME)))
        );
    }

    public void addPerf4jContributionMethods(final TYPE_TYPE type) {
        delegateMethodsTo(type, METHODS, Field(PERF4j_PROVIDER_FIELD_NAME));
    }
}
