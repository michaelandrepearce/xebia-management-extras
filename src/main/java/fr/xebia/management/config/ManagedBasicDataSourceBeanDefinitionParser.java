/*
 * Copyright 2008-2010 Xebia and the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.xebia.management.config;

import java.sql.Connection;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import fr.xebia.springframework.jdbc.ManagedBasicDataSource;

public class ManagedBasicDataSourceBeanDefinitionParser extends AbstractBeanDefinitionParser {

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ManagedBasicDataSource.class);

        builder.setRole(BeanDefinition.ROLE_APPLICATION);
        builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));

        fillBuilderWithAttributeIfExists(builder, element, "accessToUnderlyingConnectionAllowed",
                "access-to-underlying-connection-allowed", boolean.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "connectionInitSqls", "connection-init-sqls", java.util.Collection.class,
                parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "connectionProperties", "connection-properties", String.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "defaultAutoCommit", "default-auto-commit", boolean.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "defaultCatalog", "default-catalog", String.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "defaultReadOnly", "default-read-only", boolean.class, parserContext);

        Element defaultTransactionIsolationElement = DomUtils.getChildElementByTagName(element, "default-transaction-isolation");
        if (defaultTransactionIsolationElement != null) {
            int defaultTransactionIsolation;
            String value = defaultTransactionIsolationElement.getAttribute("value");
            if ("NONE".equals(value)) {
                defaultTransactionIsolation = Connection.TRANSACTION_NONE;
            } else if ("READ_UNCOMMITTED".equals(value)) {
                defaultTransactionIsolation = Connection.TRANSACTION_READ_UNCOMMITTED;
            } else if ("READ_COMMITTED".equals(value)) {
                defaultTransactionIsolation = Connection.TRANSACTION_READ_COMMITTED;
            } else if ("REPEATABLE_READ".equals(value)) {
                defaultTransactionIsolation = Connection.TRANSACTION_REPEATABLE_READ;
            } else if ("SERIALIZABLE".equals(value)) {
                defaultTransactionIsolation = Connection.TRANSACTION_SERIALIZABLE;
            } else {
                String msg = "Invalid value for '<default-transaction-isolation' value=\"" + value + "\" />'";
                parserContext.getReaderContext().fatal(msg, defaultTransactionIsolationElement);
                throw new IllegalStateException(msg);
            }
            builder.addPropertyValue("defaultTransactionIsolation", defaultTransactionIsolation);

        }

        fillBuilderWithAttributeIfExists(builder, element, "driverClassName", "driver-class-name", String.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "initialSize", "initial-size", int.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "logAbandoned", "log-abandoned", boolean.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "maxActive", "max-active", int.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "maxIdle", "max-idle", int.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "maxOpenPreparedStatements", "max-open-prepared-statements", int.class,
                parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "maxWait", "max-wait", long.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "minEvictableIdleTimeMillis", "min-evictable-idle-time-millis", long.class,
                parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "minIdle", "min-idle", int.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "numTestsPerEvictionRun", "num-tests-per-eviction-run", int.class,
                parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "password", "password", String.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "poolPreparedStatements", "pool-prepared-statements", boolean.class,
                parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "removeAbandoned", "remove-abandoned", boolean.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "removeAbandonedTimeout", "remove-abandoned-timeout", int.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "testOnBorrow", "test-on-borrow", boolean.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "testOnReturn", "test-on-return", boolean.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "testWhileIdle", "test-while-idle", boolean.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "timeBetweenEvictionRunsMillis", "time-between-eviction-runs-millis",
                long.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "url", "url", String.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "username", "username", String.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "validationQuery", "validation-query", String.class, parserContext);
        fillBuilderWithAttributeIfExists(builder, element, "validationQueryTimeout", "validation-query-timeout", int.class, parserContext);

        builder.setDestroyMethodName("close");

        return builder.getBeanDefinition();
    }

    protected void fillBuilderWithAttributeIfExists(BeanDefinitionBuilder builder, Element parentElement, String propertyName,
            String elementName, Class<?> expectedType, ParserContext parserContext) {
        Element element = DomUtils.getChildElementByTagName(parentElement, elementName);

        if (element == null) {
            // skip
        } else {
            String value = element.getAttribute("value");
            if (value == null) {
                String msg = "Sub element '" + elementName + "' is missing attribute '" + "value" + "'";
                parserContext.getReaderContext().fatal(msg, parentElement);
                throw new IllegalStateException(msg);
            }
            builder.addPropertyValue(propertyName, value);
        }
    }

}
