/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved
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

package com.huawei.flowcontrol;

import com.huawei.javamesh.core.agent.definition.EnhanceDefinition;
import com.huawei.javamesh.core.agent.definition.MethodInterceptPoint;
import com.huawei.javamesh.core.agent.matcher.ClassMatcher;
import com.huawei.javamesh.core.agent.matcher.ClassMatchers;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * dubbo增强
 * apache dubbo
 * alibaba dubbo
 */
public abstract class DubboDefinition implements EnhanceDefinition {
    private final String enhanceClass;

    private final String interceptorClass;

    protected DubboDefinition(String enhanceClass, String interceptorClass) {
        this.enhanceClass = enhanceClass;
        this.interceptorClass = interceptorClass;
    }

    @Override
    public ClassMatcher enhanceClass() {
        return ClassMatchers.named(enhanceClass);
    }

    @Override
    public MethodInterceptPoint[] getMethodInterceptPoints() {
        return new MethodInterceptPoint[]{
            MethodInterceptPoint.newInstMethodInterceptPoint(interceptorClass,
                ElementMatchers.<MethodDescription>named("invoke"))};
    }
}
