/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.deltaspike.jpa.impl.transaction.context;



import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.apache.deltaspike.jpa.api.TransactionScoped;
import org.apache.deltaspike.jpa.api.Transactional;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * CDI Context for managing &#064;{@link org.apache.deltaspike.jpa.api.TransactionScoped} contextual instances.
 */
public class TransactionContext implements Context
{
    // Attention! this is not a normal instance but a PROXY
    // thus it resolves the correct contextual instance every time
    // it will lazily initialized at runtime after the container
    // got started.
    private TransactionBeanStorage beanStorage;

    public <T> T get(Contextual<T> component)
    {
        Map<Contextual, TransactionBeanEntry> transactionBeanEntryMap = getBeanStorage().getActiveTransactionContext();

        if (transactionBeanEntryMap == null)
        {
            throw new ContextNotActiveException("Not accessed within a transactional method - use @" +
                    Transactional.class.getName());
        }

        TransactionBeanEntry transactionBeanEntry = transactionBeanEntryMap.get(component);
        if (transactionBeanEntry != null)
        {
            return (T) transactionBeanEntry.getContextualInstance();
        }

        return null;
    }

    public <T> T get(Contextual<T> component, CreationalContext<T> creationalContext)
    {
        Map<Contextual, TransactionBeanEntry> transactionBeanEntryMap = getBeanStorage().getActiveTransactionContext();

        if (transactionBeanEntryMap == null)
        {
            throw new ContextNotActiveException("Not accessed within a transactional method - use @" +
                    Transactional.class.getName());
        }

        TransactionBeanEntry transactionBeanEntry = transactionBeanEntryMap.get(component);
        if (transactionBeanEntry != null)
        {
            return (T) transactionBeanEntry.getContextualInstance();
        }

        // if it doesn't yet exist, we need to create it now!
        T instance = component.create(creationalContext);
        transactionBeanEntry = new TransactionBeanEntry(component, instance, creationalContext);
        transactionBeanEntryMap.put(component, transactionBeanEntry);

        return instance;
    }

    public Class<? extends Annotation> getScope()
    {
        return TransactionScoped.class;
    }

    public boolean isActive()
    {
        try
        {
            return getBeanStorage().getActiveTransactionContext() != null;
        }
        catch (ContextNotActiveException e)
        {
            return false;
        }
    }

    private TransactionBeanStorage getBeanStorage()
    {
        if (beanStorage == null)
        {
            synchronized (this)
            {
                beanStorage = BeanProvider.getContextualReference(TransactionBeanStorage.class);
            }
        }
        return beanStorage;
    }

}