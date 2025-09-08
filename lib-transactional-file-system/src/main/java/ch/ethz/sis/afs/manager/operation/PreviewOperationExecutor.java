/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
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
package ch.ethz.sis.afs.manager.operation;

import ch.ethz.sis.afs.dto.Transaction;
import ch.ethz.sis.afs.dto.operation.OperationName;
import ch.ethz.sis.afs.dto.operation.PreviewOperation;
import ch.ethz.sis.afs.exception.AFSExceptions;
import ch.ethz.sis.shared.io.IOUtils;
import lombok.NonNull;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static ch.ethz.sis.afs.exception.AFSExceptions.*;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.CACHED_PREVIEW_SUFFIX;
import static ch.ethz.sis.afs.manager.operation.OperationExecutor.HIDDEN_AFS_DIRECTORY;

public class PreviewOperationExecutor implements NonModifyingOperationExecutor<PreviewOperation> {
    //
    // Singleton
    //

    private static final PreviewOperationExecutor instance;

    static {
        instance = new PreviewOperationExecutor();
    }

    private PreviewOperationExecutor() {
    }

    public static PreviewOperationExecutor getInstance() {
        return instance;
    }

    @Override
    public byte[] executeOperation(@NonNull Transaction transaction, @NonNull PreviewOperation operation) throws Exception {

        if(IOUtils.exists(operation.getSource())) {
            if(IOUtils.isRegularFile(operation.getSource())) {
                Path sourcePath = Path.of(operation.getSource());
                Path hiddenFolderPath = sourcePath.getParent().resolve(HIDDEN_AFS_DIRECTORY).toAbsolutePath();
                if ( !IOUtils.isDirectory(hiddenFolderPath.toString()) ) {
                    IOUtils.createDirectories(hiddenFolderPath.toString());
                }
                Path sourceName = sourcePath.getFileName();
                String cachePath = hiddenFolderPath.resolve( sourceName.toString() + CACHED_PREVIEW_SUFFIX).toAbsolutePath().toString();
                if ( IOUtils.isRegularFile(cachePath) ) {
                    byte[] cachedValue = IOUtils.readFully(cachePath);
                    return cachedValue;
                } else {

                    if ( operation.getEnabledPreviewFileTypes().stream().anyMatch(suffix -> operation.getSource().endsWith(suffix)) && Files.size(sourcePath) < operation.getEnablePreviewSizeInBytes()) {
                        //Read image
                        BufferedImage originalImage;
                        try {
                            originalImage = ImageIO.read(sourcePath.toFile());
                        } catch (Exception e) {
                            originalImage = null;
                        }
                        if (originalImage == null) {
                            return new byte[0];
                        }

                        int originalWidth = originalImage.getWidth();
                        int originalHeight = originalImage.getHeight();

                        int longerDimension = Math.max(originalWidth, originalHeight);
                        int shorterDimension = Math.min(originalWidth, originalHeight);

                        int newLongerDimension = Math.min(longerDimension, 1980);
                        int newShorterDimension = (int) ((double) newLongerDimension / longerDimension * shorterDimension);

                        int newWidth = longerDimension == originalWidth ? newLongerDimension : newShorterDimension;
                        int newHeight = longerDimension == originalWidth ? newShorterDimension : newLongerDimension;

                        //Resize image
                        Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2d = resizedImage.createGraphics();
                        g2d.drawImage(scaledImage, 0, 0, null);
                        g2d.dispose();

                        // Compress to JPEG with lower quality
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);

                        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
                        writer.setOutput(ios);

                        ImageWriteParam param = writer.getDefaultWriteParam();
                        if (param.canWriteCompressed()) {
                            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            param.setCompressionQuality(0.5f); // Quality: 0.0 (lowest) to 1.0 (highest)
                        }

                        writer.write(null, new IIOImage(resizedImage, null, null), param);

                        writer.dispose();
                        ios.close();

                        byte[] previewBytes =  baos.toByteArray();

                        if(IOUtils.exists(cachePath)) {
                            IOUtils.delete(cachePath);
                        }
                        IOUtils.createFile(cachePath);
                        IOUtils.write(cachePath, 0L, previewBytes);

                        return previewBytes;
                    } else {
                        return new byte[0];
                    }
                }
            } else {
                AFSExceptions.throwInstance(PathNotRegularFile, OperationName.Preview.name(), operation.getSource());
                return null;
            }
        } else {
            AFSExceptions.throwInstance(PathNotInStore, OperationName.Preview.name(), operation.getSource());
            return null;
        }
    }
}
