/*
 * Copyright 2018 the original author or authors.
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
package org.beryx.jlink.impl

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.beryx.jlink.data.JlinkZipTaskData
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@CompileStatic
class JlinkZipTaskImpl extends BaseTaskImpl<JlinkZipTaskData> {
    private static final Logger LOGGER = Logging.getLogger(JlinkZipTaskImpl.class);

    JlinkZipTaskImpl(JlinkZipTaskData taskData) {
        super(taskData)
        LOGGER.info("taskData: $taskData")
    }

    @CompileDynamic
    void execute() {
        if(td.targetPlatforms) {
            def zipDir = td.imageZip.parentFile
            def zipName = td.imageZip.name
            int pos = zipName.lastIndexOf('.')
            def ext = (pos > 0) ? zipName.substring(pos+1) : 'zip'
            def baseName = (pos > 0) ? zipName.substring(0,pos) : zipName
            td.targetPlatforms.values().each { platform ->
                File zipFile = new File(zipDir, "${baseName}-${platform.name}.${ext}")
                File imageDir = new File(td.imageDir, "$td.launcherData.name-$platform.name")
                createZip(zipFile, imageDir)
            }
        } else {
            createZip(td.imageZip, td.imageDir)
        }
    }

    private void createZip(File zipFile, File imageDir) {
        zipFile.parentFile.mkdirs()
        new ZipOutputStream(new FileOutputStream(zipFile)).withCloseable { zos ->
            Path imagePath = imageDir.toPath()
            Path basePath = imageDir.parentFile.toPath()

            Files.walkFileTree(imagePath, new SimpleFileVisitor<Path>() {
                @Override
                FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativePath = basePath.relativize(file)
                    String entryName = relativePath.toString().replace('\\', '/')
                    ZipEntry entry = new ZipEntry(entryName)
                    entry.setTime(attrs.lastModifiedTime().toMillis())

                    zos.putNextEntry(entry)
                    Files.copy(file, zos)
                    zos.closeEntry()
                    return FileVisitResult.CONTINUE
                }

                @Override
                FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(imagePath)) {
                        Path relativePath = basePath.relativize(dir)
                        ZipEntry entry = new ZipEntry(relativePath.toString().replace('\\', '/') + '/')
                        zos.putNextEntry(entry)
                        zos.closeEntry()
                    }
                    return FileVisitResult.CONTINUE
                }
            })
        }
    }
}
