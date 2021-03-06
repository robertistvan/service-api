/*
 * Copyright 2016 EPAM Systems
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

package com.epam.ta.reportportal.core.log.impl;

import com.epam.ta.reportportal.core.log.ICreateLogHandler;
import com.epam.ta.reportportal.database.entity.Log;
import com.epam.ta.reportportal.database.entity.item.TestItem;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.job.SaveBinaryDataJob;
import com.epam.ta.reportportal.ws.model.EntryCreatedRS;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.base.StandardSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import javax.inject.Provider;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Asynchronous implementation of {@link ICreateLogHandler}. Saves log and
 * returns response, also starts saving binary data in storage asynchronously to
 * decrease server response time
 *
 * @author Andrei Varabyeu
 */
@Service
public class AsyncCreateLogHandler extends CreateLogHandler implements ICreateLogHandler {

    /**
     * We are using {@link Provider} there because we need
     * {@link SaveBinaryDataJob} with scope prototype. Since current class is in
     * singleton scope, we have to find a way to get new instance of job for new
     * execution
     */
    @Autowired
    private Provider<SaveBinaryDataJob> saveBinaryDataJob;

    @Autowired
    @Qualifier("saveLogsTaskExecutor")
    private TaskExecutor taskExecutor;

    @Override
    @Nonnull
    public EntryCreatedRS createLog(@Nonnull SaveLogRQ createLogRQ, MultipartFile file, String projectName) {
        TestItem testItem = testItemRepository.findOne(createLogRQ.getTestItemId());
        validate(testItem, createLogRQ);

        Log log = logBuilder.get().addSaveLogRQ(createLogRQ).addTestItem(testItem).build();

        try {
            logRepository.save(log);
        } catch (Exception exc) {
            throw new ReportPortalException("Error while Log instance creating.", exc);
        }

        if (null != file) {
            try {
                File tempFile = Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.toString(), "rp", file.getOriginalFilename()).toFile();
                if (tempFile.exists()) {
                    tempFile = File.createTempFile(file.getOriginalFilename(), UUID.randomUUID().toString());
                }
                com.google.common.io.Files.asByteSink(tempFile).writeFrom(file.getInputStream());
                taskExecutor.execute(
                        saveBinaryDataJob.get().withProject(projectName)
                                .withFile(tempFile)
                                .withContentType(file.getContentType())
                                .withLength(file.getSize())
                                .withLog(log));
            } catch (IOException e) {
                throw new ReportPortalException(ErrorType.BAD_REQUEST_ERROR, "Unable to save file");
            }
        }
        return new EntryCreatedRS(log.getId());
    }
}
