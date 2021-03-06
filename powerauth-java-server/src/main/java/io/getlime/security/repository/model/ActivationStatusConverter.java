/**
 * Copyright 2015 Lime - HighTech Solutions s.r.o.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.getlime.security.repository.model;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.springframework.stereotype.Component;

/**
 * Converter between {@link ActivationStatus} and integer values.
 * 
 * @author Petr Dvorak
 *
 */
@Converter
@Component
public class ActivationStatusConverter implements AttributeConverter<ActivationStatus, Integer> {

	@Override
	public Integer convertToDatabaseColumn(ActivationStatus status) {
		return new Integer(status.getByte());
	}

	@Override
	public ActivationStatus convertToEntityAttribute(Integer b) {
		switch (b) {
		case 1:
			return ActivationStatus.CREATED;
		case 2:
			return ActivationStatus.OTP_USED;
		case 3:
			return ActivationStatus.ACTIVE;
		case 4:
			return ActivationStatus.BLOCKED;
		default:
			return ActivationStatus.REMOVED;
		}
	}

}
