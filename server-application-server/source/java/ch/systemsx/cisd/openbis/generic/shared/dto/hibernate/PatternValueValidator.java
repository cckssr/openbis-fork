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

import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity.IPatternCompiler;
import ch.ethz.sis.openbis.generic.server.asapi.v3.executor.entity.PatternCompiler;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityPropertyPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityTypePropertyTypePE;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class PatternValueValidator implements ConstraintValidator<PatternValue, EntityPropertyPE>
{

    private Map<Long, Pattern> propertyTypeToPatternMap;
    private IPatternCompiler compiler;

    @Override
    public final void initialize(final PatternValue annotation)
    {
        propertyTypeToPatternMap = new ConcurrentHashMap<>();
        compiler = new PatternCompiler();
    }

    @Override
    public boolean isValid(EntityPropertyPE entityPropertyPE, ConstraintValidatorContext constraintValidatorContext)
    {
        if(entityPropertyPE.getValue() != null && entityPropertyPE.getEntityTypePropertyType() != null) {
            EntityTypePropertyTypePE etpt = entityPropertyPE.getEntityTypePropertyType();
                if(etpt.getPatternType() != null) {
                    Pattern pattern;
                    if(propertyTypeToPatternMap.containsKey(etpt.getId())) {
                        pattern = propertyTypeToPatternMap.get(etpt.getId());
                        if(!etpt.getPatternRegex().equals(pattern.pattern())) {
                            pattern = updatePattern(etpt.getId(), etpt.getPattern(), etpt.getPatternType());
                        }
                    } else {
                        pattern = updatePattern(etpt.getId(), etpt.getPattern(), etpt.getPatternType());
                    }
                    boolean valid = pattern.matcher(entityPropertyPE.getValue()).matches();
                    if(!valid) {
                        constraintValidatorContext.disableDefaultConstraintViolation();
                        constraintValidatorContext
                                .buildConstraintViolationWithTemplate("Value: '" + entityPropertyPE.getValue() + "' is not matching defined pattern!")
                                .addConstraintViolation();
                        return false;
                    }
                }
        }
        return true;
    }

    private Pattern updatePattern(Long etptId, String pattern, String patternType)
    {
        Pattern compiledPattern = compiler.compilePattern(pattern, patternType);
        propertyTypeToPatternMap.put(etptId, compiledPattern);
        return compiledPattern;
    }

}
