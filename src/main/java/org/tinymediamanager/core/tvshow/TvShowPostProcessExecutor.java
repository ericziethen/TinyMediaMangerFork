/*
 * Copyright 2012 - 2021 Manuel Laggner
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
package org.tinymediamanager.core.tvshow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.PostProcess;
import org.tinymediamanager.core.jmte.JmteUtils;
import org.tinymediamanager.core.jmte.NamedDateRenderer;
import org.tinymediamanager.core.jmte.NamedFirstCharacterRenderer;
import org.tinymediamanager.core.jmte.NamedUpperCaseRenderer;
import org.tinymediamanager.core.jmte.TmmModelAdaptor;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

import com.floreysoft.jmte.Engine;

/**
 * the class {@link TvShowPostProcessExecutor} executes post process steps for movies
 *
 * @author Wolfgang Janes
 */
public class TvShowPostProcessExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(TvShowPostProcessExecutor.class);
  private final PostProcess   postProcess;

  public TvShowPostProcessExecutor(PostProcess postProcess) {
    this.postProcess = postProcess;
  }

  public void execute() {
    List<String> command;

    List<Object> selectedTvShowObject = new ArrayList<>(TvShowUIModule.getInstance().getSelectionModel().getSelectedObjects());

    for (Object object : selectedTvShowObject) {
      if (object instanceof TvShow) {
        // Episode
        command = substituteTokens("tvShow", (TvShow) object);
        command.add(0, postProcess.getPath());
        // Execute File
        try {
          executeCommand(command, object);
        }
        catch (IOException | InterruptedException e) {
          e.printStackTrace();
        }
      }
      else if (object instanceof TvShowEpisode) {
        // Episode
        command = substituteTokens("episode", (TvShowEpisode) object);
        command.add(0, postProcess.getPath());
        // Execute File
        try {
          executeCommand(command, object);
        }
        catch (IOException | InterruptedException e) {
          e.printStackTrace();
        }
      }
      else if (object instanceof TvShowSeason) {
        // Season
        command = substituteTokens("season", (TvShowSeason) object);
        command.add(0, postProcess.getPath());
        try {
          executeCommand(command, object);
        }
        catch (IOException | InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private <T> List<String> substituteTokens(String id, T object) {

    Engine engine;
    Map<String, Object> root;
    List<String> commandList = new ArrayList<>();

    engine = Engine.createEngine();
    engine.registerNamedRenderer(new NamedDateRenderer());
    engine.registerNamedRenderer(new NamedUpperCaseRenderer());
    engine.registerNamedRenderer(new NamedFirstCharacterRenderer());
    engine.setModelAdaptor(new TmmModelAdaptor());

    root = new HashMap<>();
    root.put(id, object);

    String transformedCommand = engine.transform(JmteUtils.morphTemplate(postProcess.getCommand(), TvShowRenamer.getTokenMap()), root);
    commandList.add(transformedCommand);
    return commandList;

  }

  private void executeCommand(List<String> cmdline, Object object) throws IOException, InterruptedException {

    String command = String.join(" ", cmdline);
    LOGGER.debug("Running command: {}", command);

    ProcessBuilder pb;
    if (SystemUtils.IS_OS_WINDOWS) {
      pb = new ProcessBuilder("cmd", "/c", "start", command);
    }
    else if (SystemUtils.IS_OS_MAC) {
      pb = new ProcessBuilder("/bin/sh", "-c", command);
    }
    else {
      pb = new ProcessBuilder("/bin/sh", "-c", command);
    }

    pb.redirectErrorStream(true);

    if (object instanceof TvShow) {
      pb.directory(((TvShow) object).getPathNIO().toFile());
    }
    else if (object instanceof TvShowEpisode) {
      pb.directory(((TvShowEpisode) object).getPathNIO().toFile());
    }
    else if (object instanceof TvShowSeason) {
      // use the renamer to get the season foldername
      TvShowSeason tvShowSeason = (TvShowSeason) object;
      Path seasonFolderName = Paths.get(TvShowHelpers.detectSeasonFolder(tvShowSeason.getTvShow(), tvShowSeason.getSeason()));
      pb.directory(seasonFolderName.toFile());
    }

    final Process process = pb.start();

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      new Thread(() -> {
        try {
          IOUtils.copy(process.getInputStream(), outputStream);
        }
        catch (IOException e) {
          LOGGER.debug("could not get output from the process", e);
        }
      }).start();

      int processValue = process.waitFor();
      if (processValue != 0) {
        LOGGER.debug("error at Script: '{}", outputStream.toString(StandardCharsets.UTF_8));
        throw new IOException("error running Script - code '" + processValue + "'");
      }
      outputStream.toString(StandardCharsets.UTF_8);
    }
    finally {
      process.destroy();
      // Process must be destroyed before closing streams, can't use try-with-resources,
      // as resources are closing when leaving try block, before finally
      IOUtils.close(process.getErrorStream());
    }
  }
}
