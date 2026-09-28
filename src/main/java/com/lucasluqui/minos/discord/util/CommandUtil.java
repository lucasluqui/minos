package com.lucasluqui.minos.discord.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandUtil
{
  /**
   * Lists all commands defs files from both 'commands' and 'global-commands' directories.
   *
   * @return List of command defs JSON files found.
   */
  public static List<String> getCommandsDefs ()
    throws IOException, URISyntaxException
  {
    ensureCommandsDir();

    List<String> commandsNames = getFileNamesFromResource(COMMANDS_DIR);
    List<String> globalCommandsNames = getFileNamesFromResource(GLOBAL_COMMANDS_DIR);

    return Stream.concat(commandsNames.stream(), globalCommandsNames.stream()).toList();
  }

  private static List<String> getFileNamesFromResource(String dirPath) throws IOException, URISyntaxException {
    // Normalize path to remove leading slash for classloader lookup if present
    String resourcePath = dirPath.startsWith("/") ? dirPath.substring(1) : dirPath;

    var url = CommandUtil.class.getClassLoader().getResource(resourcePath);
    if (url == null) {
      // Fallback: try original path
      url = CommandUtil.class.getClassLoader().getResource(dirPath);
    }

    if (url == null) {
      return Collections.emptyList();
    }

    // Handle running in an IDE
    if (url.getProtocol().equals("file")) {
      Path path = Path.of(url.toURI());
      if (!Files.exists(path) || !Files.isDirectory(path)) {
        return Collections.emptyList();
      }
      try (var stream = Files.list(path)) {
        return stream.filter(Files::isRegularFile)
          .map(Path::getFileName)
          .map(Path::toString)
          .toList();
      }
    }

    // Handle running from a packaged JAR
    else if (url.getProtocol().equals("jar")) {
      String[] parts = url.toURI().toString().split("!");
      Path jarPath = Path.of(URI.create(parts[0].replace("jar:", "")));
      String internalPath = parts[1];

      try (FileSystem fs = FileSystems.newFileSystem(jarPath, Collections.emptyMap())) {
        Path targetDir = fs.getPath(internalPath);

        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
          return Collections.emptyList();
        }

        try (var stream = Files.list(targetDir)) {
          return stream.filter(Files::isRegularFile)
            .map(Path::getFileName)
            .map(Path::toString)
            .toList();
        }
      }
    }

    return Collections.emptyList();
  }

  public static List<String> getCommands (List<String> fileNames)
    throws IOException
  {
    ensureCommandsDir();

    // Get all the files inside this folder and return the contents of the files as a list of strings
    List<String> list = new ArrayList<>();
    for (String file : fileNames) {
      String resourceFileAsString = getResourceFileAsString(COMMANDS_DIR + "/" + file);
      list.add(Objects.requireNonNull(resourceFileAsString, "Command file not found: " + file));
    }
    return list;
  }

  private static void ensureCommandsDir ()
  {
    // Confirm that the commands folder exists
    URL url = CommandUtil.class.getClassLoader().getResource(COMMANDS_DIR + "/");
    Objects.requireNonNull(url, COMMANDS_DIR + "/" + " could not be found");
  }

  /**
   * Gets a specific resource file as String
   *
   * @param fileName The file path omitting "resources/"
   * @return The contents of the file as a String, otherwise throws an exception
   */
  private static String getResourceFileAsString (String fileName)
    throws IOException
  {
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    try (InputStream resourceAsStream = classLoader.getResourceAsStream(fileName)) {
      if (resourceAsStream == null) return null;
      try (InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream);
           BufferedReader reader = new BufferedReader(inputStreamReader)) {
        return reader.lines().collect(Collectors.joining(System.lineSeparator()));
      }
    }
  }

  /** Directory for guild-only commands. */
  private static final String COMMANDS_DIR = "rsrc/commands";

  /** Directory for global commands (extends to all guilds). */
  private static final String GLOBAL_COMMANDS_DIR = "rsrc/global-commands";
}
