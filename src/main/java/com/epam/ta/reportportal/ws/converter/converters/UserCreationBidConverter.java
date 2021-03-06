/*
 * Copyright 2017 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-api
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.epam.ta.reportportal.ws.converter.converters;

import com.epam.ta.reportportal.commons.EntityUtils;
import com.epam.ta.reportportal.database.entity.user.UserCreationBid;
import com.epam.ta.reportportal.ws.model.user.CreateUserRQ;
import com.google.common.base.Preconditions;

import java.util.UUID;
import java.util.function.Function;

/**
 * Converts internal DB model to DTO
 *
 * @author Pavel Bortnik
 */
public final class UserCreationBidConverter {

    private UserCreationBidConverter() {
        //static only
    }

    public static final Function<CreateUserRQ, UserCreationBid> TO_USER = request -> {
        Preconditions.checkNotNull(request);
        UserCreationBid user = new UserCreationBid();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(EntityUtils.normalizeId(request.getEmail().trim()));
        user.setDefaultProject(EntityUtils.normalizeId(request.getDefaultProject()));
        user.setRole(request.getRole());
        return user;
    };
}
