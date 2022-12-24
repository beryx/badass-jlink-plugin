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
package org.beryx.jlink.util


class PathUtil {
    static String getNonModularJarsDirPath(String jlinkBasePath) { "$jlinkBasePath/nonmodjars" }
    static String getMergedJarsDirPath(String jlinkBasePath) { "$jlinkBasePath/mergedjars" }
    static String getTmpMergedModuleDirPath(String jlinkBasePath) { "$jlinkBasePath/tmpmerged" }
    static String getJlinkJarsDirPath(String jlinkBasePath) { "$jlinkBasePath/jlinkjars" }
    static String getTmpJarsDirPath(String jlinkBasePath) { "$jlinkBasePath/tmpjars" }
    static String getTmpModuleInfoDirPath(String jlinkBasePath) { "$jlinkBasePath/tmpmodinfo" }
    static String getDelegatingModulesDirPath(String jlinkBasePath) { "$jlinkBasePath/delegating" }
}
