package org.adde0109.matcher;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.common.collect.ImmutableMap;
import com.google.gson.annotations.Expose;

import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatcherConfig {


  @Expose
  private Differentiators versionDifferentiatorsSettings;

  private List<Differentiators> versions;

  private MatcherConfig(Differentiators versionDifferentiatorsSettings) {
    this.versionDifferentiatorsSettings = versionDifferentiatorsSettings;
  };

  public static MatcherConfig read(Path path) {
    URL defaultConfigLocation = MatcherPlugin.class.getClassLoader()
            .getResource("default-matcher.toml");
    if (defaultConfigLocation == null) {
      throw new RuntimeException("Default configuration file does not exist.");
    }

    CommentedFileConfig config = CommentedFileConfig.builder(path)
            .defaultData(defaultConfigLocation)
            .autosave()
            .preserveInsertionOrder()
            .sync()
            .build();
    config.load();

    Differentiators versionDifferentiatorsSettings = new Differentiators(config.get("versionDifferentiatorsSettings"));

    return new MatcherConfig(versionDifferentiatorsSettings);
  }

  public Map<String, String> getRules() {
    return versionDifferentiatorsSettings.initalServerMap;
  }

  private static class Differentiators {
    private Map<String, String> initalServerMap = new HashMap<>();
    private Differentiators(){
    }

    private Differentiators(CommentedConfig config) {
      if (config != null) {
        Map<String, String> initalServer = new HashMap<>();
        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
          if (entry.getValue() instanceof CommentedConfig) {
            initalServer.put(entry.getKey(), entry.getValue());
          }
        }
        this.initalServerMap = ImmutableMap.copyOf(initalServer);
      }
    }
  }
}
