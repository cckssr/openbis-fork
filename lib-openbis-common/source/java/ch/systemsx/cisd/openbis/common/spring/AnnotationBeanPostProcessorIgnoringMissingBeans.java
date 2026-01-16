/*
 * Copyright ETH 2012 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.common.spring;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

/**
 * Spring 6 compatible: ignores missing @Resource dependencies (logs and skips injection).
 * Replaces the old subclass that overrode a removed LookupElement-based method.
 */
public class AnnotationBeanPostProcessorIgnoringMissingBeans
        implements InstantiationAwareBeanPostProcessor,
        MergedBeanDefinitionPostProcessor,
        PriorityOrdered,
        BeanFactoryAware {

    private static final Logger operationLog =
            LogFactory.getLogger(LogCategory.OPERATION, AnnotationBeanPostProcessorIgnoringMissingBeans.class);

    private final CommonAnnotationBeanPostProcessor delegate = new CommonAnnotationBeanPostProcessor();
    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        if (delegate instanceof BeanFactoryAware) {
            ((BeanFactoryAware) delegate).setBeanFactory(beanFactory);
        }
    }

    // Run close to default processors
    @Override public int getOrder() { return Ordered.LOWEST_PRECEDENCE - 10; }

    // Let the delegate prepare metadata as usual
    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        delegate.postProcessMergedBeanDefinition(beanDefinition, beanType, beanName);
    }

    /**
     * Perform JSR-250 (@Resource) injection via the delegate, but ignore missing beans.
     */
    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        try {
            return delegate.postProcessProperties(pvs, bean, beanName);
        } catch (NoSuchBeanDefinitionException | UnsatisfiedDependencyException e) {
            operationLog.warn("Couldn't resolve dependency for bean '" + beanName + "': " + e.getMessage());
            // Skip injecting the missing dependency; leave properties unchanged
            return pvs;
        } catch (BeansException e) {
            Throwable root = e.getCause();
            if (root instanceof NoSuchBeanDefinitionException || root instanceof UnsatisfiedDependencyException) {
                operationLog.warn("Couldn't resolve dependency for bean '" + beanName + "': " + root.getMessage());
                return pvs;
            }
            throw e;
        }
    }

    // No-ops / pass-throughs to satisfy interfaces and keep normal behavior
    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        return delegate.postProcessBeforeInstantiation(beanClass, beanName);
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        return delegate.postProcessAfterInstantiation(bean, beanName);
    }
    //@Override public PropertyValues postProcessPropertyValues(PropertyValues pvs, java.beans.PropertyDescriptor[] pds, Object bean, String beanName) { return pvs; }
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return delegate.postProcessBeforeInitialization(bean, beanName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return delegate.postProcessAfterInitialization(bean, beanName);
    }
}
