/*
 *  Copyright ETH 2024 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.systemsx.cisd.openbis.generic.shared.dto.hibernate;

import static org.testng.Assert.*;

import ch.systemsx.cisd.openbis.generic.shared.dto.*;
import jakarta.validation.ClockProvider;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;

public class PatternValueValidatorTest
{

    private PatternValueValidator validator;

    @BeforeMethod
    public void setUp()
    {
       validator = new PatternValueValidator();
       validator.initialize(null);
    }

    @Test
    public void validation_noConstraintsTriggered()
    {

        EntityPropertyPE propertyPE = getPropertyForTest("2", "(2|4)", "PATTERN");
        TestContext context = new TestContext();

        boolean result = validator.isValid(propertyPE, context);
        assertTrue(result);
        assertTrue(context.constraints.isEmpty());
        assertTrue(!context.disableDefaultTriggered);

    }

    @Test
    public void validation_constraintsTriggered()
    {

        EntityPropertyPE propertyPE = getPropertyForTest("3", "(2|4)", "PATTERN");
        TestContext context = new TestContext();

        boolean result = validator.isValid(propertyPE, context);
        assertFalse(result);
        assertTrue(context.disableDefaultTriggered);
        assertFalse(context.constraints.isEmpty());
        assertEquals(context.constraints.size(), 1);
        assertEquals(context.constraints.get(0), "Value: '" + propertyPE.getValue() + "' is not matching defined pattern!");
    }

    private EntityPropertyPE getPropertyForTest(String value, String pattern, String patternType)
    {
        PropertyTypePE pt = new PropertyTypePE();
        pt.setCode("TEST_CODE");

        EntityTypePropertyTypePE etpt = new SampleTypePropertyTypePE();
        etpt.setId(1L);
        etpt.setPropertyType(pt);
        etpt.setPattern(pattern);
        etpt.setPatternType(patternType);

        EntityPropertyPE propertyPE = new SamplePropertyPE();
        propertyPE.setEntityTypePropertyType(etpt);

        propertyPE.setValue(value);

        return propertyPE;
    }


    private static final class TestContext implements ConstraintValidatorContext
    {
        private final List<String> constraints = new ArrayList<>();
        private boolean disableDefaultTriggered = false;

        @Override
        public void disableDefaultConstraintViolation()
        {
            disableDefaultTriggered = true;
        }

        @Override
        public String getDefaultConstraintMessageTemplate()
        {
            return null;
        }

        @Override
        public ClockProvider getClockProvider()
        {
            return null;
        }

        @Override
        public ConstraintViolationBuilder buildConstraintViolationWithTemplate(String s)
        {
            constraints.add(s);
            return new ConstraintViolationBuilder()
            {
                @Override
                public NodeBuilderDefinedContext addNode(String s)
                {
                    return null;
                }

                @Override
                public NodeBuilderCustomizableContext addPropertyNode(String s)
                {
                    return null;
                }

                @Override
                public LeafNodeBuilderCustomizableContext addBeanNode()
                {
                    return null;
                }

                @Override
                public ContainerElementNodeBuilderCustomizableContext addContainerElementNode(
                        String s, Class<?> aClass, Integer integer)
                {
                    return null;
                }

                @Override
                public NodeBuilderDefinedContext addParameterNode(int i)
                {
                    return null;
                }

                @Override
                public ConstraintValidatorContext addConstraintViolation()
                {
                    return null;
                }
            };
        }

        @Override
        public <T> T unwrap(Class<T> aClass)
        {
            return null;
        }

    }

}
