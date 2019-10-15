/*
 * Copyright 2012 - 2019 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.scraper.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class SubtitleUtils. This is used for subtitle related tasks
 * 
 * @author Myron Boyle
 * @since 1.0
 */
public class SubtitleUtils {
  private static final Logger LOGGER          = LoggerFactory.getLogger(SubtitleUtils.class);
  /**
   * Size of the chunks that will be hashed in bytes (64 KB)
   */
  private static final int    HASH_CHUNK_SIZE = 64 * 1024;

  private SubtitleUtils() {
  }

  /**
   * Returns SubDB hash or empty string if error
   * 
   * @param file
   *          the file to compute the DB hash
   * @return hash
   */
  @SuppressWarnings("resource")
  public static String computeSubDBHash(File file) {
    long size = file.length();
    long chunkSizeForFile = Math.min(HASH_CHUNK_SIZE, size);

    FileChannel fileChannel = null;
    try {
      fileChannel = new FileInputStream(file).getChannel();

      ByteBuffer head = fileChannel.map(MapMode.READ_ONLY, 0, chunkSizeForFile);
      ByteBuffer tail = fileChannel.map(MapMode.READ_ONLY, Math.max(size - HASH_CHUNK_SIZE, 0), chunkSizeForFile);

      // md.digest(ByteBuffer.array()) always error *grml
      final byte[] hbytes = new byte[head.remaining()];
      head.duplicate().get(hbytes);
      final byte[] tbytes = new byte[tail.remaining()];
      tail.duplicate().get(tbytes);

      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(hbytes);
      md.update(tbytes);

      return StrgUtils.bytesToHex(md.digest());
    }
    catch (Exception e) {
      LOGGER.error("Error computing SubDB hash", e);
    }
    finally {
      try {
        if (fileChannel != null) {
          fileChannel.close();
        }
      }
      catch (IOException e) {
        LOGGER.error("Error closing file stream", e);
      }
    }
    return "";
  }

  /**
   * Returns OpenSubtitle hash or empty string if error
   * 
   * @param file
   *          the file to compute the hash
   * @return hash
   */
  @SuppressWarnings("resource")
  @Deprecated
  public static String computeOpenSubtitlesHash(File file) {
    long size = file.length();
    long chunkSizeForFile = Math.min(HASH_CHUNK_SIZE, size);

    FileChannel fileChannel = null;
    try {
      fileChannel = new FileInputStream(file).getChannel();
      long head = computeOpenSubtitlesHashForChunk(fileChannel.map(MapMode.READ_ONLY, 0, chunkSizeForFile));
      long tail = computeOpenSubtitlesHashForChunk(fileChannel.map(MapMode.READ_ONLY, Math.max(size - HASH_CHUNK_SIZE, 0), chunkSizeForFile));

      return String.format("%016x", size + head + tail);
    }
    catch (Exception e) {
      LOGGER.error("Error computing OpenSubtitles hash", e);
    }
    finally {
      try {
        if (fileChannel != null) {
          fileChannel.close();
        }
      }
      catch (IOException e) {
        LOGGER.error("Error closing file stream", e);
      }
    }
    return "";
  }

  private static long computeOpenSubtitlesHashForChunk(ByteBuffer buffer) {

    LongBuffer longBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
    long hash = 0;

    while (longBuffer.hasRemaining()) {
      hash += longBuffer.get();
    }

    return hash;
  }
}