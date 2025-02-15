// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClassResultType.class, name = "class"),
        @JsonSubTypes.Type(value = PartialClassResultType.class, name = "partialClass"),
        @JsonSubTypes.Type(value = TDSResultType.class, name = "tds"),
        @JsonSubTypes.Type(value = DataTypeResultType.class, name = "dataType"),
        @JsonSubTypes.Type(value = VoidResultType.class, name = "void"),
        @JsonSubTypes.Type(value = LazyVoidResultType.class, name = "lazyVoid"),
        @JsonSubTypes.Type(value = UpdateNodeResultType.class, name = "updateNode"),
})
public abstract class ResultType
{
}
