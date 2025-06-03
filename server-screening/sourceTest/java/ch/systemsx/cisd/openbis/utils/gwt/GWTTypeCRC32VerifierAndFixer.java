/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
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

package ch.systemsx.cisd.openbis.utils.gwt;

import com.google.gwt.user.server.rpc.impl.SerializabilityUtil;
import com.google.gwt.user.server.rpc.impl.StandardSerializationPolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GWTTypeCRC32VerifierAndFixer
{

    private static final String GWT_SOURCE_CODE_FOLDER = "/home/tuftak/IdeaProjects/openbis-working-onsync/core-plugin-openbis/resource/gwt/";
    private static final String GWT_RPC_FILE = "/home/tuftak/IdeaProjects/openbis-working-onsync/core-plugin-openbis/resource/gwt/ch.systemsx.cisd.openbis.OpenBIS/D92B876EF6D622EDAED05608D2D8D122.gwt.rpc";

    private final Map<String, String> classIdMap = new HashMap<>();
    private final Path gwtSourceCodeFolder;

    public GWTTypeCRC32VerifierAndFixer(Path gwtSourceCodeFolder) {
        this.gwtSourceCodeFolder = gwtSourceCodeFolder;
    }

    public void processFile(Path rpcFilePath) throws IOException {
        List<String> lines = Files.readAllLines(rpcFilePath);
        for (String line : lines) {
            try {
                parseLine(line);
            } catch (Exception e) {
                System.err.println("Error parsing line:\n" + line);
                e.printStackTrace();
            }
        }
    }

    private void parseLine(String line) {
        // Expected line format: className, true/false, ..., className/CRC, CRC
        Pattern pattern = Pattern.compile("^([^,]+).*?/([0-9-]+),\\s*([0-9-]+)$");
        Matcher matcher = pattern.matcher(line.trim());

        if (matcher.find()) {
            String className = matcher.group(1).trim();
            if (!className.startsWith("ch")) return;

            try {
                Class<?> clazz = Class.forName(className);

                String oldSignature = matcher.group(3).trim();
                classIdMap.put(className, oldSignature);

                String newSignature = SerializabilityUtil.getSerializationSignature(
                        clazz, new StandardSerializationPolicy(new HashMap<>(), new HashMap<>(), new HashMap<>())
                );

                if (!oldSignature.equals(newSignature)) {
                    System.out.printf("%s expects(file): %s, but calculated: %s%n",
                            className, oldSignature, newSignature);
                    replaceSignatureInFiles(oldSignature, newSignature);
                }

            } catch (ClassNotFoundException e) {
                System.err.println("Class not found: " + className);
            } catch (IOException e) {
                throw new RuntimeException("File update failed for class: " + className, e);
            } catch (Exception e) {
                // Catch errors from serialization generation for non-compatible classes
                System.err.println("Failed to compute signature for: " + className);
                e.printStackTrace();
            }

        }
    }

    private void replaceSignatureInFiles(String oldSignature, String newSignature) throws IOException {
        try (Stream<Path> files = Files.walk(gwtSourceCodeFolder)) {
            files.filter(Files::isRegularFile)
                .filter(path -> !path.toString().endsWith(".jar"))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path, StandardCharsets.UTF_8);
                        if (content.contains(oldSignature)) {
                            String updated = content.replace(oldSignature, newSignature);
                            Files.writeString(path, updated, StandardCharsets.UTF_8);
                            System.out.println("Updated file: " + path);
                        }
                    } catch (IOException e) {
                        //System.err.println("Error updating file: " + path + " – " + e.getMessage());
                    }
                });
        }
    }

    public void printAllMappedClasses() {
        classIdMap.forEach((cls, id) -> System.out.println(cls + " -> " + id));
    }

    public static void main(String[] args) throws IOException {
        Path sourceDir;
        Path rpcFile;

        if (args.length == 0) {
            // Use default constants
            sourceDir = Paths.get(GWT_SOURCE_CODE_FOLDER);
            rpcFile = Paths.get(GWT_RPC_FILE);
            System.out.println("Using default paths:");
        } else if (args.length == 2) {
            sourceDir = Paths.get(args[0]);
            rpcFile = Paths.get(args[1]);
            System.out.println("Using provided arguments:");
        } else {
            System.err.println("Invalid usage. Provide both source directory and .gwt.rpc file path, or no arguments to use defaults.");
            System.err.println("Usage: java GWTTypeCRC32VerifierAndFixer [<source-dir> <rpc-file>]");
            return;
        }

        // Check if the directory exists
        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            System.err.println("Source directory does not exist or is not a directory: " + sourceDir);
            return;
        }

        // Check if the rpc file exists
        if (!Files.exists(rpcFile) || !Files.isRegularFile(rpcFile)) {
            System.err.println(".gwt.rpc file does not exist or is not a regular file: " + rpcFile);
            return;
        }

        System.out.println("Source directory: " + sourceDir.toAbsolutePath());
        System.out.println(".gwt.rpc file: " + rpcFile.toAbsolutePath());

        GWTTypeCRC32VerifierAndFixer checker = new GWTTypeCRC32VerifierAndFixer(sourceDir);
        checker.processFile(rpcFile);
        checker.printAllMappedClasses();
    }


}
