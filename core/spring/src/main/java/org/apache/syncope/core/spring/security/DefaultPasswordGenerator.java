/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.spring.security;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.spring.ImplementationManager;
import org.apache.syncope.core.spring.policy.DefaultPasswordRule;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generate random passwords according to given policies.
 * When no minimum and / or maximum length are specified, default values are set.
 *
 * <strong>WARNING</strong>: This class only takes {@link DefaultPasswordRuleConf} into account.
 */
public class DefaultPasswordGenerator implements PasswordGenerator {

    protected static final Logger LOG = LoggerFactory.getLogger(PasswordGenerator.class);

    protected static final int VERY_MIN_LENGTH = 0;

    protected static final int VERY_MAX_LENGTH = 64;

    protected static final int MIN_LENGTH_IF_ZERO = 8;

    @Transactional(readOnly = true)
    @Override
    public String generate(final ExternalResource resource) {
        List<PasswordPolicy> policies = new ArrayList<>();

        if (resource.getPasswordPolicy() != null) {
            policies.add(resource.getPasswordPolicy());
        }

        return generate(policies);
    }

    @Override
    public String generate(final List<PasswordPolicy> policies) {
        List<DefaultPasswordRuleConf> ruleConfs = new ArrayList<>();

        policies.stream().forEach(policy -> policy.getRules().forEach(impl -> {
            try {
                ImplementationManager.buildPasswordRule(impl).ifPresent(rule -> {
                    if (rule.getConf() instanceof DefaultPasswordRuleConf) {
                        ruleConfs.add((DefaultPasswordRuleConf) rule.getConf());
                    }
                });
            } catch (Exception e) {
                LOG.error("Invalid {}, ignoring...", impl, e);
            }
        }));

        return generate(merge(ruleConfs));
    }

    protected DefaultPasswordRuleConf merge(final List<DefaultPasswordRuleConf> defaultRuleConfs) {
        DefaultPasswordRuleConf result = new DefaultPasswordRuleConf();
        result.setMinLength(VERY_MIN_LENGTH);
        result.setMaxLength(VERY_MAX_LENGTH);

        defaultRuleConfs.forEach(ruleConf -> {
            if (ruleConf.getMinLength() > result.getMinLength()) {
                result.setMinLength(ruleConf.getMinLength());
            }

            if (ruleConf.getMaxLength() > 0 && ruleConf.getMaxLength() < result.getMaxLength()) {
                result.setMaxLength(ruleConf.getMaxLength());
            }

            if (ruleConf.getAlphabetical() > result.getAlphabetical()) {
                result.setAlphabetical(ruleConf.getAlphabetical());
            }

            if (ruleConf.getUppercase() > result.getUppercase()) {
                result.setUppercase(ruleConf.getUppercase());
            }

            if (ruleConf.getLowercase() > result.getLowercase()) {
                result.setLowercase(ruleConf.getLowercase());
            }

            if (ruleConf.getDigit() > result.getDigit()) {
                result.setDigit(ruleConf.getDigit());
            }

            if (ruleConf.getSpecial() > result.getSpecial()) {
                result.setSpecial(ruleConf.getSpecial());
            }

            if (!ruleConf.getSpecialChars().isEmpty()) {
                result.getSpecialChars().addAll(ruleConf.getSpecialChars().stream().
                        filter(c -> !result.getSpecialChars().contains(c)).collect(Collectors.toList()));
            }

            if (!ruleConf.getIllegalChars().isEmpty()) {
                result.getIllegalChars().addAll(ruleConf.getIllegalChars().stream().
                        filter(c -> !result.getIllegalChars().contains(c)).collect(Collectors.toList()));
            }

            if (ruleConf.getRepeatSame() > result.getRepeatSame()) {
                result.setRepeatSame(ruleConf.getRepeatSame());
            }

            if (!result.isUsernameAllowed()) {
                result.setUsernameAllowed(ruleConf.isUsernameAllowed());
            }

            if (!ruleConf.getWordsNotPermitted().isEmpty()) {
                result.getWordsNotPermitted().addAll(ruleConf.getWordsNotPermitted().stream().
                        filter(w -> !result.getWordsNotPermitted().contains(w)).collect(Collectors.toList()));
            }
        });

        if (result.getMinLength() == 0) {
            result.setMinLength(
                    result.getMaxLength() < MIN_LENGTH_IF_ZERO ? result.getMaxLength() : MIN_LENGTH_IF_ZERO);
        }
        if (result.getMinLength() > result.getMaxLength()) {
            result.setMaxLength(result.getMinLength());
        }

        return result;
    }

    protected String generate(final DefaultPasswordRuleConf ruleConf) {
        List<CharacterRule> characterRules = DefaultPasswordRule.conf2Rules(ruleConf).stream().
                filter(CharacterRule.class::isInstance).map(CharacterRule.class::cast).
                collect(Collectors.toList());
        if (characterRules.isEmpty()) {
            int halfMinLength = ruleConf.getMinLength() / 2;
            characterRules = List.of(
                    new CharacterRule(EnglishCharacterData.Alphabetical, halfMinLength),
                    new CharacterRule(EnglishCharacterData.Digit, halfMinLength));
        }
        return SecureRandomUtils.passwordGenerator().generatePassword(ruleConf.getMinLength(), characterRules);
    }
}
