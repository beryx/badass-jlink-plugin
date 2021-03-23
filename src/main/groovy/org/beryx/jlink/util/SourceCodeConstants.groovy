/*
 * Copyright 2021 the original author or authors.
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

class SourceCodeConstants {
    static final String GET_MODULE_DESCRIPTOR = '''
    private static ModuleDescriptor getModuleDescriptor(File f) {
        try {
            if(!f.isFile()) throw new IllegalArgumentException(f + " is not a file");
            if(f.getName().equals("module-info.class")) {
                return ModuleDescriptor.read(new FileInputStream(f));
            }
            if(!f.getName().endsWith(".jar") && !f.getName().endsWith(".jmod")) throw new IllegalArgumentException("Unsupported file type: " + f);
            String prefix = f.getName().endsWith(".jmod") ? "classes/" : "";
            ZipFile zipFile = new ZipFile(f);
            for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                if(entry.getName().equals(prefix + "module-info.class")) {
                    InputStream entryStream = zipFile.getInputStream(entry);
                    return ModuleDescriptor.read(entryStream);
                }
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
'''
}
